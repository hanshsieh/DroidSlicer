package org.droidslicer.analysis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.droidslicer.android.AndroidAPKFormatException;
import org.droidslicer.android.manifest.AndroidManifest;
import org.droidslicer.android.manifest.AndroidManifestParser;
import org.droidslicer.android.manifest.AndroidManifestSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;
import com.ibm.wala.types.TypeReference;

public class AndroidAppInfo
{
	private final static String ANDROID_MANIFEST = "AndroidManifest.xml";
	private final static String CLASSES_DEX = "classes.dex";
	private final static Logger mLogger = LoggerFactory.getLogger(AndroidAppInfo.class);
	private final static String PERM_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
	private final static String PERM_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
	
	private AndroidManifest mManifest;
	private TypeReference mCtxImplTypeRef;
	private File mClassesJar = null;
	private AndroidAppInfo()
	{}
	@Override
	protected void finalize()
		throws Throwable
	{
		close();
	}
	public void close() throws IOException
	{
		if(mClassesJar != null)
		{
			try
			{
				mClassesJar.delete();
			}
			catch(Exception ex)
			{
				throw new IOException(ex);
			}
			mClassesJar = null;
		}
	}
	public AndroidManifest getManifest()
	{
		return mManifest;
	}
	public void setManifest(AndroidManifest manifest)
	{
		mManifest = manifest;
	}
	public File getClassesJar()
	{
		return mClassesJar;		
	}
	public TypeReference getContextImplType()
	{
		return mCtxImplTypeRef;		
	}
	public void setContextImplType(TypeReference ctxImplTypeRef)
	{
		mCtxImplTypeRef = ctxImplTypeRef;
	}
	public static AndroidAppInfo createFromAPK(InputStream stream, File tmpDir)
		throws IOException, AndroidManifestSyntaxException, AndroidAPKFormatException
	{
		if(!tmpDir.isDirectory())
			throw new IllegalArgumentException("Tmp directory is expected");
		ZipInputStream apkStream = null;
		try
		{
			AndroidAppInfo result = new AndroidAppInfo();
			apkStream = new ZipInputStream(stream);
			ZipEntry entry;
			while((entry = apkStream.getNextEntry()) != null)
			{
				if(ANDROID_MANIFEST.equals(entry.getName()))
				{
					AndroidManifest manifest = AndroidManifestParser.parseFromCompressed(apkStream, false);
					result.setManifest(manifest);
					
					// If both your minSdkVersion and targetSdkVersion values are set to 3 or lower, 
					// the system implicitly grants your app this permission.
					// See https://developer.android.com/reference/android/Manifest.permission.html#READ_EXTERNAL_STORAGE
					if(manifest.getMinSDKVersion() <= 3 && manifest.getTargetSDKVersion() <= 3)
					{
						manifest.addPermission(new AndroidManifest.Permission(PERM_WRITE_EXTERNAL_STORAGE));
						manifest.addPermission(new AndroidManifest.Permission(PERM_READ_EXTERNAL_STORAGE));
					}
					
					Set<AndroidManifest.Permission> newPerms = new HashSet<AndroidManifest.Permission>();
					for(AndroidManifest.Permission perm : manifest.getPermissions())
					{
						// Any app that declares the WRITE_EXTERNAL_STORAGE permission is implicitly 
						// granted PERM_WRITE_EXTERNAL_STORAGE.
						if(perm.getName().equals(PERM_WRITE_EXTERNAL_STORAGE))
							newPerms.add(new AndroidManifest.Permission(PERM_READ_EXTERNAL_STORAGE));
					}
					for(AndroidManifest.Permission newPerm : newPerms)
					{
						manifest.addPermission(newPerm);
					}
				}
				else if(CLASSES_DEX.equals(entry.getName()))
				{
					Path classesPath = Files.createTempFile(tmpDir.toPath(), "classes_", ".jar");
					result.mClassesJar = classesPath.toFile();
					result.mClassesJar.deleteOnExit();
					Dex2jar dex2jar = Dex2jar.from(new DexFileReader(apkStream));
					dex2jar.skipDebug(true);
					dex2jar.reUseReg(false); // Should we turn on this?
					FileOutputStream output = new FileOutputStream(result.mClassesJar);
					try
					{
						mLogger.info("Converting DEX code to Java bytecode...");
						dex2jar.to(output);
						mLogger.info("Conversion finished");
					}
					finally
					{
						output.close();
					}
					/*Path classesDirPath = Files.createTempDirectory(tmpDir.toPath(), "bytecode_");
					result.mClassesDir = classesDirPath.toFile();
					Utils.registerDirectoryDelete(result.mClassesDir);
					Dex2jar dex2jar = Dex2jar.from(new DexFileReader(apkStream));
					dex2jar.skipDebug(true);
					dex2jar.reUseReg(false); // Should we turn on this?
					mLogger.info("Converting DEX code to Java bytecode...");
					dex2jar.to(result.mClassesDir);
					mLogger.info("Conversion finished");*/
				}
			}
			if(result.getManifest() == null)
				throw new AndroidAPKFormatException("Missing AndroidManifest.xml");
			if(result.mClassesJar == null)
				throw new AndroidAPKFormatException("Missing classes.dex");
			return result;
		}
		finally
		{
			try
			{
				apkStream.close();
			}
			catch(Exception ex)
			{}
			try
			{
				stream.close();
			}
			catch(Exception ex)
			{}
		}
	}
}
