package org.starexec.util;
/**
 * This class encapsulates the fields set by a JS DataTables object to the server
 * to request a new page
 * @author Eric
 *
 */
public class DataTablesQuery {

	private String searchQuery =null;
	private boolean sortASC;
	private int startingRecord;
	private int numRecords;
	private int sortColumn;
	private int totalRecords;
	private int totalRecordsAfterQuery;
	private int syncValue;

	public DataTablesQuery() {
		
	}
	public DataTablesQuery(int totalRecords, int totalRecordsAfterQuery, int syncValue) {
		this.totalRecords = totalRecords;
		this.totalRecordsAfterQuery=totalRecordsAfterQuery;
		this.syncValue=syncValue;
	}
	
	public DataTablesQuery(int startRecord, int numRecords, int sortColumn, boolean ASC, String query) {
		this.startingRecord=startRecord;
		this.numRecords=numRecords;
		this.sortColumn=sortColumn;
		this.sortASC=ASC;
		this.searchQuery=query;
	}
	
	public boolean isSortASC() {
		return sortASC;
	}
	
	public void setSortASC(boolean sortASC) {
		this.sortASC = sortASC;
	}
	/**
	 * 
	 * @return The search query for this request. Empty means no query
	 */
	public String getSearchQuery() {
		if (searchQuery==null) {
			return "";
		}
		return searchQuery;
	}
	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}
	public int getSortColumn() {
		return sortColumn;
	}
	public void setSortColumn(int sortColumn) {
		this.sortColumn = sortColumn;
	}
	public int getTotalRecords() {
		return Math.max(totalRecords, 0);
	}
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}
	public int getTotalRecordsAfterQuery() {
		return Math.max(totalRecordsAfterQuery,0);
	}
	public void setTotalRecordsAfterQuery(int totalRecordsAfterQuery) {
		this.totalRecordsAfterQuery = totalRecordsAfterQuery;
	}
	public int getStartingRecord() {
		return startingRecord;
	}
	public void setStartingRecord(int startingRecord) {
		this.startingRecord = startingRecord;
	}
	public int getNumRecords() {
		return numRecords;
	}
	public void setNumRecords(int numRecords) {
		this.numRecords = numRecords;
	}
	public int getSyncValue() {
		return syncValue;
	}
	public void setSyncValue(int syncValue) {
		this.syncValue = syncValue;
	}
	
	public boolean hasSearchQuery() {
		return !Util.isNullOrEmpty(this.getSearchQuery());
	}
}
