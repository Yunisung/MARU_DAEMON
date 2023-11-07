package com.pgmate.dm.main;

import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.WebHookDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;


public class WebHookDaemon {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.WebHookDaemon.class );

	public WebHookDaemon() {
		payWebHook();
	}
	
	public void payWebHook(){
		try {
			WebHookDAO webHookDAO = new WebHookDAO();
			List<SharedMap<String,Object>> rfdList = webHookDAO.getRfdList();
			List<SharedMap<String,Object>> payList = webHookDAO.getPayList();
			List<SharedMap<String,Object>> rfdRetryList = webHookDAO.getRfdRetryList();
			List<SharedMap<String,Object>> payRetryList = webHookDAO.getPayRetryList();
			
			
			logger.info("payList Count [{}]",payList.size());
			for(SharedMap<String, Object> sharedMap:payList){
				String payLoad = getPayLoad(sharedMap, webHookDAO, "PAY");
				sharedMap.put("payLoad", payLoad);

				try{Thread.sleep(100);}catch(Exception e){};
				new PGWebHook("pay",sharedMap,webHookDAO,"").start();
			}
			
			try{Thread.sleep(500);}catch(Exception e){};
			logger.info("payList retry Count [{}]",payRetryList.size());
			for(SharedMap<String, Object> sharedMap:payRetryList){
				String payLoad = getPayLoad(sharedMap, webHookDAO, "PAY");
				sharedMap.put("payLoad", payLoad);

				try{Thread.sleep(200);}catch(Exception e){};
				new PGWebHook("pay",sharedMap,webHookDAO,"retry").start();
			}
			
			try{Thread.sleep(500);}catch(Exception e){};
			logger.info("rfdList Count [{}]",rfdList.size());
			for(SharedMap<String, Object> sharedMap:rfdList){
				String payLoad = getPayLoad(sharedMap, webHookDAO, "REFUND");
				sharedMap.put("payLoad", payLoad);

				try{Thread.sleep(100);}catch(Exception e){};
				new PGWebHook("refund",sharedMap,webHookDAO,"").start();
			}
			
			try{Thread.sleep(500);}catch(Exception e){};
			logger.info("rfdList retry Count [{}]",rfdRetryList.size());
			for(SharedMap<String, Object> sharedMap:rfdRetryList){
				String payLoad = getPayLoad(sharedMap, webHookDAO, "REFUND");
				sharedMap.put("payLoad", payLoad);

				try{Thread.sleep(100);}catch(Exception e){};
				new PGWebHook("refund",sharedMap,webHookDAO,"retry").start();
			}
		}catch(Exception e) {
			SmsGw smsGw = new SmsGw();
			
			String msgBody = "노티수신 수신 가맹점 노티 재전송 오류. 확인요망";
			smsGw.sendMessage("0", "1", msgBody);
            
            logger.error(e.getMessage(), e);
		}
		
	}

	public String getPayLoad(SharedMap<String,Object> sharedMap, WebHookDAO webHookDAO, String trxType) {
		String payLoad = "";
		if(!sharedMap.getString("rentId").equals("")) {
			SharedMap<String, Object> ioMap = webHookDAO.getTrxIo3d(sharedMap.getString("trxId"));
			if(ioMap != null) {
				String jsonStr = ioMap.getString("reqJson");
				SharedMap<String,Object> widget = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
				sharedMap.put("udf1", widget.getString("udf1"));

				payLoad = setRentPayLoad(sharedMap, trxType);
			}
		} else {
			payLoad =  setPayLoad(sharedMap,trxType);
		}

		return payLoad;
	}

	public String setPayLoad(SharedMap<String, Object> sharedMap, String trxType){
		SharedMap<String, String> payLoadMap = new SharedMap<String, String>();
		
		payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
		payLoadMap.put("trxId",sharedMap.getString("trxId"));
//		payLoadMap.put("van",sharedMap.getString("van"));
		payLoadMap.put("tmnId",sharedMap.getString("tmnId"));
		payLoadMap.put("trxDate",sharedMap.getString("trxDate"));
		payLoadMap.put("trxType",trxType);
		payLoadMap.put("rootTrxId", CommonUtil.nToB(sharedMap.getString("rootTrxId")));
		payLoadMap.put("trackId",sharedMap.getString("trackId"));
//		payLoadMap.put("vanTrxId",sharedMap.getString("vanTrxId"));
		payLoadMap.put("authCd",sharedMap.getString("authCd"));
		payLoadMap.put("cardType",CommonUtil.nToB(sharedMap.getString("cardType")));
		payLoadMap.put("issuer",sharedMap.getString("issuer"));
		payLoadMap.put("acquirer",sharedMap.getString("acquirer"));
		payLoadMap.put("bin",sharedMap.getString("bin"));
		payLoadMap.put("last4",sharedMap.getString("last4"));
		payLoadMap.put("installment",CommonUtil.nToB(sharedMap.getString("installment")));
		payLoadMap.put("amount",sharedMap.getString("amount"));
		
		String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
		return payLoad;
	}

	public String setRentPayLoad(SharedMap<String, Object> sharedMap, String trxType){
		SharedMap<String, String> payLoadMap = new SharedMap<String, String>();

		payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
		payLoadMap.put("trxId",sharedMap.getString("trxId"));
//		payLoadMap.put("van",sharedMap.getString("van"));
		payLoadMap.put("tmnId",sharedMap.getString("tmnId"));
		payLoadMap.put("trxDate",sharedMap.getString("trxDate"));
		payLoadMap.put("trxType",trxType);
		payLoadMap.put("rootTrxId", CommonUtil.nToB(sharedMap.getString("rootTrxId")));
		payLoadMap.put("trackId",sharedMap.getString("trackId"));
//		payLoadMap.put("vanTrxId",sharedMap.getString("vanTrxId"));
		payLoadMap.put("authCd",sharedMap.getString("authCd"));
		payLoadMap.put("cardType",CommonUtil.nToB(sharedMap.getString("cardType")));
		payLoadMap.put("issuer",sharedMap.getString("issuer"));
		payLoadMap.put("acquirer",sharedMap.getString("acquirer"));
		payLoadMap.put("bin",sharedMap.getString("bin"));
		payLoadMap.put("last4",sharedMap.getString("last4"));
		payLoadMap.put("installment",CommonUtil.nToB(sharedMap.getString("installment")));
		payLoadMap.put("amount",sharedMap.getString("amount"));
		payLoadMap.put("udf1",sharedMap.getString("udf1"));

		String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
		return payLoad;
	}
	
	public static void main(String[] args) {
		WebHookDaemon d = new WebHookDaemon();
	}

}
