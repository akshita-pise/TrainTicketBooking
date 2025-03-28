package com.shashi.service.impl;

import com.shashi.beans.HistoryBean;
import com.shashi.beans.TrainException;
import com.shashi.utility.DBUtil;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BookingServiceImplTest {

	@Mock
	private Connection mockConnection;

	@Mock
	private PreparedStatement mockPreparedStatement;

	@Mock
	private ResultSet mockResultSet;

	@InjectMocks
	private BookingServiceImpl bookingService;

	private static MockedStatic<DBUtil> mockedDBUtil;

	@BeforeClass
	public static void setUpClass() {
		mockedDBUtil = Mockito.mockStatic(DBUtil.class);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
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
	 * Test case for getAllBookingsByCustomerId method when no bookings are found.
	 * This test verifies that the method returns an empty list when there are no
	 * bookings for the given customer email ID.
	 */
	@Test
	public void getAllBookingsForCustomerwithNoBookings() throws Exception {
		// Arrange

		String customerEmailId = "nonexistent@example.com";
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

	/**
	 * Test case for getAllBookingsByCustomerId method when a valid customer email
	 * is provided. This test verifies that the method returns a non-empty list of
	 * HistoryBean objects for a customer with existing bookings.
	 */
	@Test
	public void getAllBookingsForCustomer() throws Exception {
		// Arrange

		String validCustomerEmail = "test@example.com";
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

		// Verify database interactions
		verify(mockPreparedStatement).setString(1, validCustomerEmail);
		verify(mockPreparedStatement).executeQuery();
		verify(mockResultSet, times(3)).next(); // Called 3 times (twice true, once false)

	}
	
	/**
	 *  Test ensures that the getAllBookingsByCustomerId method throws a TrainException 
	 *  when a database error occurs. It mocks an SQLException during executeQuery()
	 * @throws Exception
	 */

	@Test(expected = TrainException.class)
	public void GetAllBookingsByCustomerIdSQLError() throws Exception {
		// Arrange
		String testEmail = "test@example.com";

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

	/**
	 * Test verifies that the createHistory method throws a TrainException 
	 * when a database error occurs. 
	 * It simulates an SQL failure by making prepareStatement throw an SQLException
	 */
	
	@Test(expected = TrainException.class)
	public void testCreateHistoryFailure() throws TrainException, SQLException {
		HistoryBean details = new HistoryBean();
		// Set up details with minimal required information
		details.setMailId("test@example.com");
		details.setTr_no("TR123");
		details.setDate("2023-07-15");
		details.setFrom_stn("StationA");
		details.setTo_stn("StationB");
		details.setSeats(1);
		details.setAmount(100.0);

		when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

		// Act & Assert
		HistoryBean result = bookingService.createHistory(details);// Should throw TrainException
    }
	
	/**
	 * Test verifies TrainException thrown when database insert operation fails
	 * @throws SQLException
	 * @throws TrainException
	 */
	
	@Test(expected = TrainException.class)
    public void createHistoryInsertFailed() throws SQLException, TrainException {
        // Arrange
        HistoryBean details = new HistoryBean();
        details.setMailId("test@example.com");
        details.setTr_no("TR123");
        details.setDate("2023-07-15");
        details.setFrom_stn("StationA");
        details.setTo_stn("StationB");
        details.setSeats(1);
        details.setAmount(100.0);

        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // Act
        bookingService.createHistory(details); // Should throw TrainException
    }
		
	
	/**
	 * Test verifies that the createHistory method successfully inserts a booking record 
	 * into the database
	 */
	@Test
	public void createHistorySuccess() throws SQLException, TrainException {
		HistoryBean details = new HistoryBean();
		// Set up details with minimal required information
		details.setMailId("test@example.com");
		details.setTr_no("TR123");
		details.setDate("2023-07-15");
		details.setFrom_stn("StationA");
		details.setTo_stn("StationB");
		details.setSeats(1);
		details.setAmount(100.0);

		when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        //Act
		HistoryBean result = bookingService.createHistory(details);
		
        //Assert
		assertNotNull(result);
		assertEquals(details.getMailId(), result.getMailId());
		assertEquals(details.getTr_no(), result.getTr_no());
		assertEquals(details.getDate(), result.getDate());
		assertEquals(details.getFrom_stn(), result.getFrom_stn());
		assertEquals(details.getTo_stn(), result.getTo_stn());
		assertEquals(details.getSeats(), result.getSeats());
		assertEquals(details.getAmount(), result.getAmount(), 0.001);
		assertNotNull(result.getTransId());
		assertTrue(UUID.fromString(result.getTransId()) instanceof UUID);

		// Verify that the prepared statement was called with the correct parameters

		verify(mockPreparedStatement).setString(1, result.getTransId());
		verify(mockPreparedStatement).setString(2, details.getMailId());
		verify(mockPreparedStatement).setString(3, details.getTr_no());
		verify(mockPreparedStatement).setString(4, details.getDate());
		verify(mockPreparedStatement).setString(5, details.getFrom_stn());
		verify(mockPreparedStatement).setString(6, details.getTo_stn());
		verify(mockPreparedStatement).setLong(7, details.getSeats());
		verify(mockPreparedStatement).setDouble(8, details.getAmount());
		verify(mockPreparedStatement).executeUpdate();
		verify(mockPreparedStatement).close();
	}
}
