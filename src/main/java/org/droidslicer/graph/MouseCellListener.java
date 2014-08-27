package org.droidslicer.graph;

import java.awt.event.MouseEvent;

public interface MouseCellListener
{
	public void mouseOver(MouseEvent event, Object cell);
	public void mouseExit(MouseEvent event, Object cell);
}
