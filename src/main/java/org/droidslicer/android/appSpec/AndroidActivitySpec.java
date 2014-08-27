package org.droidslicer.android.appSpec;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AndroidActivitySpec extends AppComponentSpec
{
	public AndroidActivitySpec()
	{
		super(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Activity"));
	}
}
