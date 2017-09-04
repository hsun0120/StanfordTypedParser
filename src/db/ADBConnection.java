package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

public class ADBConnection {
	private static final String QUERY_TEMPLATE = "use dataverse %s;"
			+ "for $i in dataset('%s') limit 10 return {%s};";
	private static final String END_POINT_SUCCESS =
			"{\"status\":\"SUCCESS\"}";
	private static final String END_POINT_ERR = "{\"status\":\"ERROR\"}";
	static final String DATAVERSE = "use dataverse %s;";
	static final String PREFIX = "https://";
	static final Logger logger = Logger.getGlobal();
	
	static final String USERNAME = "sdsc";
	static final String PASSWORD = "abc123";
	static final int TIMELIMITINSECONDS = 3000;

	private static int RSPOK = 200;

	private String serverIp;

	/**
	 * Constructor for AQuery
	 * @param serverIp - server IP address
	 */
	public ADBConnection(String serverIp) {
		this.serverIp = serverIp;
	}

	/**
	 * Get the URL for asynchronous query results.
	 * @param dataverse - dataverse name
	 * @param dataset - dataset name
	 * @param doc - object name for court cases
	 * @param fields - fields to extract
	 * @return An URL to get results from
	 * @throws IOException
	 */
	public String getResultURL(String dataverse, String dataset, List<String> 
	fields) throws IOException {
		String param = "";
		if(fields.isEmpty()) return null;

		/* Build query string for return object */
		ListIterator<String> it = fields.listIterator();
		while(it.hasNext()) {
			String field = it.next();
			param += "\"" + field + "\": " + "$i." + field;
			if(it.hasNext())
				param += ", ";
		}
		String query = URLEncoder.encode(String.format(QUERY_TEMPLATE, 
				dataverse, dataset, param), "UTF-8");

		/* Form a complete query url */
		String queryUrl = PREFIX + serverIp + ":17002/query?query=" + query;
		//String handle = this.getHandle(queryUrl); //Obtain a handle
		return queryUrl;
		//PREFIX + serverIp + ":17002/query/result?handle=" + handle;
	}

	/**
	 * Check if database is ready for query result.
	 * @param handle - handle for the most recent query
	 * @return 1 - query results are ready
	 *         0 - query is still running
	 *         -1 - error
	 * @throws IOException
	 */
	public int getStatus(String handle) throws IOException {
		String status = handle.replace("result", "status");
		URL url = new URL(status);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		this.setAuth(conn);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null) 
			sb.append(inputLine);
		in.close();
		String endpoint = sb.toString();
		if(endpoint.equals(END_POINT_SUCCESS))
			return 1;
		else if(endpoint.equals(END_POINT_ERR))
			return -1;
		else
			return 0;
	}

	/**
	 * Connect to database and return the handle.
	 * @param requestUrl - query URL
	 * @return handle
	 * @throws IOException
	 */
	private String getHandle(String requestUrl) throws IOException {
		/* Connect to database */
		URL url = new URL(requestUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		this.setAuth(conn);
		if (conn.getResponseCode() != RSPOK) {
			throw new IOException("Failed : HTTP error code : "
					+ conn.getResponseCode());
		}
		/* Read handle string from the url connection */
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null) 
			sb.append(inputLine);
		in.close();
		conn.disconnect();
		return sb.toString().replaceAll(" ", "%20");
	}
	
	private void setAuth(HttpURLConnection conn) throws
	UnsupportedEncodingException, ProtocolException {
		String encoding = Base64.getEncoder().encodeToString((USERNAME + ":" +
				PASSWORD).getBytes("UTF-8"));
		conn.setRequestMethod("GET");
		conn.setDoOutput(true);
		conn.setRequestProperty("Authorization", "Basic " + encoding);
		conn.setConnectTimeout(TIMELIMITINSECONDS * 1000);
		conn.setReadTimeout(TIMELIMITINSECONDS * 1000);
	}
}