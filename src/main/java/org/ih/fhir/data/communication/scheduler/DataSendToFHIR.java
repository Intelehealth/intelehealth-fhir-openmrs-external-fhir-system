package org.ih.fhir.data.communication.scheduler;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Appointment.ParticipantRequired;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Slot;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.domain.CompeletdVisit;
import org.ih.fhir.data.communication.domain.CompletedRecord;
import org.ih.fhir.data.communication.dto.LocationDTO;
import org.ih.fhir.data.communication.model.IHMarker;
import org.ih.fhir.data.communication.service.AppointmentService;
import org.ih.fhir.data.communication.service.CommonOperationService;
import org.ih.fhir.data.communication.service.IHMarkerService;
import org.ih.fhir.data.communication.utils.HttpWebClient;
import org.ih.fhir.data.communication.utils.IHConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.param.DateRangeParam;

@Component
public class DataSendToFHIR extends IHConstant {

	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private IHMarkerService ihMarkerService;
	@Autowired
	private AppointmentService appointmentService;
	@Autowired
	private CommonOperationService commonOperationService;
	@Scheduled(fixedDelay = 60000, initialDelay = 60000)
	public void scheduleTaskUsingCronExpression() throws ParseException,
			UnsupportedEncodingException, DataFormatException {
		
		
		IHMarker medicationMarker = ihMarkerService.findByName(exportMedication);
		
		List<CompletedRecord>  medications= commonOperationService.getCompletedMedication(medicationMarker.getLastSyncTime());
		System.err.println(medications.size());
		for (CompletedRecord theMedication : medications) {
			send("Medication", theMedication.getUuid());
			medicationMarker.setLastSyncTime(theMedication.getDateCreated());
		}
		ihMarkerService.save(medicationMarker);
		IHMarker marker = ihMarkerService.findByName("CompletdVisit");
		
		List<CompeletdVisit>  visits= commonOperationService.getCompletedVisit(marker.getLastSyncTime());
		List<Integer> ids = new ArrayList<Integer>();
		for (CompeletdVisit theVisit : visits) {
			// send first patient
			send("Patient", theVisit.getPatient());
			send("Encounter", theVisit.getVisit());
			
			List<CompletedRecord> encounters = commonOperationService.getCompletedEncounter(theVisit.getVisitId());
			
			for (CompletedRecord theEncounter : encounters) {
				send("Encounter", theEncounter.getUuid());
				
				List<CompletedRecord> ServiceRequests = commonOperationService.getCompletedServiceRequest(theEncounter.getId(),3);
				System.err.println("ServiceRequests:::::"+ServiceRequests.size());
				for (CompletedRecord theServiceRequest : ServiceRequests) {
					send("ServiceRequest", theServiceRequest.getUuid());
				}
				List<CompletedRecord> medicationRequestRquests = commonOperationService.getCompletedServiceRequest(theEncounter.getId(),2);
				for (CompletedRecord theMedicationRequestRquest : medicationRequestRquests) {
					send("MedicationRequest", theMedicationRequestRquest.getUuid());
				}
				
				List<CompletedRecord> obs = commonOperationService.getCompletedObs(theEncounter.getId());				
				for (CompletedRecord theObs : obs) {
					send("Observation", theObs.getUuid());
					
				}
			}
			
			
			marker.setLastSyncTime(theVisit.getDate());
			ihMarkerService.save(marker);
			System.err.println("Done............");
			
			
			
		}
		
		IHMarker diagnosticReportMarker = ihMarkerService.findByName(exportDiagnosticReport);
		
		List<CompletedRecord> diagnosticReports  =commonOperationService.getCompletedDiagnosticReport(diagnosticReportMarker.getLastSyncTime());
		System.err.println(diagnosticReports.size());
		for (CompletedRecord theDiagnosticReport : diagnosticReports) {
			send("DiagnosticReport", theDiagnosticReport.getUuid());
			diagnosticReportMarker.setLastSyncTime(theDiagnosticReport.getDateCreated());
		}
		
		
		ihMarkerService.save(diagnosticReportMarker);
	}
	
	

	

	

