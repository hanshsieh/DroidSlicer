package org.droidslicer.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mxgraph.swing.handler.mxGraphHandler;

public class BehaviorGraphHandler extends mxGraphHandler
{
	private final static Logger mLogger = LoggerFactory.getLogger(BehaviorGraphHandler.class);
	//private Object mMouseOverCell = null;
	public BehaviorGraphHandler(BehaviorGraphComponent graphComponent)
	{
		super(graphComponent);
	}
	/*@Override
	protected void moveCells(Object[] cells, double dx, double dy,
			Object target, MouseEvent e)
	{
		// Doesn't allow changing the group of a cell by dragging
		super.moveCells(cells, dx, dy, null, e);
	}*/
	/**
	 * We referenced {@link mxGraphHandler#mouseMoved(MouseEvent)} and {@link mxGraphHandler#getCursor(MouseEvent)}.
	 */
	/*
	@Override
	public void mouseMoved(MouseEvent e)
	{
		if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed())
		{
			Cursor cursor = null;
			Object cell = graphComponent.getCellAt(e.getX(), e.getY(), false);
			BehaviorGraphComponent graphComp = (BehaviorGraphComponent)getGraphComponent();
			if(cell != mMouseOverCell)
				graphComp.showRelatedEntitiesForCell(cell);
			mMouseOverCell = cell;
			if (isMoveEnabled())
			{
				if (cell != null)
				{					
					if (graphComponent.isFoldingEnabled()
							&& graphComponent.hitFoldingIcon(cell, e.getX(),
									e.getY()))
					{
						cursor = FOLD_CURSOR;
					}
					else if (graphComponent.getGraph().isCellMovable(cell))
					{
						cursor = MOVE_CURSOR;
					}
				}
			}
			if (cursor != null)
			{
				graphComponent.getGraphControl().setCursor(cursor);
				e.consume();				
			}
			else
			{
				graphComponent.getGraphControl().setCursor(DEFAULT_CURSOR);
			}
		}
		else
			mMouseOverCell = null;
	}*/
}
