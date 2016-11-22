package pubmed.author;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import pubmed.author.sql.SQLConnection;
import pubmed.author.tools.Getter;
import pubmed.author.tools.Poster;
import pubmed.author.tools.Tools;

public class AuthorRetriever extends Thread {
	private String pubmedid;
	public boolean isDone = false;
	private String title;
	private String year;
	private String first;
	private String last;
	private String gender1;
	private String gender2;
	private String probability1 = "0.00";
	private String probability2 = "0.00";
	private SQLConnection sql;
	final static Logger logger = Logger.getLogger(AuthorRetriever.class);
	private final String GENDERIZE_API_KEY;

	public AuthorRetriever(String pubmedid, SQLConnection con, String key) {
		this.pubmedid = pubmedid;
		this.sql = con;
		this.GENDERIZE_API_KEY = key;
	}

	public void run() {
		try {
			if (getPubmedData()) {
				if (getFirstName("first")) {
					if (authorInDatabase("first")) {
						logger.info("Gender for " + this.first + " (" + this.pubmedid + ") found in database.");
					} else {
						if (!getGender("first"))
							logger.error("Retreiving gender for '" + first + "' failed. " + this.pubmedid);
					}
				} else {
					logger.error("Retreiving firstname for '" + first + "' failed. " + this.pubmedid);
				}
				if (getFirstName("last")) {
					if (authorInDatabase("last")) {
						logger.info("Gender for " + this.last + " (" + this.pubmedid + ") found in database.");
					} else {
						if (!getGender("last"))
							logger.error("Retreiving gender for '" + last + "' failed. " + this.pubmedid);
					}
				} else {
					logger.error("Retreiving firstname for '" + last + "' failed. " + this.pubmedid);
				}
			}

			if (!sql.insertAuthors(this.pubmedid, this.first, this.last, this.gender1, this.gender2, this.probability1,
					this.probability2, this.title, this.year)) {
				logger.error("Insert into database failed for: " + first + ", " + last + " (" + this.pubmedid + ")");
			} else {
				logger.info("\n" + this.toString());
			}

			this.isDone = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			this.isDone = true;
		}
	}

	@SuppressWarnings("unused")
	private boolean isGroup(String input) {
		String[] matches = new String[] { "Network", "Group", "Team", "network", "group", "team" };
		for (String s : matches)
			if (input.contains(s))
				return true;
		return false;
	}

	private boolean authorInDatabase(String which) {
		String output = null;
		if (which.equals("first"))
			output = sql.checkForAuthor(this.first);
		else
			output = sql.checkForAuthor(this.last);

		if (output != null && !output.isEmpty() && !output.contains("null") && !output.equals("null#1.00")) {
			String[] out = output.split("#");
			if (which.equals("first")) {
				this.gender1 = out[0];
				this.probability1 = out[1];
			} else {
				this.gender2 = out[0];
				this.probability2 = out[1];
			}
			return true;
		}

		return false;
	}

