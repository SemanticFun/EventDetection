package topicDetection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Classe che si occupa di scrivere su file sia gli errori che i dati della console **/
public class WriteConsole {
	private SimpleDateFormat sdf = new SimpleDateFormat();

	/** Costruttore che scrive i dati della console**/
	public WriteConsole(String text) {
		try {
			File file = new File("/home/tomcat/keygraph/console.txt");
			//File file = new File("./console.txt");

			// Se il file non esiste lo si crea
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			fw.write(text);
			fw.close();
		} catch (Exception ie) {
			System.out.println("Attenzione! Non è possibile scrivere i dati su file");
		}
	}

	/** Costruttore che scrive gli errori su file**/
	public WriteConsole(Exception e, String position) {
		sdf.applyPattern("dd/MM/yy HH:mm");
		String dataStr = sdf.format(new Date());
		try {
			File file = new File("/home/tomcat/keygraph/errorLog.txt");
			//File file = new File("./errorLog.txt");

			// Se il file non esiste lo si crea
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(dataStr + " " + position + " ");
			PrintWriter pWriter = new PrintWriter(bw, true);
			e.printStackTrace(pWriter);
			bw.close();
			
		} catch (Exception ie) {
			new WriteConsole("Attenzione! Non è possibile scrivere l'errore su file");
		}
	}

}
