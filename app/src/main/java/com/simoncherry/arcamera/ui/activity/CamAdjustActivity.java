package com.simoncherry.arcamera.ui.activity;

import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.widget.SeekBar;

import com.simoncherry.arcamera.R;
import com.simoncherry.arcamera.filter.camera.AFilter;

public class CamAdjustActivity extends CameraActivity {

    private AppCompatSeekBar mSeek;

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_cam_adjust);
        mSeek = (AppCompatSeekBar) findViewById(R.id.mSeek);
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.e("wuwang","process:" + progress);
                AFilter aFilter = mController.getLastFilter();
                aFilter.setFlag(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
