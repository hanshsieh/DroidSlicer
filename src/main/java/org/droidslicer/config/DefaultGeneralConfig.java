package org.droidslicer.config;

import java.io.File;
import java.io.IOException;

public class DefaultGeneralConfig extends AbstractGeneralConfig
{
	private File mTmpDir = new File("tmp");
	public DefaultGeneralConfig()
		throws IOException
	{
		prepareTmpDir();
	}
	private void prepareTmpDir()
		throws IOException
	{
		if(mTmpDir.exists())
		{
			if(!mTmpDir.isDirectory())
				throw new IOException(mTmpDir.getAbsolutePath() + " already exists, but it's not directory");
		}
		else
		{
			if(!mTmpDir.mkdir())
				throw new IOException("Fail to create tmp directory");
		}
	}
	@Override
	public File getTempDirectory()
	{
		return mTmpDir;
	}

}
