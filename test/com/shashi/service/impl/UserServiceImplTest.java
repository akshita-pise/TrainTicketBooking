package com.shashi.service.impl;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.ResponseCode;
import com.shashi.utility.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

	@Mock
	private Connection mockConnection;

	@Mock
	private PreparedStatement mockPreparedStatement;

	@Mock
	private ResultSet mockResultSet;

	@InjectMocks
	private UserServiceImpl userService;

	private static MockedStatic<DBUtil> mockedDBUtil; // Keep static reference

	@BeforeClass
	public static void setUpClass() {
		mockedDBUtil = Mockito.mockStatic(DBUtil.class);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		// Mock DBUtil.getConnection() to return the existing `mockConnection`
		mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
	}

	@AfterClass
	public static void tearDownClass() {
		mockedDBUtil.close(); // Ensure cleanup
	}

	@After
	public void tearDown() {
		reset(mockConnection, mockPreparedStatement, mockResultSet);
	}

	/**
	 * Test case for loginUser method when user credentials are incorrect. This test
	 * verifies that the method throws a TrainException with UNAUTHORIZED response
	 * code when provided with invalid username and password combination.
	 */
	@Test
	public void loginUserinvalidCredentials() throws SQLException {
		// Arrange
		String invalidUsername = "invalid@example.com";
		String invalidPassword = "wrongpassword";

		// Mock the database interaction
		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		// Simulate no user found by returning false for ResultSet.next()
		when(mockResultSet.next()).thenReturn(false);

		try {
			// Act
			userService.loginUser(invalidUsername, invalidPassword);
			fail("Expected TrainException to be thrown");
		} catch (TrainException e) {
			// Assert
			assertEquals("Invalid Credentials, Try Again", e.getMessage());

			// Verify database interactions
			verify(mockPreparedStatement).setString(1, invalidUsername);
			verify(mockPreparedStatement).setString(2, invalidPassword);
			verify(mockPreparedStatement).executeQuery();

			// Verify no more interactions with PreparedStatement
			verifyNoMoreInteractions(mockPreparedStatement);
		}
	}

	/**
	 * Test successful user login with valid credentials. This test verifies that
	 * the loginUser method returns a valid UserBean when provided with correct
	 * username and password.
	 */
	@Test
	public void loginUservalidCredentials() throws Exception {
		// Arrange
		String validUsername = "john@example.com";
		String validPassword = "password123";

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true);
		when(mockResultSet.getString("fname")).thenReturn("John");
		when(mockResultSet.getString("lname")).thenReturn("Doe");
		when(mockResultSet.getString("addr")).thenReturn("123 Main St");
		when(mockResultSet.getString("mailid")).thenReturn("john@example.com");
		when(mockResultSet.getLong("phno")).thenReturn(1234567890L);
		when(mockResultSet.getString("pword")).thenReturn("password123");

		// Act
		UserBean result = userService.loginUser(validUsername, validPassword);

		// Assert
		assertNotNull(result);
		assertEquals("John", result.getFName());
		assertEquals("Doe", result.getLName());
		assertEquals("123 Main St", result.getAddr());
		assertEquals("john@example.com", result.getMailId());
		assertEquals(1234567890L, result.getPhNo());
		assertEquals("password123", result.getPWord());

		// Verify database interactions
		verify(mockPreparedStatement).setString(1, validUsername);
		verify(mockPreparedStatement).setString(2, validPassword);
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test case for successful user registration when the user does not already
	 * exist. This test mocks the database connection and result set to simulate a
	 * successful registration.
	 */
	@Test
	public void registerUserSuccessfulRegistration() throws Exception {
		// Arrange
		UserBean customer = new UserBean();
		customer.setMailId("test@example.com");
		customer.setPWord("password");
		customer.setFName("John");
		customer.setLName("Doe");
		customer.setAddr("123 Test St");
		customer.setPhNo(1234567890L);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true);

		// Act
		String result = userService.registerUser(customer);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);

		// Verify database interactions
		verify(mockPreparedStatement).setString(1, customer.getMailId());
		verify(mockPreparedStatement).setString(2, customer.getPWord());
		verify(mockPreparedStatement).setString(3, customer.getFName());
		verify(mockPreparedStatement).setString(4, customer.getLName());
		verify(mockPreparedStatement).setString(5, customer.getAddr());
		verify(mockPreparedStatement).setLong(6, customer.getPhNo());
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Tests the registerUser method when attempting to register a user with an
	 * email that already exists in the database. This should result in a failure
	 * response with a specific error message indicating that the user is already
	 * registered.
	 */
	@Test
	public void registerUserexistingUser() throws SQLException {
		// Arrange
		UserBean existingUser = new UserBean();
		existingUser.setMailId("existing@example.com");
		existingUser.setPWord("password");
		existingUser.setFName("John");
		existingUser.setLName("Doe");
		existingUser.setAddr("123 Main St");
		existingUser.setPhNo(1234567890L);

		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

		// Simulate SQL constraint violation
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("ORA-00001: unique constraint violated"));

		// Act
		String result = userService.registerUser(existingUser);

		// Assert
		assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
		assertTrue(result.contains("User With Id: " + existingUser.getMailId() + " is already registered"));

		// Verify database interactions
		verify(mockPreparedStatement).setString(1, existingUser.getMailId());
		verify(mockPreparedStatement).setString(2, existingUser.getPWord());
		verify(mockPreparedStatement).setString(3, existingUser.getFName());
		verify(mockPreparedStatement).setString(4, existingUser.getLName());
		verify(mockPreparedStatement).setString(5, existingUser.getAddr());
		verify(mockPreparedStatement).setLong(6, existingUser.getPhNo());
		verify(mockPreparedStatement).executeQuery();
	}

}
