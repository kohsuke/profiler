#include "stdafx.h"
#include "profiler.h"
#include "method-trace.h"
#include "api.h"

// inline the definitions
#include "jvmti-pp.cpp.h"

ostream& log() {
	static string HEADER = "[profiler] ";
	return cerr << HEADER;
}

#ifdef _DEBUG
ostream* debug = &cerr;
#else
ostream* debug = NULL;
#endif

// the output of this profiler
FileManager* file = NULL;

// stream to write class definitions
VarDataWriter* classStream = NULL;

map<string,string> options;


BOOL APIENTRY DllMain( HANDLE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved )
{
    return TRUE;
}


// the ClassLoader.getResource method
static jmethodID getResource;
// the URL.toExternalForm method
static jmethodID toExternalForm;

static void JNICALL classPrepare(jvmtiEnv* jvmti_env, JNIEnv* jni,
								 jthread thread, jclass klass)
{
	jvmtistring sig;
	jvmti->GetClassSignature(klass,&sig,NULL);
	
//	if(debug) {
//		*debug << "prepareClass " << sig << endl;
//	}

	jint n;
	jmethodID* methods;
	jvmtiError err = jvmti->GetClassMethods(klass,&n,&methods);
	if(err) {
		log() << "Unable to parse the class " << klass << ' ' << sig << ' ' << jvmti_error(err) << endl;
		return;
	}


	// if there's no method, there won't be any method invocation on this class/interface,
	// so we don't need to record it.
	if(n>0) {
		// find out where this class was loaded from
		jstring str = NULL;
		const char* resourceName;
		jobject classLoader = NULL;
		err = jvmti->GetClassLoader(klass,&classLoader);
		if(err) {
			log() << "Unable to get the class laoder for the class " << klass << ' ' << sig << ' ' << jvmti_error(err) << endl;
			return;
		}
		if(classLoader==NULL) {
			// bootstrap class loader
			resourceName = "bs";
		} else {
			char* name = new char[strlen(sig)+32];
			strcpy(name,sig+1);
			name[strlen(name)-1] = '\0';	// remove the last ';'
			for( char* p=name; *p!='\0'; p++ ) {
				if( *p == '.' )
					*p = '/';
			}
			strcat(name,".class");
			jobject url = jni->CallObjectMethod(classLoader,getResource,jni->NewStringUTF(name));
			if(url==NULL) {
				resourceName = "";
			} else {
				str = (jstring)jni->CallObjectMethod(url,toExternalForm);
				resourceName = jni->GetStringUTFChars(str,NULL);
			}
		}

		classStream->write((BYTE)'C');
		classStream->write(DWORD(klass));
		classStream->write(sig);
		classStream->write(resourceName);

		if(str!=NULL)
			jni->ReleaseStringUTFChars(str,resourceName);

		for( int i=0; i<n; i++ ) {
			jvmtistring name,sig;
			err = jvmti->GetMethodName(methods[i],&name,&sig,NULL);
			if(err) {
				log() << "Unable to get the method name of " << methods[i] << jvmti_error(err) << endl;
				continue;
			}
			
			classStream->write((BYTE)'M');
			classStream->write(DWORD(methods[i]));
			classStream->write(name);
			classStream->write(sig);
		}

		classStream->write((BYTE)0);
	}
}


//static void test() {
//	log() << "testing FileManager" << endl;
//	FileManager fm("test.prof");
//	RecordWriter<int>* intBuf = new RecordWriter<int>(fm,"int",fm.sectorSize);
//	RecordWriter<short>* shortBuf = new RecordWriter<short>(fm,"short",fm.sectorSize*2);
//
//	int ic=0; short sc=0;
//	for( int i=0; i<fm.sectorSize; i++ ) {
//		*intBuf << ic++;
//		*shortBuf << sc++;
//	}
//	Sleep(100);
//	for( int i=0; i<fm.sectorSize; i++ ) {
//		*intBuf << ic++;
//		*shortBuf << sc++;
//	}
//
//	delete intBuf;
//	delete shortBuf;
//	log() << "testing FileManager done" << endl;
//}


