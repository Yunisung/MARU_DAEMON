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

public class WelcomeDiffUploadDAO extends DAO {
    private static Logger logger = LoggerFactory.getLogger( WelcomeDiffUploadDAO.class );

    public WelcomeDiffUploadDAO() {
    }

    public List<SharedMap<String, Object>> getMchtList() {
        super.setTable("PG_MCHT_DIFF_UPLOAD");
        super.setColumns("*");
        super.addWhere("recordType","A",eq);
        super.addWhere("vanName","WELCOME",eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public List<SharedMap<String, Object>> getMidList() {
        super.setTable("PG_VAN");
        super.setColumns("vanId");
//        super.addWhere("van", "%WELCOME%", lk);
        super.addWhere("van", "WELCOME", eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public int updateDiffMcht(List<SharedMap<String, Object>> mchtList) {
        int updated = 0;
        logger.debug("update PG_MCHT_DIFF_UPLOAD batch : {}", mchtList.size());
        String query = "UPDATE `PG_MCHT_DIFF_UPLOAD` SET recordType = 'D' WHERE mchtId = ? ;";

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

    public List<SharedMap<String, Object>> getPayList(String mid) {
        String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(B.identity) AS mchtCompNo, A.vanTrxId, A.amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '6758600152' AS compNo, '0' AS trxType, '0' AS rfdTurn "
                + "FROM PG_TRX_PAY A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId  "
                + "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
                + "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
                + "WHERE A.regDay = DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d')"
                + "AND E.trxId IS NULL "
                + "AND A.vanid = '" + mid +"' "
                + "AND A.vanTrxId NOT LIKE 'TX%' ";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public List<SharedMap<String,Object>> getRfdList(String mid){
        String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(F.identity) AS mchtCompNo, A.vanTrxId, ABS(A.rfdAmount) AS amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '6758600152' AS compNo,"
                + "A.rootTrxId, A.reqTime as trxTime, G.vanTrxId as rootVanTrxId, G.trxDay as rootTrxDay "
                + "'1' as trxType, "
                + "'0' AS rfdTurn "
                + "FROM PG_TRX_RFD A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId "
                + "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
                + "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
                + "LEFT JOIN VW_TRX_PAY_LIST F ON A.rootTrxId = F.trxId "
                + "LEFT JOIN PG_TRX_PAY G ON A.rootTrxId = G.trxId "
                + "WHERE A.rfdAll = '전액' "
                + "AND A.regDay = DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d')"
                + "AND E.trxId IS NULL "
                + "AND A.vanid = '" + mid + "' "
                + "AND A.vanTrxId NOT LIKE 'TX%' "
                + "AND A.status = '완료' ";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public List<SharedMap<String,Object>> getRootTrxList(String mid){
        String q = "SELECT A.rootTrxId FROM "
                + "PG_TRX_RFD A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId "
                + "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
                + "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
                + "LEFT JOIN VW_TRX_PAY_LIST F ON A.rootTrxId = F.trxId "
                //------------------------ GALAXIA 맞게 수정 필요
                + "AND A.rfdAll = '부분' "
                + "AND A.regDay = DATE_FORMAT(NOW() - INTERVAL 1 DAY, '%Y%m%d')"
                + "AND E.trxId IS NULL "
                + "AND A.vanid = '" + mid + "' "
                + "AND A.vanTrxId NOT LIKE 'TX%' "
                + "AND A.status = '완료' "
                + "GROUP BY A.rootTrxId";


        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public String getLastRfdTurn(String rootTrxId) {
        String q = "SELECT B.rfdTurn "
                + "FROM PG_TRX_RFD A INNER JOIN PG_TRX_DIFF B ON A.trxId = B.trxId "
                + "WHERE A.rootTrxId='" + rootTrxId + "' ORDER BY B.rfdTurn desc";

        RecordSet rset = super.query(q);
        super.initRecord();
        if(rset.size() == 0) {
            return "";
        } else {
            return rset.getRow(0).getString("rfdTurn");
        }
    }

    public List<SharedMap<String,Object>> getPartialTrx(String rootTrxId){
        String q = "SELECT A.vanId,A.reqDay AS trxDay, FN_AES_DEC(F.identity) AS mchtCompNo, A.vanTrxId, ABS(A.rfdAmount) AS amount, A.trxId, A.mchtId, A.van, A.tmnId, 'D' AS recordType, 'PG' AS systemType, '6758600152' AS compNo,"
                + "A.rootTrxId, A.rootAmount, A.reqTime as trxTime, G.vanTrxId as rootVanTrxId "
                + "'3' as trxType "
                + "FROM PG_TRX_RFD A INNER JOIN PG_MCHT B on A.mchtId = B.mchtId "
                + "INNER JOIN PG_MCHT_MNG C ON A.mchtId = C.mchtId "
                + "LEFT JOIN PG_TRX_DIFF E ON A.trxId = E.trxId "
                + "LEFT JOIN VW_TRX_PAY_LIST F ON A.rootTrxId = F.trxId "
                + "LEFT JOIN PG_TRX_PAY G ON A.rootTrxId = G.trxId "
                + "WHERE A.rootTrxId = '" + rootTrxId + "'"
                + "AND E.trxId IS NULL "
                + "ORDER BY A.regDay, A.regTime";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public int insertTrxDiffUpload(List<SharedMap<String, Object>> trxList, String regDay) {
        int inserted = 0;
        logger.info("insert PG_TRX_DIFF batch : {}", trxList.size());
        String query = "INSERT INTO `PG_TRX_DIFF` (`trxId`, `mchtId`, `tmnId`, `recordType`, `systemType`, `van`, `vanId`, `vanTrxId`, `trxType`, `trxDay`, `compNo`, `mchtCompNo`, `rfdTurn`, `mchtSalesAmt`, `amount`, `cardType`, `regDay`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

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
                if(map.getString("cardType").equals("신용")) {
                    pstmt.setString(i++, "0");
                } else {
                    pstmt.setString(i++, "1");
                }
                pstmt.setString(i++, regDay);
                pstmt.addBatch();
                if (++count % batchSize == 0) {
                    inserted += pstmt.executeBatch().length;
                }
            }

            inserted += pstmt.executeBatch().length;
            conn.commit();
        } catch (Exception e) {
            logger.error("insert batch PG_TRX_DIFF error : {}", CommonUtil.getExceptionMessage(e));
        } finally {
            db.close(pstmt);
            db.close(conn);
        }
        return inserted;
    }

    public int getLastSeq() {
        super.setTable("PG_MCHT_DIFF_UPDLOAD");
        super.setColumns("SUBSTRING(welSeq,8) AS welSeq");
        super.addWhere("vanName", "WELCOME", eq);
        super.addWhere("recordType", "D", eq);
        super.setOrderBy("regDate DESC");
        super.setLimit(1);

        String welSeq = super.search().getString("welSeq");
        super.initRecord();

        return Integer.parseInt(welSeq);
    }
}
