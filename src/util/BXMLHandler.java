package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import data.to.*;

/**
 * Handles a benchmark XML file to build the virtual level structure
 * required to put in the database. Utilizes SAX (Simple Api for XML) version 2
 * @author Tyler
 */
public class BXMLHandler extends DefaultHandler {
	private Stack<Level> dirStack;					// Temporary stack to keep track of the last seen directory
	private HashMap<Integer, Level> finalLevels;	// The final map of levels (key is left position, value is the level object)
	private int currentLevel;						// The current level we're on in terms of processing
	private List<BenchLevel> benchmarks;			// The final list of benchmark level objects to add into the database
	
	@Override
	public void startDocument(){
		currentLevel = 0;							// Initialize everything before parsing begins
		dirStack = new Stack<Level>();
		finalLevels = new HashMap<Integer, Level>();
		benchmarks = new ArrayList<BenchLevel>(20);
	}
	
	@Override
	public void endDocument(){		
		
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes){
		if(localName.equals("bench")){					// If we're starting a benchmark tag
			BenchLevel b = new BenchLevel();			// Create a new benchmark object 
			b.setName(attributes.getValue("name"));		// Get the benchmark's name
			b.setPath(uri);								// TODO: Figure out my relative path
			b.setBelongsTo(currentLevel);				// Set the level the benchmark belongs to to the current level
			benchmarks.add(b);							// Add the benchmark to the list
			System.out.println("Added Benchmark: " + b.getName() + " [" + b.getBelongsTo() + "]");			
		} else if(localName.equals("dir")){				// If we're starting a directory tag...
			Level l = new Level();						// Create a new level object
			l.setLeft(++currentLevel);					// Increment the current level and set it as my left
			l.setName(attributes.getValue("name"));		// Set the name of the level
			dirStack.add(l);							// Add the level to the temporary stack to be processed later 
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName){		
		if(localName.equals("dir")){					// If we're ending a directory tag...
			Level l = dirStack.pop();					// Get the object associated with the start of the tag
			l.setRight(++currentLevel);					// Set the right value to the current level + 1
			finalLevels.put(l.getLeft(), l);			// Put the level in the final hashmap with its left value as the key
			System.out.println("Added Directory: " + l.getName() + " [" + l.getLeft() + ", " + l.getRight() + "]");
		}
	}
	
	public Collection<Level> getLevels(){
		return finalLevels.values();
	}
	
	public List<BenchLevel> getBenchmarks(){
		return benchmarks;
	}
}
