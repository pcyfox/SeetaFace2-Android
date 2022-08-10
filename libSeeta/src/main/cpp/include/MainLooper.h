//
// Created by 潘城尧 on 2022/8/5.
//

#ifndef SEETAFACE2_MAINLOOPER_H
#define SEETAFACE2_MAINLOOPER_H

#include <android/looper.h>
#include <string>

class MainLooper {
public:
    static MainLooper *GetInstance();

    ~MainLooper();

    void init();

    void release();

    void send(const char *msg, void *data);

    static void setCallback(void (*callBack)(char *, void *));


private:
    static MainLooper *g_MainLooper;

    ALooper *looper = nullptr;

    int readPipe = -1;
    int writePipe = -1;
    pthread_mutex_t looper_mutex_;

    MainLooper();

    static int handleMessage(int fd, int events, void *data);

};

#endif //SEETAFACE2_MAINLOOPER_H
