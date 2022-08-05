//
// Created by 潘城尧 on 2022/8/5.
//

#include "include/MainLooper.h"
#include <fcntl.h>
#include "include/MainLooper.h"
#include <stdint.h>
#include "string.h"
#include <stdlib.h>
#include <unistd.h>
#include <__threading_support>
#include <AndroidLog.h>

#define LOOPER_MSG_LENGTH 12

MainLooper *MainLooper::g_MainLooper = NULL;

MainLooper *MainLooper::GetInstance() {
    if (!g_MainLooper) {
        g_MainLooper = new MainLooper();
    }
    return g_MainLooper;
}

MainLooper::MainLooper() {
    pthread_mutex_init(&looper_mutex_, NULL);
}

MainLooper::~MainLooper() {
    if (mainLooper && readPipe != -1) {
        ALooper_removeFd(mainLooper, readPipe);
    }
    if (readPipe != -1) {
        close(readPipe);
    }
    if (writePipe != -1) {
        close(writePipe);
    }
    pthread_mutex_destroy(&looper_mutex_);
}

void MainLooper::init() {
    int msgpipe[2];
    pipe(msgpipe);
    readPipe = msgpipe[0];
    writePipe = msgpipe[1];
    mainLooper = ALooper_prepare(0);
    int ret = ALooper_addFd(mainLooper, readPipe, 1, ALOOPER_EVENT_INPUT,
                            MainLooper::handle_message, data);
    if (ret < 0) {
        LOGE("add fd error!");
    }
}


int MainLooper::handle_message(int fd, int events, void *data) {
    LOGD("handle_message() called fd=%d,events=%d\n", fd, events);
    char buffer[LOOPER_MSG_LENGTH];
    memset(buffer, 0, LOOPER_MSG_LENGTH);
    read(fd, buffer, sizeof(buffer));
    if (callBack) {
        callBack(buffer, data);
    }
    return 1;
}

void MainLooper::send(const char *msg, void *data) {
    pthread_mutex_lock(&looper_mutex_);
    MainLooper::data = data;
    LOGD("send msg %s", msg);
    write(writePipe, msg, strlen(msg));
    pthread_mutex_unlock(&looper_mutex_);
}


void MainLooper::release() {
    if (mainLooper && readPipe != -1) {
        ALooper_removeFd(mainLooper, readPipe);
        ALooper_release(mainLooper);
        mainLooper = nullptr;
    }
    if (readPipe != -1) {
        close(readPipe);
        readPipe = -1;
    }
    if (writePipe != -1) {
        close(writePipe);
        readPipe = -1;
    }
}
