package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.EformStatusDAO;
import com.pgmate.lib.util.map.SharedMap;

public class EformStatus {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.EformStatus.class);
	
	public static void main(String[] args) {
		new EformStatus();
	}
	
	public EformStatus() {
		logger.info("==================================================");
		logger.info("EformStatus UPDATE START");
		
		EformStatusUpdate();
		
		logger.info("EformStatus UPDATE END");
		logger.info("==================================================");
	}
	
	//이폼 계약서 상세 API 호출하여 계약서 상태를 UPDATE 한다.
	public void EformStatusUpdate() {
		try {
			logger.info(" ----- EFORM STATUS UPDATE START -----");
			
			String token = eform_token();
			
			EformStatusDAO dao = new EformStatusDAO();
			JSONObject apiRes = new JSONObject();
		
			List<SharedMap<String,Object>> getReceiverMetaId = dao.getReceiverMetaId();
			
			logger.info("getReceiverMetaId COUNT : {}", getReceiverMetaId.size());
			
			if(getReceiverMetaId.size() > 0) {
				logger.info("==================================================");
				logger.info("전자계약서 계약서 상세 API 호출");
				
				for(SharedMap<String, Object> data : getReceiverMetaId) {
					
					String receiver_meta_id = data.getString("receiver_meta_id");
					
					String apiUrl = "https://api.eform.io/v2/doc/history?receiver_meta_id=" + receiver_meta_id;
					
					logger.info("EFORM STATUS UPDATE REQ : [{}][{}][{}]", receiver_meta_id, data.getString("doc_name"), data.getString("regDate"));
					
					apiRes = exec(apiUrl, "", token);
					
					if(!apiRes.isEmpty()) {
						
						String doc_status = "";
						String doc_date = "";
						
						JSONObject tmp = new JSONObject();
						JSONArray jArr = (JSONArray) apiRes.get("result");
						
						for (int i = 0; i < jArr.size(); i++) {
							tmp = (JSONObject) jArr.get(i);
							
							doc_status = (String) tmp.get("status");
							
							if("WS".equals(doc_status)) {
								doc_date = (String) tmp.get("created_date");
								
								dao.update(doc_status, doc_date, receiver_meta_id);
							} else {
								dao.update(doc_status, receiver_meta_id);
							}
						}
						
						logger.info("EFORM STATUS UPDATE RES : [{}][{}][{}][{}]", doc_date, doc_status, data.getString("doc_name"), data.getString("regDate"));
						
					} else {
						logger.info("-----> 전자계약서 상태 업데이트 결과 없음.-----");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			logger.info("-----> 전자계약서 상태 업데이트 오류발생. 확인요망.-----");
		}
	}
		
	/*
	 * 발급받은 id, key를 이용해서 액세스 토큰 발급 (10분간 유효)
	 */
	public String eform_token() throws Exception {
		String apiUrl = "https://api.eform.io/v2/token";
		String apiId = "JllVqkuXrn";

		JSONObject resJson = exec(apiUrl, apiId, "");

		String token = (String) resJson.get("access_token");

		return token;
	}
	
	/**
	 * 이폼 계약서 상세 API 호출
	 * @param urlAddr
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	public JSONObject exec(String apiUrl, String apiId, String token) throws Exception{
		JSONObject apiRes = new JSONObject();
		JSONParser jParser = new JSONParser();
		
        URL url = new URL(apiUrl);
        logger.error("apiUrl : [{}]",apiUrl);
        
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("x-api-key", "DjRldbyBu5hdRlTFYcC9cNqJVBQJAzAI84rnxN1a");
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(30000);
		conn.setRequestMethod("GET");
		
		if (apiId != null && !"".equals(apiId)) {
			conn.setRequestProperty("x-api-id", apiId);
		}

		if (token != null) {
			conn.setRequestProperty("x-access-token", token);
		}
		
		try {
			if (conn.getResponseCode() != 200) {
				logger.error("exec Connection error : " + conn.getResponseCode());
			}else {
				InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
	            BufferedReader reader = new BufferedReader(tmp);
	            StringBuilder builder = new StringBuilder();
	            String str;
	            while ((str = reader.readLine()) != null) {
	                builder.append(str + "\n");
	            }
	            String response = builder.toString();

				apiRes = (JSONObject) jParser.parse(response);				
			}
		} catch (Exception e) {

			e.getStackTrace();
			logger.error(e.getMessage());
		}
		return apiRes;
	}
}
