package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



/**
 * Handles the generation and validation of XML representations of space hierarchies
 * @author Benton McCune
 * 
 */
public class BatchUtil {
	private static final Logger log = Logger.getLogger(BatchUtil.class);
	
	private Document doc = null;
	private String errorMessage = "";//this will be used to given information to user about failures in validation
	private Boolean spaceCreationSuccess = false;
	/**
	 *  Will generate an xml file for a space hierarchy.
	 *  @author Benton McCune
	 *  @param space The space for which we want an xml representation.
	 *  @param userId the id of the user making the request
	 *  @return xml file to represent space hierarchy of input space
	 *  @throws Exception   
	 */	
	public File generateXMLfile(Space space, int userId) throws Exception{
		log.debug("Generating XML for Space = " +space.getId());			
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		doc = docBuilder.newDocument();
		Element rootSpace = generateSpacesXML(space, userId);
		doc.appendChild(rootSpace);
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		
		File file = new File(R.STAREXEC_ROOT, space.getName() +".xml");
		
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		
		validateAgainstSchema(file);//Not really necessary, but a good check to have in the event of future code changes
		return file;
	}
	
    /**
     *  Will generate xml for the root space.
     *  @author Benton McCune
     *  @param space The space for which we want an xml representation.
     *  @param userId the id of the user making the request
     *  @return spacesElement for the xml file to represent space hierarchy of input space	 *  
     */	
    public Element generateSpacesXML(Space space, int userId){		
	log.debug("Generating Space XML for space " + space.getId());
	Element spacesElement=null;
	//if (R.STAREXEC_SERVERNAME.contains("stardev")) {
	//	spacesElement = doc.createElementNS(Util.url("public/stardevSpaceSchema.xsd"), "tns:Spaces");
	//	spacesElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
	//	spacesElement.setAttribute("xsi:schemaLocation", 
	//				   Util.url("public/stardevSpaceSchema.xsd stardevSpaceSchema.xsd"));
	//} else {
	spacesElement = doc.createElementNS(Util.url("public/batchSpaceSchema.xsd"), "tns:Spaces");
	spacesElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
	spacesElement.setAttribute("xsi:schemaLocation", 
					   Util.url("public/batchSpaceSchema.xsd batchSpaceSchema.xsd"));
	//}
	
		
	Element rootSpaceElement = generateSpaceXML(space, userId);
	spacesElement.appendChild(rootSpaceElement);
		
	return spacesElement;
    }
	
