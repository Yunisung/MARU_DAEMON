package com.pgmate.dm.main;

import com.pgmate.dm.dao.RebillDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RebillProcess {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private SmsGw smsGw = null;
    private String msgBody = "";

    public static void main(String[] args) { new RebillProcess(); }

    public RebillProcess() {
        smsGw = new SmsGw();

        logger.info("==============================");
        logger.info("정기결제 자동결제 로직 시작");

        RebillAutoPay();

        logger.info("정기결제 자동결제 로직 끝");
        logger.info("==============================");

    }

    public void RebillAutoPay() {
        RebillDAO dao = new RebillDAO();
        String currentDate = CommonUtil.getCurrentDate("yyyyMMdd");

        logger.info(" {}일자 정기결제 시작", currentDate);
        List<SharedMap<String, Object>> getRebillList = dao.getRebillList(currentDate);

        if(getRebillList.size() > 0) {
            for(SharedMap<String, Object> data : getRebillList) {

                String rebillId = data.getString("rebillId");
                String payKey = data.getString("payKey");

                comm(rebillId, payKey);
            }
        }

    }

    public String comm(String rebillId,String payKey){
        //String paymentUrl = "https://api.bkwinners.kr/api/refund";
        //String paymentUrl = "http://pgwas1:10002/api/refund";
//        String paymentUrl = "https://devapi.bkwinners.kr/rebill/pay";
        String paymentUrl = "http://127.0.0.1:10002/api/rebill/pay";
        paymentUrl += "/" + rebillId;

        StringBuilder result = new StringBuilder();
        URL url = null;
        HttpURLConnection conn = null;

        System.setProperty("https.protocols", "TLSv1.2");

        long time = System.currentTimeMillis();
        try {
            logger.info("rebillId ["+rebillId+"]");
            url = new URL(paymentUrl);



            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);


            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", payKey);
            conn.setRequestProperty("Connection", "close");

//            OutputStream os = conn.getOutputStream();
//            os.write(rebillId.getBytes("utf-8"));
//            os.flush();
//            os.close();


            BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream()));

            String line;
            while ((line = br.readLine()) != null)
                result.append(line+"\n");

            br.close();

        } catch(Exception e) {
            result.append("CONNECT ERROR ["+e.getMessage()+"] "+paymentUrl);
            logger.error("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]");

        }finally{
            logger.info("ElapsedTime : "+(long)(System.currentTimeMillis()-time)+"msec");
            logger.info("LOCAL << PAYMENT ["+result.toString()+"]");
            conn.disconnect();
        }
        return result.toString();
    }

}
