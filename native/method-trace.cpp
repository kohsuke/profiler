#include "stdafx.h"
#include "profiler.h"
#include "method-trace.h"
/*
	Each thread has its own MethodInfo stream for writing method enter/exit
	events.

	Those streams are allocated/terminated when a thread starts/ends.
*/



// ThreadWriters that are open
set<ThreadState*> threadStates;
// control the access to threadWriters
CCriticalSection threadStateLock;

bool profilingEnabledForNewThread = false;



inline jlong getCurrentTime() {
	DWORD l,h;
	__asm rdtsc;
	__asm mov h,edx;
	__asm mov l,eax;
	return (jlong(h)<<32)|l;
}



ThreadState::~ThreadState() {
	// if the System.exit method is used to shut down the VM,
	// we'll have a non-empty stack frame.
	// just mark the current time as the end time
	while(!callStack.empty()) {
		jlong endTime = getCurrentTime();
		StackFrame frame = callStack.top();
		callStack.pop();
		writeTime(endTime,frame.timeStamp,frame.methodId);
	}

	if(p!=NULL) {
		{
			CLock lock(&threadStateLock);
			threadStates.erase(this);
		}
		delete p;
	}
	p = NULL;
}

// create ThreadWriter for the given thread
ThreadWriter* ThreadState::doGetWriter(jthread thread) {
	if(p!=NULL)		return p;


	jvmtiThreadInfo ti;
	memset(&ti,0,sizeof(ti));
	const char* name = "(unknown)";

	jvmtiError err = jvmti->GetThreadInfo(thread,&ti);
	if(err) {
		log() << "Unable to get the thread info " << thread << ' ' << jvmti_error(err) << endl;
	} else {
		name = ti.name;
	}

	char* nameBuf = new char[strlen(name)+32];
	sprintf(nameBuf,"Thread %s",name);

	p = new ThreadWriter(*file,nameBuf,512*1024);

	delete nameBuf;

	if(ti.name!=NULL)
		jvmti->Deallocate((BYTE*)ti.name);

	return p;
}


void JNICALL threadStart(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if(debug)
		*debug << "Thread start " << thread << endl;

	ThreadState::init(thread);
}

void JNICALL threadEnd(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread)
{
	if(debug)
		*debug << "Thread end " << thread << endl;
}



void JNICALL methodEntry(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
								jthread thread, jmethodID method)
{
	ThreadState* ts = ThreadState::get(thread);
	if(!ts->enabled)		return;

	StackFrame frame;
	frame.methodId = method;
	frame.timeStamp = getCurrentTime();

	ts->callStack.push(frame);

	// 1bit: 1 + 31bit method ID
	DWORD data = DWORD(method) | 0x80000000;
	ts->getWriter(thread)->write(data);
}

void JNICALL methodExit(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
							   jthread thread, jmethodID method, jboolean was_popped_by_exception, jvalue return_value)
{
	ThreadState* ts = ThreadState::get(thread);
	if(!ts->enabled)		return;

	jlong endTime = getCurrentTime();

	if(ts->callStack.empty()) {
		// if we exit from the 'root' method, disable the profiling.
		ts->enabled = false;
		// we have nothing to record here, so just return now
		return;
	}

	StackFrame frame = ts->callStack.top();
	ts->callStack.pop();

	if(frame.methodId!=method) {
		log() << " ERROR : enter/exit out of sync" << endl;
	}

	ts->writeTime(endTime,frame.timeStamp,frame.methodId);

}

