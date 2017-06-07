package nl.will.heatmaps.service;

import com.google.gson.annotations.SerializedName;

import nl.will.heatmaps.model.Location;


public class Result {
	@SerializedName("geometry")
	public Geometry geometry;
}

class Geometry {
	@SerializedName("location")
	public Location location;
}
