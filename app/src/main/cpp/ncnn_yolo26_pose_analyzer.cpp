#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/asset_manager_jni.h>
#include <android/bitmap.h>

#include "net.h"
#include "pose_decoder.h"
#include "pose_types.h"
#include "nms.h"

#include "gpu.h"


#define TAG "JNI_YOLO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static ncnn::Net yolo;
static bool modelLoaded = false;
static std::string currentDelegate = "CPU";

//---------------------------Init model Native---
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_touchlessdroid_data_repository_NCNNPoseRepository_initModelNative(
                 JNIEnv *env,
                 jobject /* this */,
                 jobject assetManagerObj)
{
    LOGD("initModelNative called");
    if (modelLoaded)
    {
        LOGD("model already loaded");
        return JNI_TRUE;
    }

    // Convert Java AssetManager → native AssetManager
    AAssetManager* assetManager =
            AAssetManager_fromJava(env, assetManagerObj);

    if (assetManager == nullptr)
    {
        LOGD("AAssetManager is null");
        return JNI_FALSE;
    }

    //vulkan
    ncnn::create_gpu_instance();
    yolo.opt.use_vulkan_compute = true;
    int ret1 = yolo.load_param(assetManager,"model.ncnn.param");
    int ret2 = yolo.load_model(assetManager,"model.ncnn.bin");

    if (ret1 != 0 || ret2 != 0)
    {
        LOGD("MODEL LOAD FAILED");
        return JNI_FALSE;
    }

    modelLoaded = true;
    LOGD("========== MODEL INFO ==========");
    LOGD("Input blob  : in0");
    LOGD("Output blob : out0");
    LOGD("Layers      : 202");
    LOGD("Blobs       : 242");
    LOGD("Quantized   : false (probably FP32)");
    LOGD("================================");
    LOGD("MODEL LOAD SUCCESS");
    return JNI_TRUE;
}


//-------------------------detect -----
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_touchlessdroid_data_repository_NCNNPoseRepository_detectNative(
        JNIEnv *env,
        jobject thiz,
        jobject bitmap)
{
    LOGD("detect called");

    //------------------------------------
    // Get bitmap information
    //------------------------------------

    AndroidBitmapInfo bitmapInfo;

    AndroidBitmap_getInfo(
            env,
            bitmap,
            &bitmapInfo
    );

    //------------------------------------
    // Access bitmap pixels
    //------------------------------------

    void* pixels = nullptr;

    AndroidBitmap_lockPixels(
            env,
            bitmap,
            &pixels
    );

    //------------------------------------
    // Convert Bitmap -> ncnn::Mat
    //------------------------------------

    ncnn::Mat input = ncnn::Mat::from_pixels(
            (const unsigned char*) pixels,
            ncnn::Mat::PIXEL_RGBA2RGB,
            bitmapInfo.width,
            bitmapInfo.height
    );
    LOGD("input w = %d", input.w);
    LOGD("input h = %d", input.h);
    LOGD("input c = %d", input.c);

    //------------------------------------
    // Release bitmap
    //------------------------------------

    AndroidBitmap_unlockPixels(
            env,
            bitmap
    );

    //------------------------------------
    // Normalize to 0-1
    //------------------------------------

    const float normValues[3] = {
            1.f / 255.f,
            1.f / 255.f,
            1.f / 255.f
    };

    input.substract_mean_normalize(
            nullptr,
            normValues
    );

    //------------------------------------
    // Run inference
    //------------------------------------

    ncnn::Extractor ex = yolo.create_extractor();

    ex.input("in0", input);

    ncnn::Mat out;

    ex.extract("out0", out);
    std::vector<PoseObject> poses =
            decodePoses(out, 0.25f);

    LOGD("before NMS, poses found = %zu", poses.size());

    std::vector<PoseObject> finalPoses =
            performNms(
                    poses,
                    0.45f);

    LOGD("after NMS, poses found = %zu", finalPoses.size());

    //------------------------------------
    // Flatten poses into float vector
    //------------------------------------

    std::vector<float> outputData;

    for (const auto& pose : finalPoses)
    {
        outputData.push_back(pose.x1);
        outputData.push_back(pose.y1);
        outputData.push_back(pose.x2);
        outputData.push_back(pose.y2);

        outputData.push_back(pose.score);

        for (const auto& kp : pose.keypoints)
        {
            outputData.push_back(kp.x);
            outputData.push_back(kp.y);
            outputData.push_back(kp.score);
        }
    }

//------------------------------------
// Convert vector<float> -> jfloatArray
//------------------------------------

    jfloatArray result =
            env->NewFloatArray(outputData.size());

    env->SetFloatArrayRegion(
            result,
            0,
            outputData.size(),
            outputData.data());

    return result;
}

//------------------------release------
extern "C"
JNIEXPORT void JNICALL
Java_com_example_touchlessdroid_data_repository_NCNNPoseRepository_releaseNative(
        JNIEnv *env, jobject thiz) {
    yolo.clear();
    modelLoaded = false;
    ncnn::destroy_gpu_instance();
    LOGD("model released");
}

//------------------------log gpu count------

extern "C"
JNIEXPORT void JNICALL
Java_com_example_touchlessdroid_MainActivity_logGpuInfo(
        JNIEnv* env,
        jobject thiz)
{
    int gpu_count = ncnn::get_gpu_count();

    LOGI("NCNN GPU count = %d", gpu_count);

    if (gpu_count > 0)
    {
        LOGI("Vulkan GPU is available");
    }
    else
    {
        LOGI("No Vulkan GPU detected");
    }
}

