package com.pgmate.dm.main;

import com.pgmate.dm.dao.VactAccountDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class VactAccountStatusHook extends Thread {
    private static Logger logger 				= LoggerFactory.getLogger( VactAccountStatusHook.class );
    private VactAccountDAO dao 	= null;
    private String hookAddr = "";
    private SharedMap<String,Object> sharedMap = null;
    private String retry = "";
    static {
        disableSslVerification();
    }

    public VactAccountStatusHook(String hookAddr, SharedMap<String,Object> sharedMap, String retry) {
        this.hookAddr = hookAddr;
        this.sharedMap 	= sharedMap;
        this.dao = new VactAccountDAO();
        this.retry = retry;
    }

    public void run(){
        SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();

        logger.info("VactAccountHook   : {}",ntsMap.getString("hookAddr"));

        ntsMap.put("trxId", sharedMap.getString("trxId"));
        ntsMap.put("hookAddr", hookAddr);
        ntsMap.put("mchtId"		, sharedMap.getString("mchtId"));
        ntsMap.put("vactAccount"	, sharedMap.getString("vactAccount"));
        ntsMap.put("vactStatus"	    , sharedMap.getString("vactStatus"));
        ntsMap.put("holderName"	, sharedMap.getString("holderName"));
        ntsMap.put("trxDay"     , CommonUtil.getCurrentDate("yyyyMMdd"));
        ntsMap.put("trxTime"    , CommonUtil.getCurrentDate("HHmmss"));
        ntsMap.put("payLoad"	, setPayLoad(ntsMap));
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
            logger.info("VactAccount Noti URL REQUEST ERROR =["+e.getMessage()+"]");
            ntsMap.put("status","전송실패");
            ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
            ntsMap.put("code", 9999);
        }finally {
            conn.disconnect();
            logger.info("VactAccount Noti THREAD RESPONSE : "+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
            logger.info("VactAccount Noti Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
            logger.info("VactAccount payLoad : [{}]",ntsMap.getString("payLoad"));

            if("retry".equals(retry)){
                dao.updateVactAccountStatusNoti(ntsMap);
            }else{
                dao.insertVactAccountStatusNoti(ntsMap);
            }
        }

    }

    public String setPayLoad(SharedMap<String, Object> sharedMap){
        SharedMap<String, String> payLoadMap = new SharedMap<String, String>();
        payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
        payLoadMap.put("vactAccount",sharedMap.getString("vactAccount"));
        payLoadMap.put("vactStatus",sharedMap.getString("vactStatus"));
        payLoadMap.put("holderName",sharedMap.getString("holderName"));
        payLoadMap.put("trxDay",CommonUtil.getCurrentDate("yyyyMMdd"));
        payLoadMap.put("trxTime",CommonUtil.getCurrentDate("HHmmss"));
        String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");

        return payLoad;
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
