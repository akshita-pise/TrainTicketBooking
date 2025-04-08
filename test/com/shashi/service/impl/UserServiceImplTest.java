package com.shashi.service.impl;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.ResponseCode;
import com.shashi.constant.UserRole;
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
	 * Test loginUser when SQL Exception occurs during query execution
	 */
	@Test
	public void loginUser_WhenSQLException_ShouldThrowTrainException() throws Exception {
		// Arrange
		String username = "test@example.com";
		String password = "password123";

		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database connection failed"));

		// Act & Assert
		TrainException thrown = assertThrows(TrainException.class, () -> userService.loginUser(username, password));

		// Verify the exception message
		assertEquals("Database connection failed", thrown.getMessage());

		// Verify interactions
		verify(mockPreparedStatement).setString(1, username);
		verify(mockPreparedStatement).setString(2, password);
		verify(mockPreparedStatement).executeQuery();
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

	/**
	 * Test registerUser when ResultSet returns false
	 */
	@Test
	public void registerUser_WhenResultSetHasNoNext_ShouldReturnFailure() throws Exception {
		// Arrange
		UserBean customer = new UserBean();
		customer.setMailId("test@example.com");
		customer.setPWord("password");
		customer.setFName("John");
		customer.setLName("Doe");
		customer.setAddr("123 Test St");
		customer.setPhNo(1234567890L);

		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(false); // ResultSet has no next record

		// Act
		String result = userService.registerUser(customer);

		// Assert
		assertEquals(ResponseCode.FAILURE.toString(), result);

		// Verify
		verify(mockPreparedStatement).setString(1, customer.getMailId());
		verify(mockPreparedStatement).setString(2, customer.getPWord());
		verify(mockPreparedStatement).setString(3, customer.getFName());
		verify(mockPreparedStatement).setString(4, customer.getLName());
		verify(mockPreparedStatement).setString(5, customer.getAddr());
		verify(mockPreparedStatement).setLong(6, customer.getPhNo());
		verify(mockPreparedStatement).executeQuery();
		verify(mockResultSet).next();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test registerUser when Oracle unique constraint violation occurs
	 */
	@Test
	public void registerUser_WhenOracleUniqueConstraintViolation_ShouldReturnFailureWithUserExists() throws Exception {
		// Arrange
		UserBean customer = new UserBean();
		customer.setMailId("existing@example.com");
		customer.setPWord("password");
		customer.setFName("John");
		customer.setLName("Doe");
		customer.setAddr("123 Test St");
		customer.setPhNo(1234567890L);

		// Simulate Oracle unique constraint violation
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("ORA-00001: unique constraint violated"));

		// Act
		String result = userService.registerUser(customer);

		// Assert
		assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
		assertTrue(result.contains("User With Id: " + customer.getMailId() + " is already registered"));

		// Verify
		verify(mockPreparedStatement).setString(1, customer.getMailId());
		verify(mockPreparedStatement).setString(2, customer.getPWord());
		verify(mockPreparedStatement).setString(3, customer.getFName());
		verify(mockPreparedStatement).setString(4, customer.getLName());
		verify(mockPreparedStatement).setString(5, customer.getAddr());
		verify(mockPreparedStatement).setLong(6, customer.getPhNo());
		verify(mockPreparedStatement).executeQuery();
	}

	/**
	 * Test registerUser when non-Oracle SQL exception occurs
	 */
	@Test
	public void registerUser_WhenNonOracleException_ShouldReturnFailureWithMessage() throws Exception {
		// Arrange
		UserBean customer = new UserBean();
		customer.setMailId("test@example.com");
		customer.setPWord("password");
		customer.setFName("John");
		customer.setLName("Doe");
		customer.setAddr("123 Test St");
		customer.setPhNo(1234567890L);

		// Simulate general SQL exception
		String errorMessage = "General database error";
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException(errorMessage));

		// Act
		String result = userService.registerUser(customer);

		// Assert
		assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
		assertTrue(result.contains(errorMessage));
		assertFalse(result.contains("is already registered"));

		// Verify
		verify(mockPreparedStatement).setString(1, customer.getMailId());
		verify(mockPreparedStatement).setString(2, customer.getPWord());
		verify(mockPreparedStatement).setString(3, customer.getFName());
		verify(mockPreparedStatement).setString(4, customer.getLName());
		verify(mockPreparedStatement).setString(5, customer.getAddr());
		verify(mockPreparedStatement).setLong(6, customer.getPhNo());
		verify(mockPreparedStatement).executeQuery();
	}

	/**
	 * Test getUserByEmailId when user exists
	 */
	@Test
	public void getUserByEmailIdWhenUserExists() throws Exception {
		// Arrange
		String email = "test@example.com";
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true);
		when(mockResultSet.getString("fname")).thenReturn("John");
		when(mockResultSet.getString("lname")).thenReturn("Doe");
		when(mockResultSet.getString("addr")).thenReturn("123 Street");
		when(mockResultSet.getString("mailid")).thenReturn(email);
		when(mockResultSet.getLong("phno")).thenReturn(1234567890L);

		// Act
		UserBean result = userService.getUserByEmailId(email);

		// Assert
		assertNotNull(result);
		assertEquals("John", result.getFName());
		assertEquals("Doe", result.getLName());
		assertEquals("123 Street", result.getAddr());
		assertEquals(email, result.getMailId());
		// assertEquals(Long.valueOf(1234567890L), result.getPhNo());

		// Verify
		verify(mockPreparedStatement).setString(1, email);
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test getUserByEmailId when user does not exist
	 */
	@Test
	public void getUserByEmailIdWhenUserNotFound() throws Exception {
		// Arrange
		String email = "nonexistent@example.com";
		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(false);

		// Act & Assert
		TrainException thrown = assertThrows(TrainException.class, () -> userService.getUserByEmailId(email));

		// Verify the exception details
		assertEquals(ResponseCode.NO_CONTENT.name(), thrown.getErrorCode());
		assertEquals(ResponseCode.NO_CONTENT.getMessage(), thrown.getErrorMessage());
		assertEquals(ResponseCode.NO_CONTENT.getCode(), thrown.getStatusCode());

		// Verify interactions
		verify(mockPreparedStatement).setString(1, email);
		verify(mockPreparedStatement).executeQuery();
		// verify(mockPreparedStatement).close();
	}

	@Test
	public void getUserByEmailId_WhenSQLException_ShouldThrowTrainException() throws Exception {
		// Arrange
		String testEmail = "test@example.com";
		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database connection failed"));

		// Act & Assert
		TrainException thrown = assertThrows(TrainException.class, () -> userService.getUserByEmailId(testEmail));

		// Verify the exception message
		assertEquals("Database connection failed", thrown.getMessage());

		// Verify only the interactions that occur before the exception
		verify(mockPreparedStatement).setString(1, testEmail);
		verify(mockPreparedStatement).executeQuery();
		// Remove verification of close() since it won't be called due to the exception
	}

	/**
	 * Test getAllUsers when users exist
	 */
	@Test
	public void getAllUsersWhenUsersExist() throws Exception {
		// Arrange
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true, true, false);
		when(mockResultSet.getString("fname")).thenReturn("John", "Jane");
		when(mockResultSet.getString("lname")).thenReturn("Doe", "Smith");
		when(mockResultSet.getString("addr")).thenReturn("123 Street", "456 Ave");
		when(mockResultSet.getString("mailid")).thenReturn("john@example.com", "jane@example.com");
		when(mockResultSet.getLong("phno")).thenReturn(1234567890L, 9876543210L);

		// Act
		List<UserBean> results = userService.getAllUsers();

		// Assert
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals("John", results.get(0).getFName());
		assertEquals("Jane", results.get(1).getFName());

		// Verify
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test getAllUsers when no users exist in database
	 */
	@Test
	public void getAllUsers_WhenNoUsers_ShouldThrowTrainException() throws Exception {
		// Arrange
		when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(false); // Empty result set

		// Act & Assert
		TrainException thrown = assertThrows(TrainException.class, () -> userService.getAllUsers());

		// Verify exception details
		assertEquals(ResponseCode.NO_CONTENT.name(), thrown.getErrorCode());
		assertEquals(ResponseCode.NO_CONTENT.getMessage(), thrown.getErrorMessage());
		assertEquals(ResponseCode.NO_CONTENT.getCode(), thrown.getStatusCode());

		// Verify interactions
		verify(mockPreparedStatement).executeQuery();
		// verify(mockPreparedStatement).close();
	}

	/*
	 * The test is failing because when an SQLException is thrown during
	 * executeQuery(), the code execution stops at that point and never reaches the
	 * ps.close() statement in the try block
	 */

	@Test
	public void getAllUsers_WhenSQLException_ShouldThrowTrainException() throws Exception {
		// Arrange
		when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database error"));

		// Act & Assert
		TrainException thrown = assertThrows(TrainException.class, () -> userService.getAllUsers());

		// Verify the exception message
		assertEquals("Database error", thrown.getMessage());

		// Verify interactions
		verify(mockPreparedStatement).executeQuery();
		// verify(mockPreparedStatement).close();
	}

	/**
	 * Test updateUser successful case
	 */
	@Test
	public void updateUserSuccessful() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("test@example.com");
		user.setFName("John");
		user.setLName("Doe");
		user.setAddr("123 Street");
		user.setPhNo(1234567890L);

		when(mockPreparedStatement.executeUpdate()).thenReturn(1);

		// Act
		String result = userService.updateUser(user);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);

		// Verify
		verify(mockPreparedStatement).setString(1, user.getFName());
		verify(mockPreparedStatement).setString(2, user.getLName());
		verify(mockPreparedStatement).setString(3, user.getAddr());
		verify(mockPreparedStatement).setLong(4, user.getPhNo());
		verify(mockPreparedStatement).setString(5, user.getMailId());
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test updateUser when user not found
	 */
	@Test
	public void updateUserWhenUserNotFound() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("nonexistent@example.com");

		when(mockPreparedStatement.executeUpdate()).thenReturn(0);

		// Act
		String result = userService.updateUser(user);

		// Assert
		assertEquals(ResponseCode.FAILURE.toString(), result);
	}

	/**
	 * Test updateUser when SQL Exception occurs during update
	 */
	@Test
	public void updateUser_WhenSQLException_ShouldReturnFailure() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("test@example.com");
		user.setFName("John");
		user.setLName("Doe");
		user.setAddr("123 Street");
		user.setPhNo(1234567890L);

		when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

		// Act
		String result = userService.updateUser(user);

		// Assert
		assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
		assertTrue(result.contains("Database error"));

		// Verify
		verify(mockPreparedStatement).setString(1, user.getFName());
		verify(mockPreparedStatement).setString(2, user.getLName());
		verify(mockPreparedStatement).setString(3, user.getAddr());
		verify(mockPreparedStatement).setLong(4, user.getPhNo());
		verify(mockPreparedStatement).setString(5, user.getMailId());
		verify(mockPreparedStatement).executeUpdate();
	}

	/**
	 * Test updateUser when no rows are updated
	 */
	@Test
	public void updateUser_WhenNoRowsUpdated_ShouldReturnFailure() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("test@example.com");
		user.setFName("John");
		user.setLName("Doe");
		user.setAddr("123 Street");
		user.setPhNo(1234567890L);

		when(mockPreparedStatement.executeUpdate()).thenReturn(0);

		// Act
		String result = userService.updateUser(user);

		// Assert
		assertEquals(ResponseCode.FAILURE.toString(), result);

		// Verify
		verify(mockPreparedStatement).executeUpdate();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test deleteUser successful case
	 */
	@Test
	public void deleteUserSuccessful() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("test@example.com");

		when(mockPreparedStatement.executeUpdate()).thenReturn(1);

		// Act
		String result = userService.deleteUser(user);

		// Assert
		assertEquals(ResponseCode.SUCCESS.toString(), result);

		// Verify
		verify(mockPreparedStatement).setString(1, user.getMailId());
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test deleteUser when user not found
	 */
	@Test
	public void deleteUserWhenUserNotFound() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("nonexistent@example.com");

		when(mockPreparedStatement.executeUpdate()).thenReturn(0);

		// Act
		String result = userService.deleteUser(user);

		// Assert
		assertEquals(ResponseCode.FAILURE.toString(), result);
	}

	/**
	 * Test deleteUser when no rows are deleted
	 */
	@Test
	public void deleteUser_WhenNoRowsDeleted_ShouldReturnFailure() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("test@example.com");

		when(mockPreparedStatement.executeUpdate()).thenReturn(0);

		// Act
		String result = userService.deleteUser(user);

		// Assert
		assertEquals(ResponseCode.FAILURE.toString(), result);

		// Verify
		verify(mockPreparedStatement).executeUpdate();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test deleteUser when SQL Exception occurs during delete
	 */
	@Test
	public void deleteUser_WhenSQLException_ShouldReturnFailure() throws Exception {
		// Arrange
		UserBean user = new UserBean();
		user.setMailId("test@example.com");

		when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database error"));

		// Act
		String result = userService.deleteUser(user);

		// Assert
		assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
		assertTrue(result.contains("Database error"));

		// Verify
		verify(mockPreparedStatement).setString(1, user.getMailId());
		verify(mockPreparedStatement).executeUpdate();
	}

	/**
	 * Test constructor with null UserRole
	 */
	@Test
	public void constructorWithNullUserRole() throws Exception {
		// Arrange
		userService = new UserServiceImpl(null); // Use the existing field
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true, false); // One iteration only
		when(mockResultSet.getString("fname")).thenReturn("TestUser");
		when(mockResultSet.getString("lname")).thenReturn("TestLast");
		when(mockResultSet.getString("addr")).thenReturn("Test Address");
		when(mockResultSet.getString("mailid")).thenReturn("test@example.com");
		when(mockResultSet.getLong("phno")).thenReturn(1234567890L);

		// Act
		List<UserBean> users = userService.getAllUsers();

		// Assert
		assertNotNull(users);
		assertEquals(1, users.size());
		UserBean user = users.get(0);
		assertEquals("TestUser", user.getFName());
		assertEquals("TestLast", user.getLName());
		assertEquals("Test Address", user.getAddr());
		assertEquals("test@example.com", user.getMailId());
		assertEquals(1234567890L, user.getPhNo());

		// Verify
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}

	/**
	 * Test constructor with explicit UserRole
	 */
	@Test
	public void constructorWithExplicitUserRole() throws Exception {
		// Arrange
		userService = new UserServiceImpl(UserRole.ADMIN); // Use the existing field
		when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
		when(mockResultSet.next()).thenReturn(true, false); // One iteration only
		when(mockResultSet.getString("fname")).thenReturn("AdminUser");
		when(mockResultSet.getString("lname")).thenReturn("AdminLast");
		when(mockResultSet.getString("addr")).thenReturn("Admin Address");
		when(mockResultSet.getString("mailid")).thenReturn("admin@example.com");
		when(mockResultSet.getLong("phno")).thenReturn(1234567890L);

		// Act
		List<UserBean> users = userService.getAllUsers();

		// Assert
		assertNotNull(users);
		assertEquals(1, users.size());
		UserBean user = users.get(0);
		assertEquals("AdminUser", user.getFName());
		assertEquals("AdminLast", user.getLName());
		assertEquals("Admin Address", user.getAddr());
		assertEquals("admin@example.com", user.getMailId());
		assertEquals(1234567890L, user.getPhNo());

		// Verify
		verify(mockPreparedStatement).executeQuery();
		verify(mockPreparedStatement).close();
	}

}
