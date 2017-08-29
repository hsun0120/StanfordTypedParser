package typedDependency;

import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;

public class StanfordTypedDependency {
	private CoNLLWord word;
	private String dep;
	
	public StanfordTypedDependency(CoNLLWord word) {
		this.word = word;
		this.dep = null;
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
	
	@Override
	public String toString() {
		if(this.word.HEAD.LEMMA.equals("##ºËÐÄ##"))
			return this.dep + "(" + "root-0" + "," + this.word.LEMMA +
					"-" + this.word.ID + ")";
		return this.dep + "(" + this.word.HEAD.LEMMA + "-" + this.word.HEAD.ID
				+  "," + this.word.LEMMA + "-" + this.word.ID + ")";
	}
	
	public String toTableEntry(int offset) {
		String entity = "NULL";
		if(this.word.POSTAG.startsWith("nt"))
			entity = "ORG";
		else if(this.word.POSTAG.startsWith("nr"))
			entity= "PERSON";
		else if(this.word.POSTAG.startsWith("ns"))
			entity = "LOCATION";
		String entityIOB = "O";
		if(this.dep.equals("nn"))
			entityIOB = "I";
		else if(this.word.POSTAG.startsWith("n"))
			entityIOB = "B";
		String object = this.word.HEAD.LEMMA;
		if(object.equals("##ºËÐÄ##"))
			object = this.word.LEMMA;
		
		return (this.word.ID - 1) + " ," + offset + " ," + this.word.LEMMA + 
				" ," + this.word.POSTAG + " ," + this.word.CPOSTAG + ", " +
		entity + " ," + entityIOB + " ," + dep + ", " + object + "\n";
	}
}