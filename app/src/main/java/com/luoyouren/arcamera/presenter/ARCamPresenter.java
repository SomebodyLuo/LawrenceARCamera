package com.luoyouren.arcamera.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.sensetime.stmobileapi.STMobileFaceAction;
import com.sensetime.stmobileapi.STUtils;
import com.luoyouren.arcamera.contract.ARCamContract;
import com.luoyouren.arcamera.model.DynamicPoint;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaScannerConnection;

/**
 * Created by Simon on 2017/7/19.
 */

public class ARCamPresenter implements ARCamContract.Presenter {
    private final static String TAG = "luoyouren_ARCamP";

    private Context mContext;
    private ARCamContract.View mView;
    private List<DynamicPoint> mDynamicPoints;
    private boolean isDebug = false;



    long time = 0;

    public ARCamPresenter(Context context, ARCamContract.View mView) {
        this.mContext = context;
        this.mView = mView;
        mDynamicPoints = new ArrayList<>();
    }

    @Override
    public void handlePhotoFrame(byte[] bytes, Bitmap mRajawaliBitmap, int photoWidth, int photoHeight) {
        // 将相机预览的帧数据转成Bitmap
        Bitmap bitmap = Bitmap.createBitmap(photoWidth, photoHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer b = ByteBuffer.wrap(bytes);
        bitmap.copyPixelsFromBuffer(b);
        // 如果Rajawali渲染的3D模型截图不为空，就将两者合成
        if (mRajawaliBitmap != null) {
            Log.i(TAG, "mRajawaliBitmap != null");
            mRajawaliBitmap = Bitmap.createScaledBitmap(mRajawaliBitmap, photoWidth, photoHeight, false);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(mRajawaliBitmap, 0, 0, null);
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
            mRajawaliBitmap.recycle();
            mRajawaliBitmap = null;
        } else {
            Log.i(TAG, "mRajawaliBitmap == null");
        }
        // 最后保存
        savePhoto(bitmap);
        bitmap.recycle();
        bitmap = null;
    }

    @Override
    public void handleVideoFrame(byte[] bytes, int[] mRajawaliPixels) {
        // 如果Rajawali渲染的3D模型帧数据不为空，就将两者合成
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
                // 取Rajawali不透明的部分
                boolean isBackground = r == 0 && g == 0 && b == 0 && a == 0;
                if (!isBackground) {
                    bytes[i] = a;
                    bytes[i + 1] = r;
                    bytes[i + 2] = g;
                    bytes[i + 3] = b;
                }
            }
        }
        mView.onGetVideoData(bytes);
    }

    @Override
    public void savePhoto(Bitmap bitmap) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenGLDemo/photo/";
        File folder = new File(path);
        if(!folder.exists() && !folder.mkdirs()){
            mView.onSavePhotoFailed();
            return;
        }
        long dataTake = System.currentTimeMillis();
        final String jpegName = path + dataTake + ".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mView.onSavePhotoSuccess(jpegName);
    }

    // 处理3D模型的旋转
    @Override
    public void handle3dModelRotation(float pitch, float roll, float yaw) {
        mView.onGet3dModelRotation(-pitch, roll+90, -yaw);
    }

    @Override
    public void handelFacePoints(STMobileFaceAction[] faceActions)
    {
//        mView.onGetPointsPosition(faceActions[0].getFace().getRect());
    }

    private float mStandardArea = 10700.0f;

    // 处理3D模型的平移
    @Override
    public void handle3dModelTransition(STMobileFaceAction[] faceActions,
                                        int orientation, int eye_dist, float pitch, float roll, float yaw,
                                        int previewWidth, int previewHeight) {
        STMobileFaceAction faceAction = faceActions[0];

        if (false) {


        }

        //==========================================================================================
        else {
            //2017-11-02
//            float x, y;
//            PointF[] pointFs = preProcessPoints(faceAction.getFace().getPointsArray(), orientation);
//            Rect rect = CalculateRectForPoints(pointFs);
//            x = rect.centerX();
//            y = rect.centerY();

            //2017-11-18
            //换成原始的人脸检测返回的那个矩形
            float x, y;
            Rect rect = preProcessRect(faceAction.getFace().getRect(), orientation);
            x = rect.centerX();
            y = rect.centerY();


            //================================平移调整：将屏幕坐标系下的坐标转换成OpenGL坐标系下的坐标====================================
            //2017-11-02 根据excel表格拟合出来的表达式，再根据实际效果调整得出x/y，
            float x1, y1;
//            x1 = -0.0292f * y + 7f;
//            y1 = 0.0291f * x - 9.3f;

            x1 = 0.0291f * x - 7.0f;
            x1 = x1 / 3.8f;
            y1 = -0.0292f * y + 5.0f;
            y1 = y1 / 3.8f;

            Log.i(TAG, "luoyouren: rect.width() = " + rect.width() + "; rect.height()" + rect.height() +"; rect.area = " + rect.width() * rect.height() + "; x1 =" + x1 + "; y1 = " + y1);

            //================================缩放调整：z轴说明的是人脸的大小，即人脸离手机的距离=======================================
            //2017-11-03
            //下面通过人脸的大小缩放比来确定Scale的参数
            /* mContainer.setScale(1.0f, 1.0f, 1.0f); getCamera().setZ(5.5); ironMan.setScale(0.04f)这样的参数下，人脸的面积大小为11877时，刚好吻合模型。*/
            /* mContainer.setScale(1.05f, 0.9f, 0.9f); getCamera().setZ(5.5); ironMan.setScale(0.04f)这样的参数下，人脸的面积大小为10700时，刚好吻合模型。*/
            float area = rect.width() * rect.height();
            Log.i(TAG, "luoyouren: --------------------- area =  " + area);

            //2017-11-09
            //对人脸矩形的面积进行补偿：pitch roll yaw
            double headPitch = (pitch > 0) ? pitch : -pitch;
            headPitch = 90 - headPitch;
            double factorPitch =  Math.sin(headPitch * Math.PI / 180.0f);    //利用pitch(俯仰角)对人脸大小进行补偿
            Log.i(TAG, "luoyouren: --------------------- factorPitch =  " + factorPitch);

            //2017-11-18
            //补偿的实际效果显示为：过度了！不用补偿这个！
            double headYaw = (yaw > 0) ? yaw : -yaw;
            double factorYaw = Math.cos(headYaw * Math.PI / 180.0f);        //利用yaw(偏航角)对人脸大小进行补偿
            Log.i(TAG, "luoyouren: --------------------- factorYaw =  " + factorYaw);

            //依据： x = L / ( cos& + sin&) --> x^2 = L^2 / (cos& + sin&)^2
            //实际效果也不行！
            double headRoll = roll + 90;
            headRoll = (headRoll > 0) ? headRoll : -headRoll;
            double cosRoll = Math.cos(headRoll * Math.PI / 180.0f);
            double sinRoll = Math.sin(headRoll * Math.PI / 180.0f);
            double factorRoll = cosRoll + sinRoll;
            factorRoll = 1.1f * cosRoll;
            Log.i(TAG, "luoyouren: --------------------- factorRoll =  " + factorRoll);


            double factor = factorPitch;
            Log.i(TAG, "luoyouren: ---------------------------------------- factor =  " + factor);
            area = area / (float) factor;

            //2017-11-03
            float z;
            if (true) {
                //平方关系: 比较合适
                z = 5.5f * 5.5f * mStandardArea / area;
                // 依据: (l x h) / (L x H) = D^2 / d^2;
                z = 5.5f - (float) Math.sqrt((double) z);
            }else
            {
                //线性关系
                z = - mStandardArea / area;
            }



            //================================用缩放修正平移：现在开始做修正工作，利用Z轴去修正X / Y轴 =======================================
            Log.i(TAG, "luoyouren: ---------------------------------------- scale z =  " + z);
            //2017-11-06
            //----------修正Y轴----------
            if (z > 0)  //分别修正
            {
                //1. 首先当人脸Y坐标不动时，Z轴如何修正平移
                y1 = y1 + 0.25f * z;    //1. 首先当人脸Y坐标不动时，Z轴如何修正平移  ~
//                y1 = y1 + 0.15f * z;
                //2. 再移动人脸Y坐标，找修正
//                y1 = y1 + (0.0365f * y1 + 0.0595f);           //
//                y1 = y1 - (0.465f * y1 + 0.695f);             //
                y1 = y1 - (0.205f * y1 + 0.395f);               //~

                //3. 最后用Z轴再次修正平移
//                y1 = y1 - (0.28f * z - 0.25f);        //
//                y1 = y1 - 0.01f * (z - 2.13f);        //
                y1 = y1 - 0.25f * (z - 1.03f);          // ~

            } else {
                //1. 首先当人脸Y坐标不动时，Z轴如何修正平移

                //2. 再移动人脸Y坐标，找修正
//                y1 = y1 - 0.08f * z;              //注意z是负的  ~
                y1 = y1 + 0.20f * (y1 + 1.3f);      //注意z是负的  ~

//                if (y1 > -1.3f) {
//                    //2. 再移动人脸Y坐标，找修正
////                y1 = y1 - 0.08f * z;              //注意z是负的  ~
//                    y1 = y1 + 0.20f * (y1 + 1.3f);      //注意z是负的  ~
//
//                    //3. 最后用Z轴再次修正平移
//                    y1 = y1 - 0.25f * (z + 1.50f);
//                } else {
                    //2. 再移动人脸Y坐标，找修正
//                    y1 = y1 + 0.20f * (y1 + 1.3f);      //注意z是负的  ~
//                }
            }

            //-----------修正X轴-----------
//            if (z > 0)  //分别修正
//            {
//                x1 = x1 + 0.25f * z;
//            } else {
//                x1 = x1 + 0.08f * z;        //注意z是负的
//            }

            //===================================================== 旋转调整：pitch roll yaw ===============================================

            //==============================================================================================================================
            //头离屏幕太远，直接不显示——把模型移动到视窗外
            if (z < -3.0f)
            {
                x1 = y1 = 20.0f;
            }


            mView.onGet3dModelTransition(x1, y1 + 1.1f, z); //1.0 是模型带来的系统误差
        }
    }




    int PREVIEW_WIDTH = 640;
    int PREVIEW_HEIGHT = 480;
    public Rect preProcessRect(Rect rect, int orientation)
    {
        boolean rotate270 = orientation == 270;
        //=================================================长方形===================================
        //----------当前对应图1
        if (rotate270) {
            rect = STUtils.RotateDeg270(rect, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        } else {
            rect = STUtils.RotateDeg90(rect, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }
        //----------此时对应图2
        if(mView.getCameraId() == 1) {
            int left = rect.left;
            rect.left = PREVIEW_HEIGHT - rect.right;
            rect.right = PREVIEW_HEIGHT - left;
        }
        //----------此时对应图3

        return rect;
    }

    public PointF[] preProcessPoints(PointF[] points, int orientation)
    {
        boolean rotate270 = orientation == 270;
        //=================================================点集=====================================
        //----------当前对应图1
        int i;
        for (i = 0; i < points.length; i++) {
            if (rotate270) {
                points[i] = STUtils.RotateDeg270(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
            } else {
                points[i] = STUtils.RotateDeg90(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }
        }
        //----------此时对应图2
        for (i = 0; i < points.length; i++) {
            if(mView.getCameraId() == 1) {
                points[i].x = PREVIEW_HEIGHT - points[i].x;
            }
        }
        //----------此时对应图3

        return points;
    }

    private Rect CalculateRectForPoints(PointF[] pointF)
    {
        if (pointF.length < 0)
        {
            return null;
        }

        float left = pointF[0].x, top = pointF[0].y, right = pointF[0].x, bottom = pointF[0].y;
        for (PointF pointF1: pointF)
        {
            left = (left < pointF1.x) ? left : pointF1.x;
            top = (top < pointF1.y) ? top : pointF1.y;
            right = (right > pointF1.x) ? right : pointF1.x;
            bottom = (bottom > pointF1.y) ? bottom : pointF1.y;

        }

        Rect rect = new Rect((int)left, (int)top, (int)right, (int)bottom);
//        Log.i(TAG, "-----------------left = " + left + "; right = " + right + "; top = " + top + "; bottom = " + bottom);

//        Log.i(TAG, "--------------------------------------------------------------------------centerX =" + rect.centerX() + "; centerY = " + rect.centerY());
        return rect;
    }

    // 处理人脸关键点
    @Override
    public void handleFaceLandmark(STMobileFaceAction[] faceActions, int orientation, int mouthAh,
                                   int previewWidth, int previewHeight) {
        boolean rotate270 = orientation == 270;
        if (faceActions != null && faceActions.length > 0) {
            Log.i(TAG, "-->> face count = " + faceActions.length);

            STMobileFaceAction faceAction = faceActions[0];

            //获取人脸检测结果点
            PointF[] points = faceAction.getFace().getPointsArray();
            Log.i(TAG, "-->> points count = " + points.length);
            float[] landmarkX = new float[points.length];
            float[] landmarkY = new float[points.length];

            String path1 = null;
            String path2 = null;
            String path3 = null;
            FileWriter fw1 = null;
            FileWriter fw2 = null;
            FileWriter fw3 = null;
            if (isDebug) {
                time = System.currentTimeMillis();
                path1 = getModelPath(time + "-points1.txt");
                path2 = getModelPath(time + "-points2.txt");
                path3 = getModelPath(time + "-points3.txt");
                Log.d(TAG, "luo: path1 = " + path1);
            }
            try {
                if (isDebug) {
                    fw1 = new FileWriter(path1);
                    fw2 = new FileWriter(path2);
                    fw3 = new FileWriter(path3);
//                fw.write("fuck you\n");
                }

                Log.d(TAG, "0: handleFaceLandmark: "+ "previewWidth = " + previewWidth + ";previewHeight = " + previewHeight);
                for (int i = 0; i < points.length; i++) {

                    Log.d(TAG, "1: handleFaceLandmark: "+ "points[i].x = " + points[i].x + ";points[i].y = " + points[i].y);
                    if (isDebug) {
                        fw1.write("" + points[i].x + "\t" + points[i].y + "\n");
                    }

                    //此时对应图1
                    if (rotate270) {
                        points[i] = STUtils.RotateDeg270(points[i], previewWidth, previewHeight);
                    } else {
                        points[i] = STUtils.RotateDeg90(points[i], previewWidth, previewHeight);
                    }
                    //此时对应图2
                    Log.d(TAG, "2: handleFaceLandmark: "+ "points[i].x = " + points[i].x + ";points[i].y = " + points[i].y);

                    if (isDebug) {
                        fw2.write("" + points[i].x + "\t" + points[i].y + "\n");
                    }

                    landmarkX[i] = 1 - points[i].x / 480.0f;
                    landmarkY[i] = points[i].y / 640.0f;
                    //此时对应图3
                    Log.d(TAG, "3: handleFaceLandmark: "+ "landmarkX[i] = " + landmarkX[i] + ";landmarkY[i] = " + landmarkY[i] + "\n\n");

                    if (isDebug) {
                        fw3.write("" + landmarkX[i] + "\t" + landmarkY[i] + "\n");
                    }
                }

                if (isDebug) {
                    fw1.flush();
                    fw1.close();

                    fw2.flush();
                    fw2.close();

                    fw3.flush();
                    fw3.close();
                }
            } catch (IOException e) {
            }

            if (isDebug) {
                String paths[] = new String[3];
                paths[0] = path1;
                paths[1] = path2;
                paths[2] = path3;
                MediaScannerConnection.scanFile(mContext, paths, null, null);
            }

            mView.onGetFaceLandmark(landmarkX, landmarkY, mouthAh);
        }
    }

    protected String getModelPath(String modelName) {
        String path = null;
        File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + modelName;
        }
        return path;
    }

    @Override
    public void handleChangeModel(float[] landmarkX, float[] landmarkY) {
        mDynamicPoints.clear();

        int length = landmarkX.length;

        String path4 = null;
        FileWriter fw4 = null;
        String path5 = null;
        FileWriter fw5 = null;
        if(isDebug) {
            path4 = getModelPath(time + "-points4.txt");
            Log.d(TAG, "luo: path4 = " + path4);
        }
        try {
            if (isDebug) {
                fw4 = new FileWriter(path4);
            }

            for (int i=0; i<length; i++) {
                Log.d(TAG, "1: handleChangeModel: "+ "landmarkX[i] = " + landmarkX[i] + ";landmarkY[i] = " + landmarkY[i]);
                landmarkX[i] = (landmarkX[i] * 2f - 1f) * 7f;
                landmarkY[i] = ((1-landmarkY[i]) * 2f - 1f) * 9.3f;
                Log.d(TAG, "2: handleChangeModel: "+ "landmarkX[i] = " + landmarkX[i] + ";landmarkY[i] = " + landmarkY[i]);
                if (isDebug) {
                    fw4.write("" + landmarkX[i] + "\t" + landmarkY[i] + "\n");
                }
            }

            if (isDebug) {
                fw4.flush();
                fw4.close();
            }
        } catch (IOException e) {
        }

        if (isDebug) {
            String paths[] = new String[1];
            paths[0] = path4;
            MediaScannerConnection.scanFile(mContext, paths, null, null);
        }

        //luoyouren: 总共44个点，跟base_face_uv3_obj保持一致
        //把人脸检测结果中的106点数据，重新拟合到模型的44个区域上面
        //下面的每一个序号，比如29/15，都是代表模型上面的某一个切片的面上面的点集
        // 额头
        mDynamicPoints.add(new DynamicPoint(29, landmarkX[41], landmarkY[41], 0.0f));
        mDynamicPoints.add(new DynamicPoint(15, landmarkX[39], landmarkY[39], 0.0f));
        mDynamicPoints.add(new DynamicPoint(20, (landmarkX[36] + landmarkX[39])*0.5f, (landmarkY[36] + landmarkY[39])*0.5f, 0.0f));
        mDynamicPoints.add(new DynamicPoint(10, landmarkX[36], landmarkY[36], 0.0f));
        mDynamicPoints.add(new DynamicPoint(30, landmarkX[34], landmarkY[34], 0.0f));
        // 左脸
        mDynamicPoints.add(new DynamicPoint(21, landmarkX[32], landmarkY[32], 0.0f));
        mDynamicPoints.add(new DynamicPoint(22, landmarkX[29], landmarkY[29], 0.0f));
        mDynamicPoints.add(new DynamicPoint(23, landmarkX[24], landmarkY[24], 0.0f));
        mDynamicPoints.add(new DynamicPoint(24, landmarkX[20], landmarkY[20], 0.0f));
        // 下巴
        mDynamicPoints.add(new DynamicPoint(9, landmarkX[16], landmarkY[16], 0.0f));
        // 右脸
        mDynamicPoints.add(new DynamicPoint(28, landmarkX[0], landmarkY[0], 0.0f));
        mDynamicPoints.add(new DynamicPoint(27, landmarkX[3], landmarkY[3], 0.0f));
        mDynamicPoints.add(new DynamicPoint(26, landmarkX[8], landmarkY[8], 0.0f));
        mDynamicPoints.add(new DynamicPoint(25, landmarkX[12], landmarkY[12], 0.0f));
        // 左眼
        mDynamicPoints.add(new DynamicPoint(19, landmarkX[61], landmarkY[61], 0.0f));
        mDynamicPoints.add(new DynamicPoint(32, landmarkX[60], landmarkY[60], 0.0f));
        mDynamicPoints.add(new DynamicPoint(16, landmarkX[75], landmarkY[75], 0.0f));
        mDynamicPoints.add(new DynamicPoint(31, landmarkX[59], landmarkY[59], 0.0f));
        mDynamicPoints.add(new DynamicPoint(17, landmarkX[58], landmarkY[58], 0.0f));
        mDynamicPoints.add(new DynamicPoint(33, landmarkX[63], landmarkY[63], 0.0f));
        mDynamicPoints.add(new DynamicPoint(18, landmarkX[76], landmarkY[76], 0.0f));
        mDynamicPoints.add(new DynamicPoint(34, landmarkX[62], landmarkY[62], 0.0f));
        // 右眼
        mDynamicPoints.add(new DynamicPoint(14, landmarkX[52], landmarkY[52], 0.0f));
        mDynamicPoints.add(new DynamicPoint(36, landmarkX[53], landmarkY[53], 0.0f));
        mDynamicPoints.add(new DynamicPoint(11, landmarkX[72], landmarkY[72], 0.0f));
        mDynamicPoints.add(new DynamicPoint(35, landmarkX[54], landmarkY[54], 0.0f));
        mDynamicPoints.add(new DynamicPoint(12, landmarkX[55], landmarkY[55], 0.0f));
        mDynamicPoints.add(new DynamicPoint(38, landmarkX[56], landmarkY[56], 0.0f));
        mDynamicPoints.add(new DynamicPoint(13, landmarkX[73], landmarkY[73], 0.0f));
        mDynamicPoints.add(new DynamicPoint(37, landmarkX[57], landmarkY[57], 0.0f));
        // 鼻子
        mDynamicPoints.add(new DynamicPoint(0, landmarkX[49], landmarkY[49], 0.0f));
        mDynamicPoints.add(new DynamicPoint(1, landmarkX[82], landmarkY[82], 0.0f));
        mDynamicPoints.add(new DynamicPoint(2, landmarkX[83], landmarkY[83], 0.0f));
        mDynamicPoints.add(new DynamicPoint(3, landmarkX[46], landmarkY[46], 0.0f));
        mDynamicPoints.add(new DynamicPoint(4, landmarkX[43], landmarkY[43], 0.0f));
        // 嘴巴
        mDynamicPoints.add(new DynamicPoint(5, landmarkX[90], landmarkY[90], 0.0f));
        mDynamicPoints.add(new DynamicPoint(41, landmarkX[99], landmarkY[99], 0.0f));
        mDynamicPoints.add(new DynamicPoint(42, landmarkX[98], landmarkY[98], 0.0f));
        mDynamicPoints.add(new DynamicPoint(43, landmarkX[97], landmarkY[97], 0.0f));
        mDynamicPoints.add(new DynamicPoint(6, landmarkX[84], landmarkY[84], 0.0f));
        mDynamicPoints.add(new DynamicPoint(40, landmarkX[103], landmarkY[103], 0.0f));
        mDynamicPoints.add(new DynamicPoint(7, landmarkX[102], landmarkY[102], 0.0f));
        mDynamicPoints.add(new DynamicPoint(39, landmarkX[101], landmarkY[101], 0.0f));
        mDynamicPoints.add(new DynamicPoint(8, landmarkX[93], landmarkY[93], 0.0f));


        if (isDebug) {
            path5 = getModelPath(time + "-points5.txt");
            Log.d(TAG, "luo: path5 = " + path5);
        try {
                fw5 = new FileWriter(path5);

            for (DynamicPoint dynamicPoint: mDynamicPoints) {
                fw5.write("" + dynamicPoint.getX()+ "\t" + dynamicPoint.getY() + "\n");
            }

                fw5.flush();
                fw5.close();
        } catch (IOException e) {
        }

            String paths2[] = new String[1];
            paths2[0] = path5;
            MediaScannerConnection.scanFile(mContext, paths2, null, null);
        }

        mView.onGetChangePoint(mDynamicPoints);
    }
}
