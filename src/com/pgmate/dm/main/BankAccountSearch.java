package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.BankAccountSearchDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.map.SharedMap;

public class BankAccountSearch {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.BankAccountSearch.class);

	private static String iv;
	private static Key keySpec;
	
	private Date currentDate    = new Date();
	private String type     = ""; // 결제타입 - test, real
	private String tokenApiURL = "";
	private String searchApiURL = "";
	private String clientId     = ""; // 복호화된 파일 내용안의 client_id 값을 입력해주세요.
	private String clientSecret = ""; // 복호화된 파일 내용안의 client_secret 값을 입력해주세요.
	private String encKey       = ""; // 복호화된 파일 내용안의 encKey 값을 입력해주세요.
	private String entrCd = ""; //기관코드
	private String appKey = ""; //appKey
	private String startDate = "";
	private String endDate = "";
	private String authorization = "";
	private SmsGw smsGw = null;
	
	private long unixTime = 0;
	
	private String day = "";
	
	public static void main(String[] args) {
        new BankAccountSearch();
	}
	
	public BankAccountSearch() {
		smsGw = new SmsGw();
		hanaBankSerash();
	}
	
	private void hanaBankSerash() {
		try {
			logger.info("HanaBank AccountSearchTest Strart");
			
			configSetting();

			day = startDate.substring(0,4) + "년 " + startDate.substring(4,6) + "월 " + startDate.substring(6,8) + "일";
			
			unixTime = currentDate.getTime() / 1000;
			authorization = clientId + ":" + clientSecret + ":" + unixTime;

			authorization = "Basic " + encrypt(encKey, authorization);

		    String encAccessToken = getToken(authorization);
		    
		    if(!"".equals(stringIsNull(encAccessToken))) {
		    	String decAccessToken = decryptAccessToken(encKey, encAccessToken);
		    	
		    	if(decAccessToken.contains(":")) {
		    		String token[] = decAccessToken.split(":");
		    		decAccessToken = token[0];
		    	}
		    	
			    logger.info("decAccessToken : " + decAccessToken);
			    
			    unixTime = currentDate.getTime() / 1000;
			    authorization = decAccessToken + ":" + unixTime + ":" + clientId;
			    logger.info("authorization : " + authorization);
			    
			    String encAuth = "bearer " + encrypt(encKey, authorization);
			    
			    accountSearch(encAuth);
		    }else {
		    	logger.info("encAccessToken is null");
		    }

		    logger.info("HanaBank AccountSearchTest End");
		} catch(Exception e) {
			String msgBody = day + " 하나은행 비대면 계좌개설 조회 오류. 확인요망";
			smsGw.sendMessage("0", "2", msgBody);
            
            logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 하나은행 계좌조회를 위해 필요한 AccessToken 생성
	 * @param authData
	 * @return
	 */
	private String getToken(String authorization) {
		logger.info("----- AccessToken Create Start -----");
		
		String encAccessToken = "";
		String resCd = "";
		String resMsg = "";
		
		try {
			String senData = "grant_type=client_credentials";//보낼 데이터
			
			logger.info("AccessToken API URL : " + tokenApiURL);
			
			URL url = new URL(tokenApiURL);
			
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("Authorization", authorization); 
			con.setRequestProperty("ENTR_CD",entrCd);
			con.setConnectTimeout(10000);
			con.setReadTimeout(15000);
			con.setUseCaches(false);
			con.setDoInput(true);
			con.setDoOutput(true);
			
			OutputStream os = con.getOutputStream();
			os.write(senData.getBytes());
			os.flush();
			os.close();
			
			int responseCode = con.getResponseCode();
			BufferedReader br;
			
			logger.info("AccessToken responseCode : " + responseCode);
			
			StringBuffer sb = new StringBuffer();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
			    br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
	            String line;
	            
	            while ((line = br.readLine()) != null) {
	                sb.append(line).append("\n");
	            }
	            br.close();
	            
	            JSONParser jsonParser = new JSONParser();   
				Map responseData = (Map) jsonParser.parse(sb.toString());
				logger.info("RecvToken DataHeader : " +  responseData.get("dataHeader"));
				logger.info("RecvToken DataBody : " +  responseData.get("dataBody"));
				
				Map responseHeader = (Map) jsonParser.parse(responseData.get("dataHeader").toString());
				resCd = responseHeader.get("GW_RSLT_CD").toString();
				resMsg = responseHeader.get("GW_RSLT_MSG").toString();
				
				if("1200".equals(resCd)) {
					Map responseBody = (Map) jsonParser.parse(responseData.get("dataBody").toString());
					encAccessToken = responseBody.get("access_token").toString();
					logger.info("RecvToken : " +  encAccessToken);
				}else{
					logger.info("Token Data Error : " +  resCd + "/" + resMsg);
				}
			} 
			else 
			{  
				logger.info("sendToken Connection Error : " + con.getResponseMessage());
			}
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
	    }
		
		logger.info("----- AccessToken Create End -----");
		return encAccessToken;
	}
	
	/**
	 * 하나은행에 비대면 대설계좌 개설정보 조회
	 * @param encAuth
	 */
	private void accountSearch(String encAuth) {
		logger.info("----- HanaBank accountSearch " + type + " Start -----");
		String name = "";
		String birthDay = "";
		String mobileNo = "";
		String accntNo = "";
		String resCd = "";
		String resMsg = "";
		int insertCnt = 0;
		
		JSONArray bankAccntArr = null;
		BankAccountSearchDAO dao = new BankAccountSearchDAO();
		DecimalFormat formatter = new DecimalFormat("###,###");
		
	    // 실제 API 호출
	    try {
	    	logger.info("API URL : " + searchApiURL);
	    	logger.info("API ENTR_CD : " + entrCd);
	    	logger.info("API APP_KEY : " + appKey);
	    	
	        URL url = new URL(searchApiURL);
	        HttpURLConnection con = (HttpURLConnection)url.openConnection();
	        con.setRequestMethod("POST");
	        con.setConnectTimeout(30000); // Connection Timeout 설정
	        con.setReadTimeout(30000); // Read Timeout 설정
	        con.setDoOutput(true); // OutPutStream 사용
	        con.setDoInput(true); // Input Stream 사용
	        con.setRequestProperty("Accept", "application/json");
	        con.setRequestProperty("Content-Type","application/json;charset=UTF-8");
	        con.setRequestProperty("Authorization",encAuth);
	        con.setRequestProperty("ENTR_CD",entrCd);
	        con.setRequestProperty("APP_KEY",appKey);

	        JSONObject jsonData = new JSONObject();
	        JSONObject jsonHeader = new JSONObject();
	        JSONObject jsonBody = new JSONObject();

	        jsonHeader.put("CLNT_IP_ADDR", "203.245.13.44"); //ip
	        jsonHeader.put("CNTY_CD", "kr"); //국가코드 
	        jsonHeader.put("ENTR_CD", entrCd);//업체코드
	        
	        if("test".equals(type)) {
	        	jsonHeader.put("DEV_CD", "T");//개발서버 요청시 "T"세팅
	        }
	        
	    	jsonData.put("dataHeader", jsonHeader);
	    	
	    	jsonBody.put("nftf_trsc_chnl_dtls_cd", "132"); //비대면거래채널상세코드 (KCP에서 부여한 광원 코드)
	    	jsonBody.put("inq_str_dt", startDate); //조회시작일자
	    	jsonBody.put("inq_end_dt", endDate); //조회종료일자
	    	jsonBody.put("page_no", "0"); //페이지번호 : 0-페이지번호 조회시, 1-페이지번호로 처리
		    jsonBody.put("page_pr_proc_ncnt", "0"); //페이지당처리건수
		    jsonData.put("dataBody", jsonBody);
	        
		    logger.info("API JWON DATA : " + jsonData.toJSONString());
		    
		    OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
	        wr.write(jsonData.toJSONString());
	        wr.flush();

	        StringBuilder sb = new StringBuilder();
	        if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
	            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
	            String line;
	            while ((line = br.readLine()) != null) {
	                sb.append(line).append("\n");
	            }
	            br.close();

	            JSONParser parser = new JSONParser();
	            Object obj = parser.parse(sb.toString());
	            JSONObject jsonObj = (JSONObject)obj;
				
	            Object headerObj = jsonObj.get("dataHeader");
	            JSONObject headerJsonObj = (JSONObject)headerObj;
	            
	            Object bodyObj = jsonObj.get("dataBody");
	            JSONObject bodyJsonObj = (JSONObject)bodyObj;
	            
	            logger.info("accountSearch DataHeader : " +  headerJsonObj.toString());
				logger.info("accountSearch DataBody : " +  bodyJsonObj.toString());
				
	            resCd = headerJsonObj.get("GW_RSLT_CD").toString();
	            resMsg = headerJsonObj.get("GW_RSLT_MSG").toString();
	            
	            if("1200".equals(resCd)) {
		            bankAccntArr = (JSONArray)bodyJsonObj.get("rept_ncnt");
		            
		            logger.info("HanaBank Accnt Count : " + bankAccntArr.size());
		            List<SharedMap<String, Object>> list = new ArrayList<SharedMap<String,Object>>();
		            
		            if(bankAccntArr.size() > 0) {
		            	for(int i=0;i<bankAccntArr.size();i++){
		            		SharedMap<String, Object> map = new SharedMap<String, Object>();
							JSONObject returnSubject = (JSONObject) bankAccntArr.get(i);
							
							name = returnSubject.get("cust_nm").toString();
							birthDay = returnSubject.get("cust_btdy").toString();
							mobileNo = returnSubject.get("mbph_no").toString();
							accntNo = returnSubject.get("acct_no").toString();
							
							map.put("name", name);
							map.put("birthDay", birthDay);
							map.put("mobileNo", mobileNo);
							map.put("accntNo", accntNo);
							
							logger.info("고객명 : " + name);
							logger.info("생년월일 : " + birthDay);
							logger.info("휴대전화번호 : " + mobileNo);
							logger.info("계좌번호 : " + accntNo);
							logger.info("-----------------------------------");
							
							list.add(map);
						}
		            	
		            	insertCnt = dao.insertBankData(list);
		            	logger.info("accountSearch Count : " + bankAccntArr.size() + "/" + insertCnt + "건");
		            }else {
		            	logger.info("bankAccntArr No Data!!");
		            }
		            
					String msgBody = day + " 하나은행 비대면 계좌개설 수는 " + formatter.format(bankAccntArr.size()) + "건 입니다.";
					
					smsGw.sendMessage("0", "2", msgBody);
	            }else {
	            	logger.info("accountSearch Data Error : " +  resCd + "/" + resMsg);
	            }
	        } else {
	        	logger.info("accountSearch Connection Error : " + con.getResponseMessage() + ":" + con.getResponseCode());
	        }
	    } catch (Exception e) {
	    	logger.info(e.getMessage(), e);
	    }
	    logger.info("----- HanaBank accountSearch End -----");
	}
	
    /**
     * 해당 키값으로 문자열 암호화
     * @param key
     * @param str
     * @return
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    public String encrypt(String key, String str) throws NoSuchAlgorithmException,
    	GeneralSecurityException, UnsupportedEncodingException {
		iv = key.substring(0, 16);
		byte[] keyBytes = new byte[16];
		byte[] b = key.getBytes("UTF-8");
		
		int len = b.length;
		if (len > keyBytes.length) {
		    len = keyBytes.length;
		}
		System.arraycopy(b, 0, keyBytes, 0, len);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv.getBytes()));
		byte[] encrypted = c.doFinal(str.getBytes("UTF-8"));
		String enStr = new String(Base64.getEncoder().encode(encrypted));

		return enStr;
	}
    
    /**
     * accessToken 복호화
     * @param key 복호화된 파일 내용안의 encKey 값
     * @param str 발급받은 암호화된 accessToken값
     * @return
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    public String decryptAccessToken(String key, String str) throws NoSuchAlgorithmException,
    GeneralSecurityException, UnsupportedEncodingException {
		iv = key.substring(0, 16);
		
		byte[] keyBytes = new byte[16];
		byte[] b = key.getBytes("UTF-8");
		int len = b.length;
		
		if (len > keyBytes.length) {
		    len = keyBytes.length;
		}
		System.arraycopy(b, 0, keyBytes, 0, len);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv.getBytes()));
		
		byte[] byteStr = Base64.getDecoder().decode(str.getBytes());
		
		return new String(c.doFinal(byteStr), "UTF-8");
	}
    
    /**
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            // 프로퍼티 파일 위치
            String propFile = "/home/MARU/MARU_DAEMON/conf/bankconfig.properties";

            // 프로퍼티 객체 생성
            Properties props = new Properties();

            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(propFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));
            
            // 항목 읽기
            type = props.getProperty("type") ;
           
            tokenApiURL = props.getProperty(type + "_tokenApiURL");
            searchApiURL = props.getProperty(type + "_searchApiURL");
            clientId = props.getProperty(type + "_clientId");
            clientSecret = props.getProperty(type + "_clientSecret");
            encKey = props.getProperty(type + "_encKey");
            entrCd = props.getProperty(type + "_entrCd");
            appKey = props.getProperty(type + "_appKey");
            startDate = props.getProperty("startDate");
            endDate = props.getProperty("endDate");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Calendar c1 = Calendar.getInstance();
            c1.add(Calendar.DATE, -1);
            String strToday = sdf.format(c1.getTime()); 

            if("".equals(stringIsNull(startDate))){
            	startDate = strToday;
            }
            
            if("".equals(stringIsNull(endDate))){
            	endDate = strToday;
            }
            
            logger.info("startDate [" + startDate + "],  endDate [" + endDate + "]");
            logger.info("config Data : clientId [" + clientId + "], clientSecret [" + clientSecret + "], encKey [" + encKey + "], entrCd [" + entrCd + "], appKey [" + appKey + "]");
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
   
    private String stringIsNull(String str) {
    	String data = "";
    	
    	if(str == null) {
    		data = ""; 
    	}else {
    		data = str;
    	}
    	
    	return data;
    }
}
