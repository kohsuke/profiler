#include "stdafx.h"
#include "profiler.h"

FileManager::FileManager(const char* fileName) : sectorSize(64*1024),nStreams(0) {
	usedSectors = 0;
	generalStream = NULL;

	file = CreateFile(fileName,GENERIC_READ|GENERIC_WRITE,FILE_SHARE_READ,NULL,CREATE_ALWAYS,
		FILE_ATTRIBUTE_ARCHIVE|FILE_FLAG_OVERLAPPED,NULL);
	if(file==INVALID_HANDLE_VALUE) {
		log() << "Unable to open file " << fileName << win32errorCode << endl;
	}

	dir = new RecordWriter<WORD>(*this,"root",sectorSize);
	// make sure that the first sector is the directory stream
	dir->prepare();

	*dir << htons(0xBeef);

	generalStream = new VarDataWriter(*this,"general",sectorSize);
}

FileManager::~FileManager() {
	delete dir;
	dir = NULL;
	delete generalStream;
	generalStream = NULL;
	CloseHandle(file);
}

void FileManager::writeData( const char* name, const char* data ) {
	_ASSERT(generalStream!=NULL);

	generalStream->write((BYTE)'R');
	generalStream->write(name);
	generalStream->write(data);
}
