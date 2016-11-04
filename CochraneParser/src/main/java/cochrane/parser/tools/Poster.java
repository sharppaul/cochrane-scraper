package cochrane.parser.tools;

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

public class Poster {
	private HttpClient httpclient;
	private HttpPost httppost;
	List<NameValuePair> params = new ArrayList<NameValuePair>();

	public Poster(String url) {
		httpclient = HttpClients.createDefault();
		httppost = new HttpPost(url);
	}

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
			e.printStackTrace();
		} finally {
			try {
				instream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		
		return "";
	}
}
