package topicDetection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import dataset.twitter.StringDuplicate;
import edu.stanford.nlp.util.StringUtils;


public class DataLoader {

	protected Constants constants;
	protected Constants twitterConstants;
	protected Constants siteConstants;
	protected Constants facebookConstants;

	public DataLoader() {
	}

	public DataLoader(Constants constants) {
		this.constants = constants;
	}
	/**
	 * Constructor
	 * 
	 * @param twitterConstants	configuration parameters if the news is published on Twitter
	 * @param siteConstants	configuration parameters if the news is published on website
	 * @param facebookConstants	configuration parameters if the news is published on Facebook
	 */
	public DataLoader(Constants twitterConstants, Constants siteConstants, Constants facebookConstants) {
		this.twitterConstants = twitterConstants;
		this.siteConstants = siteConstants;
		this.facebookConstants = facebookConstants;
	}

	public DataInputStream openDataInputStream(String fileName) throws Exception {
		return new DataInputStream(new FileInputStream(fileName));
	}

	public boolean exists(String f) throws Exception {
		return new File(f).exists();
	}

	public String[] list(String fname) throws Exception {
		return new File(fname).list();
	}

//	public void loadDocumentsForSpinn3r(String[] files, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter)
//			throws Exception {
//		for (int i = 0; i < files.length; i++) {
//			if (i % 1000 == 0)
//				System.out.println(i);
//			// System.out.println(constants.DATA_KEY_NE_PATH + files[i]);
//			String ff = (constants.DATA_KEYWORDS_1_PATH + files[i]);// +
//			// ".txt");
//			Document d = new Document(files[i]);
//			loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, constants.KEYWORDS_1_WEIGHT);
//			if (constants.KEYWORDS_2_ENABLE) {
//				ff = (constants.DATA_KEYWORDS_2_PATH + files[i]);// +
//				// ".txt");
//				loadDocumentKeyFile(openDataInputStream(ff), stopwords, porter, d, constants.KEYWORDS_2_WEIGHT);
//			}
//			if (d.keywords.size() >= constants.DOC_KEYWORDS_SIZE_MIN) {
//				docs.put(d.id, d);
//				for (Keyword k : d.keywords.values()) {
//					if (DF.containsKey(k.baseForm))
//						DF.put(k.baseForm, DF.get(k.baseForm) + 1);
//					else
//						DF.put(k.baseForm, new Double(1));
//				}
//			}
//		}
//	}

