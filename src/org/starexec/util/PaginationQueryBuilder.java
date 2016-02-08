package org.starexec.util;

/**
 * This is a simple class that is used to build up pagination queries, which for us
 * are queries that have an order by, a starting record, and a number of records
 * 
 * Note that the insertion of an order column, which is a string, is NOT SAFE against SQL injection!
 * This class is designed to be used ONLY for queries where such strings are provided
 * by Starexec, NOT by users. Other columns are numeric, and so are safe against SQL injection.
 * @author Eric
 *
 */

public class PaginationQueryBuilder {

	
	private String getOrderDirectionString() {
		if (query.isSortASC()) {
			return "ASC";
		}
		return "DESC";
	}
	DataTablesQuery query = null;
	String orderColumn;
	/**
	 * This is the SQL query without any order by statement or limit statement, and also without a 
	 * closing semicolon
	 */
	private String baseSQL = null;
	
	public PaginationQueryBuilder(String sql, String orderColumn, DataTablesQuery query) {
		baseSQL = sql;
		this.query=query;
		this.orderColumn=orderColumn;
	}
	
	public String getSQL() { 
		StringBuilder sb = new StringBuilder();
		
		sb.append(baseSQL);
		sb.append("\n");
		sb.append("ORDER BY ");
		sb.append(orderColumn);
		sb.append(" ");
		sb.append(getOrderDirectionString());
		sb.append("\n");
		sb.append("LIMIT ");
		sb.append(query.getStartingRecord());
		sb.append(", ");
		sb.append(query.getNumRecords());
		sb.append(";");
		return sb.toString();
	}
}
