package cochrane.parser;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cochrane.parser.sql.SQLConnection;
import cochrane.parser.tools.Getter;

public class CochraneReferencesParser extends Thread {
	private String cochraneId;
	private String cochraneUrl;
	private String topic;
	private SQLConnection con;
	private String outputDir;
	private boolean outputToFile = false;

	public CochraneReferencesParser(String outputDir, String cochraneUrl, SQLConnection con, String topic) {
		String[] cIds = cochraneUrl.split("\\.|\\/");
		for (int i = 0; i < cIds.length; i++) {
			if (cIds[i].startsWith("CD") || cIds[i].startsWith("MR"))
				this.cochraneId = cIds[i];
		}

		this.con = con;
		this.topic = topic;
		this.cochraneUrl = cochraneUrl;
		this.outputDir = outputDir;
	}

	@Override
	public void run() {
		if (cochraneId == null) {
			System.err.println("FAILED \tparsing :: No ID found in URL: " + this.cochraneUrl);
			return;
		}

		System.out.println("START \tparsing " + this.cochraneId);
		PrintWriter out;
		String html = "";
		Document doc = null;

		try {
			html = Getter.getHTML(this.cochraneUrl);
			doc = Jsoup.parse(html);

			if (doc == null)
				throw new Exception("DOCUMENT parsed NULL");

			Element content = doc.getElementById(cochraneId + "-bbs1-0001");
			if (content != null) {
				Elements links = content.getElementsByTag("a");

				if (outputToFile) {

					File file = new File(this.outputDir + "\\" + cochraneId + ".out");
					file.getParentFile().mkdirs();
					out = new PrintWriter(file);
					out.println("PubMed references for Cochrane ID: " + cochraneId);
					for (Element link : links) {
						if (link.html().equals("PubMed")) {
							out.println("\t" + link.attr("href"));
						}
					}
					out.close();

				} else {

					ArrayList<String> referencedids = new ArrayList<String>();
					for (Element link : links) {
						if (link.html().equals("PubMed")) {
							String[] linkurl = link.attr("href").split("/");
							referencedids.add(linkurl[linkurl.length - 1]);
						}
					}
					
					con.insertReferences(cochraneId, referencedids, topic);
				}
			}
		} catch (Exception e) {
			System.err.println("FAILED \tparsing " + this.cochraneId + " :: " + e.getMessage().replace('\n', ' '));
			return;
		}
		System.out.println("SUCCESS \tparsing " + this.cochraneId);
	}
}
