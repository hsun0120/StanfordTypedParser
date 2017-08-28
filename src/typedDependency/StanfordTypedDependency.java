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
}