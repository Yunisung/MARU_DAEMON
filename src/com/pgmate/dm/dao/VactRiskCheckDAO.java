package com.pgmate.dm.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

public class VactRiskCheckDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.VactRiskCheckDAO.class );

	public List<SharedMap<String, Object>> getTrxList() {
		super.setTable("PG_VACT_TRX_WITHDRAW");
		super.setColumns("*");
		super.addWhere("trxDay", "DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d')");
		super.addWhere("trxType", "입금");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getWithdrawAccountTotalList() {
		super.setTable("PG_VACT_TRX_WITHDRAW");
		super.setColumns("*");
		super.addWhere("(withdrawAccount, trxDay) IN " +
				"(SELECT withdrawAccount, trxDay FROM (SELECT COUNT(withdrawAccount) AS count, SUM(if(trxType='입금',amount,0)) AS sumAmount, withdrawAccount, trxDay " +
				"FROM PG_VACT_TRX_WITHDRAW WHERE trxDay=DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') GROUP BY withdrawAccount) A WHERE COUNT>=10 AND sumAmount>=40000000)");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getAccountTotalList() {
		super.setTable("PG_VACT_TRX_WITHDRAW");
		super.setColumns("*");
		super.addWhere("(account, trxDay) IN " +
				"(SELECT account, trxDay FROM (SELECT COUNT(account) AS count, SUM(if(trxType='입금',amount,0)) AS sumAmount, account, trxDay " +
				"FROM PG_VACT_TRX_WITHDRAW WHERE trxDay=DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') GROUP BY account) A WHERE COUNT>=10 AND sumAmount>=40000000)");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getLimitDayList() {
		super.setTable("PG_VACT_TRX_WITHDRAW");
		super.setColumns("*");
		super.addWhere("(withdrawAccount, trxDay) IN " +
				"(SELECT withdrawAccount, trxDay FROM (SELECT SUM(if(trxType='입금',amount,0)) AS sumAmount, withdrawAccount, trxDay " +
				"FROM PG_VACT_TRX_WITHDRAW WHERE trxDay=DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') GROUP BY withdrawAccount) A WHERE sumAmount>=30000000)");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getLimitOnceList() {
		super.setTable("PG_VACT_TRX_WITHDRAW");
		super.setColumns("*");
		super.addWhere("trxDay=DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d')");
		super.addWhere("amount", 3000000, ge);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getLimitCountList() {
		super.setTable("PG_VACT_TRX_WITHDRAW");
		super.setColumns("*");
		super.addWhere("(withdrawAccount, trxDay) IN " +
				"(SELECT withdrawAccount, trxDay FROM (SELECT COUNT(withdrawAccount) AS count, withdrawAccount, trxDay " +
				"FROM PG_VACT_TRX_WITHDRAW WHERE trxDay=DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') GROUP BY withdrawAccount) A WHERE COUNT>=7)");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();

	}

	public void insertVactTrxRisk(SharedMap<String, SharedMap> riskMap) {
		int inserted = 0;
		logger.info("insert PG_VACT_TRX_RISK batch : {}", riskMap.size());

		String query = "INSERT INTO PG_VACT_TRX_RISK (vactId,mchtId,mchtName,bankCd,account,amount,holderName,trxType,withdrawBankCd,withdrawBankName,withdrawAccount,risk,trxDay,trxTime,regDay) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;
			String regDay = CommonUtil.getCurrentDate("yyyyMMdd");

			for (Map.Entry<String, SharedMap> map : riskMap.entrySet()) {
				//logger.info("vactId : {}, risk : {}", map.getKey(), map.getValue().getString("risk"));
				int i = 1;
				pstmt.setString(i++, map.getValue().getString("vactId"));
				pstmt.setString(i++, map.getValue().getString("mchtId"));
				pstmt.setString(i++, map.getValue().getString("mchtName"));
				pstmt.setString(i++, map.getValue().getString("bankCd"));
				pstmt.setString(i++, map.getValue().getString("account"));
				pstmt.setString(i++, map.getValue().getString("amount"));
				pstmt.setString(i++, map.getValue().getString("sender"));
				pstmt.setString(i++, map.getValue().getString("trxType"));
				pstmt.setString(i++, map.getValue().getString("withdrawBankCd"));
				pstmt.setString(i++, map.getValue().getString("withdrawBankName"));
				pstmt.setString(i++, map.getValue().getString("withdrawAccount"));
				pstmt.setString(i++, map.getValue().getString("risk"));
				pstmt.setString(i++, map.getValue().getString("trxDay"));
				pstmt.setString(i++, map.getValue().getString("trxTime"));
				pstmt.setString(i++, regDay);
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			inserted += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.error("insert batch PG_VACT_TRX_RISK error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
	}

	public List<SharedMap<String, Object>> getRegLimitList() {
		super.setTable("(SELECT * FROM HT_VACT_REG WHERE (withdrawAccount, regDay) IN (SELECT withdrawAccount, regDay " +
//				"FROM (SELECT COUNT(withdrawAccount) AS count, withdrawAccount, regDay FROM HT_VACT_REG WHERE regDay=DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d') " +
				"FROM (SELECT COUNT(withdrawAccount) AS count, withdrawAccount, regDay FROM HT_VACT_REG WHERE regDay='20250406' " +
				"AND trxType='0' GROUP BY withdrawAccount) A WHERE COUNT>=2) AND trxType='0') B " +
				"LEFT OUTER JOIN HT_VACT_DTL C ON B.trackId=C.trackId " +
				"LEFT OUTER JOIN PG_MCHT D ON B.mchtId=D.mchtId ");
		super.setColumns("C.issueId, D.name as mchtName, B.*");
		super.setOrderBy("B.regDate desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}

	public void insertVactRegRisk(SharedMap<String, SharedMap> riskMap) {
		int inserted = 0;
		logger.info("insert PG_VACT_REG_RISK batch : {}", riskMap.size());

		String query = "INSERT INTO PG_VACT_REG_RISK (issueId,mchtId,mchtName,bankCd,account,withdrawBankCd,withdrawAccount,holderName,trackId,risk,pubDay,pubTime,regDay) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?);";

		DBManager db = null;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);

			int batchSize = 100;
			int count = 0;
			String regDay = CommonUtil.getCurrentDate("yyyyMMdd");

			for (Map.Entry<String, SharedMap> map : riskMap.entrySet()) {
				int i = 1;
				pstmt.setString(i++, map.getValue().getString("issueId"));
				pstmt.setString(i++, map.getValue().getString("mchtId"));
				pstmt.setString(i++, map.getValue().getString("mchtName"));
				pstmt.setString(i++, map.getValue().getString("bankCd"));
				pstmt.setString(i++, map.getValue().getString("account"));
				pstmt.setString(i++, map.getValue().getString("withdrawBankCd"));
				pstmt.setString(i++, map.getValue().getString("withdrawAccount"));
				pstmt.setString(i++, map.getValue().getString("holderName"));
				pstmt.setString(i++, map.getValue().getString("trackId"));
				pstmt.setString(i++, map.getValue().getString("risk"));
				pstmt.setString(i++, map.getValue().getString("regDay"));
				pstmt.setString(i++, map.getValue().getString("regTime"));
				pstmt.setString(i++, regDay);
				pstmt.addBatch();
				if (++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			inserted += pstmt.executeBatch().length;
			conn.commit();
		} catch (Exception e) {
			logger.error("insert batch PG_VACT_TRX_RISK error : {}", CommonUtil.getExceptionMessage(e));
		} finally {
			db.close(pstmt);
			db.close(conn);
		}
	}
}
