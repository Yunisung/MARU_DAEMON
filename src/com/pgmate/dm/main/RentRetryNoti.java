package com.pgmate.dm.main;

import com.pgmate.dm.dao.RentRetryNotiDAO;
import com.pgmate.dm.dao.WebHookDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RentRetryNoti {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.RentRetryNoti.class );
    private SmsGw smsGw = null;
    private String msgBody = "";

    public RentRetryNoti() {
        logger.info("===== RENT RETRY NOTI START =====");

        RentRetryNotiDAO dao = new RentRetryNotiDAO();

        List<SharedMap<String, Object>> riskRetryList = dao.getRiskRetryList();
        List<SharedMap<String, Object>> settleRetryList = dao.getSettleRetryList();
        List<SharedMap<String, Object>> distStlRetryList = dao.getDistStlRetryList();

        if(riskRetryList.size() > 0) {
            logger.info("riskRetryList Count [{}]", riskRetryList.size());
            for (SharedMap<String, Object> map : riskRetryList) {
                try{Thread.sleep(100);}catch(Exception e){};
                new PGWebHook("pay",map,new WebHookDAO(),"retry").start();
            }
        }

        if(settleRetryList.size() > 0) {
            logger.info("settleRetryList Count [{}]", settleRetryList.size());
            for (SharedMap<String, Object> map : settleRetryList) {
                try{Thread.sleep(100);}catch(Exception e){};
                String hookAddr = map.getString("hookAddr");
                new ChargeSettleHook(hookAddr, map, "retry");
            }
        }

        if(distStlRetryList.size() > 0) {
            logger.info("distStlRetryList Count [{}]", distStlRetryList.size());
            for (SharedMap<String, Object> map : distStlRetryList) {
                try{Thread.sleep(100);}catch(Exception e){};
                String hookAddr = map.getString("hookAddr");
                new ChargeSettleHook(hookAddr, map, "retry");
            }
        }
    }

    public static void main(String[] args) {
        new RentRetryNoti();
    }
}
