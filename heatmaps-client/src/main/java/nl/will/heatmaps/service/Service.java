package nl.will.heatmaps.service;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.will.heatmaps.database.Database;
import nl.will.heatmaps.model.Customer;
import nl.will.heatmaps.model.Location;

public class Service {

	private static final String POSTCODE_CSV = "/postcodesAll.csv";
	private static final Logger LOG = Logger.getLogger(Service.class);

	private static final Service INSTANCE = new Service();

	private static final String API_KEY = "AIzaSyDytBBZ23RuJZBvWzEjatQTNm4geyDQygA";

	private Database database = Database.instance();
	private final HashMap<String, Location> locationCache = new HashMap<>();
	private boolean useDatabase = true;
	private boolean useGeoService = true;

	public static Service instance() {
		return INSTANCE;
	}

	private Service() {
		super();
	}

	public void setUseDatabase(boolean useDatabase) {
		this.useDatabase = useDatabase;
	}

	public void setUseGeoService(boolean useGeoService) {
		this.useGeoService = useGeoService;
	}

	public void parseCsvToCustomersFirebase(int from, int to, boolean forceDelete) {
		if (forceDelete) {
			database.deleteAll();
		}
		List<Customer> customers = new ArrayList<>();
		try (InputStream input = this.getClass().getResourceAsStream(POSTCODE_CSV)) {
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(';').withHeader("").withSkipHeaderRecord(true)
					.parse(new InputStreamReader(input, "UTF-8"));
			int count = 0;
			for (CSVRecord record : records) {

				if (count >= from && (count <= to || to == 0)) {
					String key = record.get(0);
					String postalCode = record.get(1);
					String insuranceType = record.get(2);
					String ageAsString = record.get(4);
					Customer customer = new Customer(postalCode,
							Integer.parseInt(StringUtils.remove(ageAsString, " jaar")), insuranceType);
					customer.id = key;
					Location location = getLocation(postalCode);
					if (location != null) {
						customer.location = location;

						customers.add(customer);
						database.store(customer);
					}
				}
				count++;

			}

		} catch (Exception e) {
			LOG.warn("error", e);
			throw new RuntimeException(e);
		}

	}

	public void parseCsvToCustomersJson() {

		JsonObject customersObject = new JsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (InputStream input = this.getClass().getResourceAsStream(POSTCODE_CSV)) {
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(';').withHeader("").withSkipHeaderRecord(true)
					.parse(new InputStreamReader(input, "UTF-8"));

			for (CSVRecord record : records) {

				String key = record.get(0);
				String postalCode = record.get(1);
				String insuranceType = record.get(2);
				String ageAsString = record.get(4);
				Customer customer = new Customer(postalCode, Integer.parseInt(StringUtils.remove(ageAsString, " jaar")),
						insuranceType);
				customer.id = key;
				Location location = getLocation(postalCode);
				if (location != null) {
					customer.location = location;

					JsonObject jsonKey = new JsonObject();
					String customerString = gson.toJson(customer);
					jsonKey.add(customer.id, gson.fromJson(customerString, JsonElement.class));
					customersObject.add(customer.postalCode, jsonKey);

				}

			}

		} catch (Exception e) {
			LOG.warn("error", e);
			throw new RuntimeException(e);
		}
		writeToFile(customersObject, "C:/heatmaps/customers.json");

	}

	public List<Customer> parseCsvToCustomers() {

		List<Customer> customers = new ArrayList<>();

		try (InputStream input = this.getClass().getResourceAsStream(POSTCODE_CSV)) {
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(';').withHeader("").withSkipHeaderRecord(true)
					.parse(new InputStreamReader(input, "UTF-8"));

			for (CSVRecord record : records) {

				Customer customer = parseRecord(record);
				if (customer != null) {
					customers.add(customer);
				}

			}
			return customers;

		} catch (Exception e) {
			LOG.warn("error", e);
			throw new RuntimeException(e);
		}

	}

	private Customer parseRecord(CSVRecord record) {
		String postalCode = null;
		String key = null;
		try {
			key = record.get(0);
			postalCode = record.get(1);
			postalCode = postalCode.replaceAll("[^a-zA-Z0-9]", "");
			String insuranceType = record.get(2);
			String ageAsString = record.get(4);
			Customer customer = new Customer(postalCode, Integer.parseInt(StringUtils.remove(ageAsString, " jaar")),
					insuranceType);
			customer.id = key;
			Location location = getLocation(postalCode);
			if (location != null) {
				customer.location = location;
				return customer;
			}
			return null;
		} catch (Exception e) {
			LOG.warn(" parseRecord error " + key + " " + postalCode, e);
			return null;
		}
	}

