package org.droidslicer.graph.entity;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

public class VirtualPermissionUnit extends SUseUnit
{
	private final Set<String> mPerms = new HashSet<String>();
	public void addPermission(String perm)
	{
		mPerms.add(perm);
	}
	@Override
	public Collection<String> getPermissions()
	{
		return mPerms;
	}
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(VirtualPermissionUnit.class.getSimpleName());
		builder.append(" perms=[");
		boolean first = true;
		for(String perm : mPerms)
		{
			if(first)
				first = false;
			else
			{
				builder.append(", ");
			}
			builder.append('"');
			builder.append(StringEscapeUtils.escapeJava(perm));
			builder.append('"');
		}
		builder.append("]]");
		return builder.toString();
	}
	@Override
	public boolean visit(IEntityVisitor visitor)
	{
		if(visitor.visitVirtualPermissionUnit(this))
			return true;
		else
			return super.visit(visitor);
	}
}
