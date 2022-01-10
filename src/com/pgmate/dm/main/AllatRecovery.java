package com.pgmate.dm.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.regex.Validator;


/**
 * @author Administrator
 *
 */
public class AllatRecovery {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.AllatRecovery.class );
	
	public static String ALLAT_RECEIPT = "http://www.allatpay.com/servlet/AllatBizPop/member/pop_card_receipt.jsp";
	
	/**
	 * 
	 */
	public AllatRecovery() {
		checkTrx();
	}
	
	public void checkTrx(){
		try {
			List<SharedMap<String,Object>> payList = getPayList();
			if(payList.size() > 0){
				
				for(SharedMap<String, Object> sharedMap:payList){
					long epochTime 	= System.currentTimeMillis();
					String hash 		= getHash(sharedMap.getString("vanId")+sharedMap.getString("cryptoKey")+sharedMap.getString("trackId")+sharedMap.getString("amount")+epochTime);	
					String value = "shop_id="+sharedMap.getString("vanId")+"&order_number="+sharedMap.getString("trackId")+"&hash_value="+hash+"&current_time="+epochTime;
					String cardNo = connect(value);
					int cardLength = cardNo.length();
					String bin = cardNo.substring(0,6);
					String last4 = cardNo.substring(cardLength - 4, cardLength);
					String cardType = getCardType(bin);
					updateTrx(sharedMap.getString("trxId"),bin,last4,cardType,"승인");
				}
			}
			
			List<SharedMap<String,Object>> rfdList = getRefundList();
			if(rfdList.size() > 0){
				
				for(SharedMap<String, Object> sharedMap:rfdList){
					long epochTime 	= System.currentTimeMillis();
					String hash 		= getHash(sharedMap.getString("vanId")+sharedMap.getString("cryptoKey")+sharedMap.getString("trackId")+Math.abs(sharedMap.getLong("rfdAmount"))+epochTime);	
					String value = "shop_id="+sharedMap.getString("vanId")+"&order_number="+sharedMap.getString("trackId")+"&hash_value="+hash+"&current_time="+epochTime;
					String cardNo = connect(value);
					logger.debug("cardNo [{}]",cardNo);
					int cardLength = cardNo.length();
					String bin = cardNo.substring(0,6);
					String last4 = cardNo.substring(cardLength - 4, cardLength);
					String cardType = getCardType(bin);
					updateTrx(sharedMap.getString("trxId"),bin,last4,cardType,"승인취소");
				}
			}
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
		}
	}
	
	public boolean updateTrx(String trxId,String bin, String last4, String cardType, String trxType){
		if(trxType.equals("승인")){
			if(updateTrxPay(trxId,bin,last4,cardType)){
				return updateTrxCapture(trxId,bin,last4,cardType);
			}
		}else{
			if(updateTrxRfd(trxId,bin,last4)){
				return updateTrxCapture(trxId,bin,last4,cardType);
			}
		}
		return false;
	}
	
	
	public boolean updateTrxPay(String trxId,String bin,String last4,String cardType){
		DAO dao = new DAO();
		dao.setDebug(true);
		dao.setTable("PG_TRX_PAY");
		dao.setRecord("bin", bin);
		dao.setRecord("last4", last4);
		dao.setRecord("cardType", cardType);
		dao.addWhere("trxId",trxId);
		boolean updated = dao.update();
		logger.info("trx pay update : {}",updated);
		return updated;
	}
	
	public boolean updateTrxRfd(String trxId,String bin,String last4){
		DAO dao = new DAO();
		dao.setDebug(true);
		dao.setTable("PG_TRX_RFD");
		dao.setRecord("bin", bin);
		dao.setRecord("last4", last4);
		dao.addWhere("trxId",trxId);
		boolean updated = dao.update();
		logger.info("trx rfd update : {}",updated);
		return updated;
	}
	
	
	public boolean updateTrxCapture(String trxId,String bin,String last4,String cardType){
		DAO dao = new DAO();
		dao.setDebug(true);
		dao.setTable("PG_TRX_CAP");
		dao.setRecord("bin", bin);
		dao.setRecord("last4", last4);
		dao.setRecord("cardType", cardType);
		dao.addWhere("trxId",trxId);
		boolean updated = dao.update();
		logger.info("trx capture update : {}",updated);
		return updated;
	}
	

	public String getCardType(String bin){
		if(Validator.isNumber(bin)){
			DAO dao = new DAO();
			dao.setTable("PG_CODE_BIN");
			dao.setColumns("type");
			dao.addWhere("bin",bin,dao.eq);
			RecordSet rset = dao.search();
			String cardType = rset.getRowFirst().getString("type");
			if(CommonUtil.isNullOrSpace(cardType)){
				cardType = "기타";
			}
			return cardType;
		}else{
			return "기타";
		}
	}
		
	private String getHash(String text) throws Exception{
		StringBuffer buf = new StringBuffer();
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(text.getBytes("euc-kr"));
		byte[] digest = md.digest();
		
		for( int i = 0; i < digest.length; i++ ){
			if((0xff & digest[i]) < 0x10)
				buf.append("0" + Integer.toHexString(0xff & digest[i]));
			else
				buf.append(Integer.toHexString(0xff & digest[i]));
		}
		
		return buf.toString();
	}

	private String connect(String msg){
		long time = System.currentTimeMillis();
		String card = "444444xxxxxx4444";
		StringBuffer result = new StringBuffer();
		URL url = null;
		HttpURLConnection conn = null;
		
		try {
			url = new URL(ALLAT_RECEIPT);
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(10000);
			
			byte[] reqbuf = msg.getBytes(Charset.forName("euc-kr"));
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Connection", "close");
			conn.setRequestProperty("Content-length", CommonUtil.toString(reqbuf.length));
			
			OutputStream os = conn.getOutputStream();
			os.write(reqbuf);
			os.flush();
			os.close();
			
			int httpCode = conn.getResponseCode();
			BufferedReader in = null;
			
			if(httpCode ==200){
				in = new BufferedReader(new InputStreamReader( conn.getInputStream(),"euc-kr"));
			}else{
				in = new BufferedReader(new InputStreamReader( conn.getErrorStream(),"euc-kr"));
				result.append("NETWORKERROR:");
			}
			
			String line;
			while ((line = in.readLine()) != null){
				result.append(line);
			}
			
			in.close();
			Document doc = Jsoup.parse(result.toString());
			
			try {
				Element table = doc.select("table.box td.t_con").get(1);
				if(table != null){
					String number = CommonUtil.leftTrim(table.html().replaceAll("-", "").trim());
					if(number.length() > 13){
						card = number;
					}
				}
			}catch (Exception e) {
				// 해외카드 영수증 포맷
				Element table2 = doc.select("table.box td.t_con_en").get(1);
				String number = CommonUtil.leftTrim(table2.html().replaceAll("-", "").trim());
				if(number.length() > 13){
					card = number;
				}
			}
		} catch(Exception e) {
			result.append("NOTCONNECTED "+e.getMessage());
		}finally{
			logger.debug("allat Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
			conn.disconnect();
		}
		
		return card;
	}


	
	public List<SharedMap<String,Object>> getPayList(){
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY A JOIN PG_VAN B ON A.vanId = B.vanId ");
		dao.setColumns("A.*, B.cryptoKey");
		dao.addWhere("A.bin","444444",DAO.eq);
		dao.addWhere("A.regDay >= DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -7 DAY),'%Y%m%d')");
		RecordSet rset = dao.search();
		return rset.getRows();
	}

	public List<SharedMap<String,Object>> getRefundList(){
		DAO dao = new DAO();
		dao.setTable("PG_TRX_RFD A JOIN PG_VAN B ON A.vanId = B.vanId ");
		dao.setColumns("A.*, B.cryptoKey");
		dao.addWhere("A.bin","444444",DAO.eq);
		dao.addWhere("A.regDay >= DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -7 DAY),'%Y%m%d')");
		RecordSet rset = dao.search();
		return rset.getRows();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AllatRecovery k = new AllatRecovery();
	}
}


