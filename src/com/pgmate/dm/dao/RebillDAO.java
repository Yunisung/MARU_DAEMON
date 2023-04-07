package com.pgmate.dm.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RebillDAO extends DAO {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public RebillDAO() { super.setDebug(false); }

    public List<SharedMap<String,Object>> getRebillList(String payDay){
        String q = "SELECT a.rebillId, b.payKey "
                +" FROM PG_REBILL_REG a, PG_MCHT_TMN b"
                +" WHERE a.nextPayDay = '"+ payDay+ "' AND a.tmnId = b.tmnId AND a.status IN ('승인', '실패')"
                +" ORDER BY a.regDate ";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }
}
