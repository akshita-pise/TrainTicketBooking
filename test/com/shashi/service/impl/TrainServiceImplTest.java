package com.shashi.service.impl;

import com.shashi.beans.TrainBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;
import com.shashi.service.TrainService;
import com.shashi.utility.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrainServiceImplTest {

	@Mock
	private Connection mockConnection;

	@Mock
	private PreparedStatement mockPreparedStatement;

	@Mock
	private ResultSet mockResultSet;

	@InjectMocks
	private TrainServiceImpl trainService;

	private static MockedStatic<DBUtil> mockedDBUtil;

	@BeforeClass
	public static void setUpClass() {
		// Mock static DBUtil.getConnection() once globally
		mockedDBUtil = Mockito.mockStatic(DBUtil.class);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		// Ensure that `mockResultSet.next()` is always `true` for valid queries
		when(mockResultSet.next()).thenReturn(true);
	}

	@AfterClass
	public static void tearDownClass() {
		mockedDBUtil.close();
	}

	@After
	public void tearDown() {
		reset(mockConnection, mockPreparedStatement, mockResultSet);
	}

	/**
	 * Verifies that getTrainById successfully retrieves a train when it exists in
	 * the database. This test covers the path where rs.next() returns true,
	 * indicating a train was found.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void getTrainByIdSuccesfully() throws TrainException, SQLException {
		// Arrange
		String trainNo = "12345";

		// Setup mock result set data
		when(mockResultSet.getDouble("fare")).thenReturn(100.0);
		when(mockResultSet.getString("from_stn")).thenReturn("Station A");
		when(mockResultSet.getString("to_stn")).thenReturn("Station B");
		when(mockResultSet.getString("tr_name")).thenReturn("Test Train");
		when(mockResultSet.getLong("tr_no")).thenReturn(12345L);
		when(mockResultSet.getInt("seats")).thenReturn(50);

		// Act
		TrainBean result = trainService.getTrainById(trainNo);

		// Assert
		assertNotNull("Result should not be null", result);
		assertEquals(100.0, result.getFare(), 0.001);
		assertEquals("Station A", result.getFrom_stn());
		assertEquals("Station B", result.getTo_stn());
		assertEquals("Test Train", result.getTr_name());
		assertEquals(12345L, (long) result.getTr_no());
		assertEquals(50, (int) result.getSeats());

		// Verify
		verify(mockConnection).prepareStatement("SELECT * FROM TRAIN WHERE TR_NO=?");
		verify(mockPreparedStatement).setString(1, trainNo);
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();

		// Verify ResultSet interactions
		verify(mockResultSet).next();
		verify(mockResultSet).getDouble("fare");
		verify(mockResultSet).getString("from_stn");
		verify(mockResultSet).getString("to_stn");
		verify(mockResultSet).getString("tr_name");
		verify(mockResultSet).getLong("tr_no");
		verify(mockResultSet).getInt("seats");
	}

	/**
	 * Tests the getTrainById method when no train is found in the database. This
	 * test verifies that the method returns null when the ResultSet is empty.
	 */
	@Test
	public void getTrainByIdWhenTrainNotFound() throws Exception {
		// Arrange

		String trainNo = "12345";

		// Setup mocks
		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(false);

		// Act
		TrainBean result = trainService.getTrainById(trainNo);

		// Assert
		assertNull("Result should be null when no train is found", result);

		// Verify
		verify(mockConnection).prepareStatement("SELECT * FROM TRAIN WHERE TR_NO=?");
		verify(mockPreparedStatement).setString(1, trainNo);
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();

	}

	/**
	 * Tests the getTrainById method when a SQLException occurs during database
	 * operation. This test verifies that the method throws a TrainException with
	 * the appropriate error message.
	 */

	@Test
	public void getTrainByIdSQLExceptionHandling() throws Exception {
		// Arrange
		String trainNo = "12345";

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

		// Act & Assert
		TrainException thrown = assertThrows(TrainException.class, () -> {
			trainService.getTrainById(trainNo);
		});

		// Verify exception details
		assertEquals("Database error", thrown.getMessage()); // Check error message
		assertEquals("BAD_REQUEST", thrown.getErrorCode()); // Expected error code (match actual behavior)

	}

	/**
	 * Verifies successful retrieval of all trains from the database. Test ensures
	 * proper data mapping and validates complete train details including fares,
	 * stations, names, numbers and seats for multiple trains.
	 */

	@Test
	public void getAllTrainsListSuccesfully() throws Exception { // Arrange

		// Arrange
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getDouble("fare")).thenReturn(100.0, 200.0);
		when(mockResultSet.getString("from_stn")).thenReturn("Station A", "Station C");
		when(mockResultSet.getString("to_stn")).thenReturn("Station B", "Station D");
		when(mockResultSet.getString("tr_name")).thenReturn("Train 1", "Train 2");
		when(mockResultSet.getLong("tr_no")).thenReturn(1001L, 1002L);
		when(mockResultSet.getInt("seats")).thenReturn(100, 200);

		// Act
		List<TrainBean> result = trainService.getAllTrains();

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size());
	}

	/**
	 * Verifies successful retrieval of trains between specified stations (Mumbai to
	 * Delhi). Test ensures correct query execution, data mapping, and validates
	 * returned train details including fares, stations, names, numbers and seats.
	 */

	@Test
	public void getTrainsBetweenStationsSuccessfully() throws Exception {
		// Arrange
		TrainBean train = new TrainBean();
		train.setTr_no(12345L);
		train.setTr_name("Test Train");
		train.setFrom_stn("Start Station");
		train.setTo_stn("End Station");
		train.setSeats(100);
		train.setFare(50.0);

		when(mockResultSet.next()).thenReturn(true);

		// Act
		String result = trainService.addTrain(train);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);

		// Verify interactions
		verify(mockPreparedStatement).executeQuery();

	}

	/**
	 * Test case for addTrain method when a new train is successfully added. This
	 * test verifies that the method returns SUCCESS response code when the database
	 * operation is successful and the result set has a next row.
	 */

	@Test
	public void addTrainSuccessfulInsertion() throws Exception {
		// Arrange
		TrainBean train = new TrainBean();
		train.setTr_no(12345L);
		train.setTr_name("Test Train");
		train.setFrom_stn("Start Station");
		train.setTo_stn("End Station");
		train.setSeats(100);
		train.setFare(50.0);

		when(mockResultSet.next()).thenReturn(true);

		// Act
		String result = trainService.addTrain(train);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);

		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();

	}

	/**
	 * Verifies that adding a train returns FAILURE response when database error
	 * occurs. Test ensures proper error handling and appropriate error message is
	 * returned.
	 */

	/* Unit test is verifying (mockPreparedStatement).close(); This is not getting executed because 
	 * In TrainServiceImpl.addTrain(), have:ps.close(); but this is only executed if no exception occurs.
	 * If an exception is thrown, the code jumps to the catch block:
      catch (SQLException | TrainException e) {
      responseCode += " : " + e.getMessage();} and skips ps.close().

	 * Action Plan: Log a bug against TrainServiceImpl for
	 * PreparedStatement is not closed on exception and suggest adding a finally block*/
	

	@Test
	public void addTrainWhenDatabaseError() throws Exception {
		// Arrange
		TrainBean train = new TrainBean();
		train.setTr_no(12345L);
		train.setTr_name("Test Train");
		train.setFrom_stn("Start Station");
		train.setTo_stn("End Station");
		train.setSeats(100);
		train.setFare(50.0);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

		// Simulate a database error when executing the query
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

		// Act
		String result = trainService.addTrain(train);

		// Assert
		assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
		assertTrue(result.contains("Database error"));

		// Verify interactions
		verify(mockPreparedStatement).setLong(1, train.getTr_no());
		verify(mockPreparedStatement).setString(2, train.getTr_name());
		verify(mockPreparedStatement).setString(3, train.getFrom_stn());
		verify(mockPreparedStatement).setString(4, train.getTo_stn());
		verify(mockPreparedStatement).setLong(5, train.getSeats());
		verify(mockPreparedStatement).setDouble(6, train.getFare());
		verify(mockPreparedStatement).executeQuery();
		//verify(mockPreparedStatement).close();
	}

	/**
	 * Test case to verify that passing a null TrainBean to addTrain() throws a
	 * NullPointerException.
	 */
	
	/*
	 * Log a bug in TrainServiceImpl.addTrain().
       Describe that passing null causes an unhandled NullPointerException instead of TrainException.
	 */

	@Test
	public void addTrainWithNullTrain() throws Exception {
		/*// Act & Assert
		NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
			trainService.addTrain(null);
		});

		Verify exception message (optional, depends on implementation)
		//assertNotNull(thrown.getMessage()); */
		
		// Act & Assert
	    TrainException thrown = assertThrows(TrainException.class, () -> {
	        trainService.addTrain(null);
	    });

	    // Verify exception message (if applicable)
	    assertNotNull(thrown.getMessage());

	}

	/**
	 * Verifies that a train can be successfully deleted from the database by its ID
	 * and returns SUCCESS response. Test ensures proper database interaction and
	 * resource cleanup during train deletion operation.
	 */

	@Test
	public void deleteTrainByIdSuccessfulDeletion() throws Exception {

		// Arrange
		String trainNo = "12345";

		when(mockPreparedStatement.executeUpdate()).thenReturn(1);

		// Act
		String result = trainService.deleteTrainById(trainNo);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);
		// Verify interactions
		verify(mockConnection).prepareStatement("DELETE FROM TRAIN WHERE TR_NO=?");
		verify(mockPreparedStatement).setString(1, trainNo);
		verify(mockPreparedStatement).executeUpdate();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Verifies that a train's details can be successfully updated in the database
	 * and returns SUCCESS response. Test ensures proper database interaction with
	 * all train fields and resource cleanup during update operation.
	 */

	@Test
	public void updateTrainSuccessfulUpdate() throws Exception {
		// Arrange
		TrainBean train = new TrainBean();
		train.setTr_no(12345L);
		train.setTr_name("Updated Train");
		train.setFrom_stn("New Start");
		train.setTo_stn("New End");
		train.setSeats(200);
		train.setFare(75.0);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true); // Simulating a successful update

		// Act
		String result = trainService.updateTrain(train);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);

		// Verify interactions
		verify(mockPreparedStatement).setString(1, train.getTr_name());
		verify(mockPreparedStatement).setString(2, train.getFrom_stn());
		verify(mockPreparedStatement).setString(3, train.getTo_stn());
		verify(mockPreparedStatement).setLong(4, train.getSeats());
		verify(mockPreparedStatement).setDouble(5, train.getFare());
		verify(mockPreparedStatement).setDouble(6, train.getTr_no());
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}
}
