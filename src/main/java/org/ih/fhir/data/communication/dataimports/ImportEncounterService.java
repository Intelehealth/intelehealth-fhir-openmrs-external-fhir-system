package org.ih.fhir.data.communication.dataimports;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.service.CommonOperationService;
import org.ih.fhir.data.communication.service.IHMarkerService;
import org.ih.fhir.data.communication.service.VisitTypeService;
import org.ih.fhir.data.communication.utils.HttpWebClient;
import org.ih.fhir.data.communication.utils.IHConstant;
import org.ih.fhir.data.communication.utils.QueryTable;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.IQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImportEncounterService extends IHConstant{

	@Autowired
	private IHMarkerService ihMarkerService;
	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private CommonOperationService commonOperationService;
	@Autowired
	private VisitTypeService visitType;
	ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private ImportLocationService importLocationService;
	

	public void importEncounter(String patientId,String locationUuid) throws UnsupportedEncodingException, JSONException, JsonProcessingException, ParseException {
		/*IQuery<Bundle> encounter = firFhirConfig.getOpenCRFhirContext()
				.search().forResource(Encounter.class)
				.where(Encounter.SUBJECT.hasAnyOfIds(patientId)).sort()
				.ascending("_lastUpdated").returnBundle(Bundle.class);
		Bundle encounterTasksBundle = encounter.execute();*/
		
		String data = HttpWebClient.get(shrUrl, "rest/v1/bundle/Encounter?subject=" + patientId+"&_sort=_lastUpdated", firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(
				Bundle.class, data);
		
		saveToLocal(theBundle,locationUuid);
		if (theBundle.hasEntry()) {
			System.out.println("Got ENcounter bundle size"
					+ theBundle.getEntry().size());
		}
	}

	private Bundle saveToLocal(Bundle originalTasksBundle,String locationUuid) throws UnsupportedEncodingException, JSONException, JsonProcessingException, ParseException {
		Bundle transactionBundle = new Bundle();
		String table = "";
		String primaryKey = "";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			// todo handle multiple indetifier
			Encounter encounter = (Encounter) bundleEntry.getResource();
		//	System.err.println("part::"+encounter.getPartOf());
			System.err.println(encounter.getMeta().getTagFirstRep().getCode());
			String encounterOrVisitUuid = "";
			String typeName = encounter.getType().get(0).getCoding().get(0)
					.getDisplay();
			String typeUUid = encounter.getType().get(0).getCoding().get(0).getCode();
			//System.err.println(typeName+":"+typeUUid);
			//
			//System.err.println("typeName:" + typeName);
			if (encounter.getMeta().getTagFirstRep().getCode()
					.equalsIgnoreCase("encounter")) {
				table = QueryTable.ENCOUNTER.value;
				primaryKey = QueryTable.ENCOUNTER_PK.value;
				encounterOrVisitUuid = commonOperationService
						.findResourceUuidByName(table + "_type", typeName,
								"name", "uuid");
				if(StringUtils.isBlank(encounterOrVisitUuid)){
					
					visitType.saveVisitType(typeName, typeUUid,"encountertype");
					encounterOrVisitUuid = typeUUid;
				}

			} else if (encounter.getMeta().getTagFirstRep().getCode()
					.equalsIgnoreCase("visit")) {
				
				table = QueryTable.VISIT.value;
				primaryKey = QueryTable.VISIT_PK.value;
				encounterOrVisitUuid = commonOperationService
						.findResourceUuidByName(table + "_type", typeName,
								"name", "uuid");
				
				if(StringUtils.isBlank(encounterOrVisitUuid)){
					
					visitType.saveVisitType(typeName, typeUUid,"visittype");
					encounterOrVisitUuid = typeUUid;
				}
			}
			
			Resource resource = (Resource) bundleEntry.getResource();
			encounter.getType().get(0).getCoding().get(0)
					.setCode(encounterOrVisitUuid);
			System.err.println("Size::"+encounter.getLocation().size());
			int i= 0;
			for (EncounterLocationComponent location : encounter.getLocation()) {
				Reference l =  location.getLocation();
				String  uuid = l.getReferenceElement().getIdPart();
				System.err.println(l.getDisplay());
				if(StringUtils.isBlank(locationUuid)) {
					String locationuuid =  commonOperationService.findResourceUuidByName("location", l.getDisplay(), "name", "uuid");
					if(!StringUtils.isBlank(locationuuid)){
						locationUuid= locationuuid;
					}else{
						importLocationService.importLocation(uuid);
					}
				}else{
					encounter.getLocation().get(i).getLocation().setReference("Location/"+locationUuid);
				}
				
				i++;
			}
			if(StringUtils.isBlank(locationUuid)){
				encounter.getLocation();
			}
			// partitipant doen not support
			if (encounter.getParticipantFirstRep() != null) {
				encounter.setParticipant(null);
				/*
				 * r.getParticipantFirstRep().getIndividual().setReference(
				 * "Practitioner/"+"5c7814ca-facc-48a0-a652-734b25d238b2");
				 * r.getParticipantFirstRep
				 * ().getIndividual().setType("Practitioner"); Identifier
				 * identifier =new Identifier(); identifier.setValue("admin");
				 * r.
				 * getParticipantFirstRep().getIndividual().setIdentifier(identifier
				 * );
				 */
			}
			System.err.println("DDD>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(encounter));
			Integer encounterLocalId = commonOperationService
					.findResourceIdByUuid(table, resource.getIdElement()
							.getIdPart(), primaryKey);
			System.err.println("IDL::::" + encounterLocalId);
			if (encounterLocalId == null || encounterLocalId == 0) {
				MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext()
						.create().resource(encounter).execute();
				encounterLocalId = commonOperationService.findResourceIdByUuid(
						table, res.getId().getIdPart(), primaryKey);
				commonOperationService.updateResource(table, encounterLocalId,
						resource.getIdElement().getIdPart(), primaryKey);

			} else {
				
				  firFhirConfig.getLocalOpenMRSFhirContext().update()
				  .resource(encounter).execute();
				 

				System.err.println("Do nothing");

			}
		}
		return transactionBundle;
	}
}
