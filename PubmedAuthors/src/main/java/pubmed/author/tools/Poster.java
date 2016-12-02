package pubmed.author.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

public class Poster {
	private HttpClient httpclient;
	private HttpPost httppost;
	List<NameValuePair> params = new ArrayList<NameValuePair>();
	final static Logger logger = Logger.getLogger(Poster.class);

	// this class is basically a wrapper for a more extended HttpClient
	public Poster(String url) {
		httpclient = HttpClients.createDefault();
		httppost = new HttpPost(url);
	}

	// add simple http post parameter to Poster.
	public void addParameter(String key, String value) {
		params.add(new BasicNameValuePair(key, value));
	}

	public String execute() {
		// Execute and get the response.
		InputStream instream = null;
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				instream = entity.getContent();
			}
			return Tools.convertStreamToString(instream);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		try {
			instream.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return "";
	}
}
