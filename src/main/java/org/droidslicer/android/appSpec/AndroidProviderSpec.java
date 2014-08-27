package org.droidslicer.android.appSpec;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AndroidProviderSpec extends AppComponentSpec
{

	public AndroidProviderSpec()
	{
		super(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/ContentProvider"));
	}

}
