package pubmed.author;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import pubmed.author.sql.SQLConnection;
import pubmed.author.tools.Tools;

public class Main {
	public final int LIVE_THREADS = 4;
	private AuthorRetriever[] authors = new AuthorRetriever[LIVE_THREADS];
	private ArrayList<String> list;
	private Iterator<String> it;
	private SQLConnection con;
	private String genderize_key;
	private final boolean TESTING = false;
	private final String[] testpubmedid = {"19489765","24953576"};
	final static Logger logger = Logger.getLogger(Main.class);
	
	public static void main(String[] args) {
		try {
			//new Main();
			System.out.println(Tools.ANSI_GREEN+"green!"+Tools.ANSI_RESET+" Normal."+Tools.ANSI_BLUE+" Blue!");
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}

	public Main() throws InterruptedException, IOException {
		logger.info("MAIN THREAD: START");
		con = new SQLConnection("localhost", "3306", "paul", "paul", "[paul3514]");
		con.open();
		list = con.getReferences();
		it = list.iterator();

		genderize_key = Tools.readSmallFile(System.getProperty("user.home")+"/.genderize_key");
		
		if(this.TESTING){
			test();
		} else {
			run();
		}
		
		while (!isDone()) {
			Thread.sleep(40);
		}
		con.close();
		logger.info("MAIN THREAD: DONE");
	}
	
	private void test(){
		authors[0] = new AuthorRetriever(this.testpubmedid[0], this.con, this.genderize_key);
		authors[0].start();
	}
	
	private void run() throws InterruptedException {
		while (it.hasNext()) {
			for (int i = 0; i < authors.length; i++) {
				if(null == authors[i] || authors[i].isDone) {
					authors[i] = new AuthorRetriever(it.next(), this.con, this.genderize_key);
					authors[i].start();
				}
				Thread.sleep(100);
			}
		}
	}

	public boolean isDone() {
		if (it.hasNext() && !this.TESTING)
			return false;
		for (AuthorRetriever auth : authors) {
			if (auth != null && !auth.isDone)
				return false;
		}
		return true;
	}
}
