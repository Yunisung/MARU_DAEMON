package com.pgmate.dm.main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

public class GalaxiaRecovery {
	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.GalaxiaRecovery.class );
	private SmsGw smsGw = null;
	
	public GalaxiaRecovery() {
		recoveryTrx();
		smsGw = new SmsGw();
	}
	
	public void recoveryTrx(){
		
		try {
			// 거래내역 생성 할  list 들고오기
			List<SharedMap<String,Object>> newList = getGalaxiaList();
			List<SharedMap<String,Object>> setList = new ArrayList<SharedMap<String,Object>>();
			
			if(newList.size() > 0){
				logger.info("거래내역 list size : {}",newList.size());
				
				for(SharedMap<String,Object> data : newList){
					SharedMap<String,Object> tmnMap = getMchtTmnByTmnId(data.getString("tmnId"));
					if(!tmnMap.getString("tmnId").equals(data.getString("tmnId"))){
						data.put("exeStatus", "실패");
						data.put("summary", "터미널 아이디가 없습니다.");
					}else{
						
						if(data.getString("trnType").equals("승인")){
							SharedMap<String,Object> pay = new SharedMap<String,Object>();
							pay.put("trxId"		, getTrxId());
							pay.put("mchtId"	, tmnMap.getString("mchtId"));
							pay.put("tmnId"		, tmnMap.getString("tmnId"));
							pay.put("trackId"	, getTrackId());
							pay.put("amount"	, data.getLong("amount"));
							pay.put("installment", CommonUtil.zerofill(data.getInt("installment"),2));
							pay.put("cardId"	, GenKey.genKeys(CPKEY.CARD,pay.getString("trxId")));
							pay.put("bin"		, data.getString("bin"));
							pay.put("last4"		, data.getString("last4"));
							pay.put("status"	, "승인");
							pay.put("prodId"	, GenKey.genKeys(CPKEY.PRODUCT,pay.getString("trxId")));
							
							Card card = new Card();
							card.cardId 	= pay.getString("cardId");
							card.installment= pay.getInt("installment");
							card.bin		= pay.getString("bin");
							card.last4		= pay.getString("last4");
							
							SharedMap<String,Object> issuerMap = getDBIssuer(card.bin);
							if(issuerMap != null){
								card.cardType = issuerMap.getString("type") ;
								card.issuer = issuerMap.getString("issuer");
								card.acquirer = issuerMap.getString("acquirer");
							}
							
							// 카드 정보를 json 형식으로 변경해서 
							String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16)));
							insertCard(card.cardId,encrypted);
							
							pay.put("cardType"	, card.cardType);
							pay.put("issuer"	, card.issuer);
							pay.put("acquirer"	, card.acquirer);
							pay.put("reqDay"	, data.getString("trxDay"));
							pay.put("reqTime"	, data.getString("trxTime"));
							pay.put("authCd"	, data.getString("authCd"));
							pay.put("resultCd"	, "0000");
							pay.put("resultMsg"	, "[정상]정상승인");
							pay.put("van"		, tmnMap.getString("van"));		
							pay.put("vanId"		, data.getString("vanId"));
							pay.put("vanTrxId"	, data.getString("vanTrxId"));
							pay.put("regDay"	, data.getString("trxDay"));
							pay.put("regTime"	, data.getString("trxTime"));
							pay.put("regDate"	, data.getString("trxDay")+data.getString("trxTime"));
							
							// 주문자 정보 넣기 
							data.put("trxId",pay.getString("trxId"));
							data.put("tmnId",pay.getString("tmnId"));
							
							if(insertTrxPay(pay)){
								data.put("exeStatus", "완료");
								data.put("summary", "승인거래 등록완료");
							}else{
								data.put("exeStatus", "실패");
								data.put("summary", "승인거래 등록실패");
							}
						}else{
							if(getRfdList(data.get("vanTrxId").toString()).size() > 0) {
								//갤럭시아 단말기 :취소건 중복 오류 수정
								data.put("exeStatus", "완료");
								data.put("summary", "기취소 거래건");
								data.put("trxId",getTrxId());
								
							} else {
								SharedMap<String,Object> rfd = new SharedMap<String,Object>();
								SharedMap<String,Object> rootMap = getPayMap(data, tmnMap.getString("mchtId"));
								logger.info("root status : [{}]",rootMap.getString("status"));
								
								if(rootMap == null || rootMap.size() > 0){
									rfd.put("trxId"		, getTrxId());
									rfd.put("mchtId"	, tmnMap.getString("mchtId"));
									rfd.put("tmnId"		, data.getString("tmnId"));
									rfd.put("trackId"	, getTrackId());
									rfd.put("status", "완료");
									
									long amount = rootMap.getLong("amount");
									if(amount > 0){
										amount = - amount;
									}
									
									if(data.getLong("amount") == -rootMap.getLong("amount")){
										rfd.put("rfdAll", "전액");
									}else{
										rfd.put("rfdAll", "부분");
									}
									rfd.put("rfdAmount"	, amount);
									rfd.put("rfdVat"	, calcRootVat(amount));
									rfd.put("cardId"	, rootMap.getString("cardId"));
									rfd.put("bin"		, rootMap.getString("bin"));
									rfd.put("last4"		, rootMap.getString("last4"));
									rfd.put("issuer"	, rootMap.getString("issuer"));
									rfd.put("acquirer"	, rootMap.getString("acquirer"));
									rfd.put("rootTrnDay", rootMap.getString("reqDay"));
									rfd.put("rootTrxId"	, rootMap.getString("trxId"));
									rfd.put("rootTrackId", rootMap.getString("trackId"));
									rfd.put("rootAmount", rootMap.getLong("amount"));
									rfd.put("rootVat"	, calcRootVat(rootMap.getLong("amount")));
									rfd.put("reqDay"	, data.getString("trxDay"));
									rfd.put("reqTime"	, data.getString("trxTime"));
									rfd.put("authCd"	, data.getString("authCd"));
									rfd.put("resultCd"	, "0000");
									rfd.put("resultMsg"	, "[정상]정상취소");
									rfd.put("van"		, rootMap.getString("van"));				
									rfd.put("vanId"		, rootMap.getString("vanId"));
									rfd.put("vanTrxId"	, data.getString("vanTrxId"));
									rfd.put("regDay"	, data.getString("trxDay"));
									rfd.put("regTime"	, data.getString("trxTime"));
									rfd.put("regDate"	, data.getString("trxDay")+data.getString("trxTime"));
									
									data.put("trxId",rfd.getString("trxId"));
									data.put("tmnId",rfd.getString("tmnId"));
									
									if(insertTrxRfd(rfd)){
										// 취소 완료 후  pay테이블 status칼럼 승인 > 승인취소로 변경
										updateTrxPay(rootMap.getString("trxId"));
										data.put("exeStatus", "완료");
										data.put("summary", "취소거래 등록완료");
									}else{
										data.put("exeStatus", "실패");
										data.put("summary", "취소거래 등록실패");
									}
								}else{
									data.put("exeStatus", "실패");
									data.put("summary", "원거래를 찾을 수 없습니다.");
								}
							}
					
						}
					}
					setList.add(data);
				}
				
				if(setList.size() > 0){
					logger.info("update load size  =[{}]",setList.size());
					updateLoadGalaxia(setList);
				}
			}
		}catch(Exception e) {
			String msgBody = "갤럭시아 단말기 거래내역 등록 오류. 확인요망";
			smsGw.sendMessage("0", "1", msgBody);
            logger.error(e.getMessage(), e);
		}
	}
	
	public String getFunction(String function, String value) {
		String returnVal = "";
		String query = "SELECT " + function + "(?) as val";

		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rset = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, value);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				returnVal = rset.getString(1);
			}
			conn.commit();
		} catch (Exception t) {
			logger.debug("sql error : {}, query : {}", t.getMessage(), query);
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}
	
	private String getTrackId() {
		return "TX" + getFunction("FN_NEXTVAL2", "TRACKID");
	}
	
	private String getTrxId() {
		return "T" + getFunction("FN_NEXTVAL2", "TRN");
	}

	private SharedMap<String, Object> getMchtTmnByTmnId(String tmnId) {
		String key = "PG_MCHT_TMN_" + tmnId;
				
		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {
			DAO dao = new DAO();
			dao.setTable("PG_MCHT_TMN");
			dao.setColumns("*");
			dao.addWhere("tmnId", tmnId, DAO.eq);
			RecordSet rset = dao.search();
			
			if(rset.size() > 0){
				return Cache.map.put(key, rset.getRow(0));
			
			}else{
				return new SharedMap<String,Object>();
			}
		}
	}
	
	public SharedMap<String,Object> getDBIssuer(String bin){
		String key = "PG_CODE_BIN_"+bin;
		SharedMap<String,Object> issuerMap = new SharedMap<String,Object>();
		if(CommonUtil.isNullOrSpace(bin)){
			return issuerMap;
		}
		
		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {
		
			DAO dao = new DAO();
			dao.setTable("PG_CODE_BIN");
			dao.addWhere("bin", bin, DAO.eq);
			dao.setColumns("*");
			RecordSet rset = dao.search();
		
			if(rset.size() == 0){
				issuerMap.put("bin", bin);
				issuerMap.put("issuer", "기타");
				issuerMap.put("type", "신용");
			}else{
				issuerMap = rset.getRowFirst();
			}
			
			return Cache.map.put(key, rset.getRow(0));
		}
	}
	
	public void insertCard(String cardId, String value) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_BOX");
		dao.setRecord("cardId", cardId);//1개
		dao.setRecord("value", value);
		logger.info("set card : {}", dao.insert());
	}
	
	public SharedMap<String,Object> getPayMap(SharedMap<String,Object> data,String mchtId){
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY");
		dao.addWhere("reqDay"	, data.getString("rootTrxDay"));
		dao.addWhere("reqTime"	, data.getString("rootTrxTime"));
		dao.addWhere("mchtId"	, mchtId);
		dao.addWhere("tmnId"	, data.getString("tmnId"));
		dao.addWhere("bin"		, data.getString("bin"));
		dao.addWhere("authCd"	, data.getString("authCd"));
		dao.addWhere("vanId"	, data.getString("vanId"));
		dao.addWhere("amount"	, -data.getLong("amount"));
		
		dao.setColumns("*");
		RecordSet rset = dao.search();
		return rset.getRowFirst();
	}
	
	
	public boolean insertTrxPay(SharedMap<String,Object> map){
		String query = "insert into PG_TRX_PAY (trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,status,prodId,issuer,acquirer,reqDay,reqTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,regDay,regTime,regDate)  values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		int inserted = 0;
		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			int i=1;
	
			pstmt.setString(i++, map.getString("trxId"));
			pstmt.setString(i++, map.getString("mchtId"));
			pstmt.setString(i++, map.getString("tmnId"));
			pstmt.setString(i++, map.getString("trackId"));
			pstmt.setString(i++, map.getString("payerName"));
			pstmt.setString(i++, map.getString("payerEmail"));
			pstmt.setString(i++, map.getString("payerTel"));
			pstmt.setLong(i++  , map.getLong("amount"));
			pstmt.setString(i++, map.getString("installment"));
			pstmt.setString(i++, map.getString("cardId"));
			pstmt.setString(i++, map.getString("cardType"));
			pstmt.setString(i++, map.getString("bin"));
			pstmt.setString(i++, map.getString("last4"));
			pstmt.setString(i++, map.getString("status"));
			pstmt.setString(i++, map.getString("prodId"));
			pstmt.setString(i++, map.getString("issuer"));
			pstmt.setString(i++, map.getString("acquirer"));
			pstmt.setString(i++, map.getString("reqDay"));
			pstmt.setString(i++, map.getString("reqTime"));
			pstmt.setString(i++, map.getString("authCd"));
			pstmt.setString(i++, map.getString("resultCd"));
			pstmt.setString(i++, map.getString("resultMsg"));
			pstmt.setString(i++, map.getString("van"));
			pstmt.setString(i++, map.getString("vanId"));
			pstmt.setString(i++, map.getString("vanTrxId"));
			pstmt.setString(i++, map.getString("regDay"));
			pstmt.setString(i++, map.getString("regTime"));
			pstmt.setString(i++, map.getString("regDate"));
			inserted = pstmt.executeUpdate();
			conn.commit();
		}catch(Exception e){
			logger.debug("insert batch pay error : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(pstmt);
			db.close(conn);
		}
		
		if(inserted > 0){
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," create TRX_PAY");
			return true;
		}else{
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," fail   TRX_PAY");
			return false;
		}
	}

	public boolean insertTrxRfd(SharedMap<String,Object> map){
		int inserted = 0;
		String query = "insert into PG_TRX_RFD (trxId,mchtId,tmnId,trackId,status,rfdType,rfdAll,rfdAmount,rfdVat,cardId,bin,last4,issuer,acquirer,rootTrnDay,rootTrxId,rootTrackId,rootAmount,rootVat,reqDay,reqTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,regDay,regTime,regDate) "
				+" values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			
			int i=1;
			pstmt.setString(i++, map.getString("trxId"));
			pstmt.setString(i++, map.getString("mchtId"));
			pstmt.setString(i++, map.getString("tmnId"));
			pstmt.setString(i++, map.getString("trackId"));
			pstmt.setString(i++, map.getString("status"));
			pstmt.setString(i++, map.getString("rfdType"));
			pstmt.setString(i++, map.getString("rfdAll"));
			pstmt.setLong(i++  , map.getLong("rfdAmount"));
			pstmt.setLong(i++, map.getLong("rfdVat"));
			pstmt.setString(i++, map.getString("cardId"));
			pstmt.setString(i++, map.getString("bin"));
			pstmt.setString(i++, map.getString("last4"));
			pstmt.setString(i++, map.getString("issuer"));
			pstmt.setString(i++, map.getString("acquirer"));
			pstmt.setString(i++, map.getString("rootTrnDay"));
			pstmt.setString(i++, map.getString("rootTrxId"));
			pstmt.setString(i++, map.getString("rootTrackId"));
			pstmt.setLong(i++  , map.getLong("rootAmount"));
			pstmt.setLong(i++, map.getLong("rootVat"));
			pstmt.setString(i++, map.getString("reqDay"));
			pstmt.setString(i++, map.getString("reqTime"));
			pstmt.setString(i++, map.getString("authCd"));
			pstmt.setString(i++, map.getString("resultCd"));
			pstmt.setString(i++, map.getString("resultMsg"));
			pstmt.setString(i++, map.getString("van"));
			pstmt.setString(i++, map.getString("vanId"));
			pstmt.setString(i++, map.getString("vanTrxId"));
			pstmt.setString(i++, map.getString("resultCd"));
			pstmt.setString(i++, map.getString("resultMsg"));
			pstmt.setString(i++, map.getString("regDay"));
			pstmt.setString(i++, map.getString("regTime"));
			pstmt.setString(i++, map.getString("regDate"));

			inserted = pstmt.executeUpdate();
			conn.commit();
		}catch(Exception e){
			logger.debug("insert batch rfd error : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(pstmt);
			db.close(conn);
		}
		
		if(inserted > 0){
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," create TRX_RFD");
			return true;
		}else{
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," fail   TRX_RFD");
			return false;
		}
	}
	
	public long calcRootVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /110).longValue();
		}else{
			return new Double(amount *10 /110).longValue();
		}
	}
	public void updateTrxPay(String trxId) {
		
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY");
		dao.setRecord("status", "승인취소");
		dao.addWhere("trxId", trxId);
		logger.info("set TRX_PAY : {}", dao.update());
		dao.initRecord();
	}
	
	
	public List<SharedMap<String,Object>> getGalaxiaList(){
		
		DAO dao = new DAO();
		dao.setTable("PG_TRX_LOAD_GALAXIA");
		dao.setColumns("*");
		dao.addWhere("exeStatus = '대기'");
		dao.addWhere("trnType != '망취소'");
		dao.setOrderBy("trxDay asc,trxTime asc");
		RecordSet rset = dao.search();
		return rset.getRows();
	}
	
	public int updateLoadGalaxia(List<SharedMap<String,Object>> setList){
		int inserted = 0;

		String query = "UPDATE PG_TRX_LOAD_GALAXIA set trxId =?,tmnId=?,exeStatus=?,exeDate=CURRENT_TIMESTAMP,summary=? WHERE vanTrxId =? and trnType=?";
		
		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			
			int batchSize = 100;
			int count = 0;
			
			for(SharedMap<String,Object> map : setList){
				int i=1;
				pstmt.setString(i++, map.getString("trxId"));
				pstmt.setString(i++, map.getString("tmnId"));
				pstmt.setString(i++  , map.getString("exeStatus"));
				pstmt.setString(i++  , map.getString("summary"));
				pstmt.setString(i++, map.getString("vanTrxId"));
				pstmt.setString(i++  , map.getString("trnType"));
				
				pstmt.addBatch();
				if(++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			 
			inserted +=pstmt.executeBatch().length;
			conn.commit();
		}catch(Exception e){
			logger.debug("insert batch PG_TRX_LOAD_GALAXIA error : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(pstmt);
			db.close(conn);
		}
		return inserted;
	}
	
	
	public List<SharedMap<String,Object>> getRfdList(String vanTrxId){
			
		DAO dao = new DAO();
		dao.setTable("PG_TRX_RFD");
		dao.setColumns("*");
		dao.addWhere("vanTrxId",vanTrxId);
		RecordSet rset = dao.search();
		return rset.getRows();
	}
	
	public static void main(String[] args) {
		

		new GalaxiaRecovery();
	}

}
