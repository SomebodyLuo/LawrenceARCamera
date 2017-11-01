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
import com.simoncherry.arcamera.codec.CameraRecorder;
import com.simoncherry.arcamera.ui.custom.CircularProgressView;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraRecordActivity extends AppCompatActivity implements FrameCallback {

    private final static String TAG = CameraRecordActivity.class.getSimpleName();

    private Context mContext;
    private SurfaceView mSurfaceView;
    protected TextureController mController;
    private MyRenderer mRenderer;
    private int cameraId = 1;
    protected int mCurrentFilterId = R.id.menu_camera_default;

    //private ImageButton mCapture;
    private CircularProgressView mCapture;
    private CameraRecorder mp4Recorder;
    private ExecutorService mExecutor;
    private long time;
    private long maxTime = 20000;
    private long timeStep = 50;
    private boolean recordFlag = false;
    private int mFrameType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CameraRecordActivity.this;
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        //setContentView(R.layout.activity_camera);
        setContentView(R.layout.activity_cam_record);
    }

    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {
            mExecutor = Executors.newSingleThreadExecutor();

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

            mController.setFrameCallback(720, 1280, CameraRecordActivity.this);
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

            //mCapture = (ImageButton) findViewById(R.id.mShutter);
            mCapture = (CircularProgressView) findViewById(R.id.mCapture);
            mCapture.setTotal((int)maxTime);
            mCapture.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            recordFlag=false;
                            time=System.currentTimeMillis();
                            mCapture.postDelayed(captureTouchRunnable,500);
                            break;
                        case MotionEvent.ACTION_UP:
                            recordFlag = false;
                            if(System.currentTimeMillis()-time<500){
                                mFrameType = 0;
                                mCapture.removeCallbacks(captureTouchRunnable);
                                mController.setFrameCallback(720, 1280, CameraRecordActivity.this);
                                mController.takePhoto();
                            }
                            break;
                    }
                    return false;
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
                        Toast.makeText(mContext, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
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
        if (mp4Recorder != null && mFrameType == 1) {
            mp4Recorder.feedData(bytes, time);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = Bitmap.createBitmap(720,1280, Bitmap.Config.ARGB_8888);
                    ByteBuffer b = ByteBuffer.wrap(bytes);
                    bitmap.copyPixelsFromBuffer(b);
                    saveBitmap(bitmap);
                    bitmap.recycle();
                }
            }).start();
        }
    }


    public void onClick(View view){
        switch (view.getId()){
            case R.id.mShutter:
                //mController.takePhoto();
                break;
        }
    }

    protected String getSD(){
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    //图片保存
    public void saveBitmap(Bitmap b){
        String path =  getSD()+ "/Codec/photo/";
        File folder=new File(path);
        if(!folder.exists()&&!folder.mkdirs()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "无法保存照片", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(mContext, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //录像的Runnable
    private Runnable captureTouchRunnable=new Runnable() {
        @Override
        public void run() {
            recordFlag=true;
            mExecutor.execute(recordRunnable);
        }
    };

    private Runnable recordRunnable=new Runnable() {

        @Override
        public void run() {
            mFrameType = 1;
            long timeCount = 0;
            if(mp4Recorder == null){
                mp4Recorder = new CameraRecorder();
            }
            long time = System.currentTimeMillis();
            String savePath = getPath("video/", time + ".mp4");
            mp4Recorder.setSavePath(getPath("video/", time+""), "mp4");
            try {
                mp4Recorder.prepare(384, 640);
                mp4Recorder.start();
                mController.setFrameCallback(384, 640, CameraRecordActivity.this);
                mController.startRecord();
                while (timeCount <= maxTime && recordFlag){
                    long start = System.currentTimeMillis();
                    mCapture.setProcess((int)timeCount);
                    long end = System.currentTimeMillis();
                    try {
                        Thread.sleep(timeStep - (end - start));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timeCount += timeStep;
                }
                mController.stopRecord();

                if(timeCount < 2000){
                    mp4Recorder.cancel();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //mCapture.setProcess(0);
                            Toast.makeText(mContext, "录像时间太短了", Toast.LENGTH_SHORT).show();
                        }
                    });
                }else{
                    mp4Recorder.stop();
                    recordComplete(mFrameType, savePath);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private String getBaseFolder(){
        String baseFolder=Environment.getExternalStorageDirectory()+"/Codec/";
        File f=new File(baseFolder);
        if(!f.exists()){
            boolean b=f.mkdirs();
            if(!b){
                baseFolder=getExternalFilesDir(null).getAbsolutePath()+"/";
            }
        }
        return baseFolder;
    }

    //获取VideoPath
    private String getPath(String path,String fileName){
        String p= getBaseFolder()+path;
        File f=new File(p);
        if(!f.exists()&&!f.mkdirs()){
            return getBaseFolder()+fileName;
        }
        return p+fileName;
    }

    private void recordComplete(int type, final String path){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCapture.setProcess(0);
                Toast.makeText(mContext,"文件保存路径："+path,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