	private boolean getPubmedData() {
		String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=" + pubmedid;
		Document doc = null;
		try {
			// Get the document from Entrez
			doc = Jsoup.parse(Getter.get(url));

			// Get the first and last author names from Pubmed Entrez
			Elements authors = doc.getElementsByAttributeValue("Name", "Author");
			this.first = authors.first().html();
			this.last = doc.getElementsByAttributeValue("Name", "LastAuthor").first().html();

			// Replaces foreign characters, makes all characters uppercase, and
			// changes the name to the format we use (comma in between)
			this.first = Tools.removeAccents(this.first.replaceAll("\\s(?=[A-Z]+$)", ", ").toUpperCase());
			this.last = Tools.removeAccents(this.last.replaceAll("\\s(?=[A-Z]+$)", ", ").toUpperCase());

			// Get title and year out from document.
			this.title = doc.getElementsByAttributeValue("Name", "Title").first().html();
			this.year = doc.getElementsByAttributeValue("Name", "PubDate").first().html();

			// Changes the obtained pubdate from Entrez to the year, with a
			// regular expression.
			Pattern p = Pattern.compile("\\d{4}");
			Matcher m = p.matcher(this.year);
			if (m.find()) {
				this.year = m.group();
			}
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage() + " URL: " + url, e);
		}
		return false;
	}

	private boolean getFirstName(String who) {
		int attempts = 0;
		while (attempts <= 3) {
			try {
				Document doc;
				if (who.equals("first"))
					doc = Jsoup.parse(getForFirstName(this.first.replace(",", "")));
				else
					doc = Jsoup.parse(getForFirstName(this.last.replace(",", "")));
				String result = getNameFromTable(doc, 0);
				if (!matchesLastname(who.equals("first") ? this.first : this.last, result)
						|| toFirstname(result, true).isEmpty())
					for (int i = 1; i < 3; i++) {
						String oldresult = result;
						result = getNameFromTable(doc, i);
						if (!matchesLastname(oldresult, result) || toFirstname(result, true).isEmpty()) {
							result = oldresult;
						}
					}
				if (who.equals("first")) {
					if (matchesLastname(this.first, result))
						this.first = result;
				} else {
					if (matchesLastname(this.last, result))
						this.last = result;
				}
				if (!result.equals("null") || result != null)
					return true;
			} catch (Exception e) {
				attempts++;
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					// ...
				}
			}
		}
		return false;
	}

	private String getNameFromTable(Element doc, int try_no) {
		Elements authors = doc.getElementsByClass("general");
		Element tablerow = authors.get(3).getElementsByTag("tr").get(1 + try_no);
		String result;
		if (tablerow.getElementsByTag("a").size() < 2) {
			result = tablerow.getElementsByTag("td").get(2).html();
		} else {
			result = tablerow.getElementsByTag("a").first().html();
		}
		return result;
	}

	private boolean matchesLastname(String str1, String str2) {
		return str1.toUpperCase().startsWith(str2.split(",")[0].toUpperCase());
	}

	private boolean getGender(String who) {
		try {
			String firstname;
			boolean goodToGo;
			if (who.equals("first")) {
				goodToGo = (this.gender1 == null || this.gender1.isEmpty() || this.gender1.equals("null"));
				firstname = toFirstname(this.first, true);
			} else {
				goodToGo = (this.gender2 == null || this.gender2.isEmpty() || this.gender2.equals("null"));
				firstname = toFirstname(this.last, true);
			}

			if (goodToGo) {
				if (firstname != null && firstname.length() > 1) {
					JSONObject result = getGenderJSON(firstname);
					
					
					if (result.has("gender") & result.get("gender") != null & result.get("gender") != JSONObject.NULL) {
						if (who.equals("first")) {
							this.gender1 = (String) result.get("gender");
							this.probability1 = "" + result.get("probability");
						} else {
							this.gender2 = (String) result.get("gender");
							this.probability2 = "" + result.get("probability");
						}
					} else {
						if (who.equals("first")) {
							firstname = toFirstname(this.first, false);
						} else {
							firstname = toFirstname(this.last, false);
						}
						result = getGenderJSON(firstname);
						if (result.has("gender") & result.get("gender") != null & result.get("gender") != JSONObject.NULL) {
							if (who.equals("first")) {
								this.gender1 =  (String) result.get("gender");
								this.probability1 = "" + result.get("probability");
							} else {
								this.gender2 = (String) result.get("gender");
								this.probability2 = "" + result.get("probability");
							}
						} else if (who.equals("first")) {
							this.gender1 = "notfound";
						} else {
							this.gender2 = "notfound";
						}
					}
					return true;
				} else {
					if (who.equals("first")) {
						this.gender1 = "notfound";
					} else {
						this.gender2 = "notfound";
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	private JSONObject getGenderJSON(String firstname) throws JSONException, UnsupportedEncodingException, Exception {
		final String GENDERIZE_BASE_URL = "https://api.genderize.io/";
		JSONObject result = new JSONObject(Getter.get(GENDERIZE_BASE_URL + "?name="
				+ URLEncoder.encode(firstname, "UTF-8") )); //+ "&apikey=" + this.GENDERIZE_API_KEY));
		if (result.has("error")) {
			logger.error("Genderize.io HTTP Error " + result.getInt("error"));
			switch (result.getInt("error")) {
			case 400:
				logger.error("Genderize.io: 400 idk error");
				break;
			case 401:
				logger.error("Genderize.io: Authorization error");
				break;
			case 402:
				logger.error("Genderize.io: Subscription error");
				break;
			case 429:
				logger.error("Genderize.io: Too many requests!");
				Thread.sleep(100);
				Scanner s = new Scanner(System.in);
				System.out.println(
						"Thread " + this.pubmedid + " halted, too many requests, type 'CONTINUE' to continue...");
				while (!s.nextLine().equals("CONTINUE"))
					System.out.println(
							"Thread " + this.pubmedid + " halted, too many requests, type 'CONTINUE' to continue...");
				break;
			case 500:
				logger.error("Genderize.io: Internal server error!");
				Thread.sleep(1000);
				// return getGender(who);
				break;
			}
		}
		return result;
	}

	private String toFirstname(String name, boolean full) {
		if (name != null) {
			try {
				String[] nameparts = name.split(",")[1].split(" ");
				String firstname = "";
				for (String i : nameparts) {
					if (i.length() > 1 && i.matches(".*[AEIOUaeiou].*"))
						if (full) {
							if (firstname.length() > 1)
								firstname += " " + i;
							else
								firstname += i;
						} else {
							return i;
						}
				}
				return firstname;
			} catch (ArrayIndexOutOfBoundsException e) {
				logger.error("Couldn't get firstname for " + name + " " + this.pubmedid);
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private boolean getGenders() {
		return (getGender("first") & getGender("last"));
	}

	private String getForFirstName(String name) {
		Poster post = new Poster("http://hgserver2.amc.nl/cgi-bin/miner/miner2.cgi");
		try {
			post.addParameter("query", URLEncoder.encode(name + "[AU]", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		post.addParameter("abstractlimit", "25000");
		post.addParameter("col.author", "full");
		post.addParameter("subauthor", "full");
		try {
			post.addParameter("subword", URLEncoder.encode("(ti)", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		post.addParameter("bool", "AND");
		post.addParameter("merge", "YES");
		post.addParameter("minimalcount", "2");
		post.addParameter("term", "");
		return post.execute();
	}

	public String toString() {
		return "\tAuthors for " + this.pubmedid + ": " + this.title + " (" + this.year + ")\n\tFirst:\nAuthor:\t\t"
				+ this.first + "\nGender:\t\t" + this.gender1 + "\nProbability:\t" + this.probability1
				+ "\n\tLast:\nAuthor:\t\t" + this.last + "\nGender:\t\t" + this.gender2 + "\nProbability:\t"
				+ this.probability2;
	}
}
