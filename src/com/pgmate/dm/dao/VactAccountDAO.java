package com.pgmate.dm.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class VactAccountDAO extends DAO {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.VactAccountDAO.class );

    public VactAccountDAO() {
        super.setDebug(false);
    }

    public void insertVactAccountStatusNoti(SharedMap<String, Object> ntsMap) {
        String query = " INSERT INTO PG_VACT_STATUS_NOTI (trxId, mchtId, vactAccount, vactStatus, holderName, hookAddr, retry, status, code, payLoad, resData, sentDate, regDay, regTime) " +
                " VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

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
            pstmt.setString(2, ntsMap.getString("mchtId"));
            pstmt.setString(3, ntsMap.getString("vactAccount"));
            pstmt.setString(4, ntsMap.getString("vactStatus"));
            pstmt.setString(5, ntsMap.getString("holderName"));
            pstmt.setString(6, ntsMap.getString("hookAddr"));
            pstmt.setInt(7, ntsMap.getInt("retry"));
            pstmt.setString(8, ntsMap.getString("status"));
            pstmt.setString(9, ntsMap.getString("code"));
            pstmt.setString(10, ntsMap.getString("payLoad"));
            pstmt.setString(11, ntsMap.getString("resData"));
            pstmt.setTimestamp(12, ntsMap.getTimestamp("sentDate"));
            pstmt.setString(13, ntsMap.getString("regDay"));
            pstmt.setString(14, ntsMap.getString("regTime"));
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
        super.setTable("PG_VACT_STATUS_NOTI");
        super.setColumns("*");
        super.addWhere("status","전송실패",eq);
        super.addWhere("retry <= 10");
        super.setOrderBy("regDate asc");
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public void updateVactAccountStatusNoti(SharedMap<String, Object> ntsMap) {
        String query = "UPDATE PG_VACT_STATUS_NOTI SET retry = retry + 1, code='"+ntsMap.getInt("code")+"', status='"+ntsMap.getString("status")+"', resData = '"+ntsMap.getString("resData")+"' WHERE  trxId = '"+ntsMap.getString("trxId")+"'";
        logger.info("update PG_VACT_STATUS_NOTI : {}", super.update(query));

        super.initRecord();
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
            logger.debug("sql error : {}, query : {}", t.getMessage(), query);
        } finally {
            db.close(conn, pstmt, rset);
        }
        return returnVal;
    }

    public synchronized static String getNotiId() {
        return "NT" + getFunction("FN_NEXTVAL2", "TRN");
    }

}
