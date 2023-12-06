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
import java.sql.ResultSet;
import java.util.List;

public class ChargeSettleReserveDAO extends DAO {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.ChargeSettleReserveDAO.class );

    public ChargeSettleReserveDAO() {
        super.setDebug(false);
    }

    public List<SharedMap<String, Object>> getChareSettleRealTimeList() {
        String q = "SELECT *, FN_AES_DEC(account) as decAccount"
                +"	FROM PG_CHARGE_SETTLE_FIRM_RESERVE"
                +"  WHERE transferType = '실시간' and "
                +"  status != '완료' and status != '전송' and retry < 3 and rootTrxId = ''"
                +"  order by regDate";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public List<SharedMap<String, Object>> getChareSettleReserveList() {
        String q = "SELECT *, FN_AES_DEC(account) as decAccount"
                +"	FROM PG_CHARGE_SETTLE_FIRM_RESERVE "
                +"  WHERE pubDay = DATE_FORMAT(NOW(), '%Y%m%d') and "
                +"  pubTime < DATE_FORMAT(NOW(), '%H%i%s') and "
                +"  transferType = '예약' and "
                +"  status != '완료' and status != '전송' and retry < 3 and rootTrxId = ''"
                +"  order by regDate";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public List<SharedMap<String, Object>> getChareSettleReserveChildList(String rootTrxId) {
        String q = "SELECT *, FN_AES_DEC(account) as decAccount"
                +"	FROM PG_CHARGE_SETTLE_FIRM_RESERVE "
                +"  WHERE rootTrxId ='" + rootTrxId + "' and "
                +"  status != '완료' and status != '전송' "
                +"  order by regDate";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public SharedMap<String, Object> getMchtChargeMng(String mchtId) {
        super.setTable("PG_MCHT_CHARGE_MNG");
        super.setColumns("*");
        super.addWhere("mchtId",mchtId,eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public boolean updateStatus(String trxId){
        String q = "UPDATE PG_CHARGE_SETTLE_FIRM_RESERVE "
                + "    SET status = '전송', retry=retry+1 "
                + "	 WHERE trxId = '" + trxId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public String getIdx(String trxId){
        super.setTable("PG_FIRM_TRX");
        super.setColumns("idx");
        super.addWhere("filler",trxId,eq);
        super.setLimit(1);

        RecordSet rset = super.search();
        super.initRecord();

        return rset.getRowFirst().getString("idx");
    }

    public String getSeqNo(String trxId){
        String q = "SELECT seqNo"
                +"    FROM PG_FIRM_TRX "
                +"	 WHERE idx = "
                +" 		(SELECT refId "
                +"	  	   FROM PG_CHARGE_SETTLE_FIRM_RESERVE "
                +"   	  WHERE trxId = '" + trxId + "')";

        RecordSet rset = super.query(q);

        super.initRecord();
        return rset.getRowFirst().getString("seqNo");
    }


    public boolean updateRefIdUpdate(String trxId, String refId){
        String q = "UPDATE PG_CHARGE_SETTLE "
                + "    SET refId = '"+refId+"'"
                + "	 WHERE trxId = '" + trxId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateRefIdUpdate2(String trxId, String refId){
        String q = "UPDATE PG_CHARGE_SETTLE_FIRM_RESERVE "
                + "    SET refId = '"+refId+"'"
                + "	 WHERE trxId = '" + trxId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updatePayOutRes(String trxId, long balance, String resCd, String resMsg, String status, String trxDay, String trxTime){
        String q = "UPDATE PG_CHARGE_SETTLE_FIRM_RESERVE "
                + "    SET balance="+balance+ ", resultCd='"+resCd+"', resultMsg='"+resMsg+"', status='"+status+"', trxDay='"+trxDay+"', trxTime='"+trxTime+"'"
                + "	 WHERE trxid = '"+trxId+"'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updatePayOutResChild(String rootTrxId, long balance, String resCd, String resMsg, String status, String trxDay, String trxTime){
        String q = "UPDATE PG_CHARGE_SETTLE_FIRM_RESERVE "
                + "    SET balance="+balance+ ", resultCd='"+resCd+"', resultMsg='"+resMsg+"', status='"+status+"', trxDay='"+trxDay+"', trxTime='"+trxTime+"'"
                + "	 WHERE rootTrxId = '"+rootTrxId+"'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public String getRetry(String trxId){
        super.setTable("PG_CHARGE_SETTLE_FIRM_RESERVE");
        super.setColumns("retry");
        super.addWhere("trxId",trxId,eq);
        super.setLimit(1);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst().getString("retry");
    }

    public int getChargeErrCount(String trxId) {
        String query = "SELECT COUNT(*) as cnt FROM PG_CHARGE_SETTLE_ERR WHERE trxId='" + trxId + "'";
        RecordSet rset = super.query(query);
        super.initRecord();
        return rset.getRowFirst().getInt("cnt");
    }

    public SharedMap<String, Object> getChargeSettle(String trxId) {
        super.setTable("PG_CHARGE_SETTLE");
        super.setColumns("*");
        super.addWhere("trxId",trxId,eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

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

        boolean updated =  super.update(q);

        super.initRecord();
        return updated;
    }

    public void deleteChargeSettle(String trxId) {
        super.setTable("PG_CHARGE_SETTLE");
        super.addWhere("trxId",trxId,eq);
        super.delete();
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

    public synchronized static String getSettleId() {
        return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
    }

    public boolean updateTrxCapDtl(String trxId) {
        String q = "UPDATE VW_TRX_CAP "
                + "    SET stlStatus = '정산완료' , payOutDay = '" + CommonUtil.getCurrentDate("yyyyMMdd") + "' "
                + "  WHERE trxId = '" +trxId + "'";

        boolean updated =  super.update(q);

        super.initRecord();
        return updated;
    }

    /*public boolean updateStlCompleted(String trxId, String stlId) {
        String q = "UPDATE PG_CHARGE_SETTLE_FIRM_RESERVE "
                + "    SET stlStatus = '지급완료', stlId = '" +stlId+ "' "
                + "  WHERE trxId = '" +trxId + "'";

        boolean updated =  super.update(q);

        super.initRecord();
        return updated;
    }*/

    public SharedMap<String, Object> getMchtRent(String mchtId) {
        super.setTable("PG_MCHT_RENT");
        super.setColumns("*");
        super.addWhere("mchtId",mchtId,eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public SharedMap<String, Object> getTrxCapById(String trxId) {
        String q = "SELECT *"
                +"    FROM VW_TRX_CAP "
                +"	 WHERE trxId = '"+trxId+"'";

        RecordSet rset = super.query(q);

        super.initRecord();
        return rset.getRowFirst();
    }

    /**
     * 충전정산 잔액조회
     * @param mchtId
     * @return
     */
    public SharedMap<String, Object> getMchtBalance(String mchtId){
        super.setTable("PG_MCHT_BALANCE");
        super.setColumns("*");
        super.addWhere("mchtId",mchtId,eq);
        super.setOrderBy("");
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public boolean insertChargeSettle(SharedMap<String, Object> trxMap) {
        super.setTable("PG_CHARGE_SETTLE");
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
        super.setRecord("regId", trxMap.getString("regId"));
        super.setRecord("regDay", trxMap.getString("regDay"));

        boolean result = super.insert();
        super.initRecord();
        logger.info("insert PG_CHARGE_SETTLE [{}]", result);
        return result;

    }

    public synchronized String getChargeSettleTrxId() {
        return "CS" + getFunction("FN_NEXTVAL2", "TRN");
    }
}
