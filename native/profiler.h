#pragma once
#include "jvmti-pp.h"
#include "file-manager.h"

// log output
ostream& log();

// debug output, if not NULL
extern ostream* debug;

// the file to capture profiling outputs
extern FileManager* file;

inline ostream& operator << ( ostream& os, string msg ) {
	return os << msg.c_str();
}

// stream manipulator to write the current win32 error code
inline ostream& win32errorCode( ostream& os ) {
	DWORD e = GetLastError();
	os << " Err:" << e << "(";

	LPTSTR lpMsgBuf;

	FormatMessage(
		FORMAT_MESSAGE_ALLOCATE_BUFFER|FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS,
	    NULL,
	    e,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		(LPTSTR)&lpMsgBuf,
		0,
		NULL
	);

	os << lpMsgBuf << ")";

	LocalFree(lpMsgBuf);

	return os;
}