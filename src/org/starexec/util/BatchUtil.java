package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.Properties;
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
import org.starexec.data.to.Update;
import org.starexec.util.DOMHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



/**
 * Handles the generation and validation of XML representations of space hierarchies
 * @author Benton McCune. changes: Julio Cervantes
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
	 *  @param includeAttributes whether or not to include benchmark attributes
	 *  @return xml file to represent space hierarchy of input space
	 *  @throws Exception   
	 */	
    public File generateXMLfile(Space space, int userId, boolean includeAttributes) throws Exception{
	//TODO : attributes are being sorted alphabetically, want to preserve order of insertion instead
		log.debug("Generating XML for Space = " +space.getId());			
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		doc = docBuilder.newDocument();
		Element rootSpace = generateSpacesXML(space, userId, includeAttributes);
		doc.appendChild(rootSpace);
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		
		File file = new File(R.STAREXEC_ROOT, (space.getName().replaceAll("\\s+", "")) +".xml");
		log.debug(file.getAbsolutePath());
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		
		//validateAgainstSchema(file);
		return file;
	}
	
    /**
     *  Will generate xml for the root space.
     *  @author Benton McCune
     *  @param space The space for which we want an xml representation.
     *  @param userId the id of the user making the request
     *  @param includeAttributes whether or not to include benchmark attributes
     *  @return spacesElement for the xml file to represent space hierarchy of input space	 *  
     */	
    public Element generateSpacesXML(Space space, int userId, boolean includeAttributes){		
	log.debug("Generating Space XML for space " + space.getId());
	Element spacesElement=null;

	spacesElement = doc.createElementNS(Util.url("public/batchSpaceSchema.xsd"), "tns:Spaces");
	spacesElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
	spacesElement.setAttribute("xsi:schemaLocation", 
					   Util.url("public/batchSpaceSchema.xsd batchSpaceSchema.xsd"));	
		
	Element rootSpaceElement = generateSpaceXML(space, userId, includeAttributes);
	spacesElement.appendChild(rootSpaceElement);
		
	return spacesElement;
    }
	
	/**
	 *  Generates the XML for an individual space.  Called recursively to produce entire hierarchy.
	 *  @author Benton McCune
	 *  @param space The space for which we want an xml representation.
	 *  @param userId the id of the user making the request
	 *  @param includeAttributes whether or not to include benchmark attributes
	 *  @return spaceElement for xml file to represent space hierarchy of input space 
	 */	
    public Element generateSpaceXML(Space space, int userId, boolean includeAttributes){		
		log.debug("Generating Space XML for space " + space.getId());
		
		Element spaceElement = doc.createElement("Space");
		Element attrsElement = doc.createElement("SpaceAttributes");
	
		Attr id = doc.createAttribute("id");
		id.setValue(Integer.toString(space.getId()));
		spaceElement.setAttributeNode(id);
		
		Attr name = doc.createAttribute("name");
		name.setValue(space.getName());
		spaceElement.setAttributeNode(name);
		
		Element descriptionElement = doc.createElement("description");

		Attr description = doc.createAttribute("value");
		description.setValue(space.getDescription());
		descriptionElement.setAttributeNode(description);

		attrsElement.appendChild(descriptionElement);

		Element stickyLeadersElement = doc.createElement("sticky-leaders");
		
		// Sticky leaders attribute : sticky-leaders
		Attr stickyLeaders = doc.createAttribute("value");
		stickyLeaders.setValue(Boolean.toString(space.isStickyLeaders()));
		stickyLeadersElement.setAttributeNode(stickyLeaders);

		attrsElement.appendChild(stickyLeadersElement);
		

		Element inheritUsersElement = doc.createElement("inherit-users");

		// TODO: Find out if the users from the parent space are inherited (inherit-users)
		Attr inheritUsers = doc.createAttribute("value");
		Boolean iu = false;
		// Cheap implementation, just assume false
		inheritUsers.setValue(iu.toString());
		inheritUsersElement.setAttributeNode(inheritUsers);

		attrsElement.appendChild(inheritUsersElement);

		Element lockedElement = doc.createElement("locked");
		
		// Locked attribute
		Attr locked = doc.createAttribute("value");
		locked.setValue(Boolean.toString(space.isLocked()));
		lockedElement.setAttributeNode(locked);
		
		attrsElement.appendChild(lockedElement);
		
		Permission perm = space.getPermission();
		
		//Permissions attributes - only set when false since default is all true
		if (!perm.canAddBenchmark()) {

		    Element addBenchPermElement = doc.createElement("add-benchmark-perm");

		    Attr addBenchPerm = doc.createAttribute("value");
		    addBenchPerm.setValue("false");
		    addBenchPermElement.setAttributeNode(addBenchPerm);

		    attrsElement.appendChild(addBenchPermElement);
		}
		
		if (!perm.canAddJob()) {

		    Element addJobPermElement = doc.createElement("add-job-perm");

		    Attr addJobPerm = doc.createAttribute("value");
		    addJobPerm.setValue("false");
		    addJobPermElement.setAttributeNode(addJobPerm);

		    attrsElement.appendChild(addJobPermElement);
		}
		
		if (!perm.canAddSolver()){

		    Element addSolverPermElement = doc.createElement("add-solver-perm");

		    Attr addSolverPerm = doc.createAttribute("value");
		    addSolverPerm.setValue("false");
		    addSolverPermElement.setAttributeNode(addSolverPerm);

		    attrsElement.appendChild(addSolverPermElement);
		}
		
		if (!perm.canAddSpace()){
		    Element addSpacePermElement = doc.createElement("add-space-perm");

		    Attr addSpacePerm = doc.createAttribute("value");
		    addSpacePerm.setValue("false");
		    addSpacePermElement.setAttributeNode(addSpacePerm);

		    attrsElement.appendChild(addSpacePermElement);
		}
		
		if (!perm.canAddUser()){
		    Element addUserPermElement = doc.createElement("add-user-perm");

		    Attr addUserPerm = doc.createAttribute("value");
		    addUserPerm.setValue("false");
		    addUserPermElement.setAttributeNode(addUserPerm);

		    attrsElement.appendChild(addUserPermElement);
		}
		
		if (!perm.canRemoveBench()) {
		    Element remBenchPermElement = doc.createElement("rem-benchmark-perm");

		    Attr remBenchmarkPerm = doc.createAttribute("value");
		    remBenchmarkPerm.setValue("false");
		    remBenchPermElement.setAttributeNode(remBenchmarkPerm);

		    attrsElement.appendChild(remBenchPermElement);
		}
		
		if (!perm.canRemoveJob()) {
		    Element remJobPermElement = doc.createElement("rem-job-perm");

		    Attr remJobPerm = doc.createAttribute("value");
		    remJobPerm.setValue("false");
		    remJobPermElement.setAttributeNode(remJobPerm);

		    attrsElement.appendChild(remJobPermElement);
		}
		
		if (!perm.canRemoveSolver()) {
		    Element remSolverPermElement = doc.createElement("rem-solver-perm");

		    Attr remSolverPerm = doc.createAttribute("value");
		    remSolverPerm.setValue("false");
		    remSolverPermElement.setAttributeNode(remSolverPerm);

		    attrsElement.appendChild(remSolverPermElement);
		}
		
		if (!perm.canRemoveSpace()) {

		    Element remSpacePermElement = doc.createElement("rem-space-perm");

		    Attr remSpacePerm = doc.createAttribute("value");
		    remSpacePerm.setValue("false");
		    remSpacePermElement.setAttributeNode(remSpacePerm);

		    attrsElement.appendChild(remSpacePermElement);
		}
		
		if (!perm.canRemoveUser()) {
		    Element remUserPermElement = doc.createElement("rem-user-perm");

		    Attr remUserPerm = doc.createAttribute("value");
		    remUserPerm.setValue("false");
		    remUserPermElement.setAttributeNode(remUserPerm);

		    attrsElement.appendChild(remUserPermElement);
		}
		
		spaceElement.appendChild(attrsElement);
		// -------------------------------------------------
		
		for (Benchmark benchmark:space.getBenchmarks()){
			Element benchElement = doc.createElement("Benchmark");	
			benchElement.setAttribute("id", Integer.toString(benchmark.getId()));
			benchElement.setAttribute("name", benchmark.getName());
			if (includeAttributes) {
			    Properties attrs = Benchmarks.getAttributes(benchmark.getId());
			    if (attrs != null) {
				Enumeration<Object> keys = attrs.keys();
				while (keys.hasMoreElements()) {
				    String attr = (String)keys.nextElement();
				    String val = (String)attrs.get(attr);
				    Element attre = doc.createElement("Attribute");
				    attre.setAttribute("name",attr);
				    attre.setAttribute("value",val);
				    benchElement.appendChild(attre);
				}
			    }
			}
			spaceElement.appendChild(benchElement);
		}
		for (Solver solver:space.getSolvers()){		
			Element solverElement = doc.createElement("Solver");
			solverElement.setAttribute("id", Integer.toString(solver.getId()));
			solverElement.setAttribute("name", solver.getName());
			spaceElement.appendChild(solverElement);
		}	
		for (Space subspace:space.getSubspaces()){			
		    spaceElement.appendChild(generateSpaceXML(Spaces.getDetails(subspace.getId(), userId),userId, includeAttributes));
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
			String schemaLoc = R.STAREXEC_ROOT + "/" + R.SPACE_XML_SCHEMA_RELATIVE_LOC;
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
	public List<Integer> createSpacesFromFile(File file, int userId, int parentSpaceId) throws SAXException, ParserConfigurationException, IOException{
		List<Integer> spaceIds=new ArrayList<Integer>();
		if (!validateAgainstSchema(file)){
			log.warn("File from User " + userId + " is not Schema valid.");
			return null;
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
	NodeList listOfUpdates = doc.getElementsByTagName("Update");
	        log.info("# of Updates = " + listOfUpdates.getLength());
		
		//Make sure spaces all have names
		for (int i = 0; i < listOfSpaces.getLength(); i++){
			Node spaceNode = listOfSpaces.item(i);
			if (spaceNode.getNodeType() == Node.ELEMENT_NODE){
				Element spaceElement = (Element)spaceNode;
				name = spaceElement.getAttribute("name");
				if (name == null) {
					log.debug("Name not found");
					errorMessage = "Space elements must include a 'name' attribute.";
					return null;
				}
				log.debug("Space Name = " + name);
				if (name.length()<1){
					log.debug("Name was not long enough");
					errorMessage = name + "is not a valid name.  It must have at least one character.";
					return null;
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
				String idstr = solverElement.getAttribute("id");
				Integer id = Integer.parseInt(idstr);
				Boolean canSee = Permissions.canUserSeeSolver(id, userId);
				log.info("Solver Id = " + idstr + ", User can see = " + canSee);
				if (!canSee){
					errorMessage = "You do not have access to a solver with id = " + idstr;
					return null;
				}
				if (Solvers.get(id) == null) {
				    errorMessage = "The solver with id " + idstr + " is either deleted or recycled.";
				    return null;
				}
			}
			else{
				log.warn("solver Node should be an element, but isn't");
			}
		}
		//Verify user has access to benchmarks and update benchmarks.
		if(!verifyBenchmarks(listOfBenchmarks, userId)) return null;
		if(!verifyBenchmarks(listOfUpdates,userId)) return null;

		//Create Space Hierarchies as children of parent space	
		this.spaceCreationSuccess = true;
		for (int i = 0; i < listOfRootSpaceElements.getLength(); i++){
			Node spaceNode = listOfRootSpaceElements.item(i);
			if (spaceNode.getNodeType() == Node.ELEMENT_NODE){
				Element spaceElement = (Element)spaceNode;
				int spaceId=createSpaceFromElement(spaceElement, parentSpaceId, userId);
				spaceIds.add(spaceId);
				
				spaceCreationSuccess = spaceCreationSuccess && (spaceId!=-1);
			} 
		}
		return spaceIds;
	}
    
    /**
	 * Verifies that a user can look at all of the benchmarks.
	 * @author Ryan McCleeary
	 * @param listOfBenchmarks node list that contains all of the benchmarks in question. 
	 * @param parentId id of parent space
	 * @param userId id of user making request
	 * 
	 */
    private boolean verifyBenchmarks(NodeList listOfBenchmarks,int userId)
    {
	String name = "";
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
		return true;
    }
	

	
	
	
	/**
	 * Creates space from Element.  Method called from createSpaceFromFile.  Also calls itself recursively.
	 * @author Benton McCune
	 * @param spaceElement the element that 
	 * @param parentId id of parent space
	 * @param userId id of user making request
	 * @return Integer the id of the new space or -1 on error
	 * @return
	 */
	public Integer createSpaceFromElement(Element spaceElement, int parentId, int userId){
		Space space = new Space();
		space.setName(spaceElement.getAttribute("name"));
		Permission permission = new Permission(true);//default permissions
		
		Element spaceAttributes = DOMHelper.getElementByName(spaceElement, "SpaceAttributes");
		
		log.info("SpaceAttributes element created");
		// Check for description attribute

		if(DOMHelper.hasElement(spaceAttributes,"description")){
		    Element description = DOMHelper.getElementByName(spaceAttributes,"description");
		    space.setDescription(description.getAttribute("value"));
		}
		else{
		    space.setDescription("no description");
		}

		log.info("description set");

		// Check for permission attributes in XML and set permissions accordingly


		String perm;
		Element ele;

		if(DOMHelper.hasElement(spaceAttributes,"add-benchmark-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"add-benchmark-perm");
		    perm = ele.getAttribute("value");
		    permission.setAddBenchmark(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"add-job-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"add-job-perm");
		    perm = ele.getAttribute("value");
		    permission.setAddJob(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"add-solver-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"add-solver-perm");
		    perm = ele.getAttribute("value");
		    permission.setAddSolver(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"add-space-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"add-space-perm");
		    perm = ele.getAttribute("value");
		    permission.setAddSpace(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"add-user-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"add-user-perm");
		    perm = ele.getAttribute("value");
		    permission.setAddUser(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"rem-benchmark-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"rem-benchmark-perm");
		    perm = ele.getAttribute("value");
		    permission.setRemoveBench(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"rem-job-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"rem-job-perm");
		    perm = ele.getAttribute("value");
		    permission.setRemoveJob(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"rem-solver-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"rem-solver-perm");
		    perm = ele.getAttribute("value");
		    permission.setRemoveSolver(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"rem-space-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"rem-space-perm");
		    perm = ele.getAttribute("value");
		    permission.setRemoveSpace(Boolean.valueOf(perm));
		}
   
   
   
		if(DOMHelper.hasElement(spaceAttributes,"rem-user-perm")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"rem-user-perm");
		    perm = ele.getAttribute("value");
		    permission.setRemoveUser(Boolean.valueOf(perm));
		}
   
		
		space.setPermission(permission);
		
		Random rand=new Random();
		String baseSpaceName=space.getName();
		
		// Look for a sticky leaders attribute. If it's there, set sticky leaders

		if(DOMHelper.hasElement(spaceAttributes, "sticky-leaders")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"sticky-leaders");
		    Boolean stickyLeaders = Boolean.valueOf(ele.getAttribute("value"));
		    space.setStickyLeaders(stickyLeaders);
		}
	
		
		// Check for the locked attribute

		if(DOMHelper.hasElement(spaceAttributes, "locked")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"locked");
		    Boolean isLocked = Boolean.valueOf(ele.getAttribute("value"));
		    log.info("locked: " + isLocked);
		    space.setLocked(isLocked);
		}
		
		//------------------------------------------------------------------------
		
		//Is appending a random number to the name what we want?
		//Also, this will hang if there are too many spaces with the given name
		//seems unrealistic to run into that, but just in case, we'll count attempts
		int attempt=0;
		while (Spaces.notUniquePrimitiveName(space.getName(), parentId)) {
			int appendInt=rand.nextInt();
			space.setName(baseSpaceName+appendInt);
			if (attempt>1000) {
				//give up
				return -1;
			}
			attempt++;
		}
		Integer spaceId = Spaces.add(space, parentId, userId);
		
		// Check for inherit users attribute. If it is true, make the users the same as the parent
		if(DOMHelper.hasElement(spaceAttributes, "inherit-users")){
		    ele = DOMHelper.getElementByName(spaceAttributes,"inherit-users");
		    Boolean inheritUsers = Boolean.valueOf(ele.getAttribute("value"));
		    log.info("inherit = " + inheritUsers);
		    if(inheritUsers){
			List<User> users = Spaces.getUsers(parentId);
			for (User u : users) {
			    log.debug("users = " + u.getFirstName());
			    int tempId = u.getId();
			    Users.associate(tempId, spaceId);
			}
		    }
		}

		
		List<Integer> benchmarks = new ArrayList<Integer>();
		List<Integer> solvers = new ArrayList<Integer>();
		List<Update> updates = new ArrayList<Update>();
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
					benchmarks.add(id);
					
				}
				else if (elementType.equals("Solver")){
					id=Integer.parseInt(childElement.getAttribute("id"));
					solvers.add(id);
					
				}
				else if (elementType.equals("Space")){
					createSpaceFromElement(childElement, spaceId, userId);
				}
				else if(elementType.equals("Update")){
				    int bid = Integer.parseInt(childElement.getAttribute("id"));
				    int pid = Integer.parseInt(childElement.getAttribute("pid"));
				    Update u = new Update(bid,pid);
				    updates.add(u);
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
		if (!updates.isEmpty())
		{
		    //To do (create updates the updates here)
		}
		return spaceId;
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
