#pragma once

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <crtdbg.h>
#include <winsock.h>
#pragma comment(lib, "ws2_32.lib")

// set your include directory to include $JAVA_HOME/include
// and $JAVA_HOME/include/<PLATFORM>
#include <jvmti.h>
#include <jni.h>
#include <jni_md.h>

#include <jvmpi.h>

// STL
#include <iostream>
#include <set>
#include <map>
#include <string>
#include <stack>
using namespace std;
