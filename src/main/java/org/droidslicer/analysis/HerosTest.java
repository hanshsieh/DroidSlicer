package org.droidslicer.analysis;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;

public class HerosTest
{
	public static void test(CallGraph cg)
	{
		InterproceduralCFG cfg = new InterproceduralCFG(cg);
		
	}
}
