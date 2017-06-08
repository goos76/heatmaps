package nl.will.heatmaps.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import nl.will.heatmaps.model.Customer;
import nl.will.heatmaps.model.Location;

public class ServiceTest {
	private static Logger LOG =Logger.getLogger(ServiceTest.class);
	
	private Service service = Service.instance(); 

	@Test
	public void testGetLocation() {
		Location location = service.getLocation("4811AT");
		assertNotNull(location);
	}
	@Test
	public void testGetLocationNotInDatabase() {
		Location location = service.getLocation("4811TT");
		assertNotNull(location);
	}
	@Test
	public void testCsv() {
		List<Customer> customers = service.parseCsv();
		assertFalse(customers.isEmpty());
	}

}
