#!/bin/sh
java -cp . -agentpath:native/Debug/profiler.dll=enable=y Test
