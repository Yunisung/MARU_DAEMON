package com.pgmate.dm.main;

import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.dm.dao.RealTimePayOutDAO;
import com.pgmate.dm.util.FirmClient;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class RealTimePayOut {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.RealTimePayOut.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
	
	private String firmServer = "pgwas2";
	private int frimPort = 10026;
	private int firmTimeOut = 70000;
	private int firmStartTime = 3000; //출금 시작 시간
	private int firmEndTime = 233000; //출금 중지 시간 
    
	public static void main(String[] args){
		new RealTimePayOut();
	}
	
	public RealTimePayOut() {
		logger.info("==================================================");
		logger.info("RealTimePayOut Strart");
		smsGw = new SmsGw();
		
		configSetting();
		realTimePayOut();
		logger.info("RealTimePayOut End");
		logger.info("==================================================");
	}
	
	public void realTimePayOut(){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		String payType = "C";
		
		try {
			int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
			
			//매일 23:30~00:30분까지는 은행 점검시간이라서 출금기능 막음
			if(currentTime > firmEndTime || currentTime < firmStartTime) {
				logger.info("- -- --- ---- ---- ---- 실시간 출금 서비스 가능한 시간이 아닙니다. ---- ---- ---- --- -- -");
				return;
			}
			
			//실시간 정산 승인거래 원장에서 실시간 출금 데이터로 보내지 않은 데이터중에 실시간정산 전송간격이 지난 거래건 조회
			List<SharedMap<String,Object>> getRealTimePayList = dao.getRealTimePayList(payType);
			logger.info("trxRealTimePayOut COUNT : {}", getRealTimePayList.size());
			
			if(getRealTimePayList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_REALTIME_PAYOUT INSERT START");
				
				for(SharedMap<String,Object> data : getRealTimePayList){
					msgBody = "";
					SharedMap<String,Object> mchtMngMap		= dao.getMchtMngByMchtId(data.getString("mchtId"));
					SharedMap<String,Object> mchtTaxMap		= dao.getMchtTaxByMchtId(data.getString("mchtId"));
					
					SharedMap<String,Object> payOutData = new SharedMap<String, Object>();
					
					logger.info("trxId  	: {}",data.getString("trxId"));
					logger.info("trxType  	: {}",data.getString("trxType"));
					logger.info("payType  	: {}",data.getString("payType"));
					logger.info("mchtId  	: {}",data.getString("mchtId"));
					logger.info("amount  	: {}",data.getLong("amount"));
					
					payOutData.put("trxId", data.getString("trxId"));
					payOutData.put("mchtId", data.getString("mchtId"));
					payOutData.put("tmnId", data.getString("tmnId"));
					payOutData.put("trackId", data.getString("trackId"));
					payOutData.put("trxDay", data.getString("trxDay"));
					payOutData.put("trxTime", data.getString("trxTime"));
					payOutData.put("amount", data.getLong("amount"));
					payOutData.put("authCd", data.getString("authCd"));
					payOutData.put("trxType", data.getString("trxType"));
					payOutData.put("payType", data.getString("payType"));
					payOutData.put("bankCd", mchtTaxMap.getString("bankCd"));
					payOutData.put("bankName", mchtTaxMap.getString("bankName"));
					payOutData.put("account", dao.getAESEnc(mchtTaxMap.getString("account").replace("-", "").trim()));
					payOutData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
					
					if("0".equals(data.getString("trxType"))) {
						payOutData.put("stlFee"		, calcFee(data.getLong("amount"), mchtMngMap.getDouble("rate")));
						payOutData.put("stlFeeVat"	, calcVat(payOutData.getLong("stlFee")));
						payOutData.put("stlAmount", payOutData.getLong("amount") - payOutData.getLong("stlFee") - payOutData.getLong("stlFeeVat"));
						
						payOutData.put("payOutFee", mchtMngMap.getLong("payOutFee"));
						payOutData.put("payOutFeeVat", calcVat(payOutData.getLong("payOutFee")));
						payOutData.put("payOutAmount", payOutData.getLong("stlAmount") - payOutData.getLong("payOutFee") - payOutData.getLong("payOutFeeVat"));
								
						payOutData.put("sendCnt", "0");
						payOutData.put("regId", "SYSTEM");
					}else if("1".equals(data.getString("trxType"))) {
						SharedMap<String,Object> getRootRealTimePay = dao.getRootRealTimePay(data.getString("mchtId"), data.getString("authCd"));
						
						payOutData.put("stlFee", getRootRealTimePay.getLong("stlFee") * -1);
						payOutData.put("stlFeeVat", getRootRealTimePay.getLong("stlFeeVat") * -1);
						payOutData.put("stlAmount", getRootRealTimePay.getLong("stlAmount") * -1);
						
						payOutData.put("payOutFee", getRootRealTimePay.getLong("payOutFee") * -1);
						payOutData.put("payOutFeeVat", getRootRealTimePay.getLong("payOutFeeVat") * -1);
						payOutData.put("payOutAmount", getRootRealTimePay.getLong("payOutAmount") * -1);
						
						payOutData.put("payOutDay", data.getString("trxDay"));
						payOutData.put("payOutTime", data.getString("trxTime"));
						
						payOutData.put("resultCd", "0000");
						payOutData.put("resultMsg", "취소성공");
						
						payOutData.put("sendCnt", "1");
						payOutData.put("sendCheck", "Y");
						payOutData.put("regId", "REFUND");
						
						logger.info("실시간정산 취소거래 건 처리 : [{}]", data.getString("trxId"));
					}

					if(dao.insertRealtimePayOut(payOutData)){
						if(!dao.updateSendCheck(data.getString("trxId"))) {
							msgBody = "PG_TRX_REALTIME_PAY UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
						};
					}else {
						msgBody = "PG_REALTIME_PAYOUT INSERT 실패. 확인요망 [" + data.getString("trxId") + "]";
					}
					
					if(!"".equals(msgBody)) {
						logger.info(msgBody);
						
						smsGw.sendMessage("0", "4", msgBody);
					}
					logger.info("==================================================");
				}
				logger.info("PG_REALTIME_PAYOUT INSERT END");
				logger.info("==================================================");
			}
			
			List<SharedMap<String,Object>> getPayOutList = dao.getPayOutList(payType);
			logger.info("realTimePayOut COUNT : {}", getPayOutList.size());
			
			if(getPayOutList.size() > 0) {
				logger.info("==================================================");
				logger.info("실시간 출금 START");
				
				FirmBean firmBean = new FirmBean();
				
				for(SharedMap<String,Object> data : getPayOutList){
					//타임아웃이 70초라 전송횟수 업데이트 후 전송
					dao.countUpdate(data.getString("trxId"));
				}
				
				for(SharedMap<String,Object> data : getPayOutList){
					int errCnt = 0;
					boolean errFlag = false;
					String sendCheck = "N";
					
					msgBody = "";
					
					SharedMap<String,Object> mchtTaxMap	= dao.getMchtTaxByMchtId(data.getString("mchtId"));

					if(data.getLong("payOutAmount") > 0){
						if("0".equals(data.getString("sendCnt"))) {
							logger.info("실시간 출금 : [{}][{}]", data.getString("trxId"), data.getString("sendCnt"));
							
							//실시간 출금요청
							//운영
							firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).transfer("089", mchtTaxMap.getString("bankCd"), mchtTaxMap.getString("account").replace("-", "").trim(), data.getLong("payOutAmount"), data.getString("trxId"), "", "RS");
							//테스트
							//firmBean = new FirmBean();
							//firmBean.resultCd = "9999";
							//firmBean.resultMsg = "실패";
							
							if(!firmBean.resultCd.equals("0000") ) {
								msgBody = "실시간 출금 실패. trxId : [" + data.getString("trxId") + "], trackId : [" + data.getString("trackId") + "]";
								logger.info(msgBody);
								errFlag = true;
							}else {
								sendCheck = "Y";
								msgBody = "실시간 출금 성공. trxId : [" + data.getString("trxId") + "], trackId : [" + data.getString("trackId") + "]";
								logger.info(msgBody);
							}
						}else {
							String orgSeq = "";
							
							orgSeq = dao.getSeqNo(data.getString("trxId"));
							
							logger.info("실시간 출금 결과확인 : [{}][{}][{}]", data.getString("trxId"), data.getString("sendCnt"), orgSeq);
							
							//출금 실패한 건들은 결과확인
							//운영
							firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).resultCheck("089", orgSeq);
							//테스트
							//firmBean = new FirmBean();
							//firmBean.resultCd = "0000";
							//firmBean.resultMsg = "성공";
							
							if(!firmBean.resultCd.equals("0000") ) {
								msgBody = "결과확인 실패. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
								logger.info(msgBody);
								errFlag = true;
							}else {
								sendCheck = "Y";
								msgBody = "결과확인 성공. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
								logger.info(msgBody);
							}
						}
					} else {
						logger.info("실시간 출금 0원이하 거래건 : [{}][{}][{}]", data.getString("trxId"), data.getString("sendCnt"), data.getLong("payOutAmount"));
						
						sendCheck = "Y";
						firmBean.resultCd = "0000";
						firmBean.resultMsg = "0원이하 거래건";
					}
					
					
					//실시간출금 결과 저장
					if(!dao.updatePayOutRes(data.getString("trxId"), CommonUtil.getCurrentDate("yyyyMMdd"), CommonUtil.getCurrentDate("HHmmss"), mchtTaxMap.getString("bankCd"), 
										    mchtTaxMap.getString("bankName"), dao.getAESEnc(mchtTaxMap.getString("account")), dao.getAESEnc(mchtTaxMap.getString("accntHolder")), firmBean.resultCd, firmBean.resultMsg, sendCheck)) {
						msgBody = "PG_REALTIME_PAYOUT UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
						
						logger.info(msgBody);
						smsGw.sendMessage("0", "4", msgBody);
					}
					
					if("Y".equals(sendCheck)) {
						logger.info("trxId : payOutDay [{}][{}]", data.getString("trxId"), data.getString("payOutDay"));
						//실시간출금 결과 저장
						if(!dao.updatePayOutCapUpdate(data.getString("trxId"), data.getString("payOutDay"))) {
							msgBody = "PG_TRX_CAP_DTL UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
							
							logger.info(msgBody);
							smsGw.sendMessage("0", "4", msgBody);
						}else {
							msgBody = "실시간 출금 매입내역 업데이트 - trxId : [" + data.getString("trxId") + "]";
							logger.info(msgBody);
						}
					}
					
					if(errFlag) {
						//출금 실패시 출금 전송 횟수가 3회일 경우 SMS 알림 발송
						String sndCnt = dao.getSnedCnt(data.getString("trxId"));
						
						if(!"".equals(sndCnt)) {
							errCnt = Integer.parseInt(sndCnt);
							
							if(errCnt == 3) {
								dao.updatePayOutCancelCapUpdate(data.getString("trxId"));
								msgBody = "실시간 출금 3회 실패. 확인요망 [" + data.getString("trxId") + "][" + firmBean.resultMsg + "]";
								
								smsGw.sendMessage("0", "4", msgBody);
							}
						}
					}
				}
				logger.info("실시간 출금 END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "실시간 출금 오류발생. 확인요망 [" + e.getMessage() + "]";
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
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            // 프로퍼티 파일 위치
    		//운영
            String propFile = "/home/bkwinners/MARU/MARU_DAEMON/conf/firmconfig.properties"; 
    		//테스트
    		//String propFile = "/home/MARU/MARU_DAEMON/conf/firmconfig.properties";
            //로컬
    		//String propFile = "C:/workspace/MARU_DAEMON/conf/firmconfig.properties";
    		
            // 프로퍼티 객체 생성
            Properties props = new Properties();
            
            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(propFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));
            
            // 항목 읽기
            firmServer = props.getProperty("firm_server");

            String strFirmPort = props.getProperty("firm_port");
            String strFirmTimeOut = props.getProperty("firm_timeout");
            String strStartTime = props.getProperty("firm_starttime");
            String strEndTime = props.getProperty("firm_endtime");
            //String strCompNm = props.getProperty("compNm");
            
            if(strFirmPort != null && !"".equals(strFirmPort)) {
            	frimPort = Integer.parseInt(strFirmPort);
            }
            
            if(strFirmTimeOut != null && !"".equals(strFirmTimeOut)) {
            	firmTimeOut = Integer.parseInt(strFirmTimeOut);
            }
            
            if(strStartTime != null && !"".equals(strStartTime)) {
            	firmStartTime = Integer.parseInt(strStartTime);
            }
            
            if(strEndTime != null && !"".equals(strEndTime)) {
            	firmEndTime = Integer.parseInt(strEndTime);
            }
            
			/*
			 * if(strCompNm != null && !"".equals(strCompNm)) { compNm = strCompNm; }
			 */
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
