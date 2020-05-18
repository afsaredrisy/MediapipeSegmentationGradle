package co.introtuce.nex2me.demo.analytics.accelerate.utils;

import android.graphics.SurfaceTexture;

public class CustomSurfaceTexture extends SurfaceTexture {
    public CustomSurfaceTexture(int texName) {
        super(texName);
        init();
    }

    private void init() {
        super.detachFromGLContext();
    }
}
