package com.pgmate.dm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.dm.dao.WebHookDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;


public class ChargeSettleRetryNoti {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.ChargeSettleRetryNoti.class );

	public ChargeSettleRetryNoti() {
		noti();
	}
	
	public void noti(){
		try {
			ChargeSettlePayOutDAO dao = new ChargeSettlePayOutDAO();
			
			List<SharedMap<String,Object>> notiList = dao.getRetryNotiList();
			if(notiList.size() > 0) {
				logger.info("chargeSettle noti retry Count [{}]",notiList.size());
				for(SharedMap<String, Object> sharedMap:notiList){
					try{Thread.sleep(100);}catch(Exception e){};
					new ChargeSettleHook(sharedMap.getString("hookAddr"),sharedMap,dao,"retry").start();
				}
			}
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		
	}
	
	public static void main(String[] args) {
		ChargeSettleRetryNoti d = new ChargeSettleRetryNoti();
	}

}
