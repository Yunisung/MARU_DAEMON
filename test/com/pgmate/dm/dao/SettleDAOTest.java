package com.pgmate.dm.dao;

import com.pgmate.lib.util.map.SharedMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SettleDAOTest {

    private static Logger logger = LoggerFactory.getLogger(SettleDAOTest.class);

    @Test
    public void settleList() {
        SettleDistDAO settleDistDAO = new SettleDistDAO();
        List<SharedMap<String,Object>> list = settleDistDAO.getSettleList("20240202");
        for(SharedMap<String,Object> data : list) {
            logger.info("금액: {}, 아이디: {}", data.getLong("payAmt"), data.getString("memberId"));
        }
    }
}
