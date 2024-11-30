package org.ih.fhir.data.communication.dataimports;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImportPatientService extends IHConstant {

	@Autowired
	private IHMarkerService ihMarkerService;
	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private CommonOperationService commonOperationService;
	@Autowired
	private ImportObservationService importObservationService;
	@Autowired
	private ImportEncounterService importEncounterService;
	@Autowired
	private ImportLabOrderService importLabOrderService;
	@Autowired
	private ImportPrescriptionService importPrescriptionService;
	@Autowired
	private ImportLocationService importLocationService;
	@Autowired
	private VisitTypeService visitType;
	ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private ImportDiagnosticService  importDiagnostic;
	@Autowired
	private ImportAppointment importAppointment;

	public void importPatient(String patientId,String locationUuid) throws JSONException, ParseException, IOException {
		/*Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Patient?_id=" + patientId).returnBundle(Bundle.class)
				.execute();
		System.err.println("patientId::" + patientId);*/
		
		importAppointment.importAppointmentExternal();
		
		
		String data = HttpWebClient.get(shrUrl, "rest/v1/bundle/Patient?_id=" + patientId, firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(
				Bundle.class, data);
		if (theBundle.hasEntry()) {
			System.out.println("Got MedicationRequest bundle size"
					+ theBundle.getEntry().size());
		}
		
		saveToLocal(theBundle, QueryTable.PERSON.value,QueryTable.PERSON_PK.value,locationUuid);
		
		
		try {
			importEncounterService.importEncounter(patientId,locationUuid);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		importDiagnostic.importDiagnostic(patientId, locationUuid);
		
		try {
			importObservationService.importObservation(patientId);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		try {
			importLabOrderService.importLabOrder(patientId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			importPrescriptionService.importPrescription(patientId);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		System.err.println("Done");
	//	importAppointment.importAppointment();
		importAppointment.importAppointmentExternal();

	}

	private Bundle saveToLocal(Bundle originalTasksBundle, String table,
			String primaryKey,String locationUuid) throws JsonProcessingException, UnsupportedEncodingException, JSONException, ParseException {
		Bundle transactionBundle = new Bundle();

		transactionBundle.setType(Bundle.BundleType.TRANSACTION);

		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			Patient patient = (Patient) bundleEntry.getResource();
		
			
			/*Extension ex=	patient.getIdentifierFirstRep().getExtensionFirstRep();
		
			Reference providerReference = (Reference) ex.getValue();
			if(StringUtils.isBlank(locationUuid)) {
				String location = providerReference.getReference().split("/")[1];
				importLocationService.importLocation(location);
			
			}else{
				providerReference.setReference("Location/"+locationUuid);
			}*/
			
			
			
			
			
			//patient.getIdentifierFirstRep().getExtensionFirstRep().setValue(providerReference);
			
			
			
		//System.err.println("providerReference.getReference()"+providerReference.getReference());
		
		
		
			//System.err.println("Iden:;;;;;;;;;;"+patient.getIdentifierFirstRep().getExtensionFirstRep());
			Resource resource = (Resource) bundleEntry.getResource();
			
			//patient.getIdentifierFirstRep().removeExtension("http://fhir.openmrs.org/ext/patient/identifier#location");
			// todo get indetifier type from local
			 List<Identifier>  identifiers= patient.getIdentifier();
			 int i=0;
			 // handle multiple identifier
			 for (Identifier identifier : patient.getIdentifier()) {
				 
				 String code = identifier.getType().getCodingFirstRep().getCode();
				 System.out.println("code::::"+code);
				 String identifierUUid =  commonOperationService.findResourceUuidByName("patient_identifier_type", patient.getIdentifier().get(i).getType().getText(), "name", "uuid");
				 if(StringUtils.isBlank(identifierUUid)){
					 identifierUUid = visitType.saveVisitType(patient.getIdentifier().get(i).getType().getText(), code, "patientidentifiertype");
				 }
				 System.err.println("Text::"+patient.getIdentifier().get(i).getType().getText());
				 patient.getIdentifier().get(i).getType().getCoding().get(0)
					.setCode(identifierUUid);
				 patient.getIdentifier().get(i).getType().setText(patient.getIdentifier().get(i).getType().getText());
				 patient.getIdentifier().get(i).setSystem("");
				
				 
				 
				 Extension ex=	patient.getIdentifier().get(i).getExtensionFirstRep();
					
					Reference providerReference = (Reference) ex.getValue();
					
					if(StringUtils.isBlank(locationUuid)) {
						String location = providerReference.getReference().split("/")[1];
						String locationName= providerReference.getDisplay();
						
						String locationuuid =  commonOperationService.findResourceUuidByName("location", locationName, "name", "uuid");
						if(!StringUtils.isBlank(locationuuid)){
							locationUuid= locationuuid;
						}else{
							importLocationService.importLocation(location);
						}
					
					}else{
						providerReference.setReference("Location/"+locationUuid);
					}
				 
					 i++;
			}
			
			/*patient.getIdentifierFirstRep().getType().getCoding().get(0)
					.setCode("3b9f72a0-4849-45b1-af53-224f75d417ca");
			
			patient.getIdentifierFirstRep().getType().setText("Test");
			
			patient.getIdentifierFirstRep().setSystem("");*/
			
			// r.getIdentifierFirstRep().setValue("234561234");
			
			// todo telecom and maritalsttaus
			patient.setTelecom(null);
			Integer patientLocalId = commonOperationService
					.findResourceIdByUuid(table, resource.getIdElement()
							.getIdPart(), primaryKey);
			System.err.println("patientLocalId::::" + patientLocalId);
			
			System.err.println("DDD>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(patient));
			if (patient.getName() != null) {
				if (patientLocalId == null || patientLocalId == 0) {
					MethodOutcome res = firFhirConfig
							.getLocalOpenMRSFhirContext().create()
							.resource(patient).execute();
					patientLocalId = commonOperationService
							.findResourceIdByUuid(table, res.getId()
									.getIdPart(), primaryKey);
					commonOperationService.updateResource(table,
							patientLocalId,
							resource.getIdElement().getIdPart(), primaryKey);

				} else {
					
					 firFhirConfig.getLocalOpenMRSFhirContext().update()
					 .resource(patient).execute();
					
					System.out.println("DO nothing-.............");
				}
			}

		}

		return transactionBundle;
	}

}
