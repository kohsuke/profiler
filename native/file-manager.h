#pragma once
#include "lock.h"


template < class T >
class BufferManager;

template < class T >
class RecordWriter;

class VarDataWriter;


// we consider a file to consists of "sectors"
// so that multiple threads can share the same file simultaneously.
// this class manages the allocation of sectors.
class FileManager {
private:
	HANDLE file;
	// number of sectors that are already allocated
	LONG usedSectors;
	// number of streams in this file
	LONG nStreams;
	// lock to control access the thread-shared state
	CCriticalSection lock;

	// allocates a new stream number that starts from 0
	WORD assignStreamNumber() {
		return WORD(InterlockedIncrement(&nStreams)-1);
	}

	// the directory buffer.
	RecordWriter<WORD>* dir;

	// stream for writing a bag of data
	// used to keep track of metadata and other small data
	// that doesn't need its own stream
	VarDataWriter* generalStream;

public:
	// technically, we have to compute the sector size
	// from the GetFreeDiskspace API. But I'm assuming
	// that 64K is sufficiently large as a sector size
	const int sectorSize;

	FileManager(const char* fileName);

	~FileManager();

	bool hadError() {
		return file==INVALID_HANDLE_VALUE;
	}

	// write the data of the specified size by the given name
	// into the general stream
//	void writeData( const char* name, const void* data, size_t length );
	void writeData( const char* name, const char* str );

	// allocates the given number of sectors from the file, and returns
	// the offset of the first sector
	template <class T>
	LONG allocateSectors( BufferManager<T>* buffer, LONG nSector ) {
		CLock autoLock(&lock);

		int start = usedSectors;
		usedSectors += nSector ;
		
		if(buffer->id!=0) {
			// mark the allocated sectors.
			// stream id 0 is for the directory stream.
			for( int i=0; i<nSector; i++ )
				*dir << htons(buffer->id);
			// in case we ever need the 2nd sector for the directory,
			// make sure it gets the expected sector
			dir->prepare();
		}

		return start;
	}

	friend class BufferManager;
};


// manages double-buffering
// not thread safe
template < class T >
class BufferManager {
private:
	FileManager& parent;
	// front buffer is the one being used
	// back buffer is the one being written
	T* front;
	T* back;

	OVERLAPPED frontO,backO;

	// true if the back buffer is being written
	bool writePending;

	T* allocateBuffer() {
		return reinterpret_cast<T*>(VirtualAlloc(NULL,bufferSize,MEM_COMMIT,PAGE_READWRITE));
	}
	void releaseBuffer(T*& p) {
		VirtualFree(p,bufferSize,MEM_RELEASE);
		p = NULL;
	}
	// complete the pending write operation
	void wait() {
		if(!writePending)
			return;	// nothing to wait

		// wait for I/O completion
		if(debug) {
			SleepEx(0,true);
			while(writePending) {
				*debug << "BufferManager " << name << " Waiting for I/O completion" << endl;
				SleepEx(INFINITE,true);
			}
		} else {
			// SleepEx can return when other overlapped write has finished.
			// So make sure that we wait until our pending write operation is done
			while(writePending)
				SleepEx(INFINITE,true);
		}
		
		if(writePending) {
			// can this happen?
			log() << "BufferManager " << name << " buffer overflow" << endl;
			writePending = false;
		}
	}
	void write( void* buf, OVERLAPPED& o ) {
		writePending = true;
//		if(debug)
//			*debug << "BufferManager " << name << " writing" << endl;
		if(WriteFileEx( parent.file, buf, bufferSize, &o, onComplete )==0) {
			log() << "WriteFileEx failed" << win32errorCode << endl;
		}
	}

public:

	// the number of sectors that we use in one buffer
	const int sectorCount;
	// buffer size
	const LONG bufferSize;

	// name of the stream. for debugging/logging only
	const string name;

	// the stream ID
	const WORD id;

	BufferManager( FileManager& _parent, const char* pName, LONG bufferSize )
		: parent(_parent),
		  id(_parent.assignStreamNumber()),
		  name(pName),
		  sectorCount((bufferSize+parent.sectorSize-1)/parent.sectorSize),
		  bufferSize(sectorCount*parent.sectorSize) {
		
		writePending = false;
		front = back = NULL;

		// record the stream name
		char w[32];
		sprintf(w,".StreamName-%d",id);
		if(parent.generalStream!=NULL) {
			parent.writeData(w,pName);
		}
	}
	~BufferManager() {
		wait();
		if(front!=NULL) {
			// if we have the front buffer, write it now
			write(front,frontO);
			// and wait for its completion before we return
			wait();
		}

		if(front!=NULL)		releaseBuffer(front);
		if(back!=NULL)		releaseBuffer(back);
	}

