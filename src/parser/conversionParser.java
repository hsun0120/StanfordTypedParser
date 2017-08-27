package parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.logging.Logger;

import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import com.hankcs.hanlp.dependency.IDependencyParser;
import com.hankcs.hanlp.dependency.nnparser.NeuralNetworkDependencyParser;

import typedDependency.StanfordTypedDependency;

public class conversionParser {
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
	
	static final Logger logger = Logger.getGlobal();
	
	private HashSet<String> modalWords;
	
	public StanfordTypedDependency[] parse(String str) {
		if(modalWords == null) {
			logger.warning("Please load modal word list first!");
			return null;
		}
		
		IDependencyParser parser =
				new NeuralNetworkDependencyParser().enableDeprelTranslator(false);
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
        				  wordArray[i + 1].CPOSTAG.equals("ude1"))
        			  parsed[i] = new
        			  StanfordTypedDependency(wordArray[i + 1], "assmod");
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
		try {
			Scanner sc = new Scanner(new FileInputStream(path),
					StandardCharsets.UTF_8.toString());
			this.modalWords = new HashSet<>();
			while(sc.hasNextLine()) 
				this.modalWords.add(sc.nextLine());
			sc.close();
		} catch (FileNotFoundException e) {
			logger.severe("Load modal word list failed!");
		}
	}
	
	private StanfordTypedDependency convertSBV(CoNLLWord word,
			LinkedList<Integer> POBList) {
		if(word.HEAD.DEPREL.equals(POB)) //POB case
			POBList.add(word.ID - 1);
		
		if(word.CPOSTAG.equals("vshi"))
			return new StanfordTypedDependency(word, "top"); //Topic
		return new StanfordTypedDependency(word, "nsubj");
	}
	
	private StanfordTypedDependency convertVOB(CoNLLWord word) {
		if(word.CPOSTAG.equals("m"))
			return new StanfordTypedDependency(word, "range");
		else if(word.CPOSTAG.endsWith("q")) {
			if(word.HEAD.CPOSTAG.startsWith("v"))
				return new StanfordTypedDependency(word, "range");
			else if(word.HEAD.CPOSTAG.equals("p"))
				return new StanfordTypedDependency(word, "attr"); //Attribute
		} else if(word.HEAD.CPOSTAG.equals("vshi") ||
				word.HEAD.DEPREL.equals(SBV) || word.HEAD.DEPREL.equals(ADV))
			return new StanfordTypedDependency(word, "ccomp"); //Clausal complement
		return new StanfordTypedDependency(word, "dobj"); //Direct object
	}
	
	private StanfordTypedDependency convertATT(CoNLLWord word) {
		if(word.CPOSTAG.startsWith("rz"))
			return new StanfordTypedDependency(word, "det"); //Determiner
		else if(word.CPOSTAG.equals("m")) {
			if(word.HEAD.POSTAG.equals("m")) // Ordinal number modifier
				return new StanfordTypedDependency(word, "ordmod");
			else if(word.HEAD.POSTAG.endsWith("q")) //Number modifier
				return new StanfordTypedDependency(word, "nummod");
		} else if(word.CPOSTAG.endsWith("q") &&
				word.HEAD.CPOSTAG.startsWith("n")) // Classifier modifier
			return new StanfordTypedDependency(word, "clf");
		else if(word.CPOSTAG.startsWith("a") &&
				word.HEAD.CPOSTAG.startsWith("n")) // Adjectival modifier
			return new StanfordTypedDependency(word, "amod");
		return new StanfordTypedDependency(word, "dep");
	}
	
	private StanfordTypedDependency convertADV(CoNLLWord word) {
		if(word.CPOSTAG.equals("p")) // Prepositional modifier
			return new StanfordTypedDependency(word, "prep");
		else if(word.CPOSTAG.equals("pba"))
			return new StanfordTypedDependency(word, "ba"); //"ba" construction
		else if(word.CPOSTAG.startsWith("t")) //Temporal modifier
			return new StanfordTypedDependency(word, "tmod");
		/* Modal verb modifier */
		else if(word.CPOSTAG.startsWith("v") &&
				word.HEAD.CPOSTAG.startsWith("v") &&
				this.modalWords.contains(word.HEAD.LEMMA))
			return new StanfordTypedDependency(word, "mmod");
		return new StanfordTypedDependency(word, "advmod");
	}
	
	private StanfordTypedDependency convertCOO(CoNLLWord word) {
		/* Coordinated verb compound modifier */
		if(word.CPOSTAG.startsWith("v") && word.HEAD.CPOSTAG.startsWith("v"))
			return new StanfordTypedDependency(word, "comod");
		return new StanfordTypedDependency(word, "conj"); //Conjunct
	}
	
	private StanfordTypedDependency convertLAD(CoNLLWord word) {
		if(word.CPOSTAG.equals("cc")) //Coordinating conjunction
			return new StanfordTypedDependency(word, "cc");
		return new StanfordTypedDependency(word, "dep");
	}
	
	private StanfordTypedDependency convertRAD(CoNLLWord word,
			List<Integer> dvpmodList) {
		if(word.CPOSTAG.equals("ude1")) //Associative marker
			return new StanfordTypedDependency(word, "assm");
		/* Aspect marker */
		else if(word.CPOSTAG.equals("ule") || word.CPOSTAG.equals("uguo"))
			return new StanfordTypedDependency(word, "asp");
		else if(word.CPOSTAG.equals("udeng")) //Etc modifier
			return new StanfordTypedDependency(word, "etc");
		else if(word.CPOSTAG.equals("ude2")) { //Manner DE modifier
			dvpmodList.add(word.HEAD.ID - 1);
			return new StanfordTypedDependency(word, "dvpm");
		}
		return new StanfordTypedDependency(word, "dep");
	}
	
	private void postProcess(CoNLLWord[] wordArray,
			StanfordTypedDependency[] deps) {
		for(int i = 0; i < wordArray.length; i++) {
			/* Parenthetical modifier */
			if(i + 1 < wordArray.length && wordArray[i + 1].LEMMA.equals("）"))
				deps[i].setDep("prnmod");
			else if(wordArray[i].LEMMA.endsWith("是") &&
					!wordArray[i].CPOSTAG.startsWith("n")) //Copular
				deps[i].setDep("cop");
			else if(wordArray[i].CPOSTAG.equals("pbei"))
				deps[i].setDep("pass"); //Passive marker
			else if(wordArray[i].LEMMA.equals("所") ||
					wordArray[i].LEMMA.equals("以") ||
					wordArray[i].LEMMA.equals("而") ||
					wordArray[i].LEMMA.equals("来"))
				deps[i].setDep("prtmod");
		}
	}
}