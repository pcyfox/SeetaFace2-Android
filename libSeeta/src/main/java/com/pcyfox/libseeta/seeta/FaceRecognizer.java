package com.pcyfox.libseeta.seeta;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognizer implements ResultCallback {

    private static final String TAG = FaceRecognizer.class.getSimpleName();
    private ResultCallback resultCallback;
    private static final String BASE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "seeta";

    private volatile boolean isLoaded;
    private final static FaceRecognizer INSTANCE = new FaceRecognizer();


    static {
        System.loadLibrary("facerecognize");
    }

    private FaceRecognizer() {

    }

    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void onFaceRect(int x, int y, int w, int h) {
        //Log.d(TAG, "onFaceRect() called with: x = [" + x + "], y = [" + y + "], w = [" + w + "], h = [" + h + "]");
        if (resultCallback != null) {
            resultCallback.onFaceRect(x, y, w, h);
        }
    }

    @Override
    public void onPoints(int num, int[] points) {
//        Log.d(TAG, "onPoints() called with: num = [" + num + "], points = [" + points.length + "]");
        if (resultCallback != null) {
            resultCallback.onPoints(num, points);
        }
    }

    @Override
    public void onRecognize(float similarity, String file) {
        //Log.d(TAG, "onRecognize() called with: similarity = [" + similarity + "], file = [" + file + "]");
        if (resultCallback != null) {
            resultCallback.onRecognize(similarity, file);
        }
    }


    public static FaceRecognizer getInstance() {
        return INSTANCE;
    }

    public void setResultCallback(ResultCallback resultCallback) {
        this.resultCallback = resultCallback;
    }

    /**
     * 初始化引擎，加载模式文件
     */
    public boolean loadEngine(String detectModelFile, String markerModelFile, String recognizeModelFile, float threshold, float minSimilarity) {
        if (isLoaded) {
            Log.w(TAG, "loadEngine: fail,  engine is loaded!");
            return false;
        }
        Log.d(TAG, "loadEngine() called with: detectModelFile = [" + detectModelFile + "], markerModelFile = [" + markerModelFile + "], recognizeModelFile = [" + recognizeModelFile + "]");
        if (null == detectModelFile || "".equals(detectModelFile)) {
            Log.w(TAG, "detectModelFile file path is invalid!");
            return false;
        }
        if (null == markerModelFile || "".equals(markerModelFile)) {
            Log.w(TAG, "markerModelFile file path is invalid!");
            return false;
        }
        if (null == recognizeModelFile || "".equals(recognizeModelFile)) {
            Log.w(TAG, "recognizeModelFile file path is invalid!");
            return false;
        }
        initCallback();
        int ret = initNativeEngine(detectModelFile, markerModelFile, recognizeModelFile, threshold, minSimilarity);
        isLoaded = ret == 0;
        return isLoaded;
    }

    public boolean loadEngine(Context context, float threshold, float minSimilarity) {
        return loadEngine(getPath(context, "fd_2_00.dat"), getPath(context, "pd_2_00_pts5.dat"), getPath(context, "fr_2_10.dat"), threshold, minSimilarity);
    }

    public void registerFace(Context context) {
        List<String> list = FaceRecognizer.getAssetsPath(context, "image");
        if (null == list || list.isEmpty()) {
            Log.w(TAG, "face list is empty!");
            return;
        }
        Log.d(TAG, "registerFace() called");
        registerFace(list);
    }


    public int registerFace(List<String> images) {
        if (!isLoaded) {
            Log.e(TAG, "registerFace() called fail, engine is not load");
            return 0;
        }

        if (null == images || images.isEmpty()) {
            Log.w(TAG, "face list is empty!");
            return 0;
        }
        Log.d(TAG, "registerFace() called images size:" + images.size());
        int count = nativeRegisterFace(images);
        if (count < images.size()) {
            Log.w(TAG, "registerFace: not register all image!, register success count=" + count);
        }
        return count;
    }

    /**
     * 检测图片
     *
     * @param rgbaddr 图片数据内存地址
     * @return 识别结果
     */
    public void recognize(long rgbaddr) {
        nativeRecognition(rgbaddr);
    }


    public void stopRecognize(boolean isStop) {
        nativeStopRecognize(isStop);
    }

    //该函数主要用来完成载入外部模型文件时，获取文件的路径加文件名
    public static String getPath(Context context, String file) {
        String sdcardModelPath = isSdcardAssetFileExist("model", file);
        if (null != sdcardModelPath) {
            return sdcardModelPath;
        } else {
            AssetManager assetManager = context.getAssets();
            BufferedInputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(assetManager.open(file));
                byte[] data = new byte[inputStream.available()];
                inputStream.read(data);
                inputStream.close();
                File outFile = new File(context.getFilesDir(), file);
                FileOutputStream os = new FileOutputStream(outFile);
                os.write(data);
                os.close();
                return outFile.getAbsolutePath();
            } catch (IOException ex) {
                Log.i(TAG, "Failed to upload a file");
            } finally {
                if (null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return "";
        }
    }

    public static List<String> getAssetsPath(Context context, String dir) {
        AssetManager assetManager = context.getAssets();
        List<String> list = null;
        String[] fileNames = null;
        try {
            fileNames = assetManager.list(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != fileNames && fileNames.length > 0) {
            list = new ArrayList<>(fileNames.length);
            for (String fileName : fileNames) {
                Log.d(TAG, "fileName: " + fileName);
                BufferedInputStream inputStream = null;
                try {
                    inputStream = new BufferedInputStream(assetManager.open(dir + File.separator + fileName));
                    byte[] data = new byte[inputStream.available()];
                    inputStream.read(data);
                    inputStream.close();
                    File outFile = new File(context.getFilesDir(), fileName);
                    FileOutputStream os = new FileOutputStream(outFile);
                    os.write(data);
                    os.close();
                    list.add(outFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (null != inputStream) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.d(TAG, "copy file finish!!!");
        } else {
            Log.d(TAG, "no file!!!");
        }
        return list;
    }

    public final static String isSdcardAssetFileExist(String folder, String fileName) {
        String dir = BASE_DIR;
        if (!TextUtils.isEmpty(folder)) {
            dir += File.separator + folder;
        }
        File file = new File(dir, fileName);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    /**
     * 释放引擎
     */
    public void releaseEngine() {
        releaseNativeEngine();
        isLoaded = false;
    }

    public int clearFace(int id) {
        return nativeClearFace(id);
    }

    public boolean isRecognizingFace() {
        return isRecognizing() == 1;
    }

    //人脸检测的三个native函数
    private native int initNativeEngine(String detectModelFile, String markerModelFile, String recognizeModelFile, float threshold, float minSimilarity);

    private native int nativeRegisterFace(List<String> facePaths);

    private native int nativeClearFace(int id);

    private native int nativeRecognition(long addr);

    private native int nativeStopRecognize(boolean isStop);

    private native int releaseNativeEngine();


    private native int initCallback();


    private native int isRecognizing();
}
