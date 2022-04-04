package com.pgmate.dm.main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class GalaxiaRecovery extends KsnetRecovery {
	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.KsnetRecovery.class );
	
	public GalaxiaRecovery() {
		recoveryTrx();
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
					setList.add(data);
				}
				
				if(setList.size() > 0){
					updateLoadGalaxia(setList);
				}
			}
		}catch(Exception e) {
            logger.error(e.getMessage(), e);
		}
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
	
	
	public static void main(String[] args) {
		

		new GalaxiaRecovery();
	}

}
