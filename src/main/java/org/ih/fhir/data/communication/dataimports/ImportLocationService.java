package org.ih.fhir.data.communication.dataimports;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Resource;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.service.CommonOperationService;
import org.ih.fhir.data.communication.service.IHMarkerService;
import org.ih.fhir.data.communication.utils.HttpWebClient;
import org.ih.fhir.data.communication.utils.IHConstant;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImportLocationService extends IHConstant{

	@Autowired
	private IHMarkerService ihMarkerService;
	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private CommonOperationService commonOperationService;
	
	ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private ImportAppointment importAppointment;

	public void importLocation(String patientId) throws JsonProcessingException,
			UnsupportedEncodingException, JSONException, ParseException {
		/*Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Location?_id=" + patientId).returnBundle(Bundle.class)
				.execute();*/
		
		String data = HttpWebClient.get(shrUrl, "rest/v1/bundle/Location?_id=" + patientId, firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(
				Bundle.class, data);
		System.err.println("patientId::" + patientId);
		saveToLocal(theBundle);
		

	}

	private Bundle saveToLocal(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();

		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			Location r = (Location) bundleEntry.getResource();

			Resource resource = (Resource) bundleEntry.getResource();

			// System.err.println("Patirnt>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(r));

			System.err.println();
			Integer id = commonOperationService.findLoationByUuid("location",
					resource.getIdElement().getIdPart(),r.getName());
			System.err.println("IDL::::" + id);

			if (r.getName() != null) {
				if (id == null || id == 0) {
					MethodOutcome res = firFhirConfig
							.getLocalOpenMRSFhirContext().create().resource(r)
							.execute();
					id = commonOperationService.findLoationByUuid("location",
							res.getId().getIdPart(),r.getName());
					commonOperationService.updateResource("location", id,
							resource.getIdElement().getIdPart(), "location_id");

				} else {
					firFhirConfig.getLocalOpenMRSFhirContext().update()
							.resource(r).execute();

				}
			}

			
		}

		return transactionBundle;
	}

}
