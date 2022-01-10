package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.CashReceiptDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.map.SharedMap;

public class CashReceiptErrList {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.CashReceiptErrList.class);

	private SmsGw smsGw = null;
	private String msgBody = "";

	private String type = ""; //API - test, real
	private String mid = "";
	private String ApiURL = "";
	
	public static void main(String[] args) {
		new CashReceiptErrList();
	}

	public CashReceiptErrList() {
		logger.info("==================================================");
		logger.info("CashReceiptErrList START");
		smsGw = new SmsGw();

		configSetting();
		getErrList();

		logger.info("==================================================");
		logger.info("CashReceiptErrList END");
	}

	// 현금영수증 발급오류내역 조회 API
	public void getErrList() {
		logger.info("==================================================");
		logger.info("getErrList START");
		logger.info("==================================================");

		CashReceiptDAO dao = new CashReceiptDAO();
	
		List<SharedMap<String, Object>> getCashErrList = dao.getCashErrList();
		logger.info("==================================================");
		logger.info("getCashErrList COUNT : {}", getCashErrList.size());
	
		String result = "";
		String params = "";
		String trDt = "";
		String authNo = "";
		String errCd = "";
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Calendar c1 = new GregorianCalendar();
		String regDay = sdf.format(c1.getTime());
		
		Calendar c2 = new GregorianCalendar();
		c2.add(Calendar.DATE, -1);
		String yesterday = sdf.format(c2.getTime());
	
		try {
			logger.info("==================================================");
			logger.info("PG_CASH_RECEIPT ErrList START");
				
			if(getCashErrList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_CASH_RECEIPT ErrList START");
				
				params = "&mid=" + mid + "&trDtGb=2&trDt1=" + regDay + "&trDt2=" + regDay;

				String strUrl = "https://" + ApiURL + "/pgtrans/CashReceiptMultiAction.do?_method=getReceiptErrList" + params;
				
				URL url = new URL(strUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				conn.setUseCaches(false);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				conn.setConnectTimeout(30000);
				conn.setReadTimeout(30000);
				conn.setRequestMethod("GET");
				conn.setRequestMethod("POST");
				
				try {
					if (conn.getResponseCode() != 200) {
						logger.error("현금영수증 getErrList Connection error : " + conn.getResponseCode());
					} else {
						Charset charset = Charset.forName("UTF-8");
						BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
						
						//파일 읽기 테스트
						/*String path = "/home/MARU/MARU_DAEMON/temp/test3.csv";
						File file = new File(path);
						BufferedReader br = new BufferedReader(new FileReader(file));*/
						//파일 읽기 테스트
						
						String splited[] = null;
						String line = System.lineSeparator();
						int totCnt = 0;
						int sucCnt = 0;
						int errCnt = 0;
						
						while ((line = br.readLine()) != null) {
							splited = line.split(",",line.length());
						
							result = splited[0];
							
							if (result.contains("2000")) {
								msgBody = "-----> 현금영수증 오류내역 없음. 일괄 성공 업데이트 진행 -----";
								logger.info(msgBody);
								
								dao.setSuccess(yesterday);
							} else if (result.contains("2001")) {
								if("mid_test".equals(mid)){
									logger.info("현금영수증 테스트 계정 사용 건 : [{}]", mid);
								} else {
									errCnt++;
									msgBody = "-----> 현금영수증 조회 국세청 결과 미수신, 잠시후 재조회 요망 -----";
									logger.info(msgBody);
								}
								
								if(errCnt == 3) { //13~15시까지  국세청 결과 미수신일 경우 실행
									msgBody = "-----> 현금영수증 조회 국세청 결과 [" + errCnt + "]회 미수신. 확인 요망 [" + regDay + "] 일 -------- ";
									//smsGw.sendMessage("0", "4", msgBody);
									logger.info(msgBody);
								}
							} else if (result.length() > 5) {
								totCnt++;
								
								trDt = splited[0];
								authNo = splited[3];
								errCd = splited[4];
								
								logger.info("CashReceipt Daemon Res - trDt : " + trDt + ", authNo : " + authNo + ", errCd : " + errCd);
								
								boolean updateCheck = dao.setErrSuccess(errCd, trDt, authNo);
								if (updateCheck == false) {
									msgBody = "-----> 현금영수증 오류내역 업데이트 실패 : [{}][{}][{}]" + trDt + authNo + errCd;
									//smsGw.sendMessage("0", "4", msgBody);
									logger.info(msgBody);
								} else {
									sucCnt++;
								}
							} else {
								logger.info("현금영수증 오류내역 조회 기타 결과 : [{}]", result);
							}
						}

						if(totCnt > 0) {
							boolean insertCheck = dao.setSuccess(yesterday);
							if (insertCheck == false) {
								logger.info("현금영수증 오류내역 조회 성공 건 업데이트 실패 : [{}]", yesterday);
							}
						}
						msgBody = "-----> 현금영수증 오류내역 조회 : " + yesterday + "일 -----";
						//smsGw.sendMessage("0", "4", msgBody);
						logger.info(msgBody);

						br.close();
						conn.disconnect();
						logger.info("==================================================");
						logger.info("PG_CASH_RECEIPT ErrList END");
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						
						msgBody = "-----> 현금영수증 조회 오류발생. 확인 요망.-----";
						//smsGw.sendMessage("0", "4", msgBody);
						logger.info(msgBody);
					}
			} else {
				logger.info("-----> 현금영수증 등록 건 없음.-----");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
	
			msgBody = "-----> 현금영수증 조회 오류발생. 확인요망.-----";
			//smsGw.sendMessage("0", "4", msgBody);
			logger.info(msgBody);
		}
		logger.info("==================================================");
		logger.info("getErrList END");
	}
	
	/**
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            //프로퍼티 파일 위치
    		//운영
            String propFile = "/home/MARU/MARU_DAEMON/conf/cashReceipt.properties"; 
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
            
            logger.info("type : [{}], mid : [{}], ApiURL : [{}]", type, mid, ApiURL);
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
