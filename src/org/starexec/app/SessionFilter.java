package org.starexec.app;

import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.Permission;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;

/**
 * This class is responsible for intercepting all requests to protected resources
 * and checking if the user has the appropriate session variables set to continue
 * using the website. As a side-effect, this is where newly logged in users
 * are detected and logged.
 *
 * @author Tyler Jensen
 */
public class SessionFilter implements Filter {
	private static final StarLogger log = StarLogger.getLogger(SessionFilter.class);

	/**
	 * Detects requests originating from StarExecCommand
	 * @param request
	 * @return true if request is from StarExecCommand, false otherwise
	 */
	private static final boolean isFromCommand(HttpServletRequest request) {
		return request.getHeader("StarExecCommand") != null;
	}

	@Override
	public void destroy() {
		// Do nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			final String method = "doFilter";
			// Cast the servlet request to an httpRequest so we have access to the session
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			log.debug(method, "Request URI: "+httpRequest.getRequestURI());

			boolean isCommandRequest = isFromCommand(httpRequest);
			if (isCommandRequest) {
				log.debug(method, "Got request from StarExecCommand.");
			}

			HttpSession session = httpRequest.getSession();
			log.debug(method, "isRequestedSessionIdFromURL: "+httpRequest.isRequestedSessionIdFromURL());
			log.debug(method, "isRequestedSessionIdFromCookie: "+httpRequest.isRequestedSessionIdFromCookie());
			log.debug(method, "isRequestedSessionIdValid: "+httpRequest.isRequestedSessionIdValid());
			log.debug(method, "authType: "+httpRequest.getAuthType());
			Cookie[] cookies = httpRequest.getCookies();
			if ( cookies != null ) {
				log.debug(method, "Cookies: ");
				for (Cookie cookie : cookies) {
					log.debug(method, "\tName : "+cookie.getName());
					log.debug(method, "\tValue: "+cookie.getValue());
				}
			} else {
				log.debug(method, "Cookies was null");
			}
			if (session != null) {
				log.debug(method, "Session ID: "+session.getId());
			} else {
				log.debug(method, "Session was null.");
			}

			HttpServletResponse httpResponse = (HttpServletResponse) response;
			// If the user is logged in...
			if (httpRequest.getUserPrincipal() != null) {
				String userEmail = httpRequest.getUserPrincipal().getName();
				// Check if they have the necessary user SessionUtil stored in their session
				int userId = SessionUtil.getUser(httpRequest).getId();
				log.debug(method, "User Id of request was: " + userId);
				log.debug(method, "User email of request was: " + userEmail);
				if (userId == R.PUBLIC_USER_ID) {
					log.debug(method, "User is currently set to public user... Logging user in.");
					// If not, retrieve the user's information from the database
					User user = Users.get(userEmail);

					// And add it to their session to be used elsewhere
					httpRequest.getSession().setAttribute(SessionUtil.USER, user);

					// Also add an empty permission's cache for the user
					httpRequest.getSession().setAttribute(SessionUtil.PERMISSION_CACHE, new HashMap<Integer, Permission>());

					// Add the login to the database
					this.logUserLogin(user, httpRequest);
				}
				if (R.DEBUG_MODE_ACTIVE) {
					log.debug(method, "Debug mode is active.");
					if (!GeneralSecurity.hasAdminReadPrivileges(Users.get(userEmail).getId())) {
						log.debug(method, "User does not have admin read privileges, redirecting to index...");
						httpRequest.getSession().invalidate();
						httpResponse.sendRedirect(Util.docRoot(""));
						return;
					}
				}
				User user = SessionUtil.getUser(httpRequest);
				log.debug(method, "User role was found to be "+user.getRole());
				//suspended and unauthorized users cannot utilize the system: always place them back on the index page
				//whenever they try to access anything secure.
				if (user.getRole().equals(R.SUSPENDED_ROLE_NAME) || user.getRole().equals(R.UNAUTHORIZED_ROLE_NAME)) {
					if (!httpRequest.getRequestURI().equals("/" + R.STAREXEC_APPNAME + "/")) {
						log.debug(method, "Redirecting "+user.getRole()+" user to index.");
						httpResponse.sendRedirect(Util.docRoot(""));
					}
				}
			} else {
				log.debug(method, "httpRequest.getUserPrincipal() returned null.");
				if (isCommandRequest) {
					httpResponse.setHeader("CommandBadCredentials","CommandBadCredentials");
				}

			}

			// Be nice and pass on the request to the next filter
			chain.doFilter(request, response);
		} catch (Throwable t) {
			log.debug("Caught throwable in doFilter. ", t);
			throw t;
		}
	}

	/**
	 * Adds a record to the database that represents the login
	 * @param user The user that just logged in
	 * @param request The request containing data required to log
	 */
	private void logUserLogin(User user, HttpServletRequest request) {
		// Log the regular application log
		log.info(String.format("%s [%s] logged in.", user.getFullName(), user.getEmail()));

		if (isFromCommand(request)) {
			Analytics.STAREXECCOMMAND_LOGIN.record();
		}

		String ip = request.getRemoteAddr();
		String rawBrowser = request.getHeader("user-agent");

		// Also save in the database to maintain a historical record
		Common.addLoginRecord(user.getId(), ip, rawBrowser);
		// Get the number of unique logins that have occurred since the last report was sent and
		// record it in the reports table.
		Integer uniqueLogins = Logins.getNumberOfUniqueLogins();
		if (uniqueLogins != null) {
			log.debug("Number of unique logins: " + uniqueLogins);
			Reports.setEventOccurrencesNotRelatedToQueue("unique logins", uniqueLogins);
		} else {
			log.error("Could not get number of unique logins from logins table.");
		}
	}

	@Override
	public void init(FilterConfig args) throws ServletException {
		// Do nothing
	}
}
