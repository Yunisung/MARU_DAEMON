package com.pgmate.dm.main;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.SettlePhoneDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class PhoneSettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.PhoneSettle.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
    
	public static void main(String[] args){
		new PhoneSettle(args);
	}
	
	public PhoneSettle(String[] args) {
		logger.info("==================================================");
		logger.info("PhoneSettle(휴대폰 정산 배치) Strart");
		smsGw = new SmsGw();
		
		phoneSettle(args);
		
		logger.info("PhoneSettle(휴대폰 정산 배치) End");
		logger.info("==================================================");
	}
	
	public void phoneSettle(String[] args){
		SettlePhoneDAO dao = new SettlePhoneDAO();
		
		try {
			String stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
			
			if(args[0] != null && !"".equals(args[0])) {
				stlDay = args[0];
			}
			
			logger.info("stlDay : " + stlDay);
			
			//매입원장에서 정산예정일자의 정산데이터 조회
			List<SharedMap<String,Object>> getPhoneSettleList = dao.getPhoneSettleList(stlDay);
			logger.info("GetPhoneSettleList COUNT : {}", getPhoneSettleList.size());
			
			if(getPhoneSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_SETTLE_PHONE INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getPhoneSettleList){
					msgBody = "";
					
					String stlId = dao.getSettleId();

					SharedMap<String,Object> mchtTaxMap		= dao.getMchtTaxByMchtId(data.getString("mchtId"));
					
					SharedMap<String,Object> settleData = new SharedMap<String, Object>();

					settleData.put("stlId"		, stlId);
					settleData.put("mchtId"		, data.getString("mchtId"));
					settleData.put("stlDay"		, data.getString("stlDay"));
					settleData.put("startDay"	, data.getString("startDay"));
					settleData.put("endDay"		, data.getString("endDay"));
					
					settleData.put("payAmt"		, data.getLong("payAmt"));
					settleData.put("payFee"		, data.getLong("payFee"));
					settleData.put("payVat"		, data.getLong("payVat"));
					settleData.put("payCnt"		, data.getLong("payCnt"));
					
					settleData.put("rfdAmt"		, data.getLong("rfdAmt"));
					settleData.put("rfdFee"		, data.getLong("rfdFee"));
					settleData.put("rfdVat"		, data.getLong("rfdVat"));
					settleData.put("rfdCnt"		, data.getLong("rfdCnt"));
					
					settleData.put("stlAmt"		, data.getLong("stlAmount"));
					settleData.put("benefit"	, data.getLong("benefit"));
					
					settleData.put("bankCd"		, mchtTaxMap.getString("bankCd"));
					settleData.put("bankName"	, mchtTaxMap.getString("bankName"));
					settleData.put("account"	, dao.getAESEnc(mchtTaxMap.getString("account")).replace("-", "").trim());
					settleData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
					settleData.put("stlRate"	, data.getDouble("stlRate"));
					settleData.put("stlType"	, data.getString("stlType"));
					
					settleData.put("regId", "SYSTEM");
					settleData.put("regDate", CommonUtil.getCurrentDate("yyyyMMdd"));
					
					logger.info("stlId	: {}",stlId);
					logger.info("stlDay	: {}",stlDay);
					logger.info("stlType: {}",data.getString("stlType"));
					logger.info("mchtId	: {}",data.getString("mchtId"));
					
					if(!dao.insertSettlePhone(settleData)){
						msgBody = "PG_SETTLE_PHONE INSERT 실패. 확인요망 [" + data.getString("mchtId") + "]";
					}else {
						if(!dao.updatePhoneCapUpdate(stlId,stlDay,data.getString("stlType"),data.getString("mchtId"),stlDay,"정산완료")){
							msgBody = "PG_PHONE_CAP update 실패. 확인요망 [" + data.getString("mchtId") + "]";
						}
					}
					
					if(!"".equals(msgBody)) {
						logger.info(msgBody);
						
						smsGw.sendMessage("0", "4", msgBody);
					}
					logger.info("==================================================");
				}
				logger.info("PG_SETTLE_PHONE INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "휴대폰정산 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	public long calcFee(long amount,double rate){
		rate = rateFormat(rate);
		long decimal = 10000;
		long fee = 0;
		if(amount < 0){
			fee = -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
		}else{
			fee = new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
		}
		return fee;
	}
	
	public long calcVat(long fee){
		if(fee < 0){
			return -new Double(-fee *10 /100).longValue();
		}else{
			return new Double(fee *10 /100).longValue();
		}
	}
	
	public long calcFeeVat(long amount,double rate){
		long fee = calcFee(amount,rate);
		return fee+ calcVat(fee);
	}
	
	public double rateFormat(double rate){
		String pattern = "#.#####";
		DecimalFormat format = new DecimalFormat(pattern);
		return new Double(format.format(rate)).doubleValue();
	}
	
	/**
	 * 휴대폰 정산일자 자동 계산
	 * @param settleType - 0:주정산, 1:월정산
	 * @param today
	 * @return
	 */
	public String calcDay(String today){
		String nextDate = "";
		String dateFormat = "yyyyMMdd";

		if(today.length() < 8) {
			return nextDate;
		}

		int year = Integer.parseInt(today.substring(0,4));
		int month = Integer.parseInt(today.substring(4,6));
		int day = Integer.parseInt(today.substring(6,8));
		
		Calendar cal = Calendar.getInstance();
		cal.set(year, month-1, day);
		
		cal.add(Calendar.DATE, 1);
		nextDate = new SimpleDateFormat(dateFormat).format(cal.getTime());
		
		logger.info("nextDate : " + nextDate);
				
		return nextDate;
	}
}
