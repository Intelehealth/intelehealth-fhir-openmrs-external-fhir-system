package org.ih.fhir.data.communication.repository;

import org.ih.fhir.data.communication.model.IHMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommonOperationRepository extends JpaRepository<IHMarker, Long>{
	
	

}
