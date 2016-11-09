package pubmed.author.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class Tools {
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

	// I ignore the resource warning, because the inputstream should be closed
	// after calling this method. It is not initialized here and should not be
	// closed here.
	@SuppressWarnings("resource")
	public static String convertStreamToString(java.io.InputStream is) {
		Scanner s = new Scanner(is);
		s = s.useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}
}
