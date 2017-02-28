package cochrane.scraper;

import java.io.File;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cochrane.scraper.sql.SQLConnection;
import cochrane.scraper.tools.Getter;

public class CochraneReferencesParser extends Thread {
	private String cochraneId;
	private String cochraneUrl;
	private String topic;
	private SQLConnection con;
	private String outputDir;
	private boolean outputToFile = false;
	private String cochraneIdName;

	public CochraneReferencesParser(String outputDir, String cochraneUrl, SQLConnection con, String topic) {
		String[] cIds = cochraneUrl.split("\\.|\\/");
		for (int i = 0; i < cIds.length; i++) {
			if (cIds[i].startsWith("CD") || cIds[i].startsWith("MR"))
				this.cochraneIdName = this.cochraneId = cIds[i];
			if (cIds[i].startsWith("pub") && this.cochraneId != null) {
				this.cochraneIdName += "." + cIds[i];
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
			System.err.println("FAIL  \t\tparsing :: No ID found in URL: " + this.cochraneUrl);
			return;
		}

		System.out.println("START \t\tparsing " + this.cochraneIdName);
		PrintWriter out;
		String html = "";
		Document doc = null;

		try {
			html = Getter.get(this.cochraneUrl);
			doc = Jsoup.parse(html);

			if (doc == null)
				throw new Exception("DOCUMENT parsed NULL");

			// cochraneid-bbs1-0001 is the id of the references to studies
			Element referencescontent = null; // doc.getElementById(cochraneId +
												// "-bbs1-0001"); // easy way,
												// less fail proof

			// cochraneid-sec-0011 is the ID of the "data and analyses" item.
			Element datacontent = null;// = doc.getElementById(cochraneId +
										// "-sec1-0011"); // easy way, less fail
										// proof

			// look for references included in the footer
			Elements footer_sections = doc.getElementById(cochraneId + "-bibl-0001").getElementsByTag("div");
			for (Element e : footer_sections) {
				if (e.html()
						.startsWith("<h4 class=\"reference__title\">References to studies included in this review")) {
					referencescontent = e;
				}
			}

			// look for data and analyses
			Elements data_sections = doc.getElementById("main-content").getElementsByTag("section");
			for (Element e : data_sections) {
				if (e.html().startsWith("<h2>Data and analyses")) {
					datacontent = e;
				}
			}

			// checks if we should continue, if there's no data, return
			if (datacontent == null) {
				System.out.println("SUCCESS \tNo 'Data and analyses' found. " + this.cochraneIdName);
				return;
			}

			// checks if we should continue, if there's no references, return
			if (referencescontent == null) {
				System.out.println("SUCCESS \tNo 'Included references' found. " + this.cochraneIdName);
				return;
			}

			String linkrm5 = null;
			Elements datalinks = datacontent.getElementsByTag("a");
			for (Element link : datalinks) {

				if (link.html().equals("Download statistical data")) {
					// got the download statistical data link (aka. rm5 file)

					try {
						linkrm5 = URLDecoder.decode(link.attr("href").split("downloadLink=")[1], "UTF-8")
								.replaceAll("&amp;", "&");
						// decode URL decode it to retrieve the link
					} catch (Exception e) {
						e.printStackTrace();
						// try/catch for the arrayoutofbounds and other
						// unexpected errors which may not be fatal.
					}
					
					//download RM5 and save it to file.
					System.out.println("\tFOUND RM5:" + linkrm5);
					File file = new File(this.outputDir + "/" + cochraneIdName + ".rm5");
					file.getParentFile().mkdirs();
					out = new PrintWriter(file);
					out.print(Getter.get(linkrm5));
					out.close();
					break;
				}
			}
			if (linkrm5 == null)
				System.out.println("\tDID NOT FIND RM5LINK");

			Elements referencelinks = referencescontent.getElementsByTag("a");
			ArrayList<String> referencedids = new ArrayList<String>();
			for (Element link : referencelinks) {
				if (link.html().equals("PubMed")) {
					String[] linkurl = link.attr("href").split("/");
					referencedids.add(linkurl[linkurl.length - 1]);
				}
			}

			con.insertReferences(cochraneIdName, referencedids, topic);

			// if we should output to a file (even if we already output to
			// mysql)
			if (outputToFile) {
				File file = new File(this.outputDir + "/" + cochraneIdName + ".out");
				file.getParentFile().mkdirs();
				out = new PrintWriter(file);
				out.println("#PubMed references for Cochrane ID: " + cochraneIdName);
				for (String id : referencedids) {
					out.println(id);
				}
				out.close();
			}

		} catch (Exception e) {
			System.err.println("FAILED \tparsing " + this.cochraneIdName + ": " + e + ", " + e.getMessage());
			e.printStackTrace();
			return;
		}
		System.out.println("SUCCESS \tparsing " + this.cochraneIdName);
	}
}
