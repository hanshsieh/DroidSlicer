package org.droidslicer.graph.entity.resolver;

import org.droidslicer.util.TypeId;

public class FileInputEntityResolver extends FileInOutAllocResolver
{

	public FileInputEntityResolver()
	{
		super(TypeId.FILE_INPUT_STREAM.getTypeReference());
	}
	
}
