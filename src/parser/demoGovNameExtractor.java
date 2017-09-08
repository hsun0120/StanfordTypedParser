package parser;

public class demoGovNameExtractor {
	public static void main(String[] args) {
		govNameExtractor extractor = new govNameExtractor(args[0]);
		extractor.extractToFile("govList.txt");
	}
}