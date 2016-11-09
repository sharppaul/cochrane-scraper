package pubmed.author;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import pubmed.author.sql.SQLConnection;

public class Main {
	public final int LIVE_THREADS = 4;
	private AuthorRetriever[] authors = new AuthorRetriever[LIVE_THREADS];
	private ArrayList<String> list;
	private Iterator<String> it;
	private SQLConnection con;
	private final boolean TESTING = true;
	final static Logger logger = Logger.getLogger(Main.class);
	
	public static void main(String[] args) {
		try {
			new Main();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}

	public Main() throws InterruptedException {
		logger.info("MAIN THREAD: START");
		con = new SQLConnection("localhost", "3306", "paul", "paul", "[paul3514]");
		con.open();
		list = con.getReferences();
		it = list.iterator();
		
		
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
		authors[0] = new AuthorRetriever("27132542", this.con);
		authors[0].start();
	}
	
	private void run() throws InterruptedException {
		while (it.hasNext()) {
			for (int i = 0; i < authors.length; i++) {
				if(null == authors[i] || authors[i].isDone) {
					authors[i] = new AuthorRetriever(it.next(), this.con);
					authors[i].start();
				}
				Thread.sleep(100);
			}
		}
	}

	@SuppressWarnings("unused")
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
