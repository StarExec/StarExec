package servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import constants.P;

import data.Database;
import data.to.User;

public class Registration extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public Registration() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Don't accept GETs, just hand back false
		response.setContentType("text/plain");
		response.getWriter().print(false);
	}

	/**
	 * Responds with true or false in plain text indicating if the user was successfully added or not
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Database database = new Database(getServletContext());

		User u = new User(request.getParameter(P.USER_USERNAME));
		u.setAffiliation(request.getParameter(P.USER_AFILIATION));
		u.setEmail(request.getParameter(P.USER_EMAIL));
		u.setFirstName(request.getParameter(P.USER_FIRSTNAME));
		u.setLastName(request.getParameter(P.USER_LASTNAME));
		u.setPassword(request.getParameter(P.USER_PASSWORD));
		
		response.setContentType("text/plain");
		response.getWriter().print(database.addUser(u));
		response.getWriter().close();
	}

}
