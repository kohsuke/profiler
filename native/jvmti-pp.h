#pragma once

// store the pointer to the environment upon the initialization
extern jvmtiEnv* jvmti;


// stream manipulator to print out the error number
class jvmti_error {
private:
	const jvmtiError errCode;

public:
	jvmti_error( jvmtiError _errCode ) : errCode(_errCode) {}

	friend ostream& operator << ( ostream& os, jvmti_error err ) {
		if ( err.errCode == JVMTI_ERROR_NONE ) {
			return os << "No Error(0)";
		}

		char* s = NULL;
		
		jvmti->GetErrorName(err.errCode, &s);
		if( s==NULL )
			s = "Unknown Error";
		
		return os << ' ' << s << '(' << err.errCode << ") ";
	}
};

template <typename T>
class jvmtiPtr {
private:
	T* p;
	void clear() {
		if(p!=NULL) {
			jvmti->Deallocate(reinterpret_cast<BYTE*>(p));
			p = NULL;
		}
	}
public:
	jvmtiPtr() : p(NULL) {}
	~jvmtiPtr() {
		clear();
	}
	operator T* () const {
		return p;
	}
	T** operator & () {
		clear();
		return &p;
	}
};

// char* that are automatically freed at the end
typedef jvmtiPtr<char> jvmtistring;

inline ostream& operator << ( ostream& os, const jvmtistring& str ) {
	return os << static_cast<const char*>(str);
}

