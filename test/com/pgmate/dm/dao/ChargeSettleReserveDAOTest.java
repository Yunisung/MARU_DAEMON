package com.pgmate.dm.dao;

import com.pgmate.lib.util.map.SharedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChargeSettleReserveDAOTest {

    private static Logger logger = LoggerFactory.getLogger(ChargeSettleReserveDAOTest.class);

    ChargeSettleReserveDAO chargeSettleReserveDAO;

    @Before
    public void init() {
        chargeSettleReserveDAO = new ChargeSettleReserveDAO();
        chargeSettleReserveDAO.setDebug(true);
    }

    @Test
    public void chargeFirmTest() {
        ChargeSettleReserveDAO dao = new ChargeSettleReserveDAO();
        dao.updateTrxCapDtlStlComplete("T231220045250");
    }

    @Test
    public void getChareSettleReserveList() {
        List<SharedMap<String, Object>> data = chargeSettleReserveDAO.getChareSettleReserveList();
        logger.debug("건수: {}", data.size());
        Assert.assertTrue(!data.isEmpty());
    }

}
