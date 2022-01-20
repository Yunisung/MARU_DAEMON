package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.CashReceiptDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.map.SharedMap;

public class CashReceiptVaReg {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.CashReceiptVaReg.class );
	
	private SmsGw smsGw = null;
	private String msgBody = "";
	
	private String type = ""; //API - test, real
	private String mid = "";
	private String ApiURL = "";
	
	public static void main(String[] args) {
		new CashReceiptVaReg();
	}
	
	public CashReceiptVaReg() {
		logger.info("==================================================");
		logger.info("CashReceipt 가상계좌  등록 START");
		smsGw = new SmsGw();
		
		configSetting();
		cashReceiptReg();
		
		logger.info("CashReceipt 가상계좌 등록 END");
		logger.info("==================================================");
	}
	
	//현금영수증 등록 API
	public void cashReceiptReg() {
		try {
			String regURL = "https://" + ApiURL + "/pgtrans/CashReceiptMultiAction.do?_method=insertReceiptInfo";
			
			logger.info(" ----- CASH RECEIPT VA TRANSMIT START -----");
			
			CashReceiptDAO dao = new CashReceiptDAO();
		
			List<SharedMap<String,Object>> getCashReceiptList = dao.cashList();
			
			logger.info("getCashReceiptList COUNT : {}", getCashReceiptList.size());
			
			if(getCashReceiptList.size() > 0) {
				logger.info("==================================================");
				logger.info("현금영수증 가상계좌 등록 세틀뱅크 API 호출");
				
				for(SharedMap<String, Object> cashData : getCashReceiptList) {
					Map<String, Object> map = new HashMap<String, Object>();
					
					map.put("mid", cashData.getString("mid"));
					map.put("assort", cashData.getString("assort"));
					map.put("transNo", cashData.getString("transNo"));
					map.put("ordNm", cashData.getString("ordNm"));
					map.put("trDt", cashData.getString("orgTrDt"));
					map.put("taxYn", cashData.getString("taxYn"));
					map.put("amt", cashData.getString("amt"));
					map.put("vat", cashData.getString("vat"));
					map.put("svcAmt", cashData.getString("svcAmt"));
					map.put("bizRegNo", cashData.getString("bizRegNo"));
					map.put("purpose", cashData.getString("purpose"));
					map.put("identityGb", cashData.getString("identityGb"));
					map.put("identity", dao.getAESDec(cashData.getString("identity")));
					map.put("deductionType", cashData.getString("deductionType"));
					
					if("1".equals(cashData.getString("assort"))) {
						map.put("authNo", cashData.getString("orgAuthNo"));
						map.put("orgTrDt", cashData.getString("orgTrDt"));
					}
					
					logger.info("CashReceipt VA REQ : [{}][{}][{}]", cashData.getString("cashId"), cashData.getString("assort"), cashData.getString("amt"));
					
					JSONObject apiRes = new JSONObject();
					
					apiRes = exec(regURL, map);
					
					logger.info("CashReceipt VA RES : [{}][{}]", cashData.getString("cashId"), apiRes.toJSONString());
					
					String cashId = cashData.getString("cashId");
					String authNo = (String) apiRes.get("authNo");
					String resultCd = (String) apiRes.get("resultCd");
					String resultMsg = (String) apiRes.get("resultMsg");
					String stateCd = "전송성공";
					String regInfo = "DAEMON";
					String errCd = "";
					
					if(!"0000".equals(resultCd)) {
						stateCd = "전송실패";
						errCd = "9999";
					}

					dao.resultUpdate(cashId, stateCd, resultCd, resultMsg, authNo, regInfo, errCd);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			
			msgBody = "-----> 현금영수증 가상계좌 등록 오류발생. 확인요망.-----";
			smsGw.sendMessage("0", "4", msgBody);
			logger.info(msgBody);
		}
	}
		
	/**
	 * 세틀뱅크 현금영수증 등록 API호출
	 * @param urlAddr
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public JSONObject exec(String urlAddr, Map<String, Object> parameters) throws Exception{
		JSONObject apiRes = new JSONObject();
		JSONParser jParser = new JSONParser();
		
		StringBuilder postData = new StringBuilder();
        for(Map.Entry<String,Object> param : parameters.entrySet()) {
            if(postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");
        
        URL url = new URL(urlAddr);
        
        logger.error("urlAddr : [{}]",urlAddr);
		logger.error("postData : [{}]",postData);
        
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(30000);
		conn.setRequestMethod("POST");
		
		conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
		conn.getOutputStream().write(postDataBytes);
		
		/*
		 * OutputStreamWriter outStream = new OutputStreamWriter(conn.getOutputStream(),
		 * "UTF-8"); PrintWriter writer = new PrintWriter(outStream);
		 * writer.write(postData.toString()); writer.flush();
		 */
		
		try {
			if (conn.getResponseCode() != 200) {
				apiRes.put("resultCd", "9999");
				apiRes.put("resultMsg", "Connection Error : " +conn.getResponseCode());
				apiRes.put("authNo", "");
				apiRes.put("trTime", "");
				
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
			apiRes.put("resultCd", "9999");
			apiRes.put("resultMsg", e.getMessage());
			apiRes.put("authNo", "");
			apiRes.put("trTime", "");
			
			e.getStackTrace();
			logger.error(e.getMessage());
		}
		
		return apiRes;
	}
	
	/**
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            //프로퍼티 파일 위치
    		//운영
            String propFile = "/home/bkwinners/MARU/MARU_DAEMON/conf/cashReceipt.properties"; 
    		//테스트
    		//String propFile = "/home/MARU/MARU_DAEMON/conf/cashReceipt.properties";
    		
            // 프로퍼티 객체 생성
            Properties props = new Properties();
            
            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(propFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));
            
            // 항목 읽기
            type = props.getProperty("type");
            mid = props.getProperty("mid");
            ApiURL = props.getProperty(type + "_ApiURL");
            
            logger.info("config - type : [{}], mid : [{}], ApiURL : [{}]", type, mid, ApiURL);
            
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
