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

#define LOOPER_MSG_LENGTH 81

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
    if (mainlooper && readpipe != -1) {
        ALooper_removeFd(mainlooper, readpipe);
    }
    if (readpipe != -1) {
        close(readpipe);
    }
    if (writepipe != -1) {
        close(writepipe);
    }
    pthread_mutex_destroy(&looper_mutex_);
}

void MainLooper::init() {
    int msgpipe[2];
    pipe(msgpipe);
    readpipe = msgpipe[0];
    writepipe = msgpipe[1];
    mainlooper = ALooper_prepare(0);
    int ret = ALooper_addFd(mainlooper, readpipe, 1, ALOOPER_EVENT_INPUT,
                            MainLooper::handle_message, NULL);
    if (ret < 0) {
        LOGE("add fd error!");
    }
}

int MainLooper::handle_message(int fd, int events, void *data) {
    LOGD("handle_message() called fd=%d,events=%d\n", fd, events);
    char buffer[LOOPER_MSG_LENGTH];
    memset(buffer, 0, LOOPER_MSG_LENGTH);
    read(fd, buffer, sizeof(buffer));
    return 1;
}