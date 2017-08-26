package parser;

import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.dependency.IDependencyParser;
import com.hankcs.hanlp.dependency.nnparser.NeuralNetworkDependencyParser;

public class conversionParser {
	static final String SBV = "SBV";
	static final String VOB = "VOB";
	static final String IOB = "IOB";
	static final String FOB = "FOB";
	static final String DBL = "DBL";
	static final String ATT = "ATT";
	static final String ADV = "ADV";
	static final String CMP = "CMP";
	static final String COO = "COO";
	static final String POB = "POB";
	static final String LAD = "LAD";
	static final String RAD = "RAD";
	static final String IS = "IS";
	static final String WP = "WP";
	static final String HED = "HED";
	
	public static void parse(String str) {
		IDependencyParser parser =
				new NeuralNetworkDependencyParser().enableDeprelTranslator(false);
        CoNLLSentence sentence = parser.parse(str);
	}
}