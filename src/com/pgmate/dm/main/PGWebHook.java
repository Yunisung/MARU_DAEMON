package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.WebHookDAO;
import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class PGWebHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.dm.main.PGWebHook.class );
	private WebHookDAO webHookDAO 	= null;
	private String trxType = "";
	private SharedMap<String,Object> sharedMap = null;
	private String retry = "";
	static {
	    disableSslVerification();
	}
	
	
	public PGWebHook(String trxType,SharedMap<String,Object> sharedMap,WebHookDAO webHookDAO,String retry) {
		this.trxType = trxType;
		this.sharedMap 	= sharedMap;
		this.webHookDAO = new WebHookDAO();	
		this.retry = retry;
	}
	
	
	public void run(){
		SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();	
		
		ntsMap.put("webHookUrl", sharedMap.getString("hookUrl"));
		ntsMap.put("hookIdx", sharedMap.getString("hookIdx"));
		
		logger.info("PGWebHooks   : {}",ntsMap.getString("webHookUrl"));
		logger.debug("van   : {}",sharedMap.getString("van"));
		ntsMap.put("trxId"		, sharedMap.getString("trxId"));
		ntsMap.put("trxType"	, trxType.toLowerCase());
		ntsMap.put("trackId"	, sharedMap.getString("trackId"));
		ntsMap.put("vanId"		, sharedMap.getString("vanId"));
		ntsMap.put("vanTrxId"	, sharedMap.getString("vanTrxId"));
		ntsMap.put("amount"		, sharedMap.getLong("amount"));
		ntsMap.put("authCd"		, sharedMap.getString("authCd"));
		ntsMap.put("trxDay"		, sharedMap.getString("trxDay"));
		ntsMap.put("retry"		, 0);
		ntsMap.put("status"		, "대기");
		ntsMap.put("payLoad"	, sharedMap.getString("payLoad"));
		ntsMap.put("regDay"		, CommonUtil.getCurrentDate("yyyyMMdd"));
		ntsMap.put("regTime"	, CommonUtil.getCurrentDate("HHmmss"));
		
		
		long time = System.currentTimeMillis();

		URL url = null;
		HttpURLConnection conn = null;
		try {
			logger.info("trxId       : {}",ntsMap.getString("trxId"));
			url = new URL(ntsMap.getString("webHookUrl"));
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/4.0");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			OutputStream os = conn.getOutputStream();
			String payload = ntsMap.getString("payLoad");
			logger.info("payLoad :: {}", payload);
			os.write(payload.getBytes("UTF-8"));
			os.flush();
			os.close();
			StringBuilder sb = new StringBuilder();
			BufferedReader in = null;
			
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String recv = "";
			while ((recv = in.readLine()) != null) {
				sb.append(recv + "\n");
			}
			in.close();
			ntsMap.put("resData", CommonUtil.cut(sb.toString(),100));
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
			ntsMap.put("code", conn.getResponseCode());
			
			if(ntsMap.getInt("code") == 200) {
				if(ntsMap.getString("resData").indexOf("OK") > -1) {
					ntsMap.put("status"		, "전송완료");
				}else {
					ntsMap.put("status"		, "전송실패");
				}
			}else {
				ntsMap.put("status"		, "전송실패");
			}
		}catch (Exception e) {
			logger.info("PG WH MERCHANT URL REQUEST ERROR =["+e.getMessage()+"]");
			ntsMap.put("status","전송실패");
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
		}finally {
			conn.disconnect();
			logger.info("PG WH MERCHANT THREAD RESPONSE : "+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
			logger.info("PG WH MERCHANT THREAD Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
			if("retry".equals(retry)){
				webHookDAO.updateTrxNTSPG(ntsMap);
			}else{
				webHookDAO.insertTrxNTSPG(ntsMap);
			}
		}
		/*
		try {
			logger.info("trxId       : {}",ntsMap.getString("trxId"));
			String contentType = "application/x-www-form-urlencoded";
			
			
			UrlClient client = new UrlClient(ntsMap.getString("webHookUrl"), "POST", contentType);
			client.setTimeout(10000,10000);
			client.setDoInputOutput(true, true);
			String payload = ntsMap.getString("payLoad");
			ntsMap.put("resData", CommonUtil.cut(client.connect(payload),100));
			ntsMap.put("code", client.getHttpCode());
		
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
			if(client.getHttpCode() == 200){
				if(ntsMap.getString("resData").indexOf("OK") > -1) {
					ntsMap.put("status"		, "전송완료");
				}else {
					ntsMap.put("status"		, "전송실패");
				}
			}else{
				ntsMap.put("status"		, "전송실패");
			}
		} catch(Exception e) {
			logger.info("PG WH MERCHANT URL REQUEST ERROR =["+e.getMessage()+"]");
			ntsMap.put("status","전송실패");
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
		}finally{
			logger.info("PG WH MERCHANT THREAD RESPONSE : "+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
			logger.info("PG WH MERCHANT THREAD Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
			if("retry".equals(retry)){
				webHookDAO.updateTrxNTSPG(ntsMap);
			}else{
				webHookDAO.insertTrxNTSPG(ntsMap);
			}
			
		}*/
	}
	
	
	private static void disableSslVerification() {
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
				}
			};
		
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
			    public boolean verify(String hostname, SSLSession session) {
			        return true;
			    }
			};
			
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
	
	
	


}
