package com.pgmate.dm.main;

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

import com.pgmate.dm.dao.WalletNotiDAO;
import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;


public class WalletWebHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.dm.main.WalletWebHook.class );
	private WalletNotiDAO notiDAO 	= null;
	private SharedMap<String, Object> sharedMap = null;

	static {
	    disableSslVerification();
	}
	
	
	public WalletWebHook(SharedMap<String, Object> sharedMap) {
		this.sharedMap = sharedMap;
		this.notiDAO	= new WalletNotiDAO();
	}
	
	
	public void run(){
		
		
		long time = System.currentTimeMillis();
		try {
			logger.info("trxId       : {}",sharedMap.getString("trxId"));
			String contentType = "application/json; charset=utf-8";
			
			
			UrlClient client = new UrlClient(sharedMap.getString("webhookUrl"), "POST", contentType);
			if(!CommonUtil.isNullOrSpace(sharedMap.getString("authorization"))) {
				SharedMap<String, String> map = new SharedMap<String,String>();
				map.put("Authorization", sharedMap.getString("authorization"));
				client.setRequestProperty(map);
			}
//			if(sharedMap.getString("ptnId").equals("beyondinc") || sharedMap.getString("ptnId").equals("woori")) {
//				SharedMap<String, String> map = new SharedMap<String,String>();
//				// 테스트
////				map.put("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJNVG91Y2giLCJVc2VySWQiOiJtdG91Y2giLCJQYXNzd2QiOiIzMzVCQjk4QTcwNjA1MTNFMzFDNzYyQUJERDMyQjc0MSIsIkdyYW50VHlwZSI6InJ3IiwiU2VydmljZU5hbWUiOiJNVG91Y2giLCJBY2Nlc3NFeHBpcmVEVCI6IjIwMjktMDgtMTAgMTc6MzE6MjUifQ.zAkqZ9BIDVqlmJzA6oWK1ELNllWo4A8V8n9xQr_BuiZHYgEhrI2IRtNAahyarBCZEKHL8b-NiF94zmehubKEEQ");
//				// 운영
//				map.put("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJNVG91Y2giLCJVc2VySWQiOiJtdG91Y2giLCJQYXNzd2QiOiIzMzVCQjk4QTcwNjA1MTNFMzFDNzYyQUJERDMyQjc0MSIsIkdyYW50VHlwZSI6InJ3IiwiU2VydmljZU5hbWUiOiJNVG91Y2giLCJBY2Nlc3NFeHBpcmVEVCI6IjIwMjktMDgtMjUgMTA6MzE6NDQifQ.JZ3COgEkyN7bdp6WcAl6iLNQ1JooVHpG8-jxk6LuoQ13pXBICxgMNnVF96-pIJcSKBi0x028YT2Q19sQMJjFDQ");
//				client.setRequestProperty(map);
//			}
			client.setTimeout(10000,10000);
			client.setDoInputOutput(true, true);
			String payLoad = sharedMap.getString("payLoad");
			sharedMap.put("resData", CommonUtil.cut(client.connect(new String(payLoad.getBytes(),"UTF-8")),100));
			sharedMap.put("code", client.getHttpCode());
		
			sharedMap.put("sendDate", CommonUtil.getCurrentTimestamp());
			if(sharedMap.getString("resData").indexOf("OK") > -1 || client.getHttpCode() == 200){
				sharedMap.put("status"		, "전송완료");
			}else{
				sharedMap.put("status"		, "전송실패");
			}
			
		} catch(Exception e) {
			logger.info("WALLET WEBHOOK URL REQUEST ERROR =["+e.getMessage()+"]");
			sharedMap.put("status","전송실패");
			sharedMap.put("sendDate", CommonUtil.getCurrentTimestamp());
		}finally{
			logger.info("WALLET WEBHOOK THREAD RESPONSE : "+CommonUtil.cut(sharedMap.getString("resData"),100)+"]");
			logger.info("WALLET WEBHOOK THREAD Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);

			notiDAO.updateWalletNoti(sharedMap);

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
