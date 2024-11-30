package org.ih.fhir.data.communication.search;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.dto.Names;
import org.ih.fhir.data.communication.dto.PatientAddress;
import org.ih.fhir.data.communication.dto.SearchPateintDTO;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientSearchService {

	@Autowired
	private FhirConfig firFhirConfig;

	public List<SearchPateintDTO> searchPatient(PatientSearchParam param)
			throws ParseException, UnsupportedEncodingException, JSONException {

		StringBuilder searchParam = new StringBuilder();
		if (param.getIdentifiers() != null) {
			searchParam.append("&identifier=" + param.getIdentifiers());
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
		String searchParamString = searchParam.substring(1);

		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl("Patient?" + searchParamString)
				.returnBundle(Bundle.class).execute();

		return generatePatient(results);
	}

	private List<SearchPateintDTO> generatePatient(Bundle originalTasksBundle) {
		List<SearchPateintDTO> patients = new ArrayList<SearchPateintDTO>();

		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			SearchPateintDTO patient = new SearchPateintDTO();

			Patient patientResource = (Patient) bundleEntry.getResource();

			List<HumanName> names = patientResource.getName();
			List<Names> theNames = generateNames(names);

			List<Identifier> identifiers = patientResource.getIdentifier();
			List<org.ih.fhir.data.communication.dto.Identifier> theIdentifiers = generateIdentifier(identifiers);
			patient.setIdentifiers(theIdentifiers);

			patient.setBirthdate(patientResource.getBirthDate().toInstant());
			patient.setNames(theNames);
			// patient.setIdentifiers(r.getIdentifier());
			patient.setGender(patientResource.getGender().name());
			List<Address> addresses = patientResource.getAddress();
			List<PatientAddress> thePatientAddresses = generateAddress(addresses);

			patient.setAddress(thePatientAddresses);

			patients.add(patient);

		}

		return patients;
	}

	private List<org.ih.fhir.data.communication.dto.Identifier> generateIdentifier(
			List<Identifier> identifiers) {
		List<org.ih.fhir.data.communication.dto.Identifier> theIdentifiers = new ArrayList<org.ih.fhir.data.communication.dto.Identifier>();

		for (Identifier identifier : identifiers) {
			org.ih.fhir.data.communication.dto.Identifier theIdentifier = new org.ih.fhir.data.communication.dto.Identifier();
			theIdentifier.setText(identifier.getType().getText());
			theIdentifier.setValue(identifier.getValue());
			theIdentifiers.add(theIdentifier);
		}
		return theIdentifiers;

	}

	private List<PatientAddress> generateAddress(List<Address> addresses) {
		List<PatientAddress> thePatientAddresses = new ArrayList<PatientAddress>();
		for (Address address : addresses) {
			PatientAddress thePatientAddress = new PatientAddress();
			StringBuilder txt = new StringBuilder();
			List<Extension> exts = address.getExtension();

			for (Extension extension : exts) {
				List<Extension> exten = extension.getExtension();
				for (Extension extension2 : exten) {
					txt.append(extension2.getValue());
				}

			}
			thePatientAddress.setAddress(txt.toString());
			thePatientAddress.setCity(address.getCity());
			thePatientAddresses.add(thePatientAddress);
		}

		return thePatientAddresses;

	}

	private List<Names> generateNames(List<HumanName> names) {
		List<Names> theNames = new ArrayList<Names>();
		for (HumanName humanName : names) {
			Names theName = new Names();
			theName.setFamily(humanName.getFamily());
			List<StringType> givens = humanName.getGiven();
			List<String> givenNamea = new ArrayList<String>();
			for (StringType stringType : givens) {
				givenNamea.add(stringType.getValue());
			}
			theName.setGiven(givenNamea);
			theNames.add(theName);
		}
		return theNames;
	}

}
