package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by Simon on 2017/7/6.
 */

public class FireEyeFilter extends LandmarkFilter {

    private final static String TAG = FireEyeFilter.class.getSimpleName();

    private int gLandmarkX;
    private int gLandmarkY;
    private int gGlobalTime;
    private float[] uLandmarkX;
    private float[] uLandmarkY;

    public FireEyeFilter(Resources mRes) {
        super(mRes);
        uLandmarkX = new float[106];
        uLandmarkY = new float[106];
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/test/fire_eye_fragment.frag");

        gLandmarkX = GLES20.glGetUniformLocation(mProgram, "uLandmarkX");
        gLandmarkY = GLES20.glGetUniformLocation(mProgram, "uLandmarkY");
        gGlobalTime = GLES20.glGetUniformLocation(mProgram, "iGlobalTime");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }

    public void setLandmarks(float[] landmarkX, float[] landmarkY) {
        uLandmarkX = landmarkX;
        uLandmarkY = landmarkY;

        Log.e(TAG, "eye_distance: " + Math.abs(uLandmarkX[104] - uLandmarkX[105]));
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1fv(gLandmarkX, uLandmarkX.length, uLandmarkX, 0);
        GLES20.glUniform1fv(gLandmarkY, uLandmarkY.length, uLandmarkY, 0);

        long currentTime = System.currentTimeMillis();
        float globalTime = ((float) (currentTime - START_TIME)) / 1000.0f;
        if (globalTime >= 5.0f) {  // TODO 大于5秒后，基于时间变化描绘的图像有点奇怪
            setStartTime(currentTime);
        }
        Log.e(TAG, "gGlobalTime: " + globalTime);
        GLES20.glUniform1f(gGlobalTime, globalTime);
    }
}
