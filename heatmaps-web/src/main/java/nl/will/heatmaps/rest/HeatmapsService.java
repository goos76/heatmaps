package nl.will.heatmaps.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import nl.will.heatmaps.model.Customer;
import nl.will.heatmaps.model.HeatmapsResult;
import nl.will.heatmaps.service.Service;

@Path("/service")
public class HeatmapsService {
	private Service service = Service.instance(); 

	@GET
	@Path("/results")
	@Produces("application/json")
	public Response heatmaps() {

		List<Customer> customers = service.heatmaps();
		HeatmapsResult heatmapsResult = new HeatmapsResult();
		heatmapsResult.customers = customers;

		Gson gson = new Gson();
		String result = gson.toJson(heatmapsResult);
		return Response.status(Response.Status.OK).entity(result).build();
	}

	@GET
	@Path("/csv")
	@Produces("application/json")
	public Response csv() {

		service.parseCsv();

		Gson gson = new Gson();
		HeatmapsResult heatmapsResult = new HeatmapsResult();
		heatmapsResult.status = "ok";
		String result = gson.toJson(heatmapsResult);
		return Response.status(Response.Status.OK).entity(result).build();
	}

}