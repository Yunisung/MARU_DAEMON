package com.pgmate.dm.dao;

import com.pgmate.dm.main.ChargeSettlePayOut;
import com.pgmate.lib.util.map.SharedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class ChargeSettlePayOutDAOTest {

    private static Logger logger = LoggerFactory.getLogger(ChargeSettlePayOutDAOTest.class);

    ChargeSettlePayOutDAO chargeSettlePayOutDAO;

    @Before
    public void init() {
        chargeSettlePayOutDAO = new ChargeSettlePayOutDAO();
        chargeSettlePayOutDAO.setDebug(true);
    }

    @Test
    public void 충전정산에러건수확인() {
        String trxId = "CS230426042083";
        int cnt = chargeSettlePayOutDAO.getChargeErrCount(trxId);
        logger.debug("건수: {}", cnt);
        Assert.assertTrue(cnt == 1);
    }



}
