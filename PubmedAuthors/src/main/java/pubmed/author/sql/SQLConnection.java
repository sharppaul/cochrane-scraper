package pubmed.author.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class SQLConnection {

	private String url = null;
	private String user = null;
	private String password = null;
	private Connection con = null;
	final static Logger logger = Logger.getLogger(SQLConnection.class);

	public static SQLConnection local = new SQLConnection("localhost", "3306", "paul", "paul", "[paul3514]");

	public SQLConnection(String IP, String port, String dbname, String username, String password) {
		this.url = "jdbc:mysql://" + IP + ":" + port + "/" + dbname + "?autoReconnect=true&useSSL=false";
		this.user = username;
		this.password = password;
	}

	public synchronized ArrayList<String> getReferences() {
		ArrayList<String> list = new ArrayList<String>();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery("SELECT DISTINCT reference_pubmed_id FROM refs ORDER BY reference_pubmed_id ASC;");
			while (rs.next())
				list.add(rs.getString(1));
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			try {

				if (rs != null) {
					rs.close();
				}

				if (st != null) {
					st.close();
				}
			} catch (SQLException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		return list;
	}

	public synchronized String checkForAuthor(String authorName) {
		PreparedStatement st = null;
		ResultSet rs = null;
		String firstName = authorName.split(", ")[1];
		try {
			// look for first author, full name
			st = con.prepareStatement(
					"SELECT first_gender, first_probability FROM refs_author WHERE author_first = ?;");
			st.setString(1, authorName);
			rs = st.executeQuery();
			if (rs.next()) {
				return (rs.getString(1) + "#" + rs.getString(2));
			}
			st.close();
			rs.close();
			// look for first author, first name
			st = con.prepareStatement(
					"SELECT first_gender, first_probability FROM refs_author WHERE author_first LIKE ?;");
			st.setString(1, "%, " + firstName);
			rs = st.executeQuery();
			if (rs.next()) {
				return (rs.getString(1) + "#" + rs.getString(2));
			}
			st.close();
			rs.close();

			// look for last author, full name
			st = con.prepareStatement("SELECT last_gender, last_probability FROM refs_author WHERE author_last = ?;");
			st.setString(1, authorName);
			rs = st.executeQuery();
			if (rs.next()) {
				return (rs.getString(1) + "#" + rs.getString(2));
			}
			// look for last author, first name
			st.close();
			rs.close();
			st = con.prepareStatement(
					"SELECT last_gender, last_probability FROM refs_author WHERE author_last LIKE ?;");
			
			st.setString(1, "%, " + firstName);
			rs = st.executeQuery();
			if (rs.next()) {
				return (rs.getString(1) + "#" + rs.getString(2));
			}
			st.close();
			rs.close();
			return null;
		} catch (SQLException ex) {
			logger.error(ex.getLocalizedMessage() + "\n" + st.toString());
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		return null;
	}

	public synchronized boolean insertAuthors(String pubmedId, String first, String last, String gender1,
			String gender2, String probability1, String probability2, String title, String year) {
		PreparedStatement st = null;
		try {
			st = con.prepareStatement(
					"REPLACE INTO paul.`refs_author` (pubmed_id, author_first, author_last, first_gender, last_gender, first_probability, last_probability, title, year) VALUES (?,?,?,?,?,?,?,?,?)");
			st.setString(1, pubmedId);
			st.setString(2, first);
			st.setString(3, last);
			st.setString(4, gender1);
			st.setString(5, gender2);
			st.setString(6, probability1);
			st.setString(7, probability2);
			st.setString(8, title);
			st.setString(9, year);
			st.executeUpdate();
			return true;
		} catch (SQLException ex) {
			logger.error(ex.getMessage() + "\n" + st.toString(), ex);
		} finally {
			try {

				if (st != null) {
					st.close();
				}
			} catch (SQLException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
		return false;
	}

	public void getVersion() {
		Statement st = null;
		ResultSet rs = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery("SELECT VERSION()");
			if (rs.next())
				logger.info(rs.getString(1));
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			try {

				if (rs != null) {
					rs.close();
				}

				if (st != null) {
					st.close();
				}
			} catch (SQLException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
	}

	public synchronized boolean open() {
		try {
			con = DriverManager.getConnection(url, user, password);
			return true;
		} catch (SQLException e) {
			logger.error(url + ", " + user + ":" + password, e);
			return false;
		}
	}

	public synchronized boolean close() {
		try {
			con.close();
			return true;
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}
}