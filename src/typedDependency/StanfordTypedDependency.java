package typedDependency;

import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;

public class StanfordTypedDependency {
	static final int PREV = 2;
	
	private CoNLLWord word;
	private String dep;
	private boolean isGov;
	
	public StanfordTypedDependency(CoNLLWord word) {
		this.word = word;
		this.dep = null;
		this.isGov = false;
	}
	
	public StanfordTypedDependency(CoNLLWord word, String dep) {
		this(word);
		this.dep = dep;
	}
	
	public CoNLLWord getWord() {
		return this.word;
	}
	
	public String getDep() {
		return this.dep;
	}
	
	public void setDep(String dep) {
		this.dep = dep;
	}
	
	public void setIsGov() {
		this.isGov = true;
	}
	
	@Override
	public String toString() {
		if(this.word.HEAD.LEMMA.equals("##ºËÐÄ##"))
			return this.dep + "(" + "root-0" + "," + this.word.LEMMA +
					"-" + this.word.ID + ")";
		return this.dep + "(" + this.word.HEAD.LEMMA + "-" + this.word.HEAD.ID
				+  "," + this.word.LEMMA + "-" + this.word.ID + ")";
	}
	
	public String toTableEntry(int offset, StanfordTypedDependency[] deps) {
		String entity = "NULL";
		if(this.isGov) entity = "GOV";
		else if(this.word.POSTAG.startsWith("nt"))
			entity = "ORG";
		else if(this.word.POSTAG.startsWith("nr"))
			entity= "PERSON";
		else if(this.word.POSTAG.startsWith("ns"))
			entity = "LOCATION";
		String entityIOB = "O";
		if(this.dep.equals("nn") && this.getWord().ID == 1)
			entityIOB = "B";
		else if(this.getWord().ID == 1) entityIOB = "O";
		else if(this.dep.equals("nn") && 
				!deps[this.word.ID - PREV].getDep().equals("nn"))
			entityIOB = "B";
		else if(this.dep.equals("nn") ||
				deps[this.word.ID - PREV].getDep().equals("nn"))
			entityIOB = "I";
		String object = this.word.HEAD.LEMMA;
		if(object.equals("##ºËÐÄ##"))
			object = this.word.LEMMA;
		String tokenText = this.word.LEMMA;
		if(tokenText.equals(","))
			tokenText = "\",\"";
		
		return (this.word.ID - 1) + "," + offset + "," + tokenText + 
				"," + this.word.POSTAG + "," + this.word.CPOSTAG + "," +
		entity + "," + entityIOB + "," + dep + "," + object + "\r\n";
	}
}