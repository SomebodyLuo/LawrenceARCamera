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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sensetime.stmobileapi.STMobileFaceAction;
import com.sensetime.stmobileapi.STUtils;
import com.simoncherry.arcamera.R;
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
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.lights.PointLight;
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

public class Camera3DActivity extends AppCompatActivity implements FrameCallback {

    private final static String TAG = Camera3DActivity.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private TextView mTrackText, mActionText;
    private ImageView mIvLandmark;

    private Context mContext;
    protected TextureController mController;
    private MyRenderer mRenderer;

    private ISurface mRenderSurface;
    private ISurfaceRenderer mISurfaceRenderer;
    private Bitmap mRajawaliBitmap = null;

    private int cameraId = 1;
    protected int mCurrentFilterId = R.id.menu_camera_default;

    private static Accelerometer mAccelerometer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = Camera3DActivity.this;

        mAccelerometer = new Accelerometer(this);
        mAccelerometer.start();

        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        setContentView(R.layout.activity_cam_3d);
        mTrackText = (TextView) findViewById(R.id.tv_track);
        mActionText = (TextView) findViewById(R.id.tv_action);

        //面具、头盔
        mIvLandmark = (ImageView) findViewById(R.id.iv_landmark);

        mRenderSurface = (org.rajawali3d.view.SurfaceView) findViewById(R.id.rajwali_surface);
        ((org.rajawali3d.view.SurfaceView) mRenderSurface).setTransparent(true);
        ((org.rajawali3d.view.SurfaceView) mRenderSurface).getHolder().setFixedSize(720, 1280);

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
    }

    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {
            mController = new TextureController(mContext);
            // 设置数据源
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //初始化Render：
                //1. 显示摄像头图像
                //2. 追踪头部，并返回状态信息
                //3. 在回调中，根据上面的状态信息控制模型的状态，并渲染
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
//                        final Bitmap bitmap = handleDrawLandMark(faceActions, orientation);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                if (bitmap != null) {
//                                    mIvLandmark.setImageBitmap(bitmap);
//                                }
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

            mController.setFrameCallback(720, 1280, Camera3DActivity.this);
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
                        Toast.makeText(Camera3DActivity.this, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(720,1280, Bitmap.Config.ARGB_8888);
                ByteBuffer b = ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);

                if (mRajawaliBitmap != null) {
                    Log.i(TAG, "mRajawaliBitmap != null");
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
            }
        }).start();
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mShutter:
                //mController.takePhoto();
                ((org.rajawali3d.view.SurfaceView) mRenderSurface).takeScreenshot();
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
                    Toast.makeText(Camera3DActivity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(Camera3DActivity.this, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    int PREVIEW_WIDTH = 640;
    int PREVIEW_HEIGHT = 480;

    private Bitmap handleDrawLandMark(STMobileFaceAction[] faceActions, int orientation) {
        if(faceActions != null) {
            for(int i=0; i<faceActions.length; i++) {
                Log.i("Test", "detect faces: "+ faceActions[i].getFace().getRect().toString());
            }

            final Bitmap bitmap = Bitmap.createBitmap(PREVIEW_HEIGHT, PREVIEW_WIDTH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            boolean rotate270 = orientation == 270;
            for (STMobileFaceAction r : faceActions) {
                Log.i("Test", "-->> face count = "+faceActions.length);
                Rect rect;
                if (rotate270) {
                    rect = STUtils.RotateDeg270(r.getFace().getRect(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
                } else {
                    rect = STUtils.RotateDeg90(r.getFace().getRect(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
                }

                PointF[] points = r.getFace().getPointsArray();
                for (int i = 0; i < points.length; i++) {
                    if (rotate270) {
                        points[i] = STUtils.RotateDeg270(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    } else {
                        points[i] = STUtils.RotateDeg90(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    }
                }
                STUtils.drawFaceRect(canvas, rect, PREVIEW_HEIGHT,
                        PREVIEW_WIDTH, cameraId == 1);
                STUtils.drawPoints(canvas, points, PREVIEW_HEIGHT,
                        PREVIEW_WIDTH, cameraId == 1);
                return bitmap;
            }
        }
        return null;
    }

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

        // 计算 旋转后的脸的长方形区域的中心点坐标
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
        private DirectionalLight directionalLight;
        private PointLight pointLightLeft, pointLightMid, pointLightRight, pointLightUp;
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
//                final LoaderOBJ parser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.tiger_nose_obj);
//                parser.parse();
//                mMask = parser.getParsedObject();
//                mMask.setScale(0.002f);
//                mMask.setY(-0.2f);
//                mMask.setZ(0.4f);

                // 钢铁侠头盔
                final LoaderOBJ parser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.ironman_mask_obj);
                parser.parse();
                mMask = parser.getParsedObject();
                mMask.setScale(0.28f);
                mMask.setY(-8.2f);
//                mMask.setZ(0.0f);

                mContainer = new Object3D();
                mContainer.addChild(mMask);
                getCurrentScene().addChild(mContainer);

                // 方向光
                directionalLight = new DirectionalLight(0.0f, 0.0f, -1.0);
                directionalLight.setColor(1.0f, 1.0f, 1.0f);
                directionalLight.setPower(0.5f);
                getCurrentScene().addLight(directionalLight);

                // 三盏点光源
                pointLightLeft = new PointLight();
                pointLightLeft.setPosition(-15.0f, -8.2f, 0.0f);
                pointLightLeft.setColor(1.0f, 1.0f, 1.0f);
                pointLightLeft.setPower(8.0f);

                pointLightMid = new PointLight();
                pointLightMid.setPosition(0.0f, -8.2f, 25.0f);
                pointLightMid.setColor(1.0f, 1.0f, 1.0f);
                pointLightMid.setPower(4.0f);

                pointLightRight = new PointLight();
                pointLightRight.setPosition(15.0f, -8.2f, 0.0f);
                pointLightRight.setColor(1.0f, 1.0f, 1.0f);
                pointLightRight.setPower(8.0f);

                pointLightUp = new PointLight();
                pointLightUp.setPosition(0.0f, 7.0f, 0.0f);
                pointLightUp.setColor(1.0f, 1.0f, 1.0f);
                pointLightUp.setPower(8.0f);

                getCurrentScene().addLight(pointLightLeft);
                getCurrentScene().addLight(pointLightMid);
                getCurrentScene().addLight(pointLightRight);
                getCurrentScene().addLight(pointLightUp);

                getCurrentScene().getCamera().setZ(35.5);

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
}
