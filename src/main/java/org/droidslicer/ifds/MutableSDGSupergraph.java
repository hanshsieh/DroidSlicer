/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.droidslicer.ifds;

import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.ibm.wala.ipa.slicer.ISDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

/**
 * A wrapper around an SDG to make it look like a supergraph for tabulation.
 */
public class MutableSDGSupergraph extends SDGSupergraph
{
	private final Graph<Statement> mExtraGraph = new SlowSparseNumberedGraph<Statement>(1);
	private boolean mHideExtraEdges = false;

	public MutableSDGSupergraph(ISDG sdg) 
	{
		super(sdg);
	}

	public void setHideExtraEdges(boolean val)
	{
		mHideExtraEdges = val;
	}
	public boolean isHideExtraEdges()
	{
		return mHideExtraEdges;
	}
	@Override
	public boolean hasEdge(Statement src, Statement dst) 
	{
		if(sdg.hasEdge(src, dst))
			return true;
		if(mHideExtraEdges)
			return false;
		else
			return mExtraGraph.hasEdge(src, dst);		
	}
	@Override
	public void addEdge(Statement src, Statement dst) 
	{
		if(!sdg.containsNode(src) || !sdg.containsNode(dst))
			throw new IllegalArgumentException();
		if(sdg.hasEdge(src, dst))
			return;
		mExtraGraph.addNode(src);
		mExtraGraph.addNode(dst);
		mExtraGraph.addEdge(src, dst);
	}
	@Override
	public Iterator<Statement> getPredNodes(Statement stm) 
	{
		Iterator<Statement> itr = sdg.getPredNodes(stm);
		if(!mHideExtraEdges && mExtraGraph.containsNode(stm))
			return Iterators.concat(itr, mExtraGraph.getPredNodes(stm));
		else
			return itr;
	}
	@Override
	public Iterator<Statement> getSuccNodes(Statement stm) 
	{
		Iterator<Statement> itr = sdg.getSuccNodes(stm);
		if(!mHideExtraEdges && mExtraGraph.containsNode(stm))
			return Iterators.concat(itr, mExtraGraph.getSuccNodes(stm));
		else
			return itr;
	}
}