static void JNICALL vmInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread)
{
	log() << "VM init" << endl;

	jvmtiError err;

	if(::options.find("enable")!=::options.end()) {
		log() << "Profiler enabled by the command line option" << endl;
		profilingEnabledForNewThread = true;
	}

	getResource = jni->GetMethodID( jni->FindClass("java/lang/ClassLoader"), "getResource", "(Ljava/lang/String;)Ljava/net/URL;" );
	if(getResource==NULL) {
		log() << "ERROR: Unable to obtain the ClassLoader.getResource method" << endl;
		return;
	}
	toExternalForm = jni->GetMethodID( jni->FindClass("java/net/URL"), "toExternalForm", "()Ljava/lang/String;" );
	if(getResource==NULL) {
		log() << "ERROR: Unable to obtain the URL.toExternalForm method" << endl;
		return;
	}

	// some threads are created even before the agent is initialized.
	// for them, the thread start event will never be called,
	// so we need to set them up manually
	{
		jint threadCount;
		jvmtiPtr<jthread> threads;
		err = jvmti->GetAllThreads(&threadCount,&threads);
		if(err) {
			log() << "Unable to list up all threads " << jvmti_error(err) << endl;
			return;
		}

		for( int i=0; i<threadCount; i++ )
			ThreadState::init(threads[i]);
	}

	// many core JDK classes are loaded without the classPrepare event call back.
	// (presumably because they are loaded before this agent is set up)
	// we have to find them and list them up.
	jint count;
	jvmtiPtr<jclass> classes;
	err = jvmti->GetLoadedClasses(&count,&classes);
	if(err) {
		log() << "Unable to list up pre-loaded classes " << jvmti_error(err) << endl;
		return;
	}
	for( int i=0; i<count; i++ ) {
		jint status;
		err = jvmti->GetClassStatus(classes[i],&status);
		if(err) {
			log() << "Unable to get the status for class " << classes[i] << ' ' << jvmti_error(err) << endl;
			continue;
		}
		// there are some unprepared classes. ignore them for now.
		// those classes should eventually raise a class-prepare event.
		if(status&JVMTI_CLASS_STATUS_PREPARED)
			classPrepare(jvmti, jni, NULL, classes[i]);
	}
	// classes loaded later shall be reported
	jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);

	// start sending out events
	if(profilingEnabledForNewThread)
		api::enableAll(true);

	if(debug)
		*debug << "VM init complete" << endl;
}

static void JNICALL vmDeath(jvmtiEnv* jvmti_env, JNIEnv* jni_env)
{
	log() << "VM end" << endl;
}

//static void JNICALL garbageCollectionStart(jvmtiEnv* jvmti_env) {
//	cerr << "GC started" << endl;
//}
//
//static void JNICALL garbageCollectionFinish(jvmtiEnv* jvmti_env) {
//	cerr << "GC finished" << endl;
//}

static void recordSystemProperties() {
	jint count=0;
	jvmtiPtr<char*> props;
	jvmtiError err = jvmti->GetSystemProperties(&count,&props);
	if(err!=0 || props==NULL) {
		log() << "Unable to get the system properties " << jvmti_error(err) << endl;
		return;
	}

	for( int i=0; i<count; i++ ) {
		char* keyName = new char[strlen(props[i])+32];
		sprintf(keyName,"SystemProperty:%s",props[i]);

		jvmtistring value;
		err = jvmti->GetSystemProperty(props[i],&value);
		if(err==0) {
			file->writeData(keyName,value);
		} else {
			log() << "Unable to get the system property " << props[i] << jvmti_error(err) << endl;
		}

		delete keyName;
		jvmti->Deallocate((BYTE*)props[i]);
	}
}

