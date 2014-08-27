package org.droidslicer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextKey.ParameterKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class FixedZeroXCFABuilder extends ZeroXCFABuilder
{

	public FixedZeroXCFABuilder(IClassHierarchy cha, AnalysisOptions options,
			AnalysisCache cache, ContextSelector appContextSelector,
			SSAContextInterpreter appContextInterpreter, int instancePolicy)
	{
		super(cha, options, cache, appContextSelector, appContextInterpreter,
				instancePolicy);
	}
	protected IClass getReceiverClass(IMethod method)
	{
		TypeReference formalType = method.getParameterType(0);
		IClass C = getClassHierarchy().lookupClass(formalType);
		if (method.isStatic())
		{
			Assertions.UNREACHABLE("asked for receiver of static method " + method);
		}
		if (C == null)
		{
			Assertions.UNREACHABLE("no class found for " + formalType + " recv of " + method);
		}
		return C;
	}
	@Override
	protected PointerKey getTargetPointerKey(CGNode target, int index) 
	{
		int vn;
		if (target.getIR() != null)
		{
			vn = target.getIR().getSymbolTable().getParameter(index);
		}
		else
		{
			vn = index+1;
		}

		FilteredPointerKey.TypeFilter filter;
		ContextKey ctxKey = index >= ContextKey.PARAMETERS.length ? new ParameterKey(index) : ContextKey.PARAMETERS[index];
		filter = (FilteredPointerKey.TypeFilter) target.getContext().get(ctxKey);
		if (filter != null && !filter.isRootFilter())
		{
			return pointerKeyFactory.getFilteredPointerKeyForLocal(target, vn, filter);

		}
		else if (index == 0 && !target.getMethod().isStatic())
		{
			// the context does not select a particular concrete type for the
			// receiver, so use the type of the method
			IClass C = getReceiverClass(target.getMethod());
			if (C.getClassHierarchy().getRootClass().equals(C))
			{
				return pointerKeyFactory.getPointerKeyForLocal(target, vn);        
			}
			else
			{
				return pointerKeyFactory.getFilteredPointerKeyForLocal(target, vn, new FilteredPointerKey.SingleClassFilter(C));
			}

		}
		else
		{
			return pointerKeyFactory.getPointerKeyForLocal(target, vn);
		}
	}
}
