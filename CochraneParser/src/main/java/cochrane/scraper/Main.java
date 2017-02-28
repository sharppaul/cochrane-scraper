/*
 * written by Paul Weerheim
 * Use this in any way you want, just don't hold me responsible for anything (unless it's positive)
 * finished on 13-9-2016
 */

package cochrane.scraper;

import java.util.Scanner;

public class Main {
	public static void main(String[] args) {
		//gets all topics and asks user if we should iterate through each.
		CochraneTopicIterator cti = new CochraneTopicIterator();
		if ((args.length > 0) ? args[0].equals("-start") : false) {
			System.out.println("Starting...");
			cti.parseTopics(System.getProperty("user.home") + "\\parserOutput");
		} else {
			Scanner scan = new Scanner(System.in);
			System.out.println("Parse all topics? (Y/n)?");
			String str1 = scan.next();
			if (str1.equalsIgnoreCase("Y")) {
				System.out.println("Are you sure? (Y/n)?");
				String str2 = scan.next();
				if (str2.equalsIgnoreCase("Y")) {
					System.out.println("Starting...");
					cti.parseTopics(System.getProperty("user.home") + "/parserOutput");
					//cti.parseTopic(System.getProperty("user.home") + "/parserOutput",2);
				}
			} 
			scan.close();
		}
	}
}
