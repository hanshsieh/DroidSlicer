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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ExceptionalReturnCaller;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.ISDG;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntSet;

/**
 * It is a clone of com.ibm.wala.ipa.slicer.SDGSupergraph with some trivial modification to
 * make the code looks better.
 * We did this because com.ibm.wala.ipa.slicer.SDGSupergraph isn't public.
 */
public class SDGSupergraph implements ISupergraph<Statement, PDG> 
{
	protected final ISDG sdg;

	public SDGSupergraph(ISDG sdg) 
	{
		this.sdg = sdg;
	}
	public ISDG getSDG()
	{
		return sdg;
	}
	@Override
	public Graph<PDG> getProcedureGraph() 
	{
		throw new RuntimeException("Unimplemented");
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#classifyEdge(java.lang.Object, java.lang.Object)
	 */
	@Override
	public byte classifyEdge(Statement src, Statement dest) 
	{
		throw new RuntimeException("Unimplemented");
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCallSites(java.lang.Object)
	 */
	@Override
	public Iterator<? extends Statement> getCallSites(Statement r, PDG callee) 
	{
		switch (r.getKind()) 
	{
		case EXC_RET_CALLER: 
		{
			ExceptionalReturnCaller n = (ExceptionalReturnCaller) r;
			SSAAbstractInvokeInstruction call = n.getInstruction();
			PDG pdg = getProcOf(r);
			return pdg.getCallStatements(call).iterator();
		}
		case NORMAL_RET_CALLER: 
		{
			NormalReturnCaller n = (NormalReturnCaller) r;
			SSAAbstractInvokeInstruction call = n.getInstruction();
			PDG pdg = getProcOf(r);
			return pdg.getCallStatements(call).iterator();
		}
		case HEAP_RET_CALLER: 
		{
			HeapStatement.HeapReturnCaller n = (HeapStatement.HeapReturnCaller) r;
			SSAAbstractInvokeInstruction call = n.getCall();
			PDG pdg = getProcOf(r);
			return pdg.getCallStatements(call).iterator();
		}
		default:
			Assertions.UNREACHABLE(r.getKind().toString());
			return null;
		}
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCalledNodes(java.lang.Object)
	 */
	@Override
	public Iterator<? extends Statement> getCalledNodes(Statement call) 
	{
		switch (call.getKind()) 
		{
		case NORMAL:
			return Iterators.filter(getSuccNodes(call), new Predicate<Statement>(){
				@Override
				public boolean apply(Statement stm) {
					return isEntry(stm);
				}
				
			});
		case PARAM_CALLER:
		case HEAP_PARAM_CALLER:
			return getSuccNodes(call);
		default:
			Assertions.UNREACHABLE(call.getKind().toString());
			return null;
		}
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getEntriesForProcedure(java.lang.Object)
	 */
	@Override
	public Statement[] getEntriesForProcedure(PDG procedure) 
	{
		Statement[] normal = procedure.getParamCalleeStatements();
		Statement[] result = new Statement[normal.length + 1];
		result[0] = new MethodEntryStatement(procedure.getCallGraphNode());
		System.arraycopy(normal, 0, result, 1, normal.length);
		return result;
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getExitsForProcedure(java.lang.Object)
	 */
	@Override
	public Statement[] getExitsForProcedure(PDG procedure) 
	{
		Statement[] normal = procedure.getReturnStatements();
		Statement[] result = new Statement[normal.length + 1];
		result[0] = new MethodExitStatement(procedure.getCallGraphNode());
		System.arraycopy(normal, 0, result, 1, normal.length);
		return result;
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlock(java.lang.Object, int)
	 */
	@Override
	public Statement getLocalBlock(PDG procedure, int i) 
	{
		return procedure.getNode(i);
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlockNumber(java.lang.Object)
	 */
	@Override
	public int getLocalBlockNumber(Statement n) 
	{
		PDG pdg = getProcOf(n);
		return pdg.getNumber(n);
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNormalSuccessors(java.lang.Object)
	 */
	@Override
	public Iterator<Statement> getNormalSuccessors(Statement call) 
	{
		return EmptyIterator.instance();
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNumberOfBlocks(java.lang.Object)
	 */
	@Override
	public int getNumberOfBlocks(PDG procedure) 
	{
		throw new RuntimeException("Unimplemented");
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getProcOf(java.lang.Object)
	 */
	@Override
	public PDG getProcOf(Statement n) 
	{
		CGNode node = n.getNode();
		PDG result = sdg.getPDG(node);
		if (result == null) 
		{
			Assertions.UNREACHABLE("panic: " + n + " " + node);
		}
		return result;
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getReturnSites(java.lang.Object)
	 */
	@Override
	public Iterator<? extends Statement> getReturnSites(Statement call, PDG callee) 
	{
		switch (call.getKind()) 
		{
		case PARAM_CALLER: 
		{
			ParamCaller n = (ParamCaller) call;
			SSAAbstractInvokeInstruction st = n.getInstruction();
			PDG pdg = getProcOf(call);
			return pdg.getCallerReturnStatements(st).iterator();
		}
		case HEAP_PARAM_CALLER: 
		{
			HeapStatement.HeapParamCaller n = (HeapStatement.HeapParamCaller) call;
			SSAAbstractInvokeInstruction st = n.getCall();
			PDG pdg = getProcOf(call);
			return pdg.getCallerReturnStatements(st).iterator();
		}
		case NORMAL: 
		{
			NormalStatement n = (NormalStatement) call;
			SSAAbstractInvokeInstruction st = (SSAAbstractInvokeInstruction) n.getInstruction();
			PDG pdg = getProcOf(call);
			return pdg.getCallerReturnStatements(st).iterator();
		}
		default:
			Assertions.UNREACHABLE(call.getKind().toString());
			return null;
		}
	}

	/*
	 * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isCall(java.lang.Object)
	 */
	@Override
	public boolean isCall(Statement n) 
	{
		switch (n.getKind()) 
		{
		case EXC_RET_CALLEE:
		case EXC_RET_CALLER:
		case HEAP_PARAM_CALLEE:
		case NORMAL_RET_CALLEE:
		case NORMAL_RET_CALLER:
		case PARAM_CALLEE:
		case PHI:
		case HEAP_RET_CALLEE:
		case HEAP_RET_CALLER:
		case METHOD_ENTRY:
		case METHOD_EXIT:
		case CATCH:
		case PI:
			return false;
		case HEAP_PARAM_CALLER:
		case PARAM_CALLER:
			return true;
		case NORMAL:
			if (sdg.getCOptions().equals(ControlDependenceOptions.NONE)) {
				return false;
			} 
			else 
			{
				NormalStatement s = (NormalStatement) n;
				return s.getInstruction() instanceof SSAAbstractInvokeInstruction;
			}
		default:
			Assertions.UNREACHABLE(n.getKind() + " " + n.toString());
			return false;
		}
	}

	@Override
	public boolean isEntry(Statement n) 
	{
		switch (n.getKind()) 
		{
		case PARAM_CALLEE:
		case HEAP_PARAM_CALLEE:
		case METHOD_ENTRY:
			return true;
		case PHI:
		case PI:
		case NORMAL_RET_CALLER:
		case PARAM_CALLER:
		case HEAP_RET_CALLER:
		case NORMAL:
		case EXC_RET_CALLEE:
		case EXC_RET_CALLER:
		case HEAP_PARAM_CALLER:
		case HEAP_RET_CALLEE:
		case NORMAL_RET_CALLEE:
		case CATCH:
			return false;
		default:
			Assertions.UNREACHABLE(n.toString());
			return false;
		}
	}

	@Override
	public boolean isExit(Statement n) 
	{
		switch (n.getKind()) {
		case PARAM_CALLEE:
		case HEAP_PARAM_CALLEE:
		case HEAP_PARAM_CALLER:
		case PHI:
		case PI:
		case NORMAL_RET_CALLER:
		case PARAM_CALLER:
		case HEAP_RET_CALLER:
		case NORMAL:
		case EXC_RET_CALLER:
		case METHOD_ENTRY:
		case CATCH:
			return false;
		case HEAP_RET_CALLEE:
		case EXC_RET_CALLEE:
		case NORMAL_RET_CALLEE:
		case METHOD_EXIT:
			return true;
		default:
			Assertions.UNREACHABLE(n.toString());
			return false;
		}
	}

	@Override
	public boolean isReturn(Statement n) 
	{
		switch (n.getKind()) 
		{
		case EXC_RET_CALLER:
		case NORMAL_RET_CALLER:
		case HEAP_RET_CALLER:
			return true;
		case EXC_RET_CALLEE:
		case HEAP_PARAM_CALLEE:
		case HEAP_PARAM_CALLER:
		case HEAP_RET_CALLEE:
		case NORMAL:
		case NORMAL_RET_CALLEE:
		case PARAM_CALLEE:
		case PARAM_CALLER:
		case PHI:
		case PI:
		case METHOD_ENTRY:
		case CATCH:
			return false;
		default:
			Assertions.UNREACHABLE(n.getKind().toString());
			return false;
		}
	}

	@Override
	public void removeNodeAndEdges(Statement N) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void addNode(Statement n) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public boolean containsNode(Statement N) 
	{
		return sdg.containsNode(N);
	}

	@Override
	public int getNumberOfNodes() 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public Iterator<Statement> iterator() 
	{
		return sdg.iterator();
	}

	@Override
	public void removeNode(Statement n) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void addEdge(Statement src, Statement dst) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int getPredNodeCount(Statement N) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public Iterator<Statement> getPredNodes(Statement stm) 
	{
		return sdg.getPredNodes(stm);
	}

	@Override
	public int getSuccNodeCount(Statement N) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public Iterator<Statement> getSuccNodes(Statement stm) 
	{
		return sdg.getSuccNodes(stm);
	}
	
	@Override
	public boolean hasEdge(Statement src, Statement dst) 
	{
		return sdg.hasEdge(src, dst);
	}

	@Override
	public void removeAllIncidentEdges(Statement node) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void removeEdge(Statement src, Statement dst) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void removeIncomingEdges(Statement node) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void removeOutgoingEdges(Statement node) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int getMaxNumber() 
	{
		return sdg.getMaxNumber();
	}

	@Override
	public Statement getNode(int number) 
	{
		return sdg.getNode(number);
	}

	@Override
	public int getNumber(Statement N) 
	{
		return sdg.getNumber(N);
	}

	@Override
	public Iterator<Statement> iterateNodes(IntSet s) 
	{
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public IntSet getPredNodeNumbers(Statement node) 
	{
		return sdg.getPredNodeNumbers(node);
	}

	/*
	 * @see com.ibm.wala.util.graph.NumberedEdgeManager#getSuccNodeNumbers(java.lang.Object)
	 */
	@Override
	public IntSet getSuccNodeNumbers(Statement node) 
	{
		return sdg.getSuccNodeNumbers(node);
	}

}
