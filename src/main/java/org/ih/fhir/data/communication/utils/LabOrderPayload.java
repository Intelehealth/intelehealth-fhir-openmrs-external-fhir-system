package org.ih.fhir.data.communication.utils;

import org.ih.fhir.data.communication.dto.ServiceRequestUuids;

public class LabOrderPayload {
	private String payload;
	private ServiceRequestUuids serviceRequestUuids;

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public ServiceRequestUuids getServiceRequestUuids() {
		return serviceRequestUuids;
	}

	public void setServiceRequestUuids(ServiceRequestUuids serviceRequestUuids) {
		this.serviceRequestUuids = serviceRequestUuids;
	}

}
