package com.pgmate.dm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.WalletNotiDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.map.SharedMap;


public class WalletNotiDaemon {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.WalletNotiDaemon.class );

	public WalletNotiDaemon() {
		walletNoti();
	}
	
	public void walletNoti(){
		try {
			WalletNotiDAO webHookDAO = new WalletNotiDAO();
			
			List<SharedMap<String,Object>> notiList = webHookDAO.getNotiList();
			logger.info("Wallet Noti List Count [{}]",notiList.size());
			for(SharedMap<String, Object> sharedMap:notiList){
				logger.info("PAYLOAD [{}]", sharedMap.getString("payLoad"));
				try{Thread.sleep(10);}catch(Exception e){};
				new WalletWebHook(sharedMap).start();
			}
		}catch(Exception e) {
			SmsGw smsGw = new SmsGw();
			
			String msgBody = "월렛 가상계좌거래 노티 오류. 확인요망";
			smsGw.sendMessage("0", "1", msgBody);
            
            logger.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		WalletNotiDaemon d = new WalletNotiDaemon();
	}
}
