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
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class ChargeSettlePayOutDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.ChargeSettlePayOutDAO.class );
	
	public ChargeSettlePayOutDAO() {
		super.setDebug(false);
	}
	
	/**
	 * 충전정산 출금대상거래 에서 출금하지 않은 데이터 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getChargeSettleFirmList(){
		String q = "SELECT A.*, FN_AES_DEC(A.account) as decAccount, B.vactBankCd "
				+"	FROM PG_CHARGE_SETTLE_FIRM A INNER JOIN PG_MCHT_MNG_VACT B"
				+"  ON A.mchtId = B.mchtId"
				+"  WHERE A.status != '완료' and A.status != '전송' and A.retry < 2 "
				+"  order by A.regDate";
		
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
		String q = "UPDATE PG_CHARGE_SETTLE_FIRM "
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
		super.setTable("PG_CHARGE_SETTLE_FIRM");
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
				+"	  	   FROM PG_CHARGE_SETTLE_FIRM "
				+"   	  WHERE trxId = '" + trxId + "')";
		
		RecordSet rset = super.query(q);

		super.initRecord();
		return rset.getRowFirst().getString("seqNo");
	}
	
	/**
	 * 가상계좌 충전 출금 결과 업데이트
	 * @param trxId : 거래번호
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean updatePayOutRes(String trxId, String resCd, String resMsg, String status){
		String q = "UPDATE PG_CHARGE_SETTLE_FIRM "
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
		String q = "UPDATE PG_CHARGE_SETTLE "
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
		String q = "UPDATE PG_CHARGE_SETTLE_FIRM "
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
	 * 출금 실패한 거래 건의 출금예정액만큼 이후거래건들 잔액 추가 
	 * @param trxId
	 * @param stlAmount
	 * @return
	 */
	public boolean updateChargeSettleBalance(String trxId, String mchtId, long netAmount){
		String q = "UPDATE PG_CHARGE_SETTLE "
				+ "    SET balance = balance + " + netAmount +" "
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
	 * VA_TRX_ERR에 최종 실패 건 추가
	 * @param trxMap
	 * @return
	 */
	public boolean insertTrxErr(SharedMap<String, Object> trxMap) {
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
		super.setRecord("regId", trxMap.getString("regId"));
		super.setRecord("regDay", trxMap.getString("regDay"));
		
		boolean inserted = super.insert();
		logger.info("set trx : {}", inserted);
		super.initRecord();
		return inserted;
	}
	
	/**
	 * 출금 실패한 거래건 VA_TRX테이블 데이터 삭제
	 * @param trxId
	 */
	public void deleteChargeSettle(String trxId) {
		super.setTable("PG_CHARGE_SETTLE");
		super.addWhere("trxId",trxId,eq);
		super.delete();
	}
	
	/**
	 * 삭제될 거래건의 잔액조회
	 * @param trxId
	 * @return
	 */
	public long getBalance(String trxId){
		super.setTable("PG_CHARGE_SETTLE");
		super.setColumns("balance");
		super.addWhere("trxId",trxId,eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getLong("balance");
	}

	public SharedMap<String, Object> getChargeSettle(String trxId) {
		super.setTable("PG_CHARGE_SETTLE");
		super.setColumns("*");
		super.addWhere("trxId",trxId,eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}

	public List<SharedMap<String, Object>> getChargeSettleErr(String trxId) {
		super.setTable("PG_CHARGE_SETTLE_ERR");
		super.setColumns("*");
		super.addWhere("trxId",trxId,eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public int getChargeErrCount(String trxId) {
		String query = "SELECT COUNT(*) as cnt FROM PG_CHARGE_SETTLE_ERR WHERE trxId='" + trxId + "'";
		RecordSet rset = super.query(query);
		super.initRecord();
		return rset.getRowFirst().getInt("cnt");
	}

	public SharedMap<String, Object> getMchtChargeMng(String mchtId) {
		super.setTable("PG_MCHT_CHARGE_MNG");
		super.setColumns("*");
		super.addWhere("mchtId",mchtId,eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}

	public void insertChargeSettleNoti(SharedMap<String, Object> ntsMap) {
		/*super.setTable("PG_CHARGE_SETTLE_NOTI");

		super.setRecord("trxId", ntsMap.getString("trxId"));
		super.setRecord("trxType", ntsMap.getString("trxType"));
		super.setRecord("mchtId", ntsMap.getString("mchtId"));
		super.setRecord("trackId", ntsMap.getString("trackId"));
		super.setRecord("hookAddr", ntsMap.getString("hookAddr"));
		super.setRecord("retry", ntsMap.getInt("retry"));
		super.setRecord("status", ntsMap.getString("status"));
		super.setRecord("code", ntsMap.getInt("code"));
		super.setRecord("payLoad", ntsMap.getString("payLoad"));
		super.setRecord("resData", ntsMap.getString("resData"));
		super.setRecord("sentDate", ntsMap.getTimestamp("sentDate"));
		super.setRecord("regDay", ntsMap.getString("regDay"));
		super.setRecord("regTime", ntsMap.getString("regTime"));
		
		logger.info("set PG_CHARGE_SETTLE_NOTI : {}", super.insert());

		super.initRecord();*/

		String query = " INSERT INTO PG_CHARGE_SETTLE_NOTI (trxId, trxType, mchtId, trackId, hookAddr, retry, status, code, payLoad, resData, sentDate, regDay, regTime) " +
				" VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

		RecordSet rset = new RecordSet();
		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet resultSet = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, ntsMap.getString("trxId"));
			pstmt.setString(2, ntsMap.getString("trxType"));
			pstmt.setString(3, ntsMap.getString("mchtId"));
			pstmt.setString(4, ntsMap.getString("trackId"));
			pstmt.setString(5, ntsMap.getString("hookAddr"));
			pstmt.setInt(6, ntsMap.getInt("retry"));
			pstmt.setString(7, ntsMap.getString("status"));
			pstmt.setString(8, ntsMap.getString("code"));
			pstmt.setString(9, ntsMap.getString("payLoad"));
			pstmt.setString(10, ntsMap.getString("resData"));
			pstmt.setTimestamp(11, ntsMap.getTimestamp("sentDate"));
			pstmt.setString(12, ntsMap.getString("regDay"));
			pstmt.setString(13, ntsMap.getString("regTime"));
			pstmt.executeQuery();

			resultSet = pstmt.getResultSet();

			if(resultSet != null) {
				rset = new RecordSet(resultSet);
			}

		} catch (Exception e) {
			logger.debug("sel error : {}, query : {}", e.getMessage(), query);
		} finally {
			db.close(conn, pstmt, resultSet);
		}

	}

	public List<SharedMap<String, Object>> getRetryNotiList() {
		super.setDebug(true);
		super.setTable("PG_CHARGE_SETTLE_NOTI");
		super.setColumns("*");
		super.addWhere("status","전송실패",eq);
		super.addWhere("retry <= 10");
		super.setOrderBy("regDate asc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}
	
	public void updateChargeSettleNoti(SharedMap<String, Object> ntsMap) {
		String query = "UPDATE PG_CHARGE_SETTLE_NOTI SET retry = retry + 1, code='"+ntsMap.getInt("code")+"', status='"+ntsMap.getString("status")+"', resData = '"+ntsMap.getString("resData")+"' WHERE  trxId = '"+ntsMap.getString("trxId")+"'";
		logger.info("update PG_CHARGE_SETTLE_NOTI : {}", super.update(query));

		super.initRecord();
	}

	/**
	 * 충전정산 출금대상거래 에서 idx가 비어서 응답이 올경우 검색해서 idx값 재조회
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
}
