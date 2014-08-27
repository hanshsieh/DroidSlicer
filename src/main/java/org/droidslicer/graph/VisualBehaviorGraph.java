package org.droidslicer.graph;

import java.util.Map;

import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.ApplicationUnit;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.DataDependencyRelation;
import org.droidslicer.graph.entity.Entity;
import org.droidslicer.graph.entity.FileUnit;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.ICCUnit;
import org.droidslicer.graph.entity.IntentCommUnit;
import org.droidslicer.graph.entity.InvocationUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SQLiteDbUnit;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.SharedPreferencesUnit;
import org.droidslicer.graph.entity.SocketUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.graph.entity.UriCommUnit;
import org.droidslicer.graph.entity.UrlConnectionUnit;
import org.droidslicer.graph.entity.VirtualPermissionUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

public class VisualBehaviorGraph extends mxGraph
{
	private static final Logger mLogger = LoggerFactory.getLogger(VisualBehaviorGraph.class);
	// Cell --> Entity or edge
	// JGraphx requires that the cell and the value attached to a cell must be serializable.
	// Thus, we decided to maintain the mapping between cell and our business logic by ourself.
	protected final BiMap<Object, Entity> mCell2Val = HashBiMap.create();
	protected final BehaviorGraph mGraph;
	public VisualBehaviorGraph(BehaviorGraph graph)
	{
		super(new mxGraphModel());
		mGraph = graph;
		setHtmlLabels(false);
		setAllowDanglingEdges(false);
		setCellsDeletable(false);
		setCellsEditable(false);
		setCellsDisconnectable(false);
		setEdgeLabelsMovable(false);
		setAutoSizeCells(true);
		
		// Doesn't allow the coordinates to be negative
		setAllowNegativeCoordinates(false);
		
		// graph.setConnectableEdges(false); // doesn't work
		mxStylesheet styleSheet = getStylesheet();
		
		// Set the style of edges
		Map<String, Object> edgeStyle = styleSheet.getDefaultEdgeStyle();		
		//edgeStyle.put(mxConstants.STYLE_EDGE, mxEdgeStyle.SideToSide);
		//edgeStyle.put(mxConstants.SHAPE_CONNECTOR, "vertical");
		//edgeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ELBOW);
		//edgeStyle.put(mxConstants.STYLE_ELBOW, mxConstants.ELBOW_VERTICAL);
		//edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ENTITY_RELATION);
		//edgeStyle.put(mxConstants.STYLE_EDGE, mxConstants.EDGESTYLE_ORTHOGONAL);
		//edgeStyle.put(mxConstants.STYLE_ORTHOGONAL, "1");
		edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "green");
		rebuildGraph();
	}
	public Entity getEntityForCell(Object cell)
	{
		return mCell2Val.get(cell);
	}
	public Object getCellForEntity(Entity entity)
	{
		return mCell2Val.inverse().get(entity);
	}
	protected String getDataEntityStyle(UnitEntity entity)
	{
		StringBuilder builder = new StringBuilder(mxConstants.STYLE_FILLCOLOR);
		builder.append('=');
		if(entity instanceof ComponentUnit)
		{
			builder.append("#58FA58");
		}
		else if(entity instanceof ICCParamCallerUnit || entity instanceof ICCReturnCallerUnit)
		{
			builder.append("#FAAC58");
		}
		else if(entity instanceof ICCParamCalleeUnit || entity instanceof ICCReturnCalleeUnit)
		{
			builder.append("#A9F5A9");
		}
		else if(entity instanceof FileUnit)
		{
			builder.append("#58ACFA");
		}
		else if(entity instanceof InvocationUnit)
		{
			builder.append("#F5A9F2");
		}
		else if(entity instanceof SharedPreferencesUnit)
		{
			builder.append("#F5A9BC");
		}
		else if(entity instanceof SocketUnit)
		{
			builder.append("#D8D8D8");
		}
		else if(entity instanceof SQLiteDbUnit)
		{
			builder.append("#A9D0F5");
		}
		else if(entity instanceof UrlConnectionUnit)
		{
			builder.append("#F5BCA9");
		}
		else if(entity instanceof VirtualPermissionUnit)
		{
			builder.append("#9F81F7");
		}
		else
			throw new RuntimeException("Unknown entity type: " + entity.getClass().getSimpleName());
		return builder.toString();
	}
	public void rebuildGraph()
	{
		for(Object cell : mCell2Val.keySet())
			getModel().remove(cell);
		mCell2Val.clear();
		
		// Add vertex to the visualized graph for each entity
		for(UnitEntity unit : mGraph.vertexSet())
		{
			// Add the unit to the graph
			if(!(unit instanceof ComponentUnit))
				addUnitVertex(unit);
		}
		
		// Add edges to the visualized graph for each relation
		for(RelationEntity relation : mGraph.edgeSet())
		{
			if(relation instanceof DataDependencyRelation)
			{
				UnitEntity srcEntity = mGraph.getEdgeSource(relation);
				UnitEntity targetEntity = mGraph.getEdgeTarget(relation);
				Object srcCell = mCell2Val.inverse().get(srcEntity);
				Object targetCell = mCell2Val.inverse().get(targetEntity);
				if(srcCell != null && targetCell != null)
					addEntityEdge(srcCell, targetCell, relation);
			}
		}
	}
	protected boolean hasRelationType(Object srcCell, Object destCell, Class<?> type, boolean directed)
	{
		Object[] edges = getEdgesBetween(srcCell, destCell, directed);
		for(Object edgeCell : edges)
		{
			Entity edge = mCell2Val.get(edgeCell);
			if(type.isAssignableFrom(edge.getClass()))
				return true;
		}
		return false;
	}
	protected void addEntityEdge(Object src, Object dest, RelationEntity edge)
	{
		Object edgeCell = insertEdge(null, null, null, src, dest);
		mCell2Val.put(edgeCell, edge);
	}
	protected void addUnitVertex(UnitEntity entity)
	{
		Object cell = insertVertex(getDefaultParent(), null, null, 0, 0, 100, 100, getDataEntityStyle(entity), false);
		mCell2Val.put(cell, entity);
		updateCellSize(cell);
	}
	/**
	 * When a cell is moved, enlarge its container cell to ensure that it is 
	 * still enclosed by its container cell.
	 */
	/*
	@Override
	public Object[] moveCells(Object[] cells, double dx, double dy,
			boolean clone, Object target, Point location)
	{
		if(target == null)
		{
			mxIGraphModel model = getModel();
			for(Object child : cells)
			{
				Object parent = model.getParent(child);
				mxGeometry parentGeo = model.getGeometry(parent);
				mxGeometry childGeo = model.getGeometry(child);
				if(parentGeo != null && childGeo != null)
				{
					parentGeo = (mxGeometry)parentGeo.clone();
					double newX = parentGeo.getX() + childGeo.getX() + dx, newY = parentGeo.getY() + childGeo.getY() + dy;
					double left = Math.min(parentGeo.getX(), newX);
					double top = Math.min(parentGeo.getY(), newY);
					double right = Math.max(parentGeo.getX() + parentGeo.getWidth(), newX + childGeo.getWidth());
					double bottom = Math.max(parentGeo.getY() + parentGeo.getHeight(), newY + childGeo.getHeight());
					parentGeo.setRect(left, top, right - left, bottom - top);
					model.setGeometry(parent, parentGeo);
				}
			}
		}
		return super.moveCells(cells, dx, dy, clone, target, location);
	}*/
	protected String getDataEntityLabel(UnitEntity entity)
	{
		if(entity instanceof ComponentUnit)
		{
			ComponentUnit entryEntity = (ComponentUnit)entity;
			String text;
			if(entity instanceof ActivityUnit)
				text = "Activity: ";
			else if(entity instanceof ProviderUnit)
				text = "Provider: ";
			else if(entity instanceof ReceiverUnit)
				text = "Receiver: ";
			else if(entity instanceof ServiceUnit)
				text = "Service: ";
			else if(entity instanceof ApplicationUnit)
				text = "Application: ";
			else
				 throw new RuntimeException();
			return text + entryEntity.getType().getName().getClassName().toString();
		}
		else if(entity instanceof ICCUnit)
		{
			if(entity instanceof ICCParamCalleeUnit)
			{
				ICCParamCalleeUnit paramEntity = (ICCParamCalleeUnit)entity;
				return "ICC param callee: " + paramEntity.getMethod().getName().toString();
			}
			else if(entity instanceof ICCReturnCalleeUnit)
			{
				ICCReturnCalleeUnit retEntity = (ICCReturnCalleeUnit)entity;
				return "ICC return callee: " + retEntity.getMethod().getName().toString();
			}
			else if(entity instanceof ICCParamCallerUnit)
			{
				return "ICC param caller";
	 		}
			else if(entity instanceof ICCReturnCallerUnit)
			{
				return "ICC ret caller";
			}
		}
		else if(entity instanceof SUseUnit)
		{
			if(entity instanceof FileUnit)
			{
				return "File";
			}
			else if(entity instanceof InvocationUnit)
			{
				InvocationUnit invokeEntity = (InvocationUnit)entity;
				SSAAbstractInvokeInstruction inst = invokeEntity.getInvokeInstruction();
				return "Invoke: " + inst.getDeclaredTarget().getName().toString();
			}
			else if(entity instanceof SocketUnit)
			{
				return "Socket";
			}
			else if(entity instanceof SQLiteDbUnit)
			{
				return "SQLite database";
			}
			else if(entity instanceof SharedPreferencesUnit)
			{
				return "SharedPreferences";
			}
			else if(entity instanceof UrlConnectionUnit)
			{
				return "URL connection";
			}
			else if(entity instanceof UriCommUnit)
			{
				return "Provider ICC";
			}
			else if(entity instanceof IntentCommUnit)
			{
				return "Intent ICC";
			}
			else if(entity instanceof VirtualPermissionUnit)
			{
				return "Virtual permission-use";
			}
		}
		throw new RuntimeException("Unreachable");
	}
	@Override
	public String convertValueToString(Object cell)
	{
		Entity entity = mCell2Val.get(cell);
		if(entity instanceof UnitEntity)
		{
			return getDataEntityLabel((UnitEntity)entity);
		}
		return "";
	}
	/*public boolean addEntity(AppComponentEntity entity)
	{
		if(mCell2Val.containsValue(entity))
			return false;
		addEntityNoCheck(entity);
		return true;
	}
	private Object addEntityNoCheck(AppComponentEntity entity)
	{
		TypeReference compType = entity.getComponentType();
		if(compType == null)
			throw new IllegalArgumentException("Component type cannot be null");
		Object group = mCompGroups.get(compType);
		Object cell;
		if(group == null)
		{
			cell = mGraph.insertVertex(mGraph.getDefaultParent(), null, null, 0, 0, 100, 100, null, false);
			group = mGraph.groupCells(null, 5.0, new Object[]{cell});
			mCompGroups.put(compType, group);
		}
		else
		{
			cell = mGraph.insertVertex(group, null, null, 0, 0, 0, 0, null, false);
		}
		mCell2Val.put(cell, entity);
		mGraph.updateCellSize(cell);
		return cell;
	}
	public boolean addDependency(AppComponentEntity entity1, AppComponentEntity entity2)
	{
		Object cell1 = (mxICell)mCell2Val.inverse().get(entity1);
		Object cell2 = (mxICell)mCell2Val.inverse().get(entity2);
		if(cell1 == null)
			cell1 = addEntityNoCheck(entity1);
		if(cell2 == null)
			cell2 = addEntityNoCheck(entity2);
		mxIGraphModel model = mGraph.getModel();
		Object parent1 = model.getParent(cell1);
		Object parent2 = model.getParent(cell2);
		Object[] edges = mGraph.getEdgesBetween(cell1, cell2, true);
		for(Object edgeCell : edges)
		{
			Object relation = mCell2Val.get(edgeCell);
			assert relation instanceof EntityRelation;
			if(relation instanceof EntityDependencyRelation)
				return false;
		}
		Object edgeCell = mGraph.insertEdge(parent1 == parent2 ? parent1 : null, null, null, cell1, cell2);
		mCell2Val.put(edgeCell, new EntityDependencyRelation());
		return true;
	}*/
}
