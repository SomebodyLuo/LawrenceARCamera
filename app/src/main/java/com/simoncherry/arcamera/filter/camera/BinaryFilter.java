package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * Created by Simon on 2017/7/6.
 */

public class BinaryFilter extends AFilter {

    private int gThreshold;
    private float mThreshold;

    public BinaryFilter(Resources mRes) {
        super(mRes);
        setFlag(3);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/color/binary_fragment.frag");

        gThreshold = GLES20.glGetUniformLocation(mProgram, "uThreshold");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
        mThreshold = (flag + 2) / 10f;  // flag取中间值3时，threshold取0.5f
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(gThreshold, mThreshold);
    }
}
