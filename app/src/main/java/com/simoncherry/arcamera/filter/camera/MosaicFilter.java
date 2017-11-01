package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * Created by Simon on 2017/7/6.
 */

public class MosaicFilter extends AFilter {

    private int gMosaicSize;
    private float mSizeX;
    private float mSizeY;

    public MosaicFilter(Resources mRes) {
        super(mRes);
        setFlag(3);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/color/mosaic_fragment.frag");

        gMosaicSize = GLES20.glGetUniformLocation(mProgram, "uMosaicSize");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
        setParams((flag + 1) * 2.0f);  // flag取中间值3时，size取8.0f
    }

    private void setParams(float value) {
        mSizeX = value;
        mSizeY = value;
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform2f(gMosaicSize, mSizeX, mSizeY);
    }
}
