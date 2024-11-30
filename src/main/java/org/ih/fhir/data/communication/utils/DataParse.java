package org.ih.fhir.data.communication.utils;

import org.json.JSONException;
import org.json.JSONObject;

public class DataParse {
	
	public static String getOrderIdFromEncounterJson(String data) throws JSONException{		
		
		JSONObject encounter = new JSONObject(data);
		JSONObject order = (JSONObject) encounter.getJSONArray("orders").get(0);
		System.err.println(order.get("uuid"));	
		
		return order.get("uuid").toString();
		
	}
	public static String getOrderIdFromOrderJson(String data) throws JSONException{		
		
		JSONObject order = new JSONObject(data);
		
		System.err.println(order.get("uuid"));	
		
		return order.get("uuid").toString();
		
	}

}
