package topicDetection;

import java.util.ArrayList;

/**
 * Class to calculate the number of bonds between
 * the documents in the clusters found by the algorithm.
 * 
 * @author Federica
 *
 */

public class Bonds {
	
	/**
	 * Constructor
	 */
	public Bonds(){
		
	}

	/**
	 * Compute the number of bonds between the news 
	 * published in the same channel in all the clusters
	 * found by the algorithm.
	 * 
	 * @param clusters	the list of the clusters found by the algorithm
	 * @return	the number of bonds between the news published in the same channel
	 */
	public int computeChannel(ArrayList<DocumentCluster> clusters) {
		int sameChannel = 0;
		for(DocumentCluster dc: clusters){
			int FB=0, TW=0, SITO=0;
			for(Document d: dc.docs.values()){
				if(d.channel.equals("facebook")) FB++;
				if(d.channel.equals("twitter")) TW++;
				if(d.channel.equals("sito")) SITO++;
			}
			if(FB!=0 && FB!=1) sameChannel+=FB*(FB-1)/2;
			if(TW!=0 && TW!=1) sameChannel+=TW*(TW-1)/2;
			if(SITO!=0 && SITO!=1) sameChannel+=SITO*(SITO-1)/2;
		}
		return sameChannel;
	}

	/**
	 * Compute the total number of bonds in 
	 * the clusters of documents found by the algorithm.
	 * 
	 * @param clusters	the list of the clusters found by the algorithm
	 * @return	the total number of bonds in the clusters
	 */
	public int computeTotBonds(ArrayList<DocumentCluster> clusters) {
		int totBonds = 0;
		for(DocumentCluster dc: clusters){
			totBonds += (dc.docs.size()*(dc.docs.size()-1))/2;
		}
		return totBonds;
	}

	/**
	 * Compute the number of bonds between the news 
	 * published by the same newspaper in all the clusters
	 * found by the algorithm.
	 * 
	 * @param clusters	the list of the clusters found by the algorithm
	 * @return	the number of bonds between the news published by the same newspaper
	 */
	public int computeNewspaper(ArrayList<DocumentCluster> clusters) {
		int sameNewspaper = 0;
		for(DocumentCluster dc: clusters){
			int corrieredellasera=0, larepubblica=0, ilsole24ore=0, lagazzettadellosport=0, 
					lastampa=0, ilmessaggero=0, ilrestodelcarlino=0, corrieredellosport=0, ilgiornale=0,
					avvenire=0, lanazione=0, tuttosport=0, libero=0, italiaoggi=0, ilgazzettino=0, 
					ilfattoquotidiano=0, ilsecoloxix=0, iltirreno=0, ilmattino=0, ilgiorno=0, ansa=0;
			for(Document d: dc.docs.values()){
				if(d.author.equals("corrieredellasera")) corrieredellasera++;
				if(d.author.equals("larepubblica")) larepubblica++;
				if(d.author.equals("ilsole24ore")) ilsole24ore++;
				if(d.author.equals("lagazzettadellosport")) lagazzettadellosport++;
				if(d.author.equals("lastampa")) lastampa++;
				if(d.author.equals("ilmessaggero")) ilmessaggero++;
				if(d.author.equals("ilrestodelcarlino")) ilrestodelcarlino++;
				if(d.author.equals("corrieredellosport")) corrieredellosport++;
				if(d.author.equals("ilgiornale")) ilgiornale++;
				if(d.author.equals("avvenire")) avvenire++;
				if(d.author.equals("lanazione")) lanazione++;
				if(d.author.equals("tuttosport")) tuttosport++;
				if(d.author.equals("libero")) libero++;
				if(d.author.equals("italiaoggi")) italiaoggi++;
				if(d.author.equals("ilgazzettino")) ilgazzettino++;
				if(d.author.equals("ilfattoquotidiano")) ilfattoquotidiano++;
				if(d.author.equals("ilsecoloxix")) ilsecoloxix++;
				if(d.author.equals("iltirreno")) iltirreno++;
				if(d.author.equals("ilmattino")) ilmattino++;
				if(d.author.equals("ilgiorno")) ilgiorno++;
				if(d.author.equals("ansa")) ansa++;
			}
			
			if(corrieredellasera!=0 && corrieredellasera!=1) sameNewspaper+=corrieredellasera*(corrieredellasera-1)/2;
			if(larepubblica!=0 && larepubblica!=1) sameNewspaper+=larepubblica*(larepubblica-1)/2;
			if(ilsole24ore!=0 && ilsole24ore!=1) sameNewspaper+=ilsole24ore*(ilsole24ore-1)/2;
			if(lagazzettadellosport!=0 && lagazzettadellosport!=1) sameNewspaper+=lagazzettadellosport*(lagazzettadellosport-1)/2;
			if(lastampa!=0 && lastampa!=1) sameNewspaper+=lastampa*(lastampa-1)/2;
			if(ilmessaggero!=0 && ilmessaggero!=1) sameNewspaper+=ilmessaggero*(ilmessaggero-1)/2;
			if(ilrestodelcarlino!=0 && ilrestodelcarlino!=1) sameNewspaper+=ilrestodelcarlino*(ilrestodelcarlino-1)/2;
			if(corrieredellosport!=0 && corrieredellosport!=1) sameNewspaper+=corrieredellosport*(corrieredellosport-1)/2;
			if(ilgiornale!=0 && ilgiornale!=1) sameNewspaper+=ilgiornale*(ilgiornale-1)/2;
			if(avvenire!=0 && avvenire!=1) sameNewspaper+=avvenire*(avvenire-1)/2;
			if(lanazione!=0 && lanazione!=1) sameNewspaper+=lanazione*(lanazione-1)/2;
			if(tuttosport!=0 && tuttosport!=1) sameNewspaper+=tuttosport*(tuttosport-1)/2;
			if(libero!=0 && libero!=1) sameNewspaper+=libero*(libero-1)/2;
			if(italiaoggi!=0 && italiaoggi!=1) sameNewspaper+=italiaoggi*(italiaoggi-1)/2;
			if(ilgazzettino!=0 && ilgazzettino!=1) sameNewspaper+=ilgazzettino*(ilgazzettino-1)/2;
			if(ilfattoquotidiano!=0 && ilfattoquotidiano!=1) sameNewspaper+=ilfattoquotidiano*(ilfattoquotidiano-1)/2;
			if(ilsecoloxix!=0 && ilsecoloxix!=1) sameNewspaper+=ilsecoloxix*(ilsecoloxix-1)/2;
			if(iltirreno!=0 && iltirreno!=1) sameNewspaper+=iltirreno*(iltirreno-1)/2;
			if(ilmattino!=0 && ilmattino!=1) sameNewspaper+=ilmattino*(ilmattino-1)/2;
			if(ilgiorno!=0 && ilgiorno!=1) sameNewspaper+=ilgiorno*(ilgiorno-1)/2;
			if(ansa!=0 && ansa!=1) sameNewspaper+=ansa*(ansa-1)/2;
			
		}
		return sameNewspaper;
	}

