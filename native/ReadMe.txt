JVMTI
http://java.sun.com/j2se/1.5.0/docs/guide/jvmti/jvmti.html

JNI tutorial
http://java.sun.com/docs/books/tutorial/native1.1/index.html
http://java.sun.com/j2se/1.5.0/docs/guide/jni/


we need a different "stream"
	we could use chunking, but what if it grows too much?
	use different files?

File mapping needs the size to be known upfront.
not good for growing its size. Perhaps better to use async IO?


Win32 interlocked methods
	InterlockedIncrement
	InterlockedCompareExchange

Async file write
	use WriteFileEx. (http://msdn.microsoft.com/library/default.asp?url=/library/en-us/fileio/base/writefileex.asp)
	Given the various alignment constraints, this looks reasonably fast
	upon completion, use SleepEx to run the completion routine.
	
	On POSIX systems, see http://www.linux.or.jp/JM/html/LDP_man-pages/man3/aio_write.3.html


file manager allocates a "sector" (or sectors) to streams as they request.

each stream maintains two buffers. One is used while the other is written asynchronously
BufferManager maintains those and handles async I/O


class/method info buffer. shared by all threads.
	how to write variable-length data to stream?
