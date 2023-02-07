package com.pgmate.dm.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TotalAuthDAO extends DAO {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public TotalAuthDAO() { super.setDebug(false);}

    public List<SharedMap<String, Object>> getTotalAuthFeeList(String stlDay) {
        String q = "SELECT mchtId, stlType, SUM(authFee) AS authFee, SUM(authFeeVat) AS authFeeVat "
                +" FROM PG_TOTAL_AUTH "
                +" WHERE  stlDay = '"+stlDay+"' AND stlStatus != '정산완료' "
                +" GROUP BY mchtId, stlType";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public synchronized static String getSettleId() {
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
}
