package com.pgmate.dm.main;

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.dm.dao.WebHookDAO;
import com.pgmate.lib.util.map.SharedMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WebHookDaemonTest {

    private static Logger logger = LoggerFactory.getLogger(WebHookDaemonTest.class);

    WebHookDAO webHookDAO;
    @Before
    public void init() {
        webHookDAO = new WebHookDAO();
    }

    @Test
    public void payList() {
        List<SharedMap<String,Object>> payList = webHookDAO.getPayList();
        logger.info("payList Count [{}]",payList.size());
        for(SharedMap<String, Object> sharedMap:payList){
            logger.info("rentId: {}", sharedMap.getString("rentId"));
            logger.info("trxId: {}", sharedMap.getString("trxId"));
            if(!sharedMap.getString("rentId").equals("")) {
                logger.info("월세결제");
            } else {
                logger.info("일반결제");
            }
        }
    }
}
