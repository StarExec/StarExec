package com.starexec.app;

import java.io.IOException;
import java.util.Date;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import com.starexec.constants.*;
import com.starexec.data.*;
import com.starexec.data.to.*;

/**
 * This class is responsible for intercepting all requests to protected resources
 * and checking if the user has the appropriate session variables set to continue
 * using the website. As a side-effect, this is where newly logged in users
 * are detected and logged.
 * 
 * @author Tyler Jensen
 */
public class SessionFilter implements Filter {
	private static final Logger log = Logger.getLogger(SessionFilter.class);

	@Override
	public void destroy() {
		// Do nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// Cast the servlet request to an httpRequest so we have access to the session
		HttpServletRequest httpRequest = (HttpServletRequest) request; 		
		
		// If the user is logged in...
		if(httpRequest.getUserPrincipal() != null) {
			// Check if they have the neccessary user object stored in their session
			if(httpRequest.getSession().getAttribute(P.SESSION_USER) == null) {
				// If not, retreive the user's information from the database
				String userEmail = httpRequest.getUserPrincipal().getName();
				User user = Databases.next().getUser(userEmail);
				
				// And add it to their session to be used elsewhere
				httpRequest.getSession().setAttribute(P.SESSION_USER, user);
				
				// Add the login to the database
				this.logUserLogin(user, httpRequest);				
			}
		}
		
		chain.doFilter(request, response);
	}
	
	/**
	 * Adds a record to the database that represents the login
	 * @param user The user that just logged in
	 * @param request The request containing data required to log
	 */
	private void logUserLogin(User user, HttpServletRequest request) {
		// Log the regular application log
		log.info(String.format("%s [%s] logged in.", user.getFullName(), user.getEmail()));
			
		String ip = request.getRemoteAddr();
		String rawBrowser = request.getHeader("user-agent");
		
		// Also save in the database to maintain a historical record
		Databases.next().addLoginRecord(user.getUserId(), ip, rawBrowser);
	}

	@Override
	public void init(FilterConfig args) throws ServletException {
		// Do nothing		
	}
}
