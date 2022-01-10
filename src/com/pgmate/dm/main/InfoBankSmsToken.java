package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.SmsToken;
import com.pgmate.dm.dao.InfoBankSmsTokenDAO;
import com.pgmate.lib.util.gson.GsonUtil;


/**
 * @author Administrator
 *
 */
public class InfoBankSmsToken {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.InfoBankSmsToken.class );
	
	private static String TOKEN_URL = "https://auth.supersms.co:7000/auth/v3/token";
	private static String Client_Id = "mtouch_msg";
	private static String Client_Passwd = "R1QB51052T060RPW7151";
	private int result = 0;
	
	public InfoBankSmsToken() {
		do {
			updateToken();
			logger.debug("result [{}]",result);
			try {
				Thread.sleep(5000);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}while(result == 0);
		logger.debug("update sms token complete.");
	}
	
	public void updateToken(){
		String contentType = "application/json";
		SmsToken token = new SmsToken();
		try {
			URL url = new URL(TOKEN_URL);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(10000);
			
			con.addRequestProperty("X-IB-Client-Id", Client_Id);
			con.addRequestProperty("X-IB-Client-Passwd", Client_Passwd);
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", contentType);

			con.setDoInput(true);
//			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setDefaultUseCaches(false);
			
			String resData = "";
			StringBuilder sb = new StringBuilder();
			
			if(con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(con.getInputStream(), "utf-8"));
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line).append("\n");
				}
				br.close();
				
				resData = sb.toString();
				logger.debug("resData [{}]",resData);
				
				token = (SmsToken)GsonUtil.fromJson(resData, SmsToken.class);
				new InfoBankSmsTokenDAO().updateToken(token);
				result++;
			}
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
	}
	
	public static void main(String[] args) {
		InfoBankSmsToken k = new InfoBankSmsToken();
	}
}


