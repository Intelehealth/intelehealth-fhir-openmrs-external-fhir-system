package org.ih.fhir.data.communication.scheduler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.domain.Orders;
import org.ih.fhir.data.communication.domain.PrescriptionDomain;
import org.ih.fhir.data.communication.domain.ServiceRequestDomain;
import org.ih.fhir.data.communication.dto.DrugAndConceptDTO;
import org.ih.fhir.data.communication.dto.Names;
import org.ih.fhir.data.communication.dto.PatientAddress;
import org.ih.fhir.data.communication.dto.SearchPateintDTO;
import org.ih.fhir.data.communication.dto.ServiceRequestUuids;
import org.ih.fhir.data.communication.model.IHMarker;
import org.ih.fhir.data.communication.search.PatientSearchParam;
import org.ih.fhir.data.communication.service.CommonOperationService;
import org.ih.fhir.data.communication.service.IHMarkerService;
import org.ih.fhir.data.communication.utils.DataParse;
import org.ih.fhir.data.communication.utils.HttpWebClient;
import org.ih.fhir.data.communication.utils.IHConstant;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.param.DateRangeParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Component
public class DataImport extends IHConstant {

	@Autowired
	private IHMarkerService ihMarkerService;
	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private CommonOperationService commonOperationService;
	ObjectMapper mapper = new ObjectMapper();

	@Scheduled(fixedDelay = 60000, initialDelay = 3000)
	public void importJob() throws ParseException, JSONException, IOException {
		System.err.println("import LOcation");
		importResource(Location.class, importLocation);
		// importResourceByPatientId(Patient.class);
		//getDiagnostic();
		String encoded = "";

	}

	private void getDiagnostic() throws IOException, JSONException {
		String res = HttpWebClient
				.get("http://localhost:5001/shr/",
						"rest/v1/bundle/DiagnosticReport?_id=03308c59-3885-4ee1-a5c3-04017b52edf8",
						firFhirConfig.getOpenMRSCredentials()[0],
						firFhirConfig.getOpenMRSCredentials()[1]);

		Bundle theBundle = fhirContext.newJsonParser().parseResource(
				Bundle.class, res);
		JSONObject ob = new JSONObject(res);

		JSONObject entry = ob.getJSONArray("entry").optJSONObject(0);
		JSONObject resource = entry.getJSONObject("resource");
		// System.err.println(resou);
		JSONObject PRE = resource.getJSONArray("presentedForm").optJSONObject(0);
		// System.err.println(PRE.get("data"));
		importDicom(theBundle, PRE.get("data").toString());
	}

	private Bundle importDicom(Bundle originalTasksBundle, String data)
			throws IOException, JSONException {
		Bundle transactionBundle = new Bundle();

		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			DiagnosticReport r = (DiagnosticReport) bundleEntry.getResource();
			System.err.println(r.getResourceType());
			System.err.println(r.getPresentedForm().get(0).getData().length);
			System.err.println(r.getId());
			System.err.println(r.getPresentedForm().get(0).getContentType());

			Resource resource = (Resource) bundleEntry.getResource();
			String patient = r.getSubject().getReference().split("/")[1];
			String encounter = r.getEncounter().getReference().split("/")[1];
			System.err.println("DDDccc>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(r));

			JSONObject obs = new JSONObject();
			obs.put("person", patient);
			obs.put("encounter", encounter);
			obs.put("obsDatetime", new Date().toInstant());
			obs.put("value", "pro.pdf");

			List<Coding> codings = r.getCode().getCoding();
			List<String> codes = new ArrayList<String>();
			for (Coding coding : codings) {
				codes.add("'" + coding.getCode() + "'");
			}

			String concetpUUid = commonOperationService
					.findConceptUuidByMappingCode(String.join(",", codes));

			obs.put("concept", concetpUUid);
			byte[] decoded = java.util.Base64.getDecoder().decode(data);
			System.err.println(obs);
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("/opt/multimedia/pro.pdf");
				fos.write(decoded);
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) { // TODO Auto-generated catch
												// block
				e.printStackTrace();
			}

			Integer id = commonOperationService.findResourceIdByUuid("obs",
					resource.getIdElement().getIdPart(), "obs_id");
			System.err.println("IDL::::" + id);

			if (id == null || id == 0) {

				String res = HttpWebClient.postWithBasicAuth(
						"http://localhost:8082/openmrs", "/ws/rest/v1/obs",
						"admin", firFhirConfig.getOpenMRSCredentials()[1],
						obs.toString());
				System.err.println(res);
				JSONObject response = new JSONObject(res);
				System.err.println("response.getString:::"
						+ response.getString("uuid"));

				id = commonOperationService.findResourceIdByUuid("obs",
						response.getString("uuid"), "obs_id");

				System.err.println("ID:::" + id);
				commonOperationService.updateResource("obs", id, resource
						.getIdElement().getIdPart(), "obs_id");

			} else {
				System.err.println("Do nothing");
			}

		}

