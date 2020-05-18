package co.introtuce.nex2me.demo.analytics.accelerate.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.mediapipe.components.CameraHelper;
import java.io.File;
import java.util.Arrays;

import javax.annotation.Nullable;



public class Camera2Helper extends CameraHelper {

    public static final String TAG="Camera2Helper";
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;

    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private SurfaceTexture outputSurface;
    private Size frameSize;
    private int frameRotation;
    private CameraHelper.CameraFacing cameraFacing;
    private Context context;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public Camera2Helper(Context context, int frameRotation){
        this.context = context;
        this.frameRotation = frameRotation;

    }
    public Camera2Helper(Context context, SurfaceTexture surfaceTexture){
        this.context = context;
        this.outputSurface = surfaceTexture;
    }



    @Override
    public void startCamera(Activity context, CameraFacing cameraFacing, @Nullable SurfaceTexture surfaceTexture) {
        this.cameraFacing =cameraFacing;
        closeCamera();
        startBackgroundThread();
        openCamera();

    }

    @Override
    public Size computeDisplaySizeFromViewSize(Size viewSize) {
        if (viewSize == null || frameSize == null) {
            // Wait for all inputs before setting display size.
            Log.d(TAG, "viewSize or frameSize is null.");
            return null;
        }

        // Valid rotation values are 0, 90, 180 and 270.
        // Frames are rotated relative to the device's "natural" landscape orientation. When in portrait
        // mode, valid rotation values are 90 or 270, and the width/height should be swapped to
        // calculate aspect ratio.
        float frameAspectRatio = frameRotation == 0 || frameRotation == 180? frameSize.getHeight() / (float) frameSize.getWidth(): frameSize.getWidth() / (float) frameSize.getHeight();

        float viewAspectRatio = viewSize.getWidth() / (float) viewSize.getHeight();

        // Match shortest sides together.
        int scaledWidth;
        int scaledHeight;
        if (frameAspectRatio < viewAspectRatio) {
            scaledWidth = viewSize.getWidth();
            scaledHeight = Math.round(viewSize.getWidth() / frameAspectRatio);
        } else {
            scaledHeight = viewSize.getHeight();
            scaledWidth = Math.round(viewSize.getHeight() * frameAspectRatio);
        }

        return new Size(scaledWidth, scaledHeight);
    }

    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(context, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    private void closeCamera() {
        try{
            stopBackgroundThread();
            Log.d(TAG,"Closing camera ");
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }

    }

    String new_cam_id;
    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if(cameraId == null){
                cameraId = manager.getCameraIdList()[1];
            }
            Log.d(TAG,"CameraListSiize "+manager.getCameraIdList().length);
            if(cameraFacing==CameraFacing.BACK ){
                cameraId = manager.getCameraIdList()[0];
                new_cam_id = manager.getCameraIdList()[0];
                //CameraCharacteristics.LENS_FACING_BACK+"";
                Log.d(TAG,"Opening front cameraa.............................");
            }
            else{
                cameraId = manager.getCameraIdList()[1];//CameraCharacteristics.LENS_FACING_BACK+"";
                Log.d(TAG,"Opening back cameraa.............................");
                new_cam_id = manager.getCameraIdList()[1];
            }
            Log.e(TAG, "is camera open at 93 ");
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(manager.getCameraIdList()[0]);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                Log.d(TAG,"Permission issue");
                return;
            }
            Log.d(TAG,"Opening camera from manager "+cameraId);
            manager.openCamera(manager.getCameraIdList()[0], stateCallback, null);
            //fetchFrame();
            /*if(previewDisplayView!=null){
                previewDisplayView.setVisibility(View.VISIBLE);
            }*/
           /* if(outputTextureView!=null){
                outputTextureView.setVisibility(View.VISIBLE);
                initRecorder();
            }*/
            Log.d(TAG,"Camera debug start 113");

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG,e.toString());
        }
        Log.e(TAG, "openCamera X");
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
            if(onCameraStartedListener!=null){
                onCameraStartedListener.onCameraStarted(outputSurface);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            try{
                Log.d(TAG," Error on CameraDevice ");
                cameraDevice.close();
                cameraDevice = null;
            }catch (Exception e){
                Log.d(TAG,"ERROR: "+e.toString()+" ER "+error);
                e.printStackTrace();
            }
        }
    };
    private File file;
    //private CameraCaptureSession cameraCaptureSession;
    // @RequiresApi(api = Build.VERSION_CODES.O)
    protected void createCameraPreview() {
        try {
            Log.d(TAG,"Creating camera preview");
            outputSurface = (outputSurface==null)?new CustomSurfaceTexture(0):outputSurface;

            SurfaceTexture texture = outputSurface;//textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            frameSize = imageDimension;
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Log.d("CAMERA_DEBUG","Rotation "+frameRotation);

            //captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON);
            //captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(context, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.d(TAG,e.toString());
        }
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(frameRotation));
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isCameraRotated(){
        return false;
    }
}
