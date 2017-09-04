package parser;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import com.hankcs.hanlp.dependency.IDependencyParser;
import com.hankcs.hanlp.dependency.nnparser.NeuralNetworkDependencyParser;

import typedDependency.StanfordTypedDependency;

public class conversionParser implements Runnable{
	static final String SBV = "SBV";
	static final String VOB = "VOB";
	static final String ATT = "ATT";
	static final String ADV = "ADV";
	static final String COO = "COO";
	static final String POB = "POB";
	static final String LAD = "LAD";
	static final String RAD = "RAD";
	static final String WP = "WP";
	static final String HED = "HED";
	
	static final String END = "END";
	static final String HEADER = "docID,sentenceID,tokenID,tokenOffset,"
			+ "tokenText,finePOS,POS,EntityType,EntityIOB,RelationType,"
			+ "RelationObject\n";
	static final int MAX = 4;
	static final int OFFSET = 2;
	
	static final Logger logger = Logger.getGlobal();
	
	private static HashSet<String> modalWords;
	private static HashSet<String> govName;
	private LinkedBlockingQueue<String> lbq;
	private List<String> fields;
	private String path;
	private String idField;
	
	private IDependencyParser parser =
			new NeuralNetworkDependencyParser().enableDeprelTranslator(false);
	
	public conversionParser(LinkedBlockingQueue<String> lbq, List<String>
	fields, String path, String idField) {
		this.lbq = lbq;
		this.fields = fields;
		this.path = path;
		this.idField = idField;
	}
	
