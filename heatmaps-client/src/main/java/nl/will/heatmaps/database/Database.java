/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.will.heatmaps.database;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import nl.will.heatmaps.model.Customer;

public class Database {

	private static final String CUSTOMERS = "customers";
	private static final String DATABASE_URL = "https://heatmaps-168813.firebaseio.com/";
	private static final Database INSTANCE = new Database();

	private DatabaseReference database;

	private Database() {

		try (InputStream fileInputStream = Database.class.getResourceAsStream("/service-account.json")) {

			FirebaseOptions options = new FirebaseOptions.Builder().setServiceAccount(fileInputStream)
					.setDatabaseUrl(DATABASE_URL).build();
			FirebaseApp.initializeApp(options);

		} catch (Exception e) {

			throw new RuntimeException("", e);
		}

		// Shared Database reference
		database = FirebaseDatabase.getInstance().getReference();

	}

	public static Database instance() {

		return INSTANCE;
	}

	public List<Customer> selectCustomers() {

		try {
			Response response = new Response();
			ValueEventListener listener = new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {

					Iterable<DataSnapshot> featuresFound = dataSnapshot.getChildren();
					for (DataSnapshot found : featuresFound) {
						Iterable<DataSnapshot> children = found.getChildren();
						for (DataSnapshot customerSnapshot : children) {
							response.customers.add(customerSnapshot.getValue(Customer.class));
						}

					}

					response.setDone();

				}

				@Override
				public void onCancelled(DatabaseError error) {
					throw new RuntimeException("database error" + error);

				}
			};
			DatabaseReference customers = database.child(CUSTOMERS).startAt("0000XX").getRef();
			customers.addListenerForSingleValueEvent(listener);

			response.waitForResponse();

			return response.customers;
		} catch (Exception e) {
			throw new RuntimeException("selectFeatures failed", e);
		}

	}

	public List<Customer> selectCustomers(String postalcode) {
		try {
			Response response = new Response();
			ValueEventListener listener = new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {

					Iterable<DataSnapshot> featuresFound = dataSnapshot.getChildren();
					for (DataSnapshot found : featuresFound) {

						response.customers.add(found.getValue(Customer.class));

					}

					response.setDone();

				}

				@Override
				public void onCancelled(DatabaseError error) {
					throw new RuntimeException("database error" + error);

				}
			};
			DatabaseReference customers = database.child(CUSTOMERS).child(postalcode);
			customers.addListenerForSingleValueEvent(listener);

			response.waitForResponse();

			return response.customers;
		} catch (Exception e) {
			throw new RuntimeException("selectFeatures failed", e);
		}
	}

	public Customer selectCustomer(Customer customer) {

		Response response = new Response();
		ValueEventListener listener = new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				Customer customer = dataSnapshot.getValue(Customer.class);
				response.customer = customer;
				response.setDone();

			}

			@Override
			public void onCancelled(DatabaseError error) {
				throw new RuntimeException("database error" + error);

			}
		};
		DatabaseReference customers = getBasePath(customer);
		customers.child(customer.id).addListenerForSingleValueEvent(listener);

		response.waitForResponse();

		return response.customer;
	}

	public String store(Customer customer) {
		String key = customer.id;
		if (StringUtils.isEmpty(customer.id)) {
			key = database.push().getKey();
			customer.id = key;
		}

		Response response = new Response();
		CompletionListener listener = new CompletionListener() {

			@Override
			public void onComplete(DatabaseError error, DatabaseReference databaseReference) {
				if (error != null) {
					throw new RuntimeException("database error" + error);
				}
				response.setDone();

			}
		};
		DatabaseReference customers = getBasePath(customer).child(key);
		customers.setValue(customer, listener);
		customers.child("location").setValue(customer.location);

		database.push();
		response.waitForResponse();

		return key;

	}

	public void delete(Customer customer) {
		Response response = new Response();
		CompletionListener listener = new CompletionListener() {

			@Override
			public void onComplete(DatabaseError error, DatabaseReference databaseReference) {
				if (error != null) {
					throw new RuntimeException("database error" + error);
				}
				response.setDone();

			}
		};
		getBasePath(customer).child(customer.id).setValue(null, listener);
		database.push();
		response.waitForResponse();

	}

	void deleteAll() {
		Response response = new Response();
		CompletionListener listener = new CompletionListener() {

			@Override
			public void onComplete(DatabaseError error, DatabaseReference databaseReference) {
				if (error != null) {
					throw new RuntimeException("database error" + error);
				}
				response.setDone();

			}
		};
		database.child(CUSTOMERS).setValue(null, listener);
		database.push();
		response.waitForResponse();

	}

	private DatabaseReference getBasePath(Customer customer) {
		DatabaseReference customers = database.child(CUSTOMERS).child(customer.postalCode);
		return customers;
	}

	private class Response {
		Customer customer;
		List<Customer> customers = new ArrayList<>();
		AtomicBoolean done = new AtomicBoolean(false);

		Response() {
			done.set(false);
		}

		void setDone() {
			done.set(true);
		}

		void waitForResponse() {
			while (!done.get()) {
			}
			return;

		}
	}

}
