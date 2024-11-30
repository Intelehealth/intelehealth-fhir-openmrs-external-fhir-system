package org.ih.fhir.data.communication.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ih.fhir.data.communication.domain.Appointment;
import org.springframework.jdbc.core.RowMapper;

public class AppointmentMapper implements RowMapper<Appointment> {

	@Override
	public Appointment mapRow(ResultSet resultSet, int arg1)
			throws SQLException {
		Appointment appointment = new Appointment();
		appointment.setId(resultSet.getLong("id"));
		appointment.setSlotDay(resultSet.getString("slotDay"));

		appointment.setSlotDate(resultSet.getString("slotDate"));
		appointment.setSlotDuration(resultSet.getInt("slotDuration"));
		appointment
				.setSlotDurationUnit(resultSet.getString("slotDurationUnit"));
		appointment.setSlotTime(resultSet.getString("slotTime"));
		appointment.setSpeciality(resultSet.getString("speciality"));
		appointment.setUserUuid(resultSet.getString("userUuid"));
		appointment.setDrName(resultSet.getString("drName"));
		appointment.setLocationUuid(resultSet.getString("locationUuid"));
		appointment.setHwUUID(resultSet.getString("hwUUID"));
		appointment.setVisitUuid(resultSet.getString("visitUuid"));
		appointment.setPatientName(resultSet.getString("patientName"));
		appointment.setOpenMrsId(resultSet.getString("openMrsId"));
		appointment.setPatientId(resultSet.getString("patientId"));
		appointment.setStatus(resultSet.getString("status"));
		appointment.setSlotJsDate(resultSet.getString("slotJsDate"));
		appointment.setUpdatedAt(resultSet.getDate("updatedAt"));
		appointment.setUuid(resultSet.getString("uuid"));
		return appointment;
	}

}
