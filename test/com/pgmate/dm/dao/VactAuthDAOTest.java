package com.pgmate.dm.dao;

import com.pgmate.lib.util.map.SharedMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class VactAuthDAOTest {

    private static Logger logger = LoggerFactory.getLogger(VactAuthDAOTest.class);

    VactAuthDAO vactAuthDAO;

    @Before
    public void init() {
        vactAuthDAO = new VactAuthDAO();
        vactAuthDAO.setDebug(true);
    }

    @Test
    public void 해지출금계좌_조회() {
        List<SharedMap<String, Object>> list = vactAuthDAO.getTerminateAccount(10);
        logger.debug("전체건수 trxId {}", list.size());
        assertTrue(list.size() > 0);
    }
}
