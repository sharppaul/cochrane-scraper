package pubmed.author;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import pubmed.author.sql.SQLConnection;
import pubmed.author.tools.Getter;
import pubmed.author.tools.Poster;

public class AuthorRetriever extends Thread {
	private String pubmedid;
	public boolean isDone = false;
	private String first;
	private String last;
	private String gender1;
	private String gender2;
	private String probability1 = "0.00";
	private String probability2 = "0.00";
	private SQLConnection sql;
	final static Logger logger = Logger.getLogger(AuthorRetriever.class);

	public AuthorRetriever(String pubmedid, SQLConnection con) {
		this.pubmedid = pubmedid;
		this.sql = con;
	}

	public void run() {
		try {
			if (getNames()) {
				if (getFirstName("first")) {
					if (authorInDatabase("first")) {
						logger.info("Gender for " + this.first + " (" + this.pubmedid + ") found in database.");
					} else {
						if (!getGender("first"))
							logger.error("FAIL:" + pubmedid + ": Retreiving gender for '" + first + "' failed.");
					}
				} else {
					logger.error("FAIL:" + pubmedid + ": Retreiving firstname for '" + first + "' failed.");
				}
				if (getFirstName("last")) {
					if (authorInDatabase("last")) {
						logger.info("Gender for " + this.last + " (" + this.pubmedid + ") found in database.");
					} else {
						if (!getGender("last"))
							logger.error("FAIL:" + pubmedid + ": Retreiving gender for '" + last + "' failed.");
					}
				} else {
					logger.error("FAIL:" + pubmedid + ": Retreiving firstname for '" + last + "' failed.");
				}
			}

			if (!sql.insertAuthors(this.pubmedid, this.first, this.last, this.gender1, this.gender2, this.probability1,
					this.probability2)) {
				logger.error("FAIL:" + pubmedid + ": Insert into database failed for: " + first + ", " + last);
			} else {
				logger.info("\n" + this.toString());
			}
			/*
			 * if (!getNames()) throw new Exception(
			 * "Failed to get AUTHOR NAMES from https://www.ncbi.nlm.nih.gov/pubmed/?term="
			 * + pubmedid + "[uid]"); if (!getFirstName(this.first)) throw new
			 * Exception("FAILED: No first name for "+ first); if
			 * (!getFirstName(this.last)) throw new Exception(
			 * "FAILED: No first name for "+ last); if
			 * (authorInDatabase("first") & authorInDatabase("last")) { //maybe
			 * say that they're in database... } else { if (!getGender("first"))
			 * throw new Exception(
			 * "Failed to get GENDERS from https://api.genderize.io/?name="); }
			 */

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

	private boolean getNames() {
		String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=" + pubmedid;
		Document doc = null;
		try {
			doc = Jsoup.parse(Getter.get(url));
			Elements authors = doc.getElementsByAttributeValue("Name", "Author");
			this.first = authors.first().html();
			this.last = doc.getElementsByAttributeValue("Name", "LastAuthor").first().html();
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
					doc = Jsoup.parse(getForFirstName(this.first));
				else
					doc = Jsoup.parse(getForFirstName(this.last));

				Elements authors = doc.getElementsByClass("general");
				Element tablerow = authors.get(3).getElementsByTag("tr").get(1);
				String result;
				if (tablerow.getElementsByTag("a").size() < 2) {
					result = tablerow.getElementsByTag("td").get(2).html();
				} else {
					result = tablerow.getElementsByTag("a").first().html();
				}

				if (toFirstname(result, true).isEmpty()) {
					System.out.println("Result (" + result + ") is empty.");
					String oldresult = result;
					tablerow = authors.get(3).getElementsByTag("tr").get(2);
					if (tablerow.getElementsByTag("a").size() < 2) {
						result = tablerow.getElementsByTag("td").get(2).html();
					} else {
						result = tablerow.getElementsByTag("a").first().html();
					}
					if (!oldresult.split(",")[0].equals(result.split(",")[0]) | toFirstname(result, true).isEmpty()) {
						result = oldresult;
					}
				}

				if (who.equals("first"))
					this.first = result;
				else
					this.last = result;

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
					JSONObject result = new JSONObject(
							Getter.get("https://api.genderize.io/?name=" + URLEncoder.encode(firstname, "UTF-8")));
					if (!result.get("gender").equals(null)) {
						if (who.equals("first")) {
							this.gender1 = result.getString("gender");
							this.probability1 = "" + result.get("probability");
						} else {
							this.gender2 = result.getString("gender");
							this.probability2 = "" + result.get("probability");
						}
					} else {
						if (who.equals("first")) {
							firstname = toFirstname(this.first, false);
						} else {
							firstname = toFirstname(this.last, false);
						}
						if (firstname != null && !firstname.isEmpty()) {
							result = new JSONObject(Getter
									.get("https://api.genderize.io/?name=" + URLEncoder.encode(firstname, "UTF-8")));
							if (!result.get("gender").equals(null)) {
								if (who.equals("first")) {
									this.gender1 = result.getString("gender");
									this.probability1 = "" + result.get("probability");
								} else {
									this.gender2 = result.getString("gender");
									this.probability2 = "" + result.get("probability");
								}
							} else {
								if (who.equals("first")) {
									this.gender1 = "notfound";
								} else {
									this.gender2 = "notfound";
								}
							}
						} else {
							if (who.equals("first")) {
								this.gender1 = "notfound";
							} else {
								this.gender2 = "notfound";
							}
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

	private String toFirstname(String name, boolean full) {
		if (name != null) {
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
		return "\tAuthors for " + this.pubmedid + ":\n\tFirst:\nAuthor:\t\t" + this.first + "\nGender:\t\t"
				+ this.gender1 + "\nProbability:\t" + this.probability1 + "\n\tLast:\nAuthor:\t\t" + this.last
				+ "\nGender:\t\t" + this.gender2 + "\nProbability:\t" + this.probability2;
	}
}
