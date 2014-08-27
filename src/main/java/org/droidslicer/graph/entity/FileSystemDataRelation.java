package org.droidslicer.graph.entity;

public class FileSystemDataRelation extends RelationEntity
{
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitFileSystemDataRelation(this))
			return true;
		else 
			return super.visit(visitor);
	}
}
