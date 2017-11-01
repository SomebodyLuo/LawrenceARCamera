package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;

/**
 * Created by Simon on 2017/7/6.
 */

public class DrawPointFilter extends AFilter {

    public DrawPointFilter(Resources mRes) {
        super(mRes);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/base_vertex.sh",
                "shader/test/draw_point_fragment.frag");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
    }
}
