package com.pgmate.dm.main;

import java.util.Calendar;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.TotalDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class Daemon {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Daemon.class );
	
	public Daemon(String cmd, String cmd2) {
		try {
			logger.info("cmd : {} ",cmd);
			if(cmd.equals("TOT_CAP")){
				logger.info("cmd2 : {} ",cmd2);
				if(Pattern.matches("^[0-9]{8}$", cmd2)){
					for(int i = -15; i <= -1; i++) {
						String curDate = CommonUtil.getOpDate(Calendar.DATE, i,cmd2);
						logger.debug(curDate);
						TotalDAO t = new TotalDAO();
						t.getTotCapByStlDay(curDate);
						t.getTotCapByCapDay(curDate);
						t.getCapMerchantList(curDate);
						t.getCapTmnList(curDate);
						//230620 VA_PRF 테이블 미존재로 주석처리
//						t.getPrf(curDate);
					}
				}else {
					logger.info("입력받은 날짜가 올바르지 않습니다. = {}",cmd2);
				}
			}
		}catch(Exception ex) {
			SmsGw smsGw = new SmsGw();
			String msgBody = "통계 데이터 생성 오류. 확인요망";
			
			smsGw.sendMessage("0", "1", msgBody);
            
            logger.error(ex.getMessage(), ex);
		}
	}
	
	public static void main(String[] args){
		if(args.length == 2){
			new Daemon(args[0], args[1]);
		}else{
			new Daemon("TOT_CAP", CommonUtil.getCurrentDate("yyyyMMdd"));
		}
	}
}
