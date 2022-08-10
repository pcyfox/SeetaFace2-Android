package com.chihun.learn.seetafacedemo;

import android.app.Application;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


public class MyApp extends Application {

    private static final String TAG = MyApp.class.getSimpleName();

    private static MyApp instance;
    public static MyApp getInstance(){
        return instance;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, instance, new BaseLoaderCallback(instance) {
                @Override
                public void onManagerConnected(int status) {
                    if (status == LoaderCallbackInterface.SUCCESS) {
                        Log.i(TAG, "OpenCV loaded successfully");
                    } else {
                        super.onManagerConnected(status);
                        Log.i(TAG, "OpenCV loaded exception: " + status);
                    }
                }
            });
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
        }
    }

    static {
        System.loadLibrary("opencv_java3");
    }
}
