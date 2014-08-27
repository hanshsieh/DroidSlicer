package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.TypeId;

public class FileOutputEntityResolver extends FileInOutAllocResolver
{

	public FileOutputEntityResolver()
	{
		super(TypeId.FILE_OUTPUT_STREAM.getTypeReference());
	}

}
