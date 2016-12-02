package pubmed.author;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import pubmed.author.sql.SQLConnection;
import pubmed.author.tools.Tools;

public class Main {
	public final int LIVE_THREADS = 8;
	private AuthorRetriever[] authors = new AuthorRetriever[LIVE_THREADS];
	private ArrayList<String> list;
	private Iterator<String> it;
	private SQLConnection con;
	private String genderize_key;
	private final boolean TESTING = false;
	private final String[] testpubmedid = { "19489765", "24953576" };
	private int length;
	final static Logger logger = Logger.getLogger(Main.class);
	private boolean START_AT_INDEX;
	private int START_INDEX;

	public static void main(String[] args) {
		// create new main instance
		try {
			// if there are any arguments, use other constructor.
			if (args.length > 0)
				new Main(args[0]);
			else
				new Main();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Main(String arg1) throws InterruptedException, IOException {
		// catching the only possible argument: startAt<int>, a bit quick and
		// dirty, but it works for this application.
		this.START_AT_INDEX = arg1.startsWith("startAt");
		if (this.START_AT_INDEX)
			this.START_INDEX = Integer.parseInt(arg1.substring("startAt".length()));

		this.start();
	}

	public Main() throws InterruptedException, IOException {
		this.start();
	}

	private void start() throws IOException, InterruptedException {
		logger.info("MAIN THREAD: START");
		// create SQL connection and open it.
		con = new SQLConnection("localhost", "3306", "paul", "paul", "[paul3514]");
		con.open();

		// get list of reference PubMed ID's and it's iterator
		list = con.getReferences();
		it = list.iterator();
		this.length = list.size();

		// get genderize.io key from a file in home directory. (For obvious
		// security reasons, since it's on github)
		genderize_key = Tools.readSmallFile(System.getProperty("user.home") + "/.genderize_key");
		logger.info("Genderize key: " + genderize_key);

		// checks if we're testing or not
		if (this.TESTING) {
			test();
		} else {
			run();
		}

		while (!isDone()) {
			Thread.sleep(40);
			// calms down the while loop. probably isn't really needed but just
			// for good measures.
		}

		// closes the SQL connection
		con.close();
		logger.info("MAIN THREAD: DONE");
	}

	// Test with one pubmedID supplied in this.pubmedid
	private void test() {
		authors[0] = new AuthorRetriever(this.testpubmedid[0], this.con, this.genderize_key);
		authors[0].start();
	}

	// Iterate and retrieve genders and firstnames etc. with authors.length
	// amount of threads at once.
	private void run() throws InterruptedException {
		int index = 0;

		// check if we should start at a certain index (e.g. when it was killed
		// during execution)
		if (this.START_AT_INDEX) {
			logger.info("Starting at:");
			for (int i = 0; i < this.START_INDEX; index++, i++) {
				if (it.hasNext()) {
					it.next();
				} else {
					break;
				}
			}
			logger.info(index);
		}

		// common iterator iteration.
		while (it.hasNext()) {
			for (int i = 0; i < authors.length; i++) {
				// check if one of the threads is null, died or done, if so, it
				// creates a new one. With a maximum of authors.length threads
				// at once.
				if ((null == authors[i] || authors[i].isDone) && it.hasNext()) {
					authors[i] = new AuthorRetriever(it.next(), this.con, this.genderize_key);
					authors[i].start();
					index++;
					logger.info("No. " + index + "/" + this.length + " has started.");
				}
				Thread.sleep(100);
			}
		}
	}

	// checks if iterator at it's end and if all threads are done.
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
