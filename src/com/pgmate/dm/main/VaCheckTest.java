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
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class VaCheckTest {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VaCheckTest.class );
	private String msgBody = "";
	
	private String firmServer = "pgwas2";
	private int frimPort = 10026;
	private int firmTimeOut = 70000;
    
	public static void main(String[] args){
		new VaCheckTest(args[0]);
	}
	
	public VaCheckTest(String seqNo) {
		logger.info("==================================================");
		logger.info("VaCheckTest Strart");
		
		configSetting();
		vaCheckTest(seqNo);
		logger.info("VaCheckTest End");
		logger.info("==================================================");
	}
	
	public void vaCheckTest(String seqNo){
		VaPayOutDAO dao = new VaPayOutDAO();
		
		try {
			logger.info("==================================================");
			logger.info("펌 결과확인 START");
			
			FirmBean firmBean = new FirmBean();
			msgBody = "";

			//출금 실패한 건들은 결과확인
			firmBean = new FirmClient(firmServer, 10026, firmTimeOut).resultCheck("089", seqNo);
			
			if(firmBean.resultCd.equals("0000") ) {
				msgBody = "결과확인 성공. trxId : [T210317518382], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
				logger.info(msgBody);
			}else {
				msgBody = "결과확인 실패. trxId : [T210317518382], resultCd : [" + firmBean.resultCd + "], resultMsg : [" + firmBean.resultMsg + "]";
				logger.info(msgBody);
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			
			msgBody = "펌 결과확인 오류발생. 확인요망 [" + e.getMessage() + "]";
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
            
            if(strFirmPort != null && "".equals(strFirmPort)) {
            	frimPort = Integer.parseInt(strFirmPort);
            }
            
            if(strFirmTimeOut != null && "".equals(strFirmTimeOut)) {
            	firmTimeOut = Integer.parseInt(strFirmTimeOut);
            }
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
