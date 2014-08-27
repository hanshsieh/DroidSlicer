package org.droidslicer.util;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo.PointsToResult;
import com.ibm.wala.demandpa.alg.InstanceKeyAndState;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;

public class DemandInstanceUseVisitor extends SSAInstruction.Visitor
{
	public static enum Usage
	{
		ANY, ONE_USE, INVOCATION_RECEIVER
	}
	private InstanceKeyAndState mInstance;
	private LocalPointerKey mLocPointer;
	private CGNode mNode;
	private int mValNum;
	private Usage mUsage;
	private int mInstIdx = -1;
	private DemandRefinementPointsTo mPointsTo;
	private boolean mShouldStop;
	private final static Logger mLogger = LoggerFactory.getLogger(DemandInstanceUseVisitor.class);
	public DemandInstanceUseVisitor(InstanceKeyAndState instance, Usage usage)
	{
		mInstance = instance;
		mUsage = usage;
	}
	public void run(DemandRefinementPointsTo pointsTo)
	{
		mShouldStop = false;
		mPointsTo = pointsTo;
		Pair<PointsToResult, Collection<PointerKey>> pair;
		if(!Utils.isDemandPointsToSupported(mInstance.getInstanceKey()))
		{
			// Currently WALA only support InstanceKeyWithNode
			pair = null;
		}
		else
			pair = mPointsTo.getFlowsTo(mInstance);
		if(pair == null || pair.snd == null)
		{
			mLogger.warn("Skip unsupported instance in on-demand points-to: {}", mInstance);
			return;
		}
		for(PointerKey usePointer : pair.snd)
		{
			if(mShouldStop)
				break;
			if(!(usePointer instanceof LocalPointerKey))
				continue;
			mLocPointer = (LocalPointerKey)usePointer;
			mNode = mLocPointer.getNode();
			mValNum = mLocPointer.getValueNumber();
			SSAInstruction[] insts = mNode.getIR().getInstructions();
			for(mInstIdx = 0; mInstIdx < insts.length; ++mInstIdx)
			{
				SSAInstruction inst = insts[mInstIdx];
				if(inst == null)
					continue;
				boolean shouldVisit = false;
				switch(mUsage)
				{
				case ANY:
					shouldVisit = true;
					break;
				case ONE_USE:
					{
						int nUses = inst.getNumberOfUses();
						int useIdx;
						for(useIdx = 0; useIdx < nUses; ++useIdx)
						{
							if(inst.getUse(useIdx) == mValNum)
								break;
						}
						shouldVisit = (useIdx < nUses);
						break;
					}
				case INVOCATION_RECEIVER:
					{
						if(!(inst instanceof SSAAbstractInvokeInstruction))
							break;
						SSAAbstractInvokeInstruction invokeInst = (SSAAbstractInvokeInstruction)inst;
						if(invokeInst.isStatic() || invokeInst.getNumberOfParameters() <= 0)
							break;
						int receiver = invokeInst.getReceiver();
						shouldVisit = (receiver == mValNum);
						break;
					}
				default:
					break;
				}
				if(shouldVisit)
				{
					inst.visit(this);
					if(mShouldStop)
						break;
				}
			}
		}
	}
	public void setShouldStop(boolean shouldStop)
	{
		mShouldStop = shouldStop;
	}
	public DemandRefinementPointsTo getPointsTo()
	{
		return mPointsTo;
	}
	public CGNode getNode()
	{
		return mNode;
	}
	public int getValueNumber()
	{
		return mValNum;
	}
	public int getInstructionIndex()
	{
		return mInstIdx;
	}
	public LocalPointerKey getLocalPointerKey()
	{
		return mLocPointer;
	}
}
