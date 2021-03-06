# StanfordTypedParser
A parser that maps HanLP dependencies (Chinese Treebank 1.0) to Stanford Typed dependencies for Chinese text.
The motiviation of this project is to utilize the high-speed neural network dependency parser while preserve detailed grammatical relationships provided by Stanford Dependencies.
The following conversion table is created from experiment data to aid the development of this tool. However, the accuracy is not gurantee since it depends on multiple factors, including the correctness of initial dependency graphs generated by HanLP.
The conversion table is available at: https://docs.google.com/document/d/1zy_nsbbu5TrxvhBVwlLeCXYCvEu8rA-fL-eaOsDNtLo/edit?usp=sharing
Note: some of the dependencies cannot be converted unless more informatio is available.

# Chinese Treebank 1.0
Chinese Dependency Treebank 1.0 was developed by the Harbin Institute of Technologys Research Center for Social Computing and Information Retrieval (HIT-SCIR). It contains 49,996 Chinese sentences (902,191 words) randomly selected from Peoples Daily newswire stories published between 1992 and 1996 and annotated with syntactic dependency structures.
See details at: https://catalog.ldc.upenn.edu/docs/LDC2012T05/readme.html

# Stanford Dependencies
The Chinese dependencies have been developed by Huihsin Tseng and Pi-Chuan Chang. A brief description of the Chinese grammatical relations can be found in the following paper:
https://nlp.stanford.edu/pubs/ssst09-chang.pdf
