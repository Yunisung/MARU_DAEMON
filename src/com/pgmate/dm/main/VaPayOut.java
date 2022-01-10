package com.pgmate.dm.main;

import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.dm.dao.VaPayOutDAO;
import com.pgmate.dm.util.FirmClient;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class VaPayOut {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VaPayOut.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
	
	private String firmServer = "pgwas2";
	private String compNm = "";
	private int frimPort = 10026;
	private int firmTimeOut = 70000;
	private int firmStartTime = 3000; //출금 시작 시간
	private int firmEndTime = 233000; //출금 중지 시간 
    
	public static void main(String[] args){
		new VaPayOut();
	}
	
	public VaPayOut() {
		logger.info("==================================================");
		logger.info("VaFirm Strart");
		smsGw = new SmsGw();
		
		configSetting();
		vaPayOut();
		logger.info("VaFirm End");
		logger.info("==================================================");
	}
	
	public void vaPayOut(){
		VaPayOutDAO dao = new VaPayOutDAO();
		
		try {
			int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
			
			//매일 23:30~00:30분까지는 은행 점검시간이라서 출금기능 막음
			if(currentTime > firmEndTime || currentTime < firmStartTime) {
				logger.info("- -- --- ---- ---- ---- 출금 서비스 가능한 시간이 아닙니다. ---- ---- ---- --- -- -");
				return;
			}
			
			//가상계좌 충전 출금대상거래 에서 출금하지 않은 데이터중에 전송시도가 남은 거래건들 조회
			List<SharedMap<String,Object>> getVaPayList = dao.getVaPayList();
			logger.info("VaPayList COUNT : {}", getVaPayList.size());
			
			
			if(getVaPayList.size() > 0) {
				logger.info("==================================================");
				logger.info("가상계좌 충전 출금 START");
				
				for(SharedMap<String,Object> data : getVaPayList){
					dao.updateStatus(data.getString("trxId"));
				}
				
				for(SharedMap<String,Object> data : getVaPayList){
					int errCnt = 0;
					boolean errFlag = false;
					String status = "실패";
					FirmBean firmBean = new FirmBean();
					String idx = "";
					msgBody = "";
					compNm = "";
					
					if("0".equals(data.getString("retry"))){
						logger.info("가상계좌 충전 출금 : [{}][{}]", data.getString("trxId"), data.getString("retry"));
						
						if(data.getString("recordInfo") != null && !"".equals(data.getString("recordInfo"))) {
							compNm = data.getString("recordInfo");
						}
						
						//최초 1회 출금요청
						//출금요청
						//운영
						firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).transfer("089", data.getString("bankCd"), data.getString("account").replace("-", "").trim(), data.getLong("amount"), data.getString("trxId"), compNm, "VA");
						//테스트
//						firmBean = new FirmBean();
//						firmBean.resultCd = "XXXX";
//						firmBean.resultMsg = "실패";
//						firmBean.idx = dao.insertTrx("020",data.getLong("amount"),data.getString("bankCd"),data.getString("account").replace("-", "").trim(),"테스트", data.getString("trxId"));
						
						idx = String.valueOf(firmBean.idx);
						
						if("".equals(idx) || "0".equals(idx)) {
							idx = dao.getIdx(data.getString("trxId"));
							
							logger.info("idx 재검색 : [{}][{}]", data.getString("trxId"), idx);
						}
						
						if(!firmBean.resultCd.equals("0000") ) {
							msgBody = "가상계좌 충전 출금 실패. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("id") + "], idx : [" + idx + "]";
							logger.info(msgBody);
							errFlag = true;
						}else {
							status = "완료";
							msgBody = "가상계좌 충전 출금 성공. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("id") + "], idx : [" + idx + "]";
							logger.info(msgBody);
						}
						
						if(!dao.updateRefIdUpdate(data.getString("trxId"), idx)) {
							msgBody = "VA_TRX UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
							
							logger.info(msgBody);
						}
						
						if(!dao.updateRefIdUpdate2(data.getString("trxId"), idx)) {
							msgBody = "VA_TRX_FIRM UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
							
							logger.info(msgBody);
						}
					}else {
						String orgSeq = "";
						
						orgSeq = dao.getSeqNo(data.getString("trxId"));
						
						logger.info("가상계좌 결과확인 출금 : [{}][{}][{}]", data.getString("trxId"), data.getString("retry"), orgSeq);
						
						//출금 실패한 건들은 결과확인
						//운영
						firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).resultCheck("089", orgSeq);
						//테스트
//						firmBean = new FirmBean();
//						firmBean.resultCd = "XXXX";
//						firmBean.resultMsg = "실패";
						
						if(!firmBean.resultCd.equals("0000") ) {
							msgBody = "결과확인 실패. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
							logger.info(msgBody);
							errFlag = true;
						}else {
							status = "완료";
							msgBody = "결과확인 성공. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
							logger.info(msgBody);
						}
					}
					
					//실시간출금 결과 저장
					if(!dao.updatePayOutRes(data.getString("trxId"), firmBean.resultCd, firmBean.resultMsg, status)) {
						msgBody = "VA_TRX_FIRM UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
						
						logger.info(msgBody);
						smsGw.sendMessage("0", "4", msgBody);
					}
					
					if(errFlag) {
						String sendCnt = dao.getRetry(data.getString("trxId"));
						
						//출금 실패시 출금 전송 횟수가 3회일 경우 SMS 알림 발송
						if(!"".equals(sendCnt)) {
							errCnt = Integer.parseInt(sendCnt);
							
							//3회 실패 시 
							if(errCnt == 3) {
								//펌에러 테이블에 저장 
								SharedMap<String, Object> errData = getTrxErrData(data);
								dao.insertTrxErr(errData);
								
								if(!firmBean.resultCd.equals("XXXX") && !firmBean.resultCd.equals("")) {
									//실패거래건의 실출금액 조회
									String stlAmt = dao.getStlAmt(data.getString("trxId"));
									//실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌 
									dao.updateVaBalance(data.getString("trxId"), data.getString("id"), stlAmt);
									//실패건 VA_TRX테이블에서 삭제
									dao.deleteVaTrx(data.getString("trxId"));
								}
								
								msgBody = "가상계좌 충전 출금 " + errCnt + "회 실패. 확인요망 [" + data.getString("trxId") + "][" + firmBean.resultCd + "][" + firmBean.resultMsg + "]";
								logger.info(msgBody);
								
								if(!firmBean.resultCd.equals("K409") ) {
									smsGw.sendMessage("0", "4", msgBody);
								}
							}
						}
					}
				}
				logger.info("가상계좌 충전 출금 END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "가상계좌 충전 출금 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	/**
	 * VA_TRX_ERR 테이블에 넣을 데이터 세팅
	 * @param data
	 * @return
	 */
	private SharedMap<String,Object> getTrxErrData(SharedMap<String,Object> data) {
		VaPayOutDAO dao = new VaPayOutDAO();
		String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		
		SharedMap<String, Object> trxMap = dao.getVaTrx(data.getString("trxId"));
		data.put("trxType",trxMap.getString("trxType"));
		data.put("trxUnit",trxMap.getString("trxUnit"));
		data.put("feeType", trxMap.getString("feeType"));
		data.put("feeRate", trxMap.getDouble("feeRate"));
		data.put("fee", trxMap.getLong("fee"));
		data.put("feeVat", trxMap.getLong("feeVat"));
		data.put("ptnFeeRate", trxMap.getDouble("ptnFeeRate"));
		data.put("ptnFee", trxMap.getLong("ptnFee"));
		data.put("ptnFeeVat", trxMap.getLong("ptnFeeVat"));
		data.put("bankFee", trxMap.getLong("bankFee"));
		data.put("stlAmount", trxMap.getLong("stlAmount"));
		data.put("balance", trxMap.getLong("balance"));
		data.put("trackId", trxMap.getString("trackId"));
		data.put("refId", data.getString("refId"));
		data.put("bankCd", data.getString("bankCd"));
		data.put("account", data.getString("account"));
		data.put("holder", data.getString("holder"));
		data.put("regDay", regDate.substring(0, 8));
		data.put("resultCd", data.getString("resultCd"));
		data.put("resultMsg", data.getString("resultMsg"));
		
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
	
	/**
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            // 프로퍼티 파일 위치
    		//운영
            String propFile = "/home/MARU/MARU_DAEMON/conf/firmconfig.properties"; 
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
