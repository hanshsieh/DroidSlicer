package org.droidslicer.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.droidslicer.graph.entity.ActivityUnit;
import org.droidslicer.graph.entity.ApplicationUnit;
import org.droidslicer.graph.entity.Call2ReturnRelation;
import org.droidslicer.graph.entity.ComponentReachRelation;
import org.droidslicer.graph.entity.ComponentUnit;
import org.droidslicer.graph.entity.DataDependencyRelation;
import org.droidslicer.graph.entity.Entity;
import org.droidslicer.graph.entity.FileSystemDataRelation;
import org.droidslicer.graph.entity.ICCParamCalleeUnit;
import org.droidslicer.graph.entity.ICCParamCallerUnit;
import org.droidslicer.graph.entity.ICCRelation;
import org.droidslicer.graph.entity.ICCReturnCalleeUnit;
import org.droidslicer.graph.entity.ICCReturnCallerUnit;
import org.droidslicer.graph.entity.ICCUnit;
import org.droidslicer.graph.entity.IntentCommRelation;
import org.droidslicer.graph.entity.IntentCommUnit;
import org.droidslicer.graph.entity.ProviderUnit;
import org.droidslicer.graph.entity.ReceiverUnit;
import org.droidslicer.graph.entity.RelationEntity;
import org.droidslicer.graph.entity.SUseUnit;
import org.droidslicer.graph.entity.ServiceUnit;
import org.droidslicer.graph.entity.UnitEntity;
import org.droidslicer.graph.entity.UriCommRelation;
import org.droidslicer.graph.entity.UriCommUnit;
import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.Node;
import att.grappa.Parser;

public class BehaviorGraphDotFileHelper
{
	private static class LabelFilter implements Predicate<Element>
	{
		private final String mLabel;
		public LabelFilter(String label)
		{
			mLabel = label;
		}
		@Override
		public boolean apply(Element ele)
		{
			String label = getLabel(ele);
			return label.contains(mLabel);
		}
	}
	private static class TypeFilter implements Predicate<Element>
	{
		private final Class<? extends Entity> mType;
		public TypeFilter(Class<? extends Entity> type)
		{
			mType = type;
		}
		@Override
		public boolean apply(Element ele)
		{
			return mType.isAssignableFrom(getElementType(ele));
		}
	}
	private final Logger mLogger = LoggerFactory.getLogger(BehaviorGraphDotFileHelper.class);
	private final static File ANALYSIS_TAG = new File("run-20140622-2");
	//private final static File ANALYSIS_TAG = new File("run-20140714");
	//private final static File ANALYSIS_TAG = new File("run-20140714-2");
	//private final static File ANALYSIS_TAG = new File("run-20140710-2");
	//private final static File ANALYSIS_TAG = new File("run-test");
	private final static File FILE_BEHAVIOR_GRAPH = new File("behavior_graph.dot");
	private final static File DOT_FILE_FOR_ANALYSIS_TAG = new File(ANALYSIS_TAG, FILE_BEHAVIOR_GRAPH.getPath());
	private final static File ANALYSIS_DIR = new File("D:/EvaluationData/");
	private final static File GOOGLE_PLAY_DIR = new File("GooglePlay");
	private final static File MALWARE_DIR = new File("Malware");
	private final static File DOT_FILE_CALLGOGOLOOK2 = 
			concatFiles(ANALYSIS_DIR, GOOGLE_PLAY_DIR, "022 gogolook.callgogolook2", DOT_FILE_FOR_ANALYSIS_TAG);
	private final static File DOT_FILE_MSAFE_TW = 
			concatFiles(ANALYSIS_DIR, GOOGLE_PLAY_DIR, "007 com.qihoo.msafe_tw", DOT_FILE_FOR_ANALYSIS_TAG);
	private final static File DOT_FILE_LINE = 
			concatFiles(ANALYSIS_DIR, GOOGLE_PLAY_DIR, "000 jp.naver.line.android", DOT_FILE_FOR_ANALYSIS_TAG);
	private final static File DOT_FILE_CLEANMASTER_MGUARD = 
			concatFiles(ANALYSIS_DIR, GOOGLE_PLAY_DIR, "003 com.cleanmaster.mguard", DOT_FILE_FOR_ANALYSIS_TAG);
	private final static File DOT_FILE_FACEBOOK = 
			concatFiles(ANALYSIS_DIR, GOOGLE_PLAY_DIR, "001 com.facebook.katana", DOT_FILE_FOR_ANALYSIS_TAG);
	private final static File DOT_FILE_DENDROID = 
			concatFiles(ANALYSIS_DIR, MALWARE_DIR, "Dendroid", FILE_BEHAVIOR_GRAPH);
	
