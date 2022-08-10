package com.chihun.learn.seetafacedemo.seeta;

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

import com.chihun.learn.seetafacedemo.MyApp;

public class FaceRecognizer implements ResultCallback {

    private static final String TAG = FaceRecognizer.class.getSimpleName();
    private ResultCallback resultCallback;

    private static final String BASE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "seeta";

    static {
        System.loadLibrary("facerecognize");
    }

    private FaceRecognizer() {

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


    private static class InstanceHolder {
        private final static FaceRecognizer INSTANCE = new FaceRecognizer();
    }

    public static FaceRecognizer getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void setResultCallback(ResultCallback resultCallback) {
        this.resultCallback = resultCallback;
    }

    /**
     * 初始化引擎，加载模式文件
     */
    public void loadEngine(String detectModelFile, String markerModelFile, String recognizeModelFile, float threshold, float minSimilarity) {
        Log.d(TAG, "loadEngine() called with: detectModelFile = [" + detectModelFile + "], markerModelFile = [" + markerModelFile + "], recognizeModelFile = [" + recognizeModelFile + "]");
        if (null == detectModelFile || "".equals(detectModelFile)) {
            Log.w(TAG, "detectModelFile file path is invalid!");
            return;
        }
        if (null == markerModelFile || "".equals(markerModelFile)) {
            Log.w(TAG, "markerModelFile file path is invalid!");
            return;
        }
        if (null == recognizeModelFile || "".equals(recognizeModelFile)) {
            Log.w(TAG, "recognizeModelFile file path is invalid!");
            return;
        }
        initCallback();
        initNativeEngine(detectModelFile, markerModelFile, recognizeModelFile, threshold, minSimilarity);
    }

    public void loadEngine(float threshold, float minSimilarity) {
        loadEngine(getPath("fd_2_00.dat"), getPath("pd_2_00_pts5.dat"), getPath("fr_2_10.dat"), threshold, minSimilarity);
    }

    public void registerFace() {
        List<String> list = FaceRecognizer.getAssetsPath("image");
        if (null == list || list.isEmpty()) {
            Log.w(TAG, "face list is empty!");
            return;
        }
        Log.d(TAG, "registerFace() called");
        nativeRegisterFace(list);
    }

    /**
     * 检测图片
     *
     * @param rgbaddr 图片数据内存地址
     * @return 识别结果
     */
    public void recognize(long rgbaddr) {
        long start = System.currentTimeMillis();
        nativeRecognition(rgbaddr);
    }

    //该函数主要用来完成载入外部模型文件时，获取文件的路径加文件名
    public static String getPath(String file) {
        String sdcardModelPath = isSdcardAssetFileExist("model", file);
        if (null != sdcardModelPath) {
            return sdcardModelPath;
        } else {
            Context context = MyApp.getInstance();
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

    public static List<String> getAssetsPath(String dir) {
        Context context = MyApp.getInstance();
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
    }

    //人脸检测的三个native函数
    private native int initNativeEngine(String detectModelFile, String markerModelFile, String recognizeModelFile, float threshold, float minSimilarity);

    private native int nativeRegisterFace(List<String> facePaths);

    private native int nativeRecognition(long addr);

    private native int releaseNativeEngine();

    private native int initCallback();
}
