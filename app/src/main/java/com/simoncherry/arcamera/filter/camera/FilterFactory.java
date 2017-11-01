package com.simoncherry.arcamera.filter.camera;

import android.content.res.Resources;

import com.simoncherry.arcamera.R;
import com.simoncherry.arcamera.model.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 2017/7/6.
 */

public class FilterFactory {

    public static AFilter getFilter(Resources res, int menuId) {
        switch (menuId) {
            case R.id.menu_camera_default:
                return new NoFilter(res);
            case R.id.menu_camera_beauty:
                return new BeautyFilter(res);
            case R.id.menu_camera_gray:
                return new GrayFilter(res);
            case R.id.menu_camera_binary:
                return new BinaryFilter(res);
            case R.id.menu_camera_cool:
                return new CoolFilter(res);
            case R.id.menu_camera_warm:
                return new WarmFilter(res);
            case R.id.menu_camera_negative:
                return new NegativeFilter(res);
            case R.id.menu_camera_blur:
                return new BlurFilter(res);
            case R.id.menu_camera_mosaic:
                return new MosaicFilter(res);
            case R.id.menu_camera_emboss:
                return new EmbossFilter(res);
            case R.id.menu_camera_mag:
                return new MagFilter(res);
            case R.id.menu_camera_mirror:
                return new MirrorFilter(res);
            case R.id.menu_camera_fisheye:
                return new FishEyeFilter(res);
            case R.id.menu_camera_anim:
                ZipPkmAnimationFilter zipPkmAnimationFilter = new ZipPkmAnimationFilter(res);
                zipPkmAnimationFilter.setAnimation("assets/etczip/dragon.zip");
                return zipPkmAnimationFilter;
            case R.id.menu_camera_point:
                return new DrawPointFilter(res);
            case R.id.menu_camera_landmark:
                return new LandmarkFilter(res);
            case R.id.menu_camera_big_eye:
                return new BigEyeFilter(res);
            case R.id.menu_camera_small_eye:
                return new SmallEyeFilter(res);
            case R.id.menu_camera_fire_eye:
                return new FireEyeFilter(res);
            case R.id.menu_camera_black_eye:
                return new BlackEyeFilter(res);
            case R.id.menu_camera_flush:
                return new FlushFilter(res);
            case R.id.menu_camera_fat_face:
                return new FatFaceFilter(res);
            case R.id.menu_camera_rainbow:
                return new RainbowFilter(res);
            case R.id.menu_camera_rainbow2:
                return new Rainbow2Filter(res);
            case R.id.menu_camera_rainbow3:
                return new Rainbow3Filter(res);
            case R.id.menu_camera_ghost:
                return new GhostFilter(res);
            default:
                return new NoFilter(res);
        }
    }

    public static List<Filter> getPresetFilter() {
        List<Filter> filterList = new ArrayList<>();
        filterList.add(new Filter(R.id.menu_camera_default, R.drawable.filter_thumb_0, "原图"));
        filterList.add(new Filter(R.id.menu_camera_beauty, R.drawable.filter_thumb_0, "美颜"));
        filterList.add(new Filter(R.id.menu_camera_gray, R.drawable.filter_thumb_0, "灰度化"));
        filterList.add(new Filter(R.id.menu_camera_binary, R.drawable.filter_thumb_0, "二值化"));
        filterList.add(new Filter(R.id.menu_camera_cool, R.drawable.filter_thumb_0, "冷色调"));
        filterList.add(new Filter(R.id.menu_camera_warm, R.drawable.filter_thumb_0, "暖色调"));
        filterList.add(new Filter(R.id.menu_camera_negative, R.drawable.filter_thumb_0, "底片"));
        filterList.add(new Filter(R.id.menu_camera_blur, R.drawable.filter_thumb_0, "模糊"));
        filterList.add(new Filter(R.id.menu_camera_emboss, R.drawable.filter_thumb_0, "浮雕"));
        filterList.add(new Filter(R.id.menu_camera_mosaic, R.drawable.filter_thumb_0, "马赛克"));
        filterList.add(new Filter(R.id.menu_camera_mirror, R.drawable.filter_thumb_0, "镜像"));
        filterList.add(new Filter(R.id.menu_camera_fisheye, R.drawable.filter_thumb_0, "鱼眼"));
        filterList.add(new Filter(R.id.menu_camera_mag, R.drawable.filter_thumb_0, "放大镜"));
        return filterList;
    }

    public static List<Filter> getPresetEffect() {
        List<Filter> filterList = new ArrayList<>();
        filterList.add(new Filter(R.id.menu_camera_default, R.drawable.ic_remove, "原图"));
        filterList.add(new Filter(R.id.menu_camera_rainbow3, R.drawable.effect_rainbow, "吐彩虹"));
        filterList.add(new Filter(R.id.menu_camera_fire_eye, R.drawable.effect_fire, "眼睛冒火"));
        filterList.add(new Filter(R.id.menu_camera_ghost, R.drawable.effect_ghost, "恶灵"));
        filterList.add(new Filter(R.id.menu_camera_big_eye, R.drawable.effect_big_eye, "大眼"));
        filterList.add(new Filter(R.id.menu_camera_small_eye, R.drawable.effect_small_eye, "小眼"));
        filterList.add(new Filter(R.id.menu_camera_black_eye, R.drawable.effect_black_eye, "黑眼圈"));
        filterList.add(new Filter(R.id.menu_camera_flush, R.drawable.effect_shy, "红晕"));
        filterList.add(new Filter(R.id.menu_camera_fat_face, R.drawable.effect_fat_face, "胖脸"));
        return filterList;
    }
}
