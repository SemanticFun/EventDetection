package topicDetection;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Cluster of documents sharing at least a link URL.
 * 
 * @author Federica
 *
 */

public class DocumentClusterLink {
	public HashMap<String, Document> docs = new HashMap<String, Document>();

	/**
	 * Constructor
	 */
	public DocumentClusterLink(){
		
	}

	/**
	 * Compute the number of documents shared between two document clusters.
	 * 
	 * @param dc1	a cluster of documents
	 * @param dc2	a cluster of documents
	 * @return	the number of documents shared between dc1 and dc2
	 */
	public static int intersectDocs(DocumentClusterLink dc1, DocumentClusterLink dc2) {
		int intersect = 0;
		for (Document d : dc1.docs.values())
			if (dc2.docs.containsKey(d.id))
				intersect++;
		return intersect;
	}

	/**
	 * Insert in the first document cluster 
	 * all the documents in the second document cluster.
	 * 
	 * @param dc1	a cluster of documents
	 * @param dc2	a cluster of documents
	 */
	public static void mergeClustersLink(DocumentClusterLink dc1, DocumentClusterLink dc2) {
		for (Document d : dc2.docs.values())
			dc1.docs.put(d.id, d);
	}
	
	/**
	 * Merge the clusters of the documents sharing at least
	 * a link URL in a single document cluster if they
	 * have at least a document in common.
	 * 
	 * @param documentClustersLink	a list of cluster of documents sharing at least a link URL.
	 */
	public static void mergeDocumentClustersLink(ArrayList<DocumentClusterLink> documentClustersLink) {
		ArrayList<DocumentClusterLink> dcLs = new ArrayList<DocumentClusterLink>();
		while (documentClustersLink.size() > 0) {
			DocumentClusterLink dc1 = documentClustersLink.remove(0);
			ArrayList<DocumentClusterLink> toRemove = new ArrayList<DocumentClusterLink>();
			boolean isChanged = false;
			do {
				isChanged = false;
				for (DocumentClusterLink dc2 : documentClustersLink) {
					double intersect = intersectDocs(dc1, dc2);
					if (intersect>=1) {	//if at least a document in common
						mergeClustersLink(dc1, dc2);
						isChanged = true;
						toRemove.add(dc2);
					}
				}
				documentClustersLink.removeAll(toRemove);
			} while (isChanged);
			dcLs.add(dc1);
		}
		documentClustersLink.addAll(dcLs);
		
	}
	
	


}
