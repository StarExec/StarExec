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

import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;

//import jquery.datatables.model.Company;
//import jquery.datatables.model.DataRepository;

/**
 * Handler for the update cell action
 */
@WebServlet("/UpdateData")
public class UpdateData extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * This servlet handles post request from the JEditable and updates node_count property that is edited
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//This boolean indicates if the column being changed is the new queue
		boolean isNewQueue = false;
		
		//This will be the date
		String date = request.getParameter("id");
		
		
		//Column # -- hidden columns are counted
		//int columnId = Integer.parseInt(request.getParameter("columnId"));
				
		//Column # (use this if column names are being changed dynamically) hidden columns not counted
		//int columnPosition = Integer.parseInt(request.getParameter("columnPosition"));
				
		//Name of the column -- use if the column names aren't being changed dynamically
		String columnName = request.getParameter("columnName");
		String code = columnName;
		
		QueueRequest req = null;
		String queueName = null;
		int queueId = Queues.getIdByName(columnName);
		if (queueId == -2) {
			req = Requests.getQueueRequest(code);
			isNewQueue = true;
		}

		//Row #
		//int rowId = Integer.parseInt(request.getParameter("rowId"));
		
		int defaultQueueId = Cluster.getDefaultQueueId();
		
		int value = 0;
		//Updated value
		if (queueId == defaultQueueId || columnName.equals("date") || columnName.equals("total") || columnName.equals("conflict")) {
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

		int space_id = 0;
		
		
		if (isNewQueue) {
			queueName = req.getQueueName();
			space_id = Requests.getQueueRequestSpaceId(queueName);
		} else {
			Queue q = Queues.get(queueId);
			queueName = q.getName();
			space_id = Requests.getQueueReservationSpaceId(queueId);
		}
		
		Cluster.addTempNodeChange(space_id, queueName, value, reserve_date);		

		response.getWriter().print(value);
	}


}