	private final static String LABEL = "label";
	private DirectedGraph<Node, Edge> mGraph;
	private DirectedGraph<Node, RelationEntity> mDependencyGraph;
	private static final Pattern PAT_ICC_TARGET_SEL = Pattern.compile("target=([a-zA-Z]+)");
	private static File concatFiles(Object... files)
	{
		File result = null;
		for(Object val : files)
		{
			if(result == null)
			{
				if(val instanceof String)
					result = new File((String)val);
				else
					result = (File)val;
			}
			else
			{
				if(val instanceof String)
					result = new File(result, (String)val);
				else
					result = new File(result, ((File)val).getPath());
			}
		}
		return result;
	}
	@SuppressWarnings("unchecked")
	private void load(File file)
		throws IOException
	{
		mLogger.info("Loading graph from file " + file.getPath());
		InputStream input = null;
		Parser parser;
		try
		{
			input = new FileInputStream(file);
			parser = new Parser(input);
			try
			{
				parser.parse();
			}
			catch(Exception ex)
			{
				throw new IOException(ex);
			}
		}
		finally
		{
			if(input != null)
			{
				try
				{
					input.close();
				}
				catch(Exception ex)
				{}
			}
		}
		att.grappa.Graph attGraph = parser.getGraph();
		mGraph = new DefaultDirectedGraph<Node, Edge>(Edge.class);
		mDependencyGraph = 
				new DefaultDirectedGraph<Node, RelationEntity>(new EdgeFactory<Node, RelationEntity>()
		{
			@Override
			public RelationEntity createEdge(Node node1, 
					Node node2)
			{
				throw new RuntimeException();
			}			
		});
		for(Iterator<Node> itr = attGraph.nodeElements(); itr.hasNext(); )
		{
			Node node = itr.next();
			mGraph.addVertex(node);
			Class<? extends Entity> nodeType = getElementType(node);
			if(ICCUnit.class.isAssignableFrom(nodeType) || SUseUnit.class.isAssignableFrom(nodeType))
				mDependencyGraph.addVertex(node);
		}
		for(Iterator<Edge> itr = attGraph.edgeElements(); itr.hasNext(); )
		{
			Edge edge = itr.next();
			Node source = edge.getTail();
			Node target = edge.getHead();
			mGraph.addEdge(source, target, edge);
		}
		for(Edge edge : mGraph.edgeSet())
		{
			Class<? extends Entity> edgeType = getElementType(edge);
			if(ICCRelation.class.isAssignableFrom(edgeType))
			{
				Node paramCaller = mGraph.getEdgeSource(edge);
				String targetSel = getICCTargetSelector(paramCaller);
				Node comp = mGraph.getEdgeTarget(edge);
				{
					Predicate<Element> nodeFilter = new TypeFilter(ICCParamCalleeUnit.class);
					if(targetSel != null)
						nodeFilter = Predicates.and(nodeFilter, new LabelFilter(" " + targetSel + "("));
					Set<Node> paramCallees = getOutgoingNodes(comp, 
							new TypeFilter(ComponentReachRelation.class), 
							nodeFilter);
					for(Node paramCallee : paramCallees)
					{
						if(IntentCommRelation.class.isAssignableFrom(edgeType))
							mDependencyGraph.addEdge(paramCaller, paramCallee, new IntentCommRelation());
						else if(UriCommRelation.class.isAssignableFrom(edgeType))
							mDependencyGraph.addEdge(paramCaller, paramCallee, new UriCommRelation());
						else
							throw new RuntimeException();
					}
				}
				{
					Set<Node> retCallers = getOutgoingNodes(paramCaller, new TypeFilter(Call2ReturnRelation.class));
					Predicate<Element> nodeFilter = new TypeFilter(ICCReturnCalleeUnit.class);
					if(targetSel != null)
						nodeFilter = Predicates.and(nodeFilter, new LabelFilter(" " + targetSel + "("));
					Set<Node> retCallees = getOutgoingNodes(comp, 
							new TypeFilter(ComponentReachRelation.class), 
							nodeFilter);
					for(Node retCallee : retCallees)
					{
						for(Node retCaller : retCallers)
						{
							// We have no exit-to-return-site edge, so we use data dependency relation instead
							mDependencyGraph.addEdge(retCallee, retCaller, new DataDependencyRelation());
						}
					}
				}
			}
			else if(DataDependencyRelation.class.isAssignableFrom(edgeType))
			{
				Node src = mGraph.getEdgeSource(edge);
				Node target = mGraph.getEdgeTarget(edge);
				mDependencyGraph.addEdge(src, target, new DataDependencyRelation());
			}
			else if(FileSystemDataRelation.class.isAssignableFrom(edgeType))
			{
				Node src = mGraph.getEdgeSource(edge);
				Node target = mGraph.getEdgeTarget(edge);
				mDependencyGraph.addEdge(src, target, new FileSystemDataRelation());
			}
			else if(Call2ReturnRelation.class.isAssignableFrom(edgeType))
			{
				Node src = mGraph.getEdgeSource(edge);
				Node target = mGraph.getEdgeTarget(edge);
				mDependencyGraph.addEdge(src, target, new Call2ReturnRelation());
			}
			else if(ComponentReachRelation.class.isAssignableFrom(edgeType))
			{}
			else
				throw new RuntimeException();
		}
		mLogger.info("Graph is loaded");
	}
	private static String getICCTargetSelector(Node node)
	{
		Class<? extends Entity> nodeType = getElementType(node);
		if(IntentCommUnit.class.isAssignableFrom(nodeType))
			return null;
		else if(UriCommUnit.class.isAssignableFrom(nodeType))
		{
			Matcher matcher = PAT_ICC_TARGET_SEL.matcher(getLabel(node));
			if(matcher.find())
			{
				return matcher.group(1);
			}
			else
				return null;
		}
		else
			throw new IllegalArgumentException();
	}
	private static Class<? extends Entity> getElementType(Element ele)
	{
		String label = getLabel(ele);
		if(ele instanceof Node)
		{
			if(label.contains("[COMP ") || 
				label.contains("[PROVIDER ") || 
				label.contains("[" + ActivityUnit.class.getSimpleName()) || 
				label.contains("[" + ProviderUnit.class.getSimpleName()) || 
				label.contains("[" + ReceiverUnit.class.getSimpleName()) || 
				label.contains("[" + ServiceUnit.class.getSimpleName()) || 
				label.contains("[" + ApplicationUnit.class.getSimpleName()))
			{
				return ComponentUnit.class;
			}
			else if(label.contains("[URI_COMM ") || 
					label.contains("[" + UriCommUnit.class.getSimpleName()))
				return UriCommUnit.class; 
			else if(label.contains("[INTENT_COMM ") || 
					label.contains("[" + IntentCommUnit.class.getSimpleName()))
				return IntentCommUnit.class;
			else if(label.contains("ICCRetCallerUnit ") || label.contains("ICCReturnCallerUnit ") || 
					label.contains("[" + ICCReturnCallerUnit.class.getSimpleName()))
				return ICCReturnCallerUnit.class;
			else if(label.contains("ICCParamCalleeUnit ") || label.contains("ICCParameterCalleeUnit ") || 
					label.contains("[" + ICCParamCalleeUnit.class.getSimpleName()))
				return ICCParamCalleeUnit.class;
			else if(label.contains("ICCRetCalleeUnit ") || label.contains("ICCReturnCalleeUnit ") ||
					label.contains("[" + ICCReturnCalleeUnit.class))
				return ICCReturnCalleeUnit.class;
			else
				return SUseUnit.class;
		}
		else if(ele instanceof Edge)
		{
			if(label.contains(ComponentReachRelation.class.getSimpleName()))
				return ComponentReachRelation.class;
			else if(label.contains(Call2ReturnRelation.class.getSimpleName()))
				return Call2ReturnRelation.class;
			else if(label.contains(DataDependencyRelation.class.getSimpleName()))
				return DataDependencyRelation.class;
			else if(label.contains(FileSystemDataRelation.class.getSimpleName()))
				return FileSystemDataRelation.class;
			else if(label.contains(IntentCommRelation.class.getSimpleName()))
				return IntentCommRelation.class;
			else if(label.contains(UriCommRelation.class.getSimpleName()))
				return UriCommRelation.class;
			else
				throw new IllegalArgumentException();
		}
		else
			throw new IllegalArgumentException();
	}
	private static String getLabel(Element ele)
	{
		Object val = ele.getAttribute(LABEL).getValue();
		if(!(val instanceof String))
			throw new IllegalArgumentException();
		return (String)val;
	}
	private Set<Node> getOutgoingNodes(Node node, Predicate<? super Edge> edgeFilter, Predicate<? super Node> nodeFilter)
	{
		Set<Node> result = new HashSet<Node>();
		for(Edge edge : mGraph.outgoingEdgesOf(node))
		{
			if(!edgeFilter.apply(edge))
				continue;
			Node target = mGraph.getEdgeTarget(edge);
			if(nodeFilter.apply(target))
				result.add(target);
		}
		return result;
	}
	private Set<Node> getOutgoingNodes(Node node, Predicate<? super Edge> edgeFilter)
	{
		return getOutgoingNodes(node, edgeFilter, Predicates.alwaysTrue());
	}
	private Set<Node> getIncommingNodes(Node node, Predicate<? super Edge> edgeFilter, Predicate<? super Node> nodeFilter)
	{
		Set<Node> result = new HashSet<Node>();
		for(Edge edge : mGraph.incomingEdgesOf(node))
		{
			if(!edgeFilter.apply(edge))
				continue;
			Node src = mGraph.getEdgeSource(edge);
			if(nodeFilter.apply(src))
				result.add(src);
		}
		return result;
	}
	private Set<Node> getIncommingNodes(Node node, Predicate<? super Edge> edgeFilter)
	{
		return getIncommingNodes(node, edgeFilter, Predicates.alwaysTrue());
	}
	private void simpleFindPath(Predicate<Node> fromNodesPred, Predicate<Node> toNodesPred)
	{
		simpleFindPath(fromNodesPred, toNodesPred, Predicates.<Node>alwaysTrue());
	}
	private void simpleFindPath(Predicate<Node> fromNodesPred, Predicate<Node> toNodesPred, Predicate<Node> nodeAcceptPred)
	{
		Collection<Node> srcNodes = Collections2.filter(mGraph.vertexSet(), fromNodesPred);
		for(Node srcNode : srcNodes)
		{
			List<Node> path = findPath(Collections.singleton(srcNode), toNodesPred, nodeAcceptPred);
			if(path == null)
			{
				mLogger.info("No path is found from ({}) {}", srcNode.getName(), getLabel(srcNode));
			}
			else
			{
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for(Node node : path)
				{
					if(first)
						first = false;
					else
						builder.append("\n -> \n");
					builder.append(node.getName());
					builder.append(' ');
					builder.append(getLabel(node));
				}
				mLogger.info("path:\n{}", builder);
			}
		}
	}
	private List<Node> findPath(Collection<Node> startNodes, Predicate<Node> endPred)
	{
		return findPath(startNodes, endPred, Predicates.<Node>alwaysTrue());
	}
	private List<Node> findPath(Collection<Node> startNodes, Predicate<Node> endPred, Predicate<Node> acceptNodePred)
	{
		Map<Node, Integer> disForNodes = new HashMap<Node, Integer>();
		Queue<Node> que = new ArrayDeque<Node>();
		startNodes = Collections2.filter(startNodes, acceptNodePred);
		for(Node node : startNodes)
		{
			que.add(node);	
			disForNodes.put(node, 0);
			if(endPred.apply(node))
				return Collections.singletonList(node);
		}
		Node endNode = null;
		while(!que.isEmpty())
		{
			Node node = que.poll();
			if(endPred.apply(node))
			{
				endNode = node;
				break;
			}
			int dis = disForNodes.get(node).intValue();
			for(RelationEntity edge : mDependencyGraph.outgoingEdgesOf(node))
			{
				Node succ = mDependencyGraph.getEdgeTarget(edge);
				if(!acceptNodePred.apply(succ))
					continue;
				if(disForNodes.containsKey(succ))
					continue;
				disForNodes.put(succ, dis + 1);
				que.add(succ);
			}
		}
		if(endNode == null)
			return null;
		Node[] path = new Node[disForNodes.get(endNode).intValue() + 1];
		Node node = endNode;
		while(true)
		{
			int dis = disForNodes.get(node);
			path[dis] = node;
			if(dis == 0)
				break;
			Node next = null;
			for(RelationEntity edge : mDependencyGraph.incomingEdgesOf(node))
			{
				Node pre = mDependencyGraph.getEdgeSource(edge);
				Integer preDis = disForNodes.get(pre);
				if(preDis == null || preDis.intValue() != dis - 1)
					continue;
				next = pre;
				break;		
			}
			if(next == null)
				return null;
			node = next;
		}
		return Arrays.asList(path);
	}
	
