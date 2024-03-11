package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

public class WebHookDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Daemon.class );
	public WebHookDAO() {
		super.setDebug(false);
	}
	
	
	public void insertTrxNTSPG(SharedMap<String, Object> ntsMap) {
		super.setTable("PG_TRX_NTS_PG");
		super.setXssChange(false);

		super.setRecord("trxId", ntsMap.getString("trxId"));
		super.setRecord("trxType", ntsMap.getString("trxType"));
		super.setRecord("trackId", ntsMap.getString("trackId"));
		super.setRecord("vanId", ntsMap.getString("vanId"));
		super.setRecord("vanTrxId", ntsMap.getString("vanTrxId"));
		super.setRecord("amount", ntsMap.getLong("amount"));
		super.setRecord("authCd", ntsMap.getString("authCd"));
		super.setRecord("trxDay", ntsMap.getString("trxDay"));
		super.setRecord("webHookUrl", ntsMap.getString("webHookUrl"));
		super.setRecord("retry", ntsMap.getInt("retry"));
		super.setRecord("status", ntsMap.getString("status"));
		super.setRecord("code", ntsMap.getInt("code"));
		super.setRecord("payLoad", ntsMap.getString("payLoad"));
		super.setRecord("resData", ntsMap.getString("resData"));
		super.setRecord("sentDate", ntsMap.getTimestamp("sentDate"));
		super.setRecord("hookIdx", ntsMap.getLong("hookIdx"));
		super.setRecord("regDay", ntsMap.getString("regDay"));
		super.setRecord("regTime", ntsMap.getString("regTime"));
		
		logger.info("set TRX_NTS_PG : {}", super.insert());

		super.initRecord();
	}


	public List<SharedMap<String, Object>> getPayList() {
		String q = "SELECT E.* FROM ( "
				+ "SELECT A.idx AS hookIdx, A.hookUrl, B.* FROM "
				+ "(SELECT * FROM PG_MCHT_WEBHOOK WHERE trxType = 'CARD' AND STATUS = 'Y') A "
				+ "JOIN "
				+ "(SELECT C.mchtId,C.trxId,C.van,C.vanId,C.tmnId,concat(C.reqDay, C.regTime) AS trxDate,C.regDay AS trxDay,C.trackId,C.vanTrxId,C.authCd,C.cardType,C.issuer,C.acquirer,C.bin,C.last4,C.installment,C.amount,C.regDate,C.rentId, "
				+ " D.distId, D.agencyId, D.salesId "
				+ "FROM PG_TRX_PAY C join PG_MCHT D ON C.mchtId = D.mchtId WHERE C.regDay >= date_format(DATE_ADD(NOW(), INTERVAL -7 DAY),'%Y%m%d') AND C.vanTrxId NOT LIKE 'TX%') B "
				+ "ON A.id = "
				+ "CASE "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId END "
				+ "AND B.regDate >= A.regDate) E "
				+ "LEFT JOIN PG_TRX_NTS_PG F ON E.trxId = F.trxId AND E.hookIdx = F.hookIdx "
				+ "WHERE F.trxId IS null ORDER BY E.trxDate ASC ";
		/*
		String q = "SELECT A.idx AS hookIdx,A.hookUrl,B.mchtId,B.trxId,B.van,B.vanId,B.tmnId,concat(B.reqDay, B.regTime) AS trxDate,B.regDay AS trxDay,B.trackId,B.vanTrxId,B.authCd,B.cardType,B.issuer,B.acquirer,B.bin,B.last4,B.installment,B.amount "
				+ " FROM PG_MCHT_WEBHOOK A LEFT JOIN VW_TRX_PAY B ON "
				+ " A.id = "
				+ " CASE "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId END " 
				+ " AND A.trxType = 'CARD' AND A.status = 'Y' "
				+ "LEFT JOIN PG_TRX_NTS_PG C ON A.idx = C.hookIdx AND B.trxId = C.trxId "
				+ "WHERE B.regDate > DATE_ADD(NOW(), INTERVAL -7 day) AND B.regDate >= A.regDate AND C.trxId IS NULL AND B.vanTrxId NOT LIKE 'TX%'";*/
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getPayRetryList() {
		String q = "SELECT E.* FROM ( "
				+ "SELECT A.idx AS hookIdx, A.hookUrl, B.* FROM "
				+ "(SELECT * FROM PG_MCHT_WEBHOOK WHERE trxType = 'CARD' AND STATUS = 'Y') A "
				+ "JOIN "
				+ "(SELECT C.mchtId, C.trxId,C.van,C.vanId,C.tmnId,concat(C.reqDay, C.regTime) AS trxDate,C.regDay AS trxDay,C.trackId,C.vanTrxId,C.authCd,C.cardType,C.issuer,C.acquirer,C.bin,C.last4,C.installment,C.amount,C.regDate,C.rentId, "
				+ " D.distId, D.agencyId, D.salesId "
				+ "FROM PG_TRX_PAY C join PG_MCHT D ON C.mchtId = D.mchtId WHERE C.vanTrxId NOT LIKE 'TX%') B "
				+ "ON A.id = "
				+ "CASE "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId END "
				+ "AND B.regDate >= A.regDate) E "
				+ "LEFT JOIN PG_TRX_NTS_PG F ON E.trxId = F.trxId AND E.hookIdx = F.hookIdx "
				+ "WHERE F.status = '전송실패' AND F.retry < 10 ORDER BY E.trxDate ASC ";
		
//		String q = "SELECT E.* FROM ( "
//				+ "SELECT A.idx AS hookIdx, A.hookUrl, B.* FROM "
//				+ "(SELECT * FROM PG_MCHT_WEBHOOK WHERE trxType = 'CARD' AND STATUS = 'Y') A "
//				+ "JOIN "
//				+ "(SELECT C.mchtId,C.trxId,C.van,C.vanId,C.tmnId,concat(C.reqDay, C.regTime) AS trxDate,C.regDay AS trxDay,C.trackId,C.vanTrxId,C.authCd,C.cardType,C.issuer,C.acquirer,C.bin,C.last4,C.installment,C.amount,C.regDate, "
//				+ " D.distId, D.agencyId, D.salesId "
//				+ "FROM PG_TRX_PAY C join PG_MCHT D ON C.mchtId = D.mchtId WHERE C.regDay >= date_format(DATE_ADD(NOW(), INTERVAL -7 DAY),'%Y%m%d') AND C.vanTrxId NOT LIKE 'TX%') B "
//				+ "ON A.id = "
//				+ "CASE "
//				+ " When A.idType = 'distId' then B.distId "
//				+ " When A.idType = 'agencyId' then B.agencyId "
//				+ " When A.idType = 'salesId' then B.salesId "
//				+ " When A.idType = 'mchtId' then B.mchtId "
//				+ " When A.idType = 'tmnId' then B.tmnId END "
//				+ "AND B.regDate >= A.regDate) E "
//				+ "LEFT JOIN PG_TRX_NTS_PG F ON E.trxId = F.trxId AND E.hookIdx = F.hookIdx "
//				+ "WHERE F.status = '전송실패' AND F.retry < 10 ORDER BY E.trxDate ASC ";
		/*
		String q = "SELECT A.idx AS hookIdx,A.hookUrl,B.mchtId,B.trxId,B.van,B.vanId,B.tmnId,concat(B.reqDay, B.regTime) AS trxDate,B.regDay AS trxDay,B.trackId,B.vanTrxId,B.authCd,B.cardType,B.issuer,B.acquirer,B.bin,B.last4,B.installment,B.amount "
				+ " FROM PG_MCHT_WEBHOOK A LEFT JOIN VW_TRX_PAY B ON "
				+ " A.id = "
				+ " CASE "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId END " 
				+ " AND A.trxType = 'CARD' AND A.status = 'Y' "
				+ " LEFT JOIN PG_TRX_NTS_PG C ON A.idx = C.hookIdx AND B.trxId = C.trxId "
				+ " WHERE B.regDate > DATE_ADD(NOW(), INTERVAL -7 day) AND B.regDate >= A.regDate AND C.status = '전송실패' AND C.retry < 10";
		*/
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getRfdList() {
		String q = "SELECT E.* FROM ( "
				+ "SELECT A.idx AS hookIdx, A.hookUrl, B.* FROM "
				+ "(SELECT * FROM PG_MCHT_WEBHOOK WHERE trxType = 'CARD' AND STATUS = 'Y') A "
				+ "JOIN "
				+ "(SELECT C.mchtId,C.trxId,C.van,C.vanId,C.tmnId,concat(C.reqDay, C.regTime) AS trxDate,C.regDay AS trxDay,C.trackId,C.vanTrxId,C.authCd,D.cardType,C.issuer,C.acquirer,C.bin,C.last4,D.installment,ABS(C.rfdAmount) AS amount,C.rootTrxId,C.regDate, "
				+ " E.distId, E.agencyId, E.salesId, D.rentId "
				+ "FROM PG_TRX_RFD C join PG_TRX_PAY D ON C.rootTrxId = D.trxId join PG_MCHT E ON C.mchtId = E.mchtId WHERE C.regDay >= date_format(DATE_ADD(NOW(), INTERVAL -7 DAY),'%Y%m%d') AND C.status = '완료' and C.vanTrxId NOT LIKE 'TX%') B "
				+ "ON A.id = "
				+ "CASE "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId END "
				+ "AND B.regDate >= A.regDate) E "
				+ "LEFT JOIN PG_TRX_NTS_PG F ON E.trxId = F.trxId AND E.hookIdx = F.hookIdx "
				+ "WHERE F.trxId IS NULL ORDER BY E.trxDate ASC ";
		
		/*String q = "SELECT A.idx AS hookIdx,A.hookUrl,B.mchtId,B.trxId,B.van,B.vanId,B.tmnId,concat(B.reqDay, B.regTime) AS trxDate,B.regDay AS trxDay,B.trackId,B.vanTrxId,D.cardType,B.issuer,B.acquirer,B.bin,B.last4,D.installment,B.authCd,ABS(B.rfdAmount) AS amount,B.rootTrxId "
				+ " FROM PG_MCHT_WEBHOOK A LEFT JOIN VW_TRX_RFD B ON "
				+ "A.id = "
				+ "case "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId "
				+ " END "
				+ "AND A.trxType = 'CARD' AND A.status = 'Y' AND B.status = '완료' "
				+ " LEFT JOIN PG_TRX_PAY D ON B.rootTrxId = D.trxId "
				+ " LEFT JOIN PG_TRX_NTS_PG C ON A.idx = C.hookIdx AND B.trxId = C.trxId "
				+ " WHERE B.regDate > DATE_ADD(NOW(), INTERVAL -7 day) AND B.regDate >= A.regDate AND C.trxId IS NULL AND B.vanTrxId NOT LIKE 'TX%'";
				*/
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getRfdRetryList() {
		String q = "SELECT E.* FROM ( "
				+ "SELECT A.idx AS hookIdx, A.hookUrl, B.* FROM "
				+ "(SELECT * FROM PG_MCHT_WEBHOOK WHERE trxType = 'CARD' AND STATUS = 'Y') A "
				+ "JOIN "
				+ "(SELECT C.mchtId,C.trxId,C.van,C.vanId,C.tmnId,concat(C.reqDay, C.regTime) AS trxDate,C.regDay AS trxDay,C.trackId,C.vanTrxId,C.authCd,D.cardType,C.issuer,C.acquirer,C.bin,C.last4,D.installment,ABS(C.rfdAmount) AS amount,C.rootTrxId,C.regDate, "
				+ " E.distId, E.agencyId, E.salesId, D.rentId  "
				+ "FROM PG_TRX_RFD C join PG_TRX_PAY D ON C.rootTrxId = D.trxId join PG_MCHT E ON C.mchtId = E.mchtId WHERE C.status = '완료' and C.vanTrxId NOT LIKE 'TX%') B "
				+ "ON A.id = "
				+ "CASE "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId END "
				+ "AND B.regDate >= A.regDate) E "
				+ "LEFT JOIN PG_TRX_NTS_PG F ON E.trxId = F.trxId AND E.hookIdx = F.hookIdx "
				+ "WHERE F.status = '전송실패' AND F.retry < 10 ORDER BY E.trxDate ASC ";
//		String q = "SELECT E.* FROM ( "
//				+ "SELECT A.idx AS hookIdx, A.hookUrl, B.* FROM "
//				+ "(SELECT * FROM PG_MCHT_WEBHOOK WHERE trxType = 'CARD' AND STATUS = 'Y') A "
//				+ "JOIN "
//				+ "(SELECT C.mchtId,C.trxId,C.van,C.vanId,C.tmnId,concat(C.reqDay, C.regTime) AS trxDate,C.regDay AS trxDay,C.trackId,C.vanTrxId,C.authCd,D.cardType,C.issuer,C.acquirer,C.bin,C.last4,D.installment,ABS(C.rfdAmount) AS amount,C.rootTrxId,C.regDate, "
//				+ " E.distId, E.agencyId, E.salesId "
//				+ "FROM PG_TRX_RFD C join PG_TRX_PAY D ON C.rootTrxId = D.trxId join PG_MCHT E ON C.mchtId = E.mchtId WHERE C.regDay >= date_format(DATE_ADD(NOW(), INTERVAL -7 DAY),'%Y%m%d') AND C.status = '완료' and C.vanTrxId NOT LIKE 'TX%') B "
//				+ "ON A.id = "
//				+ "CASE "
//				+ " When A.idType = 'distId' then B.distId "
//				+ " When A.idType = 'agencyId' then B.agencyId "
//				+ " When A.idType = 'salesId' then B.salesId "
//				+ " When A.idType = 'mchtId' then B.mchtId "
//				+ " When A.idType = 'tmnId' then B.tmnId END "
//				+ "AND B.regDate >= A.regDate) E "
//				+ "LEFT JOIN PG_TRX_NTS_PG F ON E.trxId = F.trxId AND E.hookIdx = F.hookIdx "
//				+ "WHERE F.status = '전송실패' AND F.retry < 10 ORDER BY E.trxDate ASC ";
		/*String q = "SELECT A.idx AS hookIdx,A.hookUrl,B.mchtId,B.trxId,B.van,B.vanId,B.tmnId,concat(B.reqDay, B.regTime) AS trxDate,B.regDay AS trxDay,B.trackId,B.vanTrxId,D.cardType,B.issuer,B.acquirer,B.bin,B.last4,D.installment,B.authCd,ABS(B.rfdAmount) AS amount,B.rootTrxId "
				+ " FROM PG_MCHT_WEBHOOK A LEFT JOIN VW_TRX_RFD B ON "
				+ " A.id = "
				+ " case "
				+ " When A.idType = 'distId' then B.distId "
				+ " When A.idType = 'agencyId' then B.agencyId "
				+ " When A.idType = 'salesId' then B.salesId "
				+ " When A.idType = 'mchtId' then B.mchtId "
				+ " When A.idType = 'tmnId' then B.tmnId "
				+ " END "
				+ " AND A.trxType = 'CARD' AND A.status = 'Y' AND B.status = '완료' "
				+ " LEFT JOIN PG_TRX_PAY D ON B.rootTrxId = D.trxId "
				+ " LEFT JOIN PG_TRX_NTS_PG C ON A.idx = C.hookIdx AND B.trxId = C.trxId "
				+ " WHERE B.regDate > DATE_ADD(NOW(), INTERVAL -7 day) AND B.regDate >= A.regDate AND C.status = '전송실패' AND C.retry < 10";*/
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public SharedMap<String,Object> getTrxIo3d(String trxId) {
		super.setTable("PG_TRX_IO_3D");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.setLimit(1);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		} else {
			return rset.getRow(0);
		}
	}

	public SharedMap<String,Object> getTrxIo(String trxId) {
		super.setTable("PG_TRX_IO");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.setLimit(1);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		} else {
			return rset.getRow(0);
		}
	}


	public void updateTrxNTSPG(SharedMap<String, Object> ntsMap) {
		String query = "UPDATE PG_TRX_NTS_PG SET retry = retry + 1, status='"+ntsMap.getString("status")+"' WHERE  trxId = '"+ntsMap.getString("trxId")+"' AND hookIdx = "+ntsMap.getLong("hookIdx");
		logger.info("update TRX_NTS_PG : {}", super.update(query));

		super.initRecord();
	}

	public String getRebillTrackId(String rebillId) {
		super.setTable("PG_REBILL_REG");
		super.setColumns("trackId AS rebillTrackId");
		super.addWhere("rebillId", rebillId);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("rebillTrackId");
	}

	public String getTrxType(String trxId) {
		super.setTable("PG_TRX_REQ");
		super.setColumns("trxType");
		super.addWhere("trxId", trxId);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("trxType");
	}

	public String getRootTrackId(String trxId) {
		String q = "SELECT trackId FROM VW_TRX_CAP WHERE capId = (SELECT rootTrxId FROM VW_TRX_CAP WHERE trxId='" + trxId + "')";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRowFirst().getString("trackId");
	}

	public String getRebillId(String trackId) {
		super.setTable("PG_REBILL_PAY");
		super.setColumns("rebillId");
		super.addWhere("trackId", trackId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("rebillId");
	}
}
