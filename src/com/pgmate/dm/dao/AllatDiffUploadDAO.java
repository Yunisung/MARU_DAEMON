package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
public class AllatDiffUploadDAO extends DAO{

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.AllatDiffUploadDAO.class );

	public AllatDiffUploadDAO() {
	}
	
	public List<SharedMap<String,Object>> getPayList(){
		String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(B.identity) AS mchtCompNo, A.vanTrxId, A.amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '4198800046' AS compNo, '0' AS trxType, '0' AS rfdTurn "
				+ "FROM PG_TRX_PAY A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId  "
				+ "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
				+ "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
				+ "WHERE A.van like 'ALLAT%' "
				+ "AND CASE C.diffType WHEN '일반' THEN A.reqDay BETWEEN '20200101' AND DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') "
				+ "ELSE A.reqDay <= DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') END "
				+ "AND E.trxId IS NULL "
				+ "AND A.vanid IN ('2006500004','2006500007','2006500009') "
				+ "AND A.mchtId !='ktest' "
				+ "AND A.vanTrxId NOT LIKE 'TX%' ";
				
				
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public List<SharedMap<String,Object>> getRfdList(){
		String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(B.identity) AS mchtCompNo, A.vanTrxId, ABS(A.rfdAmount) AS amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '4198800046' AS compNo, '1' AS trxType, rfdTurn, A.rootTrxId "
				+ "FROM PG_TRX_RFD A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId "
				+ "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
				+ "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
				+ "WHERE A.van like 'ALLAT%' "
				+ "AND CASE C.diffType WHEN '일반' THEN A.reqDay BETWEEN '20200101' AND DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d')  "
				+ "ELSE A.reqDay <= DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') END "
				+ "AND E.trxId IS NULL "
				+ "AND A.vanid IN ('2006500004','2006500007','2006500009') "
				+ "AND A.mchtId != 'ktest' "
				+ "AND A.vanTrxId NOT LIKE 'TX%' "
				+ "AND A.status = '완료' "
				+ "AND A.regDay <= DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') ";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public List<SharedMap<String,Object>> getRePayList(String date){
		String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(B.identity) AS mchtCompNo, A.vanTrxId, A.amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '4198800046' AS compNo, '0' AS trxType, '0' AS rfdTurn "
				+ "FROM PG_TRX_PAY A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId  "
				+ "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
				+ "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
				+ "WHERE A.van like 'ALLAT%' "
				+ "AND CASE C.diffType WHEN '일반' THEN A.reqDay BETWEEN '20200101' AND '" + date + "'"
				+ "ELSE A.reqDay <= '" + date + "' END "
				+ "AND E.trxId IS NULL "
				+ "AND A.vanid IN ('2006500004','2006500007','2006500009') "
				+ "AND A.mchtId !='ktest' "
				+ "AND E.recordType = 'F'"
				+ "AND A.vanTrxId NOT LIKE 'TX%' ";
				
				
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public List<SharedMap<String,Object>> getReRfdList(String date){
		String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(B.identity) AS mchtCompNo, A.vanTrxId, ABS(A.rfdAmount) AS amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '4198800046' AS compNo, '1' AS trxType, '0' AS rfdTurn, A.rootTrxId "
				+ "FROM PG_TRX_RFD A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId "
				+ "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
				+ "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
				+ "WHERE A.van like 'ALLAT%' "
				+ "AND CASE C.diffType WHEN '일반' THEN A.reqDay BETWEEN '20200101' AND '" + date + "'"
				+ "ELSE A.reqDay <= '" + date + "' END "
				+ "AND E.trxId IS NULL "
				+ "AND A.vanid IN ('2006500004','2006500007','2006500009') "
				+ "AND A.mchtId != 'ktest' "
				+ "AND A.vanTrxId NOT LIKE 'TX%' "
				+ "AND A.status = '완료' "
				+ "AND E.recordType = 'F'"
				+ "AND A.regDay <= '" + date + "' ";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public int insertTrxDiffUpload(List<SharedMap<String, Object>> trxList, String regDay) {
		int inserted = 0;
		logger.debug("insert PG_TRX_DIFF batch : {}", trxList.size());
		String query = "INSERT INTO `PG_TRX_DIFF` (`trxId`, `mchtId`, `tmnId`, `recordType`, `systemType`, `van`, `vanId`, `vanTrxId`, `trxType`, `trxDay`, `compNo`, `mchtCompNo`, `rfdTurn`, `mchtSalesAmt`, `amount`, `regDay`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;

			for (SharedMap<String, Object> map : trxList) {
				int i = 1;
				pstmt.setString(i++, map.getString("trxId"));
				pstmt.setString(i++, map.getString("mchtId"));
				pstmt.setString(i++, map.getString("tmnId"));
				pstmt.setString(i++, map.getString("recordType"));
				pstmt.setString(i++, map.getString("systemType"));
				pstmt.setString(i++, map.getString("van"));
				pstmt.setString(i++, map.getString("vanId"));
				pstmt.setString(i++, map.getString("vanTrxId"));
				pstmt.setString(i++, map.getString("trxType"));
				pstmt.setString(i++, map.getString("trxDay"));
				pstmt.setString(i++, map.getString("compNo"));
				pstmt.setString(i++, map.getString("mchtCompNo"));
				pstmt.setString(i++, map.getString("rfdTurn"));
				pstmt.setLong(i++, map.getLong("amount"));
				pstmt.setLong(i++, map.getLong("amount"));
				pstmt.setString(i++, regDay);
				
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}

			inserted += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.debug("insert batch PG_TRX_DIFF error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return inserted;
	}
	public boolean isRestDay(String stlDay){
		boolean bRes = false;
		
		String q = "SELECT status FROM PG_CODE_HOLIDAY WHERE days = '" + stlDay + "'";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		SharedMap<String,Object> res = rset.getRow(0);
		if(res != null && res.size() > 0) {
			bRes = res.getString("status").equals("yes") ? true : false; 
		}
		
		return bRes;
	}

	public List<SharedMap<String, Object>> getMchtList() {
		super.setTable("PG_MCHT_DIFF_UPLOAD");
		super.setColumns("*");
		super.addWhere("recordType","A",eq);
		super.addWhere("vanName","ALLAT",eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public int updateDiffMcht(List<SharedMap<String, Object>> mchtList) {
		int updated = 0;
		logger.debug("update PG_MCHT_DIFF_UPLOAD batch : {}", mchtList.size());
		String query = "UPDATE `PG_MCHT_DIFF_UPLOAD` SET recordType = 'D', seq = ? WHERE mchtId = ? ;";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;

			for (SharedMap<String, Object> map : mchtList) {
				int i = 1;
				pstmt.setString(i++, getSeq(count));
				pstmt.setString(i++, map.getString("mchtId"));
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					updated += pstmt.executeBatch().length;
				}
			}

			updated += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.debug("UPDATE batch PG_MCHT_DIFF_UPLOAD error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
		return updated;
	}
	
	private String getSeq(int i) {
		String nowDate = CommonUtil.getCurrentDate("yyMMdd");
		String seq = "";
		seq = nowDate + "_" + CommonUtil.zerofill(i, 5);;
		
		return seq;
	}
}


