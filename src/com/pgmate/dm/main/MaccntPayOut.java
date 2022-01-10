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
public class MaccntPayOut {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.MaccntPayOut.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
	
	private String firmServer = "pgwas2";
	private int frimPort = 10028;
	private int firmTimeOut = 70000;
	private int firmStartTime = 3000; //출금 시작 시간
	private int firmEndTime = 233000; //출금 중지 시간 
	
	private String firmBankCd = "";
	private String firmAccntNo = "";
	
	public static void main(String[] args){
		new MaccntPayOut();
	}
	
	public MaccntPayOut() {
		logger.info("==================================================");
		logger.info("MaccntPayOut Strart");
		smsGw = new SmsGw();
		
		configSetting();
		mAccntPayOut();
		logger.info("MaccntPayOut End");
		logger.info("==================================================");
	}
	
	public void mAccntPayOut(){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();

		try {
			int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
			
			//매일 23:30~00:30분까지는 은행 점검시간이라서 출금기능 막음
			if(currentTime > firmEndTime || currentTime < firmStartTime) {
				logger.info("- -- --- ---- ---- ---- 실시간 출금 서비스 가능한 시간이 아닙니다. ---- ---- ---- --- -- -");
				return;
			}
			
			//이체할 모계좌정보 검색
			List<SharedMap<String,Object>> getMaccntList = dao.getMaccntList();
			logger.info("getMaccntList COUNT : {}", getMaccntList.size());
			
			if(getMaccntList.size() > 0) {
				logger.info("계좌잔액이체 시작");
				
				for(SharedMap<String,Object> data : getMaccntList){
					FirmBean firmBean = new FirmBean();
					
					long amount = 999000000;
					msgBody = "";
					
					logger.info("==================================================");
					logger.info("모계좌 잔액조회");
					logger.info("모계좌 계좌번호  	: {}",data.getString("mAccntNo"));
					logger.info("모계좌 은행코드  	: {}",data.getString("mBankCd"));
					logger.info("모계좌 은행명  	: {}",data.getString("mBankName"));
					logger.info("모계좌 업체코드  	: {}",data.getString("compCd"));
					logger.info("==================================================");
					
					//잔액조회
					firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).balance(data.getString("mBankCd"), data.getString("mAccntNo").replace("-", "").trim(), data.getString("compCd"));
					
					if(firmBean.resultCd.equals("0000") ) {
						logger.info("계좌잔액조회 성공 [{}][{}원]", data.getString("mAccntNo"), firmBean.data.getLong("amount"));
						
						logger.info("==================================================");
						logger.info("모계좌 잔액이체");
						
						if(firmBean.data.getLong("amount") >= 1000000000) {
							logger.info("잔액 10억 초과발생 : [{}]원", firmBean.data.getLong("amount"));
						}else {
							amount = firmBean.data.getLong("amount");
						}
						
						logger.info("계좌잔액이체 : [{}][{}][{}][{}][{}원]", data.getString("mBankCd"), data.getString("mAccntNo"), firmBankCd, firmAccntNo, amount);
						//잔액이체
						firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).accountTransfer(data.getString("mBankCd"), data.getString("mAccntNo").replace("-", "").trim(), firmBankCd, firmAccntNo, amount, "", "BT");
						
						if(firmBean.resultCd.equals("0000") ) {
							msgBody = "계좌잔액이체 성공 : " + data.getString("mAccntNo") + " 계좌에서 " + firmAccntNo + " 계좌로 " + amount + "원 이체성공";
						}else {
							msgBody = "계좌잔액이체 실패 - 계좌번호 : [" + data.getString("mAccntNo") + "], 응답코드 : [" + firmBean.resultCd + "], 응답메세지 : [" + firmBean.resultMsg + "]";
							
							smsGw.sendMessage("0", "4", msgBody);
						}
						
						logger.info(msgBody);
					}else {
						logger.info("계좌잔액조회 실패 - mAccntNo : [" + data.getString("mAccntNo") + "], resultCd : [" + firmBean.resultCd + "]");
					}
					
					logger.info("==================================================");
				}
				logger.info("계좌잔액이체 종료");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "모계좌이체 오류발생. 확인요망 [" + e.getMessage() + "]";
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

            String strFirmBankCd = props.getProperty("firm_bankCd");
            String strFirmAccntNo = props.getProperty("firm_accntNo");
            String strFirmTimeOut = props.getProperty("firm_timeout");
            String strStartTime = props.getProperty("firm_starttime");
            String strEndTime = props.getProperty("firm_endtime");
            
            if(strFirmBankCd != null && !"".equals(strFirmBankCd)) {
            	firmBankCd = strFirmBankCd;
            }
            
            if(strFirmAccntNo != null && !"".equals(strFirmAccntNo)) {
            	firmAccntNo = strFirmAccntNo;
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
}
