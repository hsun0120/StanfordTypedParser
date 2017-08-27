package parser;

import typedDependency.StanfordTypedDependency;

public class demoConversionParser {
	public static void main(String[] args) {
		conversionParser parser = new conversionParser();
		parser.loadModalWords("modalWords.txt");
		StanfordTypedDependency[] deps = parser.parse("本院予以支持");
		for(StanfordTypedDependency dep: deps)
			System.out.println(dep);
	}
}