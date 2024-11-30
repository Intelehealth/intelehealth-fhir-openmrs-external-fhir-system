package org.ih.fhir.data.communication.utils;

public enum QueryTable {

	ENCOUNTER("encounter"), ENCOUNTER_PK("encounter_id"),

	VISIT("visit"), VISIT_PK("visit_id"), ORDERS("orders"), ORDERS_PK(
			"order_id"), OBS("obs"), OBS_PK("obs_id"), PERSON("person"), PERSON_PK(
			"person_id");

	public final String value;

	private QueryTable(String value) {
		this.value = value;
	}

}
