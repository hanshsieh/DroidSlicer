package org.droidslicer.ifds;

import org.jgrapht.graph.DefaultDirectedGraph;

import com.ibm.wala.ipa.slicer.Statement;

public class SliceGraph extends DefaultDirectedGraph<Statement, DependencyEdge>
{
	public SliceGraph()
	{
		super(DependencyEdge.class);
	}
}
