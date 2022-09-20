package com.chihun.learn.seetafacedemo;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;


import com.pcyfox.libseeta.seeta.DrawerView;
import com.pcyfox.libseeta.seeta.FaceRecognizer;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRecognizerActivity extends AppCompatActivity {

    private static final String TAG = FaceRecognizerActivity.class.getSimpleName();

    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat mRgba;

    private FaceRecognizer mFaceRecognizer;
    private volatile boolean isLoadedEngine = false;
    private ExecutorService threadPool;
    private DrawerView drawerView;


    private void loadEngine() {
        mFaceRecognizer = FaceRecognizer.getInstance();
        mFaceRecognizer.setResultCallback(drawerView);
        threadPool.submit(
                () -> {
                    //在这里调用所有需要提前初始化的native方法
                    mFaceRecognizer.loadEngine(this, 0.65f, 0.75f);
                    mFaceRecognizer.registerFace(this);
                    isLoadedEngine = true;
                }
        );
    }


    private final CameraBridgeViewBase.CvCameraViewListener2 mCvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {

        @Override
        public void onCameraViewStarted(int width, int height) {
            Log.d(TAG, "onCameraViewStarted() called with: width = [" + width + "], height = [" + height + "]");
            mFaceRecognizer.stopRecognize(false);
            cameraBridgeViewBase.enableView();

            mRgba = new Mat(height, width, CvType.CV_8UC4);

            drawerView.post(() -> {
                ViewGroup.LayoutParams lp = drawerView.getLayoutParams();
                lp.width = width;
                lp.height = height;
                drawerView.setLayoutParams(lp);
            });
        }

        @Override
        public void onCameraViewStopped() {
            Log.d(TAG, "onCameraViewStopped() called");
            mFaceRecognizer.stopRecognize(true);
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            mRgba = inputFrame.rgba();
            if (!isLoadedEngine || mFaceRecognizer.isRecognizingFace() || !cameraBridgeViewBase.isEnabled()) {
                return mRgba;
            }
            threadPool.submit(() -> {
                //在这里调用处理每一张frame的native方法 记得在方法中传入的是long型的
                if (cameraBridgeViewBase.isEnabled() && isLoadedEngine) {
                    mFaceRecognizer.recognize(mRgba.getNativeObjAddr());
                }
            });
            return mRgba;
        }
    };

    private void release() {
        Log.d(TAG, "release() called");
        isLoadedEngine = false;
        mFaceRecognizer.stopRecognize(true);
        //在这里析构和释放所有之前初始化和分配内存的native方法
        threadPool.execute(() -> {
            while (!mFaceRecognizer.isRecognizingFace() && mFaceRecognizer.isIsLoaded()) {
                mFaceRecognizer.releaseEngine();
            }
        });
        if (mRgba != null) mRgba.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_face_recognizer);
        threadPool = Executors.newSingleThreadExecutor();
        initView();
        loadEngine();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disableCamera();
        mFaceRecognizer.stopRecognize(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
    }

    @Override
    public void onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow() called");
        super.onDetachedFromWindow();
    }

    private void initView() {
        drawerView = findViewById(R.id.drawer);
        cameraBridgeViewBase = findViewById(R.id.jCameraView);

        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(mCvCameraViewListener2);

        //在这里设置图像的大小，在手机中图像需要横屏状态，图片太大的话会卡顿
        cameraBridgeViewBase.postDelayed(() -> {
            cameraBridgeViewBase.setMaxFrameSize(cameraBridgeViewBase.getWidth(), cameraBridgeViewBase.getHeight());
        }, 50);

        cameraBridgeViewBase.setOnLongClickListener(v -> {
            finish();
            return false;
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().getDecorView().postDelayed(() -> {
            if (cameraBridgeViewBase != null)
                cameraBridgeViewBase.enableView();
        }, 100);
    }


    public void disableCamera() {
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }
}