	/**
	 * Compute the total number of bonds in 
	 * the clusters of documents found by the 
	 * algorithm considering only the links.
	 * 
	 * @param documentClustersLink	the list of the clusters found by the algorithm considering only the links
	 * @return	the total number of bonds in the clusters obtained considering only the links
	 */
	public int computeTotBondsLink(ArrayList<DocumentClusterLink> documentClustersLink) {
		int totBonds = 0;
		for(DocumentClusterLink dc: documentClustersLink){
			totBonds += (dc.docs.size()*(dc.docs.size()-1))/2;
		}
		return totBonds;
	}

	/**
	 * Compute the number of bonds between the news 
	 * published in the same channel in all the clusters
	 * found by the algorithm considering only the links.
	 * 
	 * @param documentClustersLink	the list of the clusters found by the algorithm considering only the links
	 * @return	the number of bonds between the news published in the same channel 
	 * 			in the clusters obtained considering only the links
	 */
	public int computeChannelLink(ArrayList<DocumentClusterLink> documentClustersLink) {
		int sameChannel = 0;
		for(DocumentClusterLink dc: documentClustersLink){
			int FB=0, TW=0, SITO=0;
			for(Document d: dc.docs.values()){
				if(d.channel.equals("facebook")) FB++;
				if(d.channel.equals("twitter")) TW++;
				if(d.channel.equals("sito")) SITO++;
			}
			if(FB!=0 && FB!=1) sameChannel+=FB*(FB-1)/2;
			if(TW!=0 && TW!=1) sameChannel+=TW*(TW-1)/2;
			if(SITO!=0 && SITO!=1) sameChannel+=SITO*(SITO-1)/2;
		}
		return sameChannel;
	}

