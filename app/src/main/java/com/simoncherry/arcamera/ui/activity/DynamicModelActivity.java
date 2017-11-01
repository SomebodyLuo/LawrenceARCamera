package com.simoncherry.arcamera.ui.activity;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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

import org.rajawali3d.Geometry3D;
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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class DynamicModelActivity extends AppCompatActivity implements FrameCallback {

    private final static String TAG = DynamicModelActivity.class.getSimpleName();

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

    private List<DynamicPoint> mDynamicPoints = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = DynamicModelActivity.this;

        mAccelerometer = new Accelerometer(this);
        mAccelerometer.start();

        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        setContentView(R.layout.activity_cam_3d);
        mTrackText = (TextView) findViewById(R.id.tv_track);
        mActionText = (TextView) findViewById(R.id.tv_action);
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
                mRenderer = new CameraTrackRenderer(mContext, (CameraManager)getSystemService(CAMERA_SERVICE), mController, cameraId);
                ((CameraTrackRenderer) mRenderer).setTrackCallBackListener(new CameraTrackRenderer.TrackCallBackListener() {
                    @Override
                    public void onTrackDetected(STMobileFaceAction[] faceActions, final int orientation, final int value,
                                                final float pitch, final float roll, final float yaw,
                                                final int eye_dist, final int id, final int eyeBlink, final int mouthAh,
                                                final int headYaw, final int headPitch, final int browJump) {
//                        handle3dModelRotation(pitch, roll, yaw);
//                        handle3dModelTransition(faceActions, orientation, eye_dist, yaw);
                        setLandmarkFilter(faceActions, orientation, mouthAh);
                        final Bitmap bitmap = handleDrawLandMark(faceActions, orientation);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (bitmap != null) {
                                    mIvLandmark.setImageBitmap(bitmap);
                                }
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

            mController.setFrameCallback(720, 1280, DynamicModelActivity.this);
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
                        Toast.makeText(DynamicModelActivity.this, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(DynamicModelActivity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(DynamicModelActivity.this, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
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

                float[] fuckX = new float[landmarkX.length];
                float[] fuckY = new float[landmarkY.length];
                System.arraycopy(landmarkX, 0, fuckX, 0, landmarkX.length);
                System.arraycopy(landmarkY, 0, fuckY, 0, landmarkY.length);
                handleChangeModel(fuckX, fuckY);
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

    private void handleChangeModel(float[] landmarkX, float[] landmarkY) {
        mDynamicPoints.clear();

        int length = landmarkX.length;
        for (int i=0; i<length; i++) {
            landmarkX[i] = (landmarkX[i] * 2f - 1f) * 1.25f;
//            landmarkX[i] = ((1-landmarkX[i]) * 2f - 1f) * 1.25f;
//            landmarkY[i] = (landmarkY[i] * 2f - 1f) * 1.25f;
            landmarkY[i] = ((1-landmarkY[i]) * 2f - 1f) * 1.66f;
        }

//            changePoint(vertBuffer, 6, -1.5f, 0.0f, 0.0f);
//            changePoint(vertBuffer, 7, 1.5f, 0.0f, 0.0f);
//            changePoint(vertBuffer, 1, -1.1f, -1.1f, 0.0f);
//            changePoint(vertBuffer, 2, 1.1f, -1.1f, 0.0f);
//            changePoint(vertBuffer, 3, -1.2f, 1.2f, 0.0f);
//            changePoint(vertBuffer, 4, 1.2f, 1.2f, 0.0f);
//            changePoint(vertBuffer, 8, 0.0f, -1.5f, 0.0f);
//            changePoint(vertBuffer, 5, 0.0f, 1.5f, 0.0f);

        mDynamicPoints.add(new DynamicPoint(9, landmarkX[46], landmarkY[46], 0.75f));
        mDynamicPoints.add(new DynamicPoint(6, landmarkX[27], landmarkY[27], 0.0f));
        mDynamicPoints.add(new DynamicPoint(7, landmarkX[5], landmarkY[5], 0.0f));
        mDynamicPoints.add(new DynamicPoint(1, landmarkX[22], landmarkY[22], 0.0f));
        mDynamicPoints.add(new DynamicPoint(2, landmarkX[10], landmarkY[10], 0.0f));
        mDynamicPoints.add(new DynamicPoint(3, landmarkX[32], landmarkY[32], 0.0f));
        mDynamicPoints.add(new DynamicPoint(4, landmarkX[0], landmarkY[0], 0.0f));
        mDynamicPoints.add(new DynamicPoint(8, landmarkX[16], landmarkY[16], 0.0f));
        mDynamicPoints.add(new DynamicPoint(5, (landmarkX[67] + landmarkX[68] * 0.5f), (landmarkY[35] + landmarkY[40] * 0.5f), 0.0f));

        ((My3DRenderer) mISurfaceRenderer).setDynamicPoints(mDynamicPoints);
    }

    private class My3DRenderer extends Renderer {
        private Object3D mContainer;
        private Object3D mMask;
        private Geometry3D mGeometry3D;
        private Vector3 mAccValues;
        private float mScale = 1.0f;
        private List<DynamicPoint> mPoints = new ArrayList<>();
        private boolean mIsChanging = false;

        My3DRenderer(Context context) {
            super(context);
            mAccValues = new Vector3();
        }

        @Override
        protected void initScene() {
            try {
                final LoaderOBJ parser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.simple_plane_obj);
                parser.parse();
                mMask = parser.getParsedObject();
//                mMask.setScale(0.75f);
//                mMask.setY(-0.2f);
//                mMask.setZ(0.4f);
                mMask.setColor(Color.RED);

                mContainer = new Object3D();
                mContainer.addChild(mMask);
                getCurrentScene().addChild(mContainer);

                mGeometry3D = mMask.getGeometry();

                getCurrentScene().getCamera().setZ(5.5);

                FloatBuffer vertBuffer = mGeometry3D.getVertices();
                IntBuffer indices = (IntBuffer)mGeometry3D.getIndices();

//                for (int i=0; i<vertBuffer.limit(); i++) {
//                    String type;
//                    int tmp = i%3;
//                    if (tmp == 0) {
//                        type = "x";
//                    } else if (tmp == 1) {
//                        type = "y";
//                    } else {
//                        type = "z";
//                    }
//                    Log.e(TAG, "get vertex No." + i/3 + " " + type + " : " + vertBuffer.get(i));
//                }

                for(int i=0; i<mGeometry3D.getNumIndices(); i++) {
                    int index = indices.get(i);
                    float x = vertBuffer.get(3*index);
                    float y = vertBuffer.get(3*index+1);
                    float z = vertBuffer.get(3*index+2);
                    Log.e(TAG, "get vertex No." + index + " x: " + x + ", y: " + y + ", z: " + z);
                }

            } catch (ParsingException e) {
                e.printStackTrace();
            }

            getCurrentScene().setBackgroundColor(0);
        }

        @Override
        protected void onRender(long ellapsedRealtime, double deltaTime) {
            super.onRender(ellapsedRealtime, deltaTime);

//            FloatBuffer vertBuffer = mGeometry3D.getVertices();
//            changePoint(vertBuffer, 6, -1.5f, 0.0f, 0.0f);
//            changePoint(vertBuffer, 7, 1.5f, 0.0f, 0.0f);
//            changePoint(vertBuffer, 1, -1.1f, -1.1f, 0.0f);
//            changePoint(vertBuffer, 2, 1.1f, -1.1f, 0.0f);
//            changePoint(vertBuffer, 3, -1.2f, 1.2f, 0.0f);
//            changePoint(vertBuffer, 4, 1.2f, 1.2f, 0.0f);
//            changePoint(vertBuffer, 8, 0.0f, -1.5f, 0.0f);
//            changePoint(vertBuffer, 5, 0.0f, 1.5f, 0.0f);
//            mGeometry3D.changeBufferData(mGeometry3D.getVertexBufferInfo(), vertBuffer, 0, vertBuffer.limit());

            if (mPoints != null && mPoints.size() > 0) {
                mIsChanging = true;
                FloatBuffer vertBuffer = mGeometry3D.getVertices();

                try {
                    for (int i=0; i<mPoints.size(); i++) {
                        DynamicPoint point = mPoints.get(i);
                        Log.e(TAG, "No." + i + " DynamicPoint:" + point.toString());
                        changePoint(vertBuffer, point.index, point.x, point.y, point.z);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mGeometry3D.changeBufferData(mGeometry3D.getVertexBufferInfo(), vertBuffer, 0, vertBuffer.limit());
                mIsChanging = false;
            }

            //mContainer.setRotation(mAccValues.x, mAccValues.y, mAccValues.z);
            //mContainer.setScale(mScale);
        }

        private int[] face1indices = new int[]{12};
        private int[] face2indices = new int[]{22};
        private int[] face3indices = new int[]{0};
        private int[] face4indices = new int[]{10};
        private int[] face5indices = new int[]{2, 4, 8, 11};
        private int[] face6indices = new int[]{1, 5, 14, 16};
        private int[] face7indices = new int[]{7, 9, 20, 23};
        private int[] face8indices = new int[]{13, 17, 19, 21};
        private int[] face9indices = new int[]{3, 6, 15, 18};

        private int[] getIndexArrayByFace(int faceIndex) {
            switch (faceIndex) {
                case 1:
                    return face1indices;
                case 2:
                    return face2indices;
                case 3:
                    return face3indices;
                case 4:
                    return face4indices;
                case 5:
                    return face5indices;
                case 6:
                    return face6indices;
                case 7:
                    return face7indices;
                case 8:
                    return face8indices;
                case 9:
                    return face9indices;
                default:
                    return null;
            }
        }

        private void changePoint(FloatBuffer vertBuffer, int faceIndex, float x, float y, float z) {
            int[] indices = getIndexArrayByFace(faceIndex);
            if (indices != null) {
                int len = indices.length;
                for (int i=0; i<len; i++) {
                    int index = indices[i];
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

        void setAccelerometerValues(float x, float y, float z) {
            mAccValues.setAll(x, y, z);
        }

        void setScale(float scale) {
            mScale = scale;
        }

        synchronized void setDynamicPoints(List<DynamicPoint> mPoints) {
            if (!mIsChanging) {
                this.mPoints = mPoints;
            }
        }
    }

    private class DynamicPoint {
        private int index;
        private float x;
        private float y;
        private float z;

        public DynamicPoint(int index, float x, float y, float z) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return "DynamicPoint{" +
                    "index=" + index +
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
}
