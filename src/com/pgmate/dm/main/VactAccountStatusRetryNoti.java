package com.pgmate.dm.main;

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.dm.dao.VactAccountDAO;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VactAccountStatusRetryNoti {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VactAccountStatusRetryNoti.class );

    public VactAccountStatusRetryNoti() { noti();}

    public void noti() {
        try {
            VactAccountDAO dao = new VactAccountDAO();

            List<SharedMap<String,Object>> notiList = dao.getRetryNotiList();
            if(notiList.size() > 0) {
                logger.info("vactStatus noti retry Count [{}]",notiList.size());
                for(SharedMap<String, Object> sharedMap:notiList){
                    try{Thread.sleep(100);}catch(Exception e){};
                    new VactAccountStatusHook(sharedMap.getString("hookAddr"),sharedMap,"retry").start();
                }
            }
        }catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        VactAccountStatusRetryNoti d = new VactAccountStatusRetryNoti();
    }
}
