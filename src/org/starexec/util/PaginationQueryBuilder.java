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
		if (isASC()) {
			return "ASC";
		}
		return "DESC";
	}
	
	private long startingRecord = 0;
	private long numRecords = 0;
	private String orderColumn = null;
	private boolean isASC = false;
	
	/**
	 * This is the SQL query without any order by statement or limit statement, and also without a 
	 * closing semicolon
	 */
	private String baseSQL = null;
	
	public PaginationQueryBuilder(String sql) {
		baseSQL = sql;
	}
	
	public PaginationQueryBuilder(String sql, long startingRecord, long numRecords, String orderColumn, boolean asc) {
		baseSQL = sql;
		this.startingRecord=startingRecord;
		this.numRecords=numRecords;
		this.orderColumn=orderColumn;
		this.isASC=asc;
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
		sb.append(startingRecord);
		sb.append(", ");
		sb.append(numRecords);
		sb.append(";");
		return sb.toString();
	}
	
	

	public long getStartingRecord() {
		return startingRecord;
	}

	public void setStartingRecord(long startingRecord) {
		this.startingRecord = startingRecord;
	}

	public String getOrderColumn() {
		return orderColumn;
	}

	public void setOrderColumn(String orderColumn) {
		this.orderColumn = orderColumn;
	}

	public long getNumRecords() {
		return numRecords;
	}

	public void setNumRecords(long numRecords) {
		this.numRecords = numRecords;
	}

	public boolean isASC() {
		return isASC;
	}

	public void setASC(boolean isASC) {
		this.isASC = isASC;
	}
	
}
