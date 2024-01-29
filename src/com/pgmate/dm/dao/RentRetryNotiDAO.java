package com.pgmate.dm.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

import java.util.List;

public class RentRetryNotiDAO extends DAO {


    public List<SharedMap<String, Object>> getRiskRetryList() {
        super.setTable("PG_RISK_CHANGE_NOTI");
        super.setColumns("*");
        super.addWhere("retry", 10, le);
        super.addWhere("status", "전송실패");

        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public List<SharedMap<String, Object>> getSettleRetryList() {
        super.setTable("PG_CHARGE_SETTLE_NOTI");
        super.setColumns("*");
        super.addWhere("retry", 10, le);
        super.addWhere("status", "전송실패");

        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public List<SharedMap<String, Object>> getDistStlRetryList() {
        super.setTable("PG_RENT_SETTLE_NOTI");
        super.setColumns("*");
        super.addWhere("retry", 10, le);
        super.addWhere("status", "전송실패");

        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public void updateMemSettleNoti(SharedMap<String, Object> ntsMap) {
        String query = "UPDATE PG_RENT_SETTLE_NOTI SET retry = retry + 1, code='"+ntsMap.getInt("code")+"', status='"+ntsMap.getString("status")+"', resData = '"+ntsMap.getString("resData")+"' WHERE  stlId = '"+ntsMap.getString("stlId")+"'";

        super.initRecord();
    }
}
