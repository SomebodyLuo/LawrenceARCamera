package com.simoncherry.arcamera.util;

import android.graphics.Color;

import com.simoncherry.arcamera.R;
import com.simoncherry.arcamera.gl.TextureController;
import com.simoncherry.arcamera.model.Ornament;
import com.simoncherry.arcamera.rajawali.CustomMaterialPlugin;
import com.simoncherry.arcamera.rajawali.CustomVertexShaderMaterialPlugin;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.plugins.IMaterialPlugin;
import org.rajawali3d.primitives.NPrism;
import org.rajawali3d.primitives.Sphere;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 2017/7/19.
 */

public class OrnamentFactory {
    public final static int NO_COLOR = 2333;

    public static List<Ornament> getPresetOrnament() {
        List<Ornament> ornaments = new ArrayList<>();
        ornaments.add(getNoOrnament());
        ornaments.add(getTigerNose());
        ornaments.add(getVMask());
        ornaments.add(getLaserEye());
        ornaments.add(getCamera());
        ornaments.add(getIronMan());
        ornaments.add(getMobile());
        ornaments.add(getMirror(TextureController.FRAME_CALLBACK_FILTER));
        ornaments.add(getMirror(TextureController.FRAME_CALLBACK_NO_FILTER));
        ornaments.add(getGlasses());
        ornaments.add(getLaptop());
        ornaments.add(getCrystalBall());
        ornaments.add(getInterview());
        return ornaments;
    }

    private static Ornament getNoOrnament() {
        Ornament ornament = new Ornament();
        ornament.setType(Ornament.MODEL_TYPE_NONE);
        ornament.setImgResId(R.drawable.ic_remove);
        return ornament;
    }

