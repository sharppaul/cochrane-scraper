package pubmed.author.tools;

import java.io.*;
import java.net.*;

public class Getter {
	public static String getHTML(String urlToRead) throws Exception {
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

			} catch (FileNotFoundException e) {
				attempts++;
				Thread.sleep(50);
			} catch (Exception e) {
				attempts++;
				Thread.sleep(50);
			}
		}

		return result.toString();
	}
}
