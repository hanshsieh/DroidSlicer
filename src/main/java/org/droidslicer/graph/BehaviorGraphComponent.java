package org.droidslicer.graph;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.handler.mxPanningHandler;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;

public class BehaviorGraphComponent extends mxGraphComponent
{
	private static final long serialVersionUID = 1146583818860715411L;
	private final static Logger mLogger = LoggerFactory.getLogger(BehaviorGraphComponent.class);
	public BehaviorGraphComponent(VisualBehaviorGraph graph)
	{
		super(graph);
		
		setPanning(true);
		setDragEnabled(false);
		// Disallow creating edge by dragging
		setConnectable(false);
		
		mxGraphHandler grapHandler = getGraphHandler();
			
		// Don't highlight a group when dragging a cell over it
		grapHandler.setMarkerEnabled(false);
		
		// Disallow removing a cell from its original parent by dragging
		grapHandler.setRemoveCellsFromParent(false);
		
		// Reference: com/mxgraph/examples/swing/editor/BasicGraphEditor.java
		addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent event)
			{
				if (event.getWheelRotation() < 0)
					zoomIn();
				else
					zoomOut();
			}
		});
		/*mxGraphControl graphControl = getGraphControl();
		
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				mLogger.debug("Mouse pressed: {}", e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				mLogger.debug("Mouse dragged: {}", e);
			}
		});*/
	}
	/*protected Point getPointOnGraph(Point p)
	{
	    mxGraph graph = getGraph();

	    double s = graph.getView().getScale();
	    Point tr = graph.getView().getTranslate().getPoint();

	    double off = graph.getGridSize() / 2;
	    double x = graph.snap(p.getX() / s - tr.getX() - off);
	    double y = graph.snap(p.getY() / s - tr.getY() - off);

	    return new Point((int)x, (int)y);
	}*/
	
	/**
	 * When the event is a panning event.
	 * Panning event is an event that should allow the user to use the mouse to drag
	 * the whole graph to skim over.
	 * We overwrite the original method to make it allow users to drag the graph whenever
	 * the mouse isn't pressed on a cell when the dragging event starts.
	 * This method will be invoked by {@link mxPanningHandler} registered in {@link mxGraphComponent}.
	 * We referenced {@link mxGraphHandler#mousePressed(MouseEvent) when writing this method.
	 */
	@Override
	public boolean isPanningEvent(MouseEvent event)
	{
		mxGraphHandler graphHandler = getGraphHandler();
		if (isEnabled() && graphHandler.isEnabled() && !event.isConsumed()
				&& !isForceMarqueeEvent(event))
		{
			// If the mouse is over a cell, then we don't want it to move the canvas
			Object cell = getCellAt(event.getX(), event.getY(), false);
			return cell == null;
		}
		else
			return false;
	}
	@Override
	protected mxPoint getPageTranslate(double scale)
	{
		mxPoint point = super.getPageTranslate(scale);
		return point;
	}
	/**
	 * Selects the cell for the given event, and bring the selected cell and 
	 * all its ancestors to the front.
	 */
	@Override
	public void selectCellForEvent(Object cell, MouseEvent e)
	{
		super.selectCellForEvent(cell, e);
		// Bring the cell and all its parent to the front
		mxIGraphModel model = graph.getModel();
		ArrayList<Object> targets = new ArrayList<Object>(2);
		do
		{
			targets.add(cell);
			cell = model.getParent(cell);
		}while(cell != null);
		graph.orderCells(false, targets.toArray());
	}
	@Override
	protected mxGraphHandler createGraphHandler()
	{
		return new BehaviorGraphHandler(this);
	}
	private void doLayout(Object cell, mxGraphLayout layout, boolean animate)
	{
        graph.getModel().beginUpdate();
        try
        {
            layout.execute(cell);
        }
        finally
        {
        	if(animate)
        	{
	            mxMorphing morph = new mxMorphing(this, 20, 1.2, 20);
	            morph.addListener(mxEvent.DONE, new mxIEventListener()
	            {
	
	                @Override
	                public void invoke(Object arg0, mxEventObject arg1)
	                {
	                    graph.getModel().endUpdate();
	                }
	
	            });
	            morph.startAnimation();
        	}
        	else
        		graph.getModel().endUpdate();
        }
	}
	public void doLayout(boolean animate)
	{
		mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.NORTH);
		layout.setDisableEdgeStyle(false);
		doLayout(graph.getDefaultParent(), layout, animate);
		/*mxStackLayout layout = new mxStackLayout(graph, true, 20, 20, 20, 10);
		mxHierarchicalLayout compLayout = new mxHierarchicalLayout(graph, SwingConstants.NORTH);
		compLayout.setDisableEdgeStyle(false);
		/*Iterator<Map.Entry<TypeReference, Object>> itr = mBehaviorGraph.appCompGroupsIterator();
        while(itr.hasNext())
        {
        	Object group = itr.next().getValue();
        	doLayout(group, compLayout, animate);
        	graph.updateCellSize(group);
        }
		doLayout(graph.getDefaultParent(), layout, animate);*/
	}
	/*
	public void showRelatedEntitiesForCell(Object cell)
	{
		mLogger.debug("show related {}", cell);
	}*/
}
