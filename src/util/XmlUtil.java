package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Properties;

import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.*;

import com.jamesmurty.utils.XMLBuilder;

import constants.R;

public class XmlUtil {

	
	/**
	 * Takes in a file path which corresponds to the root directory
	 * which holds all benchmarks and outputs the corresponding xml 
	 * file that is used to populate the database. Ideally, this structure 
	 * is an extracted zip file.
	 * @param rootPath The root path of the directory structure to convert (converts everything under the directory, not including itself)
	 * @param outPath The path to the directory where the built BXML file will go
	 * @return The file object associated with the newly created BXML file
	 * @throws Exception  
	 */
	public static File dirToBXml(String rootPath, String outPath) throws Exception{
		File root = new File(rootPath);								// Grab the file object associated with the root path
		XMLBuilder xml = XMLBuilder.create("STAREXEC");				// Create a new xml builder with the STAREXEC root
		
		convertDirectory(firstDir(root), xml);						// Convert the directory to xml with the current builder

		File xmlFile = new File(outPath, R.BXML_OUTPUT_NAME); 		// Create the file handle for the output file
		PrintWriter fileOut = new PrintWriter(new FileOutputStream(xmlFile));	// Create the output stream to the file
		
		Properties props = new Properties();   						// Set up our output properties
		props.put(javax.xml.transform.OutputKeys.METHOD, "xml"); 	// Explicitly identify the output as an XML document   
		props.put(javax.xml.transform.OutputKeys.INDENT, "yes"); 	// Pretty-print the XML output
		props.put("{http://xml.apache.org/xslt}indent-amount", "5");// Set the indentation amount to 3 spaces
		
		xml.toWriter(fileOut, props);								// Write the xml to the output file
		fileOut.close();											// Close the output file
		
		return xmlFile;												// Give back the output file object
	}
	
	private static File firstDir(File parentDir) throws Exception{
		File[] directories = parentDir.listFiles(XmlUtil.dirFilter);
		if(directories.length == 1)
			return directories[0];
		else
			throw new Exception("Cannot have more than one root folder for a benchmark set.");
	}
	
	/**
	 * Takes in a file path which corresponds to the root directory
	 * which holds all benchmarks and outputs the corresponding xml
	 * in the same directory
	 * @param zipFileName The root path of the directory structure to convert
	 * @return The file object associated with the newly created BXML file
	 * @throws Exception  
	 */
	public static File dirToBXml(String rootPath) throws Exception{
		return dirToBXml(rootPath, rootPath);
	}
	
	/**
	 * Parses a bxml file and builds a list of levels and benchmarks based on the structure
	 * of the fie. The bxml file location is used to determine the absolute paths to the benchmark
	 * files.
	 * @param bxmlFile The bxml file to parse
	 * @return The handler that contains the results from the parse
	 * @throws Exception
	 */
	public static BXMLHandler parseBXML(File bxmlFile) throws Exception{
		BXMLHandler handler = new BXMLHandler(bxmlFile.getParent());	// Create a new BXML handler rooted at the directory of the bxml file        
		XMLReader xr = XMLReaderFactory.createXMLReader();				// Create a new SAX parser to parse the xml
        xr.setContentHandler(handler);									// Set the handler to our custom benchmark XML handler
        xr.parse(new InputSource(new FileReader(bxmlFile)));			// Parse the generated file!
        return handler;													// Return the handler so we can gather results
	}	
	
	/**
	 * Converts an entire directory (and any subdirectory) to xml
	 * @param dir The file that corresponds to the root directory to convert
	 * @param xml The xml builder to use which initially consists of the xml root
	 */
	private static void convertDirectory(File dir, XMLBuilder xml){		
		xml = xml.e(R.BXML_DIR_NAME).a(R.BXML_NAME_ATTR, dir.getName());			// Create the element for the current directory
		
		for(File file : dir.listFiles(fileFilter)){					// For each benchmark in the directory...
			xml.e(R.BXML_BENCH_NAME).a(R.BXML_NAME_ATTR, file.getName());		// Create a new element for each benchmark and assign its name attribute to the file name
		}
		
		for(File directory : dir.listFiles(dirFilter)){				// For each subdirectory in the directory...
			convertDirectory(directory, xml);						// Convert the subdirectory to xml under the current builder
		}	
	}
	
	
	/**
	 * Filter used to extract directories from a file object
	 */
	public static FileFilter dirFilter = new FileFilter() {
	    public boolean accept(File file) {
	        return file.isDirectory();
	    }
	};
	
	/**
	 * Filter used to extract files (non-directories) from a file object
	 */
	public static FileFilter fileFilter = new FileFilter() {
	    public boolean accept(File file) {
	        return file.isFile();
	    }
	};
}
