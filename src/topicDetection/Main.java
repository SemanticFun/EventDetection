package topicDetection;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

public class Main {

	public static void main(String[] args) throws Exception {
		
		/*
		 * args[0] -> data dell'ultimo giorno da analizzare
		 * args[1] -> numero di giorni da analizzare
		 * 
		 * alternativa
		 * 
		 * args[0] --> contiene day oppure week
		 */
		
		String date = "";
		int days = 0;
		
		if (args.length == 1){
			//recupero la data di ieri
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Calendar c = GregorianCalendar.getInstance();
			
			c.add(Calendar.DAY_OF_MONTH, -1);
			Date temp_date = c.getTime();
			date = formatter.format(temp_date);
			
			if (args[0].equals("day")){
				days = 1;
			} else if (args[0].equals("week")){
				days = 7;
			} else {
				System.out.println("parametri errati --> usare day oppure week");
				System.exit(1);
			}
		} else if (args.length == 2){
			String temp_year, temp_month, temp_day;
			int int_year = 0, int_month = 0, int_day = 0;
			
			//inizio controlli correttezza data
			date = args[0];
			if (date.length() != 10){
				System.out.println("parametri errati --> data troppo lunga --> yyyy-MM-dd");
				System.exit(1);
			}
			
			temp_year = date.substring(0, 4);
			temp_month = date.substring(5, 7);
			temp_day = date.substring(8, 10);
			
			try{
				int_year = Integer.parseInt(temp_year);
				int_month = Integer.parseInt(temp_month);
				int_day = Integer.parseInt(temp_day);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("parametri errati --> parsing della data non riuscito");
				System.exit(1);
			}
			
			//controllo grossolani sulla correttezza della data
			if (int_month <= 0 || int_month > 12 || int_day <= 0 || int_day > 31){
				System.out.println("parametri errati --> data illegale 1");
				System.exit(1);
			}
			//controlli più fini sulla correttezza della data
			if (int_day > 30 && (int_month == 4 || int_month == 6 || int_month == 9 || int_month == 11)){
				System.out.println("parametri errati --> data illegale 2");
				System.exit(1);
			}
			if (int_day > 29 && int_month==2){
				System.out.println("parametri errati --> data illegale 3");
				System.exit(1);
			}
			if (int_day == 29 && int_month==2 && (int_year % 4) != 0){
				System.out.println("parametri errati --> data illegale 4");
				System.exit(1);
			}
			//fine controlli sulla correttezza della data
			
			//inizio controllo sulla correttezza del numero di giorni
			try{
				days = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("errore nella lettura dei parametri");
				System.exit(1);
			}
			//fine controllo sulla correttezza del numero di giorni
		} else {
			System.out.println("errore nel numero di parametri. chiusura del programma.");
			System.exit(1);			
		}
		
		new WriteConsole(date + " " + days + "\n");	
		
		//Lettura file di configurazione
		FileReader reader = new FileReader("config.json");
		//FileReader reader = new FileReader("./config.json");
		String fileContents = "";

		int j;
		while ((j = reader.read()) != -1) {
			char ch = (char) j;
			fileContents = fileContents + ch;
		}
		
		JSONObject jsonObject;
		JSONObject config = null;
		try {
			//Parsing del file Json
			jsonObject = new JSONObject(fileContents);
			config = new JSONObject(jsonObject.get("config").toString());
		} catch (JSONException e) {
			new WriteConsole(e, date + "error in parsing config.json");
			System.exit(1);
		}
		
		/* Configuration constants */
		String outputfile = config.getString("outputfile").toString();
		Constants constants = new Constants(config.getString("constants").toString());
		Constants twitterConstants = new Constants(config.getString("twitterConstants").toString());
		Constants siteConstants = new Constants(config.getString("siteConstants").toString());
		Constants facebookConstants = new Constants(config.getString("facebookConstants").toString());
		int version = config.getInt("version");

		reader.close();
		//fine lettura file di configurazione
		
		PrintStream out = new PrintStream(outputfile);
		HashSet<String> stopwords = Utils.importStopwords();
		HashMap<String, Double> DF = new HashMap<String, Double>();
		HashMap<String, Document> docs = new HashMap<String, Document>();
		Porter porter = new Porter();
		
		long time1 = System.currentTimeMillis();
		
		if (Files.notExists(Paths.get("tmp/keygraph_output"))){
			File dir = new File("/tmp/keygraph_output");
			dir.mkdir();
		}
		new WriteConsole("--- LOADING DOCUMENTS ---" + "\n");
		File output = new File("/tmp/keygraph_output/docs.txt");
    	output.createNewFile();
    	DataOutputStream docout = new DataOutputStream(new FileOutputStream(output));
    	docout.writeBytes("DOC_ID\tKEYWORDS\tHASHTAG\tMENTION\tURL\n");
    	File output_remove = new File("/tmp/keygraph_output/remove.txt");
    	output_remove.createNewFile();
    	DataOutputStream remove = new DataOutputStream(new FileOutputStream(output_remove));
    	remove.writeBytes("DocId\tHashtag,Mention,Url\n");
    	new DataLoader(twitterConstants, siteConstants, facebookConstants).loadDocuments(date, days, docs, stopwords, DF, porter, constants.REMOVE_DUPLICATES, docout, remove);
		docout.close();
		remove.close();
		
		long time2 = System.currentTimeMillis();
		
		new WriteConsole("--- CLUSTERING WITH LINKS ---" + "\n");
		ArrayList<DocumentClusterLink> documentClustersLink = new ArrayList<DocumentClusterLink>();
		for(Document d: docs.values())
			new SharedLinkedDocs().clusterLinkedDocs(d, docs, documentClustersLink);
		DocumentClusterLink.mergeDocumentClustersLink(documentClustersLink);
		new WriteConsole("Document clusters with shared link: " + documentClustersLink.size() + "\n");
		int docLinkCluster = 0;
    	for(DocumentClusterLink dc: documentClustersLink)
    		docLinkCluster+= dc.docs.size();
    	new WriteConsole("Documents in link cluster: " + docLinkCluster + "\n");
		File linkCluster = new File("/tmp/keygraph_output/clustering_with_links.txt");
    	linkCluster.createNewFile();
    	DataOutputStream linkClusters = new DataOutputStream(new FileOutputStream(linkCluster));
    	linkClusters.writeBytes("Document clusters with shared link: " + documentClustersLink.size() + "\n");
    	for(DocumentClusterLink dcL: documentClustersLink){
    		for(Document d: dcL.docs.values())
    			linkClusters.writeBytes(d.id + " " + d.date + " " + d.channel + " " + d.author + ", " + d.getBody() + "\n");
    		linkClusters.writeBytes("\n");
    	}
    	linkClusters.close();
    	int totBondsLink, sameChannelLink = 0, differentChannelLink = 0, sameNewspaperLink = 0, differentNewspaperLink = 0;
		Bonds bondsL = new Bonds();
		totBondsLink = bondsL.computeTotBondsLink(documentClustersLink);
		new WriteConsole("TotBonds: " + totBondsLink + "\n");
		sameChannelLink = bondsL.computeChannelLink(documentClustersLink);
		differentChannelLink = totBondsLink - sameChannelLink;
		new WriteConsole("TotBonds: " + totBondsLink + "   Same channel: " + sameChannelLink + "   Different channel: " + differentChannelLink + "\n");
		sameNewspaperLink = bondsL.computeNewspaperLink(documentClustersLink);
		differentNewspaperLink = totBondsLink - sameNewspaperLink;
		new WriteConsole("Same newspaper: " + sameNewspaperLink + "   Different newspaper: " + differentNewspaperLink + "\n");
    	
		long time3 = System.currentTimeMillis();
		
		ArrayList<DocumentCluster> clusters = new DocumentAnalyze(constants).clusterbyKeyGraph(docs, DF, version);
		
		long time4 = System.currentTimeMillis();
		
		/* Evaluation of the algorithm */
		new WriteConsole("--- ALGORITHM EVALUATION ---");
		RateAlgorithm rateAlgorithm = new RateAlgorithm();
		int clustered = 0, notClustered = 0;
		HashMap<String, Document> clusteredDocuments = new HashMap<String, Document>();
		clustered = rateAlgorithm.nClustered(clusters, clusteredDocuments);
		notClustered = docs.size() - clustered;
		new WriteConsole("Clustered documents: " + clustered + "\nNot clustered documents: " + notClustered + "\n");
		
		/* File containing a list of clustered documents. */
		File output1 = new File("/tmp/keygraph_output/clustered_documents.txt");
    	output1.createNewFile();
    	DataOutputStream docout1 = new DataOutputStream(new FileOutputStream(output1));
    	for(Document d: clusteredDocuments.values()){
    		docout1.writeBytes(d.id + "\t" + d.author + "\t" + d.channel + "\t" + d.date + "\t" + d.getBody() + "\n");
    	}
    	docout1.close();
    	
    	/* File containing a list of not clustered documents. */
    	File output11 = new File("/tmp/keygraph_output/not_clustered_documents.txt");
    	output11.createNewFile();
    	DataOutputStream docout11 = new DataOutputStream(new FileOutputStream(output11));
    	for(Document d: docs.values()){
    		if(!clusteredDocuments.containsKey(d.id))
    			docout11.writeBytes(d.id + "\t" + d.author + "\t" + d.channel + "\t" + d.date + "\t" + d.getBody() + "\n");
    	}
    	docout11.close();
		
		rateAlgorithm.rateFP(clusters);
		int countFP = 0;
		for(DocumentCluster dc: clusters){
			countFP += dc.FPdocs.size();
		}
		/**
		 * Compute the number of possible true positives,
		 * that are the documents correctly inserted in the cluster,
		 * as the difference between the number of clustered 
		 * documents and the number of possible false positives.
		 */
		int countTP = 0;
		for(DocumentCluster dc: clusters){
			countTP += dc.docs.size();
		}
		countTP-=countFP;
		
		rateAlgorithm.rateFNdc(docs, clusters);
		rateAlgorithm.rateFNdocs(docs, clusters);
		int countFN = 0;
		HashMap<String, Document> FN = new HashMap<String, Document>();
		for(DocumentCluster dc: clusters){
			for(Document d: dc.docs.values())
				FN.put(d.id, d);
		}
		countFN = FN.size();
		
		int countTN = 0;
		countTN = rateAlgorithm.rateTN(docs, clusters, countFN);
		
		new WriteConsole("possible TRUE POSITIVE: " + countTP + "\n");
		new WriteConsole("possible FALSE POSITIVE: " + countFP + "\n");
		new WriteConsole("possible FALSE NEGATIVE: " + countFN + "\n");
		new WriteConsole("possible TRUE NEGATIVE: " + countTN + "\n");
		
		int totBonds, sameChannel = 0, differentChannel = 0, sameNewspaper = 0, differentNewspaper = 0;
		Bonds bonds = new Bonds();
		totBonds = bonds.computeTotBonds(clusters);
		new WriteConsole("TotBonds: " + totBonds + "\n");
		sameChannel = bonds.computeChannel(clusters);
		differentChannel = totBonds - sameChannel;
		new WriteConsole("TotBonds: " + totBonds + "   Same channel: " + sameChannel + "   Different channel: " + differentChannel + "\n");
		sameNewspaper = bonds.computeNewspaper(clusters);
		differentNewspaper = totBonds - sameNewspaper;
		new WriteConsole("Same newspaper: " + sameNewspaper + "   Different newspaper: " + differentNewspaper + "\n");
		
		DocumentAnalyze.printTopics(clusters, out, docs, date, days);
		out.close();
				
		double toMins = 1000 * 60;
		double tot = (time2 - time1) / toMins;
		tot += (time4 - time3) / toMins;
		new WriteConsole("Total time: " + tot + "\t" + 
							"Loading documents time: " + (time2 - time1) / toMins + "\t" +
							"Clustering time: " + (time4 - time3)/ toMins + "\n");
		
		new WriteConsole("------------ DONE! ------------\n\n\n");

	}

}
