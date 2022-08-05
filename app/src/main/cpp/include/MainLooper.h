//
// Created by 潘城尧 on 2022/8/5.
//

#ifndef SEETAFACE2_MAINLOOPER_H
#define SEETAFACE2_MAINLOOPER_H


#include <android/looper.h>
#include <string>

class MainLooper
{
public:
    static MainLooper *GetInstance();
    ~MainLooper();
    void init();
    void send(const char* msg);

private:
    static MainLooper *g_MainLooper;
    MainLooper();
    ALooper* mainlooper;
    int readpipe;
    int writepipe;
    pthread_mutex_t looper_mutex_;
    static int handle_message(int fd, int events, void *data);
};

#endif //SEETAFACE2_MAINLOOPER_H
