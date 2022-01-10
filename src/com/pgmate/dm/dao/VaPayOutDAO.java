package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class VaPayOutDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.VaPayOutDAO.class );
	
	public VaPayOutDAO() {
		super.setDebug(false);
	}
	
	/**
	 * 가상계좌 충전 출금대상거래 에서 출금하지 않은 데이터중에 전송시도가 남은 거래건들 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getVaPayList(){
		String q = "SELECT * "
				+"	  FROM VA_TRX_FIRM "
				+"   WHERE status != '완료' and status != '전송' and retry < 3 "
				+"   order by regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 가상계좌 충전 대상건들 상태와 전송횟수 업데이트
	 * @param trxId : 거래번호
	 * @param refId : 출금 거래번호
	 * @return
	 */
	public boolean updateStatus(String trxId){
		String q = "UPDATE VA_TRX_FIRM "
				+ "    SET status = '전송', retry=retry+1 "
				+ "	 WHERE trxId = '" + trxId + "'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 가상계좌 충전 출금 횟수 조회
	 * @param trxId
	 * @return
	 */
	public String getRetry(String trxId){
		super.setTable("VA_TRX_FIRM");
		super.setColumns("retry");
		super.addWhere("trxId",trxId,eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("retry");
	}
	
	/**
	 * 가상계좌 충전 출금 거래건 전문번호
	 * @param idx
	 * @return
	 */
	public String getSeqNo(String trxId){
		String q = "SELECT seqNo"
				+"    FROM PG_FIRM_TRX "
				+"	 WHERE idx = "
				+" 		(SELECT refId "
				+"	  	   FROM VA_TRX_FIRM "
				+"   	  WHERE trxId = '" + trxId + "')";
		
		RecordSet rset = super.query(q);

		super.initRecord();
		return rset.getRowFirst().getString("seqNo");
	}
	
	/**
	 * VA_PTN 데이터 조회
	 * @param ptnId
	 * @return
	 */
	public RecordSet getPtn(String ptnId){
		super.setTable("VA_PTN");
		super.setColumns("*");
		super.addWhere("ptnId",ptnId,eq);
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset;
	}
	
	/**
	 * 취소건의 실출금액 조회
	 * @param trxId
	 * @return
	 */
	public String getStlAmt(String trxId){
		super.setTable("VA_TRX");
		super.setColumns("stlAmount");
		super.addWhere("trxId",trxId,eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("stlAmount");
	}
	
	/**
	 * 가상계좌 충전 출금 결과 업데이트
	 * @param trxId : 거래번호
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean updatePayOutRes(String trxId, String resCd, String resMsg, String status){
		String q = "UPDATE VA_TRX_FIRM "
				+ "    SET resultCd='"+resCd+"', resultMsg='"+resMsg+"', status='"+status+"'"
				+ "	 WHERE trxid = '"+trxId+"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 가상계좌 충전 출금 거래번호 업데이트
	 * @param trxId : 거래번호
	 * @param refId : 출금 거래번호
	 * @return
	 */
	public boolean updateRefIdUpdate(String trxId, String refId){
		String q = "UPDATE VA_TRX "
				+ "    SET refId = '"+refId+"'"
				+ "	 WHERE trxId = '" + trxId + "'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 가상계좌 충전 출금 거래번호 업데이트
	 * @param trxId : 거래번호
	 * @param refId : 출금 거래번호
	 * @return
	 */
	public boolean updateRefIdUpdate2(String trxId, String refId){
		String q = "UPDATE VA_TRX_FIRM "
				+ "    SET refId = '"+refId+"'"
				+ "	 WHERE trxId = '" + trxId + "'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 데이터 암호화
	 */
	public String getAESEnc(String value){
		String query = "SELECT FN_AES_ENC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	/**
	 *데이터 복호화
	 */
	public String getAESDec(String value){
		String query = "SELECT FN_AES_DEC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	public static String getFunction(String function, String value) {
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
			
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}
	
	/**
	 * 정산아이디 생성
	 * @return
	 */
	public synchronized static String getSettleId() {
		return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
	}
	
	/**
	 * 자동정산 출금 결과 매입데이터 업데이트
	 * @param stlId : 정산번호
	 * @param stlDay : 정산예정일자
	 * @param stlType : 정산타입
	 * @param mchtId : 가맹점아이디
	 * @param payOutDay : 출금일자
	 * @return
	 */
	public boolean updateAutoPayOutCapUpdate(String stlId, String stlDay, String stlType, String mchtId, String payOutDay, String stlStatus){
		String q = "UPDATE PG_TRX_CAP_DTL "
				+ "    SET stlId = '" + stlId +"', stlStatus = '" + stlStatus + "', risk = '', payOutDay='"+payOutDay+"'"
				+ "	 WHERE capId in "
				+ "		("
				+ "		 select B.capId "
				+ "		   from PG_TRX_CAP A, PG_TRX_CAP_DTL B" 
				+ "		  where A.capId = B.capId and A.mchtId = '" + mchtId + "' and B.stlDay = '" + stlDay + "' and B.stlType = '" + stlType + "' and B.stlStatus != '정산완료'"
				+ "		)"; 
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 출금 실패한 거래 건의 출금예정액만큼 이후거래건들 잔액 추가 
	 * @param trxId
	 * @param stlAmount
	 * @return
	 */
	public boolean updateVaBalance(String trxId, String id, String stlAmount){
		String q = "UPDATE VA_TRX "
				+ "    SET balance = balance + '" + stlAmount +"'"
				+ "  WHERE trxId in "
				+ "  ("
				+ "		SELECT trxId "
				+ "   	  FROM VA_TRX"
				+ "		  WHERE regDate > "
				+ "			("
				+ "		 		SELECT regDate "
				+ "		   	  	  FROM VA_TRX " 
				+ "		     	 WHERE trxId = '" + trxId + "'"
				+ "			) and id = '" + id + "'"
				+ "	 )";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * VA_TRX_ERR에 최종 실패 건 추가
	 * @param trxMap
	 * @return
	 */
	public boolean insertTrxErr(SharedMap<String, Object> trxMap) {
		super.setTable("VA_TRX_ERR");
		super.setRecord("trxId", trxMap.getString("trxId"));
		super.setRecord("id", trxMap.getString("id"));
		super.setRecord("ptnId", trxMap.getString("ptnId"));
		super.setRecord("userId", trxMap.getString("userId"));
		super.setRecord("trxType", trxMap.getString("trxType"));
		super.setRecord("trxUnit", trxMap.getString("trxUnit"));
		super.setRecord("trxDay", trxMap.getString("trxDay"));
		super.setRecord("trxTime", trxMap.getString("trxTime"));
		super.setRecord("amount", trxMap.getLong("amount"));
		super.setRecord("feeType", trxMap.getString("feeType"));
		super.setRecord("feeRate", trxMap.getDouble("feeRate"));
		super.setRecord("fee", trxMap.getLong("fee"));
		super.setRecord("feeVat", trxMap.getLong("feeVat"));
		
		super.setRecord("ptnFeeRate", trxMap.getDouble("ptnFeeRate"));
		super.setRecord("ptnFee", trxMap.getLong("ptnFee"));
		super.setRecord("ptnFeeVat", trxMap.getLong("ptnFeeVat"));
		super.setRecord("bankFee", trxMap.getLong("bankFee"));
		super.setRecord("stlAmount", trxMap.getLong("stlAmount"));
		super.setRecord("balance", trxMap.getLong("balance"));
		super.setRecord("trackId", trxMap.getString("trackId"));
		
		super.setRecord("refId", trxMap.getString("refId"));
		super.setRecord("bankCd", trxMap.getString("bankCd"));
		super.setRecord("account", trxMap.getString("account"));
		super.setRecord("holder", trxMap.getString("holder"));
		super.setRecord("resultCd", trxMap.getString("resultCd"));
		super.setRecord("resultMsg", trxMap.getString("resultMsg"));
		super.setRecord("regDay", trxMap.getString("regDay"));
		
		boolean inserted = super.insert();
		logger.info("set VA_TRX_ERR : {}", inserted);
		super.initRecord();
		return inserted;
	}
	
	/**
	 * 출금 실패한 거래건 VA_TRX테이블 데이터 삭제
	 * @param trxId
	 */
	public boolean deleteVaTrx(String trxId) {
		super.setTable("VA_TRX");
		super.addWhere("trxId",trxId,eq);
		boolean deleted = super.delete();
		
		return deleted;
	}
	
	/**
	 * 삭제될 거래건의 잔액조회
	 * @param trxId
	 * @return
	 */
	public long getBalance(String trxId){
		super.setTable("VA_TRX");
		super.setColumns("balance");
		super.addWhere("trxId",trxId,eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getLong("balance");
	}
	
	/**
	 * 가상계좌 충전 출금대상거래 에서 idx가 비어서 응답이 올경우 검색해서 idx값 재조회
	 * @return
	 */
	public String getIdx(String trxId){
		super.setTable("PG_FIRM_TRX");
		super.setColumns("idx");
		super.addWhere("filler",trxId,eq);
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		
		return rset.getRowFirst().getString("idx");
	}
	
	/**
	 * 가상계좌 충전 출금대상거래 조회
	 * @return
	 */
	public SharedMap<String, Object> getVaTrx(String trxId) {
		super.setTable("VA_TRX");
		super.setColumns("*");
		super.addWhere("trxId",trxId,eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}
	
	/**
	 * 가상계좌 2중 입금 검색
	 * @return
	 */
	public List<SharedMap<String,Object>> getDuplTranList(){
		String q = "SELECT * "
				+"	  FROM "
				+"   	( "
				+"			SELECT trxDay, seqNo, COUNT(1) AS cnt "
				+"   		  FROM PG_VACT_TRX "
				+"			 WHERE trxDay = date_format(NOW(),'%Y%m%d') "
				+"			 GROUP BY trxDay,seqNo "
				+"		) a "
				+"   WHERE a.cnt > 1"; 
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 가상계좌 2중 입금거래 상세 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getDuplTranDtlList(String day, String seqNo){
		String q =  "SELECT * "
				+"	   FROM PG_VACT_TRX"
				+"    WHERE trxDay = '" + day + "' and seqNo = '" + seqNo + "'"
				+" ORDER BY regDate"; 
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 가상계좌 대행서비스 사용자 아이디 조회
	 * @param trxId
	 * @return
	 */
	public String getVaUserId(String accont){
		String query = "SELECT id "
					+"	  FROM VA_USER_VACCNT "
					+"   WHERE account = ?";
	
		DBManager db 			= null;
		PreparedStatement pstmt	= null;
		Connection conn			= null;
		ResultSet rset			= null;
		String id 				= "";
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			pstmt.setString(1,accont);
			
			rset 	= pstmt.executeQuery();

			while(rset.next()){
				id = rset.getString("id");
			}
		}catch(Exception e){
			e.getStackTrace();
			logger.error("getVaUserId ERROR : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(conn,pstmt,rset);
		}
		
		return id;
	}
	
	/**
	 * 2중입금 처리된 PG_VACT_TRX테이블 데이터 삭제
	 * @param vactId
	 */
	public boolean deleteVactTrx(String trxId) {
		super.setTable("PG_VACT_TRX");
		super.addWhere("vactId",trxId,eq);
		boolean deleted = super.delete();
		
		return deleted;
	}
	
	/**
	 * 가상계좌 2중 입급건중 대행서비스 거래정보 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getVaPayList(String vactId){
		String q = "SELECT * "
				+"	  FROM VA_TRX "
				+"   WHERE refId = '" + vactId + "'";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 입금취소 시 해당 취소의 원거래건이 실시간 정산 거래건 인지 확인
	 * @param vactId
	 * @return
	 */
	public SharedMap<String,Object> getRealtimeTrx(String vactId){
		SharedMap<String,Object> result = null;
		
		String query = " SELECT * FROM PG_TRX_REALTIME_PAY WHERE trxId =? ";
		
		DBManager db 	= null;
		PreparedStatement pstmt	= null;
		Connection conn			= null;
		ResultSet rset			= null;
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			pstmt.setString(1,vactId);
			
			rset 	= pstmt.executeQuery();

			while(rset.next()){
				result = new SharedMap<String,Object>();
				
				result.put("trxId",rset.getString("trxId"));
				result.put("mchtId",rset.getString("mchtId"));
				result.put("tmnId",rset.getString("tmnId"));
				result.put("trackId",rset.getString("trackId"));
				result.put("amount",rset.getLong("amount"));
				result.put("authCd",rset.getString("authCd"));
				result.put("trxType",rset.getString("trxType"));
				result.put("trxDay",rset.getString("trxDay"));
				result.put("trxTime",rset.getString("trxTime"));
				result.put("resultCd",rset.getString("resultCd"));
				result.put("resultMsg",rset.getString("resultMsg"));
				result.put("van",rset.getString("van"));
				result.put("vanId",rset.getString("vanId"));
				result.put("vanTrxId",rset.getString("vanTrxId"));
				result.put("sendYn",rset.getString("sendYn"));
				result.put("transferInterval",rset.getString("transferInterval"));
				result.put("sendDate",rset.getString("sendDate"));
			}
		}catch(Exception e){
			e.getStackTrace();
			logger.error("getRealtimeTrx ERROR : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(conn,pstmt,rset);
		}
		
		return result;
	}
	
	/**
	 * PG_CHARGE_SETTLE_ERR에 최종 실패 건 추가
	 * @param trxMap
	 * @return
	 */
	public boolean insertChargeErr(SharedMap<String, Object> trxMap) {
		super.setTable("PG_CHARGE_SETTLE_ERR");
		super.setRecord("trxId", trxMap.getString("trxId"));
		super.setRecord("mchtId", trxMap.getString("mchtId"));
		super.setRecord("trxType", trxMap.getString("trxType"));
		super.setRecord("trxUnit", trxMap.getString("trxUnit"));
		super.setRecord("trxDay", trxMap.getString("trxDay"));
		super.setRecord("trxTime", trxMap.getString("trxTime"));
		super.setRecord("amount", trxMap.getLong("amount"));
		super.setRecord("fee", trxMap.getLong("fee"));
		super.setRecord("feeVat", trxMap.getLong("feeVat"));
		super.setRecord("bankFee", trxMap.getLong("bankFee"));
		super.setRecord("netAmount", trxMap.getLong("netAmount"));
		super.setRecord("balance", trxMap.getLong("balance"));
		super.setRecord("trackId", trxMap.getString("trackId"));
		super.setRecord("refId", trxMap.getString("refId"));
		super.setRecord("bankCd", trxMap.getString("bankCd"));
		super.setRecord("bankName", trxMap.getString("bankName"));
		super.setRecord("account", trxMap.getString("account"));
		super.setRecord("holder", trxMap.getString("holder"));
		super.setRecord("recordInfo", trxMap.getString("recordInfo"));
		super.setRecord("summary", trxMap.getString("summary"));
		super.setRecord("resultCd", trxMap.getString("resultCd"));
		super.setRecord("resultMsg", trxMap.getString("resultMsg"));
		super.setRecord("regDay", trxMap.getString("regDay"));
		
		boolean inserted = super.insert();
		logger.info("set PG_CHARGE_SETTLE_ERR : {}", inserted);
		super.initRecord();
		return inserted;
	}
	
	/**
	 * 가상계좌 2중 입급 건중 충전정산 거래정보 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getChargeSettleData(String vactId){
		String q = "SELECT * "
				+"	  FROM PG_CHARGE_SETTLE "
				+"   WHERE trxId = '" + vactId + "'";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 충전정산 2중입금 거래 건의 입금금액만큼 이후거래건들 잔액 차감 
	 * @param trxId
	 * @param mchtId
	 * @param netAmount
	 * @return
	 */
	public boolean updateChargeSettleBalance(String trxId, String mchtId, long netAmount){
		String q = "UPDATE PG_CHARGE_SETTLE "
					+ "    SET balance = balance - '" + netAmount + "'"
					+ "  WHERE trxId in "
					+ "  ("
					+ "		SELECT trxId "
					+ "   	  FROM PG_CHARGE_SETTLE"
					+ "		  WHERE regDate > "
					+ "			("
					+ "		 		SELECT regDate "
					+ "		   	  	  FROM PG_CHARGE_SETTLE " 
					+ "		     	 WHERE trxId = '" + trxId + "'"
					+ "			) and mchtId = '" + mchtId + "'"
					+ "	 )";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 2중출금 된 충전정산내역 삭제
	 * @param trxId
	 */
	public boolean deleteChargeSettle(String trxId) {
		super.setTable("PG_CHARGE_SETTLE");
		super.addWhere("trxId",trxId,eq);
		boolean deleted = super.delete();
		
		return deleted;
	}
}
