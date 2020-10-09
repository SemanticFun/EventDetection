package topicDetection;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to evaluate the algorithm.
 * 
 * @author Federica
 *
 */

public class RateAlgorithm {

	/**
	 * Constructor
	 */
	public RateAlgorithm() {
		
	}
	
	/**
	 * In each document cluster, search the documents
	 * that have one or no keyword in common with
	 * the nodes of the community that have generated the cluster.
	 * The found documents are the possible "false positives",
	 * that is documents incorrectly inserted in the cluster.
	 * 
	 * @param clusters	a list of clusters of documents
	 */
	public void rateFP(ArrayList<DocumentCluster> clusters) {
		for(DocumentCluster dc: clusters){
			for(Document d: dc.docs.values()){
				int keyInCommon = 0;
				for(Node n: dc.keyGraph.values())
					for(Keyword k: d.keywords.values()){
						if(k.baseForm.equals(n.keyword.baseForm)){
							keyInCommon++;
						}
					}
				if(keyInCommon < 2){
					// System.out.println("Il documento " + d.id + " non deve far parte del cluster n." + dc.id);
					dc.FPdocs.put(d.id, d);
				}
			}
		}
	}

	/**
	 * In each document cluster, search the documents
	 * that have at least two keywords in common with
	 * the nodes of the community that have generated the cluster.
	 * 
	 * @param docs	the documents loaded at the beginning
	 * @param clusters	the cluster of documents created by the algorithm
	 */
	public void rateFNdc(HashMap<String, Document> docs, ArrayList<DocumentCluster> clusters) {
		for(Document d: docs.values())
			for(DocumentCluster dc: clusters){
				int keyInCommon = 0;
				for(Node n: dc.keyGraph.values())
					for(Keyword k: d.keywords.values()){
						if(k.baseForm.equals(n.keyword.baseForm)){
							keyInCommon++;
						}
					}
				
				if(keyInCommon >= 2){
					if(!dc.docs.containsKey(d.id))
					// System.out.println("Il documento " + d.id + " deve far parte del cluster n." + dc.id);
					dc.FNdocs.put(d.id, d);
				}
			}
	}

	/**
	 * Add other documents to possible false negatives.
	 * These other documents are detected considering the 
	 * set of the keywords of each document in the cluster.
	 * A document (between those loaded at the beginning) 
	 * is a possible false negative if it has a minimum 
	 * number of keywords in common with the set mentioned above.
	 * The minimum number is:
	 * - 5 if the news is published on Facebook or web site;
	 * - 3 if the news is published on Twitter.
	 * 
	 * @param docs	the documents loaded at the beginning
	 * @param clusters	the cluster of documents created by the algorithm
	 */
	public void rateFNdocs(HashMap<String, Document> docs, ArrayList<DocumentCluster> clusters) {
		for(Document d: docs.values())
			for(DocumentCluster dc: clusters){
				int keyInCommon = 0;
				for(Keyword k1: dc.keywords.values())
					for(Keyword k2: d.keywords.values()){
						if(k2.baseForm.equals(k1.baseForm)){
							keyInCommon++;
						}
					}
				
				if(d.channel.equals("facebook")){
					if(keyInCommon >= 5){
						if(!dc.docs.containsKey(d.id))
						// System.out.println("Il documento " + d.id + " deve far parte del cluster n." + dc.id);
						dc.FNdocs.put(d.id, d);
					}
				}
				
				if(d.channel.equals("twitter")){
					if(keyInCommon >= 3){
						if(!dc.docs.containsKey(d.id))
						// System.out.println("Il documento " + d.id + " deve far parte del cluster n." + dc.id);
						dc.FNdocs.put(d.id, d);
					}
				}
				
				if(d.channel.equals("sito")){
					if(keyInCommon >= 5){
						if(!dc.docs.containsKey(d.id))
						// System.out.println("Il documento " + d.id + " deve far parte del cluster n." + dc.id);
						dc.FNdocs.put(d.id, d);
					}
				}
			}
	}
	
	/**
	 * Compute the number of true negative documents,
	 * that are the documents correctly not in the cluster.
	 * The number of true negatives is calculated as 
	 * the difference between the number of not clustered 
	 * documents and the number of the possible false negatives.
	 * 
	 * @param docs	the documents loaded at the beginning
	 * @param clusters	the cluster of documents created by the algorithm
	 * @param countFN	number of possible false negative documents found by "rateFNdc" and "rateFNdocs" methods
	 * @return	number of possible true negative documents
	 */
	public int rateTN(HashMap<String, Document> docs, ArrayList<DocumentCluster> clusters, int countFN) {
		int countTN = 0;
		for(Document d1: docs.values()){
			int clustered = 0;
			//int notClustered = 0;
			
			for(DocumentCluster dc: clusters){
				if(dc.docs.containsKey(d1.id))
//				for(Document d2: dc.docs.values()){
//					if(d1.id.equals(d2.id))
						clustered++;
					//else notClustered++;
//				}
			}
			if(clustered == 0) countTN++;
		}
		countTN-= countFN;
		return countTN;
	}
	
	/**
	 * Compute the number of documents clustered by
	 * the algorithm: if a document has been inserted
	 * in more than one cluster, it is counted only once.
	 * 
	 * @param clusters	cluster of documents created by the algorithm
	 * @param clusteredDocuments	empty hashmap in which the clustered documents are added
	 * @return	number of documents clustered by the algorithm
	 */
	public int nClustered(ArrayList<DocumentCluster> clusters, HashMap<String, Document> clusteredDocuments) {
		for(DocumentCluster dc: clusters)
			for(Document d: dc.docs.values())
				if(!clusteredDocuments.containsKey(d.id)){
					clusteredDocuments.put(d.id, d);
				}
		return clusteredDocuments.size();
	}
	
}
