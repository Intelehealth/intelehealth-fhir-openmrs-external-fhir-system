package org.ih.fhir.data.communication.service;

import org.ih.fhir.data.communication.model.IHMarker;
import org.ih.fhir.data.communication.repository.IHMarkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IHMarkerService {
	
	@Autowired
	private IHMarkerRepository ihRepository;
	
	public IHMarker save(IHMarker ihMarker){
		return ihRepository.save(ihMarker);
	}
	
	public IHMarker findByName(String name){
		
		IHMarker  marker= ihRepository.findByName(name);
		
		if(marker==null){
			marker = new IHMarker();
			marker.setName(name);
			marker.setLastSyncTime("2023-08-03T06:28:43Z");
			save(marker);
		}
		return marker;
	}
	
	

}
