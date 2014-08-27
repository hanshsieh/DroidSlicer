package org.droidslicer.android.appSpec;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AndroidServiceSpec extends AppComponentSpec
{
	public AndroidServiceSpec()
	{
		super(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/app/Service"));
	}
}
