package org.droidslicer.graph.entity.resolver;

import java.util.HashSet;
import java.util.Set;

import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.util.ProgressMonitor;
import org.droidslicer.util.SubProgressMonitor;
import org.droidslicer.util.TypeId;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.solver.ConcreteValueSolver;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public class ReceiverRegisterResolver extends InvocationEntityResolver 
{
	private final int mReceiverIdx;
	private final int mFilterIdx;
	public ReceiverRegisterResolver(MethodReference method, boolean isStatic, int receiverParamIdx, int intentFilterParamIdx)
	{
		super(method, isStatic);
		int nParam = getNumberOfParameters();
		TypeReference receiverTypeRef = TypeId.ANDROID_RECEIVER.getTypeReference();
		if(receiverParamIdx < 0 || receiverParamIdx >= nParam || 
			!getParameterType(receiverParamIdx).getName().equals(receiverTypeRef.getName()))
		{
			throw new IllegalArgumentException("Illegal parameter index for BroadcastReceiver");
		}
		
		TypeReference filterTypeRef = TypeId.ANDROID_INTENT_FILTER.getTypeReference();
		if(intentFilterParamIdx < 0 || intentFilterParamIdx >= nParam || 
			!getParameterType(intentFilterParamIdx).getName().equals(filterTypeRef.getName()))
		{
			throw new IllegalArgumentException("Illegal parameter index for IntentFilter");
		}
		mReceiverIdx = receiverParamIdx;
		mFilterIdx = intentFilterParamIdx;
	}

	@Override
	public void resolve(UnitsResolverContext ctx,
			SSAInvokeInstruction invokeInst, int instIdx,
			ProgressMonitor monitor) throws CancelException
	{
		try
		{
			monitor.beginTask("Resolving BroadcastReceiver registration point", 100);
			MethodReference declaredTarget = invokeInst.getDeclaredTarget();
			if(!declaredTarget.getDescriptor().equals(getMethodReference().getDescriptor()))
				return;
			CGNode node = ctx.getCurrentNode();
			
			// Resolve the value of the intent filter
			ConcreteValueSolver valSolver = ctx.getValueSolver();
			ParamCaller filterParamStm = new ParamCaller(node, instIdx, invokeInst.getUse(mFilterIdx));
			ConcreteValue filterVal = 
					valSolver.solve(filterParamStm, node, instIdx, TypeId.ANDROID_INTENT_FILTER.getTypeReference(), getResolveDepth(), new SubProgressMonitor(monitor, 90));
			
			// Collect the concrete types that the receiver can point to
			PointerAnalysis pa = ctx.getAnalysisContext().getPointerAnalysis();
			Set<TypeReference> receiverTypes = new HashSet<TypeReference>();
			LocalPointerKey receiverPtr = new LocalPointerKey(node, invokeInst.getUse(mReceiverIdx));
			for(InstanceKey receiverIns : pa.getPointsToSet(receiverPtr))
			{
				IClass receiverType = receiverIns.getConcreteType();
				receiverTypes.add(receiverType.getReference());
			}
			
			// Find the receiver components with the same concrete type 
			for(ComponentUnit comp : ctx.getComponentUnits())
			{
				TypeReference compTypeRef = comp.getType();
				if(!receiverTypes.contains(compTypeRef) || !(comp instanceof ReceiverUnit))
					continue;
				
				// Attach the intent filter to the receiver component
				ReceiverUnit receiverUnit = (ReceiverUnit)comp;
				receiverUnit.addIntentFilterValue(filterVal);
			}
		}
		finally
		{
			monitor.done();
		}
	}

}
