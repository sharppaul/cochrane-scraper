package pubmed.author.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class Tools {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	final static Logger logger = Logger.getLogger(Tools.class);

	public static String toSHA1(byte[] convertme) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage(), e);
		}
		return Tools.byteArrayToHexString(md.digest(convertme));
	}

	public static String byteArrayToHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
	
	public static String readSmallFile(String filename) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(filename));
		try {
			
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    return sb.toString();
		} catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally {
			br.close();
		}
		return null;
	}

	// I ignore the resource warning, because the inputstream should be closed
	// after calling this method. It is not initialized here and should not be
	// closed here.
	@SuppressWarnings("resource")
	public static String convertStreamToString(java.io.InputStream is) {
		Scanner s = new Scanner(is);
		s = s.useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	public static String removeAccents(String value) {
		return StringUtils.stripAccents(value);
	}
}
