package org.ih.fhir.data.communication.repository;

import org.ih.fhir.data.communication.model.IHMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IHMarkerRepository extends JpaRepository<IHMarker, Long>{
	
	IHMarker findByName(@Param("name") String name);

}
