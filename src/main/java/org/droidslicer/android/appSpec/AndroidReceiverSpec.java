package org.droidslicer.android.appSpec;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class AndroidReceiverSpec extends AppComponentSpec
{

	public AndroidReceiverSpec()
	{
		super(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/BroadcastReceiver"));
	}
	
}
