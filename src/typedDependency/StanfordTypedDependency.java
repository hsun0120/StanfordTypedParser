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
}