package com.chihun.learn.seetafacedemo.seeta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class DrawerView extends View implements ResultCallback {
    private static final String TAG = "DrawerView";
    private final Paint rectPaint;
    private Paint pointPaint;
    private final Paint textPaint;
    private RecognizeResultCallback resultCallback;
    private float scale = 1f;
    private String detectFile;
    private float similarity;
    private boolean isShowRecognizedResult = true;
    private final Queue<Rect> rectQueue = new LinkedList<>();

    public DrawerView(@NonNull Context context) {
        super(context);
    }

    public DrawerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DrawerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    {
        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setColor(Color.GREEN);
        rectPaint.setStrokeWidth(2);

        textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(40);

    }

    public void setResultCallback(RecognizeResultCallback resultCallback) {
        this.resultCallback = resultCallback;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    private int resize(int src) {
        return (int) (src * scale);
    }

    public boolean isShowRecognizedResult() {
        return isShowRecognizedResult;
    }

    @Override
    public void onFaceRect(int x, int y, int w, int h) {
        post(() -> {
            Rect rect = new Rect(resize(x), resize(y), resize(x + w), resize(y + h));
            rectQueue.add(rect);
            if (x + w == 0) {
                onRecognizeFail();
            }
            invalidate();
        });
    }

    private void onRecognizeFail() {
        detectFile = "";
        similarity = 0;
    }


    @Override
    public void onPoints(int num, int[] point) {

    }

    @Override
    public void onRecognize(float similarity, String file) {
        this.similarity = similarity;
        File f = new File(file);
        detectFile = f.getName();
        if (resultCallback != null) {
            resultCallback.onRecognized(similarity, file);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        while (!rectQueue.isEmpty()) {
            Rect rect = rectQueue.poll();
            if (rect == null) {
                return;
            }
            canvas.drawRect(rect, rectPaint);
            if (similarity > 0 && isShowRecognizedResult) {
                canvas.drawText("s=" + similarity + "," + detectFile, rect.left, rect.top - 30, textPaint);
            }
        }
    }

    public interface RecognizeResultCallback {
        void onRecognized(float similarity, String file);
    }
}
