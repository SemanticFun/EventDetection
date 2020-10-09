package topicDetection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.json.JSONException;

public class DocumentAnalyze {
	Constants constants;

	/**
	 * Constructor
	 * 
	 * @param cons	configuration parameters
	 */
	public DocumentAnalyze(Constants cons) {
		constants = cons;
	}

	public ArrayList<DocumentCluster> clusterByLDA(HashMap<String, Document> docs, HashMap<String, Double> DF, String model) {
		ArrayList<DocumentCluster> documentClusters = new ArrayList<DocumentCluster>();
		try {

			HashMap<Integer, DocumentCluster> clusters = new HashMap<Integer, DocumentCluster>();

			BufferedReader in = new BufferedReader(new FileReader("results-" + model + ".txt"));
			in.readLine();
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split(" ");
				String docid = tokens[1];
				if (docid.lastIndexOf("/") != -1)
					docid = docid.substring(docid.lastIndexOf("/") + 1);
				docid = docid.substring(0, docid.lastIndexOf("."));
				Integer clusterid = Integer.parseInt(tokens[2]);
				Document d = docs.get(docid);
				if (d == null) {
					d = new Document(docid);
					docs.put(d.id, d);
				}
				// System.out.println(docid + "\t" + d + "\t" + clusterid);
				new WriteConsole(tokens[3] + "\t");
				if (!clusters.containsKey(clusterid))
					clusters.put(clusterid, new DocumentCluster());
				clusters.get(clusterid).docs.put(d.id, d);
			}
			in.close();
			new WriteConsole("\n");
			if (model.startsWith("mallet")) {
				in = new BufferedReader(new FileReader("results-" + model + ".topickeys.txt"));
				// in.readLine();
				line = null;
				while ((line = in.readLine()) != null) {
					String[] tokens = line.split("\t");
					DocumentCluster dc = clusters.get(Integer.parseInt(tokens[0]));
					dc.keyGraph = new HashMap<String, Node>();
					for (String keyword : tokens[2].split(" "))
						dc.keyGraph.put(keyword, new Node(new Keyword(keyword, keyword, 0, 0, 0)));
				}
				in.close();
			}
			for (Integer clusterid : clusters.keySet())
				documentClusters.add(clusters.get(clusterid));

		} catch (Exception e) {
			e.printStackTrace();
		}
		new WriteConsole("Document Clusters (final) :::::::::" + documentClusters.size() + "\n");

