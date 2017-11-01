package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * Created by Simon on 2017/7/6.
 */

public class MagFilter extends AFilter {

    private int glHUxy;
    private int gScale;
    private float uXY;
    private float uScale;

    public MagFilter(Resources mRes) {
        super(mRes);
        setFlag(3);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/color/mag_fragment.frag");

        glHUxy = GLES20.glGetUniformLocation(mProgram, "uXY");
        gScale = GLES20.glGetUniformLocation(mProgram, "uScale");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        uXY = width / (float) height;
    }

    @Override
    protected void onDraw() {
        GLES20.glUniform1f(glHUxy, uXY);
        GLES20.glEnableVertexAttribArray(mHPosition);
        GLES20.glVertexAttribPointer(mHPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(mHCoord);
        GLES20.glVertexAttribPointer(mHCoord, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mHPosition);
        GLES20.glDisableVertexAttribArray(mHCoord);
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
        uScale = flag * 0.1f + 0.2f;
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(gScale, uScale);
    }
}
