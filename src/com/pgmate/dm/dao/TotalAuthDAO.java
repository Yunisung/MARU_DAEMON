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

public class TotalAuthDAO extends DAO {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public TotalAuthDAO() { super.setDebug(false);}

    public List<SharedMap<String, Object>> getTotalAuthFeeList(String stlDay, String stlType) {
        String q = "SELECT mchtId, stlType, SUM(authFee) AS authFee, SUM(authFeeVat) AS authFeeVat "
                +" FROM PG_TOTAL_AUTH "
                +" WHERE  stlDay = '"+stlDay+ "' and stlType = '" + stlType +  "' AND stlStatus != '정산완료' "
                +" GROUP BY mchtId, stlType";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public synchronized String getSettleId() {
        return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
    }

    public boolean insertSettleAuto(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE_AUTO");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }

    public boolean updateTotalAuthStlId(String stlId, String mchtId, String stlDay, String stlType) {
        String q = "UPDATE PG_TOTAL_AUTH"
                + "    SET stlId = '" + stlId +"'"
                + "	 WHERE mchtId = '" + mchtId + "' and stlDay = '" + stlDay + "' and stlType = '" + stlType + "'";

        boolean updated =  super.update(q);

        super.initRecord();
        return updated;
    }

    public List<SharedMap<String,Object>> getTotalAuthPayOutList(String stlDay, String stlType) {
        String q = "SELECT * "
                +"	  FROM PG_CHARGE_SETTLE_AUTO "
                +"   WHERE stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and status = '지급대기'"
                +"   order by regDate;";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public SharedMap<String, Object> getMchtBalance(String mchtId) {
        super.setTable("PG_MCHT_BALANCE");
        super.setColumns("*");
        super.addWhere("mchtId",mchtId ,eq);
        super.setOrderBy("");
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public synchronized String getChargeSettleTrxId() {
        return "CS" + getFunction("FN_NEXTVAL2", "TRN");
    }

    public boolean insertChargeSettle(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }

    public boolean updateTotalAuth(String stlId){
        String q = "UPDATE PG_TOTAL_AUTH"
                + "    SET stlStatus = '정산완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateChargeSettleAuto(String stlId) {
        String q = "UPDATE PG_CHARGE_SETTLE_AUTO"
                + "    SET status = '지급완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public String getFunction(String function, String value) {
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
}
