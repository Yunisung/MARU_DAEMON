package com.pgmate.dm.main;

import java.io.FileInputStream;
import java.text.DecimalFormat;
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
public class AutoSettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.AutoSettle.class );
	private SmsGw smsGw = null;
	private String msgBody = "";
    private int firmPort = 0;
    
	public static void main(String[] args){
		new AutoSettle(args);
	}
	
	public AutoSettle(String[] args) {
		logger.info("==================================================");
		logger.info("AutoSettle Strart");
		smsGw = new SmsGw();
		
		configSetting();
		autoSettle(args);
		autoVactSettle(args);
		
		logger.info("AutoSettle End");
		logger.info("==================================================");
	}
	
	public void autoSettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {
			String stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
			String stlType = "A+1";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			logger.info("자동정산 stlDay : " + stlDay + ", stlType : " + stlType);
			
			//매입원장에서 자동정산 정산예정일자의 정산데이터 조회
			List<SharedMap<String,Object>> getRealTimeSettleList = dao.getAutoSettleList(stlDay, stlType);
			logger.info("GetAutoSettleList COUNT : {}", getRealTimeSettleList.size());
			
			if(getRealTimeSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_SETTLE_AUTO INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getRealTimeSettleList){
					msgBody = "";
					
					String stlId = dao.getSettleId();
					
					SharedMap<String,Object> mchtMngMap		= dao.getMchtMngByMchtId(data.getString("mchtId"));
					SharedMap<String,Object> mchtTaxMap		= dao.getMchtTaxByMchtId(data.getString("mchtId"));
					SharedMap<String,Object> agencyMngMap	= dao.getAgencyMngByNum(mchtMngMap.getInt("agencyNum"));
					SharedMap<String,Object> distMngMap		= dao.getDistMngByNum(mchtMngMap.getInt("distNum"));
					SharedMap<String,Object> salesMngMap	= dao.getSalesMngByNum(mchtMngMap.getInt("salesNum"));
					
					SharedMap<String,Object> settleData = new SharedMap<String, Object>();
					SharedMap<String,Object> trxPayOutData = new SharedMap<String, Object>(); // 출금수수료 정산관련 거래 데이터

					settleData.put("stlId", stlId);
					settleData.put("mchtId", data.getString("mchtId"));

					trxPayOutData.put("trxId", stlId);
					trxPayOutData.put("mchtId", data.getString("mchtId"));
					trxPayOutData.put("trxType", "자동");
					trxPayOutData.put("amount", data.getLong("stlAmount"));
					
					
					//if("apiwin".equals(data.getString("mchtId"))) {
					//	settleData.put("status", "지급보류");
					//}else {
					//	settleData.put("status", "지급대기");
					//}
					
					settleData.put("status", "지급대기");
					settleData.put("stlDay", data.getString("stlDay"));
					settleData.put("payType", "C");
					settleData.put("startDay", data.getString("startDay"));
					settleData.put("endDay", data.getString("endDay"));
					
					settleData.put("payAmt", data.getLong("payAmt"));
					settleData.put("payFee", data.getLong("payFee"));
					settleData.put("payVat", data.getLong("payVat"));
					settleData.put("payCnt", data.getLong("payCnt"));
					
					settleData.put("rfdAmt", data.getLong("rfdAmt"));
					settleData.put("rfdFee", data.getLong("rfdFee"));
					settleData.put("rfdVat", data.getLong("rfdVat"));
					settleData.put("rfdCnt", data.getLong("rfdCnt"));
					
					if(agencyMngMap.size() >0 ){
						trxPayOutData.put("agencyNum"	, mchtMngMap.getLong("agencyNum"));
						trxPayOutData.put("agencyId", agencyMngMap.getString("agencyId"));
						trxPayOutData.put("stlAgencyType", agencyMngMap.getString("settleType"));
						trxPayOutData.put("stlAgencyId"	, "");
					}
					if(distMngMap.size() >0 ){
						trxPayOutData.put("distNum"	, mchtMngMap.getLong("distNum"));
						trxPayOutData.put("distId", distMngMap.getString("distId"));
						trxPayOutData.put("stlDistType", distMngMap.getString("settleType"));
						trxPayOutData.put("stlDistId"	, "");
					}
					if(salesMngMap.size() >0 ){
						trxPayOutData.put("salesNum"	, mchtMngMap.getLong("salesNum"));
						trxPayOutData.put("salesId", salesMngMap.getString("salesId"));
						trxPayOutData.put("stlSalesType", salesMngMap.getString("settleType"));
						trxPayOutData.put("stlSalesId"	, "");
					}
					
					// 출금 수수료 대납자 = 가맹점
					if(mchtMngMap.getString("payOutType").equals("가맹점")) {
						settleData.put("payOutFee", mchtMngMap.getLong("payOutFee"));
						settleData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
						
						trxPayOutData.put("payOutFee", mchtMngMap.getLong("payOutFee"));
						trxPayOutData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
						trxPayOutData.put("payOutType", "가맹점");
						trxPayOutData.put("payOutId", data.getString("mchtId"));
						
						// 출금 수수료 수익 분배 사용
						if(mchtMngMap.getString("payInStatus").equals("사용")) {
							trxPayOutData.put("distPayInFee", mchtMngMap.getLong("distPayInFee"));
							trxPayOutData.put("distPayInFeeVat", calcVat(mchtMngMap.getLong("distPayInFee")));
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
							trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("distId"));
						} else if (mchtMngMap.getString("payOutType").equals("에이전시")) {
							trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("agencyId"));
						} else if (mchtMngMap.getString("payOutType").equals("지사")) {
							trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("salesId"));
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
					
					if(firmPort == 10006) {
						//KWON_FIRM - 우리은행
						if(mchtTaxMap.getString("bankCd").equals("020")) { 
							 settleData.put("bankFee",50); 
						}else { 
							 settleData.put("bankFee", 100); 
						}
					}else if(firmPort == 10026) {
						//KWON_FIRM_KSNET - 케이뱅크
						settleData.put("bankFee", 99);
					}
					
					if(data.getLong("stlAmount") != 0) {
						settleData.put("payOutAmount", data.getLong("stlAmount") - settleData.getLong("payOutFee") - settleData.getLong("payOutFeeVat"));	
					}else {
						settleData.put("payOutAmount",0);
					}
					
					trxPayOutData.put("payOutAmount",settleData.getLong("payOutAmount"));
						
					settleData.put("bankCd", mchtTaxMap.getString("bankCd"));
					settleData.put("bankName", mchtTaxMap.getString("bankName"));
					settleData.put("account", dao.getAESEnc(mchtTaxMap.getString("account")).replace("-", "").trim());
					settleData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
					settleData.put("stlRate", data.getDouble("stlRate"));
					settleData.put("stlType", data.getString("stlType"));
					settleData.put("regId", "SYSTEM");
					
					trxPayOutData.put("bankCd", mchtTaxMap.getString("bankCd"));
					trxPayOutData.put("bankName", mchtTaxMap.getString("bankName"));
					trxPayOutData.put("account", dao.getAESEnc(mchtTaxMap.getString("account").replace("-", "").trim()));
					trxPayOutData.put("accntHolder", dao.getAESEnc(mchtTaxMap.getString("accntHolder")));
					
					logger.info("stlId  	  : {}",stlId);
					logger.info("mchtId  	  : {}",data.getString("mchtId"));
					logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
					
					if(!dao.insertSettleAuto(settleData)){
						msgBody = "PG_SETTLE_AUTO INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
					} else {
						// 출금수수료 정산 관련 거래 테이블 insert
						dao.insertTrxPayOut(trxPayOutData);
					}
					
					if(!"".equals(msgBody)) {
						logger.info(msgBody);
						
						smsGw.sendMessage("0", "4", msgBody);
					}
					logger.info("==================================================");
				}
				logger.info("PG_SETTLE_AUTO INSERT END");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "자동정산 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
		}
	}
	
	public void autoVactSettle(String[] args){
		RealTimePayOutDAO dao = new RealTimePayOutDAO();
		
		try {
			String stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
			String stlType = "A+1";
			
			if(args.length > 0 && args[0] != null && !"".equals(args[0])) {
				stlType = args[0];
			}
			if(args.length > 1 && args[1] != null && !"".equals(args[1])) {
				stlDay = args[1];
			}
			
			logger.info("가상계좌 자동정산 stlDay : " + stlDay + ", stlType : " + stlType);
			
			//가상계좌 거래내역 정산예정일자의 자동정산 정산데이터 조회
			List<SharedMap<String,Object>> getAutoVactSettleList = dao.getAutoVactSettleList(stlDay, stlType);
			logger.info("getAutoVactSettleList COUNT : {}", getAutoVactSettleList.size());
			
			if(getAutoVactSettleList.size() > 0) {
				logger.info("==================================================");
				logger.info("PG_SETTLE_AUTO VACT INSERT START");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getAutoVactSettleList){
					msgBody = "";
					
					String stlId = dao.getSettleId();
					
					SharedMap<String,Object> mchtMngVactMap	= dao.getMchtMngVactByMchtId(data.getString("mchtId"));

					SharedMap<String,Object> settleData = new SharedMap<String, Object>();
					SharedMap<String,Object> trxPayOutData = new SharedMap<String, Object>(); // 출금수수료 정산관련 거래 데이터

					settleData.put("stlId", stlId);
					settleData.put("mchtId", data.getString("mchtId"));
					
					trxPayOutData.put("trxId", stlId);
					trxPayOutData.put("mchtId", data.getString("mchtId"));
					trxPayOutData.put("trxType", "자동");
					trxPayOutData.put("amount", data.getLong("stlAmount"));
					
					settleData.put("status", "지급대기");
					settleData.put("stlDay", data.getString("stlDay"));
					settleData.put("payType", "V");
					settleData.put("startDay", data.getString("startDay"));
					settleData.put("endDay", data.getString("endDay"));
					
					settleData.put("payAmt", data.getLong("payAmt"));
					settleData.put("payFee", data.getLong("payFee"));
					settleData.put("payVat", data.getLong("payVat"));
					settleData.put("payCnt", data.getLong("payCnt"));
					
					settleData.put("rfdAmt", data.getLong("rfdAmt"));
					settleData.put("rfdFee", data.getLong("rfdFee"));
					settleData.put("rfdVat", data.getLong("rfdVat"));
					settleData.put("rfdCnt", data.getLong("rfdCnt"));
					
					trxPayOutData.put("distId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("distId"));
					trxPayOutData.put("distNum", mchtMngVactMap.getInt("distNum"));
					trxPayOutData.put("stlDistType", mchtMngVactMap.getString("distSettleType"));
					trxPayOutData.put("stlDistId"	, "");
					trxPayOutData.put("agencyId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("agencyId"));
					trxPayOutData.put("agencyNum", mchtMngVactMap.getInt("agencyNum"));
					trxPayOutData.put("stlAgencyType", mchtMngVactMap.getString("agencySettleType"));
					trxPayOutData.put("stlAgencyId"	, "");
					trxPayOutData.put("salesId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("salesId"));
					trxPayOutData.put("salesNum", mchtMngVactMap.getInt("salesNum"));
					trxPayOutData.put("stlSalesType", mchtMngVactMap.getString("salesSettleType"));
					trxPayOutData.put("stlSalesId"	, "");
					
					// 출금 수수료 대납자 = 가맹점
					if(mchtMngVactMap.getString("payOutType").equals("가맹점")) {
						settleData.put("payOutFee", mchtMngVactMap.getLong("payOutFee"));
						settleData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
						
						trxPayOutData.put("payOutFee", mchtMngVactMap.getLong("payOutFee"));
						trxPayOutData.put("payOutFeeVat", calcVat(settleData.getLong("payOutFee")));
						trxPayOutData.put("payOutType", "가맹점");
						trxPayOutData.put("payOutId", data.getString("mchtId"));
						
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
							trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("distId"));
						} else if (mchtMngVactMap.getString("payOutType").equals("에이전시")) {
							trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("agencyId"));
						} else if (mchtMngVactMap.getString("payOutType").equals("지사")) {
							trxPayOutData.put("payOutId", new RealTimePayOutDAO().getMcht(data.getString("mchtId")).getString("salesId"));
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
					
					if(firmPort == 10006) {
						//KWON_FIRM - 우리은행
						if(data.getString("bankCd").equals("020")) {
							settleData.put("bankFee", 50);
						}else {
							settleData.put("bankFee", 100);
						}
					}else if(firmPort == 10026) {
						//KWON_FIRM_KSNET - 케이뱅크
						settleData.put("bankFee", 99);
					}

					if(data.getLong("stlAmount") != 0) {
						settleData.put("payOutAmount", data.getLong("stlAmount") - settleData.getLong("payOutFee") - settleData.getLong("payOutFeeVat"));	
					}else {
						settleData.put("payOutAmount",0);
					}
					
					trxPayOutData.put("payOutAmount", settleData.getLong("payOutAmount"));
						
					settleData.put("bankCd", data.getString("bankCd"));
					settleData.put("bankName", data.getString("bankName"));
					settleData.put("account", dao.getAESEnc(data.getString("account")).replace("-", "").trim());
					settleData.put("accntHolder", dao.getAESEnc(data.getString("accntHolder")));
					settleData.put("stlRate", mchtMngVactMap.getDouble("rate"));
					settleData.put("stlType", data.getString("stlType"));
					settleData.put("regId", "SYSTEM");
					
					trxPayOutData.put("bankCd", data.getString("bankCd"));
					trxPayOutData.put("bankName", data.getString("bankName"));
					trxPayOutData.put("account", dao.getAESEnc(data.getString("account")).replace("-", "").trim());
					trxPayOutData.put("accntHolder", dao.getAESEnc(data.getString("accntHolder")));
					
					logger.info("stlId  	  : {}",stlId);
					logger.info("mchtId  	  : {}",data.getString("mchtId"));
					logger.info("payOutAmount : {}",settleData.getLong("payOutAmount"));
					
					if(dao.insertSettleAuto(settleData)){
						dao.setSettleToIdx(stlId, stlDay, data.getString("mchtId"), stlType);
						dao.updateStlId(stlId);
						// 출금수수료 정산 관련 거래 테이블 insert
						dao.insertTrxPayOut(trxPayOutData);
					}else {
						msgBody = "PG_SETTLE_AUTO VACT INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
					}
					
					if(!"".equals(msgBody)) {
						logger.info(msgBody);
						
						smsGw.sendMessage("0", "4", msgBody);
					}
					logger.info("==================================================");
				}
				logger.info("PG_SETTLE_AUTO VACT INSERT END");
				logger.info("==================================================");
			}
			
			//가상계좌 거래내역 정산예정일자의 자동정산 정산 인증 수수료 데이터 조회
			List<SharedMap<String,Object>> getAutoVactSettleOrgFeeList = dao.getAutoVactSettleOrgFeeList(stlDay, stlType);
			logger.info("getAutoVactSettleOrgFeeList COUNT : {}", getAutoVactSettleOrgFeeList.size());
			
			if(getAutoVactSettleOrgFeeList.size() > 0) {
				logger.info("==================================================");
				logger.info("자동정산 인증수수료 처리 시작");
				logger.info("==================================================");
				
				for(SharedMap<String,Object> data : getAutoVactSettleOrgFeeList){
					msgBody = "";

					String stlId = dao.getStlId(data.getString("mchtId"), stlDay, stlType);
					
					if(!"".equals(stlId)) {
						dao.updateAuthFee(stlId, data.getLong("fee"), calcVat(data.getLong("fee")));
						dao.updateAuthStlId(stlId, data.getString("mchtId"), stlDay, stlType);
						
						logger.info("자동정산 인증 수수료 UPDATE");
						logger.info("stlId  : {}",stlId);
						logger.info("mchtId : {}",data.getString("mchtId"));
						logger.info("orgFee : {}",data.getLong("fee") + calcVat(data.getLong("fee")));
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
						
						logger.info("자동정산 인증 수수료 INSERT");
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
				logger.info("자동정산 인증수수료 처리 종료");
				logger.info("==================================================");
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			
			msgBody = "가상계좌 자동정산 오류발생. 확인요망 [" + e.getMessage() + "]";
			smsGw.sendMessage("0", "4", msgBody);
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
            String propFile = "/home/bkwinners/MARU/MARU_DAEMON/conf/firmconfig.properties"; 
    		//테스트
    		//String propFile = "/home/KWON/KWON_DAEMON/conf/firmconfig.properties";
            //로컬
    		//String propFile = "C:/workspace/KWON_DAEMON/conf/firmconfig.properties";
    		
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
