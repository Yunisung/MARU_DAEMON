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
    WebHookDaemon webHookDaemon;
    @Before
    public void init() {
        webHookDAO = new WebHookDAO();
        webHookDaemon = new WebHookDaemon();
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

    @Test
    public void getPayLoad() {
        SharedMap<String, Object> sharedMap = new SharedMap<>();
        sharedMap.put("trxId", "T240129049299");
        sharedMap.put("mchtId", "bktest001");
        sharedMap.put("tmnId", "TMN001028");
        sharedMap.put("trackId", "AAA-20240126180234421");
        sharedMap.put("payerName", "오세창");
        sharedMap.put("payerEmail", "gobongju@bkwinners.com");
        sharedMap.put("amount", "5000");

        String payLoad = webHookDaemon.getPayLoad(sharedMap, webHookDAO, "PAY");
        logger.info("payload: {}", payLoad);
    }
}
