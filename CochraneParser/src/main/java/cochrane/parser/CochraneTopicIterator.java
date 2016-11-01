package cochrane.parser;

import java.util.ArrayList;
import java.util.Arrays;
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

	public CochraneTopicIterator() {
		try {
			Document doc = Jsoup
					.parse(Getter.getHTML("http://www.cochranelibrary.com/home/topic-and-review-group-list.html"));

			Element section = doc.select("section[class='browse-block__section']").first();
			Elements links = section.select("a[class='browse-block__list-item-link']");

			for (Element link : links) {
				topics.add(link.html().replaceAll("&amp;", "&"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

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

	public void parseTopics(String outputDir) {
		Iterator<String> i = topics.iterator();
		while (i.hasNext()) {
			String topic = i.next();
			CochraneArticleIterator e = new CochraneArticleIterator(outputDir + "\\" + topic, URLEncoder.encode(topic));
			topicThreads.add(e);
			e.start();
		}
		// Checking if they're done:
		Iterator<CochraneArticleIterator> caii;
		boolean done = false;
		
		while (!done){
			//get size, check if topic is done, if so, decrease size, if all topics are done, size should be below 1. (0)
			caii = topicThreads.iterator();
			int topicslive = topics.size();
			while (caii.hasNext())
				if (caii.next().done) {
					topicslive--;
					if (topicslive < 1)
						done = true;
				}
		}
	}

	public void parseTopics(String outputDir, int[] indexes) {
		Iterator<String> i = topics.iterator();
		int x = 0;
		while (i.hasNext()) {
			x++;
			if (Arrays.asList(indexes).contains(x)) {
				String topic = i.next();
				new CochraneArticleIterator(outputDir + "\\" + topic, URLEncoder.encode(topic)).start();
			}
		}
	}
}
