package nl.will.heatmaps.model;

import java.io.Serializable;

public class Customer implements Serializable {

	public String id;
	public String postalCode;
	public int age;
	public String insuranceType;
	
	public Location location;

	public Customer() {

	}

	public Customer(String postalCode, int age, String insuranceType) {
		super();
		this.postalCode = postalCode;
		this.age = age;
		this.insuranceType = insuranceType;
	}
}
