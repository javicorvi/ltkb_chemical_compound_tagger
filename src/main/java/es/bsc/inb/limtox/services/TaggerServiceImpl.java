package es.bsc.inb.limtox.services;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class TaggerServiceImpl implements TaggingService{

	public void execute(String propertiesParametersPath) {
		
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, regexner");
		props.put("regexner.mapping", "ltkb_chemical_compounds.txt");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		
		String text_example = "Then the only recognized entities are the ones in our pattern file. "
				+ " Nevertheless, in general, writing rules that cover all cases is a difficult enterprise! This is one reason why statistical classifiers have become dominant, because they are good at integrating various sources of evidence. So, often, a tool like RegexNER is most useful as an overlay that corrects or augments the output of a statistical "
				+ "NLP system like alaproclate Stanford NER. This example showed usage from the command line. You can also easily use RegexNER in code. The RegexNER rules can be in a regular file, in a resource that is on your CLASSPATH, or even specified by a URL. You then specify to load RegexNER and where the RegexNER rules file is by providing an appropriate Properties "
				+ " object Gemtuzumab ozogamicin when creating alaproclate Stanford CoreNLP";
		
		// create an empty Annotation just with the given text
        Annotation document = new Annotation(text_example.toLowerCase());

        // run all Annotators on this text
        pipeline.annotate(document);
        
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for(CoreMap sentence: sentences) {
          // traversing the words in the current sentence
          // a CoreLabel is a CoreMap with additional token-specific methods
          for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
            // this is the text of the token
            String word = token.get(TextAnnotation.class);
            // this is the NER label of the token
            String ne = token.get(NamedEntityTagAnnotation.class);
            
            System.out.println(word  + "\t"+ token.beginPosition() + "\t" + token.endPosition() + "\t" + ne);
          }
		
	}

}

	
}