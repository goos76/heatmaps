package nl.will.heatmaps.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.will.heatmaps.database.Database;
import nl.will.heatmaps.model.Customer;
import nl.will.heatmaps.model.Location;

public class Service {
	private static final String POSTCODE_CSV = "/postcodes100.csv";
	private static final Logger LOG = Logger.getLogger(Service.class);

	private static final Service INSTANCE = new Service();

	private static final String API_KEY = "AIzaSyDRaHffz_Advs4_dgputC67P5roDUEoBIA";

	private ArrayList<Customer> customers = new ArrayList<>();
	private Database database = Database.instance();

	public static Service instance() {
		return INSTANCE;
	}

	private Service() {
		super();
	}

	public List<Customer> parseCsv() {

		try (InputStream input = this.getClass().getResourceAsStream(POSTCODE_CSV)) {
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(';').withHeader("").withSkipHeaderRecord(true)
					.parse(new InputStreamReader(input, "UTF-8"));
			for (CSVRecord record : records) {

				String postalCode = record.get(1);
				String insuranceType = record.get(2);
				String ageAsString = record.get(4);
				Customer customer = new Customer(postalCode, Integer.parseInt(StringUtils.remove(ageAsString, " jaar")),
						insuranceType);
				// Location location = getLocation(postalCode);
				customer.location = new Location(1, 1);

				customers.add(customer);
				database.store(customer);

			}

		} catch (Exception e) {
			LOG.warn("error", e);
			throw new RuntimeException(e);
		}

		return customers;

	}

	Location getLocation(String postalCode) {
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
			LOG.warn("error", e);
			return null;
		}
	}

	public List<Customer> heatmaps() {
		return database.selectCustomers();

	}

}
