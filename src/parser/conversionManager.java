package parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class conversionManager {
	static final String DELI = "\\Z"; //Delimiter
	static final int NTHREADS = 4; //Number of threads used
	static final String END = "END";
	static final int CAPACITY = 200; //Limit of LinkedBlockingQueue
	static final int SIZE = 15; //Pre-filled number of files
	static final long TIMEOUT = 100;
	static final Logger logger = Logger.getGlobal();
	  
	private int nThrds;
	private int cap;
	private int prefilled;
	
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
				parser.loadModalWords("modalWords.txt");
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
}