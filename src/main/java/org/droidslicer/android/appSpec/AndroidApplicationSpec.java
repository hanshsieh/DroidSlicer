package org.droidslicer.android.appSpec;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AndroidApplicationSpec extends EntryCompSpec
{
	public AndroidApplicationSpec()
	{
		super(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Application"));
	}

}
