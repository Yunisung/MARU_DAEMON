package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

public class CashReceiptDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.CashReceiptDAO.class );
	
	public CashReceiptDAO() {
		super.setDebug(false);
	}
	
	/**
	 * 현금영수증 발급오류내역 조회 대상 SELECT
	 * @param result
	 */
	public List<SharedMap<String,Object>> getCashErrList(){
		String q = "SELECT * "
				+"	  FROM PG_CASH_RECEIPT "
				+"   WHERE (errCd = '' OR errCd = null) AND resultCd = '0000' AND "
				+"   regDay = DATE_FORMAT(date_add(now(), INTERVAL -1 DAY),'%Y%m%d');";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}

	/**
	 * 현금영수증 발급오류내역 조회 대상 mid SELECT
	 */
	public List<SharedMap<String,Object>> getCashMid(){
		String q = "SELECT DISTINCT mid FROM PG_CASH_RECEIPT WHERE regDay = DATE_FORMAT(date_add(now(), INTERVAL -1 DAY),'%Y%m%d');";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	/**
	 * 현금영수증 발급오류내역 발생 UPDATE
	 */
	public boolean setErrSuccess(String errCd, String trDt, String authNo){
		boolean updateCheck = false;
		
		String q = "UPDATE PG_CASH_RECEIPT "
				+"	SET errCd = '" + errCd + "' WHERE orgTrDt = '" + trDt + "' AND authNo = '" + authNo + "'";
		
		updateCheck = super.update(q);
		logger.info("UPDATE setErrSuccess : {}",updateCheck);
		super.initRecord();
		
		return updateCheck;
	}
	
	/**
	 * 현금영수증 발급오류내역 결과 없는 경우 / 오류내역 처리 후 나머지 성공 처리된 거래건 일괄 UPDATE
	 * @param result
	 */
	public boolean setSuccess(String regDay){
		boolean insertCheck = false;
		
		String q = "UPDATE PG_CASH_RECEIPT "
				+"	SET errCd = '0000' WHERE (errCd = '' OR errCd = null) AND regDay = '" + regDay + "' AND resultCd = '0000'";
				
		insertCheck = super.update(q);
		
		logger.info("UPDATE setSuccess : {}, setRegDay : {}",insertCheck, regDay);
		super.initRecord();
		
		return insertCheck;
	}
	
	/**
	 * 현금영수증 등록대상 (가상계좌) 조회
	 * @param result
	 */
	public List<SharedMap<String,Object>> cashList(){
		String q = "SELECT * FROM PG_CASH_RECEIPT WHERE (errCd = '' OR errCd = null) AND assort='0' AND resultCd = '0000' AND stateCd = '전송대기' AND authNo IS NULL";

		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	/**
	 * 현금영수증 가상계좌 거래건 등록 결과 UPDATE
	 * @param result
	 */
	public void resultUpdate(String cashId, String stateCd, String resCd, String resMsg, String authNo, String regInfo, String errCd){
		String q = "UPDATE PG_CASH_RECEIPT "
				+  "   SET stateCd = '" + stateCd + "', resultCd = '" + resCd + "', resultMsg = '" + resMsg + 
						"', authNo = '" + authNo + "', regInfo = '" + regInfo + "', errCd '"+ errCd +"'"
				+  " WHERE cashId = '" + cashId + "'";
				
		logger.info("UPDATE resultUpdate : {}",super.update(q));
		super.initRecord();
	}
	
	/**
	 * 데이터 복호화
	 */
	public String getAESDec(String value){
		String query = "SELECT FN_AES_DEC('"+value+"') data";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("data");
		}
	}
}
