package com.pgmate.dm.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.CodeDAO;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.sms.SmsUtil;
/**
 * @author Administrator
 *
 */
public class SmsGw{
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.util.SmsGw.class );
	private final String SMS_URL = "https://sms.supersms.co:7020/sms/v3/multiple-destinations"; 
	private String smslist = "";
	private String type = "";
	private String token = "";
	private String chatId = "";
	
	public SmsGw() {
	}
	
	/**
	 * @param messageType : 0 - sms,telegram, 1 - sms, 2 - telegram
	 * @param smsCh : sms 채널
	 * @param msg : 보낼메세지
	 */
	public void sendMessage(String messageType, String smsCh , String msg){
		getConfig(smsCh);
		
		if("0".equals(messageType)) {
			smsSend(msg);
			telegramSend(msg);
		} else if("1".equals(messageType)) {
			smsSend(msg);
		}else if("2".equals(messageType)) {
			telegramSend(msg);
		}
	}
	
	private void smsSend(String msg) {
		try {
			String[] smsnum = smslist.split(",");
			
	        for(int i = 0; i < smsnum.length; i++) {
	    		
	        	//220126 박윤성 : SMS 통합
	        	SmsUtil.sendSms(SmsUtil.SMS_URL, smsnum[i].replaceAll("\\[^0-9]+", ""), msg);
	    		
	        }
		}catch (Exception e) {
			logger.info(e.getMessage(), e);
		}	
	}
	
	private void telegramSend(String msg) {
		BufferedReader in = null;
		
		try {
    		URL obj = new URL("https://api.telegram.org/bot" + token + "/sendmessage?chat_id=" + chatId + "&text=" + msg); // 호출할 url

    		HttpURLConnection con = (HttpURLConnection)obj.openConnection();
    		con.setRequestMethod("GET");
    		in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
    		String line;

    		while((line = in.readLine()) != null) { // response를 차례대로 출력
    			logger.info("Telegram resData  [{}]",line);
    		}	
		}catch (Exception e) {
			logger.info(e.getMessage(), e);
		}finally {
			 if(in != null) {
				 try { 
					 in.close(); 
				 } catch(Exception e) { 
					 logger.error(e.getMessage(), e);
				 }
			 }
		 }
	}
	
	/**
	 * config setting
	 * @param smsCh : sms채널
	 */
	public void getConfig(String smsCh) {
    	try{
            // 프로퍼티 파일 위치
            String messagePropFile = "/home/bkwinners/MARU/MARU_DAEMON/conf/messageconf.properties";

            // 프로퍼티 객체 생성
            Properties props = new Properties();

            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(messagePropFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));
            
            // SMS리스트 항목 읽기
            smslist = props.getProperty("smslist" + smsCh);
            type = props.getProperty("type");
            token = props.getProperty(type + "_token");
            chatId = props.getProperty(type + "_chat_id");
            
            logger.info("messge config Data : smslist [" + smslist + "], smsCh [" + smsCh + "], type [" + type + "]");
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
