package com.pgmate.dm.main;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.RealTimePayOutDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class DailySettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.DailySettle.class );
	private SmsGw smsGw = null;
	
	private String msgBody = "";
	private String hour = "";
	private String stlDay = "";
	private String stlType = "";
	private String mchtId = "";
	private String stlId = "";
	private String startDay = "";
	private String endDay = "";
	private String type = "";
	
	private double stlRate = 0;
	
	private long payAmt = 0;
	private long payFee = 0;
	private long payVat = 0;
	private long payCnt = 0;
	
	private long rfdAmt = 0;
	private long rfdFee = 0;
	private long rfdVat = 0;
	private long rfdCnt = 0;
	
	private long payOutFee = 0;
	private long payOutFeeVat = 0;
	private long stlAmount = 0;
	
	private SharedMap<String,Object> settleData = new SharedMap<String, Object>();
	private SharedMap<String,Object> mchtMngMap	= new SharedMap<String, Object>();
	private SharedMap<String,Object> mchtTaxMap	= new SharedMap<String, Object>();
	private SharedMap<String,Object> mchtMngVactMap	= new SharedMap<String, Object>();
	
	HashMap<String,String> idMap = new HashMap<String,String>();
	
	public static void main(String[] args){
		new DailySettle(args);
	}
	
	public DailySettle(String[] args) {
		logger.info("==================================================");
		logger.info("DailySettle Strart");
		smsGw = new SmsGw();
		
		stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
		hour = CommonUtil.getCurrentDate("HH");
		
		stlType = "A+0";
		
		daliySettle(args);
		daliyVactSettle(args);
		
		logger.info("DailySettle End");
		logger.info("==================================================");
	}
	
	public void daliySettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {
			type = "C";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			logger.info("당일정산 stlDay : " + stlDay + ", stlType : " + stlType + ", hour : " + hour);
			
			//매입원장에서 자동정산 정산예정일자의 정산데이터 조회
			List<SharedMap<String,Object>> getDailySettleList = dao.getDailySettleList(hour, stlDay, stlType);
			logger.info("당일정산 대상거래 건수 : {}", getDailySettleList.size());
			
			if(getDailySettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("당일정산 PG_SETTLE_AUTO INSERT START");
				logger.info("==================================================");
						
				for(SharedMap<String,Object> data : getDailySettleList){
					msgBody = "";
					
					if(!mchtId.equals(data.getString("mchtId"))) {
						if(!"".equals(mchtId)) {
							//가맹점이 바뀌면 일단 insert
							insertAutoSettle(type);
							logger.info("==================================================");
						}

						stlId = dao.getSettleId();
						
						startDay = data.getString("trxDay");
						endDay = data.getString("trxDay");
						stlRate = data.getDouble("stlRate");
						mchtId = data.getString("mchtId");
						
						mchtMngMap	= dao.getMchtMngByMchtId(mchtId);
						mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);

						payAmt = data.getLong("payAmt");
						payFee = data.getLong("payFee");
						payVat = data.getLong("payVat");
						payCnt = data.getLong("payCnt");
						rfdAmt = data.getLong("rfdAmt");
						rfdFee = data.getLong("rfdFee");
						rfdVat = data.getLong("rfdVat");
						rfdCnt = data.getLong("rfdCnt");
						
						payOutFee = mchtMngMap.getLong("payOutFee");
						payOutFeeVat = calcVat(payOutFee);
						
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}else {
						if(1000000000 < stlAmount + (data.getLong("payAmt") - data.getLong("payFee") - data.getLong("payVat"))) {
							//10억이 넘으면 일단 insert
							insertAutoSettle(type);
							
							stlId = dao.getSettleId();
							logger.info("==================================================");
							
							startDay = data.getString("trxDay");
							endDay = data.getString("trxDay");
							stlRate = data.getDouble("stlRate");
							mchtId = data.getString("mchtId");
							
							mchtMngMap	= dao.getMchtMngByMchtId(mchtId);
							mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);
							
							payAmt = data.getLong("payAmt");
							payFee = data.getLong("payFee");
							payVat = data.getLong("payVat");
							payCnt = data.getLong("payCnt");
							rfdAmt = data.getLong("rfdAmt");
							rfdFee = data.getLong("rfdFee");
							rfdVat = data.getLong("rfdVat");
							rfdCnt = data.getLong("rfdCnt");
							
							payOutFee = mchtMngMap.getLong("payOutFee");
							payOutFeeVat = calcVat(payOutFee);
						}else {
							if(!endDay.equals(data.getString("trxDay"))) {
								endDay = data.getString("trxDay");
							}
							
							if(stlRate < data.getDouble("stlRate")) {
								stlRate = data.getDouble("stlRate");
							}

							payAmt += data.getLong("payAmt");
							payFee += data.getLong("payFee");
							payVat += data.getLong("payVat");
							payCnt += data.getLong("payCnt");
							rfdAmt += data.getLong("rfdAmt");
							rfdFee += data.getLong("rfdFee");
							rfdVat += data.getLong("rfdVat");
							rfdCnt += data.getLong("rfdCnt");
						}	
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}

					idMap.put(data.getString("capId"), stlId);
				}
				insertAutoSettle(type);
				
				logger.info("==================================================");
				logger.info("당일정산 PG_SETTLE_AUTO INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "당일정산 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	public void daliyVactSettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {		
			type = "V";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			logger.info("당일정산 가상계좌 stlDay : " + stlDay + ", stlType : " + stlType + ", hour : " + hour);
			
			//가상계좌 거래내역 정산예정일자의 자동정산 정산데이터 조회
			List<SharedMap<String,Object>> getDailyVactSettleList = dao.getDailyVactSettleList(hour, stlDay, stlType);
			logger.info("getAutoVactSettleList COUNT : {}", getDailyVactSettleList.size());
			
			if(getDailyVactSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("당일정산 가상계좌 PG_SETTLE_AUTO INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getDailyVactSettleList){
					msgBody = "";
					
					if(!mchtId.equals(data.getString("mchtId"))) {
						if(!"".equals(mchtId)) {
							//가맹점이 바뀌면 일단 insert
							insertAutoSettle(type);
							logger.info("==================================================");
						}
						mchtMngVactMap	= dao.getMchtMngVactByMchtId(data.getString("mchtId"));
						
						stlId = dao.getSettleId();
						
						startDay = data.getString("trxDay");
						endDay = data.getString("trxDay");
						stlRate = mchtMngVactMap.getDouble("rate");;
						mchtId = data.getString("mchtId");
						
						mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);

						payAmt = data.getLong("payAmt");
						payFee = data.getLong("payFee");
						payVat = data.getLong("payVat");
						payCnt = data.getLong("payCnt");
						rfdAmt = data.getLong("rfdAmt");
						rfdFee = data.getLong("rfdFee");
						rfdVat = data.getLong("rfdVat");
						rfdCnt = data.getLong("rfdCnt");
						
						payOutFee = mchtMngVactMap.getLong("payOutFee");
						payOutFeeVat = calcVat(payOutFee);
						
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}else {
						if(1000000000 < stlAmount + (data.getLong("payAmt") - data.getLong("payFee") - data.getLong("payVat"))) {
							//10억이 넘으면 일단 insert
							insertAutoSettle(type);
							
							stlId = dao.getSettleId();
							logger.info("==================================================");
							
							startDay = data.getString("trxDay");
							endDay = data.getString("trxDay");
							stlRate = mchtMngVactMap.getDouble("rate");;
							mchtId = data.getString("mchtId");
							
							mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);
							
							payAmt = data.getLong("payAmt");
							payFee = data.getLong("payFee");
							payVat = data.getLong("payVat");
							payCnt = data.getLong("payCnt");
							rfdAmt = data.getLong("rfdAmt");
							rfdFee = data.getLong("rfdFee");
							rfdVat = data.getLong("rfdVat");
							rfdCnt = data.getLong("rfdCnt");
							
							payOutFee = mchtMngVactMap.getLong("payOutFee");
							payOutFeeVat = calcVat(payOutFee);
						}else {
							if(!endDay.equals(data.getString("trxDay"))) {
								endDay = data.getString("trxDay");
							}
							
							if(stlRate < data.getDouble("stlRate")) {
								stlRate = data.getDouble("stlRate");
							}

							payAmt += data.getLong("payAmt");
							payFee += data.getLong("payFee");
							payVat += data.getLong("payVat");
							payCnt += data.getLong("payCnt");
							rfdAmt += data.getLong("rfdAmt");
							rfdFee += data.getLong("rfdFee");
							rfdVat += data.getLong("rfdVat");
							rfdCnt += data.getLong("rfdCnt");
						}	
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}

					idMap.put(data.getString("capId"), stlId);
				}
				insertAutoSettle(type);
				
				logger.info("==================================================");
				logger.info("당일정산 가상계쫘 PG_SETTLE_AUTO INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "가상계좌 당일정산 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	private void insertAutoSettle(String type) {
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		settleData = new SharedMap<String, Object>();
		 
		settleData.put("stlId", stlId);
		settleData.put("mchtId", mchtId);
		settleData.put("status", "지급대기");
		settleData.put("stlDay", stlDay);
		settleData.put("payType", type);
		settleData.put("startDay", startDay);
		settleData.put("endDay", endDay);
		
		settleData.put("payAmt", payAmt);
		settleData.put("payFee", payFee);
		settleData.put("payVat", payVat);
		settleData.put("payCnt", payCnt);
		
		settleData.put("rfdAmt", rfdAmt);
		settleData.put("rfdFee", rfdFee);
		settleData.put("rfdVat", rfdVat);
		settleData.put("rfdCnt", rfdCnt);
		
		settleData.put("payOutFee", payOutFee);
		settleData.put("payOutFeeVat", payOutFee);
		
		if(stlAmount != 0) {
			settleData.put("payOutAmount", stlAmount - payOutFee - payOutFeeVat);	
		}else {
			settleData.put("payOutAmount",0);
		}
				
		settleData.put("bankCd", mchtTaxMap.getString("bankCd"));
		settleData.put("bankName", mchtTaxMap.getString("bankName"));
		settleData.put("account", dao.getAESEnc(mchtTaxMap.getString("account")).replace("-", "").trim());
		settleData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
		settleData.put("stlRate", stlRate);
		settleData.put("stlType", stlType);
		settleData.put("regId", "SYSTEM");
		
		logger.info("stlId  	  : {}",stlId);
		logger.info("mchtId  	  : {}",mchtId);
		logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
		
		if(!dao.insertSettleAuto(settleData)){
			msgBody = "당일정산 PG_SETTLE_AUTO INSERT 실패. 확인요망 [" + stlDay + "][" + mchtId + "]";
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
}
