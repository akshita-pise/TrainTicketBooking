package com.shashi.servlets;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.shashi.beans.TrainBean;
import com.shashi.beans.HistoryBean;
import com.shashi.constant.UserRole;
import com.shashi.service.TrainService;
import com.shashi.service.BookingService;

public class BookTrainsTest {
	private BookTrains bookTrains;
	private StringWriter stringWriter;
	private PrintWriter writer;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private RequestDispatcher dispatcher;

	@Mock
	private ServletContext servletContext;

	@Mock
	private HttpSession session;

	@Mock
	private TrainService trainService;

	@Mock
	private BookingService bookingService;

	@Before
	public void setUp() throws Exception {
		// Initialize mocks
		MockitoAnnotations.openMocks(this);

		// Pass mocks to the BookTrains constructor
		bookTrains = new BookTrains(trainService, bookingService);

		// Setup writer
		stringWriter = new StringWriter();
		writer = new PrintWriter(stringWriter);
		when(response.getWriter()).thenReturn(writer);

		// Mock the authentication-related session attributes
		when(request.getSession()).thenReturn(session);
		when(session.getAttribute("usertype")).thenReturn(UserRole.CUSTOMER.toString());
		when(session.getAttribute("useremail")).thenReturn("test@example.com");
		when(session.getAttribute("isLoggedIn")).thenReturn(Boolean.TRUE);
		when(session.getAttribute("authToken")).thenReturn("mock-auth-token");

		// Mock cookies
		Cookie[] cookies = { new Cookie("sessionIdForCUSTOMER", "mock-session-id") };
		when(request.getCookies()).thenReturn(cookies);

		// Setup request
		when(request.getServletContext()).thenReturn(servletContext);
		when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
	}

	@After
	public void tearDown() {
		reset(session); // Reset session mock after each test to avoid authentication persistence issues
	}

	@Test
	public void testSuccessfulBooking() throws Exception {
		// Setup test data
		TrainBean mockTrain = new TrainBean();
		mockTrain.setSeats(10);
		mockTrain.setFare(100.0);
		mockTrain.setFrom_stn("Station A");
		mockTrain.setTo_stn("Station B");
		mockTrain.setTr_name("Express Train");

		// Mock context attributes
		when(servletContext.getAttribute("seats")).thenReturn(2);
		when(servletContext.getAttribute("trainnumber")).thenReturn("TR123");
		when(servletContext.getAttribute("journeydate")).thenReturn("2024-01-20");
		when(servletContext.getAttribute("class")).thenReturn("AC");

		// Mock train service response
		when(trainService.getTrainById("TR123")).thenReturn(mockTrain);
		when(trainService.updateTrain(any(TrainBean.class))).thenReturn("SUCCESS");

		// Mock booking service response
		HistoryBean mockHistory = new HistoryBean();
		mockHistory.setTransId("TXN123");
		when(bookingService.createHistory(any(HistoryBean.class))).thenReturn(mockHistory);

		// Execute
		bookTrains.doPost(request, response);
	}

	@Test
	public void testInsufficientSeats() throws Exception {
		// Setup test data
		when(servletContext.getAttribute("seats")).thenReturn(10);
		when(servletContext.getAttribute("trainnumber")).thenReturn("12345");
		when(servletContext.getAttribute("journeydate")).thenReturn("2023-12-25");
		when(servletContext.getAttribute("class")).thenReturn("AC");

		TrainBean mockTrain = new TrainBean();
		mockTrain.setSeats(5); // Only 5 seats available
		when(trainService.getTrainById("12345")).thenReturn(mockTrain);

		// Execute
		bookTrains.doPost(request, response);

		// Verify
		verify(trainService, never()).updateTrain(any(TrainBean.class));
		assertTrue(stringWriter.toString().contains("Only 5 Seats are Available"));
	}

	@Test
	public void testInvalidTrainNumber() throws Exception {
		// Setup test data
		when(servletContext.getAttribute("seats")).thenReturn(2);
		when(servletContext.getAttribute("trainnumber")).thenReturn("99999");
		when(servletContext.getAttribute("journeydate")).thenReturn("2023-12-25");
		when(servletContext.getAttribute("class")).thenReturn("AC");

		when(trainService.getTrainById("99999")).thenReturn(null);

		// Execute
		bookTrains.doPost(request, response);

		// Verify
		verify(trainService, never()).updateTrain(any(TrainBean.class));
		assertTrue(stringWriter.toString().contains("Invalid Train Number"));

	}

	@Test
	public void testFailedTransaction() throws Exception {
		TrainBean mockTrain = new TrainBean();
		mockTrain.setSeats(10);

		when(servletContext.getAttribute("seats")).thenReturn(2);
		when(servletContext.getAttribute("trainnumber")).thenReturn("TR123");
		when(servletContext.getAttribute("journeydate")).thenReturn("2024-01-20");
		when(trainService.getTrainById("TR123")).thenReturn(mockTrain);
		when(trainService.updateTrain(any(TrainBean.class))).thenReturn("FAILED");

		// Execute
		bookTrains.doPost(request, response);

		// Verify booking was not created
		verify(bookingService, never()).createHistory(any(HistoryBean.class));
	}

}