// tokenizes the command line option of the form ()
static void processArguments(const char* options) {
	const char* nameEnd;

	while(*options!='\0') {
		// look for the end of token
		for( nameEnd=options; *nameEnd!=',' && *nameEnd!='=' && *nameEnd!='\0'; nameEnd++)
			;

		string name(options,nameEnd-options);
		string value;

		if(*nameEnd=='=') {
			nameEnd++;
			const char* valueEnd = nameEnd;
			
			while( *valueEnd!=',' && *valueEnd!='\0' )
				valueEnd++;

			value = string(nameEnd,valueEnd-nameEnd);
			
			options = valueEnd;
		} else {
			value = "";
			options = nameEnd;
		}

		if(*options == ',')
			options++;

		::options.insert(pair<string,string>(name,value));
	}
}


unsigned long determineTicksPerSecond(void) {
	HKEY    handle;
	DWORD   error;
	DWORD	valueType;
	DWORD   valueLength=4;
	char *valueName="~Mhz";
	char *key="HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0";
	unsigned long cpuMhz=0;

	error=RegOpenKeyEx(HKEY_LOCAL_MACHINE,	/* Registery tree */
					   key,					/* Key to look up */
					   0,					/* Reserved, must be 0 */
					   KEY_READ,			/* READ mode */
					   &handle);			/* Handle to the key */
	if(error!=ERROR_SUCCESS) {
		log() << "Error opening CPU speed key " << error << endl;
		return 0;
	}

	error=RegQueryValueEx(handle,				/* Handle to the key */
						  valueName,			/* Value to query */
						  NULL,					/* Reserved, must be NULL */
						  &valueType,			/* Type of value */
						  (LPBYTE)&cpuMhz,		/* The result of the query */
						  &valueLength);		/* Length of the data */


	if(error!=ERROR_SUCCESS) {
		log() << "Error opening CPU speed " << error << endl;
		return 0;
	}
	RegCloseKey(handle);
	return cpuMhz*1000L*1000L;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved)
{
	log() << "started" << endl;

	jint rc;

	rc = vm->GetEnv( reinterpret_cast<void**>(&jvmti), JVMTI_VERSION );
	if (rc != JNI_OK) {
		log() << "ERROR: JVM does not support JVMTI. error=" << rc << endl;
		return -1;
	}

	if(options!=NULL)
		processArguments(options);

	jvmtiCapabilities caps;
	jvmti->GetCapabilities(&caps);
	caps.can_get_current_thread_cpu_time = true;
	caps.can_get_thread_cpu_time = true;
	caps.can_generate_method_entry_events = true;
	caps.can_generate_method_exit_events = true;
	jvmti->AddCapabilities(&caps);
	
	jvmtiEventCallbacks callbacks;
	memset(&callbacks, 0, sizeof(callbacks));
	callbacks.VMInit = vmInit;
	callbacks.VMDeath = vmDeath;
	callbacks.MethodEntry = methodEntry;
	callbacks.MethodExit = methodExit;
	callbacks.ThreadStart = threadStart;
	callbacks.ClassPrepare = classPrepare;
	callbacks.ThreadEnd = threadEnd;
	jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));

	jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
	jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
	jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
	jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL);
	
	// open file
	string fileName;
	if(::options.find("filename")!=::options.end()) {
		fileName = ::options.find("filename")->second;
	} else {
		fileName = "profiler.out";
	}
	log() << "Writing output to " << fileName << endl;
	file = new FileManager(fileName.c_str());
	classStream = new VarDataWriter(*file,"classStream",file->sectorSize);

	// record system properties
	recordSystemProperties();

	// record clock frequency
	__int64 tick = determineTicksPerSecond();
	if(tick==0) {
		return -1;
	}
	char w[32];
	sprintf(w,"%I64d",tick);
	file->writeData(".counter.frequency",w);

	return 0;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM* vm) {
	if(file!=NULL) {
		log() << "shutting down" << endl;
		{
			CLock lock(&threadStateLock);
			// the set will be modified as we delete ThreadStates, so make a copy first
			set<ThreadState*> clone(threadStates);
			for( set<ThreadState*>::iterator itr=clone.begin(); itr!=clone.end(); itr++ )
				delete *itr;
		}
		delete classStream;
		// finally close the file
		delete file;
	}
}
