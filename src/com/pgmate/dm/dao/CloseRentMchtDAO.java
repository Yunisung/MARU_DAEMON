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

public class CloseRentMchtDAO extends DAO {
    private static Logger logger = LoggerFactory.getLogger( CloseRentMchtDAO.class );

    public List<SharedMap<String, Object>> getCloseMchtList(String cmd) {
        super.setTable("PG_MCHT_RENT");
        super.setColumns("*");
        super.addWhere("rentEndDay", cmd);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public int closeMcht(List<SharedMap<String, Object>> mchtList) {
        int updated = 0;
        logger.debug("UPDATE PG_MCHT BATCH : {}", mchtList.size());
        String query = "UPDATE PG_MCHT SET status = '중지' WHERE mchtId=?";

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
            logger.debug("UPDATE PG_MCHT ERROR : {}", CommonUtil.getExceptionMessage(e));
        } finally {
            db.close(pstmt);
            db.close(conn);
        }
        return updated;
    }
}
