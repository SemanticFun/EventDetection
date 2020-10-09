package topicDetection;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	public static int intersect(HashMap<String, Document> c1, HashMap<String, Document> c2) {
		int i = 0;
		for (String k1 : c1.keySet())
			if (c2.containsKey(k1))
				i++;
		return i;
	}

	public static void extractKeys() {
	}

	/**
	 * Reads a file containing a list of stopwords,
	 * very commonly used words, deemed irrelevant
	 * for searching purposes. 
	 * The words in the file are added to a hashset of strings.
	 * 
	 * @return	hashset of the stopwords in the file
	 * @throws IOException 
	 * @throws JSONException 
	 */
	public static HashSet<String> importStopwords() throws IOException, JSONException {
		HashSet<String> stopwords = new HashSet<String>();
		
		//Lettura file di configurazione
		FileReader reader = new FileReader("/home/tomcat/keygraph/config.json");
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
			new WriteConsole(e, "error in parsing config.json");
			System.exit(1);
		}
		
		/* Configuration constants */
		String stopwordfile = config.getString("stopwordfile").toString();

		reader.close();
		//fine lettura file di configurazione
		
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(stopwordfile));
			String line = null;
			//in.readLine() is deprecated
			BufferedReader bffrrdr = new BufferedReader(new InputStreamReader(in));
			while ((line = bffrrdr.readLine()) != null)
				stopwords.add(line.trim().toLowerCase());
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stopwords;
	}
}
