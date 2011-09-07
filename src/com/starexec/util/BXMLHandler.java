package com.starexec.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.starexec.data.to.*;


/**
 * Handles a benchmark XML file to build the virtual level structure
 * required to put in the database. Utilizes SAX (Simple Api for XML) version 2
 * @author Tyler
 */
public class BXMLHandler extends DefaultHandler {
	private Stack<Level> dirStack;					// Temporary stack to keep track of the last seen directory	
	private int currentDepth;						// What our current depth is
	private HashMap<Integer, Level> finalLevels;	// The final map of levels (key is left position, value is the level object)
	private int currentLevel;						// The current level we're on in terms of processing
	private List<Benchmark> benchmarks;				// The final list of benchmark level objects to add into the database
	private String root;							// The path to where the xml file we're processing is at (used to derive absolute paths for benchmarks)
	private File fullPath;							// The current path we're at in our processing
	private int userId;								// The id of the user who owns the benchmarks
	private int communityId;						// The id of community the benchmark belongs to
	
	/**
	 * @param rootPath The root directory of the benchmarks (Where the XML file is located)
	 */
	public BXMLHandler(String rootPath, int userid, int communityId){
		this.root = rootPath;		
		this.userId = userid;
		this.communityId = communityId;
	}
	
	@Override
	public void startDocument(){
		currentLevel = 0;							// Initialize everything before parsing begins
		dirStack = new Stack<Level>();
		finalLevels = new HashMap<Integer, Level>();
		benchmarks = new ArrayList<Benchmark>(20);
		fullPath = new File(root);
		currentDepth = -1;
		
	}
	
	@Override
	public void endDocument(){		
		
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes){
		if(localName.equals("bench")){					// If we're starting a benchmark tag			
			Benchmark b = new Benchmark();				// Create a new benchmark object 			
			b.setPath(new File(fullPath, attributes.getValue("name")).getAbsolutePath());
			b.setLevel(currentLevel);					// Set the level the benchmark belongs to to the current level
			b.setUserId(userId);						// Set the userid to the id of the owner
			b.setCommunityId(communityId);				// Set the community id of the benchmark
			benchmarks.add(b);							// Add the benchmark to the list
		} else if(localName.equals("dir")){				// If we're starting a directory tag...			
			Level l = new Level();						// Create a new level object
			l.setDepth(++currentDepth);
			l.setLeft(++currentLevel);					// Increment the current level and set it as my left
			l.setName(attributes.getValue("name"));		// Set the name of the level
			l.setUserId(userId);						// Set the user who owns the level
			l.setCommunityId(communityId);				// Set the community the level belongs to
			dirStack.add(l);							// Add the level to the temporary stack to be processed later
			fullPath = new File(fullPath, l.getName());			
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName){		
		if(localName.equals("dir")){					// If we're ending a directory tag...
			Level l = dirStack.pop();					// Get the object associated with the start of the tag
			l.setRight(++currentLevel);					// Set the right value to the current level + 1
			finalLevels.put(l.getLeft(), l);			// Put the level in the final hashmap with its left value as the key
			fullPath = fullPath.getParentFile();
			currentDepth--;
		}
	}
	
	public Collection<Level> getLevels(){
		return finalLevels.values();
	}
	
	public List<Benchmark> getBenchmarks(){
		return benchmarks;
	}	
}
