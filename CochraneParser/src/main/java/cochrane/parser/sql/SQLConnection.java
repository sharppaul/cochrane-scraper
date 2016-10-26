package cochrane.parser.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;


public class SQLConnection {

	private String url = null;
	private String user = null;
	private String password = null;
	private Connection con = null;

	public static SQLConnection local = new SQLConnection("localhost", "3306", "cochranereferences", "root", "");

	public SQLConnection(String IP, String port, String dbname, String username, String password) {
		this.url = "jdbc:mysql://" + IP + ":" + port + "/" + dbname;
		this.user = username;
		this.password = password;
	}

	public synchronized boolean insertReferences(String cochraneId, ArrayList<String> pubmedId, String topic) {
		Statement st = null;
		String query = null;
		try {
			st = con.createStatement();
			query = "REPLACE INTO paul.refs (cochrane_id, reference_pubmed_id, topic) VALUES";

			Iterator<String> it = pubmedId.iterator();
			while (it.hasNext()) {
				String id = it.next();
				query += " (\"" + cochraneId + "\", \"" + (id.length()>14?"notfound":id) + "\", \"" + topic + "\")";
				if (it.hasNext())
					query += ",";
			}
			if (!pubmedId.isEmpty())
				st.executeUpdate(query);
			return true;
		} catch (SQLException ex) {
			System.err.println(ex.getLocalizedMessage() + "\n" + query);
		} finally {
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
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
				System.out.println(rs.getString(1));
		} catch (SQLException ex) {
			ex.printStackTrace();
		} finally {
			try {

				if (rs != null) {
					rs.close();
				}

				if (st != null) {
					st.close();
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	public synchronized boolean open() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(url, user, password);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	public synchronized boolean close() {
		try {
			con.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}