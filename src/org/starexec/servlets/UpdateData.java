package org.starexec.servlets;

import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.util.Log;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;

//import jquery.datatables.model.Company;
//import jquery.datatables.model.DataRepository;

/**
 * Handler for the update cell action
 */
@WebServlet("/UpdateData")
public class UpdateData extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * This servlet handles post request from the JEditable and updates company property that is edited
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//This will be the date
		String date = request.getParameter("id");
		
		
		//Column id -- hidden columns are counted
		//int columnId = Integer.parseInt(request.getParameter("columnId"));
		
		//Column # (use this if column names are being changed dynamically) hidden columns not counted
		//int columnPosition = Integer.parseInt(request.getParameter("columnPosition"));
		
		//Name of the column -- use if the column names aren't being changed dynamically
		String columnName = request.getParameter("columnName");
		int queueId = Queues.getIdByName(columnName);


		//Row #
		//int rowId = Integer.parseInt(request.getParameter("rowId"));
		
		int value = 0;
		//Updated value
		if (queueId == 1 || columnName.equals("date") || columnName.equals("total") || columnName.equals("conflict")) {
			response.getWriter().print("error - not editable");
			return;
		} else {
			value = Integer.parseInt(request.getParameter("value"));
		}

		
		

		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		Date reserve_date = null;
		try {
			reserve_date = new Date(sdf.parse(date).getTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}

		
		Cluster.updateNodeCount(value, queueId, reserve_date);
		

		response.getWriter().print(value);
	}


}