	/**
	 *  Generates the XML for an individual space.  Called recursively to produce entire hierarchy.
	 *  @author Benton McCune
	 *  @param space The space for which we want an xml representation.
	 *  @param userId the id of the user making the request
	 *  @return spaceElement for xml file to represent space hierarchy of input space 
	 */	
	public Element generateSpaceXML(Space space, int userId){		
		log.debug("Generating Space XML for space " + space.getId());
		
		Element spaceElement = doc.createElement("Space");
	
		Attr id = doc.createAttribute("id");
		id.setValue(Integer.toString(space.getId()));
		spaceElement.setAttributeNode(id);		
		Attr name = doc.createAttribute("name");
		name.setValue(space.getName());
		spaceElement.setAttributeNode(name);
		
		// New attributes to space XML elements
		// @author Tim Smith
		
		// Sticky leaders attribute : sticky-leaders
		Attr stickyLeaders = doc.createAttribute("sticky-leaders");
		stickyLeaders.setValue(Boolean.toString(space.isStickyLeaders()));
		spaceElement.setAttributeNode(stickyLeaders);
		
		// Find out if the users from the parent space are inherited (inherit-users)
		Attr inheritUsers = doc.createAttribute("inherit-users");
//		List<User> parentUsers = Spaces.getUsers(space.getParentSpace());
//		List<User> spaceUsers = space.getUsers();
		Boolean iu = false;
//		if (parentUsers != null && spaceUsers != null) {
//			// Check that the current space has all users present in the parent space
//			for (User u:parentUsers){
//				if (!spaceUsers.contains(u)){
//					iu = false;
//					break;
//				}
//			}
//			
//		}
		inheritUsers.setValue(iu.toString());
		spaceElement.setAttributeNode(inheritUsers);
		
		// Locked attribute
		Attr locked = doc.createAttribute("locked");
		locked.setValue(Boolean.toString(space.isLocked()));
		spaceElement.setAttributeNode(locked);
		
		
		Permission perm = space.getPermission();
		
		//Permissions attributes - only set when false since default is all true
		if (!perm.canAddBenchmark()) {
			Attr addBenchPerm = doc.createAttribute("add-benchmark-perm");
			addBenchPerm.setValue("false");
			spaceElement.setAttributeNode(addBenchPerm);
		}
		
		if (!perm.canAddJob()) {
			Attr addJobPerm = doc.createAttribute("add-job-perm");
			addJobPerm.setValue("false");
			spaceElement.setAttributeNode(addJobPerm);
		}
		
		if (!perm.canAddSolver()){
			Attr addSolverPerm = doc.createAttribute("add-solver-perm");
			addSolverPerm.setValue("false");
			spaceElement.setAttributeNode(addSolverPerm);
		}
		
		if (!perm.canAddSpace()){
			Attr addSpacePerm = doc.createAttribute("add-space-perm");
			addSpacePerm.setValue("false");
			spaceElement.setAttributeNode(addSpacePerm);
		}
		
		if (!perm.canAddUser()){
			Attr addUserPerm = doc.createAttribute("add-user-perm");
			addUserPerm.setValue("false");
			spaceElement.setAttributeNode(addUserPerm);
		}
		
		if (!perm.canRemoveBench()) {
			Attr remBenchmarkPerm = doc.createAttribute("rem-benchmark-perm");
			remBenchmarkPerm.setValue("false");
			spaceElement.setAttributeNode(remBenchmarkPerm);
		}
		
		if (!perm.canRemoveJob()) {
			Attr remJobPerm = doc.createAttribute("rem-job-perm");
			remJobPerm.setValue("false");
			spaceElement.setAttributeNode(remJobPerm);
		}
		
		if (!perm.canRemoveSolver()) {
			Attr remSolverPerm = doc.createAttribute("rem-solver-perm");
			remSolverPerm.setValue("false");
			spaceElement.setAttributeNode(remSolverPerm);
		}
		
		if (!perm.canRemoveSpace()) {
			Attr remSpacePerm = doc.createAttribute("rem-space-perm");
			remSpacePerm.setValue("false");
			spaceElement.setAttributeNode(remSpacePerm);
		}
		
		if (!perm.canRemoveUser()) {
			Attr remUserPerm = doc.createAttribute("rem-user-perm");
			remUserPerm.setValue("false");
			spaceElement.setAttributeNode(remUserPerm);
		}
		
		// -------------------------------------------------
		
		for (Benchmark benchmark:space.getBenchmarks()){
			Element benchElement = doc.createElement("Benchmark");	
			benchElement.setAttribute("id", Integer.toString(benchmark.getId()));
			benchElement.setAttribute("name", benchmark.getName());
			spaceElement.appendChild(benchElement);
		}
		for (Solver solver:space.getSolvers()){		
			Element solverElement = doc.createElement("Solver");
			solverElement.setAttribute("id", Integer.toString(solver.getId()));
			solverElement.setAttribute("name", solver.getName());
			spaceElement.appendChild(solverElement);
		}	
		for (Space subspace:space.getSubspaces()){			
			spaceElement.appendChild(generateSpaceXML(Spaces.getDetails(subspace.getId(), userId),userId));
		}
		
		return spaceElement;
	}
	
