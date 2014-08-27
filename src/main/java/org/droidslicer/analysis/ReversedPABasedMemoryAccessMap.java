package org.droidslicer.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.util.MemoryAccess;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.CISlicer;
import com.ibm.wala.util.collections.MapUtil;

/**
 * Modified from {@link com.ibm.wala.demandpa.util.PABasedMemoryAccessMap} to remove the necessity to remove the 
 * modification and reference map on construction so that caller may improve efficiency.
 * @author someone
 *
 */
public class ReversedPABasedMemoryAccessMap implements MemoryAccessMap
{
	private final PointerAnalysis mPa;

	private final HeapModel mHeapModel;

	private final Map<PointerKey, Set<Statement>> mInvMod;

	private final Map<PointerKey, Set<Statement>> mInvRef;

	public ReversedPABasedMemoryAccessMap(CallGraph cg, PointerAnalysis pa) {
		this(cg, pa, new SDG(cg, pa, DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, ControlDependenceOptions.NONE));
	}

	public ReversedPABasedMemoryAccessMap(CallGraph cg, PointerAnalysis pa, SDG sdg) {
		this(cg, pa, 
				MapUtil.inverseMap(CISlicer.scanForMod(sdg, pa, true, ModRef.make())), 
				MapUtil.inverseMap(CISlicer.scanForRef(sdg, pa)));
	}
	
	public ReversedPABasedMemoryAccessMap(CallGraph cg, PointerAnalysis pa, Map<PointerKey, Set<Statement>> invMod,
			Map<PointerKey, Set<Statement>> invRef)
	{
		if (pa == null)
			throw new IllegalArgumentException("null pa");
		mPa = pa;
		mHeapModel = pa.getHeapModel();
		mInvMod = invMod;
		mInvRef = invRef;
	}

	public Collection<MemoryAccess> getArrayReads(PointerKey arrayRef) {
		Collection<MemoryAccess> memAccesses = new ArrayList<MemoryAccess>();
		for (InstanceKey ik : mPa.getPointsToSet(arrayRef)) {
			PointerKey ack = mHeapModel.getPointerKeyForArrayContents(ik);
			convertStmtsToMemoryAccess(mInvRef.get(ack), memAccesses);
		}
		return memAccesses;
	}

	public Collection<MemoryAccess> getArrayWrites(PointerKey arrayRef) {
		Collection<MemoryAccess> memAccesses = new ArrayList<MemoryAccess>();
		for (InstanceKey ik : mPa.getPointsToSet(arrayRef)) {
			PointerKey ack = mHeapModel.getPointerKeyForArrayContents(ik);
			convertStmtsToMemoryAccess(mInvMod.get(ack), memAccesses);
		}
		return memAccesses;
	}

	public Collection<MemoryAccess> getFieldReads(PointerKey baseRef, IField field) {
		Collection<MemoryAccess> memAccesses = new ArrayList<MemoryAccess>();
		for (InstanceKey ik : mPa.getPointsToSet(baseRef)) {
			PointerKey ifk = mHeapModel.getPointerKeyForInstanceField(ik, field);
			convertStmtsToMemoryAccess(mInvRef.get(ifk), memAccesses);
		}
		return memAccesses;
	}

	public Collection<MemoryAccess> getFieldWrites(PointerKey baseRef, IField field) {
		Collection<MemoryAccess> memAccesses = new ArrayList<MemoryAccess>();
		for (InstanceKey ik : mPa.getPointsToSet(baseRef)) {
			PointerKey ifk = mHeapModel.getPointerKeyForInstanceField(ik, field);
			convertStmtsToMemoryAccess(mInvMod.get(ifk), memAccesses);
		}
		return memAccesses;
	}

	public Collection<MemoryAccess> getStaticFieldReads(IField field) {
		Collection<MemoryAccess> result = new ArrayList<MemoryAccess>();
		convertStmtsToMemoryAccess(mInvRef.get(mHeapModel.getPointerKeyForStaticField(field)), result);
		return result;
	}

	public Collection<MemoryAccess> getStaticFieldWrites(IField field) {
		Collection<MemoryAccess> result = new ArrayList<MemoryAccess>();
		convertStmtsToMemoryAccess(mInvMod.get(mHeapModel.getPointerKeyForStaticField(field)), result);
		return result;
	}

	private void convertStmtsToMemoryAccess(Collection<Statement> stmts, Collection<MemoryAccess> result) {
		if (stmts == null) {
			return;
		}
		for (Statement s : stmts) {
			switch (s.getKind()) {
			case NORMAL:
				NormalStatement normStmt = (NormalStatement) s;
				result.add(new MemoryAccess(normStmt.getInstructionIndex(), normStmt.getNode()));
				break;
			default:
				throw new RuntimeException("Unreachable");
			}
		}
	}

	public HeapModel getHeapModel() {
		return mHeapModel;
	}

}