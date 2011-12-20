package org.starexec.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.starexec.constants.R;
import org.starexec.data.Database;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.impl.xs.identity.Selector.Matcher;

/**
 * Class which listens for application events (mainly startup/shutdown)
 * and does any required setup/teardown.
 * 
 * @author Tyler Jensen
 */
public class Starexec implements ServletContextListener {
	private static final Logger log = Logger.getLogger(Starexec.class);
	private static String ROOT_APPLICATION_PATH = "";
	
	// The node detail attribute regex pattern to parse SGE output
	private static Pattern keyValPattern;
	
	// Path of the starexec config and log4j files which are needed at compile time to load other resources
	private static String CONFIG_PATH = "/WEB-INF/classes/org/starexec/config/starexec-config.xml";
	private static String LOG4J_PATH = "/WEB-INF/classes/org/starexec/config/log4j.properties";
	
	// XML Metadata to parse starexec's config file	
	private static String NODE_CLASS = "class";
	private static String NODE_PROP = "property";
	private static String NODE_CONFIG = "configuration";
	private static String ATTR_KEY = "key";
	private static String ATTR_VALUE = "value";
	private static String ATTR_NAME = "name";
	private static String ATTR_DEFAULT = "default";
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// Called when the application ends
		log.info("Starexec Shutdown");
	}

	/**
	 * When the application starts, this method is called. Perform any initializations here
	 */	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		// Called when the application starts		
				
		// Remember the application's root so we can load properties from it later
		Starexec.ROOT_APPLICATION_PATH = event.getServletContext().getRealPath("/");
		
		// Before we do anything we must configure log4j!
		PropertyConfigurator.configure(new File(ROOT_APPLICATION_PATH, LOG4J_PATH).getAbsolutePath());
		
		log.info(String.format("Application started at [%s]", ROOT_APPLICATION_PATH));
						
		// Load all properties from the starexec-config file
		Starexec.loadProperties();
		
		// Add the database to the application scope to expose it to JSP's via EL
		event.getServletContext().setAttribute("database", new Database());
		
		// Compile the SGE node detail parse pattern
		keyValPattern = Pattern.compile(R.NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);	
		
		// On a new thread, load the worker node data (this may take some time)
		Runnable r = new Runnable() {			
			@Override
			public void run() {
				Starexec.loadWorkerNodes();
			}
		};		
		new Thread(r).start();		
	}
	
	/**
	 * Gets the worker nodes from SGE and adds them to the database if they don't already exist.
	 */
	public static void loadWorkerNodes() {
		BufferedReader nodeResults = null;
		
		try {			
			// Execute the SGE command to get the node list
			nodeResults = Util.executeCommand(R.NODE_LIST_COMMAND);
			
			if(nodeResults == null) {
				// If the command failed, return now				
				return;
			}
			
			// Read the nodes one at a time
			String line;		
			while((line = nodeResults.readLine()) != null) {
				String name = line;
				if(!R.USE_FULL_HOST_NAME) {
					// Just get the first part of the name if we're not using the full host name
					int periodIndex = line.indexOf(".");					
					if(periodIndex > 0) {						
						name = line.substring(0, periodIndex);
					}														
				}
				
				// In the database, update the attributes for the node
				Database.updateWorkerNode(name,  Starexec.getNodeDetails(name));
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} finally {
			// Try to close the result list if it is allowed
			try { nodeResults.close(); } catch (Exception e) { }
		}
	}
	
	/**
	 * Calls SGE to get details about the given node. 
	 * @param name The name of the node to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	private static HashMap<String, String> getNodeDetails(String name) {
		// Make the results hashmap that will be returned		
		HashMap<String, String> details = new HashMap<String, String>();
		
		// Call SGE to get details for the given node
		String results = Util.bufferToString(Util.executeCommand(R.NODE_DETAILS_COMMAND + name));
		
		// Parse the output from the SGE call to get the key/value pairs for the node
		java.util.regex.Matcher matcher = keyValPattern.matcher(results);
		
		// For each match...
		while(matcher.find()) {
			// Split apart the key from the value
			String[] keyVal = matcher.group().split("=");
			
			// Add the results to the details hashmap
			details.put(keyVal[0], keyVal[1]);
		}
		
		return details;
	}
	
	/**
	 * Loads resources from the starexec-config.xml file into the static resource classes
	 * specified in the config file using reflection. The property file keys must match the
	 * corresponding field name in the specified resource class.
	 */
	@SuppressWarnings("rawtypes")
	public static void loadProperties(){
		try {
			// Open the starexec-config xml file and parse it into a dom
			File statementFile = new File(ROOT_APPLICATION_PATH, CONFIG_PATH);
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
			
			Document starexecConfigDoc = db.parse(statementFile);
			starexecConfigDoc.getDocumentElement().normalize();			   			
			
			if(starexecConfigDoc.getDocumentElement().getAttributes().getNamedItem(ATTR_DEFAULT) == null) {
				// Check if the default configuration is specified! We explicitly require it
				throw new Exception(String.format("starexec-config parsing error: the root element must define an attribute \"%s\"", ATTR_DEFAULT));
			}
			
			// Find the name of the configuration to use
			String defaultConfigName = starexecConfigDoc.getDocumentElement().getAttributes().getNamedItem(ATTR_DEFAULT).getNodeValue();
			
			// Get all configuration nodes
			NodeList configNodes = starexecConfigDoc.getDocumentElement().getElementsByTagName(NODE_CONFIG);
			
			// The default configuration node
			Node defaultConfigNode = null;			
			
			// For each of the configuration nodes
			for(int i = 0; i < configNodes.getLength(); i++) {
				Node currentConfig = configNodes.item(i);											
				Node currentConfigNameAttr = currentConfig.getAttributes().getNamedItem(ATTR_NAME);
				
				if(currentConfigNameAttr == null) {
					// If the current node doesn't have a name attribute, skip it
					continue;					
				} else if(currentConfigNameAttr.getNodeValue().equals(defaultConfigName)) {
					// Otherwise if we've found a config with the name that matches the one to use, keep it and break
					defaultConfigNode = currentConfig;
					log.debug(String.format("Using configuration [%s] for properties specification", defaultConfigName));					
					break;
				}
			}
			
			if(defaultConfigNode == null) {
				// If we didn't find a node that matched the specified name, then that's an error!
				throw new Exception(String.format("The default configuration \"%s\" was not found.", defaultConfigName));
			}
			
			// Get all class nodes from the document
			NodeList classNodes = defaultConfigNode.getChildNodes();
			log.debug("Parsing starexec-config XML file resulted in " + classNodes.getLength() + " nodes.");
			
			// For each class node in the configuration file...
			for(int i = 0; i < classNodes.getLength(); i++) {
				// Get that class node and it's child nodes
				Node currentClassNode = classNodes.item(i);				
				
				if(!currentClassNode.getNodeName().equals(NODE_CLASS)){
					// If we're not looking at class node (most likely an attribute) skip
					continue;
				}
				
				NodeList classNodeChildren = currentClassNode.getChildNodes();
				
				// Parse the class name from XML attribute and load that class via reflection
				String className = currentClassNode.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
				Class currentClass = Class.forName(className);
				
				// For each property node under the current class node...
				for(int j = 0; j < classNodeChildren.getLength(); j++) {
					// Get the property node and parse out the key/value from its attributes
					Node currentPropNode = classNodeChildren.item(j);
					
					if(!currentPropNode.getNodeName().equals(NODE_PROP)){
						// If we're not looking at property node (most likely a comment) skip
						continue;
					}
					
					String key = currentPropNode.getAttributes().getNamedItem(ATTR_KEY).getNodeValue();
					String value = currentPropNode.getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();
					
					try {
						// Get the field from the current class that matches the XML specified key
						Field field = currentClass.getField(key);
						
						// Force the field to be accessible in case it's private or final
						field.setAccessible(true);
						
						// Based on the type of field we're expecting, set that field's value to the property's value
						if(field.getType().equals(String.class)){
							field.set(null, value);
						} else if(field.getType().equals(int.class)){
							field.setInt(null, Integer.parseInt(value));
						} else if(field.getType().equals(boolean.class)){
							field.set(null,Boolean.parseBoolean(value));
						}	            
						
						log.debug(String.format("Loaded property [%s = %s] into class %s", key, field.get(null), className));
					} catch (Exception e){
						log.error(String.format("Failed to load property [%s]. Error [%s]", key, e.getMessage()));
					}
				}				
			}			 
		} catch (Exception e) {
			log.fatal(e.getMessage(), e);
		}
	}
}