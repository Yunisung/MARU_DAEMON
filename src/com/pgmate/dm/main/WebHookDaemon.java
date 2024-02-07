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
		SharedMap<String, Object> ioMap;
		// 월세앱 결제건 일 때
		if(!sharedMap.getString("rentId").equals("")) {
			ioMap = webHookDAO.getTrxIo3d(sharedMap.getString("trxId"));
			// 월세앱 일반결제
			if(ioMap != null) {
				String jsonStr = ioMap.getString("reqJson");
				SharedMap<String,Object> widget = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
				if(widget != null) {
					sharedMap.put("udf1", widget.getString("udf1"));
				}

				payLoad = createPayLoad(sharedMap, trxType, true);
			} else {
				// 월세앱 정기결제 && 취소
				String rebillTrackId = webHookDAO.getRebillTrackId(sharedMap.getString("mchtId"));
				sharedMap.put("rebillTrackId", rebillTrackId);
				payLoad =  createPayLoad(sharedMap, trxType, false);
			}
		// 일반 결제
		} else {
			// 1. PG_TRX_REQ 에서 가져올 것 - (3DTR: IO_3D), (ELSE: IO)
			String trx3DType = webHookDAO.getTrxType(sharedMap.getString("trxId"));
			String jsonStr = "";
			if(trx3DType.equals("3DTR")) {
				ioMap = webHookDAO.getTrxIo3d(sharedMap.getString("trxId"));
				if(ioMap != null) jsonStr = ioMap.getString("reqJson");
			} else {
				ioMap = webHookDAO.getTrxIo(sharedMap.getString("trxId"));
				if(ioMap != null) jsonStr = ioMap.getString("regData");
			}
			// 2. ioMap Null 체크후 jsonStr 가져오기 -> udf1, udf2 가져오기
			if(ioMap != null) {
				SharedMap<String,Object> widget = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
				if(widget != null) {
					sharedMap.put("udf1", widget.getString("udf1"));
					sharedMap.put("udf2", widget.getString("udf2"));
				}
			}
			// 3. setPayLoad에서 udf1, udf2 추가
			String rebillTrackId = webHookDAO.getRebillTrackId(sharedMap.getString("mchtId"));
			sharedMap.put("rebillTrackId", rebillTrackId);
			payLoad =  createPayLoad(sharedMap, trxType, false);
		}

		return payLoad;
	}

	public String createPayLoad(SharedMap<String, Object> sharedMap, String trxType, boolean isRentApp){
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
		if(!isRentApp) {
			payLoadMap.put("udf2", sharedMap.getString("udf2"));
			payLoadMap.put("rebillTrackId", sharedMap.getString("rebillTrackId"));
		}

		String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
		return payLoad;
	}

	public static void main(String[] args) {
		WebHookDaemon d = new WebHookDaemon();
	}

}
