#include "stdafx.h"
#include "profiler.h"
#include "api.h"
#include "org_kohsuke_kprofiler_API.h"
#include "method-trace.h"

// JNI -> internal API mapping
JNIEXPORT void JNICALL
Java_org_kohsuke_kprofiler_API_enableAll( JNIEnv* env, jclass _, jboolean enable ) {
	api::enableAll(enable?true:false);
}

JNIEXPORT void JNICALL
Java_org_kohsuke_kprofiler_API_enableThread( JNIEnv* env, jclass _, jthread thread, jboolean enable ) {
	api::enable(thread,enable?true:false);
}



void api::enableAll(bool enable) {
	jvmtiError err;
	jvmtiEventMode mode = enable?JVMTI_ENABLE:JVMTI_DISABLE;

	jint threadCount;
	jvmtiPtr<jthread> threads;
	err = jvmti->GetAllThreads(&threadCount,&threads);
	if(err) {
		log() << "Unable to list up all threads " << jvmti_error(err) << endl;
		return;
	}

	for( int i=0; i<threadCount; i++ )
		ThreadState::get(threads[i])->enabled = enable;

	jvmti->SetEventNotificationMode(mode, JVMTI_EVENT_METHOD_ENTRY, NULL);
	jvmti->SetEventNotificationMode(mode, JVMTI_EVENT_METHOD_EXIT, NULL);

	profilingEnabledForNewThread = enable;
}

void api::enable(jthread thread,bool enable) {
	if(enable) {
		jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, NULL);
		jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, NULL);
	}

	ThreadState::get(thread)->enabled = enable;
}
