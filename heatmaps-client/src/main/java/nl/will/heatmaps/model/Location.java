package nl.will.heatmaps.model;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class Location implements Serializable{
	
	@SerializedName("lat")
	public double latitude;
	@SerializedName("lng")
	public double longitude;
	
	public Location(){
		super();
	}
	
	public Location(double latitude, double longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}
}
