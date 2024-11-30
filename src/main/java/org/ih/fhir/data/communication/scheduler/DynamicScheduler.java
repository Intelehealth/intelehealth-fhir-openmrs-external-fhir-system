/*
 * package org.ih.fhir.data.communication.scheduler;
 * 
 * import java.util.List; import java.util.Map; import
 * java.util.concurrent.ScheduledFuture;
 * 
 * import javax.annotation.PostConstruct;
 * 
 * import org.ih.fhir.data.communication.config.DBConfig; import
 * org.ih.fhir.data.communication.domain.Appointment; import
 * org.ih.fhir.data.communication.mapper.AppointmentMapper; import
 * org.ih.fhir.data.communication.service.ScheduleConfigService; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.jdbc.core.JdbcTemplate; import
 * org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler; import
 * org.springframework.stereotype.Component;
 * 
 * @Component public class DynamicScheduler {
 * 
 * @Autowired private ScheduleConfigService configService;
 * 
 * @Autowired private DBConfig dbConfig;
 * 
 * @Autowired private ThreadPoolTaskScheduler taskScheduler;
 * 
 * private ScheduledFuture<?> scheduledFuture;
 * 
 * @PostConstruct public void scheduleFixedRateTask() { long fixedRate =
 * configService.getFixedRate(); System.out.println("fixedRate" + fixedRate);
 * 
 * scheduledFuture = taskScheduler.scheduleAtFixedRate(this::task, fixedRate);
 * 
 * }
 * 
 * private void task() { // Task implementation here JdbcTemplate template =
 * dbConfig.template(); List<Map<String, Object>> result = template
 * .queryForList("select * from mindmap_server.appointments");
 * System.err.println("template::" + template); String row = "";
 * List<Appointment> appointments = template.query(
 * "select * from mindmap_server.appointments", new AppointmentMapper());
 * 
 * for (Map<String, Object> map : result) {
 * 
 * for (Map.Entry<String, Object> entry : map.entrySet()) { String key =
 * entry.getKey(); Object value = entry.getValue(); System.err.println(key + ":"
 * + value); }
 * 
 * }
 * 
 * }
 * 
 * public void reschedule() { if (scheduledFuture != null) {
 * scheduledFuture.cancel(false); } long newFixedRate =
 * configService.getFixedRate(); scheduledFuture =
 * taskScheduler.scheduleAtFixedRate(this::task, newFixedRate); }
 * 
 * }
 */