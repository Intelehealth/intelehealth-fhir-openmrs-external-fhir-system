package org.ih.fhir.data.communication.dataimports;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.dto.Person;
import org.ih.fhir.data.communication.service.AppointmentService;
import org.ih.fhir.data.communication.service.CommonOperationService;
import org.ih.fhir.data.communication.service.IHMarkerService;
import org.ih.fhir.data.communication.service.VisitTypeService;
import org.ih.fhir.data.communication.utils.AppointmentStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

@Service
public class ImportAppointment {
	@Autowired
	private IHMarkerService ihMarkerService;
	FhirContext fhirContext = FhirContext.forR4();
	@Autowired
	private FhirConfig firFhirConfig;
	@Autowired
	private CommonOperationService commonOperationService;
	@Autowired
	private AppointmentService appointmentService;
	@Autowired
	private VisitTypeService visitTypeService;
	public  void importAppointment() throws ParseException{
		
		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Appointment?patient=fa2563dd-963e-4976-bd13-894a7889f9a0" ).returnBundle(Bundle.class)
				.execute();
		System.err.println("App"+results);
		System.err.println("DDD>>>>>>>>"
				+ fhirContext.newJsonParser().setPrettyPrint(true)
						.encodeResourceToString(results));
		for (BundleEntryComponent bundleEntry : results.getEntry()) {			
			Appointment appointment = (Appointment) bundleEntry.getResource();
			Resource resource = (Resource) bundleEntry.getResource();
			org.ih.fhir.data.communication.domain.Appointment theAppoinment=new org.ih.fhir.data.communication.domain.Appointment();
			
			System.err.println("App");
			Date startdate  = appointment.getStart();
			Date endDate = appointment.getEnd();
			Integer hour = startdate.getHours();
			String slotTime = "";
			if(hour>12){
				slotTime =String.format("%02d", hour) +":"+String.format("%02d", startdate.getMinutes()) +" PM";
			}else{
				slotTime =String.format("%02d", hour) +":"+String.format("%02d", startdate.getMinutes()) +" AM";
			}
			theAppoinment.setSlotTime(slotTime);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			long diff = endDate.getTime() - startdate.getTime();
			Long minutes = TimeUnit.MILLISECONDS.toMinutes(diff); 
					System.err.println("minutes:::"+minutes);
					theAppoinment.setSlotDuration(minutes.intValue());
			List<CodeableConcept> speciality = appointment.getSpecialty();
			String sp = speciality.get(0).getCodingFirstRep().getDisplay();
			
			
			theAppoinment.setUuid(appointment.getIdElement().getIdPart());
			
			String dd = format.format(startdate);
			
			
			theAppoinment.setSlotJsDate(dd);
			List<AppointmentParticipantComponent> participants = appointment.getParticipant();
			for (AppointmentParticipantComponent appointmentParticipantComponent : participants) {
				
				String actorId = appointmentParticipantComponent.getActor().getReferenceElement().getIdPart();
				String actor = appointmentParticipantComponent.getActor().getReference();
				if(actor.contains("Location")){
					
					System.err.println("Location"+actorId);
					theAppoinment.setLocationUuid(actorId);
				}else if(actor.contains("Patient")){
					System.err.println("Patient"+actorId);
					theAppoinment.setPatientId(actorId);
					theAppoinment.setPatientName(appointmentParticipantComponent.getActor().getDisplay());
				}
				else if(actor.contains("Practitioner")){
					theAppoinment.setUserUuid(actorId);
					System.err.println("Practitioner"+actorId);
					theAppoinment.setDrName(appointmentParticipantComponent.getActor().getDisplay());
				}
				System.err.println(appointmentParticipantComponent.getActor().getReference());
			}
			
			//Person person = commonOperationService.getPatientInformation(theAppoinment.getPatientId().toLowerCase());
			Person person = commonOperationService.getPatientInformation("2ad05326-2787-47c0-a552-f03214f449d5");
			theAppoinment.setStatus(appointment.getStatus().name());
			theAppoinment.setSpeciality(sp);
			//theAppoinment.setSlotDate(startdate);
			theAppoinment.setSlotDuration(appointment.getMinutesDuration());
			System.err.println(theAppoinment.toString());
			theAppoinment.setAge(person.getAge());
			theAppoinment.setGender(person.getGender());
			appointmentService.create(theAppoinment);
			System.err.println("speciality"+sp);
			
		}
		
		
		
	}
	
	
public  void importAppointmentExternal() throws ParseException, JSONException, UnsupportedEncodingException{
		
		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Appointment?patient=fa2563dd-963e-4976-bd13-894a7889f9a0" ).returnBundle(Bundle.class)
				.execute();
		System.err.println("App"+results);
		System.err.println("DDD>>>>>>>>"
				+ fhirContext.newJsonParser().setPrettyPrint(true)
						.encodeResourceToString(results));
		for (BundleEntryComponent bundleEntry : results.getEntry()) {			
			Appointment appointment = (Appointment) bundleEntry.getResource();
			Resource resource = (Resource) bundleEntry.getResource();
			org.ih.fhir.data.communication.domain.Appointment theAppoinment=new org.ih.fhir.data.communication.domain.Appointment();
			JSONObject appointmentPaload = new JSONObject();
			
			
			List<AppointmentParticipantComponent> participants = appointment.getParticipant();
			for (AppointmentParticipantComponent appointmentParticipantComponent : participants) {
				
				String actorId = appointmentParticipantComponent.getActor().getReferenceElement().getIdPart();
				String actor = appointmentParticipantComponent.getActor().getReference();
				if(actor.contains("Location")){
					
					System.err.println("Location"+actorId);
					theAppoinment.setLocationUuid(actorId);
				}else if(actor.contains("Patient")){
					System.err.println("Patient"+actorId);
					theAppoinment.setPatientId(actorId);
					theAppoinment.setPatientName(appointmentParticipantComponent.getActor().getDisplay());
				}
				else if(actor.contains("Practitioner")){
					theAppoinment.setUserUuid(actorId);
					System.err.println("Practitioner"+actorId);
					theAppoinment.setDrName(appointmentParticipantComponent.getActor().getDisplay());
				}
				System.err.println(appointmentParticipantComponent.getActor().getReference());
			}
			
			//Person person = commonOperationService.getPatientInformation(theAppoinment.getPatientId().toLowerCase());
			Person person = commonOperationService.getPatientInformation("2ad05326-2787-47c0-a552-f03214f449d5");
			theAppoinment.setStatus(appointment.getStatus().name());
			
			//theAppoinment.setSlotDate(startdate);
			theAppoinment.setSlotDuration(appointment.getMinutesDuration());
			System.err.println(theAppoinment.toString());
			theAppoinment.setAge(person.getAge());
			theAppoinment.setGender(person.getGender());
			
			JSONArray providers = new JSONArray();
			JSONObject uuids = new JSONObject();
			uuids.put("uuid", "705f5791-07a7-44b8-932f-a81f3526fc98"); // define mapped provider
			providers.put(uuids);
			
			System.err.println("App:"+AppointmentStatus.valueOf(appointment.getStatus().name().toLowerCase()).label);
			appointmentPaload.put("appointmentKind", "Scheduled");
			System.err.println("appointment.getStatus().name()::"+appointment.getStatus().name());
			appointmentPaload.put("status", AppointmentStatus.valueOf(appointment.getStatus().name().toLowerCase()).label);
			appointmentPaload.put("startDateTime",  appointment.getStart().toInstant());
			appointmentPaload.put("endDateTime",  appointment.getEnd().toInstant());
			appointmentPaload.put("dateAppointmentScheduled",  resource.getMeta().getLastUpdated().toInstant());
			appointmentPaload.put("comments",  "");
			appointmentPaload.put("patientUuid",  theAppoinment.getPatientId());
			appointmentPaload.put("locationUuid",  theAppoinment.getLocationUuid());
			appointmentPaload.put("serviceUuid",  "4ec5c4fe-cfe0-48ff-9e4d-2f201078feae"); // define mapped service uuid
			appointmentPaload.put("providers",  providers);
			// appointmentPaload.put("uuid",  appointment.getIdElement().getIdPart());
			visitTypeService.saveResource(appointmentPaload.toString(), "appointment");
			
			
		}
		
		
		
	}
	
	

}
