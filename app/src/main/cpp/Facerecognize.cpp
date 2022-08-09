#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <sstream>
#include <seeta/FaceEngine.h>
#include <seeta/Struct_cv.h>
#include <seeta/Struct.h>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <array>
#include <map>
#include <iostream>
#include <android/looper.h>
#include <unistd.h>
#include <cstdio>

#include  "MainLooper.h"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , "Seeta", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , "Seeta", __VA_ARGS__)

static seeta::FaceEngine *FE = nullptr;
static std::map<int64_t, const char *> GalleryIndexMap;
static float threshold = 0.5f;

struct CallbackObject {
    jclass clazz;
    jobject thiz;
    jmethodID onFaceRectMID;
    jmethodID onPointsMID;
    jmethodID onRecognizeMID;
} typedef *Callback;

static Callback cb;
static JavaVM *vm;


int
handleFaces(JNIEnv *env, std::vector<SeetaFaceInfo> *faces, cv::Mat &frame,
            const seeta::cv::ImageData &image) {

    vm->AttachCurrentThread(&env, nullptr);

    if (faces->empty()) {
        env->CallNonvirtualVoidMethod(cb->thiz, cb->clazz, cb->onFaceRectMID, 0, 0, 0, 0);
        return EXIT_FAILURE;
    }

    for (SeetaFaceInfo &face: *faces) {
        if (nullptr == FE) {
            return EXIT_FAILURE;
        }
        // Query top 1
        int64_t index = -1;
        float similarity = 0;
        auto points = FE->DetectPoints(image, face);

        env->CallNonvirtualVoidMethod(cb->thiz, cb->clazz, cb->onFaceRectMID, face.pos.x,
                                      face.pos.y,
                                      face.pos.width, face.pos.height);

//        cv::rectangle(frame, cv::Rect(face.pos.x, face.pos.y, face.pos.width, face.pos.height),
//                      CV_RGB(128, 128, 255), 1);
        int pCount = 5;
        int size = pCount * 2;
        jint pArray[size];
        for (int i = 0; i < pCount; ++i) {
            auto &point = points[i];
            int x = (int) point.x;
            int y = (int) point.y;
            pArray[i << 1] = x;
            pArray[(i << 1) + 1] = y;
        }
        jintArray array = env->NewIntArray(size);
        env->SetIntArrayRegion(array, 0, size - 1, pArray);
        env->CallNonvirtualVoidMethod(cb->thiz, cb->clazz, cb->onPointsMID, pCount, array);
        env->ReleaseIntArrayElements(array, pArray, JNI_COMMIT);

        auto queried = FE->QueryTop(image, points.data(), 1, &index, &similarity);
        // no face queried from database
        if (queried < 1) continue;
        // similarity greater than threshold, means recognized
        LOGW("similarity: %f", similarity);
        if (similarity > threshold) {
            const char *name = GalleryIndexMap[index];
            LOGW(" find file name: %s", name);
            jstring jFileNmae = env->NewStringUTF(name);
            env->CallNonvirtualVoidMethod(cb->thiz, cb->clazz, cb->onRecognizeMID,
                                          (jfloat) similarity, jFileNmae);
        }
    }
    //  vm->DetachCurrentThread();
    return 1;
}


extern "C"
JNIEXPORT int JNICALL
Java_com_chihun_learn_seetafacedemo_seeta_FaceRecognizer_initNativeEngine(JNIEnv *env,
                                                                          jobject instance,
                                                                          jstring detectModelFile_,
                                                                          jstring markerModelFile_,
                                                                          jstring recognizeModelFile_) {
    const char *detectModelFile = env->GetStringUTFChars(detectModelFile_, 0);
    const char *markerModelFile = env->GetStringUTFChars(markerModelFile_, 0);
    const char *recognizeModelFile = env->GetStringUTFChars(recognizeModelFile_, 0);

    seeta::ModelSetting::Device device = seeta::ModelSetting::AUTO;
    int id = 0;

    seeta::ModelSetting FD_model(detectModelFile, device, id);
    seeta::ModelSetting PD_model(markerModelFile, device, id);
    seeta::ModelSetting FR_model(recognizeModelFile, device, id);

    FE = new seeta::FaceEngine(FD_model, PD_model, FR_model, 2, 16);

    //set face detector's min face size
    FE->FD.set(seeta::FaceDetector::PROPERTY_MIN_FACE_SIZE, 180);
    FE->FD.set(seeta::FaceDetector::PROPERTY_VIDEO_STABLE, 1);
    //set face detect threshold
    FE->FD.set(seeta::FaceDetector::PROPERTY_THRESHOLD1, 0.60f);

    int res = EXIT_SUCCESS;
    env->ReleaseStringUTFChars(detectModelFile_, detectModelFile);
    env->ReleaseStringUTFChars(markerModelFile_, markerModelFile);
    env->ReleaseStringUTFChars(recognizeModelFile_, recognizeModelFile);

    return (jint)
            res;
}

