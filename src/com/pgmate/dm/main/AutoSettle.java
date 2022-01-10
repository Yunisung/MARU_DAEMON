package com.pgmate.dm.main;

import java.text.DecimalFormat;
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
public class AutoSettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.AutoSettle.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
    
	public static void main(String[] args){
		new AutoSettle(args);
	}
	
	public AutoSettle(String[] args) {
		logger.info("==================================================");
		logger.info("AutoSettle Strart");
		smsGw = new SmsGw();
		
		autoSettle(args);
		autoVactSettle(args);
		
		logger.info("AutoSettle End");
		logger.info("==================================================");
	}
	
	public void autoSettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {
			String stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
			String stlType = "A+1";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			logger.info("자동정산 stlDay : " + stlDay + ", stlType : " + stlType);
			
			//매입원장에서 자동정산 정산예정일자의 정산데이터 조회
			List<SharedMap<String,Object>> getRealTimeSettleList = dao.getAutoSettleList(stlDay, stlType);
			logger.info("GetAutoSettleList COUNT : {}", getRealTimeSettleList.size());
			
			if(getRealTimeSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_SETTLE_AUTO INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getRealTimeSettleList){
					msgBody = "";
					
					String stlId = dao.getSettleId();
					
					SharedMap<String,Object> mchtMngMap		= dao.getMchtMngByMchtId(data.getString("mchtId"));
					SharedMap<String,Object> mchtTaxMap		= dao.getMchtTaxByMchtId(data.getString("mchtId"));
					
					SharedMap<String,Object> settleData = new SharedMap<String, Object>();

					settleData.put("stlId", stlId);
					settleData.put("mchtId", data.getString("mchtId"));
					settleData.put("status", "지급대기");
					settleData.put("stlDay", data.getString("stlDay"));
					settleData.put("payType", "C");
					settleData.put("startDay", data.getString("startDay"));
					settleData.put("endDay", data.getString("endDay"));
					
					settleData.put("payAmt", data.getLong("payAmt"));
					settleData.put("payFee", data.getLong("payFee"));
					settleData.put("payVat", data.getLong("payVat"));
					settleData.put("payCnt", data.getLong("payCnt"));
					
					settleData.put("rfdAmt", data.getLong("rfdAmt"));
					settleData.put("rfdFee", data.getLong("rfdFee"));
					settleData.put("rfdVat", data.getLong("rfdVat"));
					settleData.put("rfdCnt", data.getLong("rfdCnt"));
					
					settleData.put("payOutFee", mchtMngMap.getLong("payOutFee"));
					settleData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
					
					if(data.getLong("stlAmount") != 0) {
						settleData.put("payOutAmount", data.getLong("stlAmount") - settleData.getLong("payOutFee") - settleData.getLong("payOutFeeVat"));	
					}else {
						settleData.put("payOutAmount",0);
					}
					
							
					settleData.put("bankCd", mchtTaxMap.getString("bankCd"));
					settleData.put("bankName", mchtTaxMap.getString("bankName"));
					settleData.put("account", dao.getAESEnc(mchtTaxMap.getString("account")).replace("-", "").trim());
					settleData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
					settleData.put("stlRate", data.getDouble("stlRate"));
					settleData.put("stlType", data.getString("stlType"));
					settleData.put("regId", "SYSTEM");
					
					logger.info("stlId  	  : {}",stlId);
					logger.info("mchtId  	  : {}",data.getString("mchtId"));
					logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
					
					if(!dao.insertSettleAuto(settleData)){
						msgBody = "PG_SETTLE_AUTO INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
					}
					
					if(!"".equals(msgBody)) {
						logger.info(msgBody);
						
						smsGw.sendMessage("0", "4", msgBody);
					}
					logger.info("==================================================");
				}
				logger.info("PG_SETTLE_AUTO INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "자동정산 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	public void autoVactSettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {
			String stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
			String stlType = "A+1";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			logger.info("가상계좌 자동정산 stlDay : " + stlDay + ", stlType : " + stlType);
			
			//가상계좌 거래내역 정산예정일자의 자동정산 정산데이터 조회
			List<SharedMap<String,Object>> getAutoVactSettleList = dao.getAutoVactSettleList(stlDay, stlType);
			logger.info("getAutoVactSettleList COUNT : {}", getAutoVactSettleList.size());
			
			if(getAutoVactSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_SETTLE_AUTO VACT INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getAutoVactSettleList){
					msgBody = "";
					
					String stlId = dao.getSettleId();
					
					SharedMap<String,Object> mchtMngVactMap	= dao.getMchtMngVactByMchtId(data.getString("mchtId"));

					SharedMap<String,Object> settleData = new SharedMap<String, Object>();

					settleData.put("stlId", stlId);
					settleData.put("mchtId", data.getString("mchtId"));
					settleData.put("status", "지급대기");
					settleData.put("stlDay", data.getString("stlDay"));
					settleData.put("payType", "V");
					settleData.put("startDay", data.getString("startDay"));
					settleData.put("endDay", data.getString("endDay"));
					
					settleData.put("payAmt", data.getLong("payAmt"));
					settleData.put("payFee", data.getLong("payFee"));
					settleData.put("payVat", data.getLong("payVat"));
					settleData.put("payCnt", data.getLong("payCnt"));
					
					settleData.put("rfdAmt", data.getLong("rfdAmt"));
					settleData.put("rfdFee", data.getLong("rfdFee"));
					settleData.put("rfdVat", data.getLong("rfdVat"));
					settleData.put("rfdCnt", data.getLong("rfdCnt"));
					
					settleData.put("payOutFee", mchtMngVactMap.getLong("payOutFee"));
					settleData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
					
					if(data.getLong("stlAmount") != 0) {
						settleData.put("payOutAmount", data.getLong("stlAmount") - settleData.getLong("payOutFee") - settleData.getLong("payOutFeeVat"));	
					}else {
						settleData.put("payOutAmount",0);
					}
						
					settleData.put("bankCd", data.getString("bankCd"));
					settleData.put("bankName", data.getString("bankName"));
					settleData.put("account", dao.getAESEnc(data.getString("account")).replace("-", "").trim());
					settleData.put("accntHolder", dao.getAESEnc(data.getString("accntHolder")));
					settleData.put("stlRate", mchtMngVactMap.getDouble("rate"));
					settleData.put("stlType", data.getString("stlType"));
					settleData.put("regId", "SYSTEM");
					
					logger.info("stlId  	  : {}",stlId);
					logger.info("mchtId  	  : {}",data.getString("mchtId"));
					logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
					
					if(dao.insertSettleAuto(settleData)){
						dao.setSettleToIdx(stlId, stlDay, data.getString("mchtId"), stlType);
						dao.updateStlId(stlId);
					}else {
						msgBody = "PG_SETTLE_AUTO VACT INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
					}
					
					if(!"".equals(msgBody)) {
						logger.info(msgBody);
						
						smsGw.sendMessage("0", "4", msgBody);
					}
					logger.info("==================================================");
				}
				logger.info("PG_SETTLE_AUTO VACT INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "가상계좌 자동정산 오류발생. 확인요망 [" + e.getMessage() + "]";
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
}
