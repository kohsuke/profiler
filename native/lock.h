#pragma once

class CCriticalSection : public CRITICAL_SECTION
{
public:
    CCriticalSection() {
        InitializeCriticalSection(this);
    }

    ~CCriticalSection() {
        DeleteCriticalSection(this);
    }

    void lock() {
        EnterCriticalSection(this);
    }

    void unlock() {
        LeaveCriticalSection(this);
    }
};

class CLock
{
	CRITICAL_SECTION* const cs;
public:
	CLock( CRITICAL_SECTION* p ) : cs(p) {
		EnterCriticalSection(cs);
	}
	~CLock() {
		LeaveCriticalSection(cs);
	}
};