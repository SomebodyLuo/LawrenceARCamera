package com.simoncherry.arcamera.ui.activity;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
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
import com.simoncherry.arcamera.filter.camera.AFilter;
import com.simoncherry.arcamera.filter.camera.FilterFactory;
import com.simoncherry.arcamera.filter.camera.LandmarkFilter;
import com.simoncherry.arcamera.gl.Camera1Renderer;
import com.simoncherry.arcamera.gl.CameraTrackRenderer;
import com.simoncherry.arcamera.gl.FrameCallback;
import com.simoncherry.arcamera.gl.MyRenderer;
import com.simoncherry.arcamera.gl.TextureController;
import com.simoncherry.arcamera.model.DynamicPoint;
import com.simoncherry.arcamera.util.Accelerometer;
import com.simoncherry.arcamera.util.PermissionUtils;

import org.rajawali3d.Geometry3D;
import org.rajawali3d.Object3D;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.renderer.ISurfaceRenderer;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.ISurface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class DynamicModel2Activity extends AppCompatActivity implements FrameCallback {

    private final static String TAG = DynamicModel2Activity.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private TextView mTrackText, mActionText;

    private Context mContext;
    protected TextureController mController;
    private MyRenderer mRenderer;

    private ISurface mRenderSurface;
    private ISurfaceRenderer mISurfaceRenderer;
    private Bitmap mRajawaliBitmap = null;

    private int cameraId = 1;
    protected int mCurrentFilterId = R.id.menu_camera_default;

    private static Accelerometer mAccelerometer;

    private List<DynamicPoint> mDynamicPoints = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = DynamicModel2Activity.this;

        mAccelerometer = new Accelerometer(this);
        mAccelerometer.start();

        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        setContentView(R.layout.activity_cam_3d);
        mTrackText = (TextView) findViewById(R.id.tv_track);
        mActionText = (TextView) findViewById(R.id.tv_action);

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
                mRenderer = new CameraTrackRenderer(mContext, (CameraManager)getSystemService(CAMERA_SERVICE), mController, cameraId);
                ((CameraTrackRenderer) mRenderer).setTrackCallBackListener(new CameraTrackRenderer.TrackCallBackListener() {
                    @Override
                    public void onTrackDetected(STMobileFaceAction[] faceActions, final int orientation, final int value,
                                                final float pitch, final float roll, final float yaw,
                                                final int eye_dist, final int id, final int eyeBlink, final int mouthAh,
                                                final int headYaw, final int headPitch, final int browJump) {
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

            mController.setFrameCallback(720, 1280, DynamicModel2Activity.this);
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
                        Toast.makeText(DynamicModel2Activity.this, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(DynamicModel2Activity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(DynamicModel2Activity.this, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    int PREVIEW_WIDTH = 640;
    int PREVIEW_HEIGHT = 480;

    private void setLandmarkFilter(STMobileFaceAction[] faceActions, int orientation, int mouthAh) {
        AFilter aFilter = mController.getLastFilter();
        if(faceActions != null) {
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
                    landmarkX[i] = 1 - points[i].x / 480.0f;
                    landmarkY[i] = points[i].y / 640.0f;
                }
                if (aFilter != null && aFilter instanceof LandmarkFilter) {
                    ((LandmarkFilter) aFilter).setLandmarks(landmarkX, landmarkY);
                    ((LandmarkFilter) aFilter).setMouthOpen(mouthAh);
                }

                float[] copyLandmarkX = new float[landmarkX.length];
                float[] copyLandmarkY = new float[landmarkY.length];
                System.arraycopy(landmarkX, 0, copyLandmarkX, 0, landmarkX.length);
                System.arraycopy(landmarkY, 0, copyLandmarkY, 0, landmarkY.length);
                handleChangeModel(copyLandmarkX, copyLandmarkY);
            }
        }
    }

    private void handleChangeModel(float[] landmarkX, float[] landmarkY) {
        mDynamicPoints.clear();

        int length = landmarkX.length;
        for (int i=0; i<length; i++) {
            landmarkX[i] = (landmarkX[i] * 2f - 1f) * 6.25f;
            landmarkY[i] = ((1-landmarkY[i]) * 2f - 1f) * 8.3f;
        }
        // 额头
        mDynamicPoints.add(new DynamicPoint(30, landmarkX[41], landmarkY[41], 0.0f));
        mDynamicPoints.add(new DynamicPoint(16, landmarkX[39], landmarkY[39], 0.0f));
        mDynamicPoints.add(new DynamicPoint(21, (landmarkX[36] + landmarkX[39])*0.5f, (landmarkY[36] + landmarkY[39])*0.5f, 0.0f));
        mDynamicPoints.add(new DynamicPoint(11, landmarkX[36], landmarkY[36], 0.0f));
        mDynamicPoints.add(new DynamicPoint(31, landmarkX[34], landmarkY[34], 0.0f));
        // 鼻子
        mDynamicPoints.add(new DynamicPoint(0, landmarkX[49], landmarkY[49], 0.0f));
        mDynamicPoints.add(new DynamicPoint(1, landmarkX[82], landmarkY[82], 0.0f));
        mDynamicPoints.add(new DynamicPoint(2, landmarkX[83], landmarkY[83], 0.0f));
        mDynamicPoints.add(new DynamicPoint(3, landmarkX[46], landmarkY[46], 0.0f));
        mDynamicPoints.add(new DynamicPoint(4, landmarkX[43], landmarkY[43], 0.0f));
        // 嘴巴
        mDynamicPoints.add(new DynamicPoint(5, landmarkX[90], landmarkY[90], 0.0f));
        mDynamicPoints.add(new DynamicPoint(6, landmarkX[98], landmarkY[98], 0.0f));
        mDynamicPoints.add(new DynamicPoint(7, landmarkX[84], landmarkY[84], 0.0f));
        mDynamicPoints.add(new DynamicPoint(8, landmarkX[102], landmarkY[102], 0.0f));
        mDynamicPoints.add(new DynamicPoint(9, landmarkX[93], landmarkY[93], 0.0f));
        // 右眼
        mDynamicPoints.add(new DynamicPoint(12, landmarkX[72], landmarkY[72], 0.0f));
        mDynamicPoints.add(new DynamicPoint(13, landmarkX[55], landmarkY[55], 0.0f));
        mDynamicPoints.add(new DynamicPoint(14, landmarkX[73], landmarkY[73], 0.0f));
        mDynamicPoints.add(new DynamicPoint(15, landmarkX[52], landmarkY[52], 0.0f));
        // 左眼
        mDynamicPoints.add(new DynamicPoint(17, landmarkX[75], landmarkY[75], 0.0f));
        mDynamicPoints.add(new DynamicPoint(18, landmarkX[58], landmarkY[58], 0.0f));
        mDynamicPoints.add(new DynamicPoint(19, landmarkX[76], landmarkY[76], 0.0f));
        mDynamicPoints.add(new DynamicPoint(20, landmarkX[61], landmarkY[61], 0.0f));
        // 左脸
        mDynamicPoints.add(new DynamicPoint(22, landmarkX[32], landmarkY[32], 0.0f));
        mDynamicPoints.add(new DynamicPoint(23, landmarkX[29], landmarkY[29], 0.0f));
        mDynamicPoints.add(new DynamicPoint(24, landmarkX[24], landmarkY[24], 0.0f));
        mDynamicPoints.add(new DynamicPoint(25, landmarkX[20], landmarkY[20], 0.0f));
        // 下巴
        mDynamicPoints.add(new DynamicPoint(10, landmarkX[16], landmarkY[16], 0.0f));
        // 右脸
        mDynamicPoints.add(new DynamicPoint(26, landmarkX[12], landmarkY[12], 0.0f));
        mDynamicPoints.add(new DynamicPoint(27, landmarkX[8], landmarkY[8], 0.0f));
        mDynamicPoints.add(new DynamicPoint(28, landmarkX[4], landmarkY[4], 0.0f));
        mDynamicPoints.add(new DynamicPoint(29, landmarkX[0], landmarkY[0], 0.0f));

        ((My3DRenderer) mISurfaceRenderer).setDynamicPoints(mDynamicPoints);
    }

    private class My3DRenderer extends Renderer {
        private Object3D mContainer;
        private Object3D mMask;
        private Geometry3D mGeometry3D;

        private List<DynamicPoint> mPoints = new ArrayList<>();
        private boolean mIsChanging = false;

        My3DRenderer(Context context) {
            super(context);
        }

        @Override
        protected void initScene() {
            try {
                final LoaderOBJ parser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.base_face_uv_obj);
                parser.parse();
                mMask = parser.getParsedObject();
                mMask.setScale(0.25f);
                mGeometry3D = mMask.getGeometry();

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

            if (mPoints != null && mPoints.size() > 0) {
                mIsChanging = true;
                FloatBuffer vertBuffer = mGeometry3D.getVertices();

                try {  // FIXME
                    for (int i=0; i<mPoints.size(); i++) {
                        DynamicPoint point = mPoints.get(i);
                        Log.e(TAG, "No." + i + ": " + point.toString());
                        changePoint(vertBuffer, point.getIndex(), point.getX(), point.getY(), point.getZ());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mGeometry3D.changeBufferData(mGeometry3D.getVertexBufferInfo(), vertBuffer, 0, vertBuffer.limit());
                mIsChanging = false;
            }
        }

        private int faceIndices[][]={
            {101, 105, 107, 111, 117, 120, },  // 1
            {87, 89, 92, 95, 99, 100, 103, },  // 2
            {75, 78, 81, 83, 106, 109, 113, },  // 3
            {84, 86, 104, 108, },  // 4
            {53, 57, 59, 63, 79, 82, 85, 88, },  // 5
            {15, 15, 17, 20, 110, 112, 115, 122, },  // 6
            {116, 118, },  // 7
            {24, 27, 29, 97, 102, 119, 126, },  // 8
            {121, 124, },  // 9
            {19, 22, 123, 125, },  // 10
            {16, 21, 23, 25, },  // 11
            {45, 48, 50, 52, 55, },  // 12
            {46, 49, },  // 13
            {51, 54, 90, 91, },  // 14
            {93, 94, },  // 15
            {28, 33, 36, 39, 42, 43, 47, 96, 98, },  // 16
            {58, 61, 66, 68, 71, },  // 17
            {64, 67, },  // 18
            {62, 65, 76, 80, },  // 19
            {73, 77, },  // 20
            {2, 6, 9, 12, 13, 69, 70, 74, 114, },  // 21
            {56, 60, },  // 22
            {1, 4, },  // 23
            {5, 7, },  // 24
            {8, 10, },  // 25
            {11, 14, 18, },  // 26
            {26, 30, 31, },  // 27
            {32, 34, },  // 28
            {35, 37, },  // 29
            {38, 40, },  // 30
            {3, 72, },  // 31
            {41, 44, },  // 32
        };


        private int[] getIndexArrayByFace(int faceIndex) {
            return faceIndices[faceIndex];
        }

        private void changePoint(FloatBuffer vertBuffer, int faceIndex, float x, float y, float z) {
            int[] indices = getIndexArrayByFace(faceIndex);
            if (indices != null) {
                int len = indices.length;
                for (int i=0; i<len; i++) {
                    int index = indices[i]-1;
                    vertBuffer.put(index * 3, x);
                    vertBuffer.put(index * 3 + 1, y);
                    vertBuffer.put(index * 3 + 2, z);
                }
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
        }

        synchronized void setDynamicPoints(List<DynamicPoint> mPoints) {
            if (!mIsChanging) {
                this.mPoints = mPoints;
            }
        }
    }
}
