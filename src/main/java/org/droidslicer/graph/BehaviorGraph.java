package org.droidslicer.graph;

import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.UnitEntity;
import org.jgrapht.graph.DefaultDirectedGraph;

public class BehaviorGraph extends DefaultDirectedGraph<UnitEntity, RelationEntity>
{
	private static final long serialVersionUID = 970942937858631285L;
	public BehaviorGraph()
	{
		super(RelationEntity.class);
	}
}
