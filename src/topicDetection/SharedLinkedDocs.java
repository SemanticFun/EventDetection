package topicDetection;

import java.util.ArrayList;
import java.util.HashMap;
/**
 * 
 * @author Federica
 *
 */
public class SharedLinkedDocs {

	/**
	 * Constructor
	 */
	public SharedLinkedDocs(){
		
	}

	/**
	 * Create clusters of documents sharing at least one link URL.
	 * 
	 * @param d	a document among the documents loaded at the beginning
	 * @param docs	the documents loaded at the beginning
	 * @param documentClustersLink	empty list in which clusters of documents sharing at least a link URL are added
	 */
	public void clusterLinkedDocs(Document d, HashMap<String, Document> docs, ArrayList<DocumentClusterLink> documentClustersLink) {
		DocumentClusterLink dcL = new DocumentClusterLink();
		dcL.docs.put(d.id, d);
		for (Document d1: docs.values()){
			int count = 0;
			for(int i=0; i<d.link.size(); i++)
				for(int j=0; j<d1.link.size(); j++)
					if(d.link.get(i).equals(d1.link.get(j)) && !d.id.equals(d1.id) && count==0){
						//same link means news about the same topic
						count++;
						dcL.docs.put(d1.id, d1);
					}
		}
		
		if(dcL.docs.size()>=2)
			if(documentClustersLink.size()==0) documentClustersLink.add(dcL);
			else {
				// check if the document cluster already exists
				int equals = 0;
				boolean ins = false;
				for(DocumentClusterLink dcL1: documentClustersLink)
					if(dcL.docs.size() == dcL1.docs.size()){
						equals = 1;
						int ndocs = 0;
						for(Document dd: dcL.docs.values())
							if(dcL1.docs.containsKey(dd.id))
								ndocs++;
						if(ndocs!=dcL.docs.size()) ins = true;
					}
					else if(equals==0) ins = true;
				if(ins==true) documentClustersLink.add(dcL);
			}
	}
		
}