package org.ih.fhir.data.communication.service;

import org.ih.fhir.data.communication.repository.ScheduleConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScheduleConfigService {

	@Autowired
    private ScheduleConfigRepository repository;

    public Long getFixedRate() {
        
        return repository.findById(1L)
                         .orElseThrow(() -> new RuntimeException("Configuration not found"))
                         .getFixedRate();
    }
}
