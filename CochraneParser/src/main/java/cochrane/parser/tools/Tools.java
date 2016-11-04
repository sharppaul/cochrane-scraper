package cochrane.parser.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Tools {
	public static String toSHA1(byte[] convertme) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
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
	
	@SuppressWarnings("resource")
	public static String convertStreamToString(java.io.InputStream is) {
	    Scanner s = new Scanner(is);
	    s = s.useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
}
