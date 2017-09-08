package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.CSVReader;

public class govNameExtractor {
	static final int NUMCOLS = 11;
	static final int TYPEIDX = 7;
	static final int IOBIDX = 8;
	static final int TEXTIDX = 4;
	
	private HashMap<String, LinkedList<String>> map;
	private String dir;
	
	public govNameExtractor(String dir) {
		this.map = new HashMap<>();
		this.dir = dir;
	}
	
	public void extractToFile(String filename) {
		File folder = new File(this.dir);
	    String[] fileList = folder.list();
	    for(int i = 0; i < fileList.length; i++)
	    	fileList[i] = this.dir + "/" + fileList[i];
	    
	    this.parseLine(fileList);
	    this.outputList(filename);
	}
	
	private void parseLine(String[] fileList) {
		for(int i = 0; i < fileList.length; i++) {
	    	try (CSVReader reader = new CSVReader(new InputStreamReader(new 
	    			FileInputStream(fileList[i]), 
	    			StandardCharsets.UTF_8.toString()))) {
	    		List<String[]> list = reader.readAll();
	    		ListIterator<String[]> it = list.listIterator();
	    		StringBuilder sb = new StringBuilder();
	    		String[] row = null;
	    		String id = null;
	    		while(it.hasNext()) {
	    			row = it.next();
	    			if(row.length != NUMCOLS) continue;
	    			if(!StringUtils.isNumeric(row[0])) continue;
	    			if(id == null) id = row[0];
	    			
	    			if(sb.length() != 0 && (!row[IOBIDX].equals("I"))) {
	    				this.updateList(sb.toString(), id);
	    				sb = new StringBuilder();
	    			}
	    			if(row[TYPEIDX].equals("GOV")) {
	    				sb.append(row[TEXTIDX]);
	    			}
	    		}
	    		if(sb.length() != 0)
	    			this.updateList(sb.toString(), id);
	    	} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}
	
	private void outputList(String filename) {
		try(BufferedWriter writer = new BufferedWriter(new
					OutputStreamWriter(new FileOutputStream(filename),
							StandardCharsets.UTF_8))) {
			Set<String> set = this.map.keySet();
			Iterator<String> it = set.iterator();
			while(it.hasNext()) {
				String key = it.next();
				writer.write(key);
				writer.write(" " + this.map.get(key).toString() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void updateList(String term, String id) {
		if(!this.map.containsKey(term)) {
			LinkedList<String> idList = new LinkedList<>();
			idList.add(id);
			this.map.put(term, idList);
		} else {
			LinkedList<String> idList = this.map.get(term);
			idList.add(id);
		}
	}
}