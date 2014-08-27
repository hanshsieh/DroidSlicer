package org.droidslicer.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.droidslicer.graph.BehaviorGraph;
import org.droidslicer.graph.entity.ComponentReachRelation;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.EntityVisitor;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.IInstructionUnit;
import org.droidslicer.graph.entity.IntentFilterUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.value.ConcreteValue;
import org.droidslicer.value.OrValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.ipa.callgraph.CGNode;

public class BehaviorGraphFocusWriter
{
	private final static Logger mLogger = LoggerFactory.getLogger(BehaviorGraphFocusWriter.class);
	private class NodeVisitor extends EntityVisitor
	{
		private final Writer mWriter;
		private IOException mEx = null;
		public boolean visit(UnitEntity unit)
			throws IOException
		{
			mEx = null;
			try
			{
				return unit.visit(this);
			}
			finally
			{
				if(mEx != null)
					throw mEx;
			}
		}
		public NodeVisitor(Writer writer)
		{
			mWriter = writer;
		}
		protected boolean isSysUnit(UnitEntity unit)
		{
			for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
			{
				if(!(relation instanceof ComponentReachRelation))
					continue;
				UnitEntity src = mGraph.getEdgeSource(relation);
				if(!(src instanceof ComponentUnit))
					continue;
				ComponentUnit comp = (ComponentUnit)src;
				if(comp.isSystemComponent())
				{
					return true;
				}
			}
			return false;
		}
		protected boolean isFocusSysSUseUnit(SUseUnit unit)
		{
			Set<UnitEntity> visit = new HashSet<UnitEntity>();
			Queue<UnitEntity> que = new ArrayDeque<UnitEntity>();
			que.add(unit);
			visit.add(unit);
			while(!que.isEmpty())
			{
				UnitEntity now = que.poll();
				if(!isSysUnit(now))
				{
					return true;
				}
				for(RelationEntity relation : mGraph.outgoingEdgesOf(now))
				{
					UnitEntity dst = mGraph.getEdgeTarget(relation);
					if(!visit.contains(dst))
					{
						visit.add(dst);
						que.add(dst);
					}
				}
				for(RelationEntity relation : mGraph.incomingEdgesOf(now))
				{
					if(relation instanceof ComponentReachRelation)
						continue;
					UnitEntity src = mGraph.getEdgeSource(relation);
					if(!visit.contains(src))
					{
						visit.add(src);
						que.add(src);
					}
				}
			}
			return false;
		}
		protected void printReachingComponents(UnitEntity unit)
			throws IOException
		{
			mWriter.write("Reaching components: \n");
			for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
			{
				if(!(relation instanceof ComponentReachRelation))
					continue;
				UnitEntity src = mGraph.getEdgeSource(relation);
				if(!(src instanceof ComponentUnit))
					continue;
				ComponentUnit comp = (ComponentUnit)src;
				mWriter.write('\t');
				mWriter.write(comp.toString());
				mWriter.write('\n');
			}
		}
		protected void printInstructionUnit(IInstructionUnit instUnit)
			throws IOException
		{
			CGNode node = instUnit.getNode();
			mWriter.write("Node: ");
			mWriter.write(node.toString());
			mWriter.write('\n');
		}
		@Override
		public boolean visitICCParamCallerUnit(ICCParamCallerUnit unit)
		{
			if(isSysUnit(unit))
				return false;
			try
			{
				mWriter.write("ICC parameter caller\n");
				mWriter.write(unit.toString());
				mWriter.write('\n');
				printReachingComponents(unit);
				if(unit instanceof IInstructionUnit)
				{
					IInstructionUnit instUnit = (IInstructionUnit)unit;
					printInstructionUnit(instUnit);
				}
			}
			catch(IOException ex)
			{
				mEx = ex;
			}
			return true;
		}
		@Override
		public boolean visitComponentUnit(ComponentUnit unit)
		{
			if(unit.isSystemComponent())
				return false;
			try
			{
				mWriter.write("Component unit\n");
				mWriter.write(unit.toString());
				mWriter.write('\n');
				if(unit instanceof IntentFilterUnit)
				{
					IntentFilterUnit filterUnit = (IntentFilterUnit)unit;
					OrValue intentFilters = filterUnit.getIntentFilterValues();
					mWriter.write("Intent filters:\n");
					for(Iterator<ConcreteValue> itr = intentFilters.iterator(); itr.hasNext(); )
					{
						mWriter.write('\t');
						mWriter.write(itr.next().toString());
						mWriter.write('\n');
					}
				}
				if(unit instanceof ProviderUnit)
				{
					ProviderUnit provider = (ProviderUnit)unit;
					ConcreteValue authVal = provider.getAuthorityValue();
					ConcreteValue pathVal = provider.getPathValue();
					mWriter.write("Authority: ");
					mWriter.write(authVal.toString());
					mWriter.write("\nPath: ");
					mWriter.write(pathVal.toString());
					mWriter.write('\n');
				}
			}
			catch(IOException ex)
			{
				mEx = ex;
			}
			return true;
		}
		@Override
		public boolean visitSUseUnit(SUseUnit unit)
		{
			try
			{
				boolean reachableFromSys = false;
				for(RelationEntity relation : mGraph.incomingEdgesOf(unit))
				{
					if(!(relation instanceof ComponentReachRelation))
						continue;
					UnitEntity src = mGraph.getEdgeSource(relation);
					if(!(src instanceof ComponentUnit))
						continue;
					ComponentUnit comp = (ComponentUnit)src;
					if(comp.isSystemComponent())
					{
						reachableFromSys = true;
					}
				}
				if(reachableFromSys)
				{
					if(!isFocusSysSUseUnit(unit))
						return false;
					mWriter.write("SUse unit in system component\n");
				}
				else
					mWriter.write("SUse unit in non-system component\n");
				mWriter.write(unit.toString());
				mWriter.write('\n');
				mWriter.write("Permissions: \n");
				for(String perm : unit.getPermissions())
				{
					mWriter.write('\t');
					mWriter.write(perm);
					mWriter.write('\n');
				}
				if(unit instanceof IInstructionUnit)
				{
					IInstructionUnit instUnit = (IInstructionUnit)unit;
					printInstructionUnit(instUnit);
				}
				printReachingComponents(unit);
			}
			catch(IOException ex)
			{
				mEx = ex;
			}
			return true;
		}
	}
	private final BehaviorGraph mGraph;
	public BehaviorGraphFocusWriter(BehaviorGraph graph)
	{
		if(graph == null)
			throw new IllegalArgumentException();
		mGraph = graph;
	}
	public void write(OutputStream output)
		throws IOException
	{
		mLogger.debug("Exporting focused points of beahvior graph");
		Writer writer = null;
		try
		{
			writer = new BufferedWriter(new OutputStreamWriter(output));
			NodeVisitor visitor = new NodeVisitor(writer);
			for(UnitEntity unit : mGraph.vertexSet())
			{
				if(visitor.visit(unit))
				{
					writer.write("=================================\n");
				}
			}
		}
		finally
		{
			if(writer != null)
			{
				try
				{
					writer.close();
				}
				catch(Exception ex)
				{}
			}
			mLogger.debug("Exporting finished");
		}
	}
}
