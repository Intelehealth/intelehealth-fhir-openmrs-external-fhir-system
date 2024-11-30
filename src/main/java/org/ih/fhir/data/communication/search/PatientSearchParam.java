package org.ih.fhir.data.communication.search;

public class PatientSearchParam {
	private String identifiers;
	private String family;
	private String given;
	private String birthdate;
	private String gender;
	private String phone;
	private String id;
	private String patient;

	public String getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(String identifiers) {
		this.identifiers = identifiers;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public String getGiven() {
		return given;
	}

	public void setGiven(String given) {
		this.given = given;
	}

	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPatient() {
		return patient;
	}

	public void setPatient(String patient) {
		this.patient = patient;
	}

	@Override
	public String toString() {
		return "PatientSearchParam [identifiers=" + identifiers + ", family="
				+ family + ", given=" + given + ", birthdate=" + birthdate
				+ ", gender=" + gender + ", phone=" + phone + "]";
	}

}