	@Override
	public void run() {
		while(true) {
			String text;
			try {
				text = this.lbq.take();
				if(text.equals(END)) { //Check end indicator
					lbq.put(text);
					return; //Kill current thread
				}
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			JSONObject json = new JSONObject(text);
			String filename = json.getString(this.idField);
			int end = filename.indexOf(".");
			String fileID = filename.substring(0, end);
			filename = fileID + ".csv";
			
			try (BufferedWriter writer = new BufferedWriter(new
					OutputStreamWriter(new FileOutputStream(path + "/" +
			filename), StandardCharsets.UTF_8))){
				writer.write('\ufeff');
				ListIterator<String> it = this.fields.listIterator();
				int sentenceID = 0;
				while(it.hasNext()) {
					String field = it.next();
					if(field.equals(this.idField)) continue;
					
					writer.write(field + ":\r\n");
					String content = json.get(field).toString();
					if(content == null || content.length() == 0) continue;
					/* String clean-up */
					int start = content.indexOf('\\');
					if(start < 0) continue; //Empty field
					content = content.substring(start);
					content = content.replaceAll("\",\"", "");
					content = content.replaceAll("\"", "");
					content = content.replaceAll("\\\\", "");
					content = content.replaceAll("\\[", "");
					content = content.replaceAll("\\]", "");
					content = content.replaceAll("\\{", "");
					content = content.replaceAll("\\}", "");
					
					String[] sentences = content.split("。");
					for(String sentence: sentences) {
						if(sentence == null || sentence.length() == 0)
							continue;
						writer.write('\ufeff');
						writer.write(this.formatOutput(fileID, sentence,
								sentenceID++));
					}
					writer.write("\n");
				}
			} catch (IOException ex) {
				logger.exiting("conversionParser", "run", ex);
			}
		}
	}
	
	public StanfordTypedDependency[] parse(String str) {
		if(modalWords == null || govName == null) {
			logger.warning("Please load modal word list and/or government "
					+ "institution list first!");
			return null;
		}
        CoNLLSentence sentence = parser.parse(str);
        CoNLLWord[] wordArray = sentence.getWordArray();
        
        LinkedList<Integer> POBList = new LinkedList<>();
        LinkedList<Integer> dvpmodList = new LinkedList<>();
        StanfordTypedDependency[] parsed = new
        		StanfordTypedDependency[wordArray.length];
        for(int i = 0; i < wordArray.length; i++) {
        	String dep = wordArray[i].DEPREL;
        	switch(dep) {
        	  case SBV: parsed[i] = this.convertSBV(wordArray[i], POBList);
        	            break;
        	  case VOB: parsed[i] = this.convertVOB(wordArray[i]);
        	            break;
        	  case ATT:
        		  /* Associate modifier */
        		  if(i + 1 < wordArray.length &&
        				  wordArray[i + 1].POSTAG.equals("ude1"))
        			  parsed[i] = new
        			  StanfordTypedDependency(wordArray[i], "assmod");
        		  else
        			  parsed[i] = this.convertATT(wordArray[i]);
        		        break;
        	  case ADV: parsed[i] = this.convertADV(wordArray[i]);
        	            break;
        	  case COO: parsed[i] = this.convertCOO(wordArray[i]);
        	            break;
        	  case POB: parsed[i] = new StanfordTypedDependency(wordArray[i],
        			  "pobj");
        	            break;
        	  case LAD: parsed[i] = this.convertLAD(wordArray[i]);
        	            break;
        	  case RAD: parsed[i] = this.convertRAD(wordArray[i], dvpmodList);
        	            break;
        	  case WP: parsed[i] = new StanfordTypedDependency(wordArray[i],
        			  "punct");
        	            break;
        	  case HED: parsed[i] = new StanfordTypedDependency(wordArray[i],
        			  "root");
        	            break;
        	  default: parsed[i] = new StanfordTypedDependency(wordArray[i],
        			  "dep");
        	}
        }
        
        /* Clausual complement of a preposition */
    	ListIterator<Integer> it = POBList.listIterator();
    	while(it.hasNext()) {
    		parsed[it.next().intValue()].setDep("pccomp");
    	}
    	
    	/* A "XP+DEV" phrase that modifies VP */
    	it = dvpmodList.listIterator();
    	while(it.hasNext()) {
    		int index = it.next().intValue();
    		if(parsed[index].getDep().equals("dep"))
    			parsed[index].setDep("dvpmod");
    	}
    	
    	/* Handle Stanford Typed Dependencies that are independent of HanLP
    	 * dependencies
    	 */
    	this.postProcess(wordArray, parsed);
    	
        return parsed;
	}
	
	public void loadModalWords(String path) {
		if(conversionParser.modalWords != null)
			return;
		try {
			Scanner sc = new Scanner(new FileInputStream(path),
					StandardCharsets.UTF_8.toString());
			conversionParser.modalWords = new HashSet<>();
			while(sc.hasNextLine()) 
				conversionParser.modalWords.add(sc.nextLine());
			sc.close();
		} catch (FileNotFoundException e) {
			logger.severe("Load modal word list failed!");
		}
	}
	
	public void loadGovList(String filename) {
		try {
			Scanner sc = new Scanner(new InputStreamReader(new 
					FileInputStream(filename), "UTF-8"));
			conversionParser.govName = new HashSet<>();
			while(sc.hasNextLine()) {
				conversionParser.govName.add(sc.nextLine());
			}
			sc.close();	
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			logger.throwing("conversionParser", "loadGovList", e);
		}
	}
	
	private StanfordTypedDependency convertSBV(CoNLLWord word,
			LinkedList<Integer> POBList) {
		if(word.HEAD.DEPREL.equals(POB)) //POB case
			POBList.add(word.ID - 1);
		
		if(word.POSTAG.equals("vshi"))
			return new StanfordTypedDependency(word, "top"); //Topic
		return new StanfordTypedDependency(word, "nsubj");
	}
	
	private StanfordTypedDependency convertVOB(CoNLLWord word) {
		if(word.POSTAG.equals("m"))
			return new StanfordTypedDependency(word, "range");
		else if(word.POSTAG.endsWith("q")) {
			if(word.HEAD.POSTAG.startsWith("v"))
				return new StanfordTypedDependency(word, "range");
			else if(word.HEAD.POSTAG.equals("p")) //Attribute
				return new StanfordTypedDependency(word, "attr");
		} else if(word.HEAD.POSTAG.equals("vshi") ||
				word.HEAD.DEPREL.equals(SBV) || word.HEAD.DEPREL.equals(ADV))
			/* Clausal complement */
			return new StanfordTypedDependency(word, "ccomp");
		/* Direct object */
		return new StanfordTypedDependency(word, "dobj");
	}
	
	private StanfordTypedDependency convertATT(CoNLLWord word) {
		if(word.POSTAG.startsWith("rz")) //Determiner
			return new StanfordTypedDependency(word, "det");
		else if(word.POSTAG.equals("m")) {
			if(word.HEAD.POSTAG.equals("m")) // Ordinal number modifier
				return new StanfordTypedDependency(word, "ordmod");
			else if(word.HEAD.POSTAG.endsWith("q")) //Number modifier
				return new StanfordTypedDependency(word, "nummod");
		} else if(word.POSTAG.endsWith("q") &&
				word.HEAD.POSTAG.startsWith("n")) // Classifier modifier
			return new StanfordTypedDependency(word, "clf");
		else if(word.POSTAG.startsWith("a") &&
				word.HEAD.POSTAG.startsWith("n")) // Adjectival modifier
			return new StanfordTypedDependency(word, "amod");
		return new StanfordTypedDependency(word, "dep");
	}
	
	private StanfordTypedDependency convertADV(CoNLLWord word) {
		if(word.POSTAG.equals("p")) // Prepositional modifier
			return new StanfordTypedDependency(word, "prep");
		else if(word.POSTAG.equals("pba")) //"ba" construction
			return new StanfordTypedDependency(word, "ba");
		else if(word.POSTAG.startsWith("t")) //Temporal modifier
			return new StanfordTypedDependency(word, "tmod");
		/* Modal verb modifier */
		else if(word.POSTAG.startsWith("v") &&
				word.HEAD.POSTAG.startsWith("v") &&
				conversionParser.modalWords.contains(word.LEMMA))
			return new StanfordTypedDependency(word, "mmod");
		return new StanfordTypedDependency(word, "advmod");
	}
	
	private StanfordTypedDependency convertCOO(CoNLLWord word) {
		/* Coordinated verb compound modifier */
		if(word.POSTAG.startsWith("v") && word.HEAD.POSTAG.startsWith("v"))
			return new StanfordTypedDependency(word, "comod");
		return new StanfordTypedDependency(word, "conj"); //Conjunct
	}
	
	private StanfordTypedDependency convertLAD(CoNLLWord word) {
		if(word.POSTAG.equals("cc")) //Coordinating conjunction
			return new StanfordTypedDependency(word, "cc");
		return new StanfordTypedDependency(word, "dep");
	}
	
	private StanfordTypedDependency convertRAD(CoNLLWord word,
			List<Integer> dvpmodList) {
		if(word.POSTAG.equals("ude1")) //Associative marker
			return new StanfordTypedDependency(word, "assm");
		/* Aspect marker */
		else if(word.POSTAG.equals("ule") || word.POSTAG.equals("uguo"))
			return new StanfordTypedDependency(word, "asp");
		else if(word.POSTAG.equals("udeng")) //Etc modifier
			return new StanfordTypedDependency(word, "etc");
		else if(word.POSTAG.equals("ude2")) { //Manner DE modifier
			dvpmodList.add(word.HEAD.ID - 1);
			return new StanfordTypedDependency(word, "dvpm");
		}
		return new StanfordTypedDependency(word, "dep");
	}
	
	private void postProcess(CoNLLWord[] wordArray,
			StanfordTypedDependency[] deps) {
		for(int i = 0; i < wordArray.length; i++) {
			/* Parenthetical modifier */
			if(i + 1 < wordArray.length && wordArray[i + 1].LEMMA.equals(")"))
				deps[i].setDep("prnmod");
			else if(wordArray[i].LEMMA.endsWith("是") &&
					!wordArray[i].POSTAG.startsWith("n")) //Copular
				deps[i].setDep("cop");
			else if(wordArray[i].POSTAG.equals("pbei"))
				deps[i].setDep("pass"); //Passive marker
			else if(wordArray[i].LEMMA.equals("所") ||
					wordArray[i].LEMMA.equals("以") ||
					wordArray[i].LEMMA.equals("而") ||
					wordArray[i].LEMMA.equals("来"))
				deps[i].setDep("prtmod");
			else if(deps[i].getDep().equals("dep") && 
					(deps[i].getWord().HEAD.ID - deps[i].getWord().ID == 1) &&
					(deps[i].getWord().POSTAG.startsWith("n") ||
					deps[i].getWord().POSTAG.equals("vn")) &&
					(deps[i].getWord().HEAD.POSTAG.startsWith("n") ||
					deps[i].getWord().HEAD.POSTAG.equals("vn")))
				deps[i].setDep("nn");
		}
	}
	
	private String formatOutput(String docID, String sentence, int
			sentenceID) {
		if(sentence == null || sentence.length() == 0) return "";
		
		StanfordTypedDependency[] deps = this.parse(sentence);
		StringBuilder str = new StringBuilder();
		str.append(HEADER);
		this.markGovCompound(deps);
		int offset = 0;
		for(int i = 0; i < deps.length; i++) {
			str.append(docID + " ," + sentenceID + " ," + 
		        deps[i].toTableEntry(offset, deps));
			offset += (deps[i].getWord().LEMMA.length() + 1);
		}
		str.append("\n");
		return str.toString();
	}
	
	private void markGovCompound(StanfordTypedDependency[] deps) {
		int index = deps.length - 1;
		while(index > 0) {
			if(deps[index].getWord().POSTAG.equals("nto")) {
				deps[index].setIsGov();
				index--;
			} else if ((deps[index].getWord().POSTAG.startsWith("nt") ||
					deps[index].getWord().POSTAG.startsWith("ni")) && 
					this.isGovTerm(deps[index].getWord().LEMMA)) {
				deps[index].setIsGov();
				if(index == 0) return;
				
				StanfordTypedDependency tmp = deps[--index];
				while(tmp.getDep().equals("nn")) {
					tmp.setIsGov();
					if (tmp.getWord().POSTAG.startsWith("ns") ||
							tmp.getWord().LEMMA.equals("全国")) {
						index--;
						break;
					}
					--index;
					if(index < 0) break;
					tmp = deps[index];
				}
			} 
			else
				index--;
		}
	}
	
	private boolean isGovTerm(String term) {
		if(term.length() < 1) return false;
		
		for(int i = 1; i <= MAX; i++) {
			if(term.length() - i < 0) return false;
			if(govName.contains(term.substring(term.length() - i,
					term.length())))
				return true;
		}
		return false;
	}
}