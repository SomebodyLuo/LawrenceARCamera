package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * Created by Simon on 2017/7/6.
 */

public class FlushFilter extends LandmarkFilter {

    private int gLandmarkX;
    private int gLandmarkY;
    private float[] uLandmarkX;
    private float[] uLandmarkY;

    public FlushFilter(Resources mRes) {
        super(mRes);
        uLandmarkX = new float[106];
        uLandmarkY = new float[106];
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/test/flush_fragment.frag");

        gLandmarkX = GLES20.glGetUniformLocation(mProgram, "uLandmarkX");
        gLandmarkY = GLES20.glGetUniformLocation(mProgram, "uLandmarkY");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }

    public void setLandmarks(float[] landmarkX, float[] landmarkY) {
        uLandmarkX = landmarkX;
        uLandmarkY = landmarkY;
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1fv(gLandmarkX, uLandmarkX.length, uLandmarkX, 0);
        GLES20.glUniform1fv(gLandmarkY, uLandmarkY.length, uLandmarkY, 0);
    }
}
