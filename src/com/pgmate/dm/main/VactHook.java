package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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

import com.pgmate.dm.bean.VactHookBean;
import com.pgmate.dm.dao.VactHookDAO;
import com.pgmate.lib.util.comm.TcpSocket;
import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class VactHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.dm.main.VactHook.class );
	private SharedMap<String,Object> trxMap = null;
	private VactHookDAO vactHookDAO 	= null;
	
	public VactHook(SharedMap<String,Object> trxMap, VactHookDAO vactHookDAO) {
		this.trxMap = trxMap;
		this.vactHookDAO 	= new VactHookDAO();
		
	}
	
	
	public void run(){
		
		logger.info("hookType: {}, hookAddr:{}",trxMap.getString("hookType"),trxMap.getString("hookAddr"));
	
		VactHookBean hook = new VactHookBean();
		hook.vactId		= trxMap.getString("vactId");
		hook.retry		= trxMap.getInt("hookRetry");
		hook.mchtId		= trxMap.getString("mchtId");
		hook.issueId	= trxMap.getString("issueId");
		hook.bankCd		= trxMap.getString("bankCd");
		hook.account	= trxMap.getString("account");
		hook.sender		= trxMap.getString("sender");
		hook.amount		= trxMap.getLong("amount");
		if(trxMap.isEquals("trxType", "입금")){
			hook.trxType= "deposit";
		}else if(trxMap.isEquals("trxType", "취소")){
			hook.trxType= "depositback";
		}
		hook.rootVactId	= trxMap.getString("rootVactId");
		hook.trxDay		= trxMap.getString("trxDay");
		hook.trxTime	= trxMap.getString("trxTime");
		hook.trackId	= trxMap.getString("trackId");
		hook.udf1		= trxMap.getString("udf1");
		hook.udf2		= trxMap.getString("udf2");
		hook.stlDay		= trxMap.getString("stlDay");
		
		hook.stlFee		= trxMap.getLong("stlFee");
		hook.stlFeeVat	= trxMap.getLong("stlFeeVat");
		hook.stlAmount	= hook.amount - hook.stlFee - hook.stlFeeVat;
		
		String payLoad = GsonUtil.toJson(hook);
		String hookStatus = "";
		String hookResponse = "";
		
		long time = System.currentTimeMillis();
		URL url = null;
		HttpURLConnection conn = null;
		try {
			logger.info("VACT HOOK START");
			logger.info("vactId       : {}",trxMap.getString("vactId"));
			
			
			if(trxMap.getString("hookType").startsWith("HTTP")){
//				String targetUrl 	= trxMap.getString("hookType")+"://"+trxMap.getString("hookAddr").trim();
//				logger.info("vact hook target http : {}",targetUrl);
//				
//				String contentType = "application/x-www-form-urlencoded";
//				UrlClient client = new UrlClient(targetUrl, "POST", contentType);
//				client.setTimeout(30000,30000);
//				client.setDoInputOutput(true, true);
//				hookResponse =client.connect("response="+URLEncoder.encode(payLoad,"UTF-8"));
//				hookResponse = CommonUtil.cut(hookResponse, 100)+","+client.getHttpCode();
//				if(hookResponse.indexOf("OK") > -1 || hookResponse.indexOf("result=0000") > -1 || client.getHttpCode() == 200){
//					hookStatus = "전송완료";
//				}else{
//					hookStatus = "전송실패";
//				}
				
				
				disableSslVerification();
				String targetUrl 	= trxMap.getString("hookType")+"://"+trxMap.getString("hookAddr").trim();
				logger.info("vact hook target http : {}",targetUrl);
				url = new URL(targetUrl);
				conn = (HttpURLConnection)url.openConnection();
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
				conn.setConnectTimeout(30000);
				conn.setReadTimeout(30000);
				OutputStream os = conn.getOutputStream();
				String str = "response="+URLEncoder.encode(payLoad,"UTF-8");
				os.write(str.getBytes("UTF-8"));
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
				hookResponse =sb.toString();
				hookResponse = CommonUtil.cut(hookResponse, 100)+","+conn.getResponseCode();
				
				if(hookResponse.indexOf("OK") > -1 || hookResponse.indexOf("result=0000") > -1 || conn.getResponseCode() == 200){
					hookStatus = "전송완료";
				}else{
					hookStatus = "전송실패";
				}
			}else if(trxMap.getString("hookType").startsWith("TCP")){
				String[] target 	= CommonUtil.split(trxMap.getString("hookAddr"),":",true);
				logger.info("vact hook target tcp : {}:{}",target[0],target[1]);
				
				TcpSocket tcp = new TcpSocket();
				tcp.setSocketProperty(target[0], CommonUtil.parseInt(target[1]), 30000);
				hookResponse = tcp.sendRecv(CommonUtil.zerofill(payLoad.getBytes().length,4)+payLoad);
				if(hookResponse.indexOf("OK") > -1 || hookResponse.indexOf("result=0000") > -1 ){
					hookStatus = "전송완료";
				}else{
					hookStatus = "전송실패";
				}
			}else{
				throw new Exception("hookType 구분없음,"+trxMap.getString("hookType"));
			}
			
		} catch(Exception e) {
			logger.info("VACT HOOK REQUEST ERROR =["+e.getMessage()+"]");
			hookStatus = "전송장애";
			hookResponse = e.getMessage();
		}finally{
			logger.info("VACT HOOK RESPONSE : [{}]",CommonUtil.cut(hookResponse,110)+"]");
			logger.info("VACT HOOK Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
			vactHookDAO.updateVactTrx(trxMap.getString("vactId"),hookStatus,hookResponse,hook.retry+1);
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
