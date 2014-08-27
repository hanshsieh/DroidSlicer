package org.droidslicer.value.solver;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.droidslicer.ifds.CallRecords;
import org.droidslicer.util.ProgressMonitor;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

public abstract class ValueUsageWalker extends SSAInstruction.Visitor
{
	public static interface EndCriterion
	{
		public boolean shouldEnd(CGNode node, int instIdx);
	}
	public static class FixPointEndCriterion implements EndCriterion
	{
		private final CGNode mEndNode;
		private final int mEndInstIdx;
		public FixPointEndCriterion(CGNode endNode, int endInstIdx)
		{
			if(endNode == null || endInstIdx < 0 || endInstIdx >= endNode.getIR().getInstructions().length)
				throw new IllegalArgumentException();
			mEndNode = endNode;
			mEndInstIdx = endInstIdx;
		}
		@Override
		public boolean shouldEnd(CGNode node, int instIdx)
		{
			return instIdx == mEndInstIdx && node.equals(mEndNode);
		}
	}
	public static class NoEndCriterion implements EndCriterion
	{
		private static NoEndCriterion mInstance;
		public static NoEndCriterion getInstance()
		{
			if(mInstance == null)
				mInstance = new NoEndCriterion();
			return mInstance;			
		}
		@Override
		public boolean shouldEnd(CGNode node, int instIdx)
		{
			return false;
		}
	}
	private final CallGraph mCg;
	private final CGNode mStartNode;
	private final int mStartInstIdx;
	private final EndCriterion mEndCriterion;
	private final MutableIntSet mValNums = new BitVectorIntSet();
	private final Set<SSACFG.BasicBlock> mVisitedBlocks = new HashSet<SSACFG.BasicBlock>();
	private int mFirstUseIdx = -1;
	private boolean mCutPath = false;
	private boolean mTerminate = false;
	private int mInstIdx = -1;
	private CallRecords mCallRecords = null;
	private ProgressMonitor mMonitor = null;
	
	public ValueUsageWalker(CallGraph cg, CGNode startNode, int startInstIdx, IntSet valNums, EndCriterion endCriterion)
	{
		if(cg == null || startNode == null || startInstIdx < 0 || startInstIdx >= startNode.getIR().getInstructions().length || endCriterion == null)
			throw new IllegalArgumentException();
		mCg = cg;
		mValNums.addAll(valNums);
		mStartNode = startNode;
		mStartInstIdx = startInstIdx;
		mEndCriterion = endCriterion;
	}
	public void onStartNode(CGNode node)
	{}
	public void onEndNode(CGNode node)
	{}
	public void onStartBasicBlock(SSACFG.BasicBlock block, int instIdx)
	{}
	public void onRevisitBasicBlock(SSACFG.BasicBlock block, int instIdx)
	{}
	public void onEndBasicBlock(SSACFG.BasicBlock block)
	{}
	public void onEndCondition(SSACFG.BasicBlock block)
	{}
	public void onCutPath(SSACFG.BasicBlock block)
	{}
	public void onStart()
	{}
	public void onEnd()
	{}
	public void onTerminate(SSACFG.BasicBlock block)
	{}
	public ProgressMonitor getProgressMonitor()
	{
		return mMonitor;
	}
	public void addAlias(int valNum)
	{
		mValNums.add(valNum);
	}
	public void setTerminate(boolean terminate)
	{
		mTerminate = terminate;
	}
	public void setCutPath(boolean cut)
	{
		mCutPath = cut;
	}
	public int getFirstUseIndex()
	{
		return mFirstUseIdx;
	}
	public int getInstructionIndex()
	{
		return mInstIdx;
	}
	public CallRecords getCallRecords()
	{
		return mCallRecords;
	}
	private void dfs(CGNode node, SSACFG.BasicBlock block, int startInstIdx)
		throws CancelRuntimeException
	{
		if(mVisitedBlocks.contains(block))
		{
			onRevisitBasicBlock(block, startInstIdx);
			return;
		}
		mVisitedBlocks.add(block);
		Iterator<SSAPhiInstruction> phiItr = block.iteratePhis();
		while(phiItr.hasNext())
		{
			SSAPhiInstruction phiInst = phiItr.next();
			int nUse = phiInst.getNumberOfUses();
			boolean isUsed = false;
			for(int i = 0; i < nUse; ++i)
			{
				int use = phiInst.getUse(i);
				// It is possible that use == -1
				// See the API doc of SSAPhiInstruction
				if(use >= 0 && mValNums.contains(use))
				{
					isUsed = true;
					break;
				}
			}
			if(isUsed)
				mValNums.add(phiInst.getDef());
		}
		SSAInstruction[] insts = node.getIR().getInstructions();
		int lastInstIdx = block.getLastInstructionIndex();
		try
		{
			onStartBasicBlock(block, startInstIdx);
			
			// For each instruction in the basic block starting from the offset
			for(mInstIdx = startInstIdx; mInstIdx <= lastInstIdx; ++mInstIdx)
			{
				
				// If we have reached the point where the point where the content of the StringBuilder 
				// is converted to String 
				if(mEndCriterion.shouldEnd(node, mInstIdx))
				{
					onEndCondition(block);
					return;
				}
	
				SSAInstruction inst = insts[mInstIdx];
				if(inst == null)
					continue;
				
				// Check whether the value number is used in this instruction
				int nUses = inst.getNumberOfUses();
				for(mFirstUseIdx = 0; mFirstUseIdx < nUses; ++mFirstUseIdx)
				{
					int use = inst.getUse(mFirstUseIdx);
					if(mValNums.contains(use))
						break;
				}
				
				// If not used
				if(mFirstUseIdx >= nUses)
					continue;
				mCutPath = false;
				inst.visit(this);
				if(mTerminate)
				{
					onTerminate(block);
					return;
				}
				if(mCutPath)
				{
					onCutPath(block);
					return;
				}
			}
			
			SSACFG cfg = node.getIR().getControlFlowGraph();
						
			// For each possible next block
			// TODO We ignore exceptional edges here.
			// Considering the exceptional edges would produce many more possible values 
			// which are not useful.
			for(ISSABasicBlock succ : cfg.getNormalSuccessors(block))
			{
				dfs(node, (SSACFG.BasicBlock)succ, succ.getFirstInstructionIndex());
				if(mTerminate)
					break;
			}
		}
		finally
		{
			mVisitedBlocks.remove(block);
			onEndBasicBlock(block);
		}
	}
	public void run(ProgressMonitor monitor)
		throws CancelException
	{
		run(new CallRecords(mCg), monitor);
	}
	public void run(CallRecords callRecords, ProgressMonitor monitor)
		throws CancelException
	{
		if(monitor == null)
			throw new IllegalArgumentException();
		mCallRecords = callRecords;
		mMonitor = monitor;
		mTerminate = false;
		try
		{
			IR ir = mStartNode.getIR();
			SSACFG cfg = ir.getControlFlowGraph();
			SSACFG.BasicBlock startBlock = cfg.getBlockForInstruction(mStartInstIdx);
			onStart();
			onStartNode(mStartNode);
			try
			{
				dfs(mStartNode, startBlock, mStartInstIdx);
			}
			finally
			{
				onEndNode(mStartNode);
				onEnd();
			}
		}
		catch(CancelRuntimeException ex)
		{
			throw CancelException.make(ex.getMessage());
		}
		finally
		{
			monitor.done();
			mCallRecords = null;
			mMonitor = null;
		}
	}
}
