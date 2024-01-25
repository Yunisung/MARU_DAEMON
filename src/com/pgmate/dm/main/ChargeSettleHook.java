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

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class ChargeSettleHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.dm.main.ChargeSettleHook.class );
	private ChargeSettlePayOutDAO dao 	= null;
	private String hookAddr = "";
	private SharedMap<String,Object> sharedMap = null;
	private String retry = "";
	static {
	    disableSslVerification();
	}
	
	
	public ChargeSettleHook(String hookAddr,SharedMap<String,Object> sharedMap,String retry) {
		this.hookAddr = hookAddr;
		this.sharedMap 	= sharedMap;
		this.dao = new ChargeSettlePayOutDAO();
		this.retry = retry;
	}
	
	
	public void run(){
		SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();	
		
		ntsMap.put("hookAddr", hookAddr);
		
		logger.info("ChargeSettleHook   : {}",ntsMap.getString("hookAddr"));
		ntsMap.put("trxId"		, sharedMap.getString("trxId"));
		ntsMap.put("trackId"	, sharedMap.getString("trackId"));
		ntsMap.put("trxType"	, sharedMap.getString("trxType"));
		ntsMap.put("mchtId"		, sharedMap.getString("mchtId"));
		ntsMap.put("status"		, sharedMap.getString("status"));
		ntsMap.put("trxDay"		, sharedMap.getString("trxDay"));
		ntsMap.put("trxTime"	, sharedMap.getString("trxTime"));
		
		ntsMap.put("payLoad"	, sharedMap.getString("payLoad"));
		ntsMap.put("regDay"		, CommonUtil.getCurrentDate("yyyyMMdd"));
		ntsMap.put("regTime"	, CommonUtil.getCurrentDate("HHmmss"));
		
		
		long time = System.currentTimeMillis();

		URL url = null;
		HttpURLConnection conn = null;
		try {
			logger.info("trxId       : {}",ntsMap.getString("trxId"));
			url = new URL(hookAddr);
			conn = (HttpURLConnection)url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			OutputStream os = conn.getOutputStream();
			String payload = ntsMap.getString("payLoad");
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
			logger.info("ChargeSettle Noti URL REQUEST ERROR =["+e.getMessage()+"]");
			ntsMap.put("status","전송실패");
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
			ntsMap.put("code", 9999);
		}finally {
			conn.disconnect();
			logger.info("ChargeSettle Noti THREAD RESPONSE : "+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
			logger.info("ChargeSettle Noti Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
			logger.info("ChargeSettle payLoad : [{}]",ntsMap.getString("payLoad"));
			if("retry".equals(retry)){
				dao.updateChargeSettleNoti(ntsMap);
			}else{
				dao.insertChargeSettleNoti(ntsMap);
			}
		}

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
