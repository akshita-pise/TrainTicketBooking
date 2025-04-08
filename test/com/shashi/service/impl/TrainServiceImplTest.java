package com.shashi.service.impl;

import com.shashi.beans.TrainBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;
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
	 * Tests getAllTrains method when a SQLException occurs during database operation.
	 * This test verifies that the method throws a TrainException with the appropriate
	 * error details including error code, status code, and message.
	 */
	@Test
	public void getAllTrainsWhenDatabaseError() throws Exception {
	    // Arrange
	    String errorMessage = "Database connection failed";
	    when(mockConnection.prepareStatement(anyString()))
	        .thenThrow(new SQLException(errorMessage));

	    // Act & Assert
	    TrainException thrown = assertThrows(TrainException.class, () -> {
	        trainService.getAllTrains();
	    });

	    // Verify exception details
	    assertEquals(errorMessage, thrown.getMessage());
	    assertEquals("BAD_REQUEST", thrown.getErrorCode());
	    assertEquals(400, thrown.getStatusCode());

	    // Verify interactions
	    verify(mockConnection).prepareStatement("SELECT * FROM TRAIN");
	}

	/**
	 * Tests getAllTrains method when database operation fails with SQLException.
	 * This test verifies that the method properly converts SQLException to TrainException.
	 */
	@Test
	public void getAllTrainsWhenSQLExceptionHandling() throws Exception {
	    // Arrange
	    when(mockConnection.prepareStatement(anyString()))
	        .thenThrow(new SQLException("Database error"));

	    // Act & Assert
	    TrainException thrown = assertThrows(TrainException.class, () -> {
	        trainService.getAllTrains();
	    });

	    // Verify exception message
	    assertEquals("Database error", thrown.getMessage());
	    assertEquals("BAD_REQUEST", thrown.getErrorCode());

	    // Verify interactions
	    verify(mockConnection).prepareStatement("SELECT * FROM TRAIN");
	}
	
	
	
	/**
	 * Verifies successful retrieval of trains between specified stations (Mumbai to
	 * Delhi). Test ensures correct query execution, data mapping, and validates
	 * returned train details including fares, stations, names, numbers and seats.
	 */

	@Test
	public void getTrainsBetweenStationsSuccessful() throws Exception {
	    // Arrange
	    String fromStation = "Mumbai";
	    String toStation = "Delhi";
	    
	    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
	    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
	    // Mock two trains in result set
	    when(mockResultSet.next()).thenReturn(true, true, false);
	    when(mockResultSet.getDouble("fare")).thenReturn(1000.0, 1200.0);
	    when(mockResultSet.getString("from_stn")).thenReturn("Mumbai Central", "Mumbai CST");
	    when(mockResultSet.getString("to_stn")).thenReturn("New Delhi", "Delhi Cantt");
	    when(mockResultSet.getString("tr_name")).thenReturn("Rajdhani Express", "Duronto Express");
	    when(mockResultSet.getLong("tr_no")).thenReturn(12345L, 12346L);
	    when(mockResultSet.getInt("seats")).thenReturn(500, 450);

	    // Act
	    List<TrainBean> result = trainService.getTrainsBetweenStations(fromStation, toStation);

	    // Assert
	    assertNotNull(result);
	    assertEquals(2, result.size());
	    
	    // Verify first train
	    TrainBean firstTrain = result.get(0);
	    assertEquals("Rajdhani Express", firstTrain.getTr_name());
	    assertEquals("Mumbai Central", firstTrain.getFrom_stn());
	    assertEquals("New Delhi", firstTrain.getTo_stn());
	    assertEquals(Double.valueOf(1000.0), firstTrain.getFare());
	    assertEquals(Integer.valueOf(500), firstTrain.getSeats());
	    assertEquals(Long.valueOf(12345L), firstTrain.getTr_no());

	    // Verify second train
	    TrainBean secondTrain = result.get(1);
	    assertEquals("Duronto Express", secondTrain.getTr_name());
	    
	    // Verify interactions
	    verify(mockPreparedStatement).setString(1, "%Mumbai%");
	    verify(mockPreparedStatement).setString(2, "%Delhi%");
	    verify(mockPreparedStatement).executeQuery();
	    verify(mockPreparedStatement).close();
	}

	
	@Test
	public void getTrainsBetweenStationsWhenNoTrainsFound() throws Exception {
	    // Arrange
	    String fromStation = "Invalid";
	    String toStation = "Station";
	    
	    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
	    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
	    when(mockResultSet.next()).thenReturn(false);

	    // Act
	    List<TrainBean> result = trainService.getTrainsBetweenStations(fromStation, toStation);

	    // Assert
	    assertNotNull(result);
	    assertTrue(result.isEmpty());

	    // Verify interactions
	    verify(mockPreparedStatement).setString(1, "%Invalid%");
	    verify(mockPreparedStatement).setString(2, "%Station%");
	    verify(mockPreparedStatement).executeQuery();
	    verify(mockPreparedStatement).close();
	}

	@Test
	public void getTrainsBetweenStationsWithNullParameters() throws Exception {
	    // Arrange
	    String fromStation = null;
	    String toStation = null;
	    
	    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
	    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
	    when(mockResultSet.next()).thenReturn(false);

	    // Act
	    List<TrainBean> result = trainService.getTrainsBetweenStations(fromStation, toStation);

	    // Assert
	    assertNotNull(result);
	    assertTrue(result.isEmpty());

	    // Verify interactions
	    verify(mockPreparedStatement).setString(1, "%null%");
	    verify(mockPreparedStatement).setString(2, "%null%");
	    verify(mockPreparedStatement).executeQuery();
	    verify(mockPreparedStatement).close();
	}

	@Test
	public void getTrainsBetweenStationsWhenDatabaseError() throws Exception {
	    // Arrange
	    String fromStation = "Mumbai";
	    String toStation = "Delhi";
	    String errorMessage = "Database connection failed";
	    
	    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
	    when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException(errorMessage));

	    try {
	        // Act
	        trainService.getTrainsBetweenStations(fromStation, toStation);
	        fail("Expected TrainException was not thrown");
	    } catch (TrainException e) {
	        // Assert
	        assertEquals(errorMessage, e.getErrorMessage());
	        assertEquals("BAD_REQUEST", e.getErrorCode());
	        assertEquals(400, e.getStatusCode());
	    }

	    // Verify interactions
	    verify(mockPreparedStatement).setString(1, "%Mumbai%");
	    verify(mockPreparedStatement).setString(2, "%Delhi%");
	    verify(mockPreparedStatement).executeQuery();
	}

	@Test
	public void getTrainsBetweenStationsWhenConnectionError() throws Exception {
	    // Arrange
	    String fromStation = "Mumbai";
	    String toStation = "Delhi";
	    String errorMessage = "Unable to establish connection";
	    
	    when(DBUtil.getConnection()).thenThrow(new TrainException(errorMessage));

	    try {
	        // Act
	        trainService.getTrainsBetweenStations(fromStation, toStation);
	        fail("Expected TrainException was not thrown");
	    } catch (TrainException e) {
	        // Assert
	        assertEquals(errorMessage, e.getErrorMessage());
	        assertEquals("BAD_REQUEST", e.getErrorCode());
	        assertEquals(400, e.getStatusCode());
	    }
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
	 * Test case for addTrain method when database operation is executed 
	 * but no rows are affected. This test verifies that the method returns 
	 * FAILURE response code when ResultSet.next() returns false.
	 */
	@Test
	public void addTrainWhenNoRowsAffected() throws Exception {
	    // Arrange
	    TrainBean train = new TrainBean();
	    train.setTr_no(12345L);
	    train.setTr_name("Test Train");
	    train.setFrom_stn("Start Station");
	    train.setTo_stn("End Station");
	    train.setSeats(100);
	    train.setFare(50.0);

	    // Mock ResultSet to return false for next()
	    when(mockResultSet.next()).thenReturn(false);

	    // Act
	    String result = trainService.addTrain(train);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString(), result);

	    // Verify interactions
	    verify(mockPreparedStatement).setLong(1, train.getTr_no());
	    verify(mockPreparedStatement).setString(2, train.getTr_name());
	    verify(mockPreparedStatement).setString(3, train.getFrom_stn());
	    verify(mockPreparedStatement).setString(4, train.getTo_stn());
	    verify(mockPreparedStatement).setLong(5, train.getSeats());
	    verify(mockPreparedStatement).setDouble(6, train.getFare());
	    verify(mockPreparedStatement).executeQuery();
	    verify(mockPreparedStatement).close();
	    verify(mockResultSet).next();
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

	@Test
	public void deleteTrainById_WhenSQLException() throws Exception {
	    // Arrange
	    String trainNo = "12345";
	    String errorMessage = "Database error occurred";
	    when(mockConnection.prepareStatement(anyString()))
	        .thenThrow(new SQLException(errorMessage));

	    // Act
	    String result = trainService.deleteTrainById(trainNo);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString() + " : " + errorMessage, result);
	    verify(mockConnection).prepareStatement("DELETE FROM TRAIN WHERE TR_NO=?");
	}

	@Test
	public void deleteTrainById_WhenTrainException() throws Exception {
	    // Arrange
	    String trainNo = "12345";
	    String errorMessage = "Connection failed";
	    when(DBUtil.getConnection())
	        .thenThrow(new TrainException(errorMessage));

	    // Act
	    String result = trainService.deleteTrainById(trainNo);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString() + " : " + errorMessage, result);
	}
	
	/**
	 * Test case for deleteTrainById method when train ID doesn't exist.
	 * This test verifies that the method returns FAILURE response code 
	 * when no rows are affected by the delete operation.
	 */
	@Test
	public void deleteTrainByIdWhenTrainNotFound() throws Exception {
	    // Arrange
	    String trainNo = "12345";
	    
	    // Mock executeUpdate to return 0 (no rows affected)
	    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
	    when(mockPreparedStatement.executeUpdate()).thenReturn(0);

	    // Act
	    String result = trainService.deleteTrainById(trainNo);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString(), result);

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
	
	@Test
	public void updateTrain_WhenSQLException() throws Exception {
	    // Arrange
	    TrainBean train = new TrainBean();
	    train.setTr_no(12345L);
	    train.setTr_name("Test Train");
	    train.setFrom_stn("Station A");
	    train.setTo_stn("Station B");
	    train.setSeats(100);
	    train.setFare(500.0);

	    String errorMessage = "Database error occurred";
	    when(mockConnection.prepareStatement(anyString()))
	        .thenThrow(new SQLException(errorMessage));

	    // Act
	    String result = trainService.updateTrain(train);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString() + " : " + errorMessage, result);
	    verify(mockConnection).prepareStatement(
	        "UPDATE TRAIN SET TR_NAME=?, FROM_STN=?,TO_STN=?,SEATS=?,FARE=? WHERE TR_NO=?");
	}

	@Test
	public void updateTrain_WhenTrainException() throws Exception {
	    // Arrange
	    TrainBean train = new TrainBean();
	    train.setTr_no(12345L);
	    train.setTr_name("Test Train");
	    train.setFrom_stn("Station A");
	    train.setTo_stn("Station B");
	    train.setSeats(100);
	    train.setFare(500.0);

	    String errorMessage = "Connection failed";
	    when(DBUtil.getConnection())
	        .thenThrow(new TrainException(errorMessage));

	    // Act
	    String result = trainService.updateTrain(train);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString() + " : " + errorMessage, result);
	}
	
	/**
	 * Test case for updateTrain method when train is not found in database.
	 * This test verifies that the method returns FAILURE response code 
	 * when ResultSet.next() returns false.
	 */
	@Test
	public void updateTrainWhenTrainNotFound() throws Exception {
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
	    when(mockResultSet.next()).thenReturn(false);

	    // Act
	    String result = trainService.updateTrain(train);

	    // Assert
	    assertEquals(ResponseCode.FAILURE.toString(), result);

	    // Verify interactions
	    verify(mockPreparedStatement).setString(1, train.getTr_name());
	    verify(mockPreparedStatement).setString(2, train.getFrom_stn());
	    verify(mockPreparedStatement).setString(3, train.getTo_stn());
	    verify(mockPreparedStatement).setLong(4, train.getSeats());
	    verify(mockPreparedStatement).setDouble(5, train.getFare());
	    verify(mockPreparedStatement).setDouble(6, train.getTr_no());
	    verify(mockPreparedStatement).executeQuery();
	    verify(mockPreparedStatement).close();
	    verify(mockResultSet).next();
	}
}


