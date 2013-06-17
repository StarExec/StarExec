package org.starexec.app;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.starexec.data.database.Common;
import org.starexec.data.database.Users;
import org.starexec.data.to.Permission;
import org.starexec.data.to.User;
import org.starexec.util.SessionUtil;

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
			// Check if they have the necessary user SessionUtil stored in their session
			if(SessionUtil.getUser((HttpServletRequest)request) == null) {
				// If not, retrieve the user's information from the database
				String userEmail = httpRequest.getUserPrincipal().getName();
				User user = Users.get(userEmail);
				
				// And add it to their session to be used elsewhere
				httpRequest.getSession().setAttribute(SessionUtil.USER, user);
				
				// Also add an empty permission's cache for the user
				httpRequest.getSession().setAttribute(SessionUtil.PERMISSION_CACHE, new HashMap<Integer, Permission>());
				
				// Add the login to the database
				this.logUserLogin(user, httpRequest);				
			}
		}
		
		// Be nice and pass on the request to the next filter
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
		Common.addLoginRecord(user.getId(), ip, rawBrowser);
	}

	@Override
	public void init(FilterConfig args) throws ServletException {
		// Do nothing		
	}
}