	/**
	 *  Validates the file against the specific starexec space XML Schema 
	 *  @author Benton McCune
	 *  @param file The file that we wish to validate
	 *  @return Boolean true if the file is valid
	 *  @throws SAXException
	 *  @throws ParserConfigurationException
	 *  @throws IOException   
	 */	
	public Boolean validateAgainstSchema(File file) throws ParserConfigurationException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);//This is true for DTD, but not W3C XML Schema that we're using
		factory.setNamespaceAware(true);

		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

		try {
			String schemaLoc = R.SPACE_XML_SCHEMA_LOC;
			factory.setSchema(schemaFactory.newSchema(new Source[] {new StreamSource(schemaLoc)}));
			Schema schema = factory.getSchema();
			DocumentBuilder builder = factory.newDocumentBuilder();
//			builder.setErrorHandler(new SAXErrorHandler());
			Document document = builder.parse(file);
			Validator validator = schema.newValidator();
			DOMSource source = new DOMSource(document);
            validator.validate(source);
            log.debug("Space XML File has been validated against the schema.");
            return true;
        } catch (SAXException ex) {
            log.warn("File is not valid because: \"" + ex.getMessage() + "\"");
            //log.warn("The file located at [" + file.getParentFile().getAbsolutePath() + "] has been removed since an error occured while parsing.");
            //FileUtils.deleteDirectory(file.getParentFile());
            errorMessage = "File is not valid because: \"" + ex.getMessage() + "\"";
            this.spaceCreationSuccess = false;
            return false;
        }
		
	}
	
	
	/**
	 * Creates space hierarchies from the xml file.
	 * @author Benton Mccune
	 * @param file the xml file we wish to produce a space from
	 * @param userId the userId of the user making the request
	 * @param parentSpaceId the space that will serve as the root for the space hierarchy
	 * @return Boolean true if the space is successfully created
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public Boolean createSpacesFromFile(File file, int userId, int parentSpaceId) throws SAXException, ParserConfigurationException, IOException{
		
		if (!validateAgainstSchema(file)){
			log.warn("File from User " + userId + " is not Schema valid.");
			return false;
		}
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
		Element spacesElement = doc.getDocumentElement();
		NodeList listOfRootSpaceElements = spacesElement.getChildNodes();
		
		String name = "";//name variable to check
        //Check Benchmarks and Solvers
        NodeList listOfSpaces = doc.getElementsByTagName("Space");
		log.info("# of Spaces = " + listOfSpaces.getLength());
        NodeList listOfSolvers = doc.getElementsByTagName("Solver");
		log.info("# of Solvers = " + listOfSolvers.getLength());
        NodeList listOfBenchmarks = doc.getElementsByTagName("Benchmark");
		log.info("# of Benchmarks = " + listOfBenchmarks.getLength());
		
		//Make sure spaces all have names
		for (int i = 0; i < listOfSpaces.getLength(); i++){
			Node spaceNode = listOfSpaces.item(i);
			if (spaceNode.getNodeType() == Node.ELEMENT_NODE){
				Element spaceElement = (Element)spaceNode;
				name = spaceElement.getAttribute("name");
				if (name == null) {
					log.debug("Name not found");
					errorMessage = "Space elements must include a 'name' attribute.";
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
				log.warn("Space Node should be an element, but isn't");
			}
		}
		//Verify user has access to solvers
		for (int i = 0; i < listOfSolvers.getLength(); i++){
			Node solverNode = listOfSolvers.item(i);
			if (solverNode.getNodeType() == Node.ELEMENT_NODE){
				Element solverElement = (Element)solverNode;
				String id = solverElement.getAttribute("id");
				Boolean canSee = Permissions.canUserSeeSolver(Integer.parseInt(id), userId);
				log.info("Solver Id = " + id + ", User can see = " + canSee);
				if (!canSee){
					errorMessage = "You do not have access to a solver with id = " + id;
					return false;
				}
			}
			else{
				log.warn("solver Node should be an element, but isn't");
			}
		}
		//Verify user has access to benchmarks
		for (int i = 0; i < listOfBenchmarks.getLength(); i++){
			Node benchmarkNode = listOfBenchmarks.item(i);
			if (benchmarkNode.getNodeType() == Node.ELEMENT_NODE){
				Element benchmarkElement = (Element)benchmarkNode;
				String id = benchmarkElement.getAttribute("id");
				name = benchmarkElement.getAttribute("name");
				Boolean canSee = Permissions.canUserSeeBench(Integer.parseInt(id), userId);
				log.debug("Benchmark Id = " + id + ", Benchmark name = " + name + ", User can see = " + canSee);			
				
				if (!canSee){
					errorMessage = "You do not have access to a benchmark with id = " + id;
					return false;
				}
			}
			else{
				log.warn("benchmark Node should be an element, but isn't");
			}
		}
		//Create Space Hierarchies as children of parent space	
		this.spaceCreationSuccess = true;
		for (int i = 0; i < listOfRootSpaceElements.getLength(); i++){
			Node spaceNode = listOfRootSpaceElements.item(i);
			if (spaceNode.getNodeType() == Node.ELEMENT_NODE){
				Element spaceElement = (Element)spaceNode;
				
				spaceCreationSuccess = spaceCreationSuccess && createSpaceFromElement(spaceElement, parentSpaceId, userId);
			}
		}
		return spaceCreationSuccess;
	}
	

	
	
	
	/**
	 * Creates space from Element.  Method called from createSpaceFromFile.  Also calls itself recursively.
	 * @author Benton McCune
	 * @param spaceElement the element that 
	 * @param parentId id of parent space
	 * @param userId id of user making request
	 * @return
	 */
	public Boolean createSpaceFromElement(Element spaceElement, int parentId, int userId){
		Space space = new Space();
		space.setName(spaceElement.getAttribute("name"));
		Permission permission = new Permission(true);//default permissions
		
		// Check for permission attributes in XML and set permissions accordingly
		String perm = spaceElement.getAttribute("add-benchmark-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setAddBenchmark(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("add-job-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setAddJob(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("add-solver-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setAddSolver(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("add-space-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setAddSpace(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("add-user-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setAddUser(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("rem-benchmark-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setRemoveBench(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("rem-job-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setRemoveJob(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("rem-solver-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setRemoveSolver(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("rem-space-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setRemoveSpace(Boolean.valueOf(perm));
		
		perm = spaceElement.getAttribute("rem-user-perm");
		if (!perm.equals("") && !perm.equals(null))
			permission.setRemoveUser(Boolean.valueOf(perm));
		
		space.setPermission(permission);
		
		Random rand=new Random();
		String baseSpaceName=space.getName();
		
		// Look for a sticky leaders attribute. If it's there, set sticky leaders
		String sl = spaceElement.getAttribute("sticky-leaders");		
		if (!sl.equals("") && !sl.equals(null)) {
			Boolean stickyLeaders = Boolean.valueOf(sl);
			space.setStickyLeaders(stickyLeaders);
		}
		
	
		
		// Check for the locked attribute
		String locked = spaceElement.getAttribute("locked");
		if (!locked.equals("") && !locked.equals(null)) {
			Boolean isLocked = Boolean.valueOf(locked);
			space.setLocked(isLocked);
		}
		
		//------------------------------------------------------------------------
		
		//Is appending a random number to the name what we want?
		//Also, this will hang if there are too many spaces with the given name
		//seems unrealistic to run into that, but just in case, we'll count attempts
		int attempt=0;
		while (Spaces.notUniquePrimitiveName(space.getName(), parentId, 4)) {
			int appendInt=rand.nextInt();
			space.setName(baseSpaceName+appendInt);
			if (attempt>1000) {
				//give up
				return false;
			}
			attempt++;
		}
		Integer spaceId = Spaces.add(space, parentId, userId);
		
		// Check for inherit users attribute. If it is true, make the users the same as the parent
		String iu = spaceElement.getAttribute("inherit-users");
		if (!iu.equals("") && !iu.equals(null)){
			Boolean inheritUsers = Boolean.valueOf(iu);
			log.debug("inherit = " + inheritUsers);
			if (inheritUsers) {
				log.debug("Adding inherited users");
				List<User> users = Spaces.getUsers(parentId);
				log.debug("parent users = " + users);
				for (User u : users) {
					log.debug("users = " + u.getFirstName());
					int tempId = u.getId();
					Users.associate(tempId, spaceId);
				}
			}
		}
		
		List<Integer> benchmarks = new ArrayList<Integer>();
		List<Integer> solvers = new ArrayList<Integer>();
		NodeList childList = spaceElement.getChildNodes();
		int id=0;
		for (int i = 0; i < childList.getLength(); i++){
			Node childNode = childList.item(i);
			
			if (childNode.getNodeType() == Node.ELEMENT_NODE){	
				log.debug("found a new element = "+childNode.toString());
				Element childElement = (Element)childNode;
				String elementType = childElement.getTagName();
				if (elementType.equals("Benchmark")){
					id=Integer.parseInt(childElement.getAttribute("id"));
					if (!Spaces.notUniquePrimitiveName(Benchmarks.get(id).getName(), parentId, 2)) {
						benchmarks.add(id);
					}
				}
				else if (elementType.equals("Solver")){
					id=Integer.parseInt(childElement.getAttribute("id"));
					if (!Spaces.notUniquePrimitiveName(Solvers.get(id).getName(), parentId, 1)) {
						solvers.add(id);
					}
				}
				else if (elementType.equals("Space")){
					createSpaceFromElement(childElement, spaceId, userId);
				}
				else{
					
					log.warn("\"" + elementType + "\" is not a valid element type");
				}
			}
			else{
				//do nothing, as it's probably just whitespace
				//log.warn("Space " + spaceId + " has a node that should be an element, but isn't");
			}
		}
		if (!benchmarks.isEmpty()){
			Benchmarks.associate(benchmarks, spaceId);
		}
		if (!solvers.isEmpty()){
			Solvers.associate(solvers, spaceId);
		}	
		return true;
	}
	/**
	 * @return doc the document object
	 */
	public Document getDoc() {
		return doc;
	}
	/**
	 * @param doc the document object
	 */
	public void setDoc(Document doc) {
		this.doc = doc;
	}
    /**
     * @return errorMessage error String
     */
	public String getErrorMessage() {
		return errorMessage;
	}
	/** 
	 * @param errorMessage error String
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	/**
	 * @return spaceCreationSuccess Boolean 
	 */
	public Boolean getSpaceCreationSuccess() {
		return spaceCreationSuccess;
	}
	/**
	 * @param spaceCreationSuccess Boolean
	 */
	public void setSpaceCreationSuccess(Boolean spaceCreationSuccess) {
		this.spaceCreationSuccess = spaceCreationSuccess;
	}


	
	
}
