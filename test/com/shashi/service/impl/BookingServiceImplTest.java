package com.shashi.service.impl;

import com.shashi.beans.HistoryBean;
import com.shashi.beans.TrainException;
import com.shashi.utility.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;

public class BookingServiceImplTest {

	/**
	 * Test case for getAllBookingsByCustomerId method when no bookings are found.
	 * This test verifies that the method returns an empty list when there are no
	 * bookings for the given customer email ID.
	 */
	@Test
	public void getAllBookingsForCustomerwithNoBookings() throws Exception {
		// Arrange
		BookingServiceImpl bookingService = new BookingServiceImpl();
		String customerEmailId = "nonexistent@example.com";

		// Mock database connection and prepared statement
		Connection mockConnection = mock(Connection.class);
		PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
		ResultSet mockResultSet = mock(ResultSet.class);

		// Setup mock behavior
		try (MockedStatic<DBUtil> mockedDBUtil = Mockito.mockStatic(DBUtil.class)) {
			mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);
			when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
			when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

			// Mock ResultSet to return no rows
			when(mockResultSet.next()).thenReturn(false);

			// Act
			List<HistoryBean> result = bookingService.getAllBookingsByCustomerId(customerEmailId);

			// Assert
			assertNotNull(result);
			assertTrue(result.isEmpty());

			// Verify that the database interactions occurred
			verify(mockPreparedStatement).setString(1, customerEmailId);
			verify(mockPreparedStatement).executeQuery();
			verify(mockResultSet).next();
		}
	}

	/**
	 * Test case for getAllBookingsByCustomerId method when a valid customer email
	 * is provided. This test verifies that the method returns a non-empty list of
	 * HistoryBean objects for a customer with existing bookings.
	 */
	@Test
	public void getAllBookingsForCustomer() throws Exception {
		// Arrange
		BookingServiceImpl bookingService = new BookingServiceImpl();
		String validCustomerEmail = "test@example.com";

		// Mock database connection and prepared statement
		Connection mockConnection = mock(Connection.class);
		PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
		ResultSet mockResultSet = mock(ResultSet.class);

		// Setup mock behavior
		try (MockedStatic<DBUtil> mockedDBUtil = Mockito.mockStatic(DBUtil.class)) {
			mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);
			when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
			when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

			// Mock ResultSet to return two rows
			when(mockResultSet.next()).thenReturn(true, true, false); // Return true twice for two records, then false

			// Mock the ResultSet data for each column
			when(mockResultSet.getString("transid")).thenReturn("TRANS123", "TRANS124");
			when(mockResultSet.getString("mailid")).thenReturn(validCustomerEmail, validCustomerEmail);
			when(mockResultSet.getString("trainno")).thenReturn("TR123", "TR124");
			when(mockResultSet.getString("journey_date")).thenReturn("2023-07-15", "2023-07-16");
			when(mockResultSet.getString("from_stn")).thenReturn("Station A", "Station C");
			when(mockResultSet.getString("to_stn")).thenReturn("Station B", "Station D");
			when(mockResultSet.getLong("seats")).thenReturn(2L, 1L);
			when(mockResultSet.getDouble("amount")).thenReturn(100.0, 50.0);

			// Act
			List<HistoryBean> result = bookingService.getAllBookingsByCustomerId(validCustomerEmail);

			// Assert
			assertNotNull("The returned list should not be null", result);
			assertFalse("The returned list should not be empty", result.isEmpty());
			assertEquals("Should return 2 bookings", 2, result.size());

			// Verify each booking has the correct email
			for (HistoryBean booking : result) {
				assertEquals("Each booking should have the correct customer email", validCustomerEmail,
						booking.getMailId());
			}

			// Verify database interactions
			verify(mockPreparedStatement).setString(1, validCustomerEmail);
			verify(mockPreparedStatement).executeQuery();
			verify(mockResultSet, times(3)).next(); // Called 3 times (twice true, once false)
		}
	}

	@Test(expected = TrainException.class)
	public void GetAllBookingsByCustomerIdSQLError() throws Exception {
		// Arrange
		BookingServiceImpl bookingService = new BookingServiceImpl();
		String testEmail = "test@example.com";

		// Mock database connection and prepared statement
		Connection mockConnection = mock(Connection.class);
		PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
		ResultSet mockResultSet = mock(ResultSet.class);

		// Setup mock behavior
		try (MockedStatic<DBUtil> mockedDBUtil = Mockito.mockStatic(DBUtil.class)) {
			// Mock the database connection
			mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

			// Mock the PreparedStatement creation
			when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

			// Mock the executeQuery to throw SQLException
			when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

			// Act - this should throw TrainException
			bookingService.getAllBookingsByCustomerId(testEmail);

			// Verify database interactions
			verify(mockPreparedStatement).setString(1, testEmail);
			verify(mockPreparedStatement).executeQuery();
			verify(mockPreparedStatement).close();
		}
	}

	/**
	 * Tests the createHistory method when the database operation fails to insert
	 * the record. This test verifies that a TrainException with the
	 * INTERNAL_SERVER_ERROR response code is thrown when the insert operation
	 * returns 0 rows affected.
	 */
	@Test(expected = TrainException.class)
	public void testCreateHistoryFailure() throws TrainException {
		BookingServiceImpl bookingService = new BookingServiceImpl();
		HistoryBean details = new HistoryBean();
		// Set up details with minimal required information
		details.setMailId("test@example.com");
		details.setTr_no("TR123");
		details.setDate("2023-07-15");
		details.setFrom_stn("StationA");
		details.setTo_stn("StationB");
		details.setSeats(1);
		details.setAmount(100.0);

		// This should throw a TrainException with INTERNAL_SERVER_ERROR
		bookingService.createHistory(details);
	}

	/**
	 * Test case for createHistory method when the database insert is successful.
	 * This test verifies that: 1. The method returns a HistoryBean object with the
	 * correct transaction ID. 2. The returned HistoryBean matches the input
	 * details. 3. No TrainException is thrown when the operation is successful.
	 */
	@Test
	public void createHistorySuccess() throws SQLException, TrainException {
		// Arrange
		BookingServiceImpl bookingService = new BookingServiceImpl();
		HistoryBean inputDetails = new HistoryBean();
		inputDetails.setMailId("test@example.com");
		inputDetails.setTr_no("TR123");
		inputDetails.setDate("2023-07-15");
		inputDetails.setFrom_stn("Station A");
		inputDetails.setTo_stn("Station B");
		inputDetails.setSeats(2);
		inputDetails.setAmount(100.0);

		// Mock database connection and prepared statement
		Connection mockConnection = Mockito.mock(Connection.class);
		PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);

		// Mock DBUtil to return our mock connection
		try (MockedStatic<DBUtil> mockedDBUtil = Mockito.mockStatic(DBUtil.class)) {
			mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);
			when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
			when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Simulate successful insert

			// Act
			HistoryBean result = bookingService.createHistory(inputDetails);

			// Assert
			assertNotNull(result);
			assertEquals(inputDetails.getMailId(), result.getMailId());
			assertEquals(inputDetails.getTr_no(), result.getTr_no());
			assertEquals(inputDetails.getDate(), result.getDate());
			assertEquals(inputDetails.getFrom_stn(), result.getFrom_stn());
			assertEquals(inputDetails.getTo_stn(), result.getTo_stn());
			assertEquals(inputDetails.getSeats(), result.getSeats());
			assertEquals(inputDetails.getAmount(), result.getAmount(), 0.001);
			assertNotNull(result.getTransId());
			assertTrue(UUID.fromString(result.getTransId()) instanceof UUID);

			// Verify that the prepared statement was called with the correct parameters
			verify(mockPreparedStatement).setString(1, result.getTransId());
			verify(mockPreparedStatement).setString(2, inputDetails.getMailId());
			verify(mockPreparedStatement).setString(3, inputDetails.getTr_no());
			verify(mockPreparedStatement).setString(4, inputDetails.getDate());
			verify(mockPreparedStatement).setString(5, inputDetails.getFrom_stn());
			verify(mockPreparedStatement).setString(6, inputDetails.getTo_stn());
			verify(mockPreparedStatement).setLong(7, inputDetails.getSeats());
			verify(mockPreparedStatement).setDouble(8, inputDetails.getAmount());
			verify(mockPreparedStatement).executeUpdate();
			verify(mockPreparedStatement).close();
		}
	}

}
