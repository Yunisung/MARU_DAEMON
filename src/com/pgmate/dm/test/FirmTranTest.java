package com.pgmate.dm.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.FirmBean;
import com.pgmate.lib.util.gson.GsonUtil;

/**
 * @author Administrator
 *
 */
public class FirmTranTest {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.test.FirmTranTest.class );
	//private static String host 	= "MARUpg.com";
	private static String host 	= "pgwas2";
	//private static int port 	= 10006;
	private static int port 	= 10028;
	private static int timeout  = 40000;
	
	public static void main(String[] args){
		logger.info("=========================================================");
		FirmTranTest client = new FirmTranTest();
		
		//client.open("007");
		client.balance("007","101021504175","KONEPS1");
		//client.balance("007","101021518182","KONEPS1");
		//client.holder("007","101021504175");
		//client.accountTransfer("007","101021504175","089","70110000010023",1000, "", "BT");
		//client.accountTransfer("007","101021518182","089","70110000010023",1000, "", "BT");
		//client.accountTransfer("039","2070079982702", "032","1122098216206",1000, "", "BT");
		//client.accountTransfer("039","2070079982702", "034","720107420717 ",1000, "", "BT");
	}

	public FirmTranTest() {
	}
	
	/**
	 * 잔액조회
	 * @param bankCd
	 * @param accntNo
	 * @return
	 */
	public FirmBean open(String bankCd){
		logger.info("펌개시");
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= bankCd;
		firmBean.msgType 	= "0800100";
		firmBean.userId		= "SYSTEM";
		
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("잔액 : {}",firmBean.data.getLong("amount"));
		
		return firmBean;
	}
	
	/**
	 * 은행통한 예금주조회
	 * @param bankCd
	 * @param userBankCd
	 * @param userAccount
	 */
	public FirmBean holder(String userBankCd,String userAccount){
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
		//firmBean.mAccnt		= accntNo;
		
		firmBean.data.put("mAccnt",accntNo);
		firmBean.data.put("compCd",compCd);
		
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("잔액 : {}",firmBean.data.getLong("amount"));
		
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
		
		try {
			logger.info("계좌이체");
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
			logger.info("===========================");
			firmBean = comm(firmBean);
			logger.info("===========2===============");
			logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
			logger.info("idx:{},{}",firmBean.idx,firmBean.data.getLong("balance"));
			logger.info("data : {}",GsonUtil.toJson(firmBean.data));
		}catch(Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
		return firmBean;
	}
	
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
			output.write(reqJson.getBytes());
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
			
			firmBean = (FirmBean)GsonUtil.fromJson(new String(res), FirmBean.class);
			
		}catch(Exception e){
			firmBean.resultCd = "XXXX";
			firmBean.resultMsg = "펌뱅킹 시스템과의 통신장애 :"+e.getMessage();
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
}
