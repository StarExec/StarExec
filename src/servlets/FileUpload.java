package servlets;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import com.google.gson.Gson;

/**
 * Servlet implementation class FileUpload
 */
@WebServlet(description = "Services incoming file upload requests", urlPatterns = { "/FileUpload" })
public class FileUpload extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileUpload() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO: Clean this mess up!
		Gson gson = new Gson();
		boolean success = false; 
		response.setContentType("text/plain");
		
		try{
			if (isValidFileInputForm(request)) {									// If the form is a valid file upload submission...			
		 		DataInputStream in = new DataInputStream(request.getInputStream());	// Get the input stream from the request				
				
				int contentLen = request.getContentLength();						// How large is the file we're uploading? 
				int byteRead = 0;													// How many bytes are read in a single pass
				int totalBytesRead = 0;												// How many bytes have been read overall
				byte[] buffer = new byte[contentLen];					 			// Create the buffer to hold the data that is the same size as the file length
				
				while (totalBytesRead < contentLen) {								// While we haven't read all the data...
					byteRead = in.read(buffer, totalBytesRead, contentLen);			// Read data from the stream and place into the buffer
					totalBytesRead += byteRead;										// Increment how many bytes we've seen
				}
				
				String strFile = new String(buffer);								// Convert the buffer into a raw string			
				int boundaryIndex = request.getContentType().lastIndexOf("=");		// Get the starting position of the boundary string
				String boundary = request.getContentType().substring(boundaryIndex + 1, request.getContentType().length()); // Extract the boundary string (which is what separates files in the request)
				int pos = strFile.indexOf("filename=\"");							// Get the position of the file name
				String fileExtension = strFile.substring(pos, strFile.indexOf("\n", pos));
				fileExtension = fileExtension.substring(fileExtension.lastIndexOf("."), fileExtension.lastIndexOf("\""));			
				
				pos = strFile.indexOf("\n", pos) + 1;								// Move 3 lines down to locate the start of the file data
				pos = strFile.indexOf("\n", pos) + 1;
				pos = strFile.indexOf("\n", pos) + 1;
				int boundaryLocation = strFile.indexOf(boundary, pos) - 4;			// Find the next boundary location
				int startPos = ((strFile.substring(0, pos)).getBytes()).length;		// Set the start position to the start of the file data
				int dataLen = ((strFile.substring(0, boundaryLocation)).getBytes()).length;	// Set the length of how long the actual file data is 
	 
				// TODO: Determine what we should name the file and where to put it!
				DateFormat shortDate = new SimpleDateFormat("yyyyMMdd-kk.mm.ss");
				FileOutputStream fileOut = new FileOutputStream("/home/starexec/Solvers/" + shortDate.format(new Date()) + ".upload" + fileExtension);			// Create the file stream			
				fileOut.write(buffer, startPos, (dataLen - startPos));				// Write the buffer to the file stream
				fileOut.flush();													// Clean up!
				fileOut.close();
				
				success = true;
			}
		} catch (Exception e) {			
			StringBuilder sb = new StringBuilder();
			for(StackTraceElement ste : e.getStackTrace()){
				sb.append(ste.toString());
			}
			Logger.getLogger("starexec").severe(sb.toString());
		}
		
		String json = gson.toJson(success);
		response.getWriter().write(json);
	}
	
	/**
	 * Makes sure the incoming request is the proper type for file uploading
	 * and actually has content to send.
	 * @param request The http request to check
	 * @return True if the form submitted a valid file upload request, false if otherwise
	 */
	private boolean isValidFileInputForm(HttpServletRequest request){
		String contentType = request.getContentType();
		return ((contentType != null) && !contentType.isEmpty() 
				&& contentType.contains("multipart/form-data") && request.getContentLength() > 0);
	}
}
