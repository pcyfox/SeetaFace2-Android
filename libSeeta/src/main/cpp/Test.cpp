//
// Created by 潘城尧 on 2022/8/8.
//

#include "include/Test.h"

static Test *test;
Test::Test() {

}

Test::~Test() {

}

Test *Test::GetInstance() {
    if (test == nullptr) {
        test = new Test;
    }
    return test;
}
