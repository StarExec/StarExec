package org.starexec.util;

import org.starexec.constants.R;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.logger.StarLogger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
import java.io.File;
import java.io.IOException;

/**
 * Contains functionality shared between JobUtil, BatchUtil, and JobToXMLer
 *
 * @author Eric
 */
public class XMLUtil {
	private static final StarLogger log = StarLogger.getLogger(XMLUtil.class);

	/**
	 * Validates an XML document using a schema
	 *
	 * @param file The XML file
	 * @param schemaLoc The absolute path to the schema
	 * @return A ValidatorStatusCode containing true if the validation was successful and false plus an
	 * error message otherwise
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public static ValidatorStatusCode validateAgainstSchema(File file, String schemaLoc) throws
			ParserConfigurationException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);//This is true for DTD, but not W3C XML Schema that we're using
		factory.setNamespaceAware(true);

		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

		try {
			factory.setSchema(schemaFactory.newSchema(new Source[]{new StreamSource(schemaLoc)}));
			Schema schema = factory.getSchema();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(file);
			Validator validator = schema.newValidator();
			DOMSource source = new DOMSource(document);
			validator.validate(source);
			log.debug("XML File has been validated against the schema.");
			return new ValidatorStatusCode(true);
		} catch (SAXException ex) {
			final String message = "File '" + file.getName() + "' is not valid because: \"" + ex.getMessage() + "\"";
			log.warn(message);
			return new ValidatorStatusCode(false, message);
		}

	}

	/**
	 * Generates a new, empty XML Document object
	 *
	 * @return The new Document
	 * @throws ParserConfigurationException
	 */
	public static Document generateNewDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		return docBuilder.newDocument();
	}

	/**
	 * Writes XML Document object out to a file and returns that file
	 *
	 * @param relPath The path to where the file should be place, relative to R.STAREXEC_ROOT.
	 * Any spaces will be removed from the name
	 * @param doc
	 * @return The File where the XML document was saved
	 * @throws Exception
	 */
	public static File writeDocumentToFile(String relPath, Document doc) throws Exception {
		//no spaces are permitted at the top level
		relPath = relPath.replaceAll("\\s+", "");
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);

		//we can't let the top level have spaces in the name
		File file = new File(R.STAREXEC_ROOT, relPath);
		log.debug(file.getAbsolutePath());
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		return file;
	}
}
