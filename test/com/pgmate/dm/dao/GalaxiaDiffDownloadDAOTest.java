package com.pgmate.dm.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalaxiaDiffDownloadDAOTest {

    private static Logger logger = LoggerFactory.getLogger(GalaxiaDiffDownloadDAOTest.class);

    private KsnetDiffDownloadDAO dao;

    @Before
    public void init() {
        dao = new KsnetDiffDownloadDAO();
        dao.setDebug(true);
    }

    @Test
    public void 차액정산_다운로드_테스트() {
        String nowDate = "20220708";
        int result = dao.updateTrxCap(nowDate);
        Assert.assertTrue(result > 0);
    }

}