	private void send(String resouce,
			String uuid) throws ParseException,
			UnsupportedEncodingException, DataFormatException {
		System.err.println("resouce"+resouce);
		
		String data = HttpWebClient.get(localOpenmrsOpenhimURL, "/ws/fhir2/R4/"+resouce+"?_id=" + uuid, firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(
				Bundle.class, data);

		sendFHIRBundle(theBundle);
		if (theBundle.hasEntry()) {
			System.out.println("Got  bundle size"
					+ theBundle.getEntry().size());
		}
	}
	
	
	
	public void sendFHIRBundle(Bundle originalTasksBundle)
			throws ParseException, UnsupportedEncodingException,
			DataFormatException {

		if (originalTasksBundle.hasEntry()) {

			Bundle transactionBundle = new Bundle();
			transactionBundle.setType(Bundle.BundleType.TRANSACTION);
			for (BundleEntryComponent bundleEntry : originalTasksBundle
					.getEntry()) {
				Resource resource = (Resource) bundleEntry.getResource();
				System.err.println("resource.getMeta().getLastUpdated():::"
						+ resource.getMeta().getLastUpdated());
			/*	marker.setLastSyncTime(resource.getMeta().getLastUpdated()
						.toInstant().toString());*/
				Bundle.BundleEntryComponent component = transactionBundle
						.addEntry();
				component.setResource(resource);
				component
						.getRequest()
						.setUrl(resource.fhirType() + "/"
								+ resource.getIdElement().getIdPart())
						.setMethod(Bundle.HTTPVerb.PUT);

			}

			System.err.println("DDD>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(transactionBundle));

			String res = HttpWebClient.postWithBasicAuth(shrUrl,
					"rest/v1/bundle/save",
					firFhirConfig.getOpenMRSCredentials()[0],
					firFhirConfig.getOpenMRSCredentials()[1], fhirContext
							.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(transactionBundle)
							.toString());
			/*
			 * firFhirConfig.getOpenCRFhirContext().transaction()
			 * .withBundle(transactionBundle).execute();
			 */

			/*ihMarkerService.save(marker);*/

		}
		System.err.println("done");
	}

	

	
	private void createAppointmentResource() throws ParseException,
			UnsupportedEncodingException, DataFormatException {
		List<org.ih.fhir.data.communication.domain.Appointment> appointments = appointmentService
				.getAllAppointment("");

		for (org.ih.fhir.data.communication.domain.Appointment theAppointment : appointments) {
			System.err.println("theAppointment.getLocationUuid()::"+theAppointment.getLocationUuid());
			LocationDTO location = commonOperationService.findLocationdByUuid(theAppointment.getLocationUuid());
			Appointment appointment = new Appointment();
			appointment.setId(UUID.randomUUID().toString());
			appointment.setStatus(AppointmentStatus.BOOKED);

			List<CodeableConcept> theServiceCategory = generateCode("gp",
					"http://example.org/service-category", "General Practice");
			appointment.setServiceCategory(theServiceCategory);

			List<CodeableConcept> theServiceType = generateCode("52", "",
					"General Discussion");
			appointment.setServiceType(theServiceType);

			List<CodeableConcept> theSpecialty = generateCode(
					theAppointment.getSpeciality(), "http://snomed.info/sct",
					theAppointment.getSpeciality());
			appointment.setSpecialty(theSpecialty);

			List<CodeableConcept> theAppointmentType = generateCode("FOLLOWUP",
					"http://terminology.hl7.org/CodeSystem/v2-0276",
					"A follow up visit from a previous appointment");
			appointment.setAppointmentType(theAppointmentType.get(0));

		
			appointment.setMinutesDuration(theAppointment.getSlotDuration());
			List<AppointmentParticipantComponent> theParticipant = new ArrayList<Appointment.AppointmentParticipantComponent>();

			AppointmentParticipantComponent theParticipantPatient = generateParticipant(
					"Patient/fa2563dd-963e-4976-bd13-894a7889f9a0" ,
					theAppointment.getPatientName(), "Patient", "", "");
			theParticipant.add(theParticipantPatient);
			System.err.println("LcoationDTO:::"+location.getName());
			AppointmentParticipantComponent theParticipantLocation = generateParticipant(
					"Location/" + theAppointment.getLocationUuid(),
					location.getName(), "Location", "", "");
			
			
			theParticipant.add(theParticipantLocation);
			AppointmentParticipantComponent theParticipantPractitioner = generateParticipant(
					"Practitioner/" + theAppointment.getUserUuid(),
					theAppointment.getDrName(),
					"Practitioner",
					"http://terminology.hl7.org/CodeSystem/v3-ParticipationType",
					"ATND");

			theParticipant.add(theParticipantPractitioner);

			/*
			 * firFhirConfig.getOpenCRFhirContext().transaction()
			 * .withBundle(transactionBundle).execute();
			 */

			Slot theSlot = new Slot();
			theSlot.setId(UUID.randomUUID().toString());

			SimpleDateFormat readingFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			String appointmentDate = theAppointment.getSlotDate()+" "+theAppointment.getSlotTime();
			
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm aa");
		      //Desired format: 24 hour format: Change the pattern as per the need
		      DateFormat outputformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		      Date date = null;
		      String output = null;
		      try{
		         //Converting the input String to Date
		    	 date= df.parse(appointmentDate);
		         //Changing the format of date and storing it in String
		    	 output = outputformat.format(date);
		         //Displaying the date
		    	 System.out.println(output);
		    	 Date startDate = readingFormat.parse(theAppointment.getSlotJsDate());
		    	 Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					cal.add(Calendar.MINUTE, 30);
					theSlot.setStart(date);
					theSlot.setEnd(cal.getTime());
					
					appointment.setStart(date);
					appointment.setEnd(cal.getTime());
		      }catch(ParseException pe){
		         pe.printStackTrace();
		       }
		   
		
			SimpleDateFormat format = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			// format.setTimeZone(TimeZone.getTimeZone("DST"));

			/*try {
				Date date = readingFormat.parse(theAppointment.getSlotJsDate());
				String dd = format.format(date);
				// Date f= format.parse(dd)
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.add(Calendar.MINUTE, 30);
				System.out.println(format.format(date));

				theSlot.setStart(date);
				theSlot.setEnd(cal.getTime());
				
				appointment.setStart(date);
				appointment.setEnd(cal.getTime());
				
			} catch (ParseException e) {

				e.printStackTrace();
			}*/

			Bundle slotBundle = new Bundle();
			Bundle.BundleEntryComponent slotComponent = slotBundle.addEntry();

			slotComponent.setResource(theSlot);
			slotComponent
					.getRequest()
					.setUrl(theSlot.fhirType() + "/"
							+ theSlot.getIdElement().getIdPart())
					.setMethod(Bundle.HTTPVerb.PUT);
			slotBundle.setType(Bundle.BundleType.TRANSACTION);

			System.err.println("Slot>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(theSlot));

			List<Reference> slots = new ArrayList<Reference>();
			Reference re = new Reference();
			re.setReference("Slot/" + theSlot.getIdElement().getIdPart());

			slots.add(re);

			appointment.setSlot(slots);
			appointment.setParticipant(theParticipant);

			Bundle transactionBundle = new Bundle();
			Bundle.BundleEntryComponent component = transactionBundle
					.addEntry();

			component.setResource(appointment);
			component
					.getRequest()
					.setUrl(appointment.fhirType() + "/"
							+ appointment.getIdElement().getIdPart())
					.setMethod(Bundle.HTTPVerb.PUT);
			transactionBundle.setType(Bundle.BundleType.TRANSACTION);

			/*HttpWebClient.postWithBasicAuth(
					"http://192.168.19.152:5001/shr/", "rest/v1/bundle/save",
					firFhirConfig.getOpenMRSCredentials()[0],
					firFhirConfig.getOpenMRSCredentials()[1], fhirContext
							.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(slotBundle)
							.toString());
			
			
			String res = HttpWebClient.postWithBasicAuth(
					"http://192.168.19.152:5001/shr/", "rest/v1/bundle/save",
					firFhirConfig.getOpenMRSCredentials()[0],
					firFhirConfig.getOpenMRSCredentials()[1], fhirContext
							.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(transactionBundle)
							.toString());
			System.err.println(res);*/

			System.err.println("DDDccc>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(transactionBundle));

		}
	}

	private List<CodeableConcept> generateCode(String codeValue,
			String systemValue, String displayValue) {
		List<CodeableConcept> theServiceCategory = new ArrayList<CodeableConcept>();
		CodeableConcept cc = new CodeableConcept();
		List<Coding> theCoding = new ArrayList<Coding>();
		Coding code = new Coding();
		code.setCode(codeValue);
		if (!StringUtils.isBlank(systemValue)) {
			code.setSystem(systemValue);
		}
		code.setDisplay(displayValue);
		theCoding.add(code);
		cc.setCoding(theCoding);
		theServiceCategory.add(cc);
		return theServiceCategory;
	}

	private AppointmentParticipantComponent generateParticipant(
			String reference, String displayValue, String type, String system,
			String code) {

		AppointmentParticipantComponent apc = new AppointmentParticipantComponent();
		apc.setStatus(ParticipationStatus.ACCEPTED);
		apc.setRequired(ParticipantRequired.REQUIRED);

		Reference value = new Reference();
		value.setReference(reference);
		value.setDisplay(displayValue);
		apc.setActor(value);

		if (type.equalsIgnoreCase("Practitioner")) {

			List<CodeableConcept> theType = generateCode(code, system, "");
			apc.setType(theType);
		}
		// theParticipant.add(apc);
		return apc;
	}

}
