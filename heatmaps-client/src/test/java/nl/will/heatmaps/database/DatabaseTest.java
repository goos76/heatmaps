package nl.will.heatmaps.database;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import nl.will.heatmaps.model.Customer;
import nl.will.heatmaps.model.Location;

public class DatabaseTest {
	private Database database = Database.instance();
	private Customer customer;
	
	@Before
	public void setUp(){
		database.deleteAll();
	}

	@Test
	public void testStore() {
		customer = new Customer("4811AT", 12, "Basis");
		customer.location = new Location(1, 1);
		String id = database.store(customer);
		assertNotNull(id);
	}

	@Test
	public void testSelectCustomers() {

		testStore();
		List<Customer> customers = database.selectCustomers();
		assertTrue(customers.size() > 0);

	}
	@Test
	public void testSelectCustomersByPostalcode() {

		testStore();
		List<Customer> customers = database.selectCustomers("4811AT");
		assertTrue(customers.size() > 0);

	}

	@Test
	public void testSelectCustomer() {

		testStore();
		customer = database.selectCustomer(customer);
		assertNotNull(customer);
		assertEquals("4811AT", customer.postalCode);

	}

	@Test
	public void testDeleteCustomer() {

		testStore();
		customer = database.selectCustomer(customer);
		assertNotNull(customer);
		assertEquals("4811AT", customer.postalCode);
		
		database.delete(customer);
		
		customer = database.selectCustomer(customer);
		assertNull(customer);

	}

}
