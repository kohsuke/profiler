#pragma once

void JNICALL methodEntry(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
								jthread thread, jmethodID method);

void JNICALL methodExit(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
							   jthread thread, jmethodID method, jboolean was_popped_by_exception, jvalue return_value);

void JNICALL threadStart(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread);

void JNICALL threadEnd(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread);



// true if the profiling should automatically be enabled
// when a new thread is created.
extern bool profilingEnabledForNewThread;

typedef VarDataWriter ThreadWriter;

class ThreadState;

// ThreadWriters that are open
extern set<ThreadState*> threadStates;
// control the access to threadStates
extern CCriticalSection threadStateLock;


struct StackFrame {
	jmethodID	methodId;
	jlong		timeStamp;		
};


// per-thread data structure
class ThreadState {
private:
	ThreadState() {
		p = NULL;
		enabled = profilingEnabledForNewThread;
	}

	// writer for the method enter/leave events.
	// lazily created because we can't get the thread info
	// when a thread is created.
	ThreadWriter* p;

	ThreadWriter* doGetWriter(jthread thread);

public:
	~ThreadState();

	// get the ThreadWriter
	inline ThreadWriter* getWriter(jthread thread) {
		if(p!=NULL)		return p;
		return doGetWriter(thread);
	}

	// true if the logging is enabled
	bool enabled;

	// call stack
	stack<StackFrame> callStack;

	inline void writeTime(jlong endTime, jlong startTime, jmethodID method) {
		// we must have the corresponding enter event,
		// so p is guaranteed to be created at that point.

		jlong time = endTime-startTime;
		if(time<0) {
			log() << " ERROR : negative duration " << method << ' ' << endTime << ' ' << startTime << ' ' << time << endl;
			time = 0;
		}
		// 1bit:0 + 1bit:0 + 30bit:time
		// or
		// 1bit:0 + 1bit:1 + 62bit:time
		if(time<0x40000000L) {
			p->write(DWORD(time));
		} else {
			p->write((DWORD(time>>32)&0x3FFFFFFF)|0x40000000);
			p->write(DWORD(time));
		}
	}


	// get the thread state for the given thread
	static ThreadState* get(jthread thread) {
		ThreadState* p;
		jvmti->GetThreadLocalStorage(thread,reinterpret_cast<void**>(&p));
		return p;
	}

	// initialize the thread state for the given thread
	static ThreadState* init(jthread thread) {
		ThreadState* p = NULL;
		jvmti->GetThreadLocalStorage(thread,reinterpret_cast<void**>(&p));
		if(p==NULL) {
			p = new ThreadState();
			jvmti->SetThreadLocalStorage(thread,p);
			
			{
				CLock lock(&threadStateLock);
				threadStates.insert(p);
			}
		} else {
			if(debug)
				*debug << "Thread state already initialized " << thread << endl;
		}

		return p;
	}

	static void destroy(jthread thread) {
		delete get(thread);
		jvmti->SetThreadLocalStorage(thread,NULL);
	}
};
