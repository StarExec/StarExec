package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import com.jamesmurty.utils.XMLBuilder;

import constants.R;

public class ZipXMLConverter {
	public static final String DIR_NAME = "dir";
	public static final String BENCHMARK_NAME = "bench";
	public static final String NAME_ATTR = "name";
	/**
	 * Takes in a file path which corresponds to the root directory
	 * which holds all benchmarks and outputs the file object
	 * the corresponding xml file that is used to populate the
	 * database. Ideally, this structure is an extracted zip file.
	 * @param zipFileName The root path of the directory structure to convert
	 * @param outPath The path to the directory where the built XML file will go
	 * @return The file object associated with the newly created XML file
	 * @throws Exception  
	 */
	public static File fileToXml(String rootPath, String outPath) throws Exception{
		File root = new File(rootPath);								// Grab the file object associated with the root path
		XMLBuilder xml = XMLBuilder.create("STAREXEC");				// Create a new xml builder with the STAREXEC root
		
		convertDirectory(root, xml);								// Convert the directory to xml with the current builder

		File xmlFile = new File(outPath, R.XML_OUTPUT_NAME); 		// Create the file handle for the output file
		PrintWriter fileOut = new PrintWriter(new FileOutputStream(xmlFile));	// Create the output stream to the file
		
		Properties props = new Properties();   						// Set up our output properties
		props.put(javax.xml.transform.OutputKeys.METHOD, "xml"); 	// Explicitly identify the output as an XML document   
		props.put(javax.xml.transform.OutputKeys.INDENT, "yes"); 	// Pretty-print the XML output
		props.put("{http://xml.apache.org/xslt}indent-amount", "5");// Set the indentation amount to 3 spaces
		
		xml.toWriter(fileOut, props);								// Write the xml to the output file
		fileOut.close();											// Close the output file
		
		return xmlFile;												// Give back the output file object
	}
	
	/**
	 * Converts an entire directory (and any subdirectory) to xml
	 * @param dir The file that corresponds to the root directory to convert
	 * @param xml The xml builder to use which initially consists of the xml root
	 */
	private static void convertDirectory(File dir, XMLBuilder xml){		
		xml = xml.e(DIR_NAME).a(NAME_ATTR, dir.getName());			// Create the element for the current directory
		
		for(File file : dir.listFiles(fileFilter)){					// For each benchmark in the directory...
			xml.e(BENCHMARK_NAME).a(NAME_ATTR, file.getName());		// Create a new element for each benchmark and assign its name attribute to the file name
		}
		
		for(File directory : dir.listFiles(dirFilter)){				// For each subdirectory in the directory...
			convertDirectory(directory, xml);						// Convert the subdirectory to xml under the current builder
		}	
	}
	
	/**
	 * Filter used to extract directories from a file object
	 */
	private static FileFilter dirFilter = new FileFilter() {
	    public boolean accept(File file) {
	        return file.isDirectory();
	    }
	};
	
	/**
	 * Filter used to extract files (non-directories) from a file object
	 */
	private static FileFilter fileFilter = new FileFilter() {
	    public boolean accept(File file) {
	        return file.isFile();
	    }
	};
}
