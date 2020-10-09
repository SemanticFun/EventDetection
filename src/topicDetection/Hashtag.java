package topicDetection;

import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Federica
 *
 */
public class Hashtag {
	
	public String baseForm;
	public List<String> words;
	public double tf;
	public double df;
	
	public HashMap<String, Document> documents = new HashMap<String, Document>();

	public Hashtag(String base, List<String> words, double tf, double df, double idf) {
		this.baseForm = base;
		this.words = words;
		this.tf = tf;
		this.df = df;
	}

//	public String getWordHashtag() {
//		return word;
//	}

	public void setWordHashtag(String word) {
		//this.word = word;
	}

}