	// Dendroid
	public void find_dendroid_contacts_leaks()
		throws IOException
	{
		load(DOT_FILE_DENDROID);
		Set<Node> readContactsNodes = new HashSet<Node>();
		for(Node node : mGraph.vertexSet())
		{
			Class<? extends Entity> type = getElementType(node);
			if(!SUseUnit.class.isAssignableFrom(type))
				continue;
			String label = getLabel(node);
			if(!label.contains("READ_CONTACTS"))
				continue;
			readContactsNodes.add(node);
		}
		
		for(Node readContactsNode : readContactsNodes)
		{
			List<Node> path = findPath(Collections.singleton(readContactsNode), new Predicate<Node>()
			{
				@Override
				public boolean apply(Node node)
				{
					String label = getLabel(node);
					return label.contains("SEND_SMS");
				}			
			});
			if(path == null)
			{
				mLogger.info("No path is found");
			}
			else
			{
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for(Node node : path)
				{
					if(first)
						first = false;
					else
						builder.append("\n -> \n");
					builder.append(getLabel(node));
				}
				mLogger.info("path:\n{}", builder);
			}
		}
	}
	
	// 007 com.qihoo.msafe_tw
	public void find_msafetw_SMS_to_internet()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		},
		new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return !label.contains("uri=?");
			}
		});
	}
	
	// 007 com.qihoo.msafe_tw
	public void find_msafetw_calendar_2_sms()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		Set<Node> srcNodes = new HashSet<Node>();
		for(Node node : mGraph.vertexSet())
		{
			String label = getLabel(node);
			if(!(label.contains("READ_CALENDAR")))
				continue;
			srcNodes.add(node);
		}
		for(Node srcNode : srcNodes)
		{
			List<Node> path = findPath(Collections.singleton(srcNode), new Predicate<Node>()
			{
				@Override
				public boolean apply(Node node)
				{
					String label = getLabel(node);
					return label.contains("sendTextMessage");
				}			
			});
			if(path == null)
			{
				mLogger.info("No path is found");
			}
			else
			{
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for(Node node : path)
				{
					if(first)
						first = false;
					else
						builder.append("\n -> \n");
					builder.append(getLabel(node));
				}
				mLogger.info("path:\n{}", builder);
			}
		}
	}
	
	
	// 003 com.cleanmaster.mguard
	public void find_cleanmastermguard_bookmark_2_internet()
		throws IOException
	{
		load(DOT_FILE_CLEANMASTER_MGUARD);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_HISTORY_BOOKMARKS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node).toUpperCase();
				return label.contains("SOCKET") || label.contains("URL_CONN");
			}
		});
	}
	// 003 com.cleanmaster.mguard
	public void find_cleanmastermguard_imei_2_file()
		throws IOException
	{
		load(DOT_FILE_CLEANMASTER_MGUARD);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_PHONE_STATE") || label.contains("getDeviceId");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("FileOutputUnit");
			}
		});
	}
	
	// 001 com.facebook.katana
	public void find_line_sms_2_file()
		throws IOException
	{
		load(DOT_FILE_LINE);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("RECEIVE_SMS") || label.contains("READ_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("FileOutput");
			}
		});
	}
	// 001 com.facebook.katana
	public void find_line_sms_2_internet()
		throws IOException
	{
		load(DOT_FILE_LINE);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("RECEIVE_SMS") || label.contains("READ_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
				//return label.contains("ICCReturnCallerUnit");
			}
		});
	}
	// 001 com.facebook.katana
	public void find_line_contacts_2_internet()
		throws IOException
	{
		load(DOT_FILE_LINE);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_CONTACTS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		});
	}
	// 000 jp.naver.line.android
	public void find_line_contacts_2_file()
		throws IOException
	{
		load(DOT_FILE_LINE);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("content://com.android.contacts");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("FileOutputUnit");
			}
		});
	}
	
	// 001 com.facebook.katana
	public void find_facebook_imei_2_internet()
		throws IOException
	{
		load(DOT_FILE_FACEBOOK);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("PHONE_STATE") || label.contains("getDeviceId");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		});
	}
	// 001 com.facebook.katana
	public void find_facebook_sms_2_internet()
		throws IOException
	{
		load(DOT_FILE_FACEBOOK);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("RECEIVE_SMS") || label.contains("READ_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
				//return label.contains("ICCReturnCallerUnit");
			}
		});
	}
	// 001 com.facebook.katana
	public void find_facebook_contacts_2_internet()
		throws IOException
	{
		load(DOT_FILE_FACEBOOK);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_CONTACTS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		});
	}
	// 007 com.qihoo.msafe_tw
	public void find_msafetw_sms_2_sms()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_SMS") || label.contains("RECEIVE_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("SEND_SMS");
			}
		});
	}
	
	// 007 com.qihoo.msafe_tw
	public void find_msafetw_phonestate_2_sms()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_PHONE_STATE");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("SEND_SMS");
			}
		});
	}

	// 007 com.qihoo.msafe_tw
	public void find_msafetw_internet_2_sms()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("SEND_SMS");
			}
		});
	}
	// 007 com.qihoo.msafe_tw
	public void find_msafetw_phonestate_2_file()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_PHONE_STATE");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("FileOutput");
			}
		});
	}
	// 007 com.qihoo.msafe_tw
	public void find_msafetw_contacts_2_sms()
		throws IOException
	{
		load(DOT_FILE_MSAFE_TW);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_CONTACTS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("SEND_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				//return label.contains("onActivityResult");
				return true;
			}			
		});
	}
	
	// 000 jp.naver.line.android
	public void find_line_imei_2_file()
		throws IOException
	{
		load(DOT_FILE_LINE);
		Set<Node> srcNodes = new HashSet<Node>();
		for(Node node : mGraph.vertexSet())
		{
			String label = getLabel(node);
			if(!(label.contains("getDeviceId")))
				continue;
			srcNodes.add(node);
		}
		for(Node srcNode : srcNodes)
		{
			List<Node> path = findPath(Collections.singleton(srcNode), new Predicate<Node>()
			{
				@Override
				public boolean apply(Node node)
				{
					String label = getLabel(node);
					return label.contains("FileOutputUnit");
				}			
			});
			if(path == null)
			{
				mLogger.info("No path is found");
			}
			else
			{
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for(Node node : path)
				{
					if(first)
						first = false;
					else
						builder.append("\n -> \n");
					builder.append(getLabel(node));
				}
				mLogger.info("path:\n{}", builder);
			}
		}
	}
	//022 gogolook.callgogolook2
	public void find_callgogolook2_calllog_to_internet()
		throws IOException
	{
		load(DOT_FILE_CALLGOGOLOOK2);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("content://call_log");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		},
		new Predicate<Node>()
		{

			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return !label.contains("uri=?");
			}
		});
	}
	//022 gogolook.callgogolook2
	public void find_callgogolook2_SMS_to_internet()
		throws IOException
	{
		load(DOT_FILE_CALLGOGOLOOK2);
		simpleFindPath(new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node) 
			{
				String label = getLabel(node);
				return label.contains("READ_SMS");
			}
		}, new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return label.contains("INTERNET");
			}
		},
		new Predicate<Node>()
		{
			@Override
			public boolean apply(Node node)
			{
				String label = getLabel(node);
				return !label.contains("uri=?");
			}
		});
	}
	public BehaviorGraphDotFileHelper()
		throws IOException
	{
		find_callgogolook2_SMS_to_internet();
		//find_callgogolook2_calllog_to_internet();
		//find_msafetw_phonestate_2_sms();
		//find_msafetw_contacts_2_sms();
		//find_line_imei_2_file();
		//find_line_contacts_2_file();
		//find_msafetw_calendar_2_sms();
		//find_cleanmastermguard_bookmark_2_internet();
		//find_cleanmastermguard_imei_2_file();
		//find_dendroid_contacts_leaks();
		//find_facebook_imei_2_internet();
		//find_facebook_sms_2_internet();
		//find_facebook_contacts_2_internet();
		//find_line_sms_2_internet();
		//find_line_contacts_2_internet();
		//find_msafetw_sms_2_sms();
		//find_line_sms_2_file();
		//find_msafetw_internet_2_sms();
		//find_msafetw_contacts_2_sms();
		//find_msafetw_phonestate_2_sms();
		//find_msafetw_phonestate_2_file();
		//find_msafetw_SMS_to_internet();
	}
	public static void main(String[] args)
		throws Exception
	{
		new BehaviorGraphDotFileHelper();

	}
}
