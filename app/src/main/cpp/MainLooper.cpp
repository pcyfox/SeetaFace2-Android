#include <unistd.h>
#include <thread>

#include "include/AndroidLog.h"
#include "include/MainLooper.h"

#define LOOPER_MSG_LENGTH 12

MainLooper *MainLooper::g_MainLooper = nullptr;

static void *data = nullptr;

static void (*callBack)(char *, void *);

MainLooper *MainLooper::GetInstance() {
    if (!g_MainLooper) {
        g_MainLooper = new MainLooper();
    }
    return g_MainLooper;
}

MainLooper::MainLooper() {
    pthread_mutex_init(&looper_mutex_, nullptr);
}

MainLooper::~MainLooper() {
    if (looper && readPipe != -1) {
        ALooper_removeFd(looper, readPipe);
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
    int msgPipe[2];
    pipe(msgPipe);
    readPipe = msgPipe[0];
    writePipe = msgPipe[1];
    looper = ALooper_prepare(0);
    int ret = ALooper_addFd(looper,
                            readPipe,
                            119,
                            ALOOPER_EVENT_INPUT,
                            handleMessage,
                            data);
    if (ret < 0) {
        LOGE("add fd error!");
    }
}


int MainLooper::handleMessage(int fd, int events, void *userData) {
    LOGD("handleMessage() called fd=%d,events=%d\n", fd, events);
    if (callBack) {
        char buffer[LOOPER_MSG_LENGTH];
        memset(buffer, 0, LOOPER_MSG_LENGTH);
        read(fd, buffer, sizeof(buffer));
        callBack(buffer, data);
    }
    return 1;
}

void MainLooper::send(const char *msg, void *sendDta) {
    pthread_mutex_lock(&looper_mutex_);
    data = sendDta;
    LOGD("send msg %s", msg);
    write(writePipe, msg, strlen(msg));
    pthread_mutex_unlock(&looper_mutex_);
}


void MainLooper::release() {
    if (looper && readPipe != -1) {
        ALooper_removeFd(looper, readPipe);
        ALooper_release(looper);
        looper = nullptr;
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

void MainLooper::setCallback(void (*cb)(char *, void *)) {
    callBack = cb;
}