		// System.err.println("DDD>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle));
		// importFhirServer.transaction().withBundle(transactionBundle).execute();
		return transactionBundle;
	}

	private void importResource(Class<? extends IBaseResource> resouce,
			String markerName) throws ParseException {
		IHMarker marker = ihMarkerService.findByName(markerName);
		IQuery<Bundle> searchQuery = firFhirConfig
				.getGOFRFhirContext()
				.search()
				//
				.forResource(resouce)
				.lastUpdated(new DateRangeParam(marker.getLastSyncTime(), null))
				.sort().ascending("_lastUpdated").returnBundle(Bundle.class);

		Bundle originalTasksBundle = searchQuery.execute();
		importLocation(originalTasksBundle, marker);

	}

	/**
	 * {@link}
	 * 
	 * 
	 * */

	private Bundle importLocation(Bundle originalTasksBundle, IHMarker marker) {
		Bundle transactionBundle = new Bundle();

		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			Location r = (Location) bundleEntry.getResource();

			Resource resource = (Resource) bundleEntry.getResource();

			// System.err.println("Patirnt>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(r));

			marker.setLastSyncTime(resource.getMeta().getLastUpdated()
					.toInstant().toString());
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

			ihMarkerService.save(marker);

		}

		// System.err.println("DDD>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle));
		// importFhirServer.transaction().withBundle(transactionBundle).execute();
		return transactionBundle;
	}

	private Bundle importRemoteResouceToLocal(Bundle originalTasksBundle,
			String table, String primaryKey) {
		Bundle transactionBundle = new Bundle();

		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			Patient r = (Patient) bundleEntry.getResource();

			Resource resource = (Resource) bundleEntry.getResource();

			System.err.println(resource.getResourceType());
			// r.getIdentifierFirstRep().removeExtension("http://fhir.openmrs.org/ext/patient/identifier#location");
			r.getIdentifierFirstRep().getType().getCoding().get(0)
					.setCode("3b9f72a0-4849-45b1-af53-224f75d417ca");
			// r.setIdentifier(theIdentifier)
			r.getIdentifierFirstRep().getType().setText("Test");
			// r.getIdentifierFirstRep().setValue("234561234");
			r.getIdentifierFirstRep().setSystem("");

			// System.err.println("TTTT:"+r.getTelecom().get(0).getId());
			System.err.println("Patirnt>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(r));

			r.setTelecom(null);
			Integer id = commonOperationService.findResourceIdByUuid(table,
					resource.getIdElement().getIdPart(), primaryKey);
			System.err.println("IDL::::" + id);

			if (r.getName() != null) {
				if (id == null || id == 0) {
					MethodOutcome res = firFhirConfig
							.getLocalOpenMRSFhirContext().create().resource(r)
							.execute();
					id = commonOperationService.findResourceIdByUuid(table, res
							.getId().getIdPart(), primaryKey);
					commonOperationService.updateResource(table, id, resource
							.getIdElement().getIdPart(), primaryKey);

				} else {
					firFhirConfig.getLocalOpenMRSFhirContext().update()
							.resource(r).execute();

				}
			}

		}

		// System.err.println("DDD>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle));
		// importFhirServer.transaction().withBundle(transactionBundle).execute();
		return transactionBundle;
	}

	/*
	 * public getPatientInformations(String patientId){
	 * 
	 * }
	 * 
	 * private IBaseResource getOriginalResource(Resource resource){
	 * 
	 * }
	 */

	public List<SearchPateintDTO> searchPatient(PatientSearchParam param)
			throws ParseException, UnsupportedEncodingException, JSONException {

		StringBuilder searchParam = new StringBuilder();
		if (param.getIdentifiers() != null) {
			searchParam.append("identifier=" + param.getIdentifiers());
		}
		if (param.getGender() != null) {
			searchParam.append("&gender=" + param.getGender());
		}
		if (param.getBirthdate() != null) {
			searchParam.append("&birthdate=" + param.getBirthdate());
		}
		if (param.getFamily() != null) {
			searchParam.append("&family=" + param.getFamily());
		}
		if (param.getGiven() != null) {
			searchParam.append("&given=" + param.getGiven());
		}

		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Patient?" + searchParam).returnBundle(Bundle.class)
				.execute();
		// List<BundleEntryComponent> entires = results.getEntry();

		return generatePatient(results);
	}

	private List<SearchPateintDTO> generatePatient(Bundle originalTasksBundle) {
		List<SearchPateintDTO> patients = new ArrayList<SearchPateintDTO>();

		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			SearchPateintDTO patient = new SearchPateintDTO();

			Patient r = (Patient) bundleEntry.getResource();

			List<HumanName> names = r.getName();
			List<Names> naemdd = new ArrayList<Names>();

			List<Identifier> identifiers = r.getIdentifier();
			List<org.ih.fhir.data.communication.dto.Identifier> ids = new ArrayList<org.ih.fhir.data.communication.dto.Identifier>();
			for (Identifier identifier : identifiers) {
				org.ih.fhir.data.communication.dto.Identifier iddd = new org.ih.fhir.data.communication.dto.Identifier();
				iddd.setText(identifier.getType().getText());
				iddd.setValue(identifier.getValue());
				ids.add(iddd);
			}
			patient.setIdentifiers(ids);
			for (HumanName humanName : names) {
				Names d = new Names();
				d.setFamily(humanName.getFamily());
				List<StringType> givens = humanName.getGiven();
				List<String> givenNamea = new ArrayList<String>();
				for (StringType stringType : givens) {
					givenNamea.add(stringType.getValue());
				}
				d.setGiven(givenNamea);
				naemdd.add(d);
			}
			patient.setBirthdate(r.getBirthDate().toInstant());
			patient.setNames(naemdd);
			// patient.setIdentifiers(r.getIdentifier());
			patient.setGender(r.getGender().name());
			List<Address> addrsss = r.getAddress();
			List<PatientAddress> pa = new ArrayList<PatientAddress>();
			for (Address address : addrsss) {
				PatientAddress p = new PatientAddress();
				StringBuilder txt = new StringBuilder();
				List<Extension> exts = address.getExtension();

				for (Extension extension : exts) {
					List<Extension> exten = extension.getExtension();
					for (Extension extension2 : exten) {
						txt.append(extension2.getValue());
					}

				}
				p.setAddress(txt.toString());
				p.setCity(address.getCity());
				pa.add(p);
			}

			patient.setAddress(pa);

			patients.add(patient);

		}

		return patients;
	}

	// private void importResourceByPatientId(Class<? extends IBaseResource>
	// resouce) throws ParseException, UnsupportedEncodingException,
	// JSONException{

	public void importPatientInfromationById(String patientId)
			throws ParseException, UnsupportedEncodingException, JSONException {

		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Patient?_id=" + patientId).returnBundle(Bundle.class)
				.execute();
		List<BundleEntryComponent> entires = results.getEntry();

		importRemoteResouceToLocal(results, "person", "person_id");
		if (results.hasEntry()) {
			System.out.println("Got location bundle size"
					+ results.getEntry().size());
		}

		// encounter type and visit type uuid should be same get form db by name

		/*
		 * IQuery<Bundle> encounter =
		 * firFhirConfig.getOpenCRFhirContext().search() //
		 * .forResource(Encounter.class)
		 * .where(Encounter.SUBJECT.hasAnyOfIds(patientId)) .sort()
		 * .ascending("_lastUpdated") .returnBundle(Bundle.class); Bundle
		 * encounterTasksBundle = encounter.execute();
		 * importRemoteEncounterResouceToLocal(encounterTasksBundle); if
		 * (encounterTasksBundle.hasEntry()) {
		 * System.out.println("Got ENcounter bundle size" +
		 * encounterTasksBundle.getEntry().size()); }
		 * 
		 * IQuery<Bundle> observation =
		 * firFhirConfig.getOpenCRFhirContext().search()
		 * .forResource(Observation.class)
		 * .where(Encounter.SUBJECT.hasAnyOfIds(patientId)) .sort()
		 * .ascending("_lastUpdated") .returnBundle(Bundle.class); Bundle
		 * observationTasksBundle = observation.execute();
		 * importRemoteObservationToLocal(observationTasksBundle); if
		 * (observationTasksBundle.hasEntry()) {
		 * System.out.println("Got ENcounter bundle size" +
		 * observationTasksBundle.getEntry().size()); }
		 * 
		 * IQuery<Bundle> serviceRequest =
		 * firFhirConfig.getOpenCRFhirContext().search()
		 * .forResource(ServiceRequest.class)
		 * .where(ServiceRequest.SUBJECT.hasAnyOfIds(patientId)) .sort()
		 * .ascending("_lastUpdated") .returnBundle(Bundle.class); Bundle
		 * serviceRequestTasksBundle = serviceRequest.execute();
		 * importRemoteServiceRequestToLocal(serviceRequestTasksBundle);
		 * 
		 * 
		 * 
		 * IQuery<Bundle> medicationRequest =
		 * firFhirConfig.getOpenCRFhirContext().search()
		 * .forResource(MedicationRequest.class)
		 * .where(MedicationRequest.SUBJECT.hasAnyOfIds(patientId)) .sort()
		 * .ascending("_lastUpdated") .returnBundle(Bundle.class); Bundle
		 * medicationRequestRequestTasksBundle = medicationRequest.execute();
		 * importRemotePrescriptionToLocal(medicationRequestRequestTasksBundle);
		 */

	}

	private Bundle importRemoteEncounterResouceToLocal(
			Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		String table = "";
		String primaryKey = "";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			Encounter r = (Encounter) bundleEntry.getResource();
			System.err.println(r.getMeta().getTagFirstRep().getCode());
			String localOpenMRSUUID = "";
			String typeName = r.getType().get(0).getCoding().get(0)
					.getDisplay();
			System.err.println("typeName:" + typeName);
			if (r.getMeta().getTagFirstRep().getCode()
					.equalsIgnoreCase("encounter")) {
				table = "encounter";
				primaryKey = "encounter_id";
				localOpenMRSUUID = commonOperationService
						.findResourceUuidByName(table + "_type", typeName,
								"name", "uuid");

			} else if (r.getMeta().getTagFirstRep().getCode()
					.equalsIgnoreCase("visit")) {
				table = "visit";
				primaryKey = "visit_id";
				localOpenMRSUUID = commonOperationService
						.findResourceUuidByName(table + "_type", typeName,
								"name", "uuid");
			}
			System.err.println("localOpenMRSUUID::::" + localOpenMRSUUID);
			Resource resource = (Resource) bundleEntry.getResource();
			r.getType().get(0).getCoding().get(0).setCode(localOpenMRSUUID);
			// partitipant doen not support
			if (r.getParticipantFirstRep() != null) {
				r.setParticipant(null);
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
							.encodeResourceToString(r));
			Integer id = commonOperationService.findResourceIdByUuid(table,
					resource.getIdElement().getIdPart(), primaryKey);
			System.err.println("IDL::::" + id);
			if (id == null || id == 0) {
				MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext()
						.create().resource(r).execute();
				id = commonOperationService.findResourceIdByUuid(table, res
						.getId().getIdPart(), primaryKey);
				commonOperationService.updateResource(table, id, resource
						.getIdElement().getIdPart(), primaryKey);

			} else {
				firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r)
						.execute();

			}
		}
		return transactionBundle;
	}

	private Bundle importRemoteObservationToLocal(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		String table = "obs";
		String primaryKey = "obs_id";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			Observation r = (Observation) bundleEntry.getResource();

			String localOpenMRSUUID = "";

			List<Coding> codings = r.getCode().getCoding();
			List<String> codes = new ArrayList<String>();
			for (Coding coding : codings) {
				codes.add("'" + coding.getCode() + "'");
			}

			localOpenMRSUUID = commonOperationService
					.findConceptUuidByMappingCode(String.join(",", codes));
			r.getCode().getCoding().get(0).setCode(localOpenMRSUUID);
			System.err.println("localOpenMRSUUID::::" + localOpenMRSUUID);
			Resource resource = (Resource) bundleEntry.getResource();

			System.err.println("DDD>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(r));
			Integer id = commonOperationService.findResourceIdByUuid(table,
					resource.getIdElement().getIdPart(), primaryKey);
			if (id == null || id == 0) {
				MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext()
						.create().resource(r).execute();
				id = commonOperationService.findResourceIdByUuid(table, res
						.getId().getIdPart(), primaryKey);
				commonOperationService.updateResource(table, id, resource
						.getIdElement().getIdPart(), primaryKey);

			} else {
				firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r)
						.execute();

			}
		}
		return transactionBundle;
	}

	private Bundle importRemoteServiceRequestToLocal(Bundle originalTasksBundle)
			throws UnsupportedEncodingException, JSONException {
		Bundle transactionBundle = new Bundle();
		String table = "orders";
		String primaryKey = "order_id";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			ServiceRequest r = (ServiceRequest) bundleEntry.getResource();

			String localOpenMRSUUID = "";

			List<Coding> codings = r.getCode().getCoding();
			List<String> codes = new ArrayList<String>();
			for (Coding coding : codings) {
				codes.add("'" + coding.getCode() + "'");
			}
			String[] patient = r.getSubject().getReference().split("/");
			String[] encounter = r.getEncounter().getReference().split("/");
			System.err.println("JJ;" + encounter[1]);

			ServiceRequestUuids serviceRequestUuids = commonOperationService
					.findServiceRequestRelatedUuidsByEncounterUuid(encounter[1]);
			System.err.println(serviceRequestUuids);
			localOpenMRSUUID = commonOperationService
					.findConceptUuidByMappingCode(String.join(",", codes));

			r.getCode().getCoding().get(0).setCode(localOpenMRSUUID);
			System.err.println("localOpenMRSUUID::::" + localOpenMRSUUID);
			Resource resource = (Resource) bundleEntry.getResource();

			ServiceRequestDomain serviceRequestDomain = new ServiceRequestDomain();
			serviceRequestDomain.setPatient(patient[1]);
			serviceRequestDomain.setLocation(serviceRequestUuids
					.getLocationUuid());
			serviceRequestDomain.setEncounterType(serviceRequestUuids
					.getEncounterTypeUuid());
			serviceRequestDomain.setEncounterDatetime(r.getOccurrencePeriod()
					.getStart().toInstant().toString());
			serviceRequestDomain.setVisit(serviceRequestUuids.getVisitUuid());
			List<Orders> orders = new ArrayList<Orders>();
			Orders order = new Orders();
			order.setPatient(patient[1]);
			order.setConcept(localOpenMRSUUID);
			order.setEncounter(encounter[1]);
			orders.add(order);
			serviceRequestDomain.setOrders(orders);
			ObjectWriter ow = new ObjectMapper().writer()
					.withDefaultPrettyPrinter();
			String payload = "";
			try {
				payload = ow.writeValueAsString(serviceRequestDomain);
				System.err.println(payload);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.err.println("DDD>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(r));
			Integer id = commonOperationService.findResourceIdByUuid(table,
					resource.getIdElement().getIdPart(), primaryKey);
			if (id == null || id == 0) {
				String res = HttpWebClient.postWithBasicAuth(
						localOpenmrsOpenhimURL, "/ws/rest/v1/encounter",
						firFhirConfig.getOpenMRSCredentials()[0],
						firFhirConfig.getOpenMRSCredentials()[1], payload);
				String uuid = DataParse.getOrderIdFromEncounterJson(res);
				System.out.println("uuid" + uuid);
				id = commonOperationService.findResourceIdByUuid(table, uuid,
						primaryKey);
				commonOperationService.updateResource(table, id, resource
						.getIdElement().getIdPart(), primaryKey);
				commonOperationService.updateOrderEncounterId(
						serviceRequestUuids.getEncounterId(), resource
								.getIdElement().getIdPart());
			} else {
				System.err.println("found order do nothong............");
				// firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r).execute();

			}
		}
		return transactionBundle;
	}

	private Bundle importRemotePrescriptionToLocal(Bundle originalTasksBundle)
			throws UnsupportedEncodingException, JSONException {
		Bundle transactionBundle = new Bundle();

		String table = "orders";
		String primaryKey = "order_id";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			MedicationRequest r = (MedicationRequest) bundleEntry.getResource();
			String medicationId = r.getMedicationReference().getReference()
					.split("/")[1];
			System.err.println("medicationId::" + medicationId + ":"
					+ r.getId());
			IGenericClient client = firFhirConfig.getOpenCRFhirContext();
			Bundle results = client.search()
					.byUrl("Medication?_id=" + medicationId)
					.returnBundle(Bundle.class).execute();
			List<BundleEntryComponent> entires = results.getEntry();
			System.err.println("entires::" + entires.size());
			Resource resource = (Resource) bundleEntry.getResource();

			System.err.println("resource.getIdElement().getIdPart()"
					+ resource.getIdElement().getIdPart());

			if (entires.size() != 0) {

				System.err.println("entires::" + entires.size());
				List<String> codes = new ArrayList<String>();
				List<String> formCodes = new ArrayList<String>();
				List<String> routeCodes = new ArrayList<String>();
				List<String> frequencies = new ArrayList<String>();
				List<String> durationUnitCode = new ArrayList<String>();
				for (BundleEntryComponent bundleEntryComponent : entires) {
					Resource resources = (Resource) bundleEntryComponent
							.getResource();
					System.err.println("RE:::" + resources.getResourceType());
					Medication medication = (Medication) bundleEntryComponent
							.getResource();
					List<Coding> codings = medication.getCode().getCoding();
					for (Coding coding : codings) {
						codes.add("'" + coding.getCode() + "'");
					}

					List<Coding> formCcodings = medication.getForm()
							.getCoding();
					for (Coding coding : formCcodings) {
						formCodes.add("'" + coding.getCode() + "'");
					}
				}
				List<Coding> routeCcodings = r.getDosageInstructionFirstRep()
						.getRoute().getCoding();
				for (Coding coding : routeCcodings) {
					routeCodes.add("'" + coding.getCode() + "'");
				}
				List<Coding> frequencyCcodings = r
						.getDosageInstructionFirstRep().getTiming().getCode()
						.getCoding();
				for (Coding coding : frequencyCcodings) {
					frequencies.add("'" + coding.getCode() + "'");
				}
				durationUnitCode.add("'"
						+ r.getDosageInstructionFirstRep().getTiming()
								.getRepeat().getDurationUnit() + "'");

				BigDecimal doseValue = r.getDosageInstructionFirstRep()
						.getDoseAndRate().get(0).getDoseQuantity().getValue();
				r.getDosageInstructionFirstRep().getDoseAndRate().get(0)
						.getDoseQuantity().getCode();

				// importRemoteMedicationToLocal(originalTasksBundle);
				String patient = r.getSubject().getReference().split("/")[1];
				String encounter = r.getEncounter().getReference().split("/")[1];
				PrescriptionDomain prescriptionDomain = new PrescriptionDomain();
				prescriptionDomain.setPatient(patient);
				prescriptionDomain.setEncounter(encounter);
				DrugAndConceptDTO drugAndConceptDTO = commonOperationService
						.findDrugAndConceptUuidByMappingCode(String.join(",",
								codes));
				// String formDoseUnitUuid =
				// commonOperationService.findConceptUuidByMappingCode(String.join(",",
				// formCodes));
				System.err.println(routeCodes.toString());
				String routeUuid = commonOperationService
						.findConceptUuidByMappingCode(String.join(",",
								routeCodes));
				String frequencyUuid = commonOperationService
						.findFrequencyUuidByMappingCode(String.join(",",
								frequencies));
				String durationUnitUuid = commonOperationService
						.findConceptUuidByMappingCode(String.join(",",
								durationUnitCode));

				String quantityUnitUuid = commonOperationService
						.findCOnceptUuidByName(r.getDispenseRequest()
								.getQuantity().getUnit());
				String doseUnitUuid = commonOperationService
						.findCOnceptUuidByName(r.getDosageInstructionFirstRep()
								.getDoseAndRate().get(0).getDoseQuantity()
								.getUnit());
				prescriptionDomain.setDrug(drugAndConceptDTO.getDrugUuid());
				prescriptionDomain.setConcept(drugAndConceptDTO
						.getMedicineUUid());

				System.err.println("routeUuid:" + routeUuid);
				prescriptionDomain.setDose(doseValue);

				prescriptionDomain.setRoute(routeUuid);
				prescriptionDomain.setFrequency(frequencyUuid);
				prescriptionDomain.setDoseUnits(doseUnitUuid);

				prescriptionDomain.setQuantity(r.getDispenseRequest()
						.getQuantity().getValue());
				prescriptionDomain.setQuantityUnits(quantityUnitUuid);
				prescriptionDomain.setNumRefills(r.getDispenseRequest()
						.getNumberOfRepeatsAllowed());
				prescriptionDomain.setDuration(r.getDosageInstructionFirstRep()
						.getTiming().getRepeat().getDuration());

				prescriptionDomain.setDurationUnits(durationUnitUuid);
				ObjectWriter ow = new ObjectMapper().writer()
						.withDefaultPrettyPrinter();
				String payload = "";
				try {
					payload = ow.writeValueAsString(prescriptionDomain);
					System.err.println(payload);
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// System.err.println(payload);

				// System.err.println("DDD>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(r));
				Integer id = commonOperationService.findResourceIdByUuid(table,
						resource.getIdElement().getIdPart(), primaryKey);
				if (id == null || id == 0) {
					String res = HttpWebClient.postWithBasicAuth(
							localOpenmrsOpenhimURL, "/ws/rest/v1/order",
							firFhirConfig.getOpenMRSCredentials()[0],
							firFhirConfig.getOpenMRSCredentials()[1], payload);
					String uuid = DataParse.getOrderIdFromOrderJson(res);
					System.out.println("uuid" + uuid);
					id = commonOperationService.findResourceIdByUuid(table,
							uuid, primaryKey);
					commonOperationService.updateResource(table, id, resource
							.getIdElement().getIdPart(), primaryKey);

				} else {
					System.err.println("found order do nothong............");
					// firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r).execute();

				}

			}
		}
		return transactionBundle;
	}

	private Bundle importRemoteMedicationToLocal(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		String table = "obs";
		String primaryKey = "obs_id";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {

			Observation r = (Observation) bundleEntry.getResource();

			String localOpenMRSUUID = "";

			List<Coding> codings = r.getCode().getCoding();
			List<String> codes = new ArrayList<String>();
			for (Coding coding : codings) {
				codes.add("'" + coding.getCode() + "'");
			}

			localOpenMRSUUID = commonOperationService
					.findConceptUuidByMappingCode(String.join(",", codes));
			r.getCode().getCoding().get(0).setCode(localOpenMRSUUID);
			System.err.println("localOpenMRSUUID::::" + localOpenMRSUUID);
			Resource resource = (Resource) bundleEntry.getResource();

			System.err.println("DDD>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(r));
			Integer id = commonOperationService.findResourceIdByUuid(table,
					resource.getIdElement().getIdPart(), primaryKey);
			if (id == null || id == 0) {
				MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext()
						.create().resource(r).execute();
				id = commonOperationService.findResourceIdByUuid(table, res
						.getId().getIdPart(), primaryKey);
				commonOperationService.updateResource(table, id, resource
						.getIdElement().getIdPart(), primaryKey);

			} else {
				firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r)
						.execute();

			}
		}
		return transactionBundle;
	}
}
