package org.droidslicer.util;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public enum MethodId
{
	INVALID,
	
	// java.util.Collection
	COLLECTION_ADD_OBJ(MethodReference.findOrCreate(TypeId.COLLECTION.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("add"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.OBJECT.getTypeReference().getName()
				},
				TypeReference.BooleanName))),

	// java.util.List
	LIST_GET_INT(MethodReference.findOrCreate(TypeId.LIST.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("add"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				}, 
				TypeId.OBJECT.getTypeReference().getName()))),
	LIST_ADD_OBJ(MethodReference.findOrCreate(TypeId.LIST.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("add"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.OBJECT.getTypeReference().getName()
				}, 
				TypeReference.BooleanName))),

	// java.util.ArrayList
	ARRAY_LIST_GET_INT(MethodReference.findOrCreate(TypeId.ARRAY_LIST.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("get"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				}, 
				TypeId.OBJECT.getTypeReference().getName()))),
	ARRAY_LIST_ADD_OBJ(MethodReference.findOrCreate(TypeId.ARRAY_LIST.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("add"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.OBJECT.getTypeReference().getName()
				},
				TypeReference.BooleanName))),
	
	// java.util.concurrent.Executor
	EXECUTOR_EXECUTE(MethodReference.findOrCreate(TypeId.EXECUTOR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.RUNNABLE.getTypeReference().getName()					
				},
				TypeReference.VoidName))),	

	// java.io.FileInputStream
	FILE_INPUT_STREAM_INIT_FILE(MethodReference.findOrCreate(TypeId.FILE_INPUT_STREAM.getTypeReference(), 
					MethodReference.initAtom, 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.FILE.getTypeReference().getName()}, 
							TypeReference.VoidName))),
	FILE_INPUT_STREAM_INIT_FD(MethodReference.findOrCreate(TypeId.FILE_INPUT_STREAM.getTypeReference(), 
					MethodReference.initAtom, 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.FILE_DESCRIPTOR.getTypeReference().getName()}, 
							TypeReference.VoidName))),
	FILE_INPUT_STREAM_INIT_STR(MethodReference.findOrCreate(TypeId.FILE_INPUT_STREAM.getTypeReference(), 
					MethodReference.initAtom, 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeReference.VoidName))),
	FILE_INPUT_STREAM_OPEN(MethodReference.findOrCreate(TypeId.FILE_INPUT_STREAM.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("open"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeReference.VoidName))),
	FILE_INPUT_STREAM_READ0_INT(MethodReference.findOrCreate(TypeId.FILE_INPUT_STREAM.getTypeReference(),
			Atom.findOrCreateAsciiAtom("read0"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.IntName))),
	FILE_INPUT_STREAM_READ_BYTES(MethodReference.findOrCreate(TypeId.FILE_INPUT_STREAM.getTypeReference(),
			Atom.findOrCreateAsciiAtom("readBytes"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.ByteName.getArrayTypeForElementType(),
					TypeReference.IntName,
					TypeReference.IntName, 
				},
				TypeReference.IntName))),
	FILE_OUTPUT_STREAM_WRITE_INT_BOOL(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(),
			Atom.findOrCreateAsciiAtom("write"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName,
					TypeReference.BooleanName
				},
				TypeReference.VoidName))),

	// java.io.FileOutputStream
	FILE_OUTPUT_STREAM_INIT_FILE(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(), 
		MethodReference.initAtom, 
		Descriptor.findOrCreate(
				new TypeName[]{TypeId.FILE.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	FILE_OUTPUT_STREAM_INIT_FILE_BOOL(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(), 
		MethodReference.initAtom, 
		Descriptor.findOrCreate(
				new TypeName[]{TypeId.FILE.getTypeReference().getName(), TypeReference.BooleanName}, 
				TypeReference.VoidName))),
	FILE_OUTPUT_STREAM_INIT_FD(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(), 
		MethodReference.initAtom, 
		Descriptor.findOrCreate(
				new TypeName[]{TypeId.FILE_DESCRIPTOR.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	FILE_OUTPUT_STREAM_INIT_STR(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(), 
		MethodReference.initAtom, 
		Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	FILE_OUTPUT_STREAM_INIT_STR_BOOL(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(), 
		MethodReference.initAtom, 
		Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName(), TypeReference.BooleanName}, 
				TypeReference.VoidName))),
	FILE_OUTPUT_STREAM_OPEN(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(), 
		Atom.findOrCreateAsciiAtom("open"), 
		Descriptor.findOrCreate(
				new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeReference.BooleanName}, 
				TypeReference.VoidName))),
	FILE_OUTPUT_STREAM_WRITE_BYTES(MethodReference.findOrCreate(TypeId.FILE_OUTPUT_STREAM.getTypeReference(),
		Atom.findOrCreateAsciiAtom("writeBytes"), 
		Descriptor.findOrCreate(
				new TypeName[]{
						TypeReference.ByteName.getArrayTypeForElementType(),
						TypeReference.IntName,
						TypeReference.IntName,
						TypeReference.BooleanName}, 
				TypeReference.VoidName))),

	// java.lang.System
	SYSTEM_ARRAYCOPY(MethodReference.findOrCreate(TypeId.SYSTEM.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("arraycopy"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.OBJECT.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.OBJECT.getTypeReference().getName(),
					TypeReference.IntName,
					TypeReference.IntName
				}, 
				TypeReference.VoidName))),
	SYSTEM_GET_PROPERTY(MethodReference.findOrCreate(TypeId.SYSTEM.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getProperty"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	SYSTEM_GET_PROPERTY_DEFAULT(MethodReference.findOrCreate(TypeId.SYSTEM.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getProperty"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.STRING.getTypeReference().getName()))),
							
	// java.lang.String
	STR_VALUE_OF_OBJ(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.OBJECT.getTypeReference().getName()
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	STR_VALUE_OF_BOOL(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.BooleanName
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	STR_VALUE_OF_CHAR(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.CharName
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	STR_VALUE_OF_DOUBLE(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.DoubleName
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	STR_VALUE_OF_FLOAT(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.FloatName
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	STR_VALUE_OF_INT(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				}, 
		TypeId.STRING.getTypeReference().getName()))),
	STR_VALUE_OF_LONG(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("valueOf"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.LongName
				}, 
		TypeId.STRING.getTypeReference().getName()))),
	STR_CONCAT(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("concat"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.STRING.getTypeReference().getName()))),
	STR_INIT(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeReference.VoidName))),
	STR_INIT_STR(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeReference.VoidName))),
	STR_INIT_STR_BUILDER(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STR_BUILDER.getTypeReference().getName()
				}, 
				TypeReference.VoidName))),
	STR_CHAR_AT(MethodReference.findOrCreate(TypeId.STRING.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("charAt"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				}, 
				TypeReference.CharName))),

	// java.lang.Runtime
	RUNTIME_STR_ARR_STR_ARR(MethodReference.findOrCreate(TypeId.RUNTIME.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("exec"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				}, 
				TypeId.PROCESS.getTypeReference().getName()))),
	RUNTIME_STR_STR_ARR_FILE(MethodReference.findOrCreate(TypeId.RUNTIME.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("exec"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.FILE.getTypeReference().getName()
				}, 
				TypeId.PROCESS.getTypeReference().getName()))),
	RUNTIME_STR_ARR_STR_ARR_FILE(MethodReference.findOrCreate(TypeId.RUNTIME.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("exec"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.FILE.getTypeReference().getName()
				}, 
				TypeId.PROCESS.getTypeReference().getName()))),
	RUNTIME_STR_STR_ARR(MethodReference.findOrCreate(TypeId.RUNTIME.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("exec"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				}, 
				TypeId.PROCESS.getTypeReference().getName()))),
	RUNTIME_STR(MethodReference.findOrCreate(TypeId.RUNTIME.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("exec"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.PROCESS.getTypeReference().getName()))),
	RUNTIME_STR_ARR(MethodReference.findOrCreate(TypeId.RUNTIME.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("exec"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				}, 
				TypeId.PROCESS.getTypeReference().getName()))),

	// java.lang.Runnable
	RUNNABLE_RUN(MethodReference.findOrCreate(TypeId.RUNNABLE.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("run"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeReference.VoidName))),
	
	// java.nio.file.FileSystem
	FILE_SYSTEM_GET_SEPARATOR(MethodReference.findOrCreate(TypeId.FILE_SYSTEM.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getSeparator"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeId.STRING.getTypeReference().getName()))),
	
	// java.io.FileSystem
	IO_FILE_SYSTEM_GET_SEPARATOR(MethodReference.findOrCreate(TypeId.IO_FILE_SYSTEM.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getSeparator"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeReference.CharName))),
	IO_FILE_SYSTEM_GET_PATH_SEPERATOR(MethodReference.findOrCreate(TypeId.IO_FILE_SYSTEM.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getPathSeparator"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeReference.CharName))),
			
	// java.io.File
	FILE_GET_PATH(MethodReference.findOrCreate(TypeId.FILE.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getPath"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeId.STRING.getTypeReference().getName()))),
	FILE_GET_ABSOLUTE_PATH(MethodReference.findOrCreate(TypeId.FILE.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getAbsolutePath"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeId.STRING.getTypeReference().getName()))),
	FILE_INIT_STR(MethodReference.findOrCreate(TypeId.FILE.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()				
				}, 
			TypeReference.VoidName))),
	FILE_INIT_STR_STR(MethodReference.findOrCreate(TypeId.FILE.getTypeReference(),
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				}, 
			TypeReference.VoidName))),
	FILE_INIT_FILE_STR(MethodReference.findOrCreate(TypeId.FILE.getTypeReference(),
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.FILE.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				}, 
			TypeReference.VoidName))),

	// java.lang.StringBuilder
	STR_BUILDER_INIT(MethodReference.findOrCreate(TypeReference.JavaLangStringBuilder, 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeReference.VoidName))),
	STR_BUILDER_INIT_INT(MethodReference.findOrCreate(TypeReference.JavaLangStringBuilder,
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				}, 
			TypeReference.VoidName))),
	STR_BUILDER_INIT_STR(MethodReference.findOrCreate(
				TypeReference.JavaLangStringBuilder, 
				MethodReference.initAtom, 
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName()
					}, 
				TypeReference.VoidName))),
	STR_BUILDER_TO_STR(MethodReference.findOrCreate(TypeReference.JavaLangStringBuilder,
			Atom.findOrCreateAsciiAtom("toString"), 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
			TypeId.STRING.getTypeReference().getName()))),
	STR_BUILDER_APPEND_STR(MethodReference.findOrCreate(
				TypeReference.JavaLangStringBuilder, 
				Atom.findOrCreateAsciiAtom("append"), 
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName()
					}, 
				TypeReference.JavaLangStringBuilder.getName()))),
	STR_BUILDER_APPEND_BOOL(MethodReference.findOrCreate(
			TypeReference.JavaLangStringBuilder, 
			Atom.findOrCreateAsciiAtom("append"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.BooleanName
				}, 
			TypeReference.JavaLangStringBuilder.getName()))),
	STR_BUILDER_APPEND_CHAR(MethodReference.findOrCreate(
			TypeReference.JavaLangStringBuilder, 
			Atom.findOrCreateAsciiAtom("append"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.CharName
				}, 
			TypeReference.JavaLangStringBuilder.getName()))),
	STR_BUILDER_APPEND_FLOAT(MethodReference.findOrCreate(
			TypeReference.JavaLangStringBuilder, 
			Atom.findOrCreateAsciiAtom("append"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.FloatName
				}, 
			TypeReference.JavaLangStringBuilder.getName()))),
	STR_BUILDER_APPEND_DOUBLE(MethodReference.findOrCreate(
			TypeReference.JavaLangStringBuilder, 
			Atom.findOrCreateAsciiAtom("append"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.DoubleName
				}, 
			TypeReference.JavaLangStringBuilder.getName()))),
	STR_BUILDER_APPEND_INT(MethodReference.findOrCreate(
			TypeReference.JavaLangStringBuilder, 
			Atom.findOrCreateAsciiAtom("append"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				}, 
			TypeReference.JavaLangStringBuilder.getName()))),
	STR_BUILDER_APPEND_LONG(MethodReference.findOrCreate(
			TypeReference.JavaLangStringBuilder, 
			Atom.findOrCreateAsciiAtom("append"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.LongName
				}, 
			TypeReference.JavaLangStringBuilder.getName()))),

	// android.net.Uri
	ANDROID_URI_FROM_FILE(MethodReference.findOrCreate(
				TypeId.ANDROID_URI.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("fromFile"), 
				Descriptor.findOrCreate(
						new TypeName[]{TypeId.FILE.getTypeReference().getName()}, 
						TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_URI_FROM_PARTS(MethodReference.findOrCreate(
				TypeId.ANDROID_URI.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("fromParts"), 
				Descriptor.findOrCreate(
						new TypeName[]{
								TypeId.STRING.getTypeReference().getName(),
								TypeId.STRING.getTypeReference().getName(),
								TypeId.STRING.getTypeReference().getName()}, 
						TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_URI_PARSE(MethodReference.findOrCreate(
				TypeId.ANDROID_URI.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("parse"), 
				Descriptor.findOrCreate(
						new TypeName[]{
								TypeId.STRING.getTypeReference().getName()}, 
						TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_URI_WITH_APPENDED_PATH(MethodReference.findOrCreate(
				TypeId.ANDROID_URI.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("withAppendedPath"), 
				Descriptor.findOrCreate(
						new TypeName[]{
								TypeId.ANDROID_URI.getTypeReference().getName(),
								TypeId.STRING.getTypeReference().getName()}, 
						TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_URI_BUILD_UPON(MethodReference.findOrCreate(
				TypeId.ANDROID_URI.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("buildUpon"), 
				Descriptor.findOrCreate(
						new TypeName[]{}, 
						TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),

	// android.net.Uri$Builder
	ANDROID_URI_BUILDER_BUILD(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("build"), 
					Descriptor.findOrCreate(
							new TypeName[]{}, 
							TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_APPEND_ENCODED_PATH(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("appendEncodedPath"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_APPEND_PATH(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("appendPath"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_APPEND_QUERY_PARAM(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("appendQueryParameter"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName(), TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_AUTHORITY(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("authority"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_CLEAR_QUERY(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("clearQuery"), 
					Descriptor.findOrCreate(
							new TypeName[]{}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_ENCODED_AUTHORITY(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("encodedAuthority"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_ENCODED_FRAGMENT(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("encodedFragment"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_ENCODED_OPAQUE_PART(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("encodedOpaquePart"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_ENCODED_PATH(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("encodedPath"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_ENCODED_QUERY(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("encodedQuery"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_FRAGMENT(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("fragment"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_OPAQUE_PART(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("opaquePart"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_PATH(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("path"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_QUERY(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("query"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),
	ANDROID_URI_BUILDER_SCHEME(MethodReference.findOrCreate(
					TypeId.ANDROID_URI_BUILDER.getTypeReference(), 
					Atom.findOrCreateAsciiAtom("scheme"), 
					Descriptor.findOrCreate(
							new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
							TypeId.ANDROID_URI_BUILDER.getTypeReference().getName()))),

	// android.content.Context
	ANDROID_CONTEXT_OPEN_FILE_INPUT(MethodReference.findOrCreate(
				TypeId.ANDROID_CONTEXT.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("openFileInput"), 
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName()
					}, 
					TypeId.FILE_INPUT_STREAM.getTypeReference().getName()))),
	ANDROID_CONTEXT_OPEN_FILE_OUTPUT(MethodReference.findOrCreate(
				TypeId.ANDROID_CONTEXT.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("openFileOutput"), 
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeReference.IntName
					}, 
					TypeId.FILE_OUTPUT_STREAM.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_APP_CTX(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getApplicationContext"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, TypeId.ANDROID_CONTEXT.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_APP_INFO(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getApplicationInfo"),
			Descriptor.findOrCreate(
				new TypeName[]{}, TypeId.ANDROID_APP_INFO.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_SYS_SERVICE(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getSystemService"),			 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.JavaLangObject.getName()))),
	ANDROID_CONTEXT_GET_CACHE_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getCacheDir"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_DB_PATH(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getDatabasePath"),			 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getDir"),			 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(), 
					TypeReference.IntName
				}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_EXTERNAL_CACHE_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getExternalCacheDir"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_EXTERNAL_CACHE_DIRS(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getExternalCacheDirs"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.FILE.getTypeReference().getName().getArrayTypeForElementType()))),
	ANDROID_CONTEXT_GET_EXTERNAL_FILES_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getExternalFilesDir"),			 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_EXTERNAL_FILES_DIRS(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getExternalFilesDirs"),			 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
				TypeId.FILE.getTypeReference().getName().getArrayTypeForElementType()))),
	ANDROID_CONTEXT_GET_FILE_STREAM_PATH(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getFileStreamPath"),			 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_FILES_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getFilesDir"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_OBB_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getObbDir"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_OBB_DIRS(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getObbDirs"),			 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.FILE.getTypeReference().getName().getArrayTypeForElementType()))),
	ANDROID_CONTEXT_GET_PACKAGE_NAME(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getPackageName"),
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.STRING.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_PACKAGE_RESOURCE_PATH(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getPackageResourcePath"),
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.STRING.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_PACKAGE_CODE_PATH(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getPackageCodePath"),
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeId.STRING.getTypeReference().getName()))),
	ANDROID_CONTEXT_START_ACTIVITY(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("startActivity"),			 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.ANDROID_INTENT.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_START_SERVICE(MethodReference.findOrCreate(
				TypeId.ANDROID_CONTEXT.getTypeReference(),
				Atom.findOrCreateAsciiAtom("startService"),			 
				Descriptor.findOrCreate(
					new TypeName[]{TypeId.ANDROID_INTENT.getTypeReference().getName()}, 
					TypeId.ANDROID_COMPONENT_NAME.getTypeReference().getName()))),
	ANDROID_CONTEXT_START_ACTIVITY_BUNDLE(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("startActivity"),			 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_BUNDLE.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_BROADCAST_PERM(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendBroadcast"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_BROADCAST(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendBroadcast"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_BROADCAST_AS_USER(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendBroadcastAsUser"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_USER_HANDLE.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_BROADCAST_AS_USER_PERM(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendBroadcastAsUser"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_USER_HANDLE.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_ORDERED_BROADCAST_RESULT(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendOrderedBroadcast"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_RECEIVER.getTypeReference().getName(),
					TypeId.ANDROID_HANDLER.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_BUNDLE.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_ORDERED_BROADCAST(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendOrderedBroadcast"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_ORDERED_BROADCAST_AS_USER_RESULT(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendOrderedBroadcastAsUser"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_USER_HANDLE.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_RECEIVER.getTypeReference().getName(),
					TypeId.ANDROID_HANDLER.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_BUNDLE.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_STICKY_BROADCAST(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendStickyBroadcast"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_STICKY_BROADCAST_AS_USER(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendStickyBroadcastAsUser"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_USER_HANDLE.getTypeReference().getName()},
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_STICKY_ORDERED_BROADCAST(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendStickyOrderedBroadcast"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_RECEIVER.getTypeReference().getName(),
					TypeId.ANDROID_HANDLER.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_BUNDLE.getTypeReference().getName()},
				TypeReference.VoidName))),
	ANDROID_CONTEXT_SEND_STICKY_ORDERED_BROADCAST_AS_USER(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("sendStickyOrderedBroadcastAsUser"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_USER_HANDLE.getTypeReference().getName(),
					TypeId.ANDROID_RECEIVER.getTypeReference().getName(),
					TypeId.ANDROID_HANDLER.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_BUNDLE.getTypeReference().getName()},
				TypeReference.VoidName))),
	ANDROID_CONTEXT_OPEN_OR_CREATE_DATABASE(MethodReference.findOrCreate(
				TypeId.ANDROID_CONTEXT.getTypeReference(),
				Atom.findOrCreateAsciiAtom("openOrCreateDatabase"),
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeReference.IntName,
						TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName()
					},
					TypeId.ANDROID_SQLITE_DB.getTypeReference().getName()))),
	ANDROID_CONTEXT_OPEN_OR_CREATE_DATABASE_HANDLER(MethodReference.findOrCreate(
				TypeId.ANDROID_CONTEXT.getTypeReference(),
				Atom.findOrCreateAsciiAtom("openOrCreateDatabase"),
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeReference.IntName,
						TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
						TypeId.ANDROID_DB_ERR_HANDLER.getTypeReference().getName()
					},
					TypeId.ANDROID_SQLITE_DB.getTypeReference().getName()))),
	ANDROID_CONTEXT_REGISTER_RECEIVER(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("registerReceiver"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_RECEIVER.getTypeReference().getName(),
					TypeId.ANDROID_INTENT_FILTER.getTypeReference().getName()
				},
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_CONTEXT_REGISTER_RECEIVER_PERM_HANDLER(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("registerReceiver"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_RECEIVER.getTypeReference().getName(),
					TypeId.ANDROID_INTENT_FILTER.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_HANDLER.getTypeReference().getName()
				},
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_CONTEXT_GET_SHARED_PREFERENCES(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT.getTypeReference(),
			Atom.findOrCreateAsciiAtom("getSharedPreferences"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.ANDROID_SHARED_PREFERENCES.getTypeReference().getName()))),
				
	// android.content.Intent
	ANDROID_INTENT_INIT(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_INIT_INTENT(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.ANDROID_INTENT.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_INIT_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_INIT_STR_URI(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_URI.getTypeReference().getName()
				}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_INIT_CTX_CLASS(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.ANDROID_CONTEXT.getTypeReference().getName(), TypeReference.JavaLangClass.getName()}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_INIT_STR_URI_CTX_CLASS(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			MethodReference.initAtom, 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(), 
					TypeReference.JavaLangClass.getName()}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_FILL_IN(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("fillIn"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT.getTypeReference().getName(),
					TypeReference.IntName}, 
				TypeReference.IntName))),
	ANDROID_INTENT_PARSE_INTENT(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("parseIntent"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_RESOURCES.getTypeReference().getName(),
					TypeId.XML_PULL_PARSER.getTypeReference().getName(),
					TypeId.ANDROID_ATTRIBUTE_SET.getTypeReference().getName()
				},
			TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_PARSE_URI(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("parseUri"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
			TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_CLASS(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setClass"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeReference.JavaLangClass.getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_CLASS_NAME_CTX_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setClassName"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_CLASS_NAME_STR_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setClassName"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_COMPONENT(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setComponent"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_COMPONENT_NAME.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_DATA(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setData"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_DATA_AND_NORMALIZE(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setDataAndNormalize"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_DATA_AND_TYPE(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setDataAndType"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_DATA_AND_TYPE_AND_NORMALIZE(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setDataAndTypeAndNormalize"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_ACTION(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setAction"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_ADD_CATEGORY(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addCategory"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_REMOVE_CATEGORY(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("removeCategory"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()}, 
				TypeReference.VoidName))),
	ANDROID_INTENT_SET_TYPE(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setType"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_INTENT_SET_TYPE_AND_NORMALIZE(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setTypeAndNormalize"), 
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				}, 
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),

	// android.content.SharedPreferences
	ANDROID_SHARED_PREFERENCES_EDIT(MethodReference.findOrCreate(
			TypeId.ANDROID_SHARED_PREFERENCES.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("edit"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.ANDROID_SHARED_PREFERENCES_EDITOR.getTypeReference().getName()))),
				
	// android.content.ComponentName
	ANDROID_COMPONENT_NAME_INIT_STR_STR(MethodReference.findOrCreate(
		TypeId.ANDROID_COMPONENT_NAME.getTypeReference(), 
		MethodReference.initAtom,
		Descriptor.findOrCreate(
			new TypeName[]{
				TypeId.STRING.getTypeReference().getName(),
				TypeId.STRING.getTypeReference().getName()
			},
			TypeReference.VoidName))),
	ANDROID_COMPONENT_NAME_INIT_CTX_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_COMPONENT_NAME.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_COMPONENT_NAME_INIT_CTX_CLASS(MethodReference.findOrCreate(
			TypeId.ANDROID_COMPONENT_NAME.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeId.CLASS.getTypeReference().getName()
				},
			TypeReference.VoidName))),
	ANDROID_COMPONENT_NAME_INIT_PARCEL(MethodReference.findOrCreate(
			TypeId.ANDROID_COMPONENT_NAME.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_PARCEL.getTypeReference().getName()
				},
			TypeReference.VoidName))),
							
	// android.app.Activity
	ANDROID_ACTIVITY_SET_INTENT(MethodReference.findOrCreate(
			TypeId.ANDROID_ACTIVITY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setIntent"),
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.ANDROID_INTENT.getTypeReference().getName()},
				TypeReference.VoidName))),
	ANDROID_ACTIVITY_GET_INTENT(MethodReference.findOrCreate(
			TypeId.ANDROID_ACTIVITY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getIntent"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.ANDROID_INTENT.getTypeReference().getName()))),
	ANDROID_ACTIVITY_ON_CREATE(MethodReference.findOrCreate(
			TypeId.ANDROID_ACTIVITY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("onCreate"),
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.ANDROID_BUNDLE.getTypeReference().getName()},
				TypeReference.VoidName))),
	ANDROID_ACTIVITY_ON_START(MethodReference.findOrCreate(
			TypeId.ANDROID_ACTIVITY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("onStart"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	ANDROID_ACTIVITY_SET_RESULT(MethodReference.findOrCreate(
			TypeId.ANDROID_ACTIVITY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setResult"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	ANDROID_ACTIVITY_SET_RESULT_DATA(MethodReference.findOrCreate(
			TypeId.ANDROID_ACTIVITY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("setResult"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName,
					TypeId.ANDROID_INTENT.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// android.content.IntentFilter
	ANDROID_INTENT_FILTER_INIT(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_INIT_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_INIT_STR_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_INIT_FILTER(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_INTENT_FILTER.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_ACTION(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addAction"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_CATEGORY(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addCategory"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_DATA_AUTHORITY(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addDataAuthority"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_DATA_PATH(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addDataPath"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_DATA_SCHEME(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addDataScheme"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_DATA_SSP(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addDataSchemeSpecificPart"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	ANDROID_INTENT_FILTER_ADD_DATA_TYPE(MethodReference.findOrCreate(
			TypeId.ANDROID_INTENT_FILTER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("addDataType"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
				
	// android.content.res.AssetManager
	ANDROID_ASSET_MGR_OPEN(MethodReference.findOrCreate(
			TypeId.ANDROID_ASSET_MGR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("open"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.INPUT_STREAM.getTypeReference().getName()))),
	ANDROID_ASSET_MGR_OPEN_MODE(MethodReference.findOrCreate(
			TypeId.ANDROID_ASSET_MGR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("open"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.INPUT_STREAM.getTypeReference().getName()))),	
							
	// android.app.Service
	ANDROID_SERVICE_ON_CREATE(MethodReference.findOrCreate(
				TypeId.ANDROID_SERVICE.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("onCreate"),
				Descriptor.findOrCreate(
					new TypeName[]{},
					TypeReference.VoidName))),
					
	// android.app.Application
	ANDROID_APPLICATION_ON_CREATE(MethodReference.findOrCreate(
				TypeId.ANDROID_APPLICATION.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("onCreate"),
				Descriptor.findOrCreate(
					new TypeName[]{},
					TypeReference.VoidName))),

	// android.content.BroadcastReceiver
	ANDROID_RECEIVER_ON_RECEIVE(MethodReference.findOrCreate(
			TypeId.ANDROID_RECEIVER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("onReceive"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeId.ANDROID_INTENT.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// android.content.ContentProvider
	ANDROID_PROVIDER_ON_CREATE(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("onCreate"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.BooleanName))),
	ANDROID_PROVIDER_QUERY(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_PROVIDER_QUERY_CANCEL(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CANCELLATION_SIGNAL.getTypeReference().getName()
				},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_PROVIDER_BULK_INSERT(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("bulkInsert"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName().getArrayTypeForElementType(),
				},
				TypeReference.IntName))),
	ANDROID_PROVIDER_INSERT(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("insert"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName()
				},
				TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_PROVIDER_UPDATE(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("update"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				},
				TypeReference.IntName))),
	ANDROID_PROVIDER_DELETE(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("delete"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				},
				TypeReference.IntName))),
	ANDROID_PROVIDER_GET_TYPE(MethodReference.findOrCreate(
			TypeId.ANDROID_PROVIDER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getType"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName()
				},
				TypeId.STRING.getTypeReference().getName()))),

	// android.content.ContextWrapper
	ANDROID_CTX_WRAPPER_ATTACH_BASE_CTX(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTEXT_WRAPPER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("attachBaseContext"),
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.ANDROID_CONTEXT.getTypeReference().getName()},
				TypeReference.VoidName))),

	// android.database.sqlite.SQLiteDatabase
	ANDROID_SQLITE_DB_INIT_STR_INT_FACTORY_HANDLER(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeId.ANDROID_DB_ERR_HANDLER.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_SQLITE_DB_COMPILE_STM(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("compileStatement"),
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()},
				TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_EXEC_SQL(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("execSQL"),
			Descriptor.findOrCreate(
				new TypeName[]{TypeId.STRING.getTypeReference().getName()},
				TypeReference.VoidName))),
	ANDROID_SQLITE_DB_INSERT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("insert"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName()},
				TypeReference.LongName))),
	ANDROID_SQLITE_DB_INSERT_OR_THROW(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("insertOrThrow"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName()},
				TypeReference.LongName))),
	ANDROID_SQLITE_DB_INSERT_CONFLICT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("insertWithOnConflict"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName(),
					TypeReference.IntName},
				TypeReference.LongName))),
	ANDROID_SQLITE_DB_QUERY_LIMIT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_QUERY_DISTINCT_LIMIT_CANCEL(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.BooleanName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CANCELLATION_SIGNAL.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_QUERY(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_QUERY_DISTINCT_LIMIT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.BooleanName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_QUERY_FACT_CANCEL(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("queryWithFactory"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeReference.BooleanName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CANCELLATION_SIGNAL.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_QUERY_FACT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("queryWithFactory"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeReference.BooleanName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_RAW_QUERY_CANCEL(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("rawQuery"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.ANDROID_CANCELLATION_SIGNAL.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_RAW_QUERY(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("rawQuery"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_RAW_QUERY_FACT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("rawQueryWithFactory"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_RAW_QUERY_FACT_CANCEL(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("rawQueryWithFactory"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CANCELLATION_SIGNAL.getTypeReference().getName()},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_SQLITE_DB_REPLACE(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_DB.getTypeReference(),
			Atom.findOrCreateAsciiAtom("replace"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName()},
				TypeReference.LongName))),
	ANDROID_SQLITE_DB_REPLACE_THROW(MethodReference.findOrCreate(
				TypeId.ANDROID_SQLITE_DB.getTypeReference(),
				Atom.findOrCreateAsciiAtom("replaceOrThrow"),
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName(),
						TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName()},
					TypeReference.LongName))),
	ANDROID_SQLITE_DB_UPDATE(MethodReference.findOrCreate(
				TypeId.ANDROID_SQLITE_DB.getTypeReference(),
				Atom.findOrCreateAsciiAtom("update"),
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()},
					TypeReference.IntName))),
	ANDROID_SQLITE_DB_UPDATE_CONFLICT(MethodReference.findOrCreate(
				TypeId.ANDROID_SQLITE_DB.getTypeReference(),
				Atom.findOrCreateAsciiAtom("updateWithOnConflict"),
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.STRING.getTypeReference().getName(),
						TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
						TypeReference.IntName},
					TypeReference.IntName))),

	// android.database.sqlite.SQLiteStatement
	ANDROID_SQLITE_STM_EXEC(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	ANDROID_SQLITE_STM_EXEC_INSERT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("executeInsert"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.LongName))),
	ANDROID_SQLITE_STM_EXEC_UPDATE_DELETE(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("executeUpdateDelete"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.IntName))),
	ANDROID_SQLITE_STM_SIMPLE_QUERY_BLOB_FILE_FD(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("simpleQueryForBlobFileDescriptor"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.ANDROID_PARCEL_FILE_FD.getTypeReference().getName()))),
	ANDROID_SQLITE_STM_SIMPLE_QUERY_LONG(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("simpleQueryForLong"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.LongName))),
	ANDROID_SQLITE_STM_SIMPLE_QUERY_STR(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_STATEMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("simpleQueryForString"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.STRING.getTypeReference().getName()))),

	// android.database.sqlite.SQLiteOpenHelper
	ANDROID_SQLITE_OPEN_HELPER_INIT_CTX_STR_FAC_INT(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_OPEN_HELPER.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	ANDROID_SQLITE_OPEN_HELPER_INIT_CTX_STR_FAC_INT_HANDLER(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_OPEN_HELPER.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_SQLITE_DB_CURSOR_FACTORY.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.ANDROID_DB_ERR_HANDLER.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_SQLITE_OPEN_HELPER_GET_READABLE_DB(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_OPEN_HELPER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getReadableDatabase"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.ANDROID_SQLITE_DB.getTypeReference().getName()))),
	ANDROID_SQLITE_OPEN_HELPER_GET_WRITABLE_DB(MethodReference.findOrCreate(
			TypeId.ANDROID_SQLITE_OPEN_HELPER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getWritableDatabase"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.ANDROID_SQLITE_DB.getTypeReference().getName()))),

	// android.telephony.SmsManager
	ANDROID_SMS_MGR_SEND_TEXT_MSG(MethodReference.findOrCreate(
			TypeId.ANDROID_SMS_MGR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("sendTextMessage"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.ANDROID_PENDING_INTENT.getTypeReference().getName(),
					TypeId.ANDROID_PENDING_INTENT.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// android.location.LocationManager
	ANDROID_LOCATION_MGR_REQ_LOC_UPDATE(MethodReference.findOrCreate(
			TypeId.ANDROID_LOCATION_MANAGER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("requestLocationUpdates"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.LongName,
					TypeReference.FloatName,
					TypeId.ANDROID_LOCATION_LISTENER.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	ANDROID_LOCATION_MGR_GET_LAST_LOC(MethodReference.findOrCreate(
			TypeId.ANDROID_LOCATION_MANAGER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getLastKnownLocation"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.ANDROID_LOCATION.getTypeReference().getName()))),

	// android.telephony.TelephonyManager
	ANDROID_TELEPHONY_MGR_GET_DEVICE_ID(MethodReference.findOrCreate(
			TypeId.ANDROID_TELEPHONY_MGR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getDeviceId"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.STRING.getTypeReference().getName()))),

	// android.content.ContentResolver
	ANDROID_CONTENT_RESOLVER_INSERT(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTENT_RESOLVER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("insert"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName()
				},
				TypeId.ANDROID_URI.getTypeReference().getName()))),
	ANDROID_CONTENT_RESOLVER_BULK_INSERT(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTENT_RESOLVER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("bulkInsert"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName().getArrayTypeForElementType()
				},
				TypeReference.IntName))),
	ANDROID_CONTENT_RESOLVER_QUERY(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTENT_RESOLVER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("query"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_CONTENT_RESOLVER_QUERY_CANCEL(MethodReference.findOrCreate(
				TypeId.ANDROID_CONTENT_RESOLVER.getTypeReference(), 
				Atom.findOrCreateAsciiAtom("query"),
				Descriptor.findOrCreate(
					new TypeName[]{
						TypeId.ANDROID_URI.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
						TypeId.STRING.getTypeReference().getName(),
						TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType(),
						TypeId.STRING.getTypeReference().getName(),	
						TypeId.ANDROID_CANCELLATION_SIGNAL.getTypeReference().getName()
					},
					TypeId.ANDROID_CURSOR.getTypeReference().getName()))),
	ANDROID_CONTENT_RESOLVER_DELETE(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTENT_RESOLVER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("delete"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				},
				TypeReference.IntName))),
	ANDROID_CONTENT_RESOLVER_UPDATE(MethodReference.findOrCreate(
			TypeId.ANDROID_CONTENT_RESOLVER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("update"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_URI.getTypeReference().getName(),
					TypeId.ANDROID_CONTENT_VALUES.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName().getArrayTypeForElementType()
				},
				TypeReference.IntName))),
	ANDROID_PREFERENCE_MGR_GET_DEFAULT_SHARED_PREFERENCES(MethodReference.findOrCreate(
			TypeId.ANDROID_PREFERENCE_MGR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getDefaultSharedPreferences"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.ANDROID_CONTEXT.getTypeReference().getName()
				},
				TypeId.ANDROID_SHARED_PREFERENCES.getTypeReference().getName()))),
	ANDROID_PREFERENCE_MGR_GET_SHARED_PREFERENCES(MethodReference.findOrCreate(
			TypeId.ANDROID_PREFERENCE_MGR.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getSharedPreferences"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.ANDROID_SHARED_PREFERENCES.getTypeReference().getName()))),
				
	// android.os.Environment
	ANDROID_ENVIRONMENT_GET_DATA_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_ENVIRONMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getDataDirectory"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_ENVIRONMENT_GET_DOWNLOAD_CACHE_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_ENVIRONMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getDownloadCacheDirectory"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_ENVIRONMENT_GET_EXTERNAL_STORAGE_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_ENVIRONMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getExternalStorageDirectory"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_ENVIRONMENT_GET_EXTERNAL_STORAGE_PUBLIC_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_ENVIRONMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getExternalStoragePublicDirectory"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.FILE.getTypeReference().getName()))),
	ANDROID_ENVIRONMENT_GET_ROOT_DIR(MethodReference.findOrCreate(
			TypeId.ANDROID_ENVIRONMENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getRootDirectory"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.FILE.getTypeReference().getName()))),
							
	// org.apache.http.client.ResponseHandler
	APACHE_RESPONSE_HANDLER_HANDLE_RESPONSE(MethodReference.findOrCreate(
			TypeId.APACHE_RESPONSE_HANDLER.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("handleResponse"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()
				},
				TypeReference.JavaLangObject.getName()))),

	// org.apache.http.HttpHost
	APACHE_HTTP_HOST_INIT_STR_INT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HOST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_HOST_INIT_STR_INT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HOST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	APACHE_HTTP_HOST_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HOST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_HOST_INIT_HTTP_HOST(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HOST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_HOST.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpDelete
	APACHE_HTTP_DELETE_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_DELETE.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_DELETE_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_DELETE.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_DELETE_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_DELETE.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpGet
	APACHE_HTTP_GET_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_GET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_GET_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_GET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_GET_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_GET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpHead
	APACHE_HTTP_HEAD_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HEAD.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_HEAD_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HEAD.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_HEAD_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_HEAD.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpOptions
	APACHE_HTTP_OPTIONS_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_OPTIONS.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_OPTIONS_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_OPTIONS.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_OPTIONS_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_OPTIONS.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpTrace
	APACHE_HTTP_TRACE_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_TRACE.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_TRACE_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_TRACE.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_TRACE_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_TRACE.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpPost
	APACHE_HTTP_POST_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_POST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_POST_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_POST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_POST_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_POST.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// org.apache.http.client.methods.HttpPost
	APACHE_HTTP_PUT_INIT(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_PUT.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	APACHE_HTTP_PUT_INIT_URI(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_PUT.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URI.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	APACHE_HTTP_PUT_INIT_STR(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_PUT.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// java.net.URI
	URI_INIT_STR(MethodReference.findOrCreate(
			TypeId.URI.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URI_INIT_STR_STR_STR(MethodReference.findOrCreate(
			TypeId.URI.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URI_INIT_STR_STR_STR_INT_STR_STR_STR(MethodReference.findOrCreate(
			TypeId.URI.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URI_INIT_STR_STR_STR_STR(MethodReference.findOrCreate(
			TypeId.URI.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URI_INIT_STR_STR_STR_STR_STR(MethodReference.findOrCreate(
			TypeId.URI.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// java.net.Socket
	SOCKET_INIT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeReference.VoidName))),
	SOCKET_INIT_PROXY(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.PROXY.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	SOCKET_INIT_STR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	SOCKET_INIT_STR_INT_ADDR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	SOCKET_INIT_STR_INT_BOOL(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeReference.BooleanName
				},
				TypeReference.VoidName))),
	SOCKET_INIT_ADDR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	SOCKET_INIT_ADDR_INT_ADDR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	SOCKET_INIT_ADDR_INT_BOOL(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName,
					TypeReference.BooleanName
				},
				TypeReference.VoidName))),
	SOCKET_CONNECT_TIMEOUT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("connect"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.SOCKET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	SOCKET_CONNECT(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("connect"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.SOCKET_ADDRESS.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	SOCKET_GET_INPUT_STREAM(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getInputStream"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.INPUT_STREAM.getTypeReference().getName()))),
	SOCKET_GET_OUTPUT_STREAM(MethodReference.findOrCreate(
			TypeId.SOCKET.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getOutputStream"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.OUTPUT_STREAM.getTypeReference().getName()))),

	// java.net.ServerSocket
	SERVER_SOCKET_ACCEPT(MethodReference.findOrCreate(
			TypeId.SERVER_SOCKET.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("accept"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.SOCKET.getTypeReference().getName()))),

	// java.net.InetSocketAddress
	INET_SOCKET_ADDR_INIT_INT(MethodReference.findOrCreate(
			TypeId.INET_SOCKET_ADDRESS.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	INET_SOCKET_ADDR_INIT_ADDR_INT(MethodReference.findOrCreate(
			TypeId.INET_SOCKET_ADDRESS.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	INET_SOCKET_ADDR_INIT_STR_INT(MethodReference.findOrCreate(
			TypeId.INET_SOCKET_ADDRESS.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeReference.VoidName))),
	INET_SOCKET_ADDR_CREATE_UNRESOLVED(MethodReference.findOrCreate(
			TypeId.INET_SOCKET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("createUnresolved"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.INET_SOCKET_ADDRESS.getTypeReference().getName()))),

	// org.apache.http.client.HttpClient
	HTTP_CLIENT_EXECUTE_REQ_HANDLER_CTX(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference().getName(),
					TypeId.APACHE_RESPONSE_HANDLER.getTypeReference().getName(),
					TypeId.APACHE_HTTP_CONTEXT.getTypeReference().getName()
				},
				TypeReference.JavaLangObject.getName()))),
	HTTP_CLIENT_EXECUTE_REQ(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference().getName()
				},
				TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()))),
	HTTP_CLIENT_EXECUTE_HOST_REQ_HANDLER_CTX(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_HOST.getTypeReference().getName(),
					TypeId.APACHE_HTTP_REQUEST.getTypeReference().getName(),
					TypeId.APACHE_RESPONSE_HANDLER.getTypeReference().getName(),
					TypeId.APACHE_HTTP_CONTEXT.getTypeReference().getName()
				},
				TypeReference.JavaLangObject.getName()))),
	HTTP_CLIENT_EXECUTE_REQ_CTX(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference().getName(),
					TypeId.APACHE_HTTP_CONTEXT.getTypeReference().getName()
				},
				TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()))),
	HTTP_CLIENT_EXECUTE_REQ_HANDLER(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_URI_REQUEST.getTypeReference().getName(),
					TypeId.APACHE_RESPONSE_HANDLER.getTypeReference().getName()
				},
				TypeReference.JavaLangObject.getName()))),
	HTTP_CLIENT_EXECUTE_HOST_REQ_HANDLER(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_HOST.getTypeReference().getName(),
					TypeId.APACHE_HTTP_REQUEST.getTypeReference().getName(),
					TypeId.APACHE_RESPONSE_HANDLER.getTypeReference().getName()
				},
				TypeReference.JavaLangObject.getName()))),
	HTTP_CLIENT_EXECUTE_HOST_REQ(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_HOST.getTypeReference().getName(),
					TypeId.APACHE_HTTP_REQUEST.getTypeReference().getName()
				},
				TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()))),
	HTTP_CLIENT_EXECUTE_HOST_REQ_CTX(MethodReference.findOrCreate(
			TypeId.APACHE_HTTP_CLIENT.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("execute"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.APACHE_HTTP_HOST.getTypeReference().getName(),
					TypeId.APACHE_HTTP_REQUEST.getTypeReference().getName(),
					TypeId.APACHE_HTTP_CONTEXT.getTypeReference().getName()
				},
				TypeId.APACHE_HTTP_RESPONSE.getTypeReference().getName()))),

	// java.net.InetAddress
	INET_ADDR_GET_ALL_BY_NAME(MethodReference.findOrCreate(
			TypeId.INET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getAllByName"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.INET_ADDRESS.getTypeReference().getName().getArrayTypeForElementType()))),
	INET_ADDR_GET_BY_ADDRESS_HOST(MethodReference.findOrCreate(
			TypeId.INET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getByAddress"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.ByteArray.getName()
				},
				TypeId.INET_ADDRESS.getTypeReference().getName()))),
	INET_ADDR_GET_BY_ADDRESS(MethodReference.findOrCreate(
			TypeId.INET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getByAddress"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeReference.ByteArray.getName()
				},
				TypeId.INET_ADDRESS.getTypeReference().getName()))),
	INET_ADDR_GET_BY_NAME(MethodReference.findOrCreate(
			TypeId.INET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getByName"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeId.INET_ADDRESS.getTypeReference().getName()))),
	INET_ADDR_GET_LOCAL_HOST(MethodReference.findOrCreate(
			TypeId.INET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getLocalHost"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.INET_ADDRESS.getTypeReference().getName()))),
	INET_ADDR_GET_LOOPBACK_ADDR(MethodReference.findOrCreate(
			TypeId.INET_ADDRESS.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getLoopbackAddress"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.INET_ADDRESS.getTypeReference().getName()))),

	// java.net.URLConnection
	URL_CONNECTION_INIT_URL(MethodReference.findOrCreate(
			TypeId.URL_CONNECTION.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URL.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_CONNECTION_GET_OUTPUT_STREAM(MethodReference.findOrCreate(
			TypeId.URL_CONNECTION.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getOutputStream"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.OUTPUT_STREAM.getTypeReference().getName()))),
	URL_CONNECTION_GET_INPUT_STREAM(MethodReference.findOrCreate(
			TypeId.URL_CONNECTION.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("getInputStream"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.INPUT_STREAM.getTypeReference().getName()))),

	// java.net.HttpURLConnection
	HTTP_URL_CONNECTION_INIT_URL(MethodReference.findOrCreate(
			TypeId.HTTP_URL_CONNECTION.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URL.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// java.net.JarURLConnection
	JAR_URL_CONNECTION_INIT_URL(MethodReference.findOrCreate(
			TypeId.JAR_URL_CONNECTION.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URL.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// java.net.HttpsURLConnection
	HTTPS_URL_CONNECTION_INIT_URL(MethodReference.findOrCreate(
			TypeId.HTTPS_URL_CONNECTION.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URL.getTypeReference().getName()
				},
				TypeReference.VoidName))),

	// java.net.URL
	URL_INIT_STR(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_INIT_URL_STR(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URL.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_INIT_URL_STR_HANDLER(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.URL.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.URL_STREAM_HANDLER.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_INIT_STR_STR_STR(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_INIT_STR_STR_INT_STR(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_INIT_STR_STR_INT_STR_HANDLER(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			MethodReference.initAtom,
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.STRING.getTypeReference().getName(),
					TypeId.URL_STREAM_HANDLER.getTypeReference().getName()
				},
				TypeReference.VoidName))),
	URL_OPEN_CONNECTION_PROXY(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("openConnection"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.PROXY.getTypeReference().getName()
				},
				TypeId.URL_CONNECTION.getTypeReference().getName()))),
	URL_OPEN_CONNECTION(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("openConnection"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.URL_CONNECTION.getTypeReference().getName()))),
	URL_OPEN_STREAM(MethodReference.findOrCreate(
			TypeId.URL.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("openStream"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.INPUT_STREAM.getTypeReference().getName()))),
				
	// javax.net.SocketFactory
	SOCKET_FACTORY_CREATE_SOCKET_STR_INT_ADDR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET_FACTORY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("createSocket"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.SOCKET.getTypeReference().getName()))),
	SOCKET_FACTORY_CREATE_SOCKET_ADDR_INT_ADDR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET_FACTORY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("createSocket"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName,
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.SOCKET.getTypeReference().getName()))),
	SOCKET_FACTORY_CREATE_SOCKET_ADDR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET_FACTORY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("createSocket"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.INET_ADDRESS.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.SOCKET.getTypeReference().getName()))),
	SOCKET_FACTORY_CREATE_SOCKET_STR_INT(MethodReference.findOrCreate(
			TypeId.SOCKET_FACTORY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("createSocket"),
			Descriptor.findOrCreate(
				new TypeName[]{
					TypeId.STRING.getTypeReference().getName(),
					TypeReference.IntName
				},
				TypeId.SOCKET.getTypeReference().getName()))),
	SOCKET_FACTORY_CREATE_SOCKET(MethodReference.findOrCreate(
			TypeId.SOCKET_FACTORY.getTypeReference(), 
			Atom.findOrCreateAsciiAtom("createSocket"),
			Descriptor.findOrCreate(
				new TypeName[]{},
				TypeId.SOCKET.getTypeReference().getName())));

	private static class NoLoaderMethodReference
	{
		public TypeName mClassName;
		public Selector mSelector;
		public NoLoaderMethodReference(TypeName className, Selector selector)
		{
			if(className == null || selector == null)
				throw new IllegalArgumentException("null");
			mClassName = className;
			mSelector = selector;
		}
		@Override
		public int hashCode()
		{
			return mClassName.hashCode() * 1607 + mSelector.hashCode();
		}
		@Override
		public boolean equals(Object other)
		{
			if(this == other)
				return true;
			if(!(other instanceof NoLoaderMethodReference))
				return false;
			NoLoaderMethodReference that = (NoLoaderMethodReference)other;
			return mClassName.equals(that.mClassName) &&
					mSelector.equals(that.mSelector);
		}
	}
	
	private final MethodReference mMethodRef;
	private MethodId()
	{
		mMethodRef = null;
	}
	private MethodId(MethodReference methodRef)
	{
		mMethodRef = methodRef;
	}
	public MethodReference getMethodReference()
	{
		return mMethodRef;
	}
	
	private final static Map<NoLoaderMethodReference, MethodId> METHOD_MAP = 
			new HashMap<NoLoaderMethodReference, MethodId>();
	static
	{
		for(MethodId method : MethodId.values())
		{
			MethodReference methodRef = method.getMethodReference();
			if(methodRef != null)
				METHOD_MAP.put(new NoLoaderMethodReference(methodRef.getDeclaringClass().getName(), methodRef.getSelector()), method);
		}		
	}
	public static MethodId getMethodId(MethodReference method)
	{
		return getMethodId(method.getDeclaringClass().getName(), method.getSelector());
	}
	public static MethodId getMethodId(TypeName clazzName, Selector selector)
	{
		MethodId methodId = METHOD_MAP.get(new NoLoaderMethodReference(clazzName, selector));
		return methodId == null ? INVALID : methodId;
	}
}
