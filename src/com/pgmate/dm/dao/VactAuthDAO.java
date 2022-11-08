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

/**
 * Create By PYS <
 * RealTimePayOutDAO Copy
 * >
 */
public class VactAuthDAO extends DAO {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public VactAuthDAO() { super.setDebug(false); }

    public List<SharedMap<String,Object>> getVactAuthFeeList(String stlDay, String stlType){
        String q = "SELECT a.mchtId, SUM(fee) AS fee, c.bankCd, c.bankName, c.account, c.accntHolder"
                +" FROM PG_VACT_AUTH a, PG_VACT_AUTH_DTL b, PG_MCHT_TAX c "
                +"WHERE a.authId = b.authId and a.mchtId = c.mchtId and b.stlDay = '"+stlDay+"' AND b.stlType = '" + stlType + "' AND b.stlStatus != '정산완료'"
                +"GROUP BY a.mchtId ";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public String getStlId(String mchtId, String stlDay, String stlType) {
        super.setTable("PG_CHARGE_SETTLE_AUTO");
        super.setColumns("stlId");
        super.addWhere("mchtId", mchtId);
        super.addWhere("stlDay", stlDay);
        super.addWhere("stlType", stlType);
        super.addWhere("status", "지급대기");
        super.addWhere("payType", "V");

        RecordSet rset = super.search();
        super.initRecord();
        if (rset.size() != 0) {
            return rset.getRow(0).getString("stlId");
        } else {
            return "";
        }
    }

    public boolean updateAuthFee(String stlId, long fee, long feeVat){
        String q = "UPDATE PG_CHAREGE_SETTLE_AUTO"
                + "    SET authFee = '" + fee +"', authFeeVat = '" + feeVat +"',totalAuthFee = totalAuthFee + '" + (fee + feeVat) + "'"
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateAuthDtl(String stlId){
        String q = "UPDATE PG_VACT_AUTH_DTL"
                + "    SET stlStatus = '정산완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateChargeSettleAuto(String stlId) {
        String q = "UPDATE PG_CHARGE_SETTLE_AUTO"
                + "    SET stlStatus = '지급완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    /**
     * 인증 테이블 정산번호 업데이트
     * @param stlId
     * @param mchtId
     * @param stlDay
     * @param stlType
     * @return
     */
    public boolean updateAuthStlId(String stlId, String mchtId, String stlDay, String stlType){
        String q = "UPDATE PG_VACT_AUTH a, PG_VACT_AUTH_DTL b"
                + "    SET b.stlId = '" + stlId +"'"
                + "	 WHERE a.mchtId = '" + mchtId + "' and b.stlDay = '" + stlDay + "' and b.stlType = '" + stlType + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    /**
     * 가맹점 아이디로 가맹점 가상계좌 관리 테이블(PG_MCHT_MNG_VACT) 테이블 데이터 조회
     * @param mchtId : 가맹점 아이디
     * @return
     */
    public SharedMap<String, Object> getMchtMngVactByMchtId(String mchtId) {
        super.setTable("PG_MCHT_MNG_VACT");
        super.setColumns("*");
        super.addWhere("mchtId", mchtId, eq);
        RecordSet rset = super.search();
        super.initRecord();
        if(rset.size() > 0){
            return rset.getRow(0);
        }else{
            return new SharedMap<String,Object>();
        }
    }

    /**
     * 정산아이디 생성
     * @return
     */
    public synchronized static String getSettleId() {
        return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
    }

    public synchronized static String getVactId(){
        return "V" + getFunction("FN_NETXVAL2", "VACT");
    }

    /**
     * 자동정산 데이터 (PG_SETTLE_AUTO) 테이블 INSERT
     * @param data
     * @return
     */
    public boolean insertSettleAuto(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE_AUTO");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }


    /**
     * 가상계좌 인증 정산 데이터 (PG_CHARGE_SETTLE) 테이블 INSERT
     * @param data
     * @return
     */
    public boolean insertChargeSettle(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }

    /**
     * 충전 자동정산 출금 데이터에서 출금완료하지 않은 대상건들 조회
     * @return
     */
    public List<SharedMap<String,Object>> getVactAuthPayOutList(String stlDay, String stlType){
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

}
