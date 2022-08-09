package com.chihun.learn.seetafacedemo.seeta;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.Queue;

public class DrawerView extends View implements ResultCallback {
    private static final String TAG = "DrawerView";
    private Canvas canvas;

    private final Paint rectPaint;
    private Paint pointPaint;
    private Paint textPaint;


    private Queue<Rect> rectQueue = new LinkedList<>();


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
    }

    private float scale = 1f;

    public void setScale(float scale) {
        this.scale = scale;
    }

    private int resize(int src) {
        return (int) (src * scale);
    }

    @Override
    public void onFaceRect(int x, int y, int w, int h) {
        //Log.d(TAG, "onFaceRect() called with: x = [" + x + "], y = [" + y + "], w = [" + w + "], h = [" + h + "]");
        post(() -> {
            Rect rect = new Rect(resize(x), resize(y), resize(x + w), resize(y + h));
            rectQueue.add(rect);
            invalidate();
        });
    }

    @Override
    public void onPoints(int num, int[] point) {

    }

    @Override
    public void onRecognize(float similarity, String file) {

    }


    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(TAG, "onDraw() called with: canvas = [" + canvas + "]");
        this.canvas = canvas;
        Rect rect = rectQueue.poll();
        if (rect != null) {
            canvas.drawRect(rect, rectPaint);
        }
        super.onDraw(canvas);
    }
}