	// return true if this stream is reserved by the system
	bool isSystemStream() const {
		return id<2;
	}

	// flips buffers and return the new pointer
	T* flip() {
//		if(debug)
//			*debug << "BufferManager " << name << " flipping" << endl;
		
		wait();
		
		swap(front,back);
		swap(frontO,backO);

		// allocate a new buffer
		if(front==NULL)
			front = allocateBuffer();

		// allocate new sectors
		LONG newSector = parent.allocateSectors(this,sectorCount);
		
		frontO.Offset = newSector*parent.sectorSize;
		frontO.OffsetHigh = 0;
		frontO.hEvent = this; // use this field to track back to 'this'

		if(back!=NULL)
			// queue the write operation
			write(back,backO);
		
		return front;
	}

	// called when the async write is done
	static void CALLBACK onComplete(
		DWORD dwErrorCode, DWORD dwNumberOfBytesTransfered, OVERLAPPED* lpOverlapped)
	{
		BufferManager* pThis = reinterpret_cast<BufferManager*>(lpOverlapped->hEvent);
		if(!pThis->writePending) {
			_ASSERT(false);
			log() << "ERROR: assertion failed. writePending should be true" << endl;
		}

		pThis->writePending = false;
		if(debug) {
			*debug << "I/O complete for " << pThis->name << endl;
		}
	}

	friend class FileManager;
};

// allows the client to easily write Ts one by one
template < class T >
class RecordWriter {
private:
	FileManager&	parent;
	BufferManager<T> buf;
	// the head of the new buffer
	T* head;
	// next address to write
	T* p;
	T* limit;
	// number of pages used
	int pagesUsed;

public:
	RecordWriter( FileManager& _parent, const char* name, LONG bufferSize )
		: parent(_parent),buf(_parent,name,bufferSize) {
		p = head = limit = NULL;
		pagesUsed = 0;
	}
	~RecordWriter() {
		if(p!=NULL) {
			// fill the rest with 0
			FillMemory(p,buf.bufferSize-(p-head)*sizeof(T),0);
		}
		if(!buf.isSystemStream()) {
			char w[32];
			sprintf(w,".StreamSize-%d",buf.id);
			char v[32];
			sprintf(v,"%d",max(0,pagesUsed-1)*buf.bufferSize+(p-head)*sizeof(T));
			parent.writeData(w,v);
		}
	}

	// prepare the underlying buffer. 
	void prepare() {
		if(p==NULL || p==limit) {
			head = p = buf.flip();
			limit = p+(buf.bufferSize/sizeof(T));
			pagesUsed++;
		}
	}

	// write one value
	RecordWriter<T>& operator << ( const T& value ) {
		*next() = value;
		return *this;
	}

	// allocate the storage for the new item
	// and returns a pointer to it
	T* next() {
		prepare();
		return p++;
	}
};


// write variable-length data to a stream
class VarDataWriter {
private:
	FileManager&	parent;
	BufferManager<BYTE> buf;
	// the head of the new buffer
	BYTE* p;
	BYTE* limit;
	int pagesUsed;
public:
	VarDataWriter( FileManager& _parent, const char* name, LONG bufferSize )
		: parent(_parent),buf(_parent,name,bufferSize) {
		p = limit = NULL;
		pagesUsed=0;
	}
	~VarDataWriter() {
		if(p!=NULL) {
			// fill the rest with 0
			FillMemory(p,limit-p,0);
		}
		if(!buf.isSystemStream()) {
			char w[32];
			sprintf(w,".StreamSize-%d",buf.id);
			char v[32];
			sprintf(v,"%d",pagesUsed*buf.bufferSize-(limit-p));
			parent.writeData(w,v);
		}
	}

	// prepare the underlying buffer. 
	void prepare() {
		if(p==NULL || p==limit) {
			p = buf.flip();
			limit = p+buf.bufferSize;
			pagesUsed++;
		}
	}

	// write a data of the given size
	void write( const void* data, size_t len ) {
		const BYTE* src = static_cast<const BYTE*>(data);
		// decide the size we can write into the current buffer
		while(len!=0) {
			prepare();
			size_t chunk = min(len,size_t(limit-p));
			memcpy(p,src,chunk);
			p += chunk;
			src += chunk;
			len -= chunk;
		}
	}

	// write a string in the modified UTF encoding
	void write( const char* str ) {
		write((WORD)strlen(str));
		write(str,strlen(str));
	}
	
	// write a byte
	void write( BYTE b ) {
		prepare();
		if(p!=limit)
			*p++ = b;
	}

	// write a word
	void write( WORD w ) {
		w = htons(w);
		write(&w,sizeof(w));
	}

	// write a dword
	void write( DWORD w ) {
		w = htonl(w);
		write(&w,sizeof(w));
	}
};