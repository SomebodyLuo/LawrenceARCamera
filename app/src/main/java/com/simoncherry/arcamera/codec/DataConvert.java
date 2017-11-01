package com.simoncherry.arcamera.codec;

/**
 * Created by Simon on 2017/7/15.
 */

public class DataConvert {

    public static final int RGBA_YUV420SP=0x00004012;
    public static final int BGRA_YUV420SP=0x00004210;
    public static final int RGBA_YUV420P=0x00014012;
    public static final int BGRA_YUV420P=0x00014210;
    public static final int RGB_YUV420SP=0x00003012;
    public static final int RGB_YUV420P=0x00013012;
    public static final int BGR_YUV420SP=0x00003210;
    public static final int BGR_YUV420P=0x00013210;

    static {
        System.loadLibrary("VideoConvert");
    }

    public static native void rgbaToYuv(byte[] rgba, int width, int height,byte[] yuv,int type);
}
