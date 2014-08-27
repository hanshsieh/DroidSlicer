package org.droidslicer;

import java.awt.Component;

import javax.swing.JPanel;

import org.droidslicer.graph.BehaviorGraphComponent;

public class GraphPane extends JPanel
{
	private static final long serialVersionUID = 5759596827656136783L;
	public GraphPane()
	{
		
	}
	public void setGraphComponent(BehaviorGraphComponent graphComp)
	{
		if(graphComp == null)
			removeAll();
		else
			add(graphComp);
	}
	public BehaviorGraphComponent getGraphComponent()
	{
		int nComp = getComponentCount();
		if(nComp == 0)
			return null;
		else
			return (BehaviorGraphComponent)getComponent(0);
	}
	@Override
	protected void addImpl(Component comp, Object constraints, int index)
	{
		if(!(comp instanceof BehaviorGraphComponent))
		{
			throw new IllegalArgumentException("Cannot add component other than " + BehaviorGraphComponent.class.getName());
		}
		removeAll();
		super.addImpl(comp, constraints, index);
	}
}
