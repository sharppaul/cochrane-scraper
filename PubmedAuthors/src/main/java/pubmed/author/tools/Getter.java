package pubmed.author.tools;

import java.io.*;
import java.net.*;

import org.apache.log4j.Logger;

public class Getter {
	final static Logger logger = Logger.getLogger(Getter.class);

	public static String get(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		boolean hasConnection = false;
		String line;
		BufferedReader rd;

		int attempts = 0;
		while (!hasConnection && attempts <= 3) {
			line = "";
			try {
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				rd.close();

				hasConnection = true;
			} catch (SocketException e) {
				logger.error(e.getMessage(), e);
			} catch (FileNotFoundException e) {
				attempts++;
				Thread.sleep(500);
				if (attempts > 3) {
					logger.error(e.getMessage(), e);
				}
			} catch (IOException e) {
				attempts = 4;
				logger.error(e.getMessage());
				return "{\"error\":" + conn.getResponseCode() + "}";
			} catch (Exception e) {
				attempts++;
				Thread.sleep(500);
				if (attempts > 3) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		return result.toString();
	}
}
