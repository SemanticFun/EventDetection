package topicDetection;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

public class DB {
	String host, User, Pass, dbName, connectionUrl, Query;
	int port;
	Statement stm;
	PreparedStatement prstm;
	ResultSet rs;
	Connection con;
	
	static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	static Calendar c = GregorianCalendar.getInstance();
	
	public DB(String host, String User, String Pass, String dbName, int port){
		this.host= host;
		this.User = User;
		this.Pass = Pass;
		this.dbName = dbName;
		this.connectionUrl = "jdbc:mysql://" + host + ":" + port + "/"+dbName+"?autoReconnect=true&useSSL=false";
	}
		
	public void connect() throws ClassNotFoundException, SQLException{
		Class.forName("com.mysql.jdbc.Driver");
		this.con =  DriverManager.getConnection(this.connectionUrl,this.User,this.Pass);
		this.stm = this.con.createStatement();
	}
	
	public void close() throws SQLException{
		this.rs.close();
		this.stm.close();
		this.con.close();
	}
	
	//metodo per tirare fuori i dati dal DB
	public static void loadNews(HashMap<String, Document> docs, String date_end, int days) throws SQLException, ClassNotFoundException, ParseException, IOException, JSONException{
		Date start;
		c.setTime(formatter.parse(date_end));
		c.add(Calendar.DAY_OF_MONTH, -(--days));
		start = c.getTime();
		
		//prendo i dati di configurazione per somer dal file config.json
		String user, pass, dbname, host;
		int port;
		
		//FileReader reader = new FileReader("/home/tomcat/keygraph/config.json");
		FileReader reader = new FileReader("./config.json");
		String fileContents = "";

		int j;
		while ((j = reader.read()) != -1) {
			char ch = (char) j;
			fileContents = fileContents + ch;
		}
		
		JSONObject jsonObject;
		JSONObject somer = null;
		try {
			//Parsing del file Json
			jsonObject = new JSONObject(fileContents);
			somer = new JSONObject(jsonObject.get("somer").toString());
		} catch (JSONException e) {
			new WriteConsole(e, "error in parsing config.json");
			System.exit(1);
		}
		
		/* Configuration constants */
		host = somer.getString("host").toString();
		user = somer.getString("user").toString();
		pass = somer.getString("pass").toString();
		dbname = somer.getString("dbname").toString();
		port = somer.getInt("port");

		reader.close();
		//fine lettura file di configurazione
		
		DB conn = new DB(host, user, pass, dbname, port);
		conn.connect();
		
		conn.Query = "select p.id_post, channel, a.name," +
				"CASE " +
					"WHEN p.title = \"\" THEN p.text_descr " +
			        "ELSE p.title " +
				"END as post_text, date(pubDate), l.url as link " +
				"from post p join rssfeed r on p.id_rss=r.id_rss join author a on a.id_author = r.id_author left join link l on p.id_post = l.id_post " +
				"where date(pubdate)>= ? and date(pubdate)<= ?";
			
		conn.prstm = conn.con.prepareStatement(conn.Query);
		conn.prstm.setString(1, formatter.format(start));
		conn.prstm.setString(2, date_end);
		
		conn.rs = conn.prstm.executeQuery();
		
		while (conn.rs.next()){
			Document d = docs.containsKey(new Integer(conn.rs.getInt("p.id_post")).toString()) ? docs.get(new Integer(conn.rs.getInt("p.id_post")).toString()) : new Document(new Integer(conn.rs.getInt("p.id_post")).toString(), conn.rs.getString("channel"), conn.rs.getString("a.name"), conn.rs.getDate("date(pubDate)").toString());
			d.setBody(conn.rs.getString("post_text"));
			if (conn.rs.getString("link") != null)
				d.link.add(conn.rs.getString("link"));
			docs.put(d.getID(), d);
		}
		
		conn.close();
	}
	
	
	//metodo per salvare i cluster nel DB
	public static void storeCluster(Collection<DocumentCluster> clusters, String date_end, int days) throws SQLException, ClassNotFoundException, ParseException, IOException, JSONException {
		short cluster_count = 0;
		Date start;
		c.setTime(formatter.parse(date_end));
		c.add(Calendar.DAY_OF_MONTH, -(--days));
		start = c.getTime();
		String date_start = formatter.format(start);
		days++;
		
		//prendo i dati di configurazione per somer dal file config.json
		String user, pass, dbname, host;
		int port;
		
		//FileReader reader = new FileReader("/home/tomcat/keygraph/config.json");
		FileReader reader = new FileReader("./config.json");
		String fileContents = "";
		int j;
		while ((j = reader.read()) != -1) {
			char ch = (char) j;
			fileContents = fileContents + ch;
		}
		
		JSONObject jsonObject;
		JSONObject cluster = null;
		try {
			//Parsing del file Json
			jsonObject = new JSONObject(fileContents);
			cluster = new JSONObject(jsonObject.get("cluster").toString());
		} catch (JSONException e) {
			new WriteConsole(e, "error in parsing config.json\n");
			System.exit(1);
		}
			
		/* Configuration constants */
		host = cluster.getString("host").toString();
		user = cluster.getString("user").toString();
		pass = cluster.getString("pass").toString();
		dbname = cluster.getString("dbname").toString();
		port = cluster.getInt("port");

		reader.close();
		//fine lettura file di configurazione
				
		DB conn = new DB(host, user, pass, dbname, port);		
		String temp = "";
		conn.connect();
		
		//inserimento cluster container
		
		//per ogni document cluster creo una entry di un vettore contenente il numero di notizie per ogni canale e per ogni autore
		Vector<HashMap<String, Integer>> counter = new Vector<HashMap<String, Integer>>();
		Integer count_channel, count_author;
		
		for (DocumentCluster dc : clusters){
			HashMap<String, Integer> temp_map = new HashMap<String, Integer>();
			for (Document doc : dc.docs.values()){
				//controllo il canale, se è giò presente nella mappa incremento di uno il valore altrimento lo pongo a 1
				count_channel = temp_map.get(doc.channel);
				temp_map.put(doc.channel, (count_channel==null) ? 1 : count_channel+1);
				//controllo l'autore, se è giò presente nella mappa incremento di uno il valore altrimento lo pongo a 1
				count_author = temp_map.get(doc.author);
				temp_map.put(doc.author, (count_author==null) ? 1 : count_author+1);
			}
			counter.add(temp_map);
		}

		//conterrano il nome dell'autore che ha pubblicato, rispettivamente, il numero maggiore e minore di notizie
		String top_publisher = "", worst_publisher = "";
		//conterrano il numero totale delle notizie pubblicate dai canali
		Integer FacebookCount = 0, TwitterCount = 0, SiteCount = 0, count = 0;
		HashMap<String, Integer> temp_map_author = new HashMap<String, Integer>();

		for (HashMap<String, Integer> map : counter){
			FacebookCount += (map.get("facebook") != null ? map.get("facebook") : 0);
			TwitterCount += (map.get("twitter") != null ? map.get("twitter") : 0);
			SiteCount += (map.get("sito") != null ? map.get("sito") : 0);
			
			//per controllare max e min mi server una mappa che aggreghi il cotenuto di tutte le mappe del vettore
			for (Entry<String, Integer> entry : map.entrySet()) {
				if (!(entry.getKey().equals("facebook") || entry.getKey().equals("twitter") || entry.getKey().equals("sito"))){
					count = map.get(entry.getKey());
					temp_map_author.put(entry.getKey(), (count == null ? entry.getValue() : count + entry.getValue()));
				}
			}
		}
		
		//controllo max e min per determinare top e worst publisher
		int max = 0, min = 9999;
		for (Entry<String, Integer> entry : temp_map_author.entrySet()) {
			if (entry.getValue() >= max){
				max = entry.getValue();
				top_publisher = entry.getKey();
			}
			if (entry.getValue() <= min){
				min = entry.getValue();
				worst_publisher = entry.getKey();
			}
		}
		
		conn.Query = "insert into cluster (Name, Start, End, Days, Type, TopPublisher, WorstPublisher, FacebookCount, TwitterCount, SiteCount, ID_container) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		conn.prstm = conn.con.prepareStatement(conn.Query);
		conn.prstm.setString(1, date_start + (days==1 ? " giornaliero" : days==7 ? " settimanale" : " custom"));
		conn.prstm.setString(2, date_start);
		conn.prstm.setString(3, date_end);
		conn.prstm.setInt(4, days);
		conn.prstm.setString(5, "root");
		conn.prstm.setString(6, top_publisher);
		conn.prstm.setString(7, worst_publisher);
		conn.prstm.setShort(8, FacebookCount.shortValue());
		conn.prstm.setShort(9, TwitterCount.shortValue());
		conn.prstm.setShort(10, SiteCount.shortValue());
		conn.prstm.setInt(11, 0);
				
		conn.prstm.executeUpdate();
		cluster_count++;
		
		//ottenimento id cluster container
		conn.Query = "Select max(ID_cluster) as max from cluster";
		conn.rs = conn.stm.executeQuery(conn.Query);
		Integer id_container = 0;
		while (conn.rs.next()){
			id_container = conn.rs.getInt("max");
		}
		Integer id_cur = id_container;
		
		short flag;
		Integer count_for_vector = 0, check_null;
		for (DocumentCluster dc : clusters){
			id_cur++;
			
			//cerco un nome del il document cluster
			String name = "";
			temp = "select name from somer.hashtag where id_post in (";
			for (Document d : dc.docs.values()){
				temp += (d.id + ",");
			}
			temp = temp.substring(0, temp.length()-1) + ")";
			conn.Query = temp;
			conn.rs = conn.stm.executeQuery(temp);
			HashMap<String, Integer> hashtag = new HashMap<String, Integer>();
			while (conn.rs.next()){
				Integer count_for_name = hashtag.get(conn.rs.getString("name"));
				hashtag.put(conn.rs.getString("name"), (count_for_name==null) ? 1 : count_for_name+1);
			}
			double max_for_name = 0;
			
			if (hashtag.size() != 0){
				//se ci sono hashtag cerco il nome in mezzo a questi
				for (Entry<String, Integer> entry : hashtag.entrySet()) {
				    if (entry.getValue() >= max_for_name){
				    	max_for_name = entry.getValue();
				    	name = entry.getKey();
					}
				}
			} else {
				//se non ci sono hastag lo devo cercare dai node del cluster
				for (Node n : dc.keyGraph.values()){
					if (n.keyword.tf > max_for_name){
						max_for_name = n.keyword.tf;
						name = n.keyword.getWord();
					}
				}
			}
			
			//cerco top e worst publisher di questo cluster
			max = 0;
			min = 9999;
			top_publisher = "";
			worst_publisher = "";
			for (Entry<String, Integer> entry : counter.elementAt(count_for_vector).entrySet()){
				if (!(entry.getKey().equals("facebook") || entry.getKey().equals("twitter") || entry.getKey().equals("sito"))){
					if (entry.getValue() >= max){
						max = entry.getValue();
						top_publisher = entry.getKey();
					}
					if (entry.getValue() <= min){
						min = entry.getValue();
						worst_publisher = entry.getKey();
					}
				}
			}
			
			conn.Query = "insert into cluster (Name, Start, End, Days, Type, TopPublisher, WorstPublisher, FacebookCount, TwitterCount, SiteCount, ID_container) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			conn.prstm = conn.con.prepareStatement(conn.Query);
			conn.prstm.setString(1, name);
			conn.prstm.setString(2, date_start);
			conn.prstm.setString(3, date_end);
			conn.prstm.setInt(4, days);
			conn.prstm.setString(5, "leaf");
			conn.prstm.setString(6, top_publisher);
			conn.prstm.setString(7, worst_publisher);
			check_null = counter.elementAt(count_for_vector).get("facebook");
			conn.prstm.setShort(8, (check_null != null ? check_null.shortValue() : 0));
			check_null = counter.elementAt(count_for_vector).get("twitter");
			conn.prstm.setShort(9, (check_null != null ? check_null.shortValue() : 0));
			check_null = counter.elementAt(count_for_vector).get("sito");
			conn.prstm.setShort(10, (check_null != null ? check_null.shortValue() : 0));
			conn.prstm.setInt(11, id_container);
						
			conn.prstm.executeUpdate();
			cluster_count++;
			
			flag = 0;
			temp = "insert into node values ";
			for (Node n : dc.keyGraph.values()){
				if (flag != 0)
					temp += ',';
				else
					flag = 1;
				temp += "(" + id_cur + ", \"" + n.keyword.getWord().replaceAll("[,'\"]", " ") + "\", " + n.keyword.tf + ")";
			}
			conn.Query = temp;
			conn.stm.executeUpdate(conn.Query);
			
			flag = 0;
			temp = "insert into edge values ";
			for (Node n : dc.keyGraph.values()){
				for (Edge e : n.edges.values()) {
					if (e.n1.equals(n)){
						if (flag != 0)
							temp += ',';
						else
							flag = 1;
						temp += "(" + id_cur + ", \"" + e.n1.keyword.getWord().replaceAll("[,'\"]", " ") + "\", \"" + e.n2.keyword.getWord().replaceAll("[,'\"]", " ") + "\")";
					}
				}
			}
			conn.Query = temp;
			conn.stm.executeUpdate(conn.Query);
			
			flag = 0;
			temp = "insert into news values ";
			for (Document d : dc.docs.values()){
				if (flag != 0)
					temp += ',';
				else
					flag = 1;
				temp += "(" + id_cur + ", " + d.id + ")";
			}
			conn.Query = temp;
			conn.stm.executeUpdate(conn.Query);
			
			count_for_vector++;
		}
		
		conn.close();
		new WriteConsole("Cluster aggiunti " + cluster_count + "\n");
	}
}