		return documentClusters;
	}

	/**
	 * Creates clusters of documents about the same topic.
	 * 
	 * @param docs		the documents to classify
	 * @param DF		the document frequency of documents'keywords
	 * @param version	the version of algorithm specified in the arguments
	 * @return			the document clusters created
	 */
	@SuppressWarnings("unused")
	public ArrayList<DocumentCluster> clusterbyKeyGraph(HashMap<String, Document> docs, HashMap<String, Double> DF, int version) {
		GraphAnalyze g = new GraphAnalyze(constants);
		HashMap<String, Node> graphNodes = g.buildGraph(docs, DF, constants.REMOVE_DUPLICATES);
		if(version==2 || version==3)
			g.buildGraphLink(docs, graphNodes, DF); //create edges between keywords of documents sharing at least a link url
		
		HashMap<String, Document> filteredDocByKeywords = new HashMap<String, Document>(); //docs remained after graph filtering
		for (Node n : graphNodes.values()){
			for(Document d: n.keyword.documents.values())
				if(!filteredDocByKeywords.containsKey(d.id))
					filteredDocByKeywords.put(d.id, d);
		}
		new WriteConsole(filteredDocByKeywords.size() + " documents REMAINED after nodes and edges filtering.\n" + 
				(docs.size() - filteredDocByKeywords.size()) + " documents DELETED after nodes and edges filtering.\n");
		
		File output2 = new File("/tmp/keygraph_output/documentsRemained(NodeAndEdgesFiltering).txt");
    	try {
			output2.createNewFile();
			DataOutputStream docsRemained = new DataOutputStream(new FileOutputStream(output2));
			for(Document d: filteredDocByKeywords.values()){
				docsRemained.writeBytes(d.id + " " + d.getBody() + "\n");
			}
			docsRemained.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	File output3 = new File("/tmp/keygraph_output/documentsDeleted(NodeAndEdgesFiltering).txt");
    	try {
			output3.createNewFile();
			DataOutputStream docsDeleted = new DataOutputStream(new FileOutputStream(output3));
			for(Document d: docs.values()){
				if(!filteredDocByKeywords.containsKey(d.id))
					docsDeleted.writeBytes(d.id + " " + d.getBody() + "\n");
			}
			docsDeleted.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
    	new WriteConsole("N° nodes: " + g.graphNodes.size() + "\n");
		int nedges = 0;
		for(Node n: g.graphNodes.values()){
			for(Edge e: n.edges.values())
				nedges+=1;
		}
		nedges/=2;
		new WriteConsole("N° edges: " + nedges + "\n");
		g.printGraph(g.graphNodes, DF); //create file edge.txt and node.txt
		ComputeDocumentVectorSize(docs, DF, g.graphNodes);
		ArrayList<HashMap<String, Node>> communities = g.extractCommunities(g.graphNodes);
		/* Detection of documents that have at least one keyword in at least one community. */
		HashMap<String, Document> docInCommunities = new HashMap<String, Document>();
		for(int i=0; i<communities.size(); i++){
			HashMap<String, Node> temp = new HashMap<String, Node>();
			temp = communities.get(i);
			for(Node n: temp.values())
				for(Document d: n.keyword.documents.values())
					if(!docInCommunities.containsKey(d.id))
						docInCommunities.put(d.id, d);
		}
		
		/* Check that all the remaining documents are among the documents left after nodes and edges filtering. */
		int control_count=0;
		for(Document d: docInCommunities.values()){
			if(!filteredDocByKeywords.containsKey(d.id))
				control_count++;
		}
		if(control_count!=0)
			new WriteConsole(control_count + " documents that have at least one keyword in "
				+ "at least one community are among deleted documents after nodes and edges filtering.\n");
		
		new WriteConsole(
				docInCommunities.size() + " documents are in the extracted communities.\n" + 
				(filteredDocByKeywords.size() - docInCommunities.size()) + " documents are not in the extracted communities.\n" + 
				"Communities size after extractCommunities: " + communities.size() + "\n");
		
		return extractClustersFromKeyCommunity(docs, communities, DF, docs.size(), g.graphNodes, version);
	}

	/**
	 * Compute the vector size of each document.
	 * 
	 * @param docs			documents to analyze
	 * @param DF			the document frequency of documents'keywords
	 * @param graphNodes	the co-occurrence graph
	 */
	public void ComputeDocumentVectorSize(HashMap<String, Document> docs, HashMap<String, Double> DF, HashMap<String, Node> graphNodes) {
		// ArrayList<String> toRemove=new ArrayList<String>();
		for (Document d : docs.values()) {
			d.vectorSize = 0;
			for (Keyword k : d.keywords.values())
				if (graphNodes.containsKey(k.baseForm))
					d.vectorSize += Math.pow(TFIDF(k.tf, idf(DF.get(k.baseForm), docs.size())), 2);
			d.vectorSize = Math.sqrt(d.vectorSize);

			// if(d.vectorSize==0)
			// toRemove.add(d.id);
		}

		// for(String id: toRemove)
		// docs.remove(id);
	}

	/**
	 * Compute the tf-idf function.
	 * 
	 * @param tf	term frequency
	 * @param idf	inverse document frequency
	 * @return		product between tf and idf
	 */
	public double TFIDF(double tf, double idf) {
		if (tf == 0 || idf == 0)
			return 0;
		return tf * idf;
	}

	/**
	 * Groups the similar documents to the same community
	 * in a document cluster.
	 * 
	 * @param docs			documents to be analyzed
	 * @param communities	groups of nodes more connected to each other
	 * @param DF			the document frequency of documents'keywords
	 * @param docSize		how many documents you have to analyze
	 * @param graphNodes	the co-occurrence graph
	 * @param version		the version of the algorithm specified among the arguments
	 * @return				the cluster of documents
	 */
	public ArrayList<DocumentCluster> extractClustersFromKeyCommunity(HashMap<String, Document> docs, ArrayList<HashMap<String, Node>> communities,
			HashMap<String, Double> DF, int docSize, HashMap<String, Node> graphNodes, int version) {

		ArrayList<DocumentCluster> documentClusters = new ArrayList<DocumentCluster>();
		for (HashMap<String, Node> c : communities) {
			DocumentCluster dc = new DocumentCluster();
			// -- find related documents -----------
			dc.keyGraph = c;
			for (Node n : c.values())
				for (Document d : n.keyword.documents.values())
					if (!dc.docs.containsKey(d.id)) {
						double cosineSimilarity = cosineSimilarity(c, d, DF, docSize);
						if (cosineSimilarity > constants.DOC_SIM2KEYGRAPH_MIN) {
							dc.docs.put(d.id, d);
							dc.similarities.put(d.id, cosineSimilarity);
						}
					}

			// -- filter clusters -------------
			// dc.variance = variance(dc, DF, docSize, graphNodes);
			// if (dc.centroid.vectorSize == 0 || dc.variance <=
			// constants.CLUSTER_VAR_MAX)
			{
				//ArrayList<String> toRemove = new ArrayList<String>();
				// System.out.println("\n****** Community #" +
				// documentClusters.size());
				// printKeywords(dc);
				// for (Document d : dc.docs.values()) {
				// if (cosineSimilarity(dc.centroid, d, DF, docSize) <
				// constants.DOC_SIM2CENTROID_MIN)
				// toRemove.add(d.id);
				// // else
				// // System.out.println(d.topic + ": " + d.id);
				// }
				// -- time based filtering -----------
				// if (dc.docs.size() > 0){
				// DocumentCluster[] dcs = filterTimeBased(dc, toRemove);
				// // if(dcs[0].docs.size() >= constants.TOPIC_MIN_SIZE)
				// // documentClusters.add(dcs[0]);
				// // if(dcs[1].docs.size() >= constants.TOPIC_MIN_SIZE)
				// // documentClusters.add(dcs[1]);
				// }
				//if (dc.docs.size() - toRemove.size() >= constants.TOPIC_MIN_SIZE) {
				if (dc.docs.size() >= constants.TOPIC_MIN_SIZE) {
					documentClusters.add(dc);
					//for (String id : toRemove) {
						//dc.docs.remove(id);
						//dc.similarities.remove(id);
					//}
				}

			}
		}

		new WriteConsole("Document Clusters (after extractClustersFromKeyCommunity) :::" + documentClusters.size() + "\n");
		for(int i=0; i<documentClusters.size(); i++)
			new WriteConsole("DocumentCluster n." + documentClusters.get(i).id + " 	" + documentClusters.get(i).docs.keySet() + "\n");
		
		new WriteConsole("--- MERGE SIMILAR CLUSTERS ---\n");
		mergeSimilarClusters(documentClusters);
		new WriteConsole("Document Clusters (after mergeSimilarClusters) :::" + documentClusters.size() + "\n");
		for(int i=0; i<documentClusters.size(); i++)
			new WriteConsole("DocumentCluster n." + documentClusters.get(i).id + " 	" + documentClusters.get(i).docs.keySet() + "\n");
		
		/* Link control: in most cases documents sharing at least a link url
		 * are about the same topic, then they must be part of the same cluster. */
		new WriteConsole("--- LINK CONTROL ---\n");
		int linkedDocs = 0; // documents sharing link url but not in the same cluster
		ArrayList<DocumentCluster> documentClustersLink = new ArrayList<DocumentCluster>();
		for(DocumentCluster dc: documentClusters){
			linkedDocs = checkLinkIntoDC(dc, docs, linkedDocs, documentClustersLink, DF, docSize, version);
		}
		if(version==1 || version==2)
			new WriteConsole("Documents sharing link url but not in the same cluster: " + linkedDocs + "\n");
		if(version==3){
			new WriteConsole("Document Clusters (after link control) :::" + documentClustersLink.size() + "\n");
			mergeSimilarClustersLink(documentClustersLink);
			new WriteConsole("Document Clusters (after link control and merge) :::" + documentClustersLink.size() + "\n");
			for(int i=0; i<documentClustersLink.size(); i++)
				new WriteConsole("DocumentCluster n." + documentClustersLink.get(i).id + " 	" + documentClustersLink.get(i).docs.keySet() + "\n");
			documentClusters = documentClustersLink;
		}
		
		if (constants.HARD_CLUSTERING){
			new WriteConsole("--- HARD CLUSTERING ---\n");
			hardClustering(docs, DF, docSize, documentClusters);
			new WriteConsole("Document Clusters (after hard clustering) :::" + documentClusters.size() + "\n");
		}

		return documentClusters;
	}
	
	/**
	 * Check how many documents sharing at least a link url are not in the same cluster.
	 * If you are using the version 3, the documents are added.
	 * 
	 * @param dc					a document cluster
	 * @param docs					documents to be analyzed
	 * @param linkedDocs			how many documents sharing at least a link url are not in the same cluster, initialized to 0
	 * @param documentClustersLink	clusters of documents sharing at least a link url
	 * @param DF					the document frequency of documents'keywords
	 * @param docSize				how many documents you have to analyze
	 * @param version				the version of algorithm you are using
	 * @return						how many documents sharing at least a link url are not in the same cluster
	 */
	private int checkLinkIntoDC(DocumentCluster dc, HashMap<String, Document> docs, int linkedDocs, ArrayList<DocumentCluster> documentClustersLink, 
			HashMap<String, Double> DF, int docSize, int version) {
		ArrayList<Document> dtemp = new ArrayList<Document>();
		
		for(Document d1 : dc.docs.values())
			for(Document d2: docs.values()){
				int count = 0;
				for(int i=0; i<d1.link.size(); i++)
					for(int j=0; j<d2.link.size(); j++)
						if(d1.link.get(i).equals(d2.link.get(j)) && !d1.id.equals(d2.id) && count==0){
							// news about the same topic
							count++;
							if(!dc.docs.containsKey(d2.id))
								if(!dtemp.contains(d2))
									dtemp.add(d2);
						}
			}
		
		for(int i=0; i<dtemp.size(); i++)
			if(!dc.docs.containsKey(dtemp.get(i).id)){
				linkedDocs++;
				if(version==1 || version==2)
					new WriteConsole("False Negative document " + dtemp.get(i).id + " in cluster " + dc.id + "\n");
				if(version==3){
					dc.docs.put(dtemp.get(i).id, dtemp.get(i));
					for(Keyword k: dtemp.get(i).keywords.values())
						dc.keywords.put(k.baseForm, k);
					//HashMap<String, Node> keyGraph = new HashMap<String, Node>();
					//keyGraph = dc.keyGraph;
					double cosineSimilarity = cosineSimilarity(dc.keyGraph, dtemp.get(i), DF, docSize);
					dc.similarities.put(dtemp.get(i).id, cosineSimilarity);
					new WriteConsole("Inserito documento " + dtemp.get(i).id + " in cluster " + dc.id + "\n");
				}
				
			}
		documentClustersLink.add(dc);
		return linkedDocs;
	}

	public ArrayList<DocumentCluster> extractClustersFromKeyCommunity2(HashMap<String, Document> docs, ArrayList<HashMap<String, Node>> communities,
			HashMap<String, Double> DF, int docSize, HashMap<String, Node> graphNodes) {

		ArrayList<DocumentCluster> tmpdocumentClusters = new ArrayList<DocumentCluster>();
		ArrayList<DocumentCluster> documentClusters = new ArrayList<DocumentCluster>();

		for (HashMap<String, Node> c : communities) {
			DocumentCluster dc = new DocumentCluster();
			dc.keyGraph = c;
			tmpdocumentClusters.add(dc);
		}

		for (Document d : docs.values()) {
			double maxSim = 0;
			DocumentCluster maxDC = null;
			for (DocumentCluster dc : tmpdocumentClusters) {
				// double sim = cosineSimilarity(dc.keyGraph, d, DF, docSize);
				double sim = dc.similarities.get(d.id);
				if (sim > maxSim) {
					new WriteConsole("siiiim:: " + sim +"\n");
					maxSim = sim;
					maxDC = dc;
				}
			}
			if (maxSim > constants.DOC_SIM2KEYGRAPH_MIN)
				maxDC.docs.put(d.id, d);
		}

		for (DocumentCluster dc : tmpdocumentClusters)
		// -- filter clusters -------------
		// dc.variance = variance(dc, DF, docSize, graphNodes);
		// if (dc.centroid.vectorSize == 0 || dc.variance <=
		// constants.CLUSTER_VAR_MAX)
		{
			ArrayList<String> toRemove = new ArrayList<String>();
			// System.out.println("\n****** Community #" +
			// documentClusters.size());
			// printKeywords(dc);
			// for (Document d : dc.docs.values()) {
			// if (cosineSimilarity(dc.centroid, d, DF, docSize) <
			// constants.DOC_SIM2CENTROID_MIN)
			// toRemove.add(d.id);
			// // else
			// // System.out.println(d.topic + ": " + d.id);
			// }
			// -- time based filtering -----------
			// if (dc.docs.size() > 0){
			// DocumentCluster[] dcs = filterTimeBased(dc, toRemove);
			// // if(dcs[0].docs.size() >= constants.TOPIC_MIN_SIZE)
			// // documentClusters.add(dcs[0]);
			// // if(dcs[1].docs.size() >= constants.TOPIC_MIN_SIZE)
			// // documentClusters.add(dcs[1]);
			// }
			if (dc.docs.size() - toRemove.size() >= constants.TOPIC_MIN_SIZE) {
				documentClusters.add(dc);
				for (String id : toRemove) {
					dc.docs.remove(id);
					dc.similarities.remove(id);
				}
			}
		}

		new WriteConsole("Keyword Communities :::::::::" + communities.size() + "\n");
		new WriteConsole("Document Clusters (initial) :::::::::" + documentClusters.size()+"\n");
		// printClusters(documentClusters);
		mergeSimilarClusters(documentClusters);
		// printClusters(documentClusters);

		if (constants.HARD_CLUSTERING)
			hardClustering(docs, DF, docSize, documentClusters);

		new WriteConsole("Document Clusters (final) :::::::::" + documentClusters.size()+"\n");

		return documentClusters;
	}

	public DocumentCluster[] filterTimeBased(DocumentCluster dc, ArrayList<String> toRemove) {
		long time = 0;
		HashMap<String, Document> docs = dc.docs;
		for (int i = 0; i < 5 && docs.size() > 0; i++) {
			time = 0;
			toRemove.clear();
			for (Document d : docs.values())
				time += d.publishDate.getTime();
			time /= docs.size();
			docs = new HashMap<String, Document>();
			for (Document d : dc.docs.values())
				if (Math.abs(d.publishDate.getTime() - time) > ((long) 15) * 24 * 60 * 60 * 1000)
					toRemove.add(d.id);
				else
					docs.put(d.id, d);
		}

		DocumentCluster[] dcs = new DocumentCluster[] { new DocumentCluster(), new DocumentCluster() };
		dcs[0].keyGraph = dc.keyGraph;
		dcs[1].keyGraph = dc.keyGraph;
		for (String id : toRemove) {
			Document doc = dc.docs.get(id);
			if (doc.publishDate.after(new Timestamp(time)))
				dcs[1].docs.put(id, doc);
			else
				dcs[0].docs.put(id, doc);
		}
		return dcs;
	}

	private void hardClustering(HashMap<String, Document> docs, HashMap<String, Double> DF, int docSize, ArrayList<DocumentCluster> documentClusters) {
		int ii = 0;
		for (Document d : docs.values()) {
			boolean isAssigned = false;
			for (DocumentCluster dc : documentClusters)
				if (dc.docs.containsKey(d.id)) {
					isAssigned = true;
					break;
				}
			if (!isAssigned) {
				double max_sim = 0;
				DocumentCluster bestDC = null;
				for (DocumentCluster dc : documentClusters)
					// if (cosineSimilarity(dc.keyGraph, d, DF, docSize) >
					// max_sim) {
					if (dc.similarities.containsKey(d.id) && dc.similarities.get(d.id) > max_sim) {
						max_sim = cosineSimilarity(dc.keyGraph, d, DF, docSize);
						bestDC = dc;
						new WriteConsole("BestDC " + bestDC + "\n");
					}
				if (max_sim > constants.DOC_SIM2KEYGRAPH_MIN / 3.5)
					bestDC.docs.put(d.id, d);
				else
					ii++;
			}
		}
		new WriteConsole("Off topic documents:" + ii + " out of " + docs.size() + "\n");
	}

	/**
	 * Print the result of the algorithm in a text file.
	 * For each cluster created by the algorithm, the information printed are:
	 * - the keywords that are the nodes of the community that generated the cluster,
	 * - the keywords of the documents in the cluster,
	 * - the documents of the cluster,
	 * - the false positives,
	 * - the documents that share at least a link URL with at least a document in the cluster,
	 * - the nodes of the community that generated the cluster and their edges.
	 * 
	 * @param clusters	a list of the clusters of documents found by the algorithm
	 * @param out	the name of the output file
	 * @param docs	the documents loaded at the beginning
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws ParseException 
	 * @throws JSONException 
	 * @throws IOException 
	 */
	public static void printTopics(Collection<DocumentCluster> clusters, PrintStream out, HashMap<String, Document> docs, String date_start, int days) throws ClassNotFoundException, SQLException, ParseException, IOException, JSONException {
		File dir = new File("/tmp/keygraph_output/toLoadFiles");
		dir.mkdir();
		
		//se si desidera modificare le colonne da visualizzare occorre agire anche su questo file!!
		File c = new File(dir, "columns.json");
		PrintStream fColumns = new PrintStream(c);
		fColumns.println("{\"columns\":\n"
				+ "\t[\n"
				+ "\t\"id\",\n"
				+ "\t\"date\",\n"
				+ "\t\"author\",\n"
				+ "\t\"channel\",\n"
				+ "\t\"text\"\n"
				+ "\t]\n}");
		
		fColumns.close();

		
		File f = new File(dir, "elencoCluster.json");
		PrintStream fCluster = new PrintStream(f);
		
		fCluster.println("{\n"
				+ "\t\"name\": \"clusters\",\n"
				+ "\t\"string\": \"clusters\",\n"
				+ "\t\"children\": [");
		
		int i = 0;
		for (DocumentCluster dc : clusters) {
			
			/*
			double max1 = 0, max2 = 0;
			String node1 = "", node2 = "";
			for (Node n : dc.keyGraph.values()){
				if(max1 == 0 && max2 == 0){
					max1 = n.keyword.tf;
					node1 = n.keyword.getWord().replaceAll("[,'\"]", " ");
				} else if(max2==0) {
					max2 = n.keyword.tf;
					node2 = n.keyword.getWord().replaceAll("[,'\"]", " ");
					if(max1<max2){
						double temp = max1;
						max1=max2;
						max2=temp;
					}
				}
				if(max1 != 0 && max2!=0){
					if(n.keyword.tf > max1){
						max2 = max1;
						node2 = node1;
						max1 = n.keyword.tf;
						node1 = n.keyword.getWord().replaceAll("[,'\"]", " ");
					} else if( n.keyword.tf > max2){
						max2 = n.keyword.tf;
						node2 = n.keyword.getWord().replaceAll("[,'\"]", " ");
					}
				}
				
				
				System.out.println(n.keyword.getWord().replaceAll("[,'\"]", " ") + " " + n.keyword.tf);
				
			}
			
			
			fCluster.println("\t\t{\n"
					+ "\t\t\t\"name\": \"cluster" + dc.id + "\",\n"
					+ "\t\t\t\"string\": " + node1 + "-" + node2 +",\n"
					+ "\t\t\t\"size\": " + dc.docs.size());
					
			
			
			if(i!=clusters.size()-1)
				fCluster.println("\t\t},");
			else
				fCluster.println("\t\t}");
			*/
			
			File fInt = new File(dir, "cluster"+dc.id + ".json");
			PrintStream fDocCl = new PrintStream(fInt);
			
			out.println("DOCUMENT CLUSTER n." + dc.id + ": " + dc.docs.size() + " documents");
			out.print("KEYWORDS:\t");
			fDocCl.println("{\n"
					+ "\t\"nodes\": [");
			printKeywords(dc, out, fDocCl);
			for(Keyword k: dc.keywords.values())
				out.print(k.word + ", ");
			out.print("\n");
			
			fDocCl.println("\t],\n"
					+ "\t\"documents\": [");
			out.print("DOCUMENTS:");
			int k=0;
			for (Document d : dc.docs.values()){
				out.print("\n" + d.id + " " + d.date + " " + d.channel + " " + d.author + ", " + d.getBody());
				fDocCl.println("\t\t{\n"
						+ "\t\t\t\"id\": "+ d.id + ",\n"
						+ "\t\t\t\"date\": \"" + d.date + "\",\n"
						+ "\t\t\t\"author\": \"" + d.author + "\",\n"
						+ "\t\t\t\"channel\": \"" + d.channel + "\",\n"
						+ "\t\t\t\"text\": \"" + d.getBody().replace("\"", "'") + "\"");
				if(k!=dc.docs.values().size()-1)
					fDocCl.println("\t\t},");
				else
					fDocCl.println("\t\t}");
				
				k++;
			}
			
			fDocCl.println("\t],\n"
					+ "\t\"links\": [");
					
			out.print("\n");
//			if(dc.FPdocs.size()>0){
//				out.print("RELATED DOCUMENTS (with at most one keyword in common):");
//				for(Document d: dc.FPdocs.values())
//					out.print("\n" + d.id + " " + d.date + " " + d.channel + " " + d.author + ", " + d.getBody());
//				out.print("\n");
//			}
			if(dc.FNdocs.size()>0){
				out.print("NOT RELATED DOCUMENTS (with some keywords in common):");
				for(Document d: dc.FNdocs.values())
					out.print("\n" + d.id + " " + d.date + " " + d.channel + ", " + d.getBody());
				out.print("\n");
			}
			
			out.print("NOT RELATED DOCUMENTS WITH SHARED LINK:");
			for (Document d1: dc.docs.values()){
				for (Document d2: docs.values()){
					int count = 0;
					for(int i1=0; i1<d1.link.size(); i1++)
						for(int j=0; j<d2.link.size(); j++)
							if(d1.link.get(i1).equals(d2.link.get(j)) && !d1.id.equals(d2.id) && count==0){
								//same link means news about the same topic
								count++;
								if(!dc.docs.containsKey(d2.id))
									// if(!dc.FNdocs.containsKey(d2.id))
											out.print("\n" + d2.id + " " + d2.date + " " + d2.channel + ", " + d2.getBody() + " (linked to " + d1.id + " )");
							}
				}
			}
			out.print("\n");
			out.println("KEYGRAPH_NODES: node_id:node_labelBaseForm:node_label\t");
			out.print("\t\t\t\t");
			for (Node n : dc.keyGraph.values()){
				out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.getWord().replaceAll("[,'\"]", " ") + ", ");
			}
			out.print("\n");
			out.println("KEYGRAPH_EDGES: node1_id:node1_labelBaseForm:node1_label-node2_id:node2_labelBaseForm\t");
			out.print("\t\t\t\t");

			String temp = new String("");
			double max = -1;
			String node1 = "", node2 = "";
			for (Node n : dc.keyGraph.values()) {
				for (Edge e : n.edges.values()) {
					if (e.n1.equals(n)){
						
						//System.out.println(e.cp1 + " " + e.cp2 + " " + e.df + " " + e.id +" " + e.n1.keyword.baseForm + " " +  e.n2.keyword.baseForm + " " + e.betweennessScore);
						
						if(max < e.betweennessScore){
							max = e.betweennessScore;
							node1 = e.n1.keyword.getWord().replaceAll("[,'\"]", " ");
							node2 = e.n2.keyword.getWord().replaceAll("[,'\"]", " ");
						}
						
						out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ", ");
						
						temp += "\t\t{\n"
								+ "\t\t\t\"source\": \"" + e.n1.keyword.getWord().replaceAll("[,'\"]", " ") + "\",\n"
								+ "\t\t\t\"target\": \"" + e.n2.keyword.getWord().replaceAll("[,'\"]", " ") + "\"\n"
								+ "\t\t},\n";
					}
				}
			}
			
			fCluster.println("\t\t{\n"
					+ "\t\t\t\"name\": \"cluster" + dc.id + "\",\n"
					+ "\t\t\t\"string\": \"" + node1 + "-" + node2 +"\",\n"
					+ "\t\t\t\"size\": " + dc.docs.size());
			
			if(i!=clusters.size()-1)
				fCluster.println("\t\t},");
			else
				fCluster.println("\t\t}");

			i++;
			
			k = temp.length();
			temp = temp.substring(0, k-2);
			fDocCl.println(temp);
			
			fDocCl.println("\t]\n}");
			out.println("\n\n");

		}
		fCluster.println("\t]\n}");
		
		fCluster.close();
		
		DB.storeCluster(clusters, date_start, days);

	}

	// public static void printClusters(Collection<DocumentCluster> clusters,
	// PrintStream out) {
	// // printClusters(clusters, out, false);
	// for (DocumentCluster dc : clusters)
	// dc.serialize(out);
	// }

	// public static void printClusters(Collection<DocumentCluster> clusters,
	// PrintStream out, boolean printDocContent) {
	// for (DocumentCluster dc : clusters) {
	// printCluster(dc, out, printDocContent);
	// }
	// }

	// public static void printCluster(DocumentCluster dc, PrintStream out) {
	// printCluster(dc, out, false);
	// }

	// public static void printCluster(DocumentCluster dc, PrintStream out,
	// boolean printDocContent) {
	//
	// out.println("\n****** Community #" + dc.id);
	// printKeywords(dc, out);
	// printKeyGraph(dc.keyGraph, out);
	// out.println("~" + dc.docs.size() / 10 + "0: " + dc.docs.size() +
	// " docs");
	// for (Document d : dc.docs.values())
	// // out.println(d.topics + ": " + d.publishDate + " " + d.id);
	// if (printDocContent)
	// out.println(d.publishDate + "\t" + d.id + "\t" + d.getBody());
	// else
	// out.println(d.publishDate + "\t" + d.id);
	// }

	public static void printClustersForTheWebsite(Collection<DocumentCluster> clusters, String outputFileName) throws Exception {

		PrintStream out = new PrintStream(outputFileName + ".event_document");
		for (DocumentCluster dc : clusters) {
			out.print(dc.id + "\t");
			// out.println("~" + dc.docs.size() / 10 + "0: " + dc.docs.size() +
			// " docs");
			for (Document d : dc.docs.values())
				out.print(d.id + ":" + (d.isDuplicate ? 0 : 1) + ",");
			out.println();
		}
		out.close();
		out = new PrintStream(outputFileName + ".event_keyGraph_nodes");
		for (DocumentCluster dc : clusters) {
			out.print(dc.id + "\t");
			for (Node n : dc.keyGraph.values())
				out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.getWord().replaceAll("[,'\"]", " ") + ",");
			out.println();
		}
		out.close();
		out = new PrintStream(outputFileName + ".event_keyGraph_edges");
		for (DocumentCluster dc : clusters) {
			out.print(dc.id + "\t");
			for (Node n : dc.keyGraph.values()) {
				for (Edge e : n.edges.values())
					if (e.n1.equals(n))
						out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ",");
			}
			out.println();
		}
		out.close();

		// printKeywords(dc, out);
		// printKeyGraph(dc.keyGraph, out);

	}

	/**
	 * Print in the output file the keywords that are the 
	 * nodes of the community that generated the cluster.
	 * 
	 * @param dc	a document cluster created by the algorithm
	 * @param out	the name of output file
	 */
	public static void printKeywords(DocumentCluster dc, PrintStream out, PrintStream fDocCl) {
		int i = 0;
		for (Node n : dc.keyGraph.values()){
			out.print(n.keyword.getWord().replaceAll("[,'\"]", " ") + " " + n.keyword.tf + " " + n.keyword.df + n.keyword.documents.keySet() + "\n\t\t\t");
			fDocCl.println("\t\t{\n"
					+ "\t\t\t\"id\": \"" + n.keyword.getWord().replaceAll("[,'\"]", " ") + "\",\n"
					+ "\t\t\t\"value\": " + n.keyword.tf);
			if(i!=dc.keyGraph.values().size()-1)
				fDocCl.println("\t\t},");
			else
				fDocCl.println("\t\t}");
			
			i++;
		}
	}
	
	public static void printKeywords(DocumentCluster dc, PrintStream out) {
		for (Node n : dc.keyGraph.values()){
			out.print(n.keyword.getWord().replaceAll("[,'\"]", " ") + " " + n.keyword.tf + " " + n.keyword.df + n.keyword.documents.keySet() + "\n\t\t\t");
		}
	}

	public static void printKeyGraph(HashMap<String, Node> keyGraph, PrintStream out) {
		for (Node n : keyGraph.values())
			out.print(n.id + ":" + n.keyword.baseForm + ":" + n.keyword.getWord().replaceAll("[,'\"]", " ") + ", ");
		out.println();
		for (Node n : keyGraph.values()) {
			for (Edge e : n.edges.values())
				if (e.n1.equals(n))
					out.print(e.n1.id + ":" + e.n1.keyword.baseForm + "-" + e.n2.id + ":" + e.n2.keyword.baseForm + ", ");
		}
		out.println();
	}

	/**
	 * Merge the document clusters with some documents in common.
	 * 
	 * @param documentClusters	cluster of similar documents
	 */
	private void mergeSimilarClusters(ArrayList<DocumentCluster> documentClusters) {
		ArrayList<DocumentCluster> topics = new ArrayList<DocumentCluster>();
		while (documentClusters.size() > 0) {
			DocumentCluster dc1 = documentClusters.remove(0);
			ArrayList<DocumentCluster> toRemove = new ArrayList<DocumentCluster>();
			boolean isChanged = false;
			do {
				isChanged = false;
				for (DocumentCluster dc2 : documentClusters) {
					double intersect = intersectDocs(dc1.docs, dc2.docs);
					if (intersect / Math.min(dc1.docs.size(), dc2.docs.size()) >= constants.CLUSTER_INTERSECT_MIN) {
						mergeClusters(dc1, dc2);
						isChanged = true;
						toRemove.add(dc2);
					}
				}
				documentClusters.removeAll(toRemove);
			} while (isChanged);
			dc1.keywords.clear();
			for(Document d: dc1.docs.values()){
				for(Keyword k: d.keywords.values())
					dc1.keywords.put(k.baseForm, k);
			}
			topics.add(dc1);
		}
		documentClusters.addAll(topics);
	}
	
	private void mergeSimilarClustersLink(ArrayList<DocumentCluster> documentClusters) {
		ArrayList<DocumentCluster> topics = new ArrayList<DocumentCluster>();
		while (documentClusters.size() > 0) {
			DocumentCluster dc1 = documentClusters.remove(0);
			ArrayList<DocumentCluster> toRemove = new ArrayList<DocumentCluster>();
			boolean isChanged = false;
			do {
				isChanged = false;
				for (DocumentCluster dc2 : documentClusters) {
					double intersect = intersectDocs(dc1.docs, dc2.docs);
					if (intersect / Math.min(dc1.docs.size(), dc2.docs.size()) >= 0.3) {
						mergeClusters(dc1, dc2);
						isChanged = true;
						toRemove.add(dc2);
					}
				}
				documentClusters.removeAll(toRemove);
			} while (isChanged);
			dc1.keywords.clear();
			for(Document d: dc1.docs.values()){
				for(Keyword k: d.keywords.values())
					dc1.keywords.put(k.baseForm, k);
			}
			topics.add(dc1);
		}
		documentClusters.addAll(topics);
	}

	/**
	 * Compute how many documents are in both document clusters.
	 * 
	 * @param dc1	a document cluster
	 * @param dc2	a document cluster
	 * @return		compute how many documents are in both document clusters
	 */
	public int intersectDocs(HashMap dc1, HashMap dc2) {
		int intersect = 0;
		for (Object key : dc1.keySet())
			if (dc2.containsKey(key))
				intersect++;
		return intersect;
	}

	public void mergeClusters(DocumentCluster dc1, DocumentCluster dc2) {
		for (Document d : dc2.docs.values())
			if (!dc1.docs.containsKey(d.id)) {
				dc1.docs.put(d.id, d);
				dc1.similarities.put(d.id, dc2.similarities.get(d.id));
			} else if (dc1.similarities.get(d.id) < dc2.similarities.get(d.id))
				dc1.similarities.put(d.id, dc2.similarities.get(d.id));
		dc1.keyGraph.putAll(dc2.keyGraph);
	}

	public double cosineSimilarity(Document d1, Document d2, HashMap<String, Double> DF, int docSize) {
		double sim = 0;
		for (Keyword k1 : d1.keywords.values()) {
			if (d2.keywords.containsKey(k1.baseForm)) {
				Double df = DF.get(k1.baseForm);
				double tf1 = k1.tf;
				double tf2 = d2.keywords.get(k1.baseForm).tf;
				sim += TFIDF(tf1, idf(df, docSize)) * TFIDF(tf2, idf(df, docSize));
			}
		}
		return sim / d1.vectorSize / d2.vectorSize;
	}

	/**
	 * Compute the similarity between a community and a document.
	 * 
	 * @param community	a set of nodes
	 * @param d2		a document
	 * @param DF		the document frequency of documents'keywords
	 * @param docSize	how many documents you have to analyze
	 * @return			how much the community and the document are similar
	 */
	public double cosineSimilarity(HashMap<String, Node> community, Document d2, HashMap<String, Double> DF, int docSize) {
		double sim = 0;
		double vectorSize1 = 0;
		int numberOfKeywordsInCCommon = 0;
		for (Node n : community.values()) {
			double nTF = 0;
			for (Edge e : n.edges.values())
				// nTF += e.df;
				nTF += Math.max(e.cp1, e.cp2);
			// nkeywordtf += (n.equals(e.n2)) ? e.cp1 : e.cp2;
			n.keyword.tf = nTF / n.edges.size();
			vectorSize1 += Math.pow(TFIDF(n.keyword.tf, idf(DF.get(n.keyword.baseForm), docSize)), 2);

			if (d2.keywords.containsKey(n.keyword.baseForm)) {
				numberOfKeywordsInCCommon++;
				sim += TFIDF(n.keyword.tf, idf(DF.get(n.keyword.baseForm), docSize))
						* TFIDF(d2.keywords.get(n.keyword.baseForm).tf, idf(DF.get(n.keyword.baseForm), docSize));
			}
		}
		vectorSize1 = Math.sqrt(vectorSize1);
		if (numberOfKeywordsInCCommon >= 2)
			return sim / vectorSize1 / d2.vectorSize;
		else
			return 0;
	}

	public double variance(DocumentCluster dc, HashMap<String, Double> DF, int docSize, HashMap<String, Node> graphNodes) {
		double var = 0;
		if (dc.centroid == null)
			dc.centroid = centroid(dc.docs, DF, graphNodes);
		for (Document d : dc.docs.values()) {
			double diff = 1 - cosineSimilarity(dc.centroid, d, DF, docSize);
			var += diff * diff;
		}
		return var / dc.docs.size();
	}

	public Document centroid(HashMap<String, Document> docs, HashMap<String, Double> DF, HashMap<String, Node> graphNodes) {
		Document centroid = new Document("-1");
		for (Document d : docs.values())
			for (Keyword k : d.keywords.values()) {
				// if (graphNodes.containsKey(k.baseForm))
				if (centroid.keywords.containsKey(k.baseForm)) {
					Keyword kk = centroid.keywords.get(k.baseForm);
					kk.tf += k.tf;
					kk.df++;
				} else
					centroid.keywords.put(k.baseForm, new Keyword(k.baseForm, k.getWord(), k.tf, k.df, 0));
			}
		for (Keyword k : centroid.keywords.values())
			if (idf(k.df, docs.size()) != 0) {// DF.get(k.baseForm) >
				// if (idf(DF.get(k.baseForm), 2) != 0) {// DF.get(k.baseForm) >
				// Constants.KEYWORD_DF_MIN)
				// {
				k.tf /= docs.size();
				centroid.vectorSize += Math.pow(TFIDF(k.tf, DF.get(k.baseForm)), 2);
			} else
				k.tf = 0;
		centroid.vectorSize = Math.sqrt(centroid.vectorSize);
		return centroid;
	}

	/**
	 * Compute the inverse document frequency.
	 * 
	 * @param df	the document frequency of documents'keywords
	 * @param size	how many documents you have to analyze
	 * @return		inverse document frequency
	 */
	public double idf(double df, int size) {
		// if (df < constants.SIMILARITY_KEYWORD_DF_MIN || df >
		// constants.NODE_DF_MAX * size)
		// return 0;
		return Math.log(size / df) / Math.log(2);
	}
}
