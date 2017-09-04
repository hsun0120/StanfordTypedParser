package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;

public class demoConversionParser {
	public static void main(String[] args) {
	    LinkedList<String> fields = new LinkedList<>();
	    fields.add("holding");
	    fields.add("facts");
	    fields.add("parties");
	    
	    conversionManager cm = new conversionManager();
	    File folder = new File(args[0]);
	    String[] fileList = folder.list();
	    for(int i = 0; i < fileList.length; i++)
	    	fileList[i] = args[0] + "/" + fileList[i];
	    
	    cm.setConnection("awesome.sdsc.edu", "courtCase", "CourtDoc");
	    long start = System.nanoTime();
	    try {
			//cm.convert(fields, args[0], "file_id");
	    	cm.convert(fileList, fields, args[1], "file_id");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	    long end = System.nanoTime();
	    System.out.println("Total time: " + (end - start) + "ns.");
	}
}