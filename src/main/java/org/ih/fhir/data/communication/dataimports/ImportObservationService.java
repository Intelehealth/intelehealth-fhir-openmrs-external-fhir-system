package org.ih.fhir.data.communication.dataimports;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.service.CommonOperationService;
import org.ih.fhir.data.communication.service.IHMarkerService;
import org.ih.fhir.data.communication.utils.HttpWebClient;
import org.ih.fhir.data.communication.utils.IHConstant;
import org.ih.fhir.data.communication.utils.QueryTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.IQuery;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImportObservationService extends IHConstant{

	@Autowired
	private IHMarkerService ihMarkerService;
	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private CommonOperationService commonOperationService;
	ObjectMapper mapper = new ObjectMapper();

	public void importObservation(String patientId) throws UnsupportedEncodingException {
		/*IQuery<Bundle> observation = firFhirConfig.getOpenCRFhirContext()
				.search().forResource(Observation.class)
				.where(Encounter.SUBJECT.hasAnyOfIds(patientId)).sort()
				.ascending("_lastUpdated").returnBundle(Bundle.class);
		Bundle observationTasksBundle = observation.execute();*/
		
		
		String data = HttpWebClient.get(shrUrl, "rest/v1/bundle/Observation?subject=" + patientId+"&_sort=_lastUpdated", firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(
				Bundle.class, data);
		
		
		saveToLocal(theBundle);
		if (theBundle.hasEntry()) {
			System.out.println("Got Obs bundle size"
					+ theBundle.getEntry().size());
		}
	}

	private Bundle saveToLocal(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		String table = QueryTable.OBS.value;
		String primaryKey = QueryTable.OBS_PK.value;
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			Observation obs = (Observation) bundleEntry.getResource();

			String conceptUuid = "";

			List<Coding> codings = obs.getCode().getCoding();

			List<String> codes = new ArrayList<String>();

			for (Coding coding : codings) {

				codes.add("'" + coding.getCode() + "'");
				codes.add("'" + coding.getDisplay() + "'");
			}

			conceptUuid = commonOperationService
					.findConceptUuidByMappingCode(String.join(",", codes));
			obs.getCode().getCoding().get(0).setCode(conceptUuid);

			Resource resource = (Resource) bundleEntry.getResource();

			/*
			 * System.err.println("DDD>>>>>>>>" +
			 * fhirContext.newJsonParser().setPrettyPrint(true)
			 * .encodeResourceToString(obs));
			 */
			Integer obsLocalId = commonOperationService.findResourceIdByUuid(
					table, resource.getIdElement().getIdPart(), primaryKey);

			if (obsLocalId == null || obsLocalId == 0) {
				MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext()
						.create().resource(obs).execute();

				obsLocalId = commonOperationService.findResourceIdByUuid(table,
						res.getId().getIdPart(), primaryKey);

				commonOperationService.updateResource(table, obsLocalId,
						resource.getIdElement().getIdPart(), primaryKey);

			} else {
				/*
				 * firFhirConfig.getLocalOpenMRSFhirContext().update()
				 * .resource(obs).execute();
				 */
				System.err.println("Do nothing");

			}
		}
		return transactionBundle;
	}

}
