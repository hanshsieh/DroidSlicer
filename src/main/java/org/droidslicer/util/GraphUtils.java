package org.droidslicer.util;

import heros.InterproceduralCFG;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphUtils
{
	private static final Logger mLogger = LoggerFactory.getLogger(GraphUtils.class);
	public static class LabeledEdge
	{
		private final String mLabel;
		public LabeledEdge(String label)
		{
			mLabel = label;
		}
		public LabeledEdge()
		{
			mLabel = "";
		}
		@Override
		public String toString()
		{
			return mLabel;
		}
	}
	public static <N, M> org.jgrapht.Graph<N, LabeledEdge> convertICFG2JGraphT(InterproceduralCFG<N, M> icfg)
	{
		mLogger.debug("Converting ICFG to JGraphT");
		org.jgrapht.Graph<N, LabeledEdge> result = new DefaultDirectedGraph<N, LabeledEdge>(LabeledEdge.class);
		Set<N> nodes = new LinkedHashSet<N>();
		{
			Set<N> nonCallStartNodes = icfg.allNonCallStartNodes();
			Set<M> methods = new HashSet<M>();
			for(N node : nonCallStartNodes)
			{
				M method = icfg.getMethodOf(node);
				if(methods.add(method))
				{
					for(N entry : icfg.getStartPointsOf(method))
						nodes.add(entry);
					for(N caller : icfg.getCallsFromWithin(method))
						nodes.add(caller);
				}
			}
			nodes.addAll(nonCallStartNodes);
		}
		for(N node : nodes)
			result.addVertex(node);
		Set<N> missedNodes = new LinkedHashSet<N>();
		do
		{
			for(N node : nodes)
			{
				for(N succ : icfg.getSuccsOf(node))
				{
					if(result.addVertex(succ))
						missedNodes.add(succ);
					result.addEdge(node, succ);
				}
			}
			if(missedNodes.isEmpty())
				break;
			nodes = missedNodes;
			missedNodes = new LinkedHashSet<N>();
		}while(true);
		mLogger.debug("Convertion finished");
		return result;
	}
	public static <V, E> void writeDotFile(org.jgrapht.Graph<V, E> graph, File output)
	{
		FileWriter writer = null;
		try
		{
			mLogger.info("Exporting graph to file in DOT format...");
			DOTExporter<V, E> exporter = new DOTExporter<V, E>(new IntegerNameProvider<V>(), 
				new VertexNameProvider<V>()
				{

					@Override
					public String getVertexName(V vertex)
					{
						return StringEscapeUtils.escapeJava(vertex.toString());
					}
				
				}, new EdgeNameProvider<E>()
				{

					@Override
					public String getEdgeName(E edge)
					{
						return StringEscapeUtils.escapeJava(edge.toString());
					}
				});
			writer = new FileWriter(output);
			exporter.export(writer, graph);
			mLogger.info("Dot file of the graph has been stored at {}", output);
		}
		catch(Exception ex)
		{
			mLogger.error("Exception occurred: ", ex);
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
		}
	}
}
