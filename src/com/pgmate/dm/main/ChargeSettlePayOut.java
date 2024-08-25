package com.pgmate.dm.main;

import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;

import com.pgmate.dm.dao.FirmFailCheckDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.dm.dao.VaPayOutDAO;
import com.pgmate.dm.util.FirmClient;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class ChargeSettlePayOut {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.ChargeSettlePayOut.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
	
	private String firmServer = "10.100.200.10";
	private String compNm = "";
	private int frimPort = 10006;
	private int firmTimeOut = 70000;
	private int firmStartTime = 3000; //출금 시작 시간
	private int firmEndTime = 233000; //출금 중지 시간 
    
	public static void main(String[] args){
		new ChargeSettlePayOut();
	}
	
	public ChargeSettlePayOut() {
		logger.info("==================================================");
		logger.info("ChargeSettleFirm Strart");
		smsGw = new SmsGw();
		
		configSetting();
		chargeSettleFirm();
		logger.info("ChargeSettleFirm End");
		logger.info("==================================================");
	}
	
	public void chargeSettleFirm(){
		ChargeSettlePayOutDAO dao = new ChargeSettlePayOutDAO();
		
		try {
			int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
			
			//매일 23:30~00:30분까지는 은행 점검시간이라서 출금기능 막음
			if(currentTime > firmEndTime || currentTime < firmStartTime) {
				logger.info("- -- --- ---- ---- ---- 출금 서비스 가능한 시간이 아닙니다. ---- ---- ---- --- -- -");
				return;
			}
			
			//충전정산 출금대상거래 에서 출금하지 않은 데이터중에 전송시도가 남은 거래건들 조회
			List<SharedMap<String,Object>> firmList = dao.getChargeSettleFirmList();
			logger.info("chargeSettle firmList COUNT : {}", firmList.size());
			
			
			if(firmList.size() > 0) {
				logger.info("==================================================");
				logger.info("충전정산 출금 START");
				
				for(SharedMap<String,Object> data : firmList){
					dao.updateStatus(data.getString("trxId"));
				}
				
				for(SharedMap<String,Object> data : firmList){
					int errCnt = 0;
					boolean errFlag = false;
					String status = "실패";
					FirmBean firmBean = new FirmBean();
					String idx = "";
					msgBody = "";
					compNm = "";
					//PYS : 어느 은행에서 돈을 이체할것인가
					String vactBankCd = data.getString("vactBankCd");

					SharedMap<String, Object> chargeMngMap = dao.getMchtChargeMng(data.getString("mchtId"));
					
					if("0".equals(data.getString("retry"))){
						logger.info("충전정산 출금 : [{}][{}]", data.getString("trxId"), data.getString("retry"));
						
						if(data.getString("recordInfo") != null && !"".equals(data.getString("recordInfo"))) {
							compNm = data.getString("recordInfo");
						}
						
						//최초 1회 출금요청
						//출금요청
						//운영

						//PYS : 가상계좌은행 입력하게 변경
						firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).transfer(vactBankCd, data.getString("bankCd"), data.getString("decAccount").replace("-", "").trim(), data.getLong("amount"), data.getString("trxId"), compNm, "CS");

						//테스트
//						firmBean = new FirmBean();
//						firmBean.resultCd = "XXXX";
//						firmBean.resultMsg = "실패";
//						firmBean.idx = dao.insertTrx("020",data.getLong("amount"),data.getString("bankCd"),data.getString("decAccount").replace("-", "").trim(),"테스트", data.getString("trxId"));
						
						idx = String.valueOf(firmBean.idx);
						
						if("".equals(idx) || "0".equals(idx)) {
							idx = dao.getIdx(data.getString("trxId"));
							
							logger.info("idx 재검색 : [{}][{}]", data.getString("trxId"), idx);
						}
						
						if(!firmBean.resultCd.equals("0000") ) {
							msgBody = "충전정산 출금 실패. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
							logger.info(msgBody);
							//errFlag = true;
							//펌에러 테이블에 저장
							SharedMap<String, Object> errData = dao.getChargeSettle(data.getString("trxId"));
							errData.put("refId", idx);
							errData.put("resultCd", firmBean.resultCd);
							errData.put("resultMsg", firmBean.resultMsg);
							String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
							errData.put("regDay", regDate.substring(0, 8));
							dao.insertTrxErr(errData);

							if (!firmBean.resultCd.equals("XXXX") && !firmBean.resultCd.equals("")) {
								logger.info("===========================");
								logger.info("충전정산 잔액 복구 로직 실행");
								logger.info("가맹점 ID : {}", data.getString("mchtId"));
								logger.info("복구금액 : {}", errData.getLong("netAmount"));
								logger.info("===========================");

								//실패거래건의 실출금액 조회
								long netAmt = errData.getLong("netAmount");
								//실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌
								dao.updateChargeSettleBalance(data.getString("trxId"), data.getString("mchtId"), netAmt);
								//실패건 PG_CHARGE_SETTLE 테이블에서 삭제
								dao.deleteChargeSettle(data.getString("trxId"));

								logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
								// 출금 실패결과 noti 발송
								if (!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
									String payLoad = setPayLoad(data, "출금실패", firmBean.resultCd, firmBean.resultMsg);
									data.put("payLoad", payLoad);
									data.put("trxType", "출금실패");
									new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
								}
							}
						}else {
							status = "완료";
							msgBody = "충전정산 출금 성공. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
							logger.info(msgBody);
							
							// 출금 완료 결과 noti 발송
							if(!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
								String payLoad = setPayLoad(data, "출금완료", firmBean.resultCd, firmBean.resultMsg);
								data.put("payLoad", payLoad);
								data.put("trxType", "출금");
								new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
							}
						}
						
						if(!dao.updateRefIdUpdate(data.getString("trxId"), idx)) {
							msgBody = "PG_CHARGE_SETTLE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
							
							logger.info(msgBody);
						}
						
						if(!dao.updateRefIdUpdate2(data.getString("trxId"), idx)) {
							msgBody = "PG_CHARGE_SETTLE_FIRM UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
							
							logger.info(msgBody);
						}
					} else if("1".equals(data.getString("retry")) {
						String orgSeq = "";
						
						orgSeq = dao.getSeqNo(data.getString("trxId"));
						
						logger.info("충전정산 결과확인 출금 : [{}][{}][{}]", data.getString("trxId"), data.getString("retry"), orgSeq);

						firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).resultCheck(vactBankCd, orgSeq);

						if(vactBankCd.equals("048") {
							logger.info("==재확인 체크==");
							logger.info("resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]");
						
						}
					}
					else {
						String orgSeq = "";
						
						orgSeq = dao.getSeqNo(data.getString("trxId"));
						
						logger.info("충전정산 결과확인 출금 : [{}][{}][{}]", data.getString("trxId"), data.getString("retry"), orgSeq);
						
						//출금 실패한 건들은 결과확인
						//운영
						//PYS : 가상계좌은행 입력하게 변경
						firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).resultCheck(vactBankCd, orgSeq);

						//테스트
//						firmBean = new FirmBean();
//						firmBean.resultCd = "0000";
//						firmBean.resultMsg = "처리완료";

						//230405_PYS : 거래없음일때 출금 재시도 로직 추가
						//230913_PYS : 광주은행 예외추가
						if(vactBankCd.equals("034") || vactBankCd.equals("007")) {
							if(firmBean.resultCd.equals("0000")) {
								status = "완료";
								msgBody = "충전정산 결과확인 성공. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
								logger.info(msgBody);

								// 출금완료 결과 noti 발송
								if(!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
									String payLoad = setPayLoad(data, "출금완료", firmBean.resultCd, firmBean.resultMsg);
									data.put("payLoad", payLoad);
									data.put("trxType", "출금");
									new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
								}
							}else if(firmBean.resultCd.equals("VTIM") || firmBean.resultCd.equals("0011")) {
								logger.info("더즌 타임아웃, 이중송금방지, 한번더 실행");
								errFlag = true;

							}else {
								msgBody = "충전정산 결과확인 실패. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
								logger.info(msgBody);
								errFlag = true;
							}

						}
						else {
							if( firmBean.resultCd.equals("KS10")) {
								logger.info("충전정산 출금 재시도 : [{}]", data.getString("trxId"));
								firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).reTransfer(vactBankCd, data.getString("trxId"));
								logger.info("결과메세지 체크 : {}", firmBean.resultMsg);
								//PYS : 결과메세지 제대로 나오면 아래 로직은 삭제하는걸로.
								if(firmBean.resultCd.startsWith("KS")) {
									firmBean.resultMsg = FirmFailCheckDAO.getResultMsg("ERR", firmBean.resultCd);
								} else {
									firmBean.resultMsg = FirmFailCheckDAO.getResultMsg(firmBean.bankCd, firmBean.resultCd);
								}

								//위에 있는 출금로직 복붙
								idx = String.valueOf(firmBean.idx);

								if("".equals(idx) || "0".equals(idx)) {
									idx = dao.getIdx(data.getString("trxId"));

									logger.info("idx 재검색 : [{}][{}]", data.getString("trxId"), idx);
								}

								if(!firmBean.resultCd.equals("0000") ) {
									msgBody = "충전정산 재시도 출금 실패. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
									logger.info(msgBody);
									errFlag = true;
								}else {
									status = "완료";
									msgBody = "충전정산 재시도 출금 성공. trxId : [" + data.getString("trxId") + "], id : [" + data.getString("mchtId") + "], idx : [" + idx + "]";
									logger.info(msgBody);

									// 출금 완료 결과 noti 발송
									if(!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
										String payLoad = setPayLoad(data, "출금완료", firmBean.resultCd, firmBean.resultMsg);
										data.put("payLoad", payLoad);
										data.put("trxType", "출금");
										new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
									}
								}

								if(!dao.updateRefIdUpdate(data.getString("trxId"), idx)) {
									msgBody = "PG_CHARGE_SETTLE UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

									logger.info(msgBody);
								}

								if(!dao.updateRefIdUpdate2(data.getString("trxId"), idx)) {
									msgBody = "PG_CHARGE_SETTLE_FIRM UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";

									logger.info(msgBody);
								}
							}else {
								//PYS : KS10이 아닐땐 기존 로직실행
								if(!firmBean.resultCd.equals("0000") ) {
									msgBody = "충전정산 결과확인 실패. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
									logger.info(msgBody);
									errFlag = true;
								}else {
									status = "완료";
									msgBody = "충전정산 결과확인 성공. trxId : [" + data.getString("trxId") + "], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
									logger.info(msgBody);

									// 출금완료 결과 noti 발송
									if(!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
										String payLoad = setPayLoad(data, "출금완료", firmBean.resultCd, firmBean.resultMsg);
										data.put("payLoad", payLoad);
										data.put("trxType", "출금");
										new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
									}
								}
							}
						}
					}
					
					//실시간출금 결과 저장
					if(!dao.updatePayOutRes(data.getString("trxId"), firmBean.resultCd, firmBean.resultMsg, status)) {
						msgBody = "PG_CHARGE_SETTLE_FIRM UPDATE 실패. 확인요망 [" + data.getString("trxId") + "]";
						
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
								//230622 타행이체불능 에러 처리 안되어있을 때 로직 수행
								if(dao.getChargeErrCount(data.getString("trxId")) == 0) {
									//펌에러 테이블에 저장
									SharedMap<String, Object> errData = dao.getChargeSettle(data.getString("trxId"));
									errData.put("refId", data.getString("refId"));
									errData.put("resultCd", data.getString("resultCd"));
									errData.put("resultMsg", data.getString("resultMsg"));
									String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
									errData.put("regDay", regDate.substring(0, 8));
									dao.insertTrxErr(errData);

									if (!firmBean.resultCd.equals("XXXX") && !firmBean.resultCd.equals("")) {
										logger.info("===========================");
										logger.info("충전정산 잔액 복구 로직 실행");
										logger.info("가맹점 ID : {}", data.getString("mchtId"));
										logger.info("복구금액 : {}", errData.getLong("netAmount"));
										logger.info("===========================");

										//실패거래건의 실출금액 조회
										long netAmt = errData.getLong("netAmount");
										//실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌
										dao.updateChargeSettleBalance(data.getString("trxId"), data.getString("mchtId"), netAmt);
										//실패건 PG_CHARGE_SETTLE 테이블에서 삭제
										dao.deleteChargeSettle(data.getString("trxId"));

										logger.debug("hookAddr [{}]", chargeMngMap.getString("hookAddr"));
										// 출금 실패결과 noti 발송
										if (!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
											String payLoad = setPayLoad(data, "출금실패", firmBean.resultCd, firmBean.resultMsg);
											data.put("payLoad", payLoad);
											data.put("trxType", "출금실패");
											new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, "0").start();
										}
									}

									msgBody = "충전정산 출금 " + errCnt + "회 실패. 확인요망 [" + data.getString("trxId") + "][" + firmBean.resultMsg + "]";

									logger.info(msgBody);
									smsGw.sendMessage("0", "4", msgBody);
								}
								
							}
						}
					}
				}
				logger.info("충전정산 출금 END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "충전정산 출금 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
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
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
    
    /*
     * 펌뱅킹 노티 데이터 설정
     */
    public String setPayLoad(SharedMap<String, Object> sharedMap, String status, String resultCd, String resultMsg){
		SharedMap<String, String> payLoadMap = new SharedMap<String, String>();
		payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
		payLoadMap.put("trxId",sharedMap.getString("trxId"));
		payLoadMap.put("trxDay",CommonUtil.getCurrentDate("yyyyMMdd"));
		payLoadMap.put("trxTime",CommonUtil.getCurrentDate("HHmmss"));
		payLoadMap.put("status",status);
		payLoadMap.put("trackId",sharedMap.getString("trackId"));
		payLoadMap.put("resultCd",resultCd);
		payLoadMap.put("resultMsg",resultMsg);
		payLoadMap.put("amount",sharedMap.getString("amount"));
		String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
		return payLoad;
	}
}
