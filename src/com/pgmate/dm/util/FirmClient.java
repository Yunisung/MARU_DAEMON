package com.pgmate.dm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class FirmClient {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.util.FirmClient.class );
	private static String host 	= "pgwas3";
	private static int port 	= 10006;
	private static int timeout  = 35000;
	
	public FirmClient(String firmServer, int frimPort, int firmTimeOut) {
		host = firmServer;
		port = frimPort;
		timeout = firmTimeOut;
	}

	public FirmBean vactUnReg(String vactBankCd, String companyCd, String virtualAccount, String bankCd, String account, String holderName, String regType, String identity) {
		FirmBean firmBean = new FirmBean();

		firmBean.bankCd     = vactBankCd;
		firmBean.msgType    = "0900400";
		firmBean.userId	    = "SYSTEM";

		firmBean.data.put("companyCd", companyCd);
		firmBean.data.put("virtualAccount", virtualAccount);
		firmBean.data.put("withdrawBankCd", bankCd);
		firmBean.data.put("withdrawAccount", account);
		firmBean.data.put("customerName", holderName);

		if(vactBankCd.equals("089")) {
			firmBean.data.put("trxType", "2");
		}else if(vactBankCd.equals("039")) {
			firmBean.data.put("trxType", "3");
			firmBean.data.put("regType", regType);
			firmBean.data.put("identity", identity);
		}else if(vactBankCd.equals("007")) {
			firmBean.data.put("trxType", "3");
		}




		firmBean = comm(firmBean);

		logger.info("vactUnReg 응답 : [{}][{}]", firmBean.resultCd, firmBean.resultMsg);
		logger.info("vactUnReg data : [{}]", GsonUtil.toJson(firmBean.data));

		return firmBean;
	}

	/**
	 * 은행통한 예금주조회
	 * @param bankCd
	 * @param userBankCd
	 * @param userAccount
	 */
	public FirmBean holderFCS(String userBankCd,String userAccount){
		logger.info("예금주조회");
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= "099";
		firmBean.msgType 	= "0600400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("bankCd", userBankCd);
		firmBean.data.put("account", userAccount);
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.get("resData"));
		logger.info("name : {}",firmBean.data.getString("name"));
		return firmBean;
	}
	
	/**
	 * fcsCheck : (6)	신원확인번호 체크 : 계좌번호+신원확인번호 일치 여부 체크시 ‘99’ 세팅
	 * 예금주명+신원확인번호 일치 여부 체크시 ‘77’ 세팅 (실명 인증)
	 * @param userBankCd
	 * @param userAccount
	 * @param holder
	 * @param socialNumber
	 * @param fcsCheck
	 */
	public FirmBean holderFCS(String userBankCd,String userAccount,String holder,String socialNumber,String fcsCheck){
		logger.info("예금주조회");
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= "099";
		firmBean.msgType 	= "0600400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("bankCd", userBankCd);
		firmBean.data.put("account", userAccount);
		firmBean.data.put("name", holder);
		firmBean.data.put("socialNumber", socialNumber);
		firmBean.data.put("socialCheck", fcsCheck);
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.get("resData"));
		logger.info("name : {}",firmBean.data.getString("name"));
		return firmBean;
	}
	
	/**
	 * 잔액조회
	 * @param bankCd
	 * @param accntNo
	 * @return
	 */
	public FirmBean balance(String bankCd, String accntNo, String compCd){
		logger.info("잔액조회");
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= bankCd;
		firmBean.msgType 	= "0600300";
		firmBean.userId		= "SYSTEM";
		
		firmBean.data.put("mAccnt",accntNo);
		firmBean.data.put("compCd",compCd);
		
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.get("resData"));
		logger.info("name : {}",firmBean.data.getString("name"));
		return firmBean;
	}

	/**
	 * 모계좌 잔액송금
	 * @param bankCd
	 * @param recvBankCd
	 * @param recvAccount
	 * @param amount
	 * @param trxId
	 * @param sender
	 * @return
	 */
	public FirmBean accountTransfer(String bankCd, String account, String recvBankCd,String recvAccount,long amount, String sender, String type){
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= bankCd;
		firmBean.msgType 	= "0100100";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("amount",amount);
		firmBean.data.put("sendBankCd",bankCd);
		firmBean.data.put("sendAccount",account);
		firmBean.data.put("recvBankCd",recvBankCd);
		firmBean.data.put("recvAccount",recvAccount);
		firmBean.data.put("procType", type);
		firmBean.data.put("sender",sender);

		firmBean = comm(firmBean);
		
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.getLong("balance"));
		logger.info("data : {}",GsonUtil.toJson(firmBean.data));
		return firmBean;
	}
	
	/**
	 * 실시간 출금
	 * @param bankCd
	 * @param recvBankCd
	 * @param recvAccount
	 * @param amount
	 * @param trxId
	 * @param sender
	 * @return
	 */
	public FirmBean transfer(String bankCd, String recvBankCd,String recvAccount,long amount,String trxId, String sender, String type){
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= bankCd;
		firmBean.msgType 	= "0100100";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("amount",amount);
		firmBean.data.put("recvBankCd",recvBankCd);
		firmBean.data.put("recvAccount",recvAccount);
		firmBean.data.put("recordInfo",trxId);
		firmBean.data.put("procType", type);
		
		if(!"".equals(sender)) {
			firmBean.data.put("sender",sender);
		}

		if("VA".equals(type)) {
			firmBean = vaPayOutComm(firmBean);
		}else if("CS".equals(type)) {
			firmBean = chargeComm(firmBean);
		}else {
			firmBean = comm(firmBean);
		}
		
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.getLong("balance"));
		logger.info("data : {}",GsonUtil.toJson(firmBean.data));
		return firmBean;
	}
	
	/**
	 * 처리결과 조회
	 * @param bankCd
	 * @param orgSeqNo
	 * @return
	 */
	public FirmBean resultCheck(String bankCd, String orgSeqNo){
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= bankCd;
		firmBean.msgType 	= "0600101";
		firmBean.userId		= "SYSTEM";
		
		firmBean.data.put("orgSeqNo",orgSeqNo);
	
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("data : {}",GsonUtil.toJson(firmBean.data));
		return firmBean;
	}

	public FirmBean reTransfer(String vactBankCd, String trxId) {
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= vactBankCd;
		firmBean.msgType 	= "0600102";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("trxId",trxId);

		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("data : {}",GsonUtil.toJson(firmBean.data));
		return firmBean;
	}
	
	/**
	 * 실시간 출금 통신
	 * @param firmBean
	 * @return
	 */
	public FirmBean comm(FirmBean firmBean){
		Socket socket = null;
		OutputStream output = null;
		InputStream input = null;
		String reqJson = GsonUtil.toJson(firmBean);
		String resJson = "";
		long time = System.currentTimeMillis();
		try{
			socket = new Socket(host, port);
			socket.setSoTimeout(timeout);
			
			output = socket.getOutputStream();
			output.write(reqJson.getBytes(Charset.forName("EUC-KR")));
			output.flush();
			
			input = socket.getInputStream();
		
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int bcount = 0;
			byte[] buf = new byte[2048];
			int read_retry_count = 0;
			while(true) {
				int n = input.read(buf);
			    if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
			    else if (n == -1) break;
			    else  { // n == 0
			if (++read_retry_count >= 5)
			  throw new IOException("inputstream-read-retry-count(5) exceed !");
			    }
			    if(input.available() == 0){ break; }
			}
			bout.flush();
			byte[] res = bout.toByteArray();
			bout.close();
			resJson = new String(res,"MS949");
			if(!CommonUtil.isNullOrSpace(resJson)) {
				firmBean = (FirmBean)GsonUtil.fromJson(resJson, FirmBean.class);
			}else {
				throw new Exception("서버응답없음");
			}
			
		}catch(Exception e){
			firmBean.resultCd = "XXXX";
			firmBean.resultMsg = "펌뱅킹 시스템과의 통신장애 :"+e.getMessage();
			logger.info(firmBean.resultMsg);
		}finally{
			logger.info("-> FIRM : [{}]",reqJson);
			logger.info("<- FIRM : [{}],{}",resJson,(System.currentTimeMillis()-time));
			
			try{
				if(input != null){ input.close();}
				if(output != null){ output.close();}
				if(socket != null){ socket.close();}
			}catch(Exception ex){
				
			}
		}
		
		return firmBean;
	}
	
	public FirmBean chargeComm(FirmBean firmBean){
		Socket socket = null;
		OutputStream output = null;
		InputStream input = null;
		String reqJson = GsonUtil.toJson(firmBean);
		String resJson = "";
		long time = System.currentTimeMillis();
		try{
			socket = new Socket(host, port);
			socket.setSoTimeout(timeout);
			
			output = socket.getOutputStream();
			output.write(reqJson.getBytes(Charset.forName("MS949")));
			output.flush();
			
			input = socket.getInputStream();
		
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int bcount = 0;
			byte[] buf = new byte[2048];
			int read_retry_count = 0;
			while(true) {
				int n = input.read(buf);
			    if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
			    else if (n == -1) break;
			    else  { // n == 0
			if (++read_retry_count >= 5)
			  throw new IOException("inputstream-read-retry-count(5) exceed !");
			    }
			    if(input.available() == 0){ break; }
			}
			bout.flush();
			byte[] res = bout.toByteArray();
			bout.close();
			resJson = new String(res,"MS949");
			if(!CommonUtil.isNullOrSpace(resJson)) {
				firmBean = (FirmBean)GsonUtil.fromJson(resJson, FirmBean.class);
			}else {
				throw new Exception("서버응답없음");
			}
			
		}catch(Exception e){
			firmBean.resultCd = "XXXX";
			firmBean.resultMsg = "펌뱅킹 시스템과의 통신장애 :"+e.getMessage();
			logger.info(firmBean.resultMsg);
		}finally{
			logger.info("-> FIRM : [{}]",reqJson);
			logger.info("<- FIRM : [{}],{}",resJson,(System.currentTimeMillis()-time));
			
			try{
				if(input != null){ input.close();}
				if(output != null){ output.close();}
				if(socket != null){ socket.close();}
			}catch(Exception ex){
				
			}
		}
		
		return firmBean;
	}
	
	/**
	 * 펌 배치 통신
	 * @param firmBean
	 * @return
	 */
	public FirmBean vaPayOutComm(FirmBean firmBean){
		Socket socket = null;
		OutputStream output = null;
		InputStream input = null;
		String reqJson = GsonUtil.toJson(firmBean);
		String resJson = "";
		long time = System.currentTimeMillis();
		try{
			socket = new Socket(host, port);
			socket.setSoTimeout(timeout);
			
			output = socket.getOutputStream();
			output.write(reqJson.getBytes(Charset.forName("MS949")));
			output.flush();
			
			input = socket.getInputStream();
		
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int bcount = 0;
			byte[] buf = new byte[2048];
			int read_retry_count = 0;
			while(true) {
				int n = input.read(buf);
			    if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
			    else if (n == -1) break;
			    else  { // n == 0
			if (++read_retry_count >= 5)
			  throw new IOException("inputstream-read-retry-count(5) exceed !");
			    }
			    if(input.available() == 0){ break; }
			}
			bout.flush();
			byte[] res = bout.toByteArray();
			bout.close();
			resJson = new String(res,"MS949");
			if(!CommonUtil.isNullOrSpace(resJson)) {
				firmBean = (FirmBean)GsonUtil.fromJson(resJson, FirmBean.class);
			}else {
				throw new Exception("서버응답없음");
			}
			
		}catch(Exception e){
			firmBean.resultCd = "XXXX";
			firmBean.resultMsg = "펌뱅킹 시스템과의 통신장애 :"+e.getMessage();
			logger.info(firmBean.resultMsg);
		}finally{
			logger.info("-> FIRM : [{}]",reqJson);
			logger.info("<- FIRM : [{}],{}",resJson,(System.currentTimeMillis()-time));
			
			try{
				if(input != null){ input.close();}
				if(output != null){ output.close();}
				if(socket != null){ socket.close();}
			}catch(Exception ex){
				
			}
		}
		
		return firmBean;
	}
	
	public static void main(String[] args){
		FirmClient client = new FirmClient("",0,0);
	}
}
