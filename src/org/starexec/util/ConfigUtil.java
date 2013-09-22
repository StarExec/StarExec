package org.starexec.util;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Contains methods used to parse the starexec config file and load properties into specified classes.
 * @author Tyler Jensen
 *
 */
public class ConfigUtil {
	private static final Logger log = Logger.getLogger(ConfigUtil.class);
	
	// XML Metadata to parse starexec's config file	
	private static String NODE_CLASS = "class";
	private static String NODE_PROP = "property";
	private static String NODE_CONFIG = "configuration";
	private static String NODE_VALUE = "value";
	private static String ATTR_KEY = "key";
	private static String ATTR_VALUE = "value";
	private static String ATTR_NAME = "name";
	private static String ATTR_DEFAULT = "default";
	private static String ATTR_INHERIT = "inherit";
	
	// Build property information
	private static String buildVersion = null;
	private static String buildUser = null;
	private static Date buildDate = null;
	
	// The configuration in use
	private static String configName = "";
	
	/**
	 * Loads resources from the starexec-config.xml file into the static resource classes
	 * specified in the config file using reflection. The property file keys must match the
	 * corresponding field name in the specified resource class.
	 */
	public static void loadProperties(File configFile){
		try {
			// Open the starexec-config xml file and parse it into a dom			
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 			
			Document starexecConfigDoc = db.parse(configFile);
			starexecConfigDoc.getDocumentElement().normalize();			   			
			
			if(starexecConfigDoc.getDocumentElement().getAttributes().getNamedItem(ATTR_DEFAULT) == null) {
				// Check if the default configuration is specified! We explicitly require it
				throw new Exception(String.format("starexec-config parsing error: the root element must define an attribute \"%s\"", ATTR_DEFAULT));
			}
			
			// Find the name of the configuration to use
			String defaultConfigName = 
			    starexecConfigDoc.getDocumentElement().getAttributes().getNamedItem(ATTR_DEFAULT).getNodeValue(); 
			log.info("Loading default configuration "+defaultConfigName);

			Node defaultConfigNode = findConfigNode(starexecConfigDoc.getDocumentElement(), defaultConfigName);
			
			if(defaultConfigNode == null) {
				// If we didn't find a node that matched the specified name, then that's an error!
				throw new Exception(String.format("The default configuration \"%s\" was not found.", defaultConfigName));
			}

			loadPropertiesFromNode(starexecConfigDoc, defaultConfigNode);
			ConfigUtil.configName = defaultConfigName;
		} catch (Exception e) {
			log.fatal(e.getMessage(), e);
		}
	}
	
	/**
	 * Given the root XML element, this method will find the XML node with the given configuration name
	 * @param rootElement The XML document element
	 * @param configName The name of the configuration to find
	 * @return The XML node for the given configuration name
	 */
	private static Node findConfigNode(Element rootElement, String configName) {
		// Get all configuration nodes
		NodeList configNodes = rootElement.getElementsByTagName(NODE_CONFIG);
		
		// The config node to find
		Node configNode = null;				
		
		// For each of the configuration nodes
		for(int i = 0; i < configNodes.getLength(); i++) {
			Node currentConfig = configNodes.item(i);											
			Node currentConfigNameAttr = currentConfig.getAttributes().getNamedItem(ATTR_NAME);
			
			if(currentConfigNameAttr == null) {
				// If the current node doesn't have a name attribute, skip it
				continue;					
			} else if(currentConfigNameAttr.getNodeValue().equals(configName)) {
				// Otherwise if we've found a config with the name that matches the one to use, keep it and break
				configNode = currentConfig;								
				break;
			}
		}
		
		return configNode;
	}
	
