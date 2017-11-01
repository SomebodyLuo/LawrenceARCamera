package com.simoncherry.arcamera.ui.activity;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.simoncherry.arcamera.R;
import com.simoncherry.arcamera.filter.camera.BinaryFilter;
import com.simoncherry.arcamera.filter.camera.FilterFactory;
import com.simoncherry.arcamera.gl.Camera1Renderer;
import com.simoncherry.arcamera.gl.Camera2Renderer;
import com.simoncherry.arcamera.gl.FrameCallback;
import com.simoncherry.arcamera.gl.MyRenderer;
import com.simoncherry.arcamera.gl.TextureController;
import com.simoncherry.arcamera.util.PermissionUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity implements FrameCallback {
    private Context mContext;
    private SurfaceView mSurfaceView;
    protected TextureController mController;
    private MyRenderer mRenderer;
    private int cameraId = 1;
    protected int mCurrentFilterId = R.id.menu_camera_default;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CameraActivity.this;
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        setContentView(R.layout.activity_camera);
    }

    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {
            mController = new TextureController(mContext);
            // 设置数据源
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRenderer = new Camera2Renderer((CameraManager)getSystemService(CAMERA_SERVICE), mController, cameraId);
            }else{
                mRenderer = new Camera1Renderer(mController, cameraId);
            }
            setContentView();
            mSurfaceView = (SurfaceView) findViewById(R.id.mSurface);
            mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mController.clearFilter();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            mController.addFilter(FilterFactory.getFilter(getResources(), mCurrentFilterId));
                            break;
                    }
                    return true;
                }
            });

//            onFilterSet(mController);
            mController.setFrameCallback(720, 1280, CameraActivity.this);
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mController.surfaceCreated(holder);
                    mController.setRenderer(mRenderer);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    mController.surfaceChanged(width, height);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mController.surfaceDestroyed();
                }
            });
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode == 10, grantResults, initViewRunnable,
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraActivity.this, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_camera_filter, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mCurrentFilterId = item.getItemId();
        if (mCurrentFilterId == R.id.menu_camera_switch) {
            switchCamera();
        } else {
            setSingleFilter(mController, mCurrentFilterId);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onFilterSet(TextureController controller) {
//        GrayFilter grayFilter = new GrayFilter(getResources());
//        controller.addFilter(grayFilter);
        BinaryFilter binaryFilter = new BinaryFilter(getResources());
        controller.addFilter(binaryFilter);
    }

    private void setSingleFilter(TextureController controller, int menuId) {
        controller.clearFilter();
        controller.addFilter(FilterFactory.getFilter(getResources(), menuId));
    }

    public void switchCamera(){
        cameraId = cameraId == 1 ? 0 : 1;
        if (mController != null) {
            mController.destroy();
        }
        initViewRunnable.run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mController != null) {
            mController.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mController != null) {
            mController.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mController != null) {
            mController.destroy();
        }
    }

    @Override
    public void onFrame(final byte[] bytes, long time) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap=Bitmap.createBitmap(720,1280, Bitmap.Config.ARGB_8888);
                ByteBuffer b=ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);
                saveBitmap(bitmap);
                bitmap.recycle();
            }
        }).start();
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mShutter:
                mController.takePhoto();
                break;
        }
    }

    protected String getSD(){
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    //图片保存
    public void saveBitmap(Bitmap b){
        String path =  getSD()+ "/OpenGLDemo/photo/";
        File folder=new File(path);
        if(!folder.exists()&&!folder.mkdirs()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CameraActivity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        long dataTake = System.currentTimeMillis();
        final String jpegName=path+ dataTake +".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
