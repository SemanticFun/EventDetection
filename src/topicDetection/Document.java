package topicDetection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Document {
	public boolean isDuplicate=false;
	private String title, body;
	public List<String> mention = new ArrayList<>();
	public List<String> url = new ArrayList<>();
	public List<String> link = new ArrayList<>();
	public HashSet<String> topics = new HashSet<String>();
	public String id;
	public String date;
	public Timestamp publishDate = null;
	public String channel, author;
	public double vectorSize;
	public HashMap<String, Keyword> keywords = new HashMap<String, Keyword>();
	public HashMap<String, Hashtag> hashtag = new HashMap<String, Hashtag>();

	public Document() {
		
	}
	
	public Document(String id) {
		this.id = id;
		// TODO: compute vectorSize
	}
	
	/**
	 * Constructor
	 * 
	 * @param id		identification number of the document
	 * @param channel	channel of publication
	 * @param author	newspaper publishing the news
	 * @param date		date of publication
	 */
	public Document(String id, String channel, String author, String date) {
		this.id = id;
		this.channel = channel;
		this.author = author;
		this.date = date;
		// TODO: compute vectorSize
	}

	public Document(String id, String title, String body, HashMap<String, Keyword> keywords) {
		this.id = id;
//		this.title = title;
//		this.body = body;
		this.keywords = keywords;
		// vectorSize = 0;
		// for (Keyword k : keywords.values())
		// vectorSize += k.tf * k.tf;
		// vectorSize = Math.sqrt(vectorSize);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
//		this.title = title;
	}
	
	public String getID(){
		return id;
	}

	public String getBody() {
		return body;
	}

	/**
	 * Set the text of the news.
	 * 
	 * @param body	text of the news
	 */
	public void setBody(String body) {
		this.body = body;
	}

	public HashSet<String> getTopics() {
		return topics;
	}

	public void setTopics(HashSet<String> topics) {
		this.topics = topics;
	}

	public Timestamp getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Timestamp publishDate) {
		this.publishDate = publishDate;
	}

	

}
