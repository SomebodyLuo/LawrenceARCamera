package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by Simon on 2017/7/6.
 */

public class Rainbow3Filter extends LandmarkFilter {

    private final static String TAG = Rainbow3Filter.class.getSimpleName();

    private int gLandmarkX;
    private int gLandmarkY;
    private int gStarPosX;
    private int gStarPosY;
    private int gMouthOpen;
    private int gGlobalTime;
    private int gRainbowHeight;

    private float[] uLandmarkX;
    private float[] uLandmarkY;
    private float[] uStarPosX;
    private float[] uStarPosY;
    private int isMouthOpen = 0;
    private float uRainbowHeight = 0.0f;

    public Rainbow3Filter(Resources mRes) {
        super(mRes);
        uLandmarkX = new float[106];
        uLandmarkY = new float[106];
        uStarPosX = new float[]{-0.05f, 0.01f, 0.06f, 0.04f, -0.08f, 0.03f, 0.1f};
        uStarPosY = new float[]{0.02f, 0.08f, 0.17f, 0.25f, 0.31f, 0.36f, 0.42f};
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/test/rainbow3_fragment.frag");

        gLandmarkX = GLES20.glGetUniformLocation(mProgram, "uLandmarkX");
        gLandmarkY = GLES20.glGetUniformLocation(mProgram, "uLandmarkY");
        gStarPosX = GLES20.glGetUniformLocation(mProgram, "uStarPosX");
        gStarPosY = GLES20.glGetUniformLocation(mProgram, "uStarPosY");
        gMouthOpen = GLES20.glGetUniformLocation(mProgram, "uMouthOpen");
        gGlobalTime = GLES20.glGetUniformLocation(mProgram, "iGlobalTime");
        gRainbowHeight = GLES20.glGetUniformLocation(mProgram, "uRainbowHeight");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }

    public void setLandmarks(float[] landmarkX, float[] landmarkY) {
        uLandmarkX = landmarkX;
        uLandmarkY = landmarkY;
    }

    public void setMouthOpen(int isOpen) {
        isMouthOpen = isOpen;
        if (isOpen == 0) {
            uRainbowHeight = 0.0f;
        }
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1fv(gLandmarkX, uLandmarkX.length, uLandmarkX, 0);
        GLES20.glUniform1fv(gLandmarkY, uLandmarkY.length, uLandmarkY, 0);
        GLES20.glUniform1fv(gStarPosX, uStarPosX.length, uStarPosX, 0);
        GLES20.glUniform1fv(gStarPosY, uStarPosY.length, uStarPosY, 0);
        GLES20.glUniform1i(gMouthOpen, isMouthOpen);

        long currentTime = System.currentTimeMillis();
        float globalTime = ((float) (currentTime - START_TIME)) / 1000.0f;
        if (globalTime >= 20.0f) {  // TODO 大于20秒后，基于时间变化描绘的图像有点奇怪（不平滑，出现锯齿），大于50秒后就非常明显
            setStartTime(currentTime);
        }
        Log.e(TAG, "gGlobalTime: " + globalTime);
        GLES20.glUniform1f(gGlobalTime, globalTime);

        if (uRainbowHeight < 1.0f) {
            uRainbowHeight += 0.2f;
        }
        Log.i(TAG, "uRainbowHeight: " + uRainbowHeight);
        GLES20.glUniform1f(gRainbowHeight, uRainbowHeight);
    }
}