	/**
	 * Compute the number of bonds between the news 
	 * published by the same newspaper in all the clusters
	 * found by the algorithm considering only the links.
	 * 
	 * @param documentClustersLink	the list of the clusters found by the algorithm considering only the links
	 * @return	the number of bonds between the news published by the same newspaper 
	 * 			in the clusters obtained considering only the links
	 */
	public int computeNewspaperLink(ArrayList<DocumentClusterLink> documentClustersLink) {
		int sameNewspaper = 0;
		for(DocumentClusterLink dc: documentClustersLink){
			int corrieredellasera=0, larepubblica=0, ilsole24ore=0, lagazzettadellosport=0, 
					lastampa=0, ilmessaggero=0, ilrestodelcarlino=0, corrieredellosport=0, ilgiornale=0,
					avvenire=0, lanazione=0, tuttosport=0, libero=0, italiaoggi=0, ilgazzettino=0, 
					ilfattoquotidiano=0, ilsecoloxix=0, iltirreno=0, ilmattino=0, ilgiorno=0, ansa=0;
			for(Document d: dc.docs.values()){
				if(d.author.equals("corrieredellasera")) corrieredellasera++;
				if(d.author.equals("larepubblica")) larepubblica++;
				if(d.author.equals("ilsole24ore")) ilsole24ore++;
				if(d.author.equals("lagazzettadellosport")) lagazzettadellosport++;
				if(d.author.equals("lastampa")) lastampa++;
				if(d.author.equals("ilmessaggero")) ilmessaggero++;
				if(d.author.equals("ilrestodelcarlino")) ilrestodelcarlino++;
				if(d.author.equals("corrieredellosport")) corrieredellosport++;
				if(d.author.equals("ilgiornale")) ilgiornale++;
				if(d.author.equals("avvenire")) avvenire++;
				if(d.author.equals("lanazione")) lanazione++;
				if(d.author.equals("tuttosport")) tuttosport++;
				if(d.author.equals("libero")) libero++;
				if(d.author.equals("italiaoggi")) italiaoggi++;
				if(d.author.equals("ilgazzettino")) ilgazzettino++;
				if(d.author.equals("ilfattoquotidiano")) ilfattoquotidiano++;
				if(d.author.equals("ilsecoloxix")) ilsecoloxix++;
				if(d.author.equals("iltirreno")) iltirreno++;
				if(d.author.equals("ilmattino")) ilmattino++;
				if(d.author.equals("ilgiorno")) ilgiorno++;
				if(d.author.equals("ansa")) ansa++;
			}
			
			if(corrieredellasera!=0 && corrieredellasera!=1) sameNewspaper+=corrieredellasera*(corrieredellasera-1)/2;
			if(larepubblica!=0 && larepubblica!=1) sameNewspaper+=larepubblica*(larepubblica-1)/2;
			if(ilsole24ore!=0 && ilsole24ore!=1) sameNewspaper+=ilsole24ore*(ilsole24ore-1)/2;
			if(lagazzettadellosport!=0 && lagazzettadellosport!=1) sameNewspaper+=lagazzettadellosport*(lagazzettadellosport-1)/2;
			if(lastampa!=0 && lastampa!=1) sameNewspaper+=lastampa*(lastampa-1)/2;
			if(ilmessaggero!=0 && ilmessaggero!=1) sameNewspaper+=ilmessaggero*(ilmessaggero-1)/2;
			if(ilrestodelcarlino!=0 && ilrestodelcarlino!=1) sameNewspaper+=ilrestodelcarlino*(ilrestodelcarlino-1)/2;
			if(corrieredellosport!=0 && corrieredellosport!=1) sameNewspaper+=corrieredellosport*(corrieredellosport-1)/2;
			if(ilgiornale!=0 && ilgiornale!=1) sameNewspaper+=ilgiornale*(ilgiornale-1)/2;
			if(avvenire!=0 && avvenire!=1) sameNewspaper+=avvenire*(avvenire-1)/2;
			if(lanazione!=0 && lanazione!=1) sameNewspaper+=lanazione*(lanazione-1)/2;
			if(tuttosport!=0 && tuttosport!=1) sameNewspaper+=tuttosport*(tuttosport-1)/2;
			if(libero!=0 && libero!=1) sameNewspaper+=libero*(libero-1)/2;
			if(italiaoggi!=0 && italiaoggi!=1) sameNewspaper+=italiaoggi*(italiaoggi-1)/2;
			if(ilgazzettino!=0 && ilgazzettino!=1) sameNewspaper+=ilgazzettino*(ilgazzettino-1)/2;
			if(ilfattoquotidiano!=0 && ilfattoquotidiano!=1) sameNewspaper+=ilfattoquotidiano*(ilfattoquotidiano-1)/2;
			if(ilsecoloxix!=0 && ilsecoloxix!=1) sameNewspaper+=ilsecoloxix*(ilsecoloxix-1)/2;
			if(iltirreno!=0 && iltirreno!=1) sameNewspaper+=iltirreno*(iltirreno-1)/2;
			if(ilmattino!=0 && ilmattino!=1) sameNewspaper+=ilmattino*(ilmattino-1)/2;
			if(ilgiorno!=0 && ilgiorno!=1) sameNewspaper+=ilgiorno*(ilgiorno-1)/2;
			if(ansa!=0 && ansa!=1) sameNewspaper+=ansa*(ansa-1)/2;
			
		}
		return sameNewspaper;
	}

}