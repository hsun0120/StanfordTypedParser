package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import db.ADBConnection;

public class conversionManager {
	static final String DELI = "\\Z"; //Delimiter
	static final int NTHREADS = 4; //Number of threads used
	static final String END = "END";
	static final int CAPACITY = 200; //Limit of LinkedBlockingQueue
	static final int SIZE = 15; //Pre-filled number of files
	static final long TIMEOUT = 100;
	static final Logger logger = Logger.getGlobal();
	
	static final String USERNAME = "sdsc";
	static final String PASSWORD = "abc123";
	static final int TIMELIMITINSECONDS = 3000;
	  
	private int nThrds;
	private int cap;
	private int prefilled;
	
	private String server;
	private String dataverse;
	private String dataset;
	
	String path;
	
	public conversionManager() {
		this.nThrds = NTHREADS;
		this.cap = CAPACITY;
		this.prefilled = SIZE;
	}
	
	public conversionManager(int nThrds, int cap, int prefilled) {
		this.nThrds = nThrds;
		this.cap = cap;
		this.prefilled = prefilled;
	}
	
	public void setConnection(String server, String dataverse, String
			dataset) {
		this.server = server;
		this.dataverse = dataverse;
		this.dataset = dataset;
	}
	
	public void convert(List<String> fields, String path, String idField) 
			throws FileNotFoundException {
		if(server == null || this.dataset == null || this.dataset == null) {
			logger.severe("Missing connection and query information.");
			return;
		}
		this.path = path;
		
		final ExecutorService es = Executors.newFixedThreadPool(nThrds);
		LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>(cap);
		
		HttpURLConnection conn = this.connect(fields, idField);
		if(conn == null) return;
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), "UTF-8"));
			String inputLine;
			int count = 0;
			/* Fill the LinkedBlockingQueue with certain amount of data */
			while ((inputLine = in.readLine()) != null) {
				/* Parse results */
				if(inputLine.charAt(0) == '[' || inputLine.charAt(0) == ',')
					inputLine = inputLine.substring(1);
				if(inputLine.equals(" ]"))
					continue;
				lbq.put(inputLine);
				count++;
				if(count == this.prefilled) {
					/* Assign tasks to each thread */
					this.assignTasks(lbq, fields, idField, es);
				}
			}

			lbq.put(END);
			conn.disconnect();

			if(count < this.prefilled) {
				/* Assign tasks to each thread */
				this.assignTasks(lbq, fields, idField, es);
			}

			es.shutdown();
			es.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			logger.exiting("conversionManager", "convert", e);
		}
	}
	
	public void convert(String[] fileList, List<String> fields, String path,
			String idField) throws FileNotFoundException {
		final ExecutorService es = Executors.newFixedThreadPool(nThrds);
		LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>(cap);
		Scanner sc;
		try {
			int index = 0;
			/* Fill the LinkedBlockingQueue with certain amount of data */
			while(index < fileList.length && index < prefilled) {
				sc = new Scanner(new FileInputStream(fileList[index]),
						StandardCharsets.UTF_8.toString());
				sc.useDelimiter(DELI);
				lbq.put(sc.next());
				index++;
			}

			if(index == fileList.length)
				lbq.put(END);

			/* Assign tasks to each thread */
			for(int i = 0; i < nThrds; i++) {
				conversionParser parser = new conversionParser(lbq, fields, 
						path, idField);
				if(i == 0) {
					parser.loadModalWords("modalWords.txt");
					parser.loadGovList("gov_postfix.txt");
				}
				es.execute(parser);
			}

			/* Read all files and send next file to available thread */
			while(index < fileList.length) {
				sc = new Scanner(new FileInputStream(fileList[index]),
						StandardCharsets.UTF_8.toString());
				sc.useDelimiter(DELI);
				lbq.put(sc.next());
				index++;
			}
			lbq.put(END);

			es.shutdown();
			es.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	private HttpURLConnection connect(List<String> fields, String idField) {
		ADBConnection in = new ADBConnection(this.server);
		fields.add(idField);
		String handleUrl;
	    try {
	      
	      handleUrl = in.getResultURL(this.dataverse, this.dataset, fields);
	      /*
	      int status = 0;
	      while(status != 1) { //Polling until the results are ready
	        status = in.getStatus(handleUrl);
	        if(status == -1) {
	          logger.severe("Fail to retrieve data.");
	          return null;
	        }
	      }*/
	      /* Use handle to get results */
	      URL query = new URL(handleUrl);
	      HttpURLConnection conn = (HttpURLConnection) query.openConnection();
	      String encoding = Base64.getEncoder().encodeToString((USERNAME + ":" +
	    			PASSWORD).getBytes("UTF-8"));
	      conn.setRequestMethod("GET");
	      conn.setDoOutput(true);
	      conn.setRequestProperty("Authorization", "Basic " + encoding);
	      conn.setConnectTimeout(TIMELIMITINSECONDS * 1000);
	      conn.setReadTimeout(TIMELIMITINSECONDS * 1000);
	      return conn;
	    } catch (IOException e) {
	      logger.exiting("conversionManager", "connect", e);
	    }
		return null;
	}
	
	private void assignTasks(LinkedBlockingQueue<String> lbq, List<String> 
	fields, String idField, ExecutorService es) {
		for(int i = 0; i < nThrds; i++) {
			conversionParser parser = new conversionParser(lbq, fields, 
					this.path, idField);
			if(i == 0) {
				parser.loadModalWords("modalWords.txt");
				parser.loadGovList("gov_postfix.txt");
			}
			es.execute(parser);
		}
	}
}