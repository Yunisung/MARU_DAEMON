package com.pgmate.dm.main;

import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.RealTimePayOutDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class DailySettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.DailySettle.class );
	private SmsGw smsGw = null;
	
	private String msgBody = "";
	private String hour = "";
	private String stlDay = "";
	private String stlType = "";
	private String mchtId = "";
	private String stlId = "";
	private String startDay = "";
	private String endDay = "";
	private String type = "";
	
	private double stlRate = 0;
	
	private long payAmt = 0;
	private long payFee = 0;
	private long payVat = 0;
	private long payCnt = 0;
	
	private long rfdAmt = 0;
	private long rfdFee = 0;
	private long rfdVat = 0;
	private long rfdCnt = 0;
	
	private long payOutFee = 0;
	private long payOutFeeVat = 0;
	private long stlAmount = 0;
	
	private SharedMap<String,Object> settleData = new SharedMap<String, Object>();
	private SharedMap<String,Object> mchtMngMap	= new SharedMap<String, Object>();
	private SharedMap<String,Object> mchtTaxMap	= new SharedMap<String, Object>();
	private SharedMap<String,Object> mchtMngVactMap	= new SharedMap<String, Object>();
	
	HashMap<String,String> idMap = new HashMap<String,String>();
	
	private int firmPort = 0;
	 
	public static void main(String[] args){
		new DailySettle(args);
//		String[] myArgs = {"A+0", "20231122"};
//		new DailySettle(myArgs);
	}
	
	public DailySettle(String[] args) {
		logger.info("==================================================");
		logger.info("DailySettle Strart");
		smsGw = new SmsGw();
		
		configSetting();
		
		stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
		hour = CommonUtil.getCurrentDate("HH");
		
		stlType = "A+0";
		
		dailySettle(args);
		dailyVactSettle(args);
		
		stlType = "A+2";
		
		dailySettle(args);
		dailyVactSettle(args);
		
		logger.info("DailySettle End");
		logger.info("==================================================");
	}
	
	public void dailySettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		String typeText = "당일정산(영업일)";
		mchtId = "";
		
		try {
			type = "C";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			if("A+2".equals(stlType)) {
				typeText = "당일정산(365)";
			}
			
			logger.info(typeText + " stlDay : " + stlDay + ", stlType : " + stlType + ", hour : " + hour);
			
			//매입원장에서 자동정산 정산예정일자의 정산데이터 조회
			List<SharedMap<String,Object>> getDailySettleList = dao.getDailySettleList(hour, stlDay, stlType);
			
			logger.info(typeText + " 대상거래 건수 : {}", getDailySettleList.size());
			
			if(getDailySettleList.size() > 0) {
				logger.info("==================================================");
				logger.info(typeText + " PG_SETTLE_AUTO INSERT START");
				logger.info("==================================================");
						
				for(SharedMap<String,Object> data : getDailySettleList){
					msgBody = "";
					
					if(!mchtId.equals(data.getString("mchtId"))) {
						if(!"".equals(mchtId)) {
							//가맹점이 바뀌면 일단 insert
							insertAutoSettle(type);
							logger.info("==================================================");
						}

						stlId = dao.getSettleId();
						
						startDay = data.getString("trxDay");
						endDay = data.getString("trxDay");
						stlRate = data.getDouble("stlRate");
						mchtId = data.getString("mchtId");
						
						mchtMngMap	= dao.getMchtMngByMchtId(mchtId);
						mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);

						payAmt = data.getLong("payAmt");
						payFee = data.getLong("payFee");
						payVat = data.getLong("payVat");
						payCnt = data.getLong("payCnt");
						rfdAmt = data.getLong("rfdAmt");
						rfdFee = data.getLong("rfdFee");
						rfdVat = data.getLong("rfdVat");
						rfdCnt = data.getLong("rfdCnt");
						
						payOutFee = mchtMngMap.getLong("payOutFee");
						payOutFeeVat = calcVat(payOutFee);
						
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}else {
						if(1000000000 < stlAmount + (data.getLong("payAmt") - data.getLong("payFee") - data.getLong("payVat"))) {
							//10억이 넘으면 일단 insert
							insertAutoSettle(type);
							
							stlId = dao.getSettleId();
							logger.info("==================================================");
							
							startDay = data.getString("trxDay");
							endDay = data.getString("trxDay");
							stlRate = data.getDouble("stlRate");
							mchtId = data.getString("mchtId");
							
							mchtMngMap	= dao.getMchtMngByMchtId(mchtId);
							mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);
							
							payAmt = data.getLong("payAmt");
							payFee = data.getLong("payFee");
							payVat = data.getLong("payVat");
							payCnt = data.getLong("payCnt");
							rfdAmt = data.getLong("rfdAmt");
							rfdFee = data.getLong("rfdFee");
							rfdVat = data.getLong("rfdVat");
							rfdCnt = data.getLong("rfdCnt");
							
							payOutFee = mchtMngMap.getLong("payOutFee");
							payOutFeeVat = calcVat(payOutFee);
						}else {
							if(!endDay.equals(data.getString("trxDay"))) {
								endDay = data.getString("trxDay");
							}
							
							if(stlRate < data.getDouble("stlRate")) {
								stlRate = data.getDouble("stlRate");
							}

							payAmt += data.getLong("payAmt");
							payFee += data.getLong("payFee");
							payVat += data.getLong("payVat");
							payCnt += data.getLong("payCnt");
							rfdAmt += data.getLong("rfdAmt");
							rfdFee += data.getLong("rfdFee");
							rfdVat += data.getLong("rfdVat");
							rfdCnt += data.getLong("rfdCnt");
						}	
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}

					idMap.put(data.getString("capId"), stlId);
				}
				insertAutoSettle(type);
				
				logger.info("==================================================");
				logger.info(typeText + " PG_SETTLE_AUTO INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = typeText + " 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	public void dailyVactSettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		String typeText = "당일정산(영업일)";
		mchtId = "";
		
		try {		
			type = "V";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			if("A+2".equals(stlType)) {
				typeText = "당일정산(365)";
			}
			
			logger.info(typeText +" 가상계좌 stlDay : " + stlDay + ", stlType : " + stlType + ", hour : " + hour);
			
			//가상계좌 거래내역 정산예정일자의 자동정산 정산데이터 조회
			List<SharedMap<String,Object>> getDailyVactSettleList = dao.getDailyVactSettleList(hour, stlDay, stlType);
			logger.info("getAutoVactSettleList COUNT : {}", getDailyVactSettleList.size());
			
			if(getDailyVactSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info(typeText + " 가상계좌 PG_SETTLE_AUTO INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getDailyVactSettleList){
					msgBody = "";
					
					if(!mchtId.equals(data.getString("mchtId"))) {
						if(!"".equals(mchtId)) {
							//가맹점이 바뀌면 일단 insert
							insertAutoSettle(type);
							logger.info("==================================================");
						}
						mchtMngVactMap	= dao.getMchtMngVactByMchtId(data.getString("mchtId"));
						
						stlId = dao.getSettleId();
						
						startDay = data.getString("trxDay");
						endDay = data.getString("trxDay");
						stlRate = mchtMngVactMap.getDouble("rate");;
						mchtId = data.getString("mchtId");
						
						mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);

						payAmt = data.getLong("payAmt");
						payFee = data.getLong("payFee");
						payVat = data.getLong("payVat");
						payCnt = data.getLong("payCnt");
						rfdAmt = data.getLong("rfdAmt");
						rfdFee = data.getLong("rfdFee");
						rfdVat = data.getLong("rfdVat");
						rfdCnt = data.getLong("rfdCnt");
						
						payOutFee = mchtMngVactMap.getLong("payOutFee");
						payOutFeeVat = calcVat(payOutFee);
						
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}else {
						if(1000000000 < stlAmount + (data.getLong("payAmt") - data.getLong("payFee") - data.getLong("payVat"))) {
							//10억이 넘으면 일단 insert
							insertAutoSettle(type);
							
							stlId = dao.getSettleId();
							logger.info("==================================================");
							
							startDay = data.getString("trxDay");
							endDay = data.getString("trxDay");
							stlRate = mchtMngVactMap.getDouble("rate");;
							mchtId = data.getString("mchtId");
							
							mchtTaxMap	= dao.getMchtTaxByMchtId(mchtId);
							
							payAmt = data.getLong("payAmt");
							payFee = data.getLong("payFee");
							payVat = data.getLong("payVat");
							payCnt = data.getLong("payCnt");
							rfdAmt = data.getLong("rfdAmt");
							rfdFee = data.getLong("rfdFee");
							rfdVat = data.getLong("rfdVat");
							rfdCnt = data.getLong("rfdCnt");
							
							payOutFee = mchtMngVactMap.getLong("payOutFee");
							payOutFeeVat = calcVat(payOutFee);
						}else {
							if(!endDay.equals(data.getString("trxDay"))) {
								endDay = data.getString("trxDay");
							}
							
							if(stlRate < data.getDouble("stlRate")) {
								stlRate = data.getDouble("stlRate");
							}

							payAmt += data.getLong("payAmt");
							payFee += data.getLong("payFee");
							payVat += data.getLong("payVat");
							payCnt += data.getLong("payCnt");
							rfdAmt += data.getLong("rfdAmt");
							rfdFee += data.getLong("rfdFee");
							rfdVat += data.getLong("rfdVat");
							rfdCnt += data.getLong("rfdCnt");
						}	
						stlAmount = (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee);
					}

					idMap.put(data.getString("capId"), stlId);
				}
				insertAutoSettle(type);
				
				logger.info("==================================================");
				logger.info(typeText + " 가상계좌 PG_SETTLE_AUTO INSERT END");
				logger.info("==================================================");
			}
			
			//가상계좌 거래내역 정산예정일자의 자동정산 정산 인증 수수료 데이터 조회
			List<SharedMap<String,Object>> getAutoVactSettleOrgFeeList = dao.getAutoVactSettleOrgFeeList(stlDay, stlType);
			logger.info("getAutoVactSettleOrgFeeList COUNT : {}", getAutoVactSettleOrgFeeList.size());
			
			if(getAutoVactSettleOrgFeeList.size() > 0) {
				logger.info("==================================================");
				logger.info("당일정산 인증수수료 처리 시작");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getAutoVactSettleOrgFeeList){
					msgBody = "";

					String stlId = dao.getStlId(data.getString("mchtId"), stlDay, stlType);
					
					if(!"".equals(stlId)) {
						dao.updateAuthFee(stlId, data.getLong("fee"), calcVat(data.getLong("fee")));
						dao.updateAuthStlId(stlId, data.getString("mchtId"), stlDay, stlType);
						
						logger.info("당일정산 인증 수수료 UPDATE");
						logger.info("stlId  : {}",stlId);
						logger.info("mchtId : {}",data.getString("mchtId"));
						logger.info("orgFee : {}",data.getString("fee"));
						logger.info("==================================================");
					}else {
						SharedMap<String,Object> mchtMngVactMap	= dao.getMchtMngVactByMchtId(data.getString("mchtId"));

						SharedMap<String,Object> settleData = new SharedMap<String, Object>();
						
						stlId = dao.getSettleId();
						settleData.put("stlId", stlId);
						settleData.put("mchtId", data.getString("mchtId"));
						settleData.put("status", "지급대기");
						settleData.put("stlDay", stlDay);
						settleData.put("payType", "V");
						settleData.put("startDay", stlDay);
						settleData.put("endDay", stlDay);
						
						settleData.put("payAmt", 0);
						settleData.put("payFee", 0);
						settleData.put("payVat", 0);
						settleData.put("payCnt", 0);
						
						settleData.put("rfdAmt", 0);
						settleData.put("rfdFee", 0);
						settleData.put("rfdVat", 0);
						settleData.put("rfdCnt", 0);
						
						settleData.put("payOutFee", 0);
						settleData.put("payOutFeeVat", 0);
						settleData.put("bankFee", 0);
						settleData.put("authFee", data.getLong("fee"));
						settleData.put("authFeeVat", calcVat(data.getLong("fee")));
						settleData.put("payOutAmount", (data.getLong("fee") + calcVat(data.getLong("fee"))) * -1);
						
						settleData.put("bankCd", data.getString("bankCd"));
						settleData.put("bankName", data.getString("bankName"));
						settleData.put("account", dao.getAESEnc(data.getString("account")).replace("-", "").trim());
						settleData.put("accntHolder", dao.getAESEnc(data.getString("accntHolder")));
						settleData.put("stlRate", mchtMngVactMap.getDouble("rate"));
						
						settleData.put("stlType", stlType);
						settleData.put("regId", "SYSTEM");
						
						logger.info("당일정산 인증 수수료 INSERT");
						logger.info("stlId  	  : {}",stlId);
						logger.info("mchtId  	  : {}",data.getString("mchtId"));
						logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
						
						if(dao.insertSettleAuto(settleData)){
							dao.updateAuthStlId(stlId, data.getString("mchtId"), stlDay, stlType);
						}else {
							msgBody = "PG_SETTLE_AUTO VACT INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
						}
						
						if(!"".equals(msgBody)) {
							logger.info(msgBody);
							
							smsGw.sendMessage("0", "4", msgBody);
						}
						logger.info("==================================================");
					}
				}
				logger.info("당일정산 인증수수료 처리 종료");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = typeText + "가상계좌 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	private void insertAutoSettle(String type) {
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		settleData = new SharedMap<String, Object>();
		SharedMap<String,Object> mchtMap	= dao.getMcht(mchtId);
		SharedMap<String,Object> agencyMngMap	= dao.getAgencyMngById(mchtMap.getString("agencyId"));
		SharedMap<String,Object> distMngMap		= dao.getDistMngById(mchtMap.getString("distId"));
		SharedMap<String,Object> salesMngMap	= dao.getSalesMngById(mchtMap.getString("salesId"));
//		SharedMap<String,Object> agencyMngMap	= dao.getAgencyMngByNum(mchtMngMap.getInt("agencyNum"));
//		SharedMap<String,Object> distMngMap		= dao.getDistMngByNum(mchtMngMap.getInt("distNum"));
//		SharedMap<String,Object> salesMngMap	= dao.getSalesMngByNum(mchtMngMap.getInt("salesNum"));
		SharedMap<String,Object> trxPayOutData = new SharedMap<String, Object>(); // 출금수수료 정산관련 거래 데이터
		 
		settleData.put("stlId", stlId);
		settleData.put("mchtId", mchtId);
		
		trxPayOutData.put("trxId", stlId);
		trxPayOutData.put("mchtId", mchtId);
		trxPayOutData.put("trxType", "당일");
		trxPayOutData.put("amount", stlAmount);
		
		//if("apiwin".equals(mchtId)) {
		//	settleData.put("status", "지급보류");
		//}else {
		//	settleData.put("status", "지급대기");
		//}
		
		settleData.put("status", "지급대기");
		settleData.put("stlDay", stlDay);
		settleData.put("payType", type);
		settleData.put("startDay", startDay);
		settleData.put("endDay", endDay);
		
		settleData.put("payAmt", payAmt);
		settleData.put("payFee", payFee);
		settleData.put("payVat", payVat);
		settleData.put("payCnt", payCnt);
		
		settleData.put("rfdAmt", rfdAmt);
		settleData.put("rfdFee", rfdFee);
		settleData.put("rfdVat", rfdVat);
		settleData.put("rfdCnt", rfdCnt);
		
		if(type.equals("C")) {
			if(agencyMngMap.size() >0 ){
				//trxPayOutData.put("agencyNum"	, mchtMngMap.getLong("agencyNum"));
				trxPayOutData.put("agencyId", agencyMngMap.getString("agencyId"));
				trxPayOutData.put("stlAgencyType", agencyMngMap.getString("settleType"));
				trxPayOutData.put("stlAgencyId"	, "");
			}
			if(distMngMap.size() >0 ){
				//trxPayOutData.put("distNum"	, mchtMngMap.getLong("distNum"));
				trxPayOutData.put("distId", distMngMap.getString("distId"));
				trxPayOutData.put("stlDistType", distMngMap.getString("settleType"));
				trxPayOutData.put("stlDistId"	, "");
			}
			if(salesMngMap.size() >0 ){
				//trxPayOutData.put("salesNum"	, mchtMngMap.getLong("salesNum"));
				trxPayOutData.put("salesId", salesMngMap.getString("salesId"));
				trxPayOutData.put("stlSalesType", salesMngMap.getString("settleType"));
				trxPayOutData.put("stlSalesId"	, "");
			}
			// 출금 수수료 대납자 = 가맹점
			if(mchtMngMap.getString("payOutType").equals("가맹점")) {
				settleData.put("payOutFee", payOutFee);
				settleData.put("payOutFeeVat", payOutFeeVat);
				
				trxPayOutData.put("payOutFee", mchtMngMap.getLong("payOutFee"));
				trxPayOutData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
				trxPayOutData.put("payOutType", "가맹점");
				trxPayOutData.put("payOutId", mchtId);
				
				// 출금 수수료 수익 분배 사용
				if(mchtMngMap.getString("payInStatus").equals("사용")) {
					trxPayOutData.put("distPayInFee", mchtMngMap.getLong("distPayInFee"));
					trxPayOutData.put("distPayInFeeVat", calcVat(mchtMngMap.getLong("distPayInFee")));
					//trxPayOutData.put("agencyNum", mchtMngMap.getInt("agencyNum"));
					trxPayOutData.put("agencyPayInFee", mchtMngMap.getLong("agencyPayInFee"));
					trxPayOutData.put("agencyPayInFeeVat", calcVat(mchtMngMap.getLong("agencyPayInFee")));
					trxPayOutData.put("salesPayInFee", mchtMngMap.getLong("salesPayInFee"));
					trxPayOutData.put("salesPayInFeeVat", calcVat(mchtMngMap.getLong("salesPayInFee")));
				// 출금 수수료 수익 분배 미사용
				} else {
					trxPayOutData.put("distPayInFee", 0);
					trxPayOutData.put("distPayInFeeVat", 0);
					trxPayOutData.put("agencyPayInFee", 0);
					trxPayOutData.put("agencyPayInFeeVat", 0);
					trxPayOutData.put("salesPayInFee", 0);
					trxPayOutData.put("salesPayInFeeVat", 0);
				}
				
			// 출금 수수료 대납자 = 영업사 (출금 수수료 수익 분배 미사용)
			} else {
				settleData.put("payOutFee", 0);
				settleData.put("payOutFeeVat", 0);
				
				trxPayOutData.put("payOutFee", mchtMngMap.getLong("payOutFee"));
				trxPayOutData.put("payOutFeeVat", calcVat(mchtMngMap.getLong("payOutFee")));
				trxPayOutData.put("payOutType", mchtMngMap.getString("payOutType"));
				
				if(mchtMngMap.getString("payOutType").equals("대행사")) {
					trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(mchtId).getString("distId"));
				} else if (mchtMngMap.getString("payOutType").equals("에이전시")) {
					trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(mchtId).getString("agencyId"));
				} else if (mchtMngMap.getString("payOutType").equals("지사")) {
					trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(mchtId).getString("salesId"));
				} else {
					trxPayOutData.put("payOutId", "");
				}
				
				trxPayOutData.put("distPayInFee", 0);
				trxPayOutData.put("distPayInFeeVat", 0);
				trxPayOutData.put("agencyPayInFee", 0);
				trxPayOutData.put("agencyPayInFeeVat", 0);
				trxPayOutData.put("salesPayInFee", 0);
				trxPayOutData.put("salesPayInFeeVat", 0);
			}
		} else {
			trxPayOutData.put("distId", new RealTimePayOutDAO().getMcht(mchtId).getString("distId"));
			//trxPayOutData.put("distNum", mchtMngVactMap.getInt("distNum"));
			trxPayOutData.put("stlDistType", mchtMngVactMap.getString("distSettleType"));
			trxPayOutData.put("stlDistId"	, "");
			trxPayOutData.put("agencyId", new RealTimePayOutDAO().getMcht(mchtId).getString("agencyId"));
			//trxPayOutData.put("agencyNum", mchtMngVactMap.getInt("agencyNum"));
			trxPayOutData.put("stlAgencyType", mchtMngVactMap.getString("agencySettleType"));
			trxPayOutData.put("stlAgencyId"	, "");
			trxPayOutData.put("salesId", new RealTimePayOutDAO().getMcht(mchtId).getString("salesId"));
			//trxPayOutData.put("salesNum", mchtMngVactMap.getInt("salesNum"));
			trxPayOutData.put("stlSalesType", mchtMngVactMap.getString("salesSettleType"));
			trxPayOutData.put("stlSalesId"	, "");
			
			// 출금 수수료 대납자 = 가맹점
			if(mchtMngVactMap.getString("payOutType").equals("가맹점")) {
				settleData.put("payOutFee", payOutFee);
				settleData.put("payOutFeeVat", payOutFeeVat);
				
				trxPayOutData.put("payOutFee", mchtMngVactMap.getLong("payOutFee"));
				trxPayOutData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
				trxPayOutData.put("payOutType", "가맹점");
				trxPayOutData.put("payOutId", mchtId);
				
				// 출금 수수료 수익 분배 사용
				if(mchtMngVactMap.getString("payInStatus").equals("사용")) {
					trxPayOutData.put("distPayInFee", mchtMngVactMap.getLong("distPayInFee"));
					trxPayOutData.put("distPayInFeeVat", calcVat(mchtMngVactMap.getLong("distPayInFee")));
					trxPayOutData.put("agencyPayInFee", mchtMngVactMap.getLong("agencyPayInFee"));
					trxPayOutData.put("agencyPayInFeeVat", calcVat(mchtMngVactMap.getLong("agencyPayInFee")));
					trxPayOutData.put("salesPayInFee", mchtMngVactMap.getLong("salesPayInFee"));
					trxPayOutData.put("salesPayInFeeVat", calcVat(mchtMngVactMap.getLong("salesPayInFee")));
				// 출금 수수료 수익 분배 미사용
				} else {
					trxPayOutData.put("distPayInFee", 0);
					trxPayOutData.put("distPayInFeeVat", 0);
					trxPayOutData.put("agencyPayInFee", 0);
					trxPayOutData.put("agencyPayInFeeVat", 0);
					trxPayOutData.put("salesPayInFee", 0);
					trxPayOutData.put("salesPayInFeeVat", 0);
				}
			
			// 출금 수수료 대납자 = 영업사 (출금 수수료 수익 분배 미사용)
			} else {
				settleData.put("payOutFee", 0);
				settleData.put("payOutFeeVat", 0);
				
				trxPayOutData.put("payOutFee", mchtMngVactMap.getLong("payOutFee"));
				trxPayOutData.put("payOutFeeVat", calcVat(mchtMngVactMap.getLong("payOutFee")));
				trxPayOutData.put("payOutType", mchtMngVactMap.getString("payOutType"));
				
				if(mchtMngVactMap.getString("payOutType").equals("대행사")) {
					trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(mchtId).getString("distId"));
				} else if (mchtMngVactMap.getString("payOutType").equals("에이전시")) {
					trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(mchtId).getString("agencyId"));
				} else if (mchtMngVactMap.getString("payOutType").equals("지사")) {
					trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(mchtId).getString("salesId"));
				} else {
					trxPayOutData.put("payOutId", "");
				}
				
				trxPayOutData.put("distPayInFee", 0);
				trxPayOutData.put("distPayInFeeVat", 0);
				trxPayOutData.put("agencyPayInFee", 0);
				trxPayOutData.put("agencyPayInFeeVat", 0);
				trxPayOutData.put("salesPayInFee", 0);
				trxPayOutData.put("salesPayInFeeVat", 0);
			}
		}
		
		
		/*if(firmPort == 10006) {
			//KWON_FIRM - 우리은행
			if(mchtTaxMap.getString("bankCd").equals("020")) { 
				 settleData.put("bankFee",50); 
			}else { 
				 settleData.put("bankFee", 100); 
			}
		}else if(firmPort == 10026) {
			//KWON_FIRM_KSNET - 케이뱅크
			settleData.put("bankFee", 99);
		}*/
		
		if(stlAmount != 0) {
			settleData.put("payOutAmount", stlAmount - settleData.getLong("payOutFee") - settleData.getLong("payOutFeeVat"));	
		}else {
			settleData.put("payOutAmount",0);
		}
		
		trxPayOutData.put("payOutAmount", settleData.getLong("payOutAmount"));
				
		settleData.put("bankCd", mchtTaxMap.getString("bankCd"));
		settleData.put("bankName", mchtTaxMap.getString("bankName"));
		settleData.put("account", dao.getAESEnc(mchtTaxMap.getString("account")).replace("-", "").trim());
		settleData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
		settleData.put("stlRate", stlRate);
		settleData.put("stlType", stlType);
		settleData.put("regId", "SYSTEM");
		
		trxPayOutData.put("bankCd", mchtTaxMap.getString("bankCd"));
		trxPayOutData.put("bankName", mchtTaxMap.getString("bankName"));
		trxPayOutData.put("account", dao.getAESEnc(mchtTaxMap.getString("account")).replace("-", "").trim());
		trxPayOutData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
		
		logger.info("stlId  	  : {}",stlId);
		logger.info("mchtId  	  : {}",mchtId);
		logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
		
		if(!dao.insertSettleAuto(settleData)){
			msgBody = type + " PG_SETTLE_AUTO INSERT 실패. 확인요망 [" + stlDay + "][" + mchtId + "]";
		} else {
			if(type.equals("V")) {
				dao.setSettleToIdx(stlId, stlDay, mchtId, stlType);
				dao.updateStlId(stlId);
			}
			dao.insertTrxPayOut(trxPayOutData);
		}
	}

	public long calcFee(long amount,double rate){
		rate = rateFormat(rate);
		long decimal = 10000;
		long fee = 0;
		if(amount < 0){
			fee = -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
		}else{
			fee = new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
		}
		return fee;
	}
	
	public long calcVat(long fee){
		if(fee < 0){
			return -new Double(-fee *10 /100).longValue();
		}else{
			return new Double(fee *10 /100).longValue();
		}
	}
	
	public long calcFeeVat(long amount,double rate){
		long fee = calcFee(amount,rate);
		return fee+ calcVat(fee);
	}
	
	public double rateFormat(double rate){
		String pattern = "#.#####";
		DecimalFormat format = new DecimalFormat(pattern);
		return new Double(format.format(rate)).doubleValue();
	}
	
	/**
     * config 파일 읽어서 변수에 세팅
     */
    public void configSetting() {
    	try{
            // 프로퍼티 파일 위치
    		//운영
            //String propFile = "/home/bkwinners/MARU/MARU_DAEMON/conf/firmconfig.properties";
    		//테스트
    		String propFile = "/home/KWON/KWON_DAEMON/conf/firmconfig.properties";
            //로컬
    		//String propFile = "D:\\workspace_creditop/github/MARU_DAEMON/conf/firmconfig.properties";
    		
            // 프로퍼티 객체 생성
            Properties props = new Properties();
            
            // 프로퍼티 파일 스트림에 담기
            FileInputStream fis = new FileInputStream(propFile);

            // 프로퍼티 파일 로딩
            props.load(new java.io.BufferedInputStream(fis));
            
            String strFirmPort = props.getProperty("firm_port");
            
            if(strFirmPort != null && !"".equals(strFirmPort)) {
            	firmPort = Integer.parseInt(strFirmPort);
            }
        }catch(Exception e){
        	logger.info(e.getMessage(), e);
        }
    }
}
