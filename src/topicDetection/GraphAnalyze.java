package topicDetection;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GraphAnalyze {
	Constants constants;

	public HashMap<String, Node> graphNodes;

	public PrintStream logger = System.out;

	/**
	 * Constructor
	 * 
	 * @param cons	configuratioin parameters
	 */
	public GraphAnalyze(Constants cons) {
		constants = cons;
	}

	/**
	 * Builds a co-occurrence graph in which the nodes 
	 * are the words of the documents and the edges link 
	 * words in the same document.
	 * 
	 * @param documents			the documents to classify
	 * @param DF				the document frequency of documents'keywords
	 * @param removeDuplicates	boolean specified in the configuration parameters, true if you want to remove the documents with the same text
	 * @return					the nodes created
	 */
	public HashMap<String, Node> buildGraph(HashMap<String, Document> documents, HashMap<String, Double> DF, boolean removeDuplicates) {
		new WriteConsole("--- BUILDING GRAPH ---\n");
		graphNodes = new HashMap<String, Node>();
		// -- add nodes -------
		for (Document d : documents.values())
			if (!removeDuplicates || !d.isDuplicate)
				for (Keyword k : d.keywords.values()) {
					Node n = null;
					if (graphNodes.containsKey(k.baseForm)){
						n = graphNodes.get(k.baseForm);
						n.keyword.documents.put(d.id, d);
						n.keyword.tf+=k.tf;
					}
					else {
						// Keyword keyword = new Keyword(k.baseForm, k.word, 0, DF.get(k.baseForm), 0);
						Keyword keyword = new Keyword(k.baseForm, k.word, k.tf, DF.get(k.baseForm), 0);
						n = new Node(keyword);
						graphNodes.put(keyword.baseForm, n);
						n.keyword.documents.put(d.id, d);
					}
					// n.keyword.documents.put(d.id, d);
					// n.keyword.tf++;
				}

		/* Print keywords ordered by term frequency in a file before filtering. */
		TreeMap<Double, String> sortedTfMap = new TreeMap<Double, String>();
		for(Node n: graphNodes.values())
			sortedTfMap.put(n.keyword.tf, n.keyword.word);
		File output = new File("/tmp/keygraph_output/keywordsSortedByTf.txt");
    	try {
			output.createNewFile();
			DataOutputStream keywordsout = new DataOutputStream(new FileOutputStream(output));
			Set set = sortedTfMap.entrySet();
			Iterator i = set.iterator();
			while(i.hasNext()) {
			      Map.Entry me = (Map.Entry)i.next();
			      keywordsout.writeBytes(me.getKey() + ": " + me.getValue().toString() + "\n");
			    }
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
		/* Print keywords ordered by document frequency in a file before filtering. */
    	TreeMap<Double, String> sortedDfMap = new TreeMap<Double, String>();
		for(Node n: graphNodes.values())
			sortedDfMap.put(DF.get(n.keyword.baseForm), n.keyword.word);
		File output1 = new File("/tmp/keygraph_output/keywordsSortedByDf.txt");
    	try {
			output1.createNewFile();
			DataOutputStream keywordsout1 = new DataOutputStream(new FileOutputStream(output1));
			Set set = sortedDfMap.entrySet();
			Iterator i = set.iterator();
			while(i.hasNext()) {
			      Map.Entry me = (Map.Entry)i.next();
			      keywordsout1.writeBytes(me.getKey() + ": " + me.getValue().toString() + "\n");
			    }
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	
		// -- filter nodes ----------
		ArrayList<String> toRemoveIds = new ArrayList<String>();
		for (Node n : graphNodes.values())
			if (n.keyword.tf < constants.NODE_DF_MIN || DF.get(n.keyword.baseForm) > constants.NODE_DF_MAX * documents.size())
				toRemoveIds.add(n.keyword.baseForm);
		for (String baseForm : toRemoveIds)
			graphNodes.remove(baseForm);
		toRemoveIds.clear();

		// -- add edges --------
		for (Document d : documents.values())
			if (!removeDuplicates || !d.isDuplicate)
				for (Keyword k1 : d.keywords.values())
					if (graphNodes.containsKey(k1.baseForm)) {
						Node n1 = graphNodes.get(k1.baseForm);
						for (Keyword k2 : d.keywords.values()) {
							if (graphNodes.containsKey(k2.baseForm) && k1.baseForm.compareTo(k2.baseForm) == -1) {
								Node n2 = graphNodes.get(k2.baseForm);
								String edgeId = Edge.getId(n1, n2);
								if (!n1.edges.containsKey(edgeId)) {
									Edge e = new Edge(n1, n2, edgeId);
									n1.edges.put(edgeId, e);
									n2.edges.put(edgeId, e);
								}
								n1.edges.get(edgeId).df++;
							}
						}
					}

		// -- filter edges ---------
		ArrayList<Edge> toRemove = new ArrayList<Edge>();
		for (Node n : graphNodes.values()) {
			for (Edge e : n.edges.values()) {
				// Double
				// MI=Math.log(1.0*e.df*documents.size()/DF.get(e.n1.keyword.baseForm)/DF.get(e.n2.keyword.baseForm))/Math.log(2);
				Double MI = e.df / (e.n1.keyword.df + e.n2.keyword.df - e.df);
				//System.out.println("...........................MI: " + MI);
				if (e.df < constants.EDGE_DF_MIN || MI < constants.EDGE_CORRELATION_MIN) {
					// ||Math.max(1.0 * e.df / e.n1.keyword.df, 1.0 * e.df /
					// e.n2.keyword.df) < Constants.EDGE_CORRELATION_MIN) {
					toRemove.add(e);
				} else
					e.computeCPs();
				// e.cp1=e.cp2=e.df*documents.size()/DF.get(e.n1.keyword.baseForm)*DF.get(e.n2.keyword.baseForm);

			}
			for (Edge e : toRemove) {
				e.n1.edges.remove(e.id);
				e.n2.edges.remove(e.id);
			}
			toRemove.clear();
		}
		// -- postfilter nodes ----------
		for (Node n : graphNodes.values())
			if (n.edges.size() == 0)
				toRemoveIds.add(n.keyword.baseForm);
		for (String baseForm : toRemoveIds)
			graphNodes.remove(baseForm);
		toRemoveIds.clear();
				
		return graphNodes;
	}

	/**
	 * Extract groups of nodes more connected to each other, 
	 * called communities.
	 * 
	 * @param nodes	nodes of the co-occurrence graph
	 * @return		communities extracted from the graph
	 */
	public ArrayList<HashMap<String, Node>> extractCommunities(HashMap<String, Node> nodes) {
		new WriteConsole("--- EXTRACT COMMUNITIES ---\n");
		for (Node n : nodes.values())
			n.visited = false;
		ArrayList<HashMap<String, Node>> communities = new ArrayList<HashMap<String, Node>>();
		ArrayList<HashMap<String, Node>> connectedComponents = findConnectedComponents(nodes);
		while (connectedComponents.size() != 0) {
			HashMap<String, Node> subNodes = connectedComponents.remove(0);
			// System.out.println("##########"+subNodes.size());
			if (subNodes.size() >= constants.CLUSTER_NODE_SIZE_MIN) {
				if (subNodes.size() > constants.CLUSTER_NODE_SIZE_MAX) {
					new WriteConsole("filterTopKPercentOfEdges\n");
					filterTopKPercentOfEdges(subNodes, 1);
					for (Node n : subNodes.values())
						n.visited = false;
					connectedComponents.addAll(0, findConnectedComponents(subNodes));
				} else if (constants.CLUSTERING_ALG.toLowerCase().equals("newman"))
					findCommunities_Newman(subNodes, communities);
				else
					findCommunities_betweenness_centrality(subNodes, communities);
			}
			// else
			// if(subNodes.size()>2)
			// communities.add(subNodes);
		}
		return communities;
	}

	/**
	 * Filter k% of edges from the connected component.
	 * 
	 * @param nodes the nodes of the co-occurrence graph
	 * @param k		percentage of edges to be filtered
	 */
	private void filterTopKPercentOfEdges(HashMap<String, Node> nodes, double k) {
		// -- To compute betweenness centrality scores!
		// Edge maxEdge = findMaxEdge(nodes);

		int edgeSize = 0;
		for (Node n1 : nodes.values())
			edgeSize += n1.edges.size();
		edgeSize /= 2;
		Edge[] toRemove = new Edge[(int) (edgeSize * k / 100)];

		for (Node n1 : nodes.values()) {
			for (Edge e : n1.edges.values())
				if (n1.equals(e.n1))
					insertInto(toRemove, e);
		}
		
		new WriteConsole("Nodes total: " + nodes.size() + "	Edges total: " + edgeSize + "	Edges deleted: " + toRemove.length + "\n");
		for (Edge e : toRemove) {
			e.n1.edges.remove(e.id);
			e.n2.edges.remove(e.id);

		}
	}

	/**
	 * Finds the groups of interconnected nodes within the graph.
	 * 
	 * @param nodes	the nodes of the co-occurrence graph
	 * @return		groups of interconnected nodes
	 */
	public ArrayList<HashMap<String, Node>> findConnectedComponents(HashMap<String, Node> nodes) {
		ArrayList<HashMap<String, Node>> cc = new ArrayList<HashMap<String, Node>>();
		while (nodes.size() > 0) {
			Node source = nodes.values().iterator().next();
			HashMap<String, Node> subNodes = new HashMap<String, Node>();
			ArrayList<Node> q = new ArrayList<Node>();
			q.add(0, source);
			while (q.size() > 0) {
				Node n = q.remove(0);
				n.visited = true;
				nodes.remove(n.keyword.baseForm);
				subNodes.put(n.keyword.baseForm, n);
				for (Edge e : n.edges.values()) {
					Node n2 = e.opposit(n);
					if (!n2.visited) {
						n2.visited = true;
						q.add(n2);
					}
				}
			}
			cc.add(subNodes);
		}
		return cc;
	}

	public ArrayList<HashMap<String, Node>> findConnectedComponentsFromSubset(HashMap<String, Node> nodes) {
		new WriteConsole("Finding connected nodes...\n");
		ArrayList<HashMap<String, Node>> cc = new ArrayList<HashMap<String, Node>>();
		while (nodes.size() > 0) {
			Node source = nodes.values().iterator().next();
			HashMap<String, Node> subNodes = new HashMap<String, Node>();
			ArrayList<Node> q = new ArrayList<Node>();
			q.add(0, source);
			while (q.size() > 0) {
				Node n = q.remove(0);
				n.visited = true;
				nodes.remove(n.keyword.baseForm);
				subNodes.put(n.keyword.baseForm, n);
				for (Edge e : n.edges.values()) {
					Node n2 = e.opposit(n);
					if (!n2.visited && nodes.containsKey(n2.keyword.baseForm)) {
						n2.visited = true;
						q.add(n2);
					}
				}
			}
			cc.add(subNodes);
		}
		return cc;
	}

	/**
	 * Finds the communities, using the betweenness centrality score.
	 * 
	 * @param nodes			the nodes of the co-occurrence graph
	 * @param communities	groups of interconnected nodes
	 * @return				communities
	 */
	public ArrayList<HashMap<String, Node>> findCommunities_betweenness_centrality(HashMap<String, Node> nodes, ArrayList<HashMap<String, Node>> communities) {
//		System.out.println("Find Communities: " + nodes.size() + " nodes");
//		for(Node n: nodes.values())
//			System.out.print(n.keyword.word + ", ");
//		System.out.println(" ");
		
		Edge maxEdge = findMaxEdge(nodes);
		if (getFilterStatus(nodes.size(), maxEdge)) {
			maxEdge.n1.edges.remove(maxEdge.id);
			maxEdge.n2.edges.remove(maxEdge.id);

			// -- check if still connected ----
			HashMap<String, Node> subgraph1 = findSubgraph(maxEdge.n1, nodes);
			if (subgraph1.size() == nodes.size())
				return findCommunities_betweenness_centrality(nodes, communities);
			else {
				for (String key : subgraph1.keySet())
					nodes.remove(key);

				if (maxEdge.cp1 > constants.EDGE_CP_MIN_TO_DUPLICATE) {
					Keyword k = maxEdge.n2.keyword;
					Node newn = new Node(new Keyword(k.baseForm, k.word, k.tf, k.df, 0));
					Edge e = new Edge(maxEdge.n1, newn);
					maxEdge.n1.edges.put(e.id, e);
					newn.edges.put(e.id, e);
					subgraph1.put(k.baseForm, newn);
				}
				if (maxEdge.cp2 > constants.EDGE_CP_MIN_TO_DUPLICATE) {
					Keyword k = maxEdge.n1.keyword;
					Node newn = new Node(new Keyword(k.baseForm, k.word, k.tf, k.df, 0));
					Edge e = new Edge(newn, maxEdge.n2);
					maxEdge.n2.edges.put(e.id, e);
					newn.edges.put(e.id, e);
					nodes.put(k.baseForm, newn);
				}

				findCommunities_betweenness_centrality(subgraph1, communities);
				findCommunities_betweenness_centrality(nodes, communities);
				return communities;
			}
		} else {
			// if(nodes.size()>=Constants.CLUSTER_NODE_SIZE_MIN)
			communities.add(nodes);
			new WriteConsole("Find Communities: " + nodes.size() + " nodes\n");
			for(Node n: nodes.values())
				new WriteConsole(n.keyword.word + ", ");
			new WriteConsole("\n");
			return communities;
		}
	}

	public ArrayList<HashMap<String, Node>> findCommunities_Newman(HashMap<String, Node> nodes, ArrayList<HashMap<String, Node>> communities) {
		try {
			String label = "hassan";
			//printGraph(nodes);
			Runtime.getRuntime().exec("cp edge.txt FastCommunity_GPL_v1.0.3/edge.pairs");
			Runtime.getRuntime().exec("rm -f edge-fc_hassan.info edge-fc_" + label + ".joins", null, new File("FastCommunity_GPL_v1.0.3/"));
			Process run = Runtime.getRuntime().exec("FastCommunityMH -f edge.pairs -l " + label, null, new File("FastCommunity_GPL_v1.0.3/"));
			DataInputStream tmp = new DataInputStream(run.getInputStream());
			while (tmp.readLine() != null)
				;

			HashMap<String, Node> nodeIds = new HashMap<String, Node>();
			for (Node n : nodes.values())
				nodeIds.put(n.id, n);
			//File f = new File("FastCommunity_GPL_v1.0.3/edge-fc_" + label + ".joins");
			// System.out.println(nodes.size()+"::::"+f.exists()+"::::"+f.getAbsolutePath());
			BufferedReader in = new BufferedReader(new FileReader("FastCommunity_GPL_v1.0.3/edge-fc_" + label + ".joins"));
			String line = null;
			HashMap<String, Node> tmpNodes = new HashMap<String, Node>();
			Node n1 = null, n2 = null;
			String t1 = null, t2 = null;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split("\t");
				if (tokens[0].charAt(0) != '-') {
					t1 = tokens[0];
					t2 = tokens[1];
					n1 = new Node(new Keyword(t1, null, 0, 0, 0));
					n2 = new Node(new Keyword(t2, null, 0, 0, 0));
					if (!tmpNodes.containsKey(t1))
						tmpNodes.put(t1, n1);
					if (!tmpNodes.containsKey(t2))
						tmpNodes.put(t2, n2);
					Edge e = new Edge(n1, n2, Edge.getId(n1, n2));
					n1.edges.put(e.id, e);
					n2.edges.put(e.id, e);
				}
			}
			ArrayList<HashMap<String, Node>> ccs = findConnectedComponents(tmpNodes);
			new WriteConsole(tmpNodes.size() + "=>" + ccs.size() + "\n");
			tmpNodes = null;
			HashMap<String, Node> cc = null;
			HashMap<String, Node> community = null;
			while (ccs.size() > 0) {
				cc = ccs.remove(0);
				community = new HashMap<String, Node>();
				for (String key : cc.keySet())
					community.put(nodeIds.get(key).keyword.baseForm, nodeIds.get(key));
				communities.add(community);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return communities;
	}

	public boolean insertInto(Edge[] toRemove, Edge e) {
		if (toRemove[toRemove.length - 1] == null) { //se è il primo edge che considero
			int i = toRemove.length - 1;
			while (i >= 1 && (toRemove[i - 1] == null || toRemove[i - 1].compareBetweenness2(e) < 0)) {
				toRemove[i] = toRemove[i - 1];
				i--;
			}
			toRemove[i] = e;
			return true;
		} else if (toRemove[0].compareBetweenness2(e) >= 0)
			return false;
		else {
			int i = 0;
			while (i < toRemove.length - 1 && toRemove[i + 1].compareBetweenness2(e) < 0) {
				toRemove[i] = toRemove[i + 1];
				i++;
			}
			toRemove[i] = e;
			return true;
		}
	}

	private boolean getFilterStatus(int graphSize, Edge maxEdge) {
		double possiblePath = Math.min(graphSize * (graphSize - 1) / 2, constants.CLUSTER_NODE_SIZE_MAX * (constants.CLUSTER_NODE_SIZE_MAX - 1) / 2);
		// double th = Math.min(4.2 * Math.log(possiblePath) / Math.log(2) + 1,
		// 3 * graphSize);
		// double th =3 * graphSize;
		// double th= possiblePath / 8;
		double th = 6 * Math.log(possiblePath) / Math.log(2) + 1;
		// double th = 0;
		return graphSize >= constants.CLUSTER_NODE_SIZE_MIN && maxEdge != null && maxEdge.df > 0 && (maxEdge.betweennessScore > th);
	}

	public HashMap<String, Node> findSubgraph(Node source, HashMap<String, Node> nodes) {
		for (Node n : nodes.values())
			n.visited = false;
		HashMap<String, Node> subNodes = new HashMap<String, Node>();
		ArrayList<Node> q = new ArrayList<Node>();
		q.add(source);
		while (q.size() > 0) {
			Node n = q.remove(0);
			n.visited = true;
			subNodes.put(n.keyword.baseForm, n);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				if (!n2.visited) {
					n2.visited = true;
					q.add(n2);
				}
			}
		}
		return subNodes;
	}

	/**
	 * Finds the edge with the maximum betweenness centrality 
	 * score through the breadth-first search in the graph.
	 * 
	 * @param nodes	the nodes of the co-occurrence graph
	 * @return		the edge with the maximum betweenness centrality
	 */
	public Edge findMaxEdge(HashMap<String, Node> nodes) {
		// if (nodes.size() > 4 * Constants.CLUSTER_NODE_SIZE_MAX)
		// return findMaxEdgeApproximation(nodes);
		// logger.println("Node size: " + nodes.size());
		for (Node n : nodes.values()) {
			// n.clusteringInfo.lastCheked = null;
			// n.visited=false;
			for (Edge e : n.edges.values()) {
				e.betweennessScore = 0;
			}
		}
		Edge maxEdge = new Edge(null, null, null);
		maxEdge.betweennessScore = -1;
		for (Node source : nodes.values()) {
			for (Node n : nodes.values())
				n.visited = false;
			maxEdge = BFS(source, maxEdge);	//breadth-first search
			// maxEdge = dijkstra(source, maxEdge, nodes);
		}
		// maxEdge.clusteringInfo.betweennessScore /= 2;
		maxEdge.betweennessScore /= 2;
		// System.out.println(maxEdge.betweennessScore);
		return maxEdge;
	}

	public Edge dijkstra(Node source, Edge maxEdge, HashMap<String, Node> nodes) {
		HashSet<Node> q = new HashSet<Node>();
		HashMap<Node, Double> dist = new HashMap<Node, Double>();

		for (Node n : nodes.values()) {
			q.add(n);
			dist.put(n, 99999999999999.0);
		}
		dist.put(source, 0.0);
		q.remove(source);
		for (Edge e : source.edges.values()) {
			Node n2 = e.opposit(source);
			if (q.contains(n2)) {
				dist.put(n2, weight(e, n2));
				n2.prev = source;
			}
		}

		while (q.size() > 0) {
			Node n = findMin(dist, q);
			q.remove(n);
			maxEdge = updateCenterality(n, source, maxEdge);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				if (q.contains(n2)) {
					if (dist.get(n2) >= dist.get(n) + weight(e, n2)) {
						dist.put(n2, dist.get(n) + weight(e, n2));
						n2.prev = n;
					}
				}
			}
		}
		return maxEdge;
	}

	public double weight(Edge e, Node n2) {
		return 1.0 / e.df;
		// return 1- Math.max(e.cp1, e.cp2);
		// return (e.n2.equals(n2))? 1-e.cp1: 1-e.cp2;
	}

	public Node findMin(HashMap<Node, Double> dist, HashSet<Node> q) {
		Node min = null;
		Double minDist = -1.0;
		for (Node n : q)
			if (minDist == -1 || dist.get(n) < minDist) {
				minDist = dist.get(n);
				min = n;
			}
		return min;
	}

	public Edge BFS(Node source, Edge maxEdge) {
		ArrayList<Node> q = new ArrayList<Node>();
		q.add(source);
		// source.clusteringInfo.lastCheked = source.id;
		while (q.size() > 0) {
			Node n = q.remove(0);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				// if (!source.id.equals(n2.clusteringInfo.lastCheked)) {
				if (!n2.visited) {
					// n2.clusteringInfo.lastCheked = source.id;
					n2.visited = true;
					n2.prev = n;
					// e.clusteringInfo.betweennessScore++;
					updateCenterality(n2, source, maxEdge);
					if (e.compareBetweenness(maxEdge) > 0)
						maxEdge = e;
					q.add(n2);
				}
			}
		}
		return maxEdge;
	}

	public Edge updateCenterality(Node n, Node root, Edge maxEdge) {
		do {
			// n.betweennessScore ++;
			Edge e = n.edges.get(Edge.getId(n, n.prev));
			e.betweennessScore++;
			if (e.compareBetweenness(maxEdge) > 0)
				maxEdge = e;
			n = n.prev;
		} while (!n.equals(root));
		// root.centeralityScore += 1;
		return maxEdge;
	}

	public Edge findMaxEdgeApproximation(HashMap<String, Node> nodes) {
		logger.println("Node size: " + nodes.size());
		for (Node n : nodes.values()) {
			for (Edge e : n.edges.values())
				e.betweennessScore = 0;
		}

		Edge maxEdge = new Edge(null, null, null);
		// maxEdge.clusteringInfo.betweennessScore = -1;
		maxEdge.betweennessScore = -1;
		long milis = System.currentTimeMillis();
		for (Node source : nodes.values())
			for (Node dest : nodes.values())
				if (source.id.compareTo(dest.id) < 0 && Math.random() < Math.pow((double) constants.CLUSTER_NODE_SIZE_MAX / nodes.size(), 2)) {
					for (Node nn : nodes.values())
						nn.visited = false;
					maxEdge = BFS(source, dest, maxEdge);
				}
		new WriteConsole("hooooy: " + (System.currentTimeMillis() - milis) / 1000 + "\n");
		return maxEdge;
	}

	public Edge BFS(Node source, Node dest, Edge maxEdge) {
		ArrayList<Node> q = new ArrayList<Node>();
		q.add(source);
		source.visited = true;
		while (q.size() > 0) {
			Node n = q.remove(0);
			for (Edge e : n.edges.values()) {
				Node n2 = e.opposit(n);
				if (n2.equals(dest)) {
					e.betweennessScore++;
					if (e.compareBetweenness(maxEdge) > 0)
						maxEdge = e;
					return maxEdge;
				}
				if (!n2.visited) {
					n2.visited = true;
					e.betweennessScore++;
					if (e.compareBetweenness(maxEdge) > 0)
						maxEdge = e;
					q.add(n2);
				}
			}
		}
		return maxEdge;
	}

	/**
	 * Prints nodes and edges in two text files.
	 * 
	 * @param nodes	list of nodes of graph
	 * @param DF	the document frequency of documents'keywords
	 */
	public void printGraph(HashMap<String, Node> nodes, HashMap<String, Double> DF) {
		try {
			DataOutputStream nout = new DataOutputStream(new FileOutputStream("/tmp/keygraph_output/node.txt"));
			DataOutputStream eout = new DataOutputStream(new FileOutputStream("/tmp/keygraph_output/edge.txt"));
			printGraph(nodes, nout, eout, DF);
			nout.close();
			eout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void printGraph(ArrayList<HashMap<String, Node>> communities, HashMap<String, Double> DF) {
		try {
			DataOutputStream nout = new DataOutputStream(new FileOutputStream("/tmp/keygraph_output/node.txt"));
			DataOutputStream eout = new DataOutputStream(new FileOutputStream("/tmp/keygraph_output/edge.txt"));
			for (HashMap<String, Node> nodes : communities)
				printGraph(nodes, nout, eout, DF);
			nout.close();
			eout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Prints nodes and edges in two text files.
	 * 
	 * @param nodes		nodes of the co-occurrence graph
	 * @param nout		file in which nodes'information are printed
	 * @param eout		file in which edges'information are printed
	 * @param DF		the document frequency of documents'keywords
	 * @throws IOException
	 */
	private void printGraph(HashMap<String, Node> nodes, DataOutputStream nout, DataOutputStream eout, HashMap<String, Double> DF) throws IOException {
		nout.writeBytes("Id\tLabel\tTF\tDF\tDocuments\n");
		eout.writeBytes("Source\tTarget\tType\tWeight\n");
		for (Node n : nodes.values()) {
			nout.writeBytes(n.id + "\t" + (n.keyword.word).trim() + "\t" + n.keyword.tf + "\t" + DF.get(n.keyword.baseForm) + "\t" + n.keyword.documents.keySet() + "\n");
			for (Edge e : n.edges.values())
				if (e.n1.equals(n))
					eout.writeBytes(e.n1.id + "\t" + e.n2.id + "\tUndirected\t" + e.df * 1.0 + "\n");
		}
	}

	public static HashMap<String, Node> mergeKeyGraphs(HashMap<String, Node> kg1, HashMap<String, Node> kg2) {
		HashMap<String, Node> kg = new HashMap<String, Node>();
		for (Node n : kg1.values())
			kg.put(n.keyword.baseForm, new Node(n.keyword));
		for (Node n : kg2.values())
			kg.put(n.keyword.baseForm, new Node(n.keyword));
		for (Node n : kg1.values())
			for (Edge e : n.edges.values())
				if (n.keyword.baseForm.compareTo(e.opposit(n).keyword.baseForm) == -1) {
					if (kg.get(e.n1.keyword.baseForm) == null || kg.get(e.n2.keyword.baseForm) == null)
						// if(e.id.equals("conserv talker_differ point"))
						new WriteConsole("");
					if (!kg.get(e.n1.keyword.baseForm).edges.containsKey(e.id)) {
						Node n1 = kg.get(e.n1.keyword.baseForm);
						Node n2 = kg.get(e.n2.keyword.baseForm);
						Edge ee = new Edge(n1, n2, e.id);
						n1.edges.put(ee.id, ee);
						n2.edges.put(ee.id, ee);
					}
				}
		for (Node n : kg2.values())
			for (Edge e : n.edges.values())
				if (n.keyword.baseForm.compareTo(e.opposit(n).keyword.baseForm) == -1) {
					if (!kg.get(e.n1.keyword.baseForm).edges.containsKey(e.id)) {
						Node n1 = kg.get(e.n1.keyword.baseForm);
						Node n2 = kg.get(e.n2.keyword.baseForm);
						Edge ee = new Edge(n1, n2, e.id);
						n1.edges.put(ee.id, ee);
						n2.edges.put(ee.id, ee);
					}
				}

		return kg;
	}

	/**
	 * Adds to the graph nodes and edges between 
	 * the words of the documents sharing at least 
	 * a link URL, if necessary.
	 * 
	 * @param docs			the documents to classify
	 * @param graphNodes	co-occurrence graph
	 * @param DF			the document frequency of documents'keywords
	 */
	public void buildGraphLink(HashMap<String, Document> docs, HashMap<String, Node> graphNodes, HashMap<String, Double> DF) {
		for (Document d1 : docs.values())
			for(Document d2: docs.values()){
				int count = 0;
				for(int i=0; i<d1.link.size(); i++)
					for(int j=0; j<d2.link.size(); j++)
						if(d1.link.get(i).equals(d2.link.get(j)) && !d1.id.equals(d2.id) && count==0){
							count++;
							for(Keyword k1: d1.keywords.values()){
								Node n = null;
								if (graphNodes.containsKey(k1.baseForm)){
									n = graphNodes.get(k1.baseForm);
									if(n.keyword.documents.get(d1.id)==null){
										n.keyword.documents.put(d1.id, d1);
										n.keyword.tf+=k1.tf;
									}
								}
								else {
									Keyword keyword = new Keyword(k1.baseForm, k1.word, k1.tf, DF.get(k1.baseForm), 0);
									n = new Node(keyword);
									graphNodes.put(keyword.baseForm, n);
									n.keyword.documents.put(d1.id, d1);
								}
							}
							for(Keyword k2: d2.keywords.values()){
								Node n = null;
								if (graphNodes.containsKey(k2.baseForm)){
									n = graphNodes.get(k2.baseForm);
									if(n.keyword.documents.get(d2.id)==null){
										n.keyword.documents.put(d2.id, d2);
										n.keyword.tf+=k2.tf;
									}
								}
								else {
									Keyword keyword = new Keyword(k2.baseForm, k2.word, k2.tf, DF.get(k2.baseForm), 0);
									n = new Node(keyword);
									graphNodes.put(keyword.baseForm, n);
									n.keyword.documents.put(d2.id, d2);
								}
							}
							
							/* Print keywords ordered by term frequency in a file before filtering. */
							TreeMap<Double, String> sortedTfMap = new TreeMap<Double, String>();
							for(Node n: graphNodes.values())
								sortedTfMap.put(n.keyword.tf, n.keyword.word);
							File output = new File("/tmp/keygraph_output/keywordsSortedByTf_link.txt");
					    	try {
								output.createNewFile();
								DataOutputStream keywordsout = new DataOutputStream(new FileOutputStream(output));
								Set set = sortedTfMap.entrySet();
								Iterator i1 = set.iterator();
								while(i1.hasNext()) {
								      Map.Entry me = (Map.Entry)i1.next();
								      keywordsout.writeBytes(me.getKey() + ": " + me.getValue().toString() + "\n");
								    }
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
					    	
							/* Print keywords ordered by document frequency in a file before filtering. */
					    	TreeMap<Double, String> sortedDfMap = new TreeMap<Double, String>();
							for(Node n: graphNodes.values())
								sortedDfMap.put(DF.get(n.keyword.baseForm), n.keyword.word);
							File output1 = new File("/tmp/keygraph_output/keywordsSortedByDf_link.txt");
					    	try {
								output1.createNewFile();
								DataOutputStream keywordsout1 = new DataOutputStream(new FileOutputStream(output1));
								Set set = sortedDfMap.entrySet();
								Iterator i1 = set.iterator();
								while(i1.hasNext()) {
								      Map.Entry me = (Map.Entry)i1.next();
								      keywordsout1.writeBytes(me.getKey() + ": " + me.getValue().toString() + "\n");
								    }
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							
							// -- filter nodes ----------
//							ArrayList<String> toRemoveIds = new ArrayList<String>();
//							for (Node n : graphNodes.values())
//								if (n.keyword.tf < constants.NODE_DF_MIN || DF.get(n.keyword.baseForm) > constants.NODE_DF_MAX * documents.size())
//									toRemoveIds.add(n.keyword.baseForm);
//							for (String baseForm : toRemoveIds)
//								graphNodes.remove(baseForm);
//							toRemoveIds.clear();
							
							//creat edges between the nodes of documents sharing at least a link URL
							for (Keyword k1 : d1.keywords.values())
								if (graphNodes.containsKey(k1.baseForm)) {
									Node n1 = graphNodes.get(k1.baseForm);
									for (Keyword k2 : d2.keywords.values()) {
										if (graphNodes.containsKey(k2.baseForm) && k1.baseForm.compareTo(k2.baseForm) == -1) {
											Node n2 = graphNodes.get(k2.baseForm);
											String edgeId = Edge.getId(n1, n2);
											if (!n1.edges.containsKey(edgeId)) {
												Edge e = new Edge(n1, n2, edgeId);
												n1.edges.put(edgeId, e);
												n2.edges.put(edgeId, e);
												n1.edges.get(edgeId).df++;
												e.computeCPsLink();
											}
											else {
												Edge e1 = n1.edges.get(edgeId);
												// n1.edges.get(edgeId).df-=1;
												e1.computeCPsLink();
												Edge e2 = n2.edges.get(edgeId);
												// n2.edges.get(edgeId).df-=1;
												e2.computeCPsLink();
											}
											// n1.edges.get(edgeId).df++;
										}
									}
								}
							
							// -- filter edges ---------
//							ArrayList<Edge> toRemove = new ArrayList<Edge>();
//							for (Node n : graphNodes.values()) {
//								for (Edge e : n.edges.values()) {
//									Double MI = e.df / (e.n1.keyword.df + e.n2.keyword.df - e.df);
//									if (e.df < constants.EDGE_DF_MIN || MI < constants.EDGE_CORRELATION_MIN) {
//										toRemove.add(e);
//									} else
//										e.computeCPs();
//
//								}
//								for (Edge e : toRemove) {
//									e.n1.edges.remove(e.id);
//									e.n2.edges.remove(e.id);
//								}
//								toRemove.clear();
//							}
							
							// -- postfilter nodes ----------
//							for (Node n : graphNodes.values())
//								if (n.edges.size() == 0)
//									toRemoveIds.add(n.keyword.baseForm);
//							for (String baseForm : toRemoveIds)
//								graphNodes.remove(baseForm);
//							toRemoveIds.clear();
							
						}
			}
		
	}

}
