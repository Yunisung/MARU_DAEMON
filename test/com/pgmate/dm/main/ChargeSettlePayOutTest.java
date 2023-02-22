package com.pgmate.dm.main;

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertTrue;

public class ChargeSettlePayOutTest {

    private static Logger logger = LoggerFactory.getLogger(ChargeSettlePayOutTest.class);

    ChargeSettlePayOutDAO dao;

    @Before
    public void init() {
        dao = new ChargeSettlePayOutDAO();
    }

    @Test
    public void 노티생성() {
        String trxId = "CS221110037667";
        SharedMap<String, Object> data = dao.getChargeSettle(trxId);

        String mchtId = data.getString("mchtId");
        SharedMap<String, Object> chargeMngMap = dao.getMchtChargeMng(mchtId);

        String payLoad = setPayLoad(data, "출금완료", "0000", "정상처리");
        data.put("payLoad", payLoad);
        data.put("trxType", "출금");
        new ChargeSettleHook(chargeMngMap.getString("hookAddr"), data, dao, "0").start();
    }

    public String setPayLoad(SharedMap<String, Object> sharedMap, String status, String resultCd, String resultMsg){
        SharedMap<String, String> payLoadMap = new SharedMap<String, String>();

        payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
        payLoadMap.put("trxId",sharedMap.getString("trxId"));
        payLoadMap.put("trxDay",sharedMap.getString("trxDay"));
        payLoadMap.put("trxTime",sharedMap.getString("trxTime"));
        payLoadMap.put("status",status);
        payLoadMap.put("trackId",sharedMap.getString("trackId"));
        payLoadMap.put("resultCd",resultCd);
        payLoadMap.put("resultMsg",resultMsg);
        payLoadMap.put("amount",sharedMap.getString("amount"));
        String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
        return payLoad;
    }

}