extern "C"
JNIEXPORT int JNICALL
Java_com_chihun_learn_seetafacedemo_seeta_FaceRecognizer_nativeRegisterFace(JNIEnv
                                                                            *env,
                                                                            jobject instance,
                                                                            jobject
                                                                            faceList) {

    if (NULL == FE) {
        LOGW("FE is NULL");
        return EXIT_FAILURE;
    }

    jclass jArrayList = env->GetObjectClass(faceList);
    jmethodID jArrayList_get = env->GetMethodID(jArrayList, "get", "(I)Ljava/lang/Object;");
    jmethodID jArrayList_size = env->GetMethodID(jArrayList, "size", "()I");
    jint len = env->CallIntMethod(faceList, jArrayList_size);
    LOGD("face len: %d", len);
    GalleryIndexMap.clear();

    for (int i = 0; i < len; i++) {
        auto filepath_ = (jstring) env->CallObjectMethod(faceList, jArrayList_get, i);
        const char *filepath = env->GetStringUTFChars(filepath_, 0);
        LOGD("filepath: %s", filepath);

        seeta::cv::ImageData image = cv::imread(filepath);
        auto id = FE->Register(image);
        LOGD("Registered id = %lld", id);
        if (id >= 0) {
            GalleryIndexMap.
                    insert(std::make_pair(id, filepath)
            );
        }
//env->ReleaseStringUTFChars(filepath_, filepath);
    }

    return EXIT_SUCCESS;
}



extern "C"
JNIEXPORT jint
JNICALL
Java_com_chihun_learn_seetafacedemo_seeta_FaceRecognizer_nativeRecognition(JNIEnv *env,
                                                                           jobject
                                                                           instance,
                                                                           jlong addr
) {
    if (nullptr == FE) {
        LOGW("FE is NULL");
        return EXIT_FAILURE;
    }

    cv::Mat &frame = *(cv::Mat *) addr;
    cv::Mat rgb_img;
    cv::cvtColor(frame, rgb_img, cv::COLOR_RGBA2BGR
    );
    seeta::cv::ImageData image = rgb_img;
// Detect all faces
    std::vector<SeetaFaceInfo> faces = FE->DetectFaces(image);
    handleFaces(env, &faces, frame, image);
    return EXIT_FAILURE;
}


extern "C"
JNIEXPORT jint
JNICALL
Java_com_chihun_learn_seetafacedemo_seeta_FaceRecognizer_releaseNativeEngine(JNIEnv *env,
                                                                             jobject
                                                                             instance) {
    delete FE;
    MainLooper::GetInstance()->release();
    return (jint) EXIT_SUCCESS;
}


extern "C"
JNIEXPORT jint
JNICALL
Java_com_chihun_learn_seetafacedemo_seeta_FaceRecognizer_initCallback(JNIEnv *env, jobject thiz) {
    env->GetJavaVM(&vm);
    jclass c = env->GetObjectClass(thiz);
    if (cb) {
        env->DeleteGlobalRef(cb->thiz);
        free(cb);
    }
    cb = (Callback) malloc(sizeof(CallbackObject));
    cb->clazz = c;
    cb->thiz = env->NewGlobalRef(thiz);
    cb->onFaceRectMID = env->GetMethodID(c, "onFaceRect", "(IIII)V");
    cb->onPointsMID = env->GetMethodID(c, "onPoints", "(I[I)V");
    cb->onRecognizeMID = env->GetMethodID(c, "onRecognize", "(FLjava/lang/String;)V");
    return 1;
}

