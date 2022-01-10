package com.pgmate.dm.main;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class DailySettlePayOut {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.DailySettlePayOut.class );
	private SmsGw smsGw = null;
	
	private String firmServer = "pgwas2";
	private int frimPort = 10026;
	private int firmTimeOut = 35000;
	
	private String msgBody = "";
	private String stlDay = "";
	private String stlType = "";
	private String hour = "";

	HashMap<String,String> idMap = new HashMap<String,String>();
	
	public static void main(String[] args){
		new DailySettlePayOut(args);
	}
	
	public DailySettlePayOut(String[] args) {
		logger.info("==================================================");
		logger.info("DailySettlePayOut Strart");
		smsGw = new SmsGw();
		
		configSetting();
		dailySettlePayOut(args);
		
		logger.info("DailySettlePayOut End");
		logger.info("==================================================");
	}
	
	public void dailySettlePayOut(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {
			stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
			stlType = "A+0";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			hour = CommonUtil.getCurrentDate("HH");
			
			logger.info("당일정산 출금 stlDay : " + stlDay + ", stlType : " + stlType + ", hour : " + hour);
			
			List<SharedMap<String,Object>> getAutoPayOutList = dao.getAutoPayOutList(stlDay, stlType);
			logger.info("당일정산 출금대상 건수 : {}", getAutoPayOutList.size());
			
			if(getAutoPayOutList.size() > 0) {
				logger.info("==================================================");
				logger.info("당일정산 출금 START");
				
				for(SharedMap<String,Object> data : getAutoPayOutList){
					boolean errFlag = false;
					String stlStatus = "지급완료";
					String sendCheck = "N";
					String resCd = "";
					String resMsg = "";
					boolean skip = false;
					
					msgBody = "";
					
					SharedMap<String,Object> mchtTaxMap	= dao.getMchtTaxByMchtId(data.getString("mchtId"));
					
					if(data.getLong("payOutAmount") > 0) {
						//당일정산 출금요청
						//운영
						FirmBean firmBean = new FirmClient(firmServer, frimPort, firmTimeOut).transfer("089", mchtTaxMap.getString("bankCd"), mchtTaxMap.getString("account").replace("-", "").trim(), data.getLong("payOutAmount"), data.getString("stlId"), "", "AS");
						//테스트
						//FirmBean firmBean = new FirmBean();
						//firmBean.resultCd = "0000";
						//firmBean.resultMsg = "성공";
						
						resCd = firmBean.resultCd;
						resMsg = firmBean.resultMsg;
						
						if(!resCd.equals("0000") ) {
							msgBody = "당일정산 출금 실패. stlId : [" + data.getString("stlId") + "], mchtId : [" + data.getString("mchtId") + "][" + resMsg + "]";
							logger.info(msgBody);
							stlStatus = "지급실패";
							errFlag = true;
						}else {
							sendCheck = "Y";
							msgBody = "당일정산 출금 성공. stlId : [" + data.getString("stlId") + "], mchtId : [" + data.getString("mchtId") + "], payOutAmount : [" + data.getLong("payOutAmount") + "]";
							logger.info(msgBody);
						}
					} else if(data.getLong("payOutAmount") <= 0) {
						resCd = "0000";
						resMsg = "0원 업데이트";
					} else {
						skip = true;
					}
					
					if(!skip) {
						//자동정산출금 결과 저장
						if(!dao.updateAutoPayOutRes(data.getString("stlId"), stlStatus, CommonUtil.getCurrentDate("yyyyMMdd"), CommonUtil.getCurrentDate("HHmmss"), mchtTaxMap.getString("bankCd"), 
											    mchtTaxMap.getString("bankName"), dao.getAESEnc(mchtTaxMap.getString("account")), dao.getAESEnc(mchtTaxMap.getString("accntHolder")), resCd, resMsg, sendCheck)) {
							msgBody = "당일정산 PG_SETTLE_AUTO UPDATE 실패. 확인요망 [" + data.getString("stlId") + "]";
							
							logger.info(msgBody);
							smsGw.sendMessage("0", "4", msgBody);
						}
						
						if("C".equals(data.getString("payType"))){
							//자동정산출금 결과 매입테이블 업데이트
							if(!dao.updateDailyPayOutCap(data.getString("stlDay"), data.getString("stlType"), data.getString("mchtId"), CommonUtil.getCurrentDate("yyyyMMdd"),stlStatus, hour)) {
								msgBody = "당일정산 PG_TRX_CAP_DTL UPDATE 실패. 확인요망 [" + data.getString("mchtId") + "]";
								
								logger.info(msgBody);
								smsGw.sendMessage("0", "4", msgBody);
							}
						}

						if(errFlag) {
							smsGw.sendMessage("0", "4", msgBody);
						}
					}
				}
				
				//매입데이터에 정산번호 업데이트
				for(Map.Entry<String, String> stlData : idMap.entrySet()) {
					dao.updateDailyStlIdCap(stlData.getKey(), stlData.getValue());
					
					msgBody = "당일정산 출금 매입내역 업데이트 - capId : [" + stlData.getKey() + "], stlId : [" + stlData.getValue() + "]";
					logger.info(msgBody);
				}
				
				logger.info("당일정산 출금 END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "당일정산 오류발생. 확인요망 [" + e.getMessage() + "]";
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
            String propFile = "/home/MARU/MARU_DAEMON/conf/firmconfig.properties"; 
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
            
            if(strFirmPort != null && !"".equals(strFirmPort)) {
            	frimPort = Integer.parseInt(strFirmPort);
            }
            
            if(strFirmTimeOut != null && !"".equals(strFirmTimeOut)) {
            	firmTimeOut = Integer.parseInt(strFirmTimeOut);
            }        
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
