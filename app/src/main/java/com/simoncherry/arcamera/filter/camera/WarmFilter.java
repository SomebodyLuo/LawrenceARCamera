package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * Created by Simon on 2017/7/6.
 */

public class WarmFilter extends AFilter {

    private int gLevel;
    private float mLevel;

    public WarmFilter(Resources mRes) {
        super(mRes);
        setFlag(3);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/color/warm_fragment.frag");

        gLevel = GLES20.glGetUniformLocation(mProgram, "uLevel");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
        mLevel = (flag + 2) * 0.05f;
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform3f(gLevel, mLevel, mLevel, 0.0f);
    }
}
