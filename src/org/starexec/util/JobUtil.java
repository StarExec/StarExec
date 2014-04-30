package org.starexec.util;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JobUtil {
	private static final Logger log = Logger.getLogger(JobUtil.class);
	
	private Boolean jobCreationSuccess = false;
	private String errorMessage = "";//this will be used to given information to user about failures in validation
	
	/**
	 * Creates jobs from the xml file.
	 * @author Tim Smith
	 * @param file the xml file we wish to create jobs from
	 * @param userId the userId of the user making the request
	 * @param spaceId the space that will serve as the root for jobs to run under
	 * @return Boolean true if the jobs are successfully created
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public Boolean createJobsFromFile(File file, int userId, Integer spaceId) throws Exception {
		if (!validateAgainstSchema(file)){
			log.warn("File from User " + userId + " is not Schema valid.");
			return false;
		}
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
        Element jobsElement = doc.getDocumentElement();
		NodeList listOfJobElements = jobsElement.getChildNodes();
		
        //Check Jobs and Job Pairs
        NodeList listOfJobs = doc.getElementsByTagName("Job");
		log.info("# of Jobs = " + listOfJobs.getLength());
        NodeList listOfJobPairs = doc.getElementsByTagName("JobPair");
		log.info("# of JobPairs = " + listOfJobPairs.getLength());
		
		String name = "";//name variable to check
		
		// Make sure jobs are named
		for (int i = 0; i < listOfJobs.getLength(); i++){
			Node jobNode = listOfJobs.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE){
				Element jobElement = (Element)jobNode;
				name = jobElement.getAttribute("name");
				if (name == null) {
					log.debug("Name not found");
					errorMessage = "Job elements must include a 'name' attribute.";
					return false;
				}
				log.debug("Space Name = " + name);
				if (name.length()<2){
					log.debug("Name was not long enough");
					errorMessage = name + "is not a valid name.  It must have two characters.";
					return false;
				}
				
			}
			else{
				log.warn("Job Node should be an element, but isn't");
			}
		}
		
		// Verify the user has access to the benchmarks and solvers for the JobPair elements
		
		// TODO Create job pairs from an XML configuration file
		
		return true;
	}
	
	public Boolean validateAgainstSchema(File file) throws ParserConfigurationException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);//This is true for DTD, but not W3C XML Schema that we're using
		factory.setNamespaceAware(true);

		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

		try {
			String schemaLoc = Util.url("public/batchJobSchema.xsd");
			factory.setSchema(schemaFactory.newSchema(new Source[] {new StreamSource(schemaLoc)}));
			Schema schema = factory.getSchema();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(file);
			Validator validator = schema.newValidator();
			DOMSource source = new DOMSource(document);
            validator.validate(source);
            log.debug("Job XML File has been validated against the schema.");
            return true;
        } catch (SAXException ex) {
            log.warn("File is not valid because: \"" + ex.getMessage() + "\"");
            errorMessage = "File is not valid because: \"" + ex.getMessage() + "\"";
            this.jobCreationSuccess = false;
            return false;
        }
		
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Boolean getJobCreationSuccess() {
		return jobCreationSuccess;
	}

	public void setJobCreationSuccess(Boolean jobCreationSuccess) {
		this.jobCreationSuccess = jobCreationSuccess;
	}

}
