package com.google.mediapipe.apps.hairsegmentationgpu;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.Log;

import com.google.mediapipe.components.TextureFrameConsumer;
import com.google.mediapipe.components.TextureFrameProducer;
import com.google.mediapipe.framework.AppTextureFrame;
import com.google.mediapipe.glutil.ExternalTextureRenderer;
import com.google.mediapipe.glutil.GlThread;
import com.google.mediapipe.glutil.ShaderUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLContext;

public class MyCusExternalTextureConverter  implements TextureFrameProducer {
    private static final String TAG = "ExternalTextureConv"; // Max length of a tag is 23.
    private static final int DEFAULT_NUM_BUFFERS = 2; // Number of output frames allocated.
    private static final String THREAD_NAME = "MyCusExternalTextureConverter";

    private MyCusExternalTextureConverter.RenderThread thread;

    /**
     * Creates the MyCusExternalTextureConverter to create a working copy of each camera frame.
     *
     * @param numBuffers the number of camera frames that can enter processing simultaneously.
     */
    public MyCusExternalTextureConverter(EGLContext parentContext, int numBuffers) {
        thread = new MyCusExternalTextureConverter.RenderThread(parentContext, numBuffers);
        thread.setName(THREAD_NAME);
        thread.start();
        try {
            thread.waitUntilReady();
        } catch (InterruptedException ie) {
            // Someone interrupted our thread. This is not supposed to happen: we own
            // the thread, and we are not going to interrupt it. Therefore, it is not
            // reasonable for this constructor to throw an InterruptedException
            // (which is a checked exception). If it should somehow happen that the
            // thread is interrupted, let's set the interrupted flag again, log the
            // error, and throw a RuntimeException.
            Thread.currentThread().interrupt();
            Log.e(TAG, "thread was unexpectedly interrupted: " + ie.getMessage());
            throw new RuntimeException(ie);
        }
    }

    /**
     * Sets vertical flipping of the texture, useful for conversion between coordinate systems with
     * top-left v.s. bottom-left origins. This should be called before {@link
     * #setSurfaceTexture(SurfaceTexture, int, int)} or {@link
     * #setSurfaceTextureAndAttachToGLContext(SurfaceTexture, int, int)}.
     */
    public void setFlipY(boolean flip) {
        thread.setFlipY(flip);
    }

    /**
     * Sets an offset that can be used to adjust the timestamps on the camera frames, for example to
     * conform to a preferred time-base or to account for a known device latency. The offset is added
     * to each frame timetamp read by the MyCusExternalTextureConverter.
     */
    public void setTimestampOffsetNanos(long offsetInNanos) {
        thread.setTimestampOffsetNanos(offsetInNanos);
    }

    public MyCusExternalTextureConverter(EGLContext parentContext) {
        this(parentContext, DEFAULT_NUM_BUFFERS);
    }

    public MyCusExternalTextureConverter(
            EGLContext parentContext, SurfaceTexture texture, int targetWidth, int targetHeight) {
        this(parentContext);
        thread.setSurfaceTexture(texture, targetWidth, targetHeight);
    }

    /**
     * Sets the input surface texture.
     *
     * <p>The provided width and height will be the size of the converted texture, so if the input
     * surface texture is rotated (as expressed by its transformation matrix) the provided width and
     * height should be swapped.
     */
    // TODO: Clean up setSurfaceTexture methods.
    public void setSurfaceTexture(SurfaceTexture texture, int width, int height) {
        if (texture != null && (width == 0 || height == 0)) {
            throw new RuntimeException(
                    "MyCusExternalTextureConverter: setSurfaceTexture dimensions cannot be zero");
        }
        thread.getHandler().post(() -> thread.setSurfaceTexture(texture, width, height));
    }

    // TODO: Clean up setSurfaceTexture methods.
    public void setSurfaceTextureAndAttachToGLContext(SurfaceTexture texture, int width, int height) {
        if (texture != null && (width == 0 || height == 0)) {
            throw new RuntimeException(
                    "MyCusExternalTextureConverter: setSurfaceTexture dimensions cannot be zero");
        }
        thread
                .getHandler()
                .post(() -> thread.setSurfaceTextureAndAttachToGLContext(texture, width, height));
    }

    @Override
    public void setConsumer(TextureFrameConsumer next) {
        thread.setConsumer(next);
    }

    public void addConsumer(TextureFrameConsumer consumer) {
        thread.addConsumer(consumer);
    }

    public void removeConsumer(TextureFrameConsumer consumer) {
        thread.removeConsumer(consumer);
    }

    public void close() {
        if (thread == null) {
            return;
        }
        thread.getHandler().post(() -> thread.setSurfaceTexture(null, 0, 0));
        thread.quitSafely();
        try {
            thread.join();
        } catch (InterruptedException ie) {
            // Set the interrupted flag again, log the error, and throw a RuntimeException.
            Thread.currentThread().interrupt();
            Log.e(TAG, "thread was unexpectedly interrupted: " + ie.getMessage());
            throw new RuntimeException(ie);
        }
    }

    private static class RenderThread extends GlThread
            implements SurfaceTexture.OnFrameAvailableListener {
        private static final long NANOS_PER_MICRO = 1000; // Nanoseconds in one microsecond.
        private volatile SurfaceTexture surfaceTexture = null;
        private final List<TextureFrameConsumer> consumers;
        private List<AppTextureFrame> outputFrames = null;
        private int outputFrameIndex = -1;
        private ExternalTextureRenderer renderer = null;
        private long nextFrameTimestampOffset = 0;
        private long timestampOffsetNanos = 0;
        private long previousTimestamp = 0;
        private boolean previousTimestampValid = false;

        protected int destinationWidth = 0;
        protected int destinationHeight = 0;

        public RenderThread(EGLContext parentContext, int numBuffers) {
            super(parentContext);
            outputFrames = new ArrayList<>();
            outputFrames.addAll(Collections.nCopies(numBuffers, null));
            renderer = new ExternalTextureRenderer();
            consumers = new ArrayList<>();
        }

        public void setFlipY(boolean flip) {
            renderer.setFlipY(flip);
        }

        public void setSurfaceTexture(SurfaceTexture texture, int width, int height) {
            if (surfaceTexture != null) {
                surfaceTexture.setOnFrameAvailableListener(null);
            }
            surfaceTexture = texture;
            if (surfaceTexture != null) {
                surfaceTexture.setOnFrameAvailableListener(this);
            }
            destinationWidth = width;
            destinationHeight = height;
        }

        public void setSurfaceTextureAndAttachToGLContext(
                SurfaceTexture texture, int width, int height) {
            setSurfaceTexture(texture, width, height);
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            surfaceTexture.attachToGLContext(textures[0]);
        }

        public void setConsumer(TextureFrameConsumer consumer) {
            synchronized (consumers) {
                consumers.clear();
                consumers.add(consumer);
            }
        }

        public void addConsumer(TextureFrameConsumer consumer) {
            synchronized (consumers) {
                consumers.add(consumer);
            }
        }

        public void removeConsumer(TextureFrameConsumer consumer) {
            synchronized (consumers) {
                consumers.remove(consumer);
            }
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            handler.post(() -> renderNext(surfaceTexture));
        }

        @Override
        public void prepareGl() {
            super.prepareGl();

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            renderer.setup();
        }

        @Override
        public void releaseGl() {
            for (int i = 0; i < outputFrames.size(); ++i) {
                teardownDestination(i);
            }
            renderer.release();
            super.releaseGl(); // This releases the EGL context, so must do it after any GL calls.
        }

        public void setTimestampOffsetNanos(long offsetInNanos) {
            timestampOffsetNanos = offsetInNanos;
        }

        protected void renderNext(SurfaceTexture fromTexture) {
            if (fromTexture != surfaceTexture) {
                // Although the setSurfaceTexture and renderNext methods are correctly sequentialized on
                // the same thread, the onFrameAvailable callback is not. Therefore, it is possible for
                // onFrameAvailable to queue up a renderNext call while a setSurfaceTexture call is still
                // pending on the handler. When that happens, we should simply disregard the call.
                return;
            }
            try {
                synchronized (consumers) {
                    boolean frameUpdated = false;
                    for (TextureFrameConsumer consumer : consumers) {
                        AppTextureFrame outputFrame = nextOutputFrame();
                        // TODO: Switch to ref-counted single copy instead of making additional
                        // copies blitting to separate textures each time.
                        updateOutputFrame(outputFrame);
                        frameUpdated = true;

                        if (consumer != null) {
                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(
                                        TAG,
                                        String.format(
                                                "Locking tex: %d width: %d height: %d",
                                                outputFrame.getTextureName(),
                                                outputFrame.getWidth(),
                                                outputFrame.getHeight()));
                            }
                            outputFrame.setInUse();
                            CustomFrameProcessor processor = (CustomFrameProcessor)consumer;
                            processor.onNewFrame(outputFrame,0.0f);
                        }
                    }
                    if (!frameUpdated) {  // Need to update the frame even if there are no consumers.
                        AppTextureFrame outputFrame = nextOutputFrame();
                        // TODO: Switch to ref-counted single copy instead of making additional
                        // copies blitting to separate textures each time.
                        updateOutputFrame(outputFrame);
                    }
                }
            } finally {
            }
        }

        private void teardownDestination(int index) {
            if (outputFrames.get(index) != null) {
                waitUntilReleased(outputFrames.get(index));
                GLES20.glDeleteTextures(1, new int[] {outputFrames.get(index).getTextureName()}, 0);
                outputFrames.set(index, null);
            }
        }

        private void setupDestination(int index) {
            teardownDestination(index);
            int destinationTextureId = ShaderUtil.createRgbaTexture(destinationWidth, destinationHeight);
            Log.d(
                    TAG,
                    String.format(
                            "Created output texture: %d width: %d height: %d",
                            destinationTextureId, destinationWidth, destinationHeight));
            bindFramebuffer(destinationTextureId, destinationWidth, destinationHeight);
            outputFrames.set(
                    index, new AppTextureFrame(destinationTextureId, destinationWidth, destinationHeight));
        }


        /**
         * Gets next available frame or creates new one if next frame is not initialized
         * or cannot be used with current surface texture.
         *
         * <ul>
         *  <li>Makes sure frame width and height are same as current surface texture</li>
         *  <li>Makes sure frame is not in use (blocks thread until frame is released)</li>
         * </ul>
         *
         * NOTE: must be invoked on GL thread
         */
        private AppTextureFrame nextOutputFrame() {
            outputFrameIndex = (outputFrameIndex + 1) % outputFrames.size();
            AppTextureFrame outputFrame = outputFrames.get(outputFrameIndex);
            // Check if the size has changed.
            if (outputFrame == null
                    || outputFrame.getWidth() != destinationWidth
                    || outputFrame.getHeight() != destinationHeight) {
                // setupDestination will wait for the frame to be released before reallocating it.
                setupDestination(outputFrameIndex);
                outputFrame = outputFrames.get(outputFrameIndex);
            }
            waitUntilReleased(outputFrame);
            return outputFrame;
        }

        /**
         * Updates output frame with current pixels of surface texture and corresponding timestamp.
         *
         * @param outputFrame {@link AppTextureFrame} to populate.
         *
         * NOTE: must be invoked on GL thread
         */
        private void updateOutputFrame(AppTextureFrame outputFrame) {
            // Copy surface texture's pixels to output frame
            bindFramebuffer(outputFrame.getTextureName(), destinationWidth, destinationHeight);
            renderer.render(surfaceTexture);

            // Populate frame timestamp with surface texture timestamp after render() as renderer
            // ensures that surface texture has the up-to-date timestamp. (Also adjust
            // |nextFrameTimestampOffset| to ensure that timestamps increase monotonically.)
            long textureTimestamp =
                    (surfaceTexture.getTimestamp() + timestampOffsetNanos) / NANOS_PER_MICRO;
            if (previousTimestampValid
                    && textureTimestamp + nextFrameTimestampOffset <= previousTimestamp) {
                nextFrameTimestampOffset = previousTimestamp + 1 - textureTimestamp;
            }
            outputFrame.setTimestamp(textureTimestamp + nextFrameTimestampOffset);
            previousTimestamp = outputFrame.getTimestamp();
            previousTimestampValid = true;
        }

        private void waitUntilReleased(AppTextureFrame frame) {
            try {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(
                            TAG,
                            String.format(
                                    "Waiting for tex: %d width: %d height: %d",
                                    frame.getTextureName(), frame.getWidth(), frame.getHeight()));
                }
                frame.waitUntilReleased();
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(
                            TAG,



                            String.format(
                                    "Finished waiting for tex: %d width: %d height: %d",
                                    frame.getTextureName(), frame.getWidth(), frame.getHeight()));
                }
            } catch (InterruptedException ie) {
                // Someone interrupted our thread. This is not supposed to happen: we own
                // the thread, and we are not going to interrupt it. If it should somehow
                // happen that the thread is interrupted, let's set the interrupted flag
                // again, log the error, and throw a RuntimeException.
                Thread.currentThread().interrupt();
                Log.e(TAG, "thread was unexpectedly interrupted: " + ie.getMessage());
                throw new RuntimeException(ie);
            }
        }
    }
}
