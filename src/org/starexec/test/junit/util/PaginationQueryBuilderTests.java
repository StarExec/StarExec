package org.starexec.test.junit.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.PaginationQueryBuilder;


public class PaginationQueryBuilderTests {

	private static String baseSQL = "select * from users";
	private static DataTablesQuery query = null;
	
	@Before
	public void setupQuery() {
		query = new DataTablesQuery();
		query.setNumRecords(5);
		query.setStartingRecord(3);
		query.setSortASC(false);
	}
	
	@Test
	public void testGetSQLDESC() {
		PaginationQueryBuilder b = new PaginationQueryBuilder(baseSQL, "first_name", query);
		String expected = "select * from users\nORDER BY first_name DESC\nLIMIT 3, 5;";
		Assert.assertEquals(expected, b.getSQL());
	}
	
	@Test
	public void testGetSQLASC() {
		query.setSortASC(true);
		PaginationQueryBuilder b = new PaginationQueryBuilder(baseSQL, "first_name", query);
		String expected = "select * from users\nORDER BY first_name ASC\nLIMIT 3, 5;";
		Assert.assertEquals(expected, b.getSQL());
		
	}
}
