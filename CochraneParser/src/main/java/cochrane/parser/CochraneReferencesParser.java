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
			if (cIds[i].startsWith("pub") && this.cochraneId != null) {
				this.cochraneId += "." + cIds[i];
			}
		}

		this.con = con;
		this.topic = topic;
		this.cochraneUrl = cochraneUrl;
		this.outputDir = outputDir;
	}

	@Override
	public void run() {
		if (cochraneId == null) {
			System.err.println("FAIL  \tparsing :: No ID found in URL: " + this.cochraneUrl);
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
			// cochraneid-bbs1-0001 is the id of the references to studies
			Element referencescontent = doc.getElementById(cochraneId + "-bbs1-0001");
			// cochraneid-sec-0011 is the ID of the "data and analyses" item.
			Element datacontent = doc.getElementById(cochraneId + "-sec1-0011");
			if (datacontent == null) {
				System.out.println("SKIPPED_SUCCESS: " + this.cochraneId + " No 'Data and analyses' found.");
				return;
			}
			if (referencescontent == null) {
				System.out.println("SKIPPED_SUCCESS: " + this.cochraneId + " No 'References' found.");
				return;
			}
			
			Elements datalinks = datacontent.getElementsByTag("a");
			for(Element link : datalinks){
				if (link.html().equals("Download statistical data")) {
					String rm5link = "http://onlinelibrary.wiley.com" + link.attr("href");
					//TODO: download the RM5 files from the rm5link. It's a post request with a hidden link & checkbox. Hidden link is the download URL.
				}
			}
			
			
			Elements referencelinks = referencescontent.getElementsByTag("a");
			ArrayList<String> referencedids = new ArrayList<String>();
			for (Element link : referencelinks) {
				if (link.html().equals("PubMed")) {
					String[] linkurl = link.attr("href").split("/");
					referencedids.add(linkurl[linkurl.length - 1]);
				}
			}

			con.insertReferences(cochraneId, referencedids, topic);

			if (outputToFile) {
				File file = new File(this.outputDir + "\\" + cochraneId + ".out");
				file.getParentFile().mkdirs();
				out = new PrintWriter(file);
				out.println("#PubMed references for Cochrane ID: " + cochraneId);
				for (String id : referencedids) {
						out.println(id);
				}
				out.close();
			}

		} catch (Exception e) {
			System.err.println("FAILED \tparsing " + this.cochraneId + " :: " + e.getMessage().replace('\n', ' '));
			return;
		}
		System.out.println("SUCCESS \tparsing " + this.cochraneId);
	}
}
