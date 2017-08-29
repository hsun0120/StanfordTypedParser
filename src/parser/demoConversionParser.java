package parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import typedDependency.StanfordTypedDependency;

public class demoConversionParser {
	public static void main(String[] args) {
		conversionParser parser = new conversionParser();
		parser.loadModalWords("modalWords.txt");
		File folder = new File(args[0]);
	    String[] fileList = folder.list(); //Get all the files of the source folder
	    Scanner sc;
		  for(int i = 0; i < fileList.length; i++) {
			  try {
				  sc = new Scanner(new FileInputStream(args[0] + "/" + fileList[i]), 
						  StandardCharsets.UTF_8.toString());
				  sc.useDelimiter("\\Z");
				  String content = sc.next().replaceAll("\",\"", "");
				  content = content.replaceAll("\"", "");
				  content = content.replaceAll("\\\\", "");
				  String[] sentences = content.split("¡£");
				  PrintWriter writer = new PrintWriter(new OutputStreamWriter(new
						  FileOutputStream(args[1] + "/" + fileList[i]), 
						  StandardCharsets.UTF_8.toString()));
				  for(String sentence: sentences) {
					  writer.write(demoConversionParser.parseHelper(parser,
							  sentence));
					  writer.write("\n");
				  }
				  writer.close();
				  sc.close();
			  } catch (FileNotFoundException | UnsupportedEncodingException e) {
				  e.printStackTrace();
			  }
		  }
	}
	
	private static String parseHelper(conversionParser parser, String sentence) {
		StanfordTypedDependency[] deps = parser.parse(sentence);
		StringBuilder str = new StringBuilder();
		str.append("[");
		for(int i = 0; i < deps.length - 1; i++)
			str.append(deps[i] + ", ");
		str.append(deps[deps.length - 1] + "]");
		str.append("\n");
		for(int i = 0; i < deps.length; i++)
			str.append(deps[i].getWord().LEMMA + "\\" + 
		deps[i].getWord().POSTAG + " ");
		str.append("\n");
		return str.toString();
	}
}