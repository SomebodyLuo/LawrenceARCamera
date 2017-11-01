package com.simoncherry.arcamera.ui.activity;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sensetime.stmobileapi.STMobileFaceAction;
import com.sensetime.stmobileapi.STUtils;
import com.simoncherry.arcamera.R;
import com.simoncherry.arcamera.codec.CameraRecorder;
import com.simoncherry.arcamera.ui.custom.CircularProgressView;
import com.simoncherry.arcamera.filter.camera.AFilter;
import com.simoncherry.arcamera.filter.camera.FilterFactory;
import com.simoncherry.arcamera.filter.camera.LandmarkFilter;
import com.simoncherry.arcamera.gl.Camera1Renderer;
import com.simoncherry.arcamera.gl.CameraTrackRenderer;
import com.simoncherry.arcamera.gl.FrameCallback;
import com.simoncherry.arcamera.gl.MyRenderer;
import com.simoncherry.arcamera.gl.TextureController;
import com.simoncherry.arcamera.util.Accelerometer;
import com.simoncherry.arcamera.util.PermissionUtils;

import org.rajawali3d.Object3D;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.ISurfaceRenderer;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.ISurface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Record3DActivity extends AppCompatActivity implements FrameCallback {

    private final static String TAG = Record3DActivity.class.getSimpleName();
    private final static int IMAGE_WIDTH = 720;
    private final static int IMAGE_HEIGHT = 1280;
    private final static int VIDEO_WIDTH = 384;
    private final static int VIDEO_HEIGHT = 640;

    private SurfaceView mSurfaceView;
    private TextView mTrackText, mActionText;

    private Context mContext;
    protected TextureController mController;
    private MyRenderer mRenderer;

    private ISurface mRenderSurface;
    private ISurfaceRenderer mISurfaceRenderer;
    private Bitmap mRajawaliBitmap = null;
    private int[] mRajawaliPixels = null;

    private int cameraId = 1;
    protected int mCurrentFilterId = R.id.menu_camera_default;

    private static Accelerometer mAccelerometer;

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
        mContext = Record3DActivity.this;

        mAccelerometer = new Accelerometer(this);
        mAccelerometer.start();

        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        setContentView(R.layout.activity_record_3d);
        mTrackText = (TextView) findViewById(R.id.tv_track);
        mActionText = (TextView) findViewById(R.id.tv_action);
        mCapture = (CircularProgressView) findViewById(R.id.mCapture);

        mRenderSurface = (org.rajawali3d.view.SurfaceView) findViewById(R.id.rajwali_surface);
        ((org.rajawali3d.view.SurfaceView) mRenderSurface).setTransparent(true);
        ((org.rajawali3d.view.SurfaceView) mRenderSurface).getHolder().setFixedSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mISurfaceRenderer = new My3DRenderer(this);
        mRenderSurface.setSurfaceRenderer(mISurfaceRenderer);
        ((View) mRenderSurface).bringToFront();

        ((org.rajawali3d.view.SurfaceView) mRenderSurface).setOnTakeScreenshotListener(new org.rajawali3d.view.SurfaceView.OnTakeScreenshotListener() {
            @Override
            public void onTakeScreenshot(Bitmap bitmap) {
                Log.e(TAG, "onTakeScreenshot(Bitmap bitmap)");
                mRajawaliBitmap = bitmap;
                mController.takePhoto();
            }
        });

        ((org.rajawali3d.view.SurfaceView) mRenderSurface).setOnTakeScreenshotListener2(new org.rajawali3d.view.SurfaceView.OnTakeScreenshotListener2() {
            @Override
            public void onTakeScreenshot(int[] pixels) {
                Log.e(TAG, "onTakeScreenshot(byte[] pixels)");
                mRajawaliPixels = pixels;
            }
        });

        mCapture.setTotal((int)maxTime);
        mCapture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        recordFlag=false;
                        time=System.currentTimeMillis();
                        mCapture.postDelayed(captureTouchRunnable, 500);
                        break;
                    case MotionEvent.ACTION_UP:
                        recordFlag = false;
                        if(System.currentTimeMillis()-time<500){
                            mFrameType = 0;
                            mCapture.removeCallbacks(captureTouchRunnable);
                            mController.setFrameCallback(IMAGE_WIDTH, IMAGE_HEIGHT, Record3DActivity.this);
                            //mController.takePhoto();
                            ((org.rajawali3d.view.SurfaceView) mRenderSurface).takeScreenshot();
                        }
                        break;
                }
                return false;
            }
        });
    }

    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {
            mExecutor = Executors.newSingleThreadExecutor();

            mController = new TextureController(mContext);
            // 设置数据源
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRenderer = new CameraTrackRenderer(mContext, (CameraManager)getSystemService(CAMERA_SERVICE), mController, cameraId);
                ((CameraTrackRenderer) mRenderer).setTrackCallBackListener(new CameraTrackRenderer.TrackCallBackListener() {
                    @Override
                    public void onTrackDetected(STMobileFaceAction[] faceActions, final int orientation, final int value,
                                                final float pitch, final float roll, final float yaw,
                                                final int eye_dist, final int id, final int eyeBlink, final int mouthAh,
                                                final int headYaw, final int headPitch, final int browJump) {
                        handle3dModelRotation(pitch, roll, yaw);
                        handle3dModelTransition(faceActions, orientation, eye_dist, yaw);
                        setLandmarkFilter(faceActions, orientation, mouthAh);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTrackText.setText("TRACK: " + value + " MS"
                                        + "\nPITCH: " + pitch + "\nROLL: " + roll + "\nYAW: " + yaw + "\nEYE_DIST:" + eye_dist);
                                mActionText.setText("ID:" + id + "\nEYE_BLINK:" + eyeBlink + "\nMOUTH_AH:"
                                        + mouthAh + "\nHEAD_YAW:" + headYaw + "\nHEAD_PITCH:" + headPitch + "\nBROW_JUMP:" + browJump);
                            }
                        });
                    }
                });

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

            mController.setFrameCallback(IMAGE_WIDTH, IMAGE_HEIGHT, Record3DActivity.this);
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
                        Toast.makeText(Record3DActivity.this, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
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
            if (mRajawaliPixels != null) {
                final ByteBuffer buf = ByteBuffer.allocate(mRajawaliPixels.length * 4)
                        .order(ByteOrder.LITTLE_ENDIAN);
                buf.asIntBuffer().put(mRajawaliPixels);
                mRajawaliPixels = null;
                byte[] tmpArray = buf.array();
                for (int i=0; i<bytes.length; i+=4) {
                    byte a = tmpArray[i];
                    byte r = tmpArray[i+1];
                    byte g = tmpArray[i+2];
                    byte b = tmpArray[i+3];
                    boolean isBackground = r == 0 && g == 0 && b == 0 && a == 0;
                    if (!isBackground) {
                        bytes[i] = a;
                        bytes[i + 1] = r;
                        bytes[i + 2] = g;
                        bytes[i + 3] = b;
                    }
                }
            }
            mp4Recorder.feedData(bytes, time);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH,IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                    ByteBuffer b = ByteBuffer.wrap(bytes);
                    bitmap.copyPixelsFromBuffer(b);

                    if (mRajawaliBitmap != null) {
                        Log.i(TAG, "mRajawaliBitmap != null");
                        mRajawaliBitmap = Bitmap.createScaledBitmap(mRajawaliBitmap, IMAGE_WIDTH, IMAGE_HEIGHT, false);
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawBitmap(mRajawaliBitmap, 0, 0, null);
                        canvas.save(Canvas.ALL_SAVE_FLAG);
                        canvas.restore();
                        mRajawaliBitmap.recycle();
                        mRajawaliBitmap = null;
                    } else {
                        Log.i(TAG, "mRajawaliBitmap == null");
                    }

                    saveBitmap(bitmap);
                    bitmap.recycle();
                    bitmap = null;
                }
            }).start();
        }
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mShutter:
                //mController.takePhoto();
                //((org.rajawali3d.view.SurfaceView) mRenderSurface).takeScreenshot();
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
                    Toast.makeText(Record3DActivity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(Record3DActivity.this, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final static int PREVIEW_WIDTH = 640;
    private final static int PREVIEW_HEIGHT = 480;

    private void setLandmarkFilter(STMobileFaceAction[] faceActions, int orientation, int mouthAh) {
        AFilter aFilter = mController.getLastFilter();
        if(aFilter != null && aFilter instanceof LandmarkFilter && faceActions != null) {
            for(int i=0; i<faceActions.length; i++) {
                Log.i("Test", "detect faces: "+ faceActions[i].getFace().getRect().toString());
            }

            boolean rotate270 = orientation == 270;
            for (STMobileFaceAction r : faceActions) {
                Log.i("Test", "-->> face count = "+faceActions.length);
                PointF[] points = r.getFace().getPointsArray();
                float[] landmarkX = new float[points.length];
                float[] landmarkY = new float[points.length];
                for (int i = 0; i < points.length; i++) {
                    if (rotate270) {
                        points[i] = STUtils.RotateDeg270(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    } else {
                        points[i] = STUtils.RotateDeg90(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    }
//                    Log.e("Test", "-->> face landmark [" + i + "] : " + points[i]);
//                    landmarkX[i] = points[i].x;
//                    landmarkY[i] = points[i].y;

                    landmarkX[i] = 1 - points[i].x / 480.0f;
                    landmarkY[i] = points[i].y / 640.0f;
                }
                ((LandmarkFilter) aFilter).setLandmarks(landmarkX, landmarkY);
                ((LandmarkFilter) aFilter).setMouthOpen(mouthAh);
            }
        }
    }

    private void handle3dModelRotation(final float pitch, final float roll, final float yaw) {
        ((My3DRenderer) mISurfaceRenderer).setAccelerometerValues(roll+90, -yaw, -pitch);
    }

    private void handle3dModelTransition(STMobileFaceAction[] faceActions, int orientation, int eye_dist, float yaw) {
        boolean rotate270 = orientation == 270;
        STMobileFaceAction r = faceActions[0];
        Rect rect;
        if (rotate270) {
            rect = STUtils.RotateDeg270(r.getFace().getRect(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
        } else {
            rect = STUtils.RotateDeg90(r.getFace().getRect(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }
        //Log.i(TAG, "rect center: (" + String.valueOf((rect.right + rect.left)/2) + ", " + String.valueOf((rect.bottom + rect.top)/2) + ")");

        float centerX = (rect.right + rect.left)/2.0f;
        float centerY = (rect.bottom + rect.top)/2.0f;
        float x = (centerX / PREVIEW_HEIGHT) * 2.0f - 1.0f;
        float y = (centerY / PREVIEW_WIDTH) * 2.0f - 1.0f;
        //float tmp = (eye_dist * 0.000001f - 1115) * 0.04f;   // 1115xxxxxx ~ 1140xxxxxx - > 0 ~ 25 -> 0 ~ 1
        float tmp = eye_dist * 0.000001f - 1115;  // 1115xxxxxx ~ 1140xxxxxx - > 0 ~ 25
        tmp = (float) (tmp / Math.cos(Math.PI*yaw/180));  // 根据旋转角度还原两眼距离
        tmp = tmp * 0.04f;  // 0 ~ 25 -> 0 ~ 1
        float z = tmp * 3.0f + 1.0f;
        Log.e(TAG, "transition: x= " + x + ", y= " + y + ", z= " + z);

        My3DRenderer renderer = ((My3DRenderer) mISurfaceRenderer);
        renderer.getCurrentCamera().setX(x);
        renderer.getCurrentCamera().setY(y);
        renderer.setScale(z);
    }

    private class My3DRenderer extends Renderer {
        private Object3D mContainer;
        private Object3D mMask;
        private Vector3 mAccValues;
        private float mScale = 1.0f;

        My3DRenderer(Context context) {
            super(context);
            mAccValues = new Vector3();
        }

        @Override
        protected void initScene() {
            try {
                // V字仇杀队面具
//                final LoaderOBJ parser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.v_mask_obj);
//                parser.parse();
//                mMask = parser.getParsedObject();
//                mMask.setScale(0.15f);
//                mMask.setY(0.01f);  // 上正下负
//                mMask.getMaterial().enableLighting(false);
                // 老虎鼻子
                final LoaderOBJ parser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.tiger_nose_obj);
                parser.parse();
                mMask = parser.getParsedObject();
                mMask.setScale(0.002f);
                mMask.setY(-0.2f);
                mMask.setZ(0.4f);

                mContainer = new Object3D();
                mContainer.addChild(mMask);
                getCurrentScene().addChild(mContainer);

                getCurrentScene().getCamera().setZ(5.5);

            } catch (ParsingException e) {
                e.printStackTrace();
            }

            getCurrentScene().setBackgroundColor(0);
        }

        @Override
        protected void onRender(long ellapsedRealtime, double deltaTime) {
            super.onRender(ellapsedRealtime, deltaTime);
            mContainer.setRotation(mAccValues.x, mAccValues.y, mAccValues.z);
            mContainer.setScale(mScale);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
        }

        void setAccelerometerValues(float x, float y, float z) {
            mAccValues.setAll(x, y, z);
        }

        void setScale(float scale) {
            mScale = scale;
        }
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
                mp4Recorder.prepare(VIDEO_WIDTH, VIDEO_HEIGHT);
                mp4Recorder.start();
                mController.setFrameCallback(VIDEO_WIDTH, VIDEO_HEIGHT, Record3DActivity.this);
                mController.startRecord();
                ((org.rajawali3d.view.SurfaceView) mRenderSurface).startRecord();

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
                ((org.rajawali3d.view.SurfaceView) mRenderSurface).stopRecord();

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
        String baseFolder=Environment.getExternalStorageDirectory()+"/OpenGLDemo/";
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
