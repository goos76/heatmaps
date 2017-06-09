package nl.will.heatmaps.model;

import java.io.Serializable;

public class Location implements Serializable {

	public String postalCode;

	public double lat;

	public double lng;

	public int weight;

	public Location() {
		super();
	}

	public Location(String postalCode, double latitude, double longitude) {
		super();
		this.postalCode = postalCode;
		this.lat = latitude;
		this.lng = longitude;
	}
}
