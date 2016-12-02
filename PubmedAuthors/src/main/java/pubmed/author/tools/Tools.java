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
	final static Logger logger = Logger.getLogger(Tools.class);
	
	//Convert byte array to SHA1 string. Not used anymore.
	public static String toSHA1(byte[] convertme) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage(), e);
		}
		return Tools.byteArrayToHexString(md.digest(convertme));
	}
	
	//Convert byte array to Hexadecimal string. Not used anymore.
	public static String byteArrayToHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
	
	//Reads small files into a string. It's intended for small files, reading big files with this might not be safe.
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
	// closed here. This method converts an IS to a readable string.
	@SuppressWarnings("resource")
	public static String convertStreamToString(java.io.InputStream is) {
		Scanner s = new Scanner(is);
		s = s.useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	// Removes accents from text. Used to be a different method, hence why it's in Tools.
	public static String removeAccents(String value) {
		return StringUtils.stripAccents(value);
	}
	
	// Zerofill a string to given length int. If length is lower than string length, nothing is added.
	public static String zeroFill(String in, int length){
		while(in.length() < length){
			in = "0" + in;
		}
		return in;
	}
}
