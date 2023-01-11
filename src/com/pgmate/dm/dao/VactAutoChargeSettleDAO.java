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

public class VactAutoChargeSettleDAO extends DAO {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public VactAutoChargeSettleDAO() { super.setDebug(false); }

    public static String getFunction(String function,String value){
        String returnVal = "";
        String query = "SELECT "+function+"(?) as val";

        DBManager db 			= null;
        PreparedStatement pstmt = null;
        Connection conn		= null;
        ResultSet rset			= null;

        try {
            db 			= DBFactory.getInstance();
            conn		= db.getConnection();
            pstmt		= conn.prepareStatement(query);
            pstmt.setString(1,value);
            rset		= pstmt.executeQuery();

            while(rset.next()){
                returnVal = rset.getString(1);
            }
            conn.commit();
        }catch(Exception t){

        }finally {
            db.close(conn, pstmt, rset);
        }
        return returnVal;
    }

    public String getSettleId(){
        return "S"+getFunction("FN_NEXTVAL2","SETTLE");
    }

    public List<SharedMap<String,Object>> getAutoChargeSettleList(String stlDay){
        String q = "SELECT A.*, (A.payFee + A.payFeeVat) +(A.rfdFee + A.rfdFeeVat) - A.distFee - A.agencyFee - A.salesFee - A.vanFee AS benefit, B.bankCd,B.bankName,B.account,B.accntHolder,B.taxId FROM( "
                +" SELECT mchtId, MIN(trxDay) AS startDay, MAX(trxDay) AS endDay, stlType, "
                +" SUM(IF(trxType = '입금',amount,0)) AS payAmt, "
                +" SUM(IF(trxType = '입금',stlFee,0)) AS payFee, "
                +" SUM(IF(trxType = '입금',stlFeeVat,0)) AS payFeeVat, "
                +" SUM(IF(trxType ='입금',1,0)) as payCnt, "
                +" SUM(IF(trxType = '취소',amount,0)) AS rfdAmt, "
                +" SUM(IF(trxType = '취소',stlFee,0)) AS rfdFee, "
                +" SUM(IF(trxType = '취소',stlFeeVat,0)) AS rfdFeeVat, "
                +" SUM(IF(trxType ='취소',1,0)) as rfdCnt, "
                +" SUM(stlDistFee)+SUM(stlDistFeeVat) AS distFee, "
                +" SUM(stlAgencyFee)+SUM(stlAgencyFeeVat) AS agencyFee, "
                +" SUM(stlSalesFee)+SUM(stlSalesFeeVat) AS salesFee, "
                +" SUM(vanFee) AS vanFee, "
                +" SUM(stlAmount) AS stlAmount "
                +" FROM VW_VACT_TRX WHERE stlDay = '"+stlDay+"' AND stlId = '' AND stlType LIKE 'B+%' AND settleTarget = 'Y' GROUP BY mchtId) A  "
                +" LEFT JOIN PG_MCHT_TAX B ON A.mchtId = B.mchtId AND B.taxStatus = '사용' ";
        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

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

    public boolean insertSettleAuto(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE_AUTO");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }

    public void setSettleToIdx(String stlId,String stlDay,String mchtId){
        String q = "INSERT INTO PG_VACT_SETTLE_IDX  "
                +"	SELECT '"+stlId+"' ,vactId,trxType"
                +"  FROM VW_VACT_TRX where stlDay ='"+stlDay+"' and mchtId = '"+mchtId+"' AND stlType != 'D+0' AND stlType !='A+1' AND stlType != 'C+0' AND settleTarget = 'Y' ";
        logger.info("set PG_VACT_SETTLE_IDX : {}",super.update(q));
        super.initRecord();
    }

    public void updateTrxCap(String stlId){
        String q = "UPDATE PG_VACT_TRX SET stlId='"+stlId+"' "
                +"	WHERE vactId in (SELECT vactId FROM PG_VACT_SETTLE_IDX WHERE stlId='"+stlId+"' )";
        logger.info("set PG_VACT_TRX : {}",super.update(q));
        super.initRecord();
    }

    public List<SharedMap<String,Object>> getAutoChargePayOutList(String stlDay) {
        String q = "SELECT * "
                +"	  FROM PG_CHARGE_SETTLE_AUTO "
                +"   WHERE stlDay = '" + stlDay + "' and stlType LIKE 'B+%' and status = '지급대기'"
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

    public synchronized static String getVactId(){
        return "V" + getFunction("FN_NEXTVAL2", "VACT");
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

    public boolean updateChargeSettleAuto(String stlId) {
        String q = "UPDATE PG_CHARGE_SETTLE_AUTO"
                + "    SET status = '지급완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }
}
