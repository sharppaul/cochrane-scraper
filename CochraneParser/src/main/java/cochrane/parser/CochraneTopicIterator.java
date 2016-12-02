package cochrane.parser;

import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cochrane.parser.tools.Getter;
import cochrane.parser.tools.URLEncoder;

public class CochraneTopicIterator {
	public ArrayList<String> topics = new ArrayList<String>();
	public ArrayList<CochraneArticleIterator> topicThreads = new ArrayList<CochraneArticleIterator>();
	private final int MAX_THREADS = 4;

	public CochraneTopicIterator() {
		try {
			//get the topic page from cochrane library.
			Document doc = Jsoup
					.parse(Getter.get("http://www.cochranelibrary.com/home/topic-and-review-group-list.html"));

			Element section = doc.select("section[class='browse-block__section']").first();
			Elements links = section.select("a[class='browse-block__list-item-link']");
			//put all links in an ArrayList
			for (Element link : links) {
				topics.add(link.html().replaceAll("&amp;", "&"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		//print all topics
		System.out.println("Topics found: ");
		Iterator<String> i = topics.iterator();
		if (!i.hasNext())
			System.out.println("\tNONE");
		int x = 0;
		while (i.hasNext()) {
			x++;
			System.out.println("\t" + x + " " + i.next());
		}
	}

	// parse a single topic.
	public void parseTopic(String outputDir, int t) {
		Iterator<String> i = topics.iterator();
		CochraneArticleIterator cai = new CochraneArticleIterator(outputDir + "/none", URLEncoder.encode("none"));
		cai.done = true;
		int x = 1;
		// iterates through topics, untill the chosen one is found
		while (i.hasNext()) {
			String topic = i.next();
			if (x == t) {
				cai = new CochraneArticleIterator(outputDir + "/" + topic, URLEncoder.encode(topic));
				cai.start();
			}
			x++;
		}
		// keep track of topic, checks if it's done.
		while (!cai.done) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}
		System.out.println("DONE WITH ALL TOPIC(s)");
	}

	// parse all topics
	public void parseTopics(String outputDir) {
		Iterator<String> i = topics.iterator();
		int liveThreads = 0;
		while (i.hasNext()) {
			if (liveThreads <= MAX_THREADS) {
				String topic = i.next();
				CochraneArticleIterator e = new CochraneArticleIterator(outputDir + "/" + topic,
						URLEncoder.encode(topic));
				topicThreads.add(e);
				e.start();
				liveThreads++;
			} else {
				int threadcount = topicThreads.size();
				for (CochraneArticleIterator e : topicThreads) {
					if (e.done)
						threadcount--;
				}
				liveThreads = threadcount;
			}
		}
		// Checking if threads are done:
		Iterator<CochraneArticleIterator> caii;
		boolean done = false;

		while (!done) {
			// get size, check if topic is done, if so, decrease size, if all
			// topics are done, size should be below 1. (0)
			caii = topicThreads.iterator();
			int topicslive = topics.size();
			while (caii.hasNext())
				if (caii.next().done) {
					topicslive--;
					if (topicslive < 1)
						done = true;
				}
		}
		System.out.println("DONE WITH ALL TOPIC(s)");
	}
}
