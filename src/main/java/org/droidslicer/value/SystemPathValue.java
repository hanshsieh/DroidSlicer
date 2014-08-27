package org.droidslicer.value;


public class SystemPathValue extends ConcreteValue
{
	enum Kind
	{
		// e.g. /data/data/<package name>/
		// It can be obtained by ApplicationInfo.
		// Directories in it can be created by Context.getDir(String, int).
		ANDROID_APP_DATA_DIR,
		
		// It can be obtained by ApplicationInfo.nativeLibraryDir
		ANDROID_APP_NATIVE_LIBS_DIR, 
		
		// e.g. /data/data/<package name>/files
		// It can be obtained by Context.getFilesDir
		// The files in this directory can be open or created by 
		// Context.openFileOutput. The path of a file in this directory 
		// can be obtained by Context.getFileStreamPath
		ANDROID_APP_FILES_DIR,
		
		// e.g. /data/data/com.study/databases/
		ANDROID_APP_DBS_DIR,
		
		// e.g. /data/data/<package name>/cache
		ANDROID_APP_CACHE_DIR,
		
		// e.g. /data/app/*.apk
		// It can be obtained by Context.getPackageCodePath, ApplicationInfo.sourceDir 
		ANDROID_APP_PKG_SRC,
		
		// For non-forward-locked apps this will be the same as ANDROID_APP_PKG_SRC.
		// It can be obtained by ApplicationInfo.publicSourceDir
		ANDROID_APP_PUBLIC_PKG_SRC,
		
		// e.g. /data/app/*.apk
		// It can be obtained by Context.getPackageResourcePath
		// It may be either ApplicationInfo.sourceDir or ApplicationInfo.publicSourceDir.
		// In most case, it is the same as ANDROID_APP_PKG_SRC.
		// The definition of it is at 
		// android.app.LoadedApk#LoadedApk(ActivityThread, ApplicationInfo, CompatibilityInfo, ActivityThread, ClassLoader, boolean, boolean)
		ANDROID_APP_PKG_RES,
		
		// e.g. /mnt/sdcard/Android/data/<package name>/cache
		// It can be obtained by Context.getExternalCacheDir, Context.getExternalCacheDirs
		ANDROID_APP_EXT_CACHE_DIR,
		
		// It can be obtained by Context.getExternalFilesDir, Context.getExternalFilesDirs
		ANDROID_APP_EXT_FILES_DIR,
		
		// It can be obtained by Context.getObbDir, Context.getObbDirs
		ANDROID_APP_OBB_DIR,
		
		// It represent the path to a shared library file linked with an application. 
		// It can be obtained by ApplicationInfo.sharedLibraryFiles
		ANDROID_APP_SHARED_LIB_FILE
	}

	@Override
	public ConcreteValue getStringValue()
	{
		// TODO Maybe we should do better
		return this;
	}
}
