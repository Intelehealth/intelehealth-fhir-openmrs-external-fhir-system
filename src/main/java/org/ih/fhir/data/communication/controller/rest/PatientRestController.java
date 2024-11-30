package org.ih.fhir.data.communication.controller.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.ih.fhir.data.communication.dataimports.ImportPatientService;
import org.ih.fhir.data.communication.domain.ImportResponse;
import org.ih.fhir.data.communication.dto.SearchPateintDTO;
import org.ih.fhir.data.communication.scheduler.DataExport;
import org.ih.fhir.data.communication.search.PatientSearchParam;
import org.ih.fhir.data.communication.search.PatientSearchService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.context.FhirContext;

@RestController
@RequestMapping("/patientinfo/rest/v1/patient")
public class PatientRestController {

	@Autowired
	private ImportPatientService patientService;
	@Autowired
	private PatientSearchService patientSearchService;
	@Autowired
	private DataExport dataExport;

	@GetMapping("/import/{uuid}")
	public ResponseEntity<ImportResponse> reschedule(
			@PathVariable("uuid") String uuid)
			throws ParseException, JSONException,
			IOException {
		//dataImport.importPatientInfromationById(uuid);
		patientService.importPatient(uuid,"");
		ImportResponse res = new ImportResponse();
		res.setMessage("ok");
		res.setUuid(uuid);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}
	@GetMapping("/import/{uuid}/{locationUUid}")
	public ResponseEntity<ImportResponse> patientImport(
			@PathVariable("uuid") String uuid,@PathVariable("locationUUid") String locationUUid)
			throws ParseException, JSONException,
			IOException {
		
		Properties properties = new Properties();
       /* try {
            File file = ResourceUtils.getFile("classpath:application.properties");
            InputStream in = new FileInputStream(file);
            System.err.println(file.getAbsolutePath());
            properties.load(in);
        } catch (IOException e) {
           e.printStackTrace();
        }*/
		//dataImport.importPatientInfromationById(uuid);
		//Process p = Runtime.getRuntime().exec ("/home/proshanto/Workspace/intelehealthModule/shr/something.sh >> /home/proshanto/Workspace/intelehealthModule/shr/shr.log");
	
		patientService.importPatient(uuid,locationUUid);
		ImportResponse res = new ImportResponse();
		res.setMessage("ok");
		res.setUuid(uuid);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@PostMapping("/search")
	public ResponseEntity<List<SearchPateintDTO>> searchPatient(
			@RequestBody PatientSearchParam param)
			throws UnsupportedEncodingException, ParseException, JSONException {

		return new ResponseEntity<>(patientSearchService.searchPatient(param),
				HttpStatus.OK);
	}

	@PostMapping("/add")
	public ResponseEntity<String> postAdd(
			@RequestBody String originalTasksBundle) throws ParseException {

		FhirContext ctx = FhirContext.forR4();

		String bundleJson = "{ \"resourceType\": \"Bundle\", \"type\": \"transaction\", \"entry\": [ { \"fullUrl\": \"urn:uuid:patient-1\", \"resource\": { \"resourceType\": \"Patient\", \"id\": \"patient-1\", \"name\": [ { \"family\": \"Doe\", \"given\": [ \"John\" ] } ], \"gender\": \"male\", \"birthDate\": \"1974-12-25\" }, \"request\": { \"method\": \"POST\", \"url\": \"Patient\" } } ] }";

		Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class,
				originalTasksBundle);

		// Accessing properties
		bundle.getEntry().forEach(
				entry -> {
					System.out.println("Resource Type: "
							+ entry.getResource().getResourceType());
					System.out.println("Full URL: " + entry.getFullUrl());
				});
		System.err.println(originalTasksBundle);
		// dataExport.exportBundle(bundle, new IHMarker());
		return new ResponseEntity<>(originalTasksBundle, HttpStatus.OK);
	}

	@RequestMapping(value = "/downlaod/hpv", method = RequestMethod.GET)
	public void dicom(HttpServletRequest req, HttpServletResponse resp,
			@RequestParam(name = "file") String filename) throws IOException {

		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\""
				+ filename + "\"");

		// Then copy the stream, for example using IOUtils.copy ...
		// lookup the URL from the bits after /dl/*
		// URL url = new URL("/opt/multimedia/" + filename);
		InputStream in = new FileInputStream("/opt/multimedia/" + filename);
		// in = url.openConnection().getInputStream();
		IOUtils.copy(in, resp.getOutputStream());
		in.close();

	}

}
