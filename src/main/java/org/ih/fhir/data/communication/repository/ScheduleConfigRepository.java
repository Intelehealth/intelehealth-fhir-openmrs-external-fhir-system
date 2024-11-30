package org.ih.fhir.data.communication.repository;

import org.ih.fhir.data.communication.model.ScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleConfigRepository extends JpaRepository<ScheduleConfig	, Long>{

}
