/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.introtuce.mediapipesegmentationgradle.videoviews;

import android.content.Context;
import android.view.MotionEvent;

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
public class MyGLSurfaceView extends RecordableSurfaceView implements RecordableSurfaceView.RendererCallbacks {

    private final MyGLRenderer mRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer();
        mRenderer.setContext(context);
        setRendererCallbacks(this);
    }

    public void setSourceTexture(AutoFitTextureView sourceTexture){
        mRenderer.setSourceTexture(sourceTexture);
    }
    public void pauseRender(){
        mRenderer.pauseRendering();
    }
    public void resumeRenderer(){
        mRenderer.resumeRendring();
    }
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    public StringBuilder getGPUInfo(){
        if(mRenderer!=null){
            return  mRenderer.getGpuInfo();
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        /*
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1 ;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1 ;
                }

                mRenderer.setAngle(
                        mRenderer.getAngle() +
                        ((dx + dy) * TOUCH_SCALE_FACTOR));  // = 180.0f / 320
                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;*/

        super.onTouchEvent(e);

        return true;
    }

    /*public void setWaterMark(WatermarkImage waterMark){
        mRenderer.setWatermarkImage(waterMark);
    }*/

    @Override
    public void onSurfaceCreated() {
        mRenderer.onSurfaceCreated(null, null);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mRenderer.onSurfaceChanged(null, width, height);
    }

    @Override
    public void onSurfaceDestroyed() {

    }

    @Override
    public void onContextCreated() {

    }

    @Override
    public void onPreDrawFrame() {

    }

    @Override
    public void onDrawFrame() {
        mRenderer.onDrawFrame(null);
    }
}
