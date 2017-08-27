package parser;

import typedDependency.StanfordTypedDependency;

public class demoConversionParser {
	public static void main(String[] args) {
		conversionParser parser = new conversionParser();
		parser.loadModalWords("modalWords.txt");
		StanfordTypedDependency[] deps = parser.parse("��Ժ����֧��");
		for(StanfordTypedDependency dep: deps)
			System.out.println(dep);
	}
}