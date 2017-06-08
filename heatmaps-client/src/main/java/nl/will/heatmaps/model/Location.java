package nl.will.heatmaps.model;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class Location implements Serializable{
	
	@SerializedName("postalCode")
	public String postalCode;
	@SerializedName("lat")
	public double latitude;
	@SerializedName("lng")
	public double longitude;
	
	public Location(){
		super();
	}
	
	public Location(String postalCode,double latitude, double longitude) {
		super();
		this.postalCode = postalCode;
		this.latitude = latitude;
		this.longitude = longitude;
	}
}
