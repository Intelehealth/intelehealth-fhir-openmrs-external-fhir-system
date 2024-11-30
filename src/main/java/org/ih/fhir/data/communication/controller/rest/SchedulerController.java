/*
 * package org.ih.fhir.data.communication.controller.rest;
 * 
 * import org.ih.fhir.data.communication.scheduler.DynamicScheduler; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.web.bind.annotation.PostMapping; import
 * org.springframework.web.bind.annotation.RestController;
 * 
 * @RestController public class SchedulerController {
 * 
 * @Autowired private DynamicScheduler dynamicScheduler;
 * 
 * @PostMapping("/reschedule") public String reschedule() {
 * dynamicScheduler.reschedule(); return "Rescheduled with new fixed rate"; }
 * 
 * }
 */