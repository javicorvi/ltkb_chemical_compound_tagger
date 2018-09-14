package es.bsc.inb.limtox.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

@Service
public class TaggingServiceImpl implements TaggingService{
	
	static final Logger taggingLog = Logger.getLogger("taggingLog");
	
	public void execute(String propertiesParametersPath) {
		try {
			taggingLog.info("Tagging ltkb chemical compounds with properties :  " +  propertiesParametersPath);
			Properties propertiesParameters = this.loadPropertiesParameters(propertiesParametersPath);
			taggingLog.info("Input directory with the articles to tag : " + propertiesParameters.getProperty("inputDirectory"));
			taggingLog.info("Output directory : " + propertiesParameters.getProperty("outputDirectory"));
			taggingLog.info(" LTKB list chemical compounds used : " + propertiesParameters.getProperty("ltkb_chemical_compounds"));
			
			String inputDirectoryPath = propertiesParameters.getProperty("inputDirectory");
			String outputDirectoryPath = propertiesParameters.getProperty("outputDirectory");
			String ltkbChemicalCompoundsPath = propertiesParameters.getProperty("ltkb_chemical_compounds");
			Integer index_id = new Integer(propertiesParameters.getProperty("index_id"));
			Integer index_text_to_tag = new Integer(propertiesParameters.getProperty("index_text_to_tag"));
			
			File inputDirectory = new File(inputDirectoryPath);
		    if(!inputDirectory.exists()) {
		    	return ;
		    }
		    if (!Files.isDirectory(Paths.get(inputDirectoryPath))) {
		    	return ;
		    }
		    File outputDirectory = new File(outputDirectoryPath);
		    if(!outputDirectory.exists())
		    	outputDirectory.mkdirs();
		    
			
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, regexner, entitymentions");
			props.put("regexner.mapping", ltkbChemicalCompoundsPath);
			props.put("regexner.posmatchtype", "MATCH_ALL_TOKENS");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	
			
		    List<String> filesProcessed = readFilesProcessed(outputDirectoryPath); 
		    BufferedWriter filesPrecessedWriter = new BufferedWriter(new FileWriter(outputDirectoryPath + File.separator + "list_files_processed.dat", true));
		    File[] files =  inputDirectory.listFiles();
			for (File file_to_classify : files) {
				if(file_to_classify.getName().endsWith(".txt") && filesProcessed!=null && !filesProcessed.contains(file_to_classify.getName())){
					taggingLog.info("Processing file  : " + file_to_classify.getName());
					String fileName = file_to_classify.getName();
					String outputFilePath = outputDirectory + File.separator + fileName;
					BufferedWriter outPutFile = new BufferedWriter(new FileWriter(outputFilePath));
					for (String line : ObjectBank.getLineIterator(file_to_classify.getAbsolutePath(), "utf-8")) {
						try {
							String[] data = line.split("\t");
							String id =  data[index_id];
							String text =  data[index_text_to_tag];
							tagging(pipeline, id, text, outPutFile, file_to_classify.getName());
						}  catch (Exception e) {
							taggingLog.error("Error tagging the document line " + line + " belongs to the file: " +  fileName,e);
						} 
					
					}
					outPutFile.close();
					filesPrecessedWriter.write(file_to_classify.getName()+"\n");
					filesPrecessedWriter.flush();
				}
			}
			filesPrecessedWriter.close();
		}  catch (Exception e) {
			taggingLog.error("Generic error in the classification step",e);
		} 
	}
	
	/**
	 * Findings of LTKB ChemicalCompunds
	 * 
	 * @param sourceId
	 * @param document_model
	 * @param first_finding_on_document
	 * @param section
	 * @param sentence_text
	 * @return
	 * @throws MoreThanOneEntityException
	 */
	private void tagging(StanfordCoreNLP pipeline, String id, String text_to_tag, BufferedWriter output, String fileName) {
		String text = "Joe Smith was born in California. " +
			      "In 2017, he went to Paris, France in the summer. " +
			      "His flight left at 3:00pm on alatrofloxacin mesylate July 10th, 2017. " +
			      "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
			      "He sent a postcard to his sister Jane Smith. " +
			      "After hearing about Joe's fipexide trip, Jane decided she might go to France one day.";
		Annotation document = new Annotation(text_to_tag.toLowerCase());
		//Annotation document = new Annotation(text_to_tag.toLowerCase());
		// run all Annotators on this text
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
        	// traversing the words in the current sentence
	        // a CoreLabel is a CoreMap with additional token-specific methods
        	
        	List<CoreMap> entityMentions = sentence.get(MentionsAnnotation.class);
    		for (CoreMap entityMention : entityMentions) {
    			try {
    				String keyword = entityMention.get(TextAnnotation.class);
        			String entityType = entityMention.get(CoreAnnotations.EntityTypeAnnotation.class);
			        if(entityType!=null && entityType.equals("LTKB_CHEMICAL_COMPOUND")) {
			        	CoreLabel token = entityMention.get(TokensAnnotation.class).get(0);
			        	output.write(id + "\t"+ token.beginPosition() + "\t" + (token.beginPosition() + keyword.length())  + "\t" + keyword + "\t" + entityType + "\n");
				        output.flush();
				        //leave this code here for future changes.
			        	/*for (CoreLabel token: entityMention.get(TokensAnnotation.class)) {
            				// this is the text of the token
        			        String word = token.get(TextAnnotation.class);
        			        // this is the NER label of the token
        			        String ne = token.get(NamedEntityTagAnnotation.class);
        			        if(ne!=null) {
        			        	output.write(id + "\t"+ token.beginPosition() + "\t" + token.endPosition() + "\t" + word + "\t" + ne);
        				        output.flush();
        			        }
    			        }*/
				        /*for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			        	try {
			        		// this is the text of the token
					        String word = token.get(TextAnnotation.class);
					        // this is the NER label of the token
					        String ne = token.get(NamedEntityTagAnnotation.class);
					        if(ne!=null) {
					        	output.write(id + "\t"+ token.beginPosition() + "\t" + token.endPosition() + "\t" + word + "\t" + ne);
						        output.flush();
					        }
					    } catch (Exception e) {
							taggingLog.error("Generic Error tagging id "  + id + " in file " + fileName, e);
						}
			        }*/
        				
			        }
        		} catch (Exception e) {
					taggingLog.error("Generic Error tagging id "  + id + " in file " + fileName, e);
				}
    		}
        	
	        
        }
	}


	private List<String> readFilesProcessed(String outputDirectoryPath) {
		try {
			List<String> files_processed = new ArrayList<String>();
			if(Files.isRegularFile(Paths.get(outputDirectoryPath + File.separator + "list_files_processed.dat"))) {
				FileReader fr = new FileReader(outputDirectoryPath + File.separator + "list_files_processed.dat");
			    BufferedReader br = new BufferedReader(fr);
			    
			    String sCurrentLine;
			    while ((sCurrentLine = br.readLine()) != null) {
			    	files_processed.add(sCurrentLine);
				}
			    br.close();
			    fr.close();
			}
			return files_processed;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	  * Load Properties
	  * @param properitesParametersPath
	  */
	 public Properties loadPropertiesParameters(String properitesParametersPath) {
		 Properties prop = new Properties();
		 InputStream input = null;
		 try {
			 input = new FileInputStream(properitesParametersPath);
			 // load a properties file
			 prop.load(input);
			 return prop;
		 } catch (IOException ex) {
			 ex.printStackTrace();
		 } finally {
			 if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			 }
		}
		return null;
	 }	

	
}