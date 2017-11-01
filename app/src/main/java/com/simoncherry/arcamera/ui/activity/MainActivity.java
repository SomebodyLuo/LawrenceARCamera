package com.simoncherry.arcamera.ui.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.simoncherry.arcamera.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 请点这个
        findViewById(R.id.btn_ar_cam).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ARCamActivity.class);
            }
        });
        // 下面这些页面全是测试用的
        findViewById(R.id.btn_swap_face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(SwapFaceActivity.class);
            }
        });

        findViewById(R.id.btn_image_renderer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ImageActivity.class);
            }
        });

        findViewById(R.id.btn_camera_renderer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CameraActivity.class);
            }
        });

        findViewById(R.id.btn_cam_adjust).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CamAdjustActivity.class);
            }
        });

        findViewById(R.id.btn_faceu_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CameraTrackActivity.class);
            }
        });

        findViewById(R.id.btn_cam_3d).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(Camera3DActivity.class);
            }
        });

        findViewById(R.id.btn_cam_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CameraRecordActivity.class);
            }
        });

        findViewById(R.id.btn_record_3d).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(Record3DActivity.class);
            }
        });

        findViewById(R.id.btn_dynamic_model).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(DynamicModel3Activity.class);
            }
        });
    }

    private void startActivity(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }
}
