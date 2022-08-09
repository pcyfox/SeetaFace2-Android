package com.chihun.learn.seetafacedemo;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.chihun.learn.seetafacedemo.seeta.DrawerView;
import com.chihun.learn.seetafacedemo.seeta.FaceRecognizer;
import com.chihun.learn.seetafacedemo.seeta.ResultCallback;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRecognizerActivity extends AppCompatActivity {

    private static final String TAG = FaceRecognizerActivity.class.getSimpleName();

    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat mRgba;
    private Mat mGray;
    private FaceRecognizer mFaceRecognizer;
    private volatile boolean isLoadedEngine = false;
    private volatile boolean isRecognizing = false;
    private ExecutorService threadPool;
    private DrawerView drawerView;


    private void loadEngine() {
        mFaceRecognizer = FaceRecognizer.getInstance();
        mFaceRecognizer.setResultCallback(drawerView);
        threadPool.submit(
                () -> {
                    //在这里调用所有需要提前初始化的native方法
                    mFaceRecognizer.loadEngine();
                    mFaceRecognizer.registerFace();
                    isLoadedEngine = true;
                }
        );
    }


    private final CameraBridgeViewBase.CvCameraViewListener2 mCvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {


        @Override
        public void onCameraViewStarted(int width, int height) {
            Log.d(TAG, "onCameraViewStarted() called with: width = [" + width + "], height = [" + height + "]");
            mRgba = new Mat(height, width, CvType.CV_8UC4);
            mGray = new Mat(height, width, CvType.CV_8UC1);

            drawerView.post(() -> {
                ViewGroup.LayoutParams lp = drawerView.getLayoutParams();
                lp.width = cameraBridgeViewBase.getPreviewWidth();
                lp.height = cameraBridgeViewBase.getPreViewHeight();
                drawerView.setLayoutParams(lp);
            });
        }

        @Override
        public void onCameraViewStopped() {
            mRgba.release();
            mGray.release();

            //在这里析构和释放所有之前初始化和分配内存的native方法
            mFaceRecognizer.releaseEngine();
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

            mRgba = inputFrame.rgba();
            mGray = inputFrame.gray();

            if (!isLoadedEngine || isRecognizing) {
                return mRgba;
            }

//            isRecognizing = true;
//            mFaceRecognizer.recognize(mRgba.getNativeObjAddr());
//            isRecognizing = false;

            threadPool.submit(() -> {
                //在这里调用处理每一张frame的native方法 记得在方法中传入的是long型的
                isRecognizing = true;
                mFaceRecognizer.recognize(mRgba.getNativeObjAddr());
                isRecognizing = false;
            });

            return mRgba;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_face_recognizer);
        threadPool = Executors.newCachedThreadPool();
        initView();
        loadEngine();
    }

    private void initView() {
        drawerView = findViewById(R.id.drawer);
        cameraBridgeViewBase = findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(mCvCameraViewListener2);

        //在这里设置图像的大小，在手机中图像需要横屏状态，图片太大的话会卡顿
        cameraBridgeViewBase.setMaxFrameSize(640, 480);

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
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        disableCamera();
    }

    public void disableCamera() {
        if (cameraBridgeViewBase != null)
            cameraBridgeViewBase.disableView();
    }
}