	/**
	 * Reads and analyzes the documents contained in the input file.
	 * 
	 * @param inputFileName		input file containing the documents to analyze
	 * @param docs				hashmap in which the documents to analyze are added
	 * @param stopwords			list of stopwords
	 * @param DF				hashmap in which the keywords of the documents are added with their document frequency
	 * @param porter			the stemmer
	 * @param removeDuplicates	boolean specified in the configuration parameters
	 * @param docout			file "docs.txt" in which the information about documents are printed
	 * @param remove			file "remove.txt" in which hashtag, mention and url to remove are printed
	 * @param linkFileName		input file containing the links 
	 * @throws Exception
	 */
	public void loadDocuments(String date_end, int days, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			boolean removeDuplicates, DataOutputStream docout, DataOutputStream remove) throws Exception {
		
		StringDuplicate sd = new StringDuplicate();
		
		DB.loadNews(docs, date_end, days);
		
		for (Document d : docs.values()){
			try {			
				/* Check if the document contains hashtag, mention and url. */
				String[] toRemove = new String[50];
				searchHashtagMentionUrl(new DataInputStream(new ByteArrayInputStream(d.getBody().getBytes("UTF-8"))), d, toRemove, stopwords, porter);

				/* Writing 'remove.txt', a file containing hashtag, mention and url removed from each news' text. */
				remove.writeBytes(d.id + "\t");
				for (int j=0; j<toRemove.length && toRemove[j]!=null; j++){
					remove.writeBytes(toRemove[j] + " ");
				}
				remove.writeBytes("\n");
				
				/* Delete hashtag, mention and url from news' text. */
				for (int j=0; toRemove[j]!=null; j++){
					//System.out.println("Token before deleting: " + tokens[3]);
						int index = d.getBody().indexOf(toRemove[j]);
						if(index!=-1){
							d.setBody(d.getBody().replace(toRemove[j], ""));
							//System.out.println("Token after deleting: " + tokens[3]);
						}
				}
				
				/* Delete author from news' text. */
				d.setBody(replaceAuthor(d.getBody(), "Post di ", 8));
				d.setBody(replaceAuthor(d.getBody(), " Post ", 6));			//lo spazio prima di Post server per evitare che nomi cone HuffPost vengano troncati
				d.setBody(replaceAuthor(d.getBody(), "post di ", 8));
				d.setBody(replaceAuthor(d.getBody(), " post ", 6));			//lo spazio prima di Post server per evitare che nomi cone HuffPost vengano troncati
				d.setBody(replaceAuthor(d.getBody(), "Il commento di ", 15));
				// d.setBody(replaceAuthor(d.getBody(), "Dal blog di ", 12));
				
				
				loadDocumentTextFile(new DataInputStream(new ByteArrayInputStream(d.getBody().getBytes("UTF-8"))), stopwords, porter, d, 
						removeDuplicates, sd, twitterConstants.TEXT_WEIGHT, siteConstants.TEXT_WEIGHT, facebookConstants.TEXT_WEIGHT);

				docs.put(d.getID(), d);
				
				/* Write 'docs.txt', a file containing id, text, hashtag, mention and url for each news. */
				docout.writeBytes(d.id + "\t");
				docout.writeBytes(d.getBody() + "\n\t\t");
				for (Keyword k : d.keywords.values()){
					docout.writeBytes(k.word + " ");
				}
				if(d.hashtag.size()!=0) docout.writeBytes("\n\t\t");
				for (Hashtag h : d.hashtag.values()){
					docout.writeBytes(h.words + " ");
				}
				if(d.mention.size()!=0) docout.writeBytes("\n\t\t");
				for (int i1=0; i1 < d.mention.size(); i1++){
					docout.writeBytes(d.mention.get(i1) + " ");
				}
				if(d.url.size()!=0) docout.writeBytes("\n\t\t");
				for (int i1=0; i1 < d.url.size(); i1++){
					docout.writeBytes(d.url.get(i1) + " ");
				}
				if(d.link.size()!=0) docout.writeBytes("\n\t\t");
				for (int i1=0; i1 < d.link.size(); i1++){
					docout.writeBytes(d.link.get(i1) + " ");
				}
				docout.writeBytes("\n");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}

		new WriteConsole(docs.size() + " documents are loaded.\n");
		
		/* FILTERING: removing documents containing a low number of keywords. */
		new WriteConsole("--- FILTERING ---\n");
		File docsRemoved = new File("/tmp/keygraph_output/docsRemoved(small documents).txt");
    	docsRemoved.createNewFile();
    	DataOutputStream dremoved = new DataOutputStream(new FileOutputStream(docsRemoved));
    	dremoved.writeBytes("DocId\tChannel\tContent\n");
    	
		ArrayList<String> toRemove = new ArrayList<String>();
		int twitter=0, sito=0, fb=0;
		int docKeySizeMin = 0;
		for (Document d : docs.values()){
			if(d.channel.equals("twitter")) docKeySizeMin = twitterConstants.DOC_KEYWORDS_SIZE_MIN;
			if(d.channel.equals("sito")) docKeySizeMin = siteConstants.DOC_KEYWORDS_SIZE_MIN;
			if(d.channel.equals("facebook")) docKeySizeMin = facebookConstants.DOC_KEYWORDS_SIZE_MIN;
			if (d.keywords.size() >= docKeySizeMin) {
				if (!removeDuplicates || !d.isDuplicate) {
					for (Keyword k : d.keywords.values())
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
				}
			} else{
				if(d.channel.equals("twitter"))
					twitter++;
				if(d.channel.equals("sito"))
					sito++;
				if(d.channel.equals("facebook"))
					fb++;
				toRemove.add(d.id);
				dremoved.writeBytes(d.id + "\t" + d.channel + "\t" + d.getBody() + "\n");
			}
		}
		for (String id : toRemove)
			docs.remove(id);

		new WriteConsole(docs.size() + " documents remained after filtering small documents (Documents removed: " + toRemove.size() + " ( " + twitter + " twitter, " + sito + " sito, " + fb + " facebook)).\n");
		dremoved.close();
	}


	/**
	 * Removes the name of author contained in the text of news.
	 * 
	 * @param string	text of news
	 * @param author	string preceding the author's name
	 * @param i			length of String author
	 * @return			text of news without author's name
	 */
	private String replaceAuthor(String string, String author, int i) {
		int ind = 0;
		if((ind = string.indexOf(author)) != -1){
			ind+= i;
			String a1="", a2="", a3="";
			if( ind < string.length() && Character.isUpperCase(string.charAt(ind)) ){
				int i1 = ind;
				while(i1 < string.length() && (string.charAt(i1)) != ' '&& (string.charAt(i1)) != ')'){
					a1+=string.charAt(i1);
					i1++;
				}
			}
			
			if(a1!=""){
				ind+= a1.length();
				ind++;
				if( ind < string.length() && string.charAt(ind) != '\"' && string.charAt(ind) != '-' && string.charAt(ind) != ' ' && string.charAt(ind) != ')' && Character.isUpperCase(string.charAt(ind)) ){
					int i1 = ind;
					while(i1 < string.length() && (string.charAt(i1)) != ' ' && (string.charAt(i1)) != '\"' && (string.charAt(i1)) != '-' && (string.charAt(i1)) != ')'){
						a2+=string.charAt(i1);
						i1++;
					}
				}
			}
			
			if(a2!=""){
				ind+= a2.length();
				ind++;
				if( ind < string.length() && string.charAt(ind) != '\"' && string.charAt(ind) != '-' && string.charAt(ind) != ' ' && string.charAt(ind) != ')' && Character.isUpperCase(string.charAt(ind)) ){
					int i1 = ind;
					while(i1 < string.length() && (string.charAt(i1)) != ' ' && (string.charAt(i1)) != '\"' && (string.charAt(i1)) != '-' && (string.charAt(i1)) != ')'){
						a3+=string.charAt(i1);
						i1++;
					}
				}
			}
			
			String replace = new String();
			if(a1!=""){
				replace = author + a1;
			}
			if(a2!="") {
				replace+=" ";
				replace+=a2;
			}
			if(a3!="") {
				replace+=" ";
				replace+=a3;
			}
			string = string.replaceAll(replace, "");
		}
				
		return string;
	}

	public void addToHashtag(String base, List<String> words, Document d){
		if(!d.hashtag.containsKey(base)){
				d.hashtag.put(base, new Hashtag(base, words, 1, 1, 0));
		}
		else
			d.hashtag.get(base).tf += 1;
	}
	
	/**
	 * Adds the hashtag to the list of the keywords 
	 * of the document that is being analyzed.
	 * 
	 * @param keyword	the hashtag
	 * @param d			the document that is being analyzed
	 * @param weight	the weight of each word specified in the configuration parameters according to the publication channel
	 */
	public void addToKeyword(String keyword, Document d, double weight){
		if (!d.keywords.containsKey(keyword))
			d.keywords.put(keyword, new Keyword(keyword, keyword, weight, 1, 0));
		else
			d.keywords.get(keyword).tf += weight;
	}

	/**
	 * Looks for hashtag, mention and url in the text of the documents in input file.
	 * Hashtags are extracted from the text, added to hashtags and keywords list of the document.
	 * Mentions are extracted from the text and added to mentions list of the document.
	 * Url are removed from text and added to url list of the document.
	 * 
	 * @param in			the text of the document
	 * @param d				the document that is being analyzed
	 * @param toRemove		string in which the elements to remove from document's text are added (hashtag, mention and url)
	 * @param stopwords		list of stopwords
	 * @param porter		the stemmer
	 */
	public void searchHashtagMentionUrl(DataInputStream in, Document d, String[] toRemove, HashSet<String> stopwords, Porter porter) {
		try {
			String content = "";
			String line = "";
			int i=0;
			//in.readline is depracated
			BufferedReader bffrrdr = new BufferedReader(new InputStreamReader(in));
			while ((line = bffrrdr.readLine()) != null)
				content += line;

			StringTokenizer st = new StringTokenizer(content, " \",-;,!?()");
			while (st.hasMoreTokens()) {
				String word = st.nextToken();
				int ind = 0;
				/* Check if 'word' is hashtag. */
				if((ind = word.indexOf("#")) != -1){
					word = word.substring(ind, word.length());
					/* Check if there is an url in the hashtag. */
					if((ind = word.indexOf("http")) != -1){
						String word_http = new String();
						word_http = word.substring(ind, word.length());
						if(!d.url.contains(word_http)){
							toRemove[i++]=word_http;
							d.url.add(word_http);
						}
						word = word.substring(0, ind);
					}
					toRemove[i++]=word;
					int ind2 = 0;
					if((ind2 = word.indexOf("Â»"))!=-1){
						word = word.substring(0, ind2);
					}
					if((ind2 = word.indexOf("."))!=-1){
						word = word.substring(0, ind2);
					}
					if((ind2 = word.indexOf(":"))!=-1){
						word = word.substring(0, ind2);
					}
					word = word.trim();
					String base = new String(word);
					base = base.toLowerCase();
					//System.out.println("HASHTAG base: " + base);
					if(!stopwords.contains(base)){
						if(d.channel.equals("twitter")) addToKeyword(base, d, twitterConstants.HASHTAG_WEIGHT);
						if(d.channel.equals("sito")) addToKeyword(base, d, siteConstants.HASHTAG_WEIGHT);
						if(d.channel.equals("facebook")) addToKeyword(base, d, facebookConstants.HASHTAG_WEIGHT);
					}
					
					/* Parsing of hashtag and splitting. */
					word = word.replaceAll("#", "");
					List<String> words = new ArrayList<>(); //words in hashtag
					StringTokenizer st_hashtag = new StringTokenizer(word, "");
					while (st_hashtag.hasMoreTokens()) {
						String token = st_hashtag.nextToken();
						for(int k = 0; k < token.length(); k++){
							if(Character.isUpperCase(token.charAt(k)) && k!=token.length()-1){
								int h=k;
								Vector<Character> upper = new Vector<Character>();
								do {
									upper.add(token.charAt(h));
									h++;
								} while(h < token.length() && Character.isUpperCase(token.charAt(h)));
								if(upper.size() > 2){
									String upperCase = new String();
									for(int m=0; m < upper.size(); m++)
										upperCase+=upper.get(m);
									token = token.replaceAll(upperCase, "");
									upperCase = upperCase.toLowerCase();
									upperCase = finalCharacter(upperCase);
									
									if(upperCase.length() > 1){
										if(!stopwords.contains(upperCase.trim()))
											words.add(upperCase.trim());
									}
								}
							}
						}
						
						String[] s = token.split("(?=\\p{Lu})");
						for(int j=0; j < s.length; j++){
							s[j] = finalCharacter(s[j]);
							if(s[j].length() > 1){
								s[j] = s[j].toLowerCase();
								if(!stopwords.contains(s[j].trim()))
									words.add(s[j].trim());
							}
						}
					}
					
					for(int j=0; j < words.size(); j++){
						String s = words.get(j).toLowerCase();
						words.set(j, s);
					}
					
					addToHashtag(base, words, d);
					
					for(int j=0; j < words.size(); j++){
						if(d.channel.equals("twitter")) addToKeyword(words.get(j), d, porter, twitterConstants.HASHTAG_WEIGHT);
						if(d.channel.equals("sito")) addToKeyword(words.get(j), d, porter, siteConstants.HASHTAG_WEIGHT);
						if(d.channel.equals("facebook")) addToKeyword(words.get(j), d, porter, facebookConstants.HASHTAG_WEIGHT);
					}
					
				}
				
				/* Check if 'word' is a mention. */
				if((ind = word.indexOf("@")) != -1){
					word = word.substring(ind);
					toRemove[i++]=word;
					//System.out.println("Mention: " + word);
					word = word.replaceAll("@", "");
					if(!d.mention.contains(word))
						d.mention.add(word);
				}
				
				/* Check if 'word' is an URL address. */
				if((ind = word.indexOf("http")) != -1){
					word = word.substring(ind, word.length());
					//System.out.println("Url: " + word);
					toRemove[i++]=word;
					if(!d.url.contains(word))
						d.url.add(word);
				}

			}

			in.close();
			
		} catch (Exception e) {
			// System.out.println(f.getName());
			e.printStackTrace();
		}
	}

	/**
	 * Adds each word of the hashtag to the list 
	 * of keywords of the document that is being analyzed.
	 * 
	 * @param string	the word contained in the hashtag
	 * @param d			the document that is being analyzed
	 * @param porter	the stemmer
	 * @param weight	the weight of each word specified in the configuration parameters according to the publication channel
	 */
	private void addToKeyword(String string, Document d, Porter porter, double weight) {
		String base = new String();
		base = porter.stripAffixes(string);
		if (!d.keywords.containsKey(base))
			d.keywords.put(base, new Keyword(base, string, weight, 1, 0));
		else
			d.keywords.get(base).tf += weight;
	}

	private String finalCharacter(String string) {
		if(string.endsWith("nell")){
			string = string.substring(0, string.length()-4);
		}
		if(string.endsWith("nel")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("dell")){
			string = string.substring(0, string.length()-4);
		}
		if(string.endsWith("del")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("dall")){
			string = string.substring(0, string.length()-4);
		}
		if(string.endsWith("dal")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("d")){
			string = string.substring(0, string.length()-1);
		}
		if(string.endsWith("in")){
			string = string.substring(0, string.length()-2);
		}
		if(string.endsWith("il")){
			string = string.substring(0, string.length()-2);
		}
		if(string.endsWith("sull")){
			string = string.substring(0, string.length()-4);
		}
		if(string.endsWith("sul")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("per")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("con")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("col")){
			string = string.substring(0, string.length()-3);
		}
		if(string.endsWith("al")){
			string = string.substring(0, string.length()-2);
		}
		if(string.endsWith("all")){
			string = string.substring(0, string.length()-3);
		}
		return string;
	}

	public void loadDocumentsOLD(String inputFileName, HashMap<String, Document> docs, HashSet<String> stopwords, HashMap<String, Double> DF, Porter porter,
			boolean removeDuplicates) throws Exception {
		File inputFile = new File(inputFileName);
		StringDuplicate sd = new StringDuplicate();
		if (inputFile.isDirectory()) {
			int i = 0;
			for (String file : inputFile.list())
				try {
					file = inputFileName + "/" + file;
					if (i++ % 1000 == 0)
						new WriteConsole(i + " documents are loaded.\n");
					String id = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
					Document d = docs.containsKey(id) ? docs.get(id) : new Document(id);
					// d.setTitle(line.split(",")[0]);
					if (file.endsWith(".keywords"))
						loadDocumentKeyFile(openDataInputStream(file), stopwords, porter, d, twitterConstants.KEYWORDS_1_WEIGHT);
					else
						// if (file.endsWith(".txt"))
						loadDocumentTextFile(openDataInputStream(file), stopwords, porter, d, removeDuplicates, sd, twitterConstants.TEXT_WEIGHT, siteConstants.TEXT_WEIGHT, facebookConstants.TEXT_WEIGHT);
					docs.put(id, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
		} else {
			BufferedReader in = new BufferedReader(new FileReader(inputFile));
			String line = null;
			int i = 0;
			while ((line = in.readLine()) != null)
				try {
					String file = line.split(",")[1];
					if (i++ % 1000 == 0)
						new WriteConsole(i + " documents are loaded.\n");
					String id = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
					Document d = docs.containsKey(id) ? docs.get(id) : new Document(id);
					// d.setTitle(line.split(",")[0]);
					if (file.endsWith(".keywords"))
						loadDocumentKeyFile(openDataInputStream(file), stopwords, porter, d, twitterConstants.KEYWORDS_1_WEIGHT);

					if (file.endsWith(".txt"))
						loadDocumentTextFile(openDataInputStream(file), stopwords, porter, d, removeDuplicates, sd, twitterConstants.TEXT_WEIGHT, siteConstants.TEXT_WEIGHT, facebookConstants.TEXT_WEIGHT);

					docs.put(id, d);
				} catch (Exception e) {
					e.printStackTrace();
				}
			in.close();
		}

		new WriteConsole(docs.size() + " documents are loaded.\n");
		ArrayList<String> toRemove = new ArrayList<String>();
		int docKeySizeMin = 0;
		for (Document d : docs.values()){
			if(d.channel.equals("twitter")) docKeySizeMin = twitterConstants.DOC_KEYWORDS_SIZE_MIN;
			if(d.channel.equals("sito")) docKeySizeMin = siteConstants.DOC_KEYWORDS_SIZE_MIN;
			if(d.channel.equals("facebook")) docKeySizeMin = facebookConstants.DOC_KEYWORDS_SIZE_MIN;
		
			if (d.keywords.size() >= docKeySizeMin) {
				if (!removeDuplicates || !d.isDuplicate) {
					for (Keyword k : d.keywords.values())
						if (DF.containsKey(k.baseForm))
							DF.put(k.baseForm, DF.get(k.baseForm) + 1);
						else
							DF.put(k.baseForm, new Double(1));
				}
			} else
				toRemove.add(d.id);
		}

		for (String id : toRemove)
			docs.remove(id);

		new WriteConsole(docs.size() + "documents remaind after filterig small documents.\n");

	}

	public void loadDocumentKeyFile(DataInputStream in, HashSet<String> stopwords, Porter porter, Document d, double BoostRate) {
		// if (Constants.BREAK_NP)
		// fetchDocumentNEAndNPFileWithBreaking(f, stopwords, porter, d);
		// System.out.println("injaaaaaaaaaaaaaaaaa:"+d.id);
		try {
			// DataInputStream in = openDataInputStream(f);
			String line = null;
			//in.readline is depracated
			BufferedReader bffrrdr = new BufferedReader(new InputStreamReader(in));
			while ((line = bffrrdr.readLine()) != null && line.length() > 2) {
				// System.out.println(line);
				int index = line.lastIndexOf("==");
				String word = line.substring(0, index);
				double tf = Integer.parseInt(line.substring(index + 2)) * BoostRate;
				String base = getBaseForm(stopwords, porter, word);
				if (base.length() > 2)
					if (!d.keywords.containsKey(base))
						d.keywords.put(base, new Keyword(base, word, tf, 1, 0));
					else
						d.keywords.get(base).tf += tf;

			}
			in.close();
			// System.out.println("done::"+d.keywords.size());
		} catch (Exception e) {
			// System.out.println(f.getName());
			e.printStackTrace();
		}

	}

	public void loadDocumentKeyFile(ArrayList<String> words, HashSet<String> stopwords, Porter porter, Document d, double BoostRate) {
		// if (Constants.BREAK_NP)
		// fetchDocumentNEAndNPFileWithBreaking(f, stopwords, porter, d);
		// System.out.println("injaaaaaaaaaaaaaaaaa:"+d.id);
		try {
			// DataInputStream in = openDataInputStream(f);
			for (String word : words)
				if (!stopwords.contains(word) && word.length() > 2) {
					String base = getBaseForm(stopwords, porter, word);
					if (base.length() > 2)
						if (!d.keywords.containsKey(base))
							d.keywords.put(base, new Keyword(base, word, 1, 1, 0));
						else
							d.keywords.get(base).tf += 1;

				}
			// System.out.println("done::"+d.keywords.size());
		} catch (Exception e) {
			// System.out.println(f.getName());
			e.printStackTrace();
		}

	}

	public static String getBaseForm(HashSet<String> stopwords, Porter porter, String word) {
		String base = "";
		StringTokenizer stt = new StringTokenizer(word, "!' -_@0123456789.");
		// System.out.println(stt.countTokens()+"::"+stopwords+"::"+porter);
		while (stt.hasMoreTokens()) {
			String token = stt.nextToken().toLowerCase();
			if ((token.indexOf("?") == -1 && token.length() > 2 && !stopwords.contains(token)))
				base += porter.stripAffixes(token) + " ";
		}
		return base.trim();
	}

	/**
	 * Analyzes every word of news'text.
	 * 
	 * @param in				text of the news
	 * @param stopwords			list of stopwords
	 * @param porter			the stemmer
	 * @param d					the document that is being analyzed
	 * @param removeDuplicates	boolean specified in configuration parameters
	 * @param sd				string duplicate, check if there is another news with the same text only if removeDuplicates is true
	 * @param BoostRateT		the weight of each word specified in the configuration parameters if Twitter is the publication channel
	 * @param BoostRateS		the weight of each word specified in the configuration parameters if Website is the publication channel
	 * @param BoostRateF		the weight of each word specified in the configuration parameters if Facebook is the publication channel
	 */
	public void loadDocumentTextFile(DataInputStream in, HashSet<String> stopwords, Porter porter, Document d, boolean removeDuplicates,
			StringDuplicate sd, double BoostRateT, double BoostRateS, double BoostRateF) {
		try {
			String content = "";
			String line = "";
			//in.readline is depracated
			BufferedReader bffrrdr = new BufferedReader(new InputStreamReader(in));
			while ((line = bffrrdr.readLine()) != null)
				content += line;

			if (removeDuplicates)
				d.isDuplicate = sd.isDuplicate(content);
			
			StringTokenizer st = new StringTokenizer(content, "|\"' -_.,;:$&£%=+§°/\\*()[]{}<>«»!?â");
			while (st.hasMoreTokens()) {
				String word = st.nextToken();
				word = word.toLowerCase();
				String token = word;
				double tf = 0;
				if(d.channel.equals("twitter")) tf = 1 * BoostRateT;
				if(d.channel.equals("sito")) tf = 1 * BoostRateS;
				if(d.channel.equals("facebook")) tf = 1 * BoostRateF;
				String base = "";
				
				if ((token.indexOf("?") == -1 && token.length() >= 2 && !stopwords.contains(token) && !StringUtils.isNumeric(token))){
					base = porter.stripAffixes(token);
					// System.out.println("Stemmer: " + token + " .................. " + base);
				}

				if (base.length() >= 2 && !stopwords.contains(base) && !StringUtils.isNumeric(base))
					if (!d.keywords.containsKey(base))
						d.keywords.put(base, new Keyword(base, word, tf, 1, 0));
					else
						d.keywords.get(base).tf += tf;

			}
			in.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean hasDigit(String in) {
		for (int i = 0; i < in.length(); i++)
			if (Character.isDigit(in.charAt(i)))
				return true;
		return false;
	}
}