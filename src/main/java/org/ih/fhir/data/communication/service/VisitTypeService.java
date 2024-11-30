package org.ih.fhir.data.communication.service;

import java.io.UnsupportedEncodingException;

import org.ih.fhir.data.communication.config.FhirConfig;
import org.ih.fhir.data.communication.utils.HttpWebClient;
import org.ih.fhir.data.communication.utils.IHConstant;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VisitTypeService extends IHConstant{
	@Autowired
	private FhirConfig firFhirConfig;
	
	public String saveVisitType(String name,String uuid,String type) throws JSONException, UnsupportedEncodingException{
		JSONObject visitType= new JSONObject();
		visitType.put("name", name);
		visitType.put("uuid", uuid);
		visitType.put("description", name);
		
		String res = HttpWebClient.postWithBasicAuth(localOpenmrsOpenhimURL,
				"/ws/rest/v1/"+type,
				firFhirConfig.getOpenMRSCredentials()[0],
				firFhirConfig.getOpenMRSCredentials()[1],visitType.toString());
		return uuid;
	}
	
	public void saveResource(String data,String type) throws JSONException, UnsupportedEncodingException{
		
		
		String res = HttpWebClient.postWithBasicAuth(localOpenmrsOpenhimURL,
				"/ws/rest/v1/"+type,
				firFhirConfig.getOpenMRSCredentials()[0],
				firFhirConfig.getOpenMRSCredentials()[1],data.toString());
		
	}

}
