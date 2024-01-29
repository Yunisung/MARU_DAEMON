package com.pgmate.dm.main;

import com.pgmate.dm.dao.RentRetryNotiDAO;
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

public class MemSettleHook extends Thread{
    private static Logger logger = LoggerFactory.getLogger( MemSettleHook.class );
    private RentRetryNotiDAO dao = null;
    private String hookAddr = "";
    private SharedMap<String,Object> sharedMap = null;
    private String retry = "";
    static {
        disableSslVerification();
    }

    public MemSettleHook(String hookAddr, SharedMap<String,Object> sharedMap, String retry) {
        this.hookAddr = hookAddr;
        this.sharedMap = sharedMap;
        this.dao = new RentRetryNotiDAO();
        this.retry = retry;
    }

    public void run() {
        SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();

        ntsMap.put("hookAddr", hookAddr);

        logger.info("MemberSettleHook   : {}",ntsMap.getString("hookAddr"));
        ntsMap.put("stlId", sharedMap.getString("stlId"));
        ntsMap.put("memberId", sharedMap.getString("memberId"));
        ntsMap.put("payLoad", sharedMap.getString("payLoad"));
        ntsMap.put("regDay"		, CommonUtil.getCurrentDate("yyyyMMdd"));
        ntsMap.put("regTime"	, CommonUtil.getCurrentDate("HHmmss"));

        long time = System.currentTimeMillis();

        URL url = null;
        HttpURLConnection conn = null;

        try {
            logger.info("stlId       : {}",ntsMap.getString("stlId"));
            url = new URL(hookAddr);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            OutputStream os = conn.getOutputStream();
            String payLoad = ntsMap.getString("payLoad");
            os.write(payLoad.getBytes("UTF-8"));
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
            logger.info("RiskChange Noti URL REQUEST ERROR =["+e.getMessage()+"]");
            ntsMap.put("status","전송실패");
            ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
        }finally {
            conn.disconnect();
            logger.info("RiskChange Noti THREAD RESPONSE : "+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
            logger.info("RiskChange Noti Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
            logger.info("ChargeSettle payLoad : [{}]",ntsMap.getString("payLoad"));
            dao.updateMemSettleNoti(ntsMap);
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

