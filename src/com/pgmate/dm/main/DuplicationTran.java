package com.pgmate.dm.main;

import java.text.DecimalFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.VaPayOutDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class DuplicationTran {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.DuplicationTran.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
    
	public static void main(String[] args){
		new DuplicationTran();
	}
	
	public DuplicationTran() {
		logger.info("==================================================");
		logger.info("DuplicationTran Strart");
		smsGw = new SmsGw();
		
		duplicationTran();
		
		logger.info("DuplicationTran End");
		logger.info("==================================================");
	}
	
	public void duplicationTran(){
		VaPayOutDAO dao = new VaPayOutDAO();
		
		try {
			//가상계좌 2중 입금거래건 검색
			List<SharedMap<String,Object>> duplTranList = dao.getDuplTranList();
			logger.info("DuplTranList COUNT : {}", duplTranList.size());
			
			if(duplTranList.size() > 0) {
				logger.info("==================================================");
				logger.info("가상계좌 중복입금 처리 START");
				
				for(SharedMap<String,Object> data : duplTranList){
					List<SharedMap<String,Object>> duplTranDtlList = dao.getDuplTranDtlList(data.getString("trxDay"), data.getString("seqNo"));
					
					logger.info(data.getString("trxDay") + "일 은행전문번호 " + data.getString("seqNo") + " 거래 " + data.getString("cnt") + "건 중복발생!!!");
					
					int cnt = 1;
					
					for(SharedMap<String,Object> dtlData : duplTranDtlList){
						logger.info("은행전문번호 " + data.getString("seqNo") + " : " + cnt + "번 거래 [" + dtlData.getString("vactId") + "]");

						//첫번째 거래 건을 제외한 나머지 거래 건만 처리
						if(cnt != 1) {
							//중복거래 건 가상계좌 대행서비스인지 확인 
							String vactUserId = dao.getVaUserId(dtlData.getString("account"));
					
							boolean delRes = dao.deleteVactTrx(dtlData.getString("vactId"));
							
							logger.info("PG_VACT_TRX 삭제처리 : [{}][{}]", dtlData.getString("vactId"), delRes);
							
							if(delRes) {
								//대행서비스 처리
								if(!"".equals(vactUserId)) {
									List<SharedMap<String,Object>> getVaPayList = dao.getVaPayList(dtlData.getString("vactId"));
									
									for(SharedMap<String,Object> data2 : getVaPayList){
										logger.info("가상계좌 대행서비스 중복입금 건 : [{}]", data2.getString("trxId"));
										
										//펌에러 테이블에 저장 
										SharedMap<String, Object> errData = getTrxErrData(data2);
										boolean errInsertRes = dao.insertTrxErr(errData);
										logger.info("VA_TRX_ERR insert : [{}][{}]", data2.getString("trxId"), errInsertRes);
										
										//실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌 
										boolean updateRes = dao.updateVaBalance(data2.getString("trxId"), data2.getString("id"), "-" + data2.getString("stlAmount"));
										logger.info("VA_TRX Balance update : [{}][{}]", data2.getString("trxId"), updateRes);
										
										//실패건 VA_TRX테이블에서 삭제
										boolean deleteVaTrxRes = dao.deleteVaTrx(data2.getString("trxId"));
										logger.info("VA_TRX delete : [{}][{}]", data2.getString("trxId"), deleteVaTrxRes);
										
										smsGw.sendMessage("0", "4", "가상계좌 대행서비스 중복입금 건 처리 : " + data2.getString("trxId"));
									}
								}else {
									if("D+0".equals(dtlData.getString("stlType"))) {
										//실시간 정산 거래건일 경우 처리	
										logger.info("실시간정산 중복입금 건 처리 : [{}]", dtlData.getString("vactId"));
										
										//취소건이 실시간 전송 거래건일 경우 실시간 정산 테이블에 취소거래 저장
										SharedMap<String, Object> realtimeTrx = dao.getRealtimeTrx(dtlData.getString("vactId"));
						
										if(realtimeTrx != null) {
											smsGw.sendMessage("0", "4", "가상계좌 실시간정산 중복입금 건 처리 : " + realtimeTrx.getString("trxId"));
										}
									}else if(dtlData.getString("stlType").startsWith("C")){
										//충정정산일 경우
										logger.info("충전정산 중복입금 건 처리 : [{}][{}]", dtlData.getString("mchtId"), dtlData.getString("vactId"));
										
										List<SharedMap<String,Object>> getChargeData = dao.getChargeSettleData(dtlData.getString("vactId"));
										
										for(SharedMap<String,Object> chargeData : getChargeData){
											//펌에러 테이블에 저장 
											SharedMap<String, Object> errData = getChargeErrData(chargeData);
											boolean errInsertRes = dao.insertChargeErr(errData);
											logger.info("VA_TRX_ERR insert : [{}][{}]", chargeData.getString("trxId"), errInsertRes);
											
											if(dao.updateChargeSettleBalance(chargeData.getString("trxId"), chargeData.getString("mchtId"), chargeData.getLong("netAmount"))){
												logger.info("충전정산 잔액차감 성공 : [{}][{}][{}]", chargeData.getString("mchtId"), chargeData.getString("trxId"), chargeData.getLong("netAmount"));
												
												if(dao.deleteChargeSettle(chargeData.getString("trxId"))) {
													logger.info("충전정산 내역삭제 성공 : [{}][{}]", chargeData.getString("mchtId"), chargeData.getString("trxId"));	
												}else {
													logger.info("충전정산 내역삭제 실패 : [{}][{}]", chargeData.getString("mchtId"), chargeData.getString("trxId"));
													
													smsGw.sendMessage("0", "4", "가상계좌 충전정산 중복입금 건 내역삭제 실패 : " + chargeData.getString("trxId"));
												}
											}else {
												logger.info("충전정산 잔액차감 실패 : [{}][{}]", chargeData.getString("mchtId"), chargeData.getString("trxId"));
												
												smsGw.sendMessage("0", "4", "가상계좌 충전정산 중복입금 건 잔액차감 실패 : " + chargeData.getString("trxId"));
											}
										}
									}else {
										smsGw.sendMessage("0", "4", "가상계좌 중복입금 건 처리 : " + dtlData.getString("vactId"));
									}
								}
							}else {
								smsGw.sendMessage("0", "4", "가상계좌 중복입금 거래삭제 오류발생 확인요망 : " + dtlData.getString("vactId"));
							}
						}
						cnt++;
					}
				}

				logger.info("가상계좌 중복입금 처리 END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "가상계좌 2중 입금처리 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	/**
	 * VA_TRX_ERR 테이블에 넣을 데이터 세팅
	 * @param data
	 * @return
	 */
	private SharedMap<String,Object> getTrxErrData(SharedMap<String,Object> trxData) {
		SharedMap<String,Object> data = new SharedMap<String, Object>();
		
		try {
			String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			data.put("trxType", trxData.getString("trxType"));
			data.put("trxUnit", trxData.getString("trxUnit"));
			data.put("feeType", trxData.getString("feeType"));
			data.put("feeRate", trxData.getDouble("feeRate"));
			data.put("fee", trxData.getLong("fee"));
			data.put("feeVat", trxData.getLong("feeVat"));
			data.put("ptnFeeRate", trxData.getDouble("ptnFeeRate"));
			data.put("ptnFee", trxData.getLong("ptnFee"));
			data.put("ptnFeeVat", trxData.getLong("ptnFeeVat"));
			data.put("bankFee", trxData.getLong("bankFee"));
			data.put("stlAmount", trxData.getLong("stlAmount"));
			data.put("balance", trxData.getLong("balance"));
			data.put("trackId", trxData.getString("trackId"));
			data.put("refId", trxData.getString("refId"));
			data.put("bankCd", trxData.getString("bankCd"));
			data.put("account", trxData.getString("account"));
			data.put("holder", trxData.getString("holder"));
			data.put("regDay", regDate.substring(0, 8));
			data.put("resultCd", trxData.getString("resultCd"));
			data.put("resultMsg", trxData.getString("resultMsg"));
		}catch(Exception e) {
			e.printStackTrace();;
			logger.error("getTrxErrData Error : [{}]", e.getMessage());
		}
		
		return data;
	}
	
	/**
	 * PG_CHARGE_SETTLE_ERR 테이블에 넣을 데이터 세팅
	 * @param data
	 * @return
	 */
	private SharedMap<String,Object> getChargeErrData(SharedMap<String,Object> chargeData) {
		SharedMap<String,Object> data = new SharedMap<String, Object>();
		
		try {
			data.put("trxId", chargeData.getString("trxId"));
			data.put("mchtId", chargeData.getString("mchtId"));
			data.put("trxType", chargeData.getString("trxType"));
			data.put("trxUnit", chargeData.getString("trxUnit"));
			data.put("trxDay", chargeData.getString("trxDay"));
			data.put("trxTime", chargeData.getString("trxTime"));
			data.put("amount", chargeData.getLong("amount"));
			data.put("fee", chargeData.getLong("fee"));
			data.put("feeVat", chargeData.getLong("feeVat"));
			data.put("bankFee", chargeData.getLong("bankFee"));
			data.put("netAmount",chargeData.getLong("netAmount"));
			data.put("balance", chargeData.getLong("balance"));
			data.put("trackId", chargeData.getString("trackId"));
			data.put("refId", chargeData.getString("refId"));
			data.put("bankCd", chargeData.getString("bankCd"));
			data.put("bankName", chargeData.getString("bankName"));
			data.put("account", chargeData.getString("account"));
			data.put("holder", chargeData.getString("holder"));
			data.put("recordInfo", chargeData.getString("recordInfo"));
			data.put("summary", "가상계좌 2증입금 삭제 건");	

			String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			data.put("regDay", regDate.substring(0, 8));
			data.put("regId", chargeData.getString("regId"));
			data.put("resultCd", chargeData.getString("resultCd"));
			data.put("resultMsg", chargeData.getString("resultMsg"));
		}catch(Exception e) {
			e.printStackTrace();
			logger.error("getChargeErrData 오류 : [{}]", e.getMessage());
		}
		
		return data;
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