    private static Ornament getTigerNose() {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.tiger_nose_obj);
        model.setScale(0.002f);
        model.setOffset(0, -0.2f, 0.4f);
        model.setRotate(0.0f, 0.0f, 0.0f);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_tiger);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        ornament.setModelList(modelList);

        return ornament;
    }

    private static Ornament getVMask() {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.v_mask_obj);
        model.setScale(0.15f);
        model.setOffset(0, 0.01f, 0.0f);
        model.setRotate(0, 0, 0);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_v_mask);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        ornament.setModelList(modelList);

        return ornament;
    }

    private static Ornament getLaserEye() {
        Ornament ornament = new Ornament();
        List<Object3D> object3DList = new ArrayList<>();
        List<List<IMaterialPlugin>> materialList = new ArrayList<>();

        List<IMaterialPlugin> laserPlugins = new ArrayList<>();
        laserPlugins.add(new CustomVertexShaderMaterialPlugin(0.35f));
        laserPlugins.add(new CustomMaterialPlugin());

        List<IMaterialPlugin> spherePlugins = new ArrayList<>();
        spherePlugins.add(new CustomVertexShaderMaterialPlugin(0.15f));
        spherePlugins.add(new CustomMaterialPlugin());

        float radius = 0.05f;
        float height = 2.0f;
        float posX = 0.225f;
        float posY = 0.175f;
        float posZ = 1.35f;

        // 左眼光柱
        Object3D laserLeft = new NPrism(24, radius, radius, height);
        laserLeft.setRotation(0, 0, -90);
        laserLeft.setPosition(-posX, posY, posZ);

        // 左眼光球
        Object3D sphereLeft = new Sphere(radius * 1.2f, 60, 60);
        sphereLeft.setPosition(-posX, posY, posZ + height - height * 0.4f);

        // 右眼光柱
        Object3D laserRight = new NPrism(24, radius, radius, height);
        laserRight.setRotation(0, 0, -90);
        laserRight.setPosition(posX, posY, posZ);

        // 右眼光球
        Object3D sphereRight = new Sphere(radius * 1.2f, 60, 60);
        sphereRight.setPosition(posX, posY, posZ + height - height * 0.4f);

        object3DList.add(laserLeft);
        object3DList.add(sphereLeft);
        object3DList.add(laserRight);
        object3DList.add(sphereRight);

        materialList.add(laserPlugins);
        materialList.add(spherePlugins);
        materialList.add(laserPlugins);
        materialList.add(spherePlugins);

        ornament.setType(Ornament.MODEL_TYPE_SHADER);
        ornament.setImgResId(R.drawable.ic_laser);
        ornament.setObject3DList(object3DList);
        ornament.setMaterialList(materialList);
        ornament.setTimeStep(2.5f);

        return ornament;
    }

    public static Ornament getCamera() {
        Ornament ornament = new Ornament();

        Ornament.Model camera = new Ornament.Model();
        camera.setModelResId(R.raw.camera_obj);
        camera.setScale(0.2f);
        camera.setOffset(0, -0.01f, -0.5f);
        camera.setRotate(0, 0, 0);

        Ornament.Model leftHand = new Ornament.Model();
        leftHand.setModelResId(R.raw.left_hand_obj);
        leftHand.setScale(0.2f);
        leftHand.setOffset(0, -0.01f, -0.5f);
        leftHand.setRotate(0, 0, 0);

        Ornament.Model rightHand = new Ornament.Model();
        rightHand.setModelResId(R.raw.right_hand_obj);
        rightHand.setScale(0.2f);
        rightHand.setOffset(0, -0.01f, -0.5f);
        rightHand.setRotate(0, 0, 0);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_camera);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(camera);
        modelList.add(leftHand);
        modelList.add(rightHand);
        ornament.setModelList(modelList);

        ornament.setHasShaderPlane(true);
        ornament.setVertResId(R.raw.flash_out_vert_shader);
        ornament.setFragResId(R.raw.flash_out_frag_shader);
        ornament.setTimeStep(0.01f);
        ornament.setTimePeriod(0.125f);
        ornament.setPlaneOffsetZ(0.6f);

        return ornament;
    }

    private static Ornament getIronMan() {
        Ornament ornament = new Ornament();

        Ornament.Model ironManTop = new Ornament.Model();
        ironManTop.setName("ironManTop");
        ironManTop.setModelResId(R.raw.iron_man_helmet_top_obj);
        ironManTop.setScale(0.75f);
        ironManTop.setOffset(0, -0.5f, 0);
        // for object pick
        ironManTop.setNeedObjectPick(true);
        ironManTop.setBeforeY(-0.5f);
        ironManTop.setAfterY(-0.15f);
        ironManTop.setBeforeZ(0);
        ironManTop.setAfterZ(0.5f);
        ironManTop.setAxisX(1);
        ironManTop.setBeforeAngle(0);
        ironManTop.setAfterAngle(40);

        Ornament.Model ironManBottom = new Ornament.Model();
        ironManBottom.setName("ironManBottom");
        ironManBottom.setModelResId(R.raw.iron_man_helmet_bottom_obj);
        ironManBottom.setScale(0.75f);
        ironManBottom.setOffset(0, -0.5f, 0);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_iron_man);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(ironManTop);
        modelList.add(ironManBottom);
        ornament.setModelList(modelList);
        ornament.setToastMsg("点击头盔");

        return ornament;
    }

    private static Ornament getMobile() {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.mobile_obj);
        model.setScale(0.125f);
        model.setOffset(0, 0.05f, 0.5f);
        model.setRotate(0, 0, 0);

        model.setNeedStreaming(true);
        model.setStreamingViewWidth(216);
        model.setStreamingViewHeight(384);
        model.setStreamingModelWidth(6.25f);
        model.setStreamingModelHeight(11.1f);
        model.setStreamingScale(1.0f);
        model.setStreamingOffsetY(0.1f);
        model.setStreamingOffsetZ(0.85f);

        Ornament.Model rightHand = new Ornament.Model();
        rightHand.setModelResId(R.raw.hand_hold_mobile_obj);
        rightHand.setScale(0.125f);
        rightHand.setOffset(0, 0.05f, 0.5f);
        rightHand.setRotate(0, 0, 0);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_mobile);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        modelList.add(rightHand);
        ornament.setModelList(modelList);

        return ornament;
    }

    private static Ornament getMirror(int frameCallbackType) {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.magic_mirror_obj);
        model.setScale(0.35f);
        model.setOffset(0, 0, -0.5f);
        model.setRotate(0, 0, 0);

        model.setNeedStreaming(true);
        model.setStreamingViewWidth(200);
        model.setStreamingViewHeight(200);
        model.setStreamingModelWidth(3.5f);
        model.setStreamingModelHeight(3.5f);
        model.setStreamingScale(1.0f);
        model.setStreamingOffsetX(0.03f);
        model.setStreamingOffsetY(0.05f);
        model.setStreamingOffsetZ(2.22f);
        model.setAlphaMapResId(R.drawable.circle_alpha);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_mirror);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        ornament.setModelList(modelList);
        ornament.setEnableRotation(false);
        ornament.setEnableScale(false);
        ornament.setFrameCallbackType(frameCallbackType);
        ornament.setSelectFilterId(R.id.menu_camera_negative);

        return ornament;
    }

    private static Ornament getGlasses() {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.glasses_obj);
        model.setScale(0.15f);
        model.setOffset(0.01f, 0.05f, -0.1f);
        model.setRotate(0, 0, 0);

        model.setNeedStreaming(true);
        model.setStreamingViewWidth(800);
        model.setStreamingViewHeight(800);
        model.setStreamingModelWidth(6);
        model.setStreamingModelHeight(6);
        model.setStreamingScale(1.0f);
        model.setStreamingOffsetX(0.01f);
        model.setStreamingOffsetY(0.17f);
        model.setStreamingOffsetZ(0.40f);
        model.setAlphaMapResId(R.drawable.glasses_alpha);
        model.setStreamingViewType(Ornament.Model.STREAMING_WEB_VIEW);
        model.setStreamingViewMirror(true);
        model.setStreamingModelTransparent(true);
        model.setStreamingTextureInfluence(0.6f);
        model.setColorInfluence(0.4f);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_glasses);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        ornament.setModelList(modelList);
        ornament.setFrameCallbackType(TextureController.FRAME_CALLBACK_DISABLE);
        ornament.setToastMsg("镜面显示浏览器画面");

        return ornament;
    }

    private static Ornament getLaptop() {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.laptop_obj);
        model.setScale(0.2f);
        model.setOffset(0, -0.4f, 0.0f);
        model.setRotate(0, 0, -15);

        model.setNeedStreaming(true);
        model.setStreamingViewWidth(450);
        model.setStreamingViewHeight(300);
        model.setStreamingModelWidth(10.7f);
        model.setStreamingModelHeight(7);
        model.setStreamingScale(1.0f);
        model.setStreamingOffsetY(0.7f);
        model.setStreamingOffsetZ(0.0f);
        model.setStreamingRotateZ(-15);

        Ornament.Model desk = new Ornament.Model();
        desk.setModelResId(R.raw.desk_obj);
        desk.setScale(0.225f);
        desk.setOffset(0, -0.5f, 0.0f);
        desk.setRotate(0, 0, -15);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_laptop);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        modelList.add(desk);
        ornament.setModelList(modelList);
        ornament.setEnableRotation(false);
        ornament.setEnableScale(false);
        ornament.setEnableTransition(false);
        ornament.setFrameCallbackType(TextureController.FRAME_CALLBACK_ONLY);

        return ornament;
    }

    private static Ornament getCrystalBall() {
        Ornament ornament = new Ornament();

        Ornament.Model baseRing = new Ornament.Model();
        baseRing.setModelResId(R.raw.crystal_ball_base_ring_obj);
        baseRing.setScale(0.15f);
        baseRing.setOffset(0, -0.5f, 0.0f);
        baseRing.setRotate(0, 0, 0);
        baseRing.setColor(Color.YELLOW);
        baseRing.setMaterialId(MaterialFactory.MATERIAL_SKY_CUBE);

        Ornament.Model baseLeg = new Ornament.Model();
        baseLeg.setModelResId(R.raw.crystal_ball_base_leg_obj);
        baseLeg.setScale(0.15f);
        baseLeg.setOffset(0, -0.5f, 0.0f);
        baseLeg.setRotate(0, 0, 0);
        baseLeg.setColor(Color.YELLOW);
        baseLeg.setMaterialId(MaterialFactory.MATERIAL_SKY_CUBE);

//        Ornament.Model crystalBall = new Ornament.Model();
//        crystalBall.setBuildInType(Ornament.Model.BUILD_IN_SPHERE);
//        crystalBall.setBuildInWidth(1);
//        crystalBall.setBuildInHeight(1);
//        crystalBall.setBuildInSegmentsW(24);
//        crystalBall.setBuildInSegmentsH(24);
//        crystalBall.setScale(0.7f);
//        crystalBall.setOffset(0, 0.01f, 0);
//        crystalBall.setRotate(0, 0, 0);
//        crystalBall.setColor(Color.BLUE);
//        crystalBall.setMaterialId(MaterialFactory.MATERIAL_FROST_CUBE);

        baseRing.setNeedStreaming(true);
        baseRing.setStreamingModelType(Ornament.Model.STREAMING_SPHERE_MODEL);
        baseRing.setStreamingViewWidth(400);
        baseRing.setStreamingViewHeight(400);
        baseRing.setStreamingModelWidth(4.5f);
        baseRing.setStreamingModelHeight(4.5f);
        baseRing.setStreamingModelSegmentsW(24);
        baseRing.setStreamingModelSegmentsH(24);
        baseRing.setStreamingScale(1.0f);
        baseRing.setStreamingOffsetX(0);
        baseRing.setStreamingOffsetY(0);
        baseRing.setStreamingOffsetZ(0);
        baseRing.setStreamingViewMirror(true);
        baseRing.setStreamingModelTransparent(true);
        baseRing.setStreamingTextureInfluence(0.9f);
        baseRing.setColorInfluence(0.1f);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_crystal_ball);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(baseRing);
        modelList.add(baseLeg);
