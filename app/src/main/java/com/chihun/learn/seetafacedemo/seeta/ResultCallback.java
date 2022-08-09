package com.chihun.learn.seetafacedemo.seeta;

public interface ResultCallback {
    void onFaceRect(int x, int y, int w, int h);


    void onPoints(int num, int[] point);


    void onRecognize(float similarity, String file);
}
