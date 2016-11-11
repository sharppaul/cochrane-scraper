package cochrane.parser.tools;

import java.io.*;
import java.net.*;

public class Getter {
	public static String get(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		boolean hasConnection = false;
		String line;
		BufferedReader rd;
		
		while(!hasConnection){
			line = "";
			try{
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				rd.close();
				hasConnection = true;
			}catch(SocketException e){
				
			}
		}
		
		
		return result.toString();
	}
}
