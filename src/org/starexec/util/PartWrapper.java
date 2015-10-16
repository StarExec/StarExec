package org.starexec.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;


public class PartWrapper {
	Part p = null;
	private String filePath = null;
	public PartWrapper(Part p){
		this.p = p;
		setFilenameFromHeaders();
	}
	
	private void setFilenameFromHeaders() {
        for (String header : p.getHeader("content-disposition").split(";")) {
            if (header.trim().startsWith("filename=")) {
                this.filePath = (header.substring(header.indexOf('=') + 1).trim().replace("\"", ""));
                return;
            }
        }
    }
	
	public boolean isFile() {
		return getFilePath()!=null;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public String getName() {
		return Paths.get(filePath).getFileName().toString();
	}
	
	/**
	 * Retrieves the contents of this Part as a string using the default
	 * character encoding
	 * @return
	 * @throws IOException 
	 */
	public String getString() throws IOException {
		return IOUtils.toString(this.p.getInputStream());
	}
	
	/**
	 * Writes out this part to the given file.
	 * @param f
	 * @throws IOException
	 */
	public void write(File f) throws IOException {
		FileOutputStream output = new FileOutputStream(f);
    	IOUtils.copy(this.p.getInputStream(), output);
    	output.close();
	}
	
}