	/**
	 * Given a configuration node, loads all properties into the classes within the node
	 * @param node The configuration node which contains class specifications to load into
	 */
	@SuppressWarnings("rawtypes")
	    private static void loadPropertiesFromNode(Document starexecConfigDoc, Node node) throws Exception {

		/* first process any parent configurations we are inheriting from */

		// Get the name of any configurations to inherit from
		String inheritFrom = null;
		Node inheritNodeAttr = node.getAttributes().getNamedItem(ATTR_INHERIT);
			
		// If there is an inheritance specified, load properties from the inherited node
		if(inheritNodeAttr != null) {
		    inheritFrom = inheritNodeAttr.getNodeValue();
		    Node inheritConfigNode = findConfigNode(starexecConfigDoc.getDocumentElement(), inheritFrom);
		    
		    if(inheritConfigNode != null) {
			// If we found a valid node to inherit from, load from that node first
			loadPropertiesFromNode(starexecConfigDoc, inheritConfigNode);
		    } else {
			// If we didn't find the inheritance node, warn it
			log.warn("Could not find specified inheritance configuration: " + inheritFrom);
		    }
		}			
			
		// Now load the properties from the current node itself
		
		log.debug(String.format("Loading configuration %s", node.getAttributes().getNamedItem(ATTR_NAME)));

		// Get all subnodes from the given node
		NodeList classNodes = node.getChildNodes();		
		
		// For each class node in the configuration...
		for(int i = 0; i < classNodes.getLength(); i++) {
			// Get that class node and its child nodes
			Node currentClassNode = classNodes.item(i);				
			
			if(!currentClassNode.getNodeName().equals(NODE_CLASS)){
				// If we're not looking at class node (most likely an attribute) skip
				continue;
			}
			
			NodeList classNodeChildren = currentClassNode.getChildNodes();
			
			// Parse the class name from XML attribute and load that class via reflection
			String className = currentClassNode.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
			Class currentClass = Class.forName(className);
			
			log.debug("Loading class "+className);

			// For each property node under the current class node...
			for(int j = 0; j < classNodeChildren.getLength(); j++) {
				// Get the property node and parse out the key/value from its attributes
				Node currentPropNode = classNodeChildren.item(j);
				
				if(!currentPropNode.getNodeName().equals(NODE_PROP)){
					// If we're not looking at property node skip
					continue;
				}
				
				String key = currentPropNode.getAttributes().getNamedItem(ATTR_KEY).getNodeValue();				
				String value = null;
				
				if(currentPropNode.getAttributes().getNamedItem(ATTR_VALUE) != null) {
					// If the property node has a "value" attribute, use that as the node's value
					value = currentPropNode.getAttributes().getNamedItem(ATTR_VALUE).getNodeValue();
				} else {
					NodeList valueNodes = currentPropNode.getChildNodes();
					Node valueNode = null;
					for(int k = 0; k < valueNodes.getLength(); k++) {
						if(!valueNodes.item(k).getNodeName().equals(NODE_VALUE)) {
							continue;
						}	
						
						valueNode = valueNodes.item(k);
						break;
					}
					
					if(valueNode != null) {
						// Or else it may be contained within the node as CDATA					
						value = ((CharacterData)valueNode.getFirstChild()).getData();	
					} else {
						throw new Exception("Expected CDATA value but none was specified");
					}
				}
											
				log.debug("Setting "+key+" = "+value);


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
					} else if(field.getType().equals(long.class)){
						field.setLong(null, Long.parseLong(value));
					} else if(field.getType().equals(boolean.class)){
						field.set(null,Boolean.parseBoolean(value));
					}	            
					
					//log.debug(String.format("Loaded property [%s = %s] into class %s", key, field.get(null), className));
				} catch (Exception e){
					log.error(String.format("Failed to load property [%s]. Error [%s]", key, e.getMessage()));
				}
			}				
		}	
	}
	
	/**
	 * @return The name of the currently loaded configuration
	 */
	public static String getConfigName() {						
		return ConfigUtil.configName;
	}
	
	/**
	 * @return The SVN revision number of the build
	 */
	public static String getBuildVersion() {		
		if(buildVersion == null) {
			ConfigUtil.loadBuildProperties();
		}
		
		return ConfigUtil.buildVersion;
	}
	
	/**
	 * @return The user who created the last build
	 */
	public static String getBuildUser() {		
		if(buildUser == null) {
			ConfigUtil.loadBuildProperties();
		}
		
		return ConfigUtil.buildUser;
	}
	
	/**
	 * @return The date the build was created
	 */
	public static Date getBuildDate() {		
		if(buildDate == null) {
			ConfigUtil.loadBuildProperties();
		}
		
		return ConfigUtil.buildDate;
	}
	
	/**
	 * Loads the build properties file and reads in it's values for use throughout the application
	 */
	private static void loadBuildProperties() {
		try {
			SimpleDateFormat buildDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
			
			Properties propFile = new Properties();
			propFile.load(new FileInputStream(new File(R.CONFIG_PATH, "build.properties")));
			
			ConfigUtil.buildVersion = propFile.getProperty("build");
			ConfigUtil.buildDate = buildDateFormat.parse(propFile.getProperty("buildtime"));
			ConfigUtil.buildUser = propFile.getProperty("builder");
			
			log.debug("Loaded build version: " + ConfigUtil.buildVersion);
			log.debug("Loaded build user: " + ConfigUtil.buildUser);
			log.debug("Loaded build date: " + buildDateFormat.format(ConfigUtil.buildDate));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