//        modelList.add(crystalBall);
        ornament.setModelList(modelList);
        ornament.setFrameCallbackType(TextureController.FRAME_CALLBACK_FILTER);
        ornament.setSelectFilterId(R.id.menu_camera_negative);

        return ornament;
    }

    private static Ornament getInterview() {
        Ornament ornament = new Ornament();

        Ornament.Model glasses = new Ornament.Model();
        glasses.setModelResId(R.raw.sun_glasses_obj);
        glasses.setScale(0.15f);
        glasses.setOffset(0.015f, 0.1f, -0.1f);
        glasses.setRotate(0, 0, 0);

        Ornament.Model mic1 = new Ornament.Model();
        mic1.setModelResId(R.raw.microphone_obj);
        mic1.setScale(0.17f);
        mic1.setOffset(0, -0.1f, 0);
        mic1.setRotate(0, 0, 0);

        Ornament.Model mic2 = new Ornament.Model();
        mic2.setModelResId(R.raw.microphone_obj);
        mic2.setScale(0.17f);
        mic2.setOffset(-0.025f, 0.05f, -0.02f);
        mic2.setRotate(0, 30, 0);

        Ornament.Model mic3 = new Ornament.Model();
        mic3.setModelResId(R.raw.microphone_obj);
        mic3.setScale(0.17f);
        mic3.setOffset(0.025f, 0.015f, -0.01f);
        mic3.setRotate(0, -30, 0);

        ornament.setType(Ornament.MODEL_TYPE_STATIC);
        ornament.setImgResId(R.drawable.ic_interview);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(glasses);
        modelList.add(mic1);
        modelList.add(mic2);
        modelList.add(mic3);
        ornament.setModelList(modelList);

        ornament.setHasShaderPlane(true);
        ornament.setVertResId(R.raw.flash_light_vert_shader);
        ornament.setFragResId(R.raw.flash_light_frag_shader);
        ornament.setTimeStep(0.05f);
        ornament.setTimePeriod(0.5f);
        ornament.setPlaneOffsetZ(0);

        return ornament;
    }

    public static List<Ornament> getPresetMask() {
        List<Ornament> ornaments = new ArrayList<>();
        ornaments.add(getMask(R.drawable.average_male, R.drawable.mask_man, true));
        ornaments.add(getMask(R.drawable.average_female, R.drawable.mask_woman, true));
        ornaments.add(getMask(R.drawable.female_virtual_makeup, R.drawable.mask_makeup, false));
        ornaments.add(getMask(R.drawable.lion_texture, R.drawable.mask_lion, false));
        ornaments.add(getMask(R.drawable.skull_texture, R.drawable.mask_skull, false));
        return ornaments;
    }

    private static Ornament getMask(int textureResId, int imgResId, boolean needSkinColor) {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(R.raw.base_face_uv3_obj);
        model.setTextureResId(textureResId);
        model.setScale(0.25f);
        model.setOffset(0, 0, 0);
        model.setRotate(0, 0, 0);
        model.setColor(NO_COLOR);
        model.setNeedSkinColor(needSkinColor);

        ornament.setType(Ornament.MODEL_TYPE_DYNAMIC);
        ornament.setImgResId(imgResId);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        ornament.setModelList(modelList);
        return ornament;
    }

    public static Ornament getMask(String texturePath) {
        Ornament ornament = new Ornament();

        Ornament.Model model = new Ornament.Model();
        model.setModelResId(-1);
        model.setTexturePath(texturePath);
        model.setScale(0.25f);
        model.setOffset(0, 0, 0);
        model.setRotate(0, 0, 0);
        model.setColor(NO_COLOR);
        model.setNeedSkinColor(true);

        ornament.setType(Ornament.MODEL_TYPE_DYNAMIC);
        ornament.setImgResId(0);
        List<Ornament.Model> modelList = new ArrayList<>();
        modelList.add(model);
        ornament.setModelList(modelList);
        return ornament;
    }
}