	public void locationsToJson() {
		cacheLocations();
		List<Customer> customers = parseCsvToCustomers();
		List<Location> locations = locations(customers);
		JsonObject locationsObject = new JsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		for (Location location : locations) {
			String locationString = gson.toJson(location);
			JsonObject jsonLocation = new JsonObject();
			jsonLocation.add("location", gson.fromJson(locationString, JsonElement.class));
			locationsObject.add(location.postalCode, jsonLocation);
		}

		writeToFile(locationsObject, "C:/heatmaps/postalcodes.json");

	}

	Location getLocation(String postalCode) {

		Location location = locationCache.get(postalCode);
		if (location != null) {
			return location;
		}
		if (useDatabase) {

			location = database.selectLocation(postalCode);
			if (location != null) {
				locationCache.put(postalCode, location);
				return location;
			}
		}
		if (useGeoService) {

			location = getGeoLocation(postalCode);
			if (location != null) {
				location.postalCode = postalCode;
				locationCache.put(postalCode, location);
				database.store(location);
			}
		}
		return location;

	}

	public List<Customer> heatmaps() {

		return database.selectCustomers();

	}

	public List<Location> locationsPostalcode4() {

		List<Location> locations = database.selectLocations();
		HashMap<String, Location> locationMap = new HashMap<>();
		for (Location location : locations) {
			String postalCode = StringUtils.substring(location.postalCode, 0, 4);
			location.postalCode = postalCode;
			if (!locationMap.containsKey(location.postalCode)) {
				locationMap.put(location.postalCode, location);
			}
			Location locationInMap = locationMap.get(location.postalCode);
			locationInMap.weight = location.weight + 1;
		}
		return new ArrayList<>(locationMap.values());

	}
	public List<Location> locations() {

		List<Location> locations = database.selectLocations();
		HashMap<String, Location> locationMap = new HashMap<>();
		for (Location location : locations) {
			String postalCode = StringUtils.substring(location.postalCode, 0, 6);
			location.postalCode = postalCode;
			if (!locationMap.containsKey(location.postalCode)) {
				locationMap.put(location.postalCode, location);
			}
			Location locationInMap = locationMap.get(location.postalCode);
			locationInMap.weight = location.weight + 1;
		}
		return new ArrayList<>(locationMap.values());

	}

	private List<Location> locations(List<Customer> customers) {
		HashMap<String, Location> locationMap = new HashMap<>();
		for (Customer customer : customers) {
			if (!locationMap.containsKey(customer.postalCode)) {
				locationMap.put(customer.postalCode, customer.location);
			}
			Location location = locationMap.get(customer.postalCode);
			location.weight = location.weight + 1;
		}
		return new ArrayList<>(locationMap.values());

	}

	private void cacheLocations() {
		List<Location> locations = this.locations();
		for (Location location : locations) {
			if (!locationCache.containsKey(location.postalCode)) {
				locationCache.put(location.postalCode, location);
			}
		}
	}

	private Location getGeoLocation(String postalCode) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(
					"https://maps.googleapis.com/maps/api/geocode/json?address=" + postalCode + "&key=" + API_KEY);
			CloseableHttpResponse response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();

			String responseContent = IOUtils.toString(entity.getContent());

			JsonParser parser = new JsonParser();
			JsonObject obj = parser.parse(responseContent).getAsJsonObject();

			JsonArray results = obj.get("results").getAsJsonArray();
			if (results.size() == 0) {
				return null;
			}

			JsonElement jsonElement = results.get(0).getAsJsonObject().get("geometry").getAsJsonObject()
					.get("location");
			return new Gson().fromJson(jsonElement, Location.class);

		} catch (Exception e) {
			LOG.warn("error getGeoLocation " + postalCode);
			return null;
		}
	}

	private void writeToFile(JsonElement element, String filename) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (FileWriter writer = new FileWriter(filename)) {
			gson.toJson(element, writer);
		} catch (Exception e) {
			LOG.warn("error", e);
			throw new RuntimeException(e);
		}
	}

}
