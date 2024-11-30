package org.ih.fhir.data.communication.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.ih.fhir.data.communication.config.DBConfig;
import org.ih.fhir.data.communication.domain.Appointment;
import org.ih.fhir.data.communication.mapper.AppointmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

	@Autowired
	private DBConfig dbConfig;

	public List<Appointment> getAllAppointment(String lastupdated) {
		JdbcTemplate template = dbConfig.template();
		List<Appointment> appointments = template.query(
				"select * from mindmap_server.appointments  order by id desc limit 1",
				new AppointmentMapper());
		
		return appointments;
	}
	
	public Appointment create(Appointment appointment) throws ParseException {
		System.err.println("fffL:"+appointment.getSlotJsDate());
		String slotDate= getSlotDate(appointment);
		String INSERT_SQL = "INSERT INTO appointments(slotDate,slotDuration,slotDurationUnit,slotTime,speciality,userUuid,drName,locationUuid,patientName,openMrsId,patientId,status,slotJsDate,patientAge,patientGender,type,uuid,createdAt,updatedAt) "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";
		KeyHolder holder = new GeneratedKeyHolder();
		dbConfig.template().update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, slotDate);
				ps.setInt(2, appointment.getSlotDuration());
				ps.setString(3, "Minute");
				ps.setString(4, appointment.getSlotTime());
				ps.setString(5, appointment.getSpeciality());
				ps.setString(6, appointment.getUserUuid());
				ps.setString(7, appointment.getDrName());
				ps.setString(8, appointment.getLocationUuid());				
				ps.setString(9, appointment.getPatientName());
				ps.setString(10, appointment.getOpenMrsId());
				ps.setString(11, appointment.getPatientId());
				ps.setString(12, appointment.getStatus().toLowerCase());
				ps.setString(13, appointment.getSlotJsDate());
				ps.setString(14, appointment.getAge());
				ps.setString(15, appointment.getGender());
				ps.setString(16, "appointment");
				ps.setString(17, appointment.getUuid());
				//ps.setDate(2, (java.sql.Date) new Date());
				//ps.setDate(3, (java.sql.Date) new Date());
				/*ps.setString(2, user.getAddress());
				ps.setString(3, user.getEmail());*/
				return ps;
			}
		}, holder);

		Integer newUserId = holder.getKey().intValue();
		appointment.setId(newUserId.longValue());
		return appointment;
	}

	private String getSlotDate(Appointment appointment) throws ParseException{
		SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat targetFormat = new SimpleDateFormat(
				"dd/MM/yyyy");
		Date date = format.parse(appointment.getSlotJsDate());
		return targetFormat.format(date);
	}
}
