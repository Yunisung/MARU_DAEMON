package com.pgmate.dm.main;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;

import com.pgmate.dm.dao.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Settle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Settle.class );
	private static String[] grageArray = {"대표가맹점"};
	private static String[] monthArray = {"대행사","에이전시","지사"};
	private int dayErr = 0;
	private int monthErr = 0;
	private long daySumAmt = 0;
	private long monthSumAmt = 0;
	private int daySumCnt = 0;
	private int monthSumCnt = 0;
	private String msgBody = "";
	private String day = "";
	
	public Settle(String cmd) {
		if(Pattern.matches("^[0-9]{8}$", cmd)){
			DecimalFormat formatter = new DecimalFormat("###,###");
			
			day = cmd.substring(0,4) + "년 " + cmd.substring(4,6) + "월 " + cmd.substring(6) + "일";
			
			// 일정산(터미널)
//			execute(cmd);
//			vactMchtExcute(cmd); // 가상계좌
			
			SmsGw smsGw = new SmsGw();
			
			// 휴일엔 정산대상건이 없으므로 문자발송을 하지 않음
			if(!new SettleMchtDAO().isRestDay(cmd)) {
				if(dayErr == 0) {
					msgBody = "[" + day + "] 일정산대상 건수 : " +  formatter.format(daySumCnt) + "건, 지급예정금액 : " + formatter.format(daySumAmt) + "원 입니다.";
				}else {
					msgBody = day + " 일정산 생성오류. 확인요망.";
				}
				
				smsGw.sendMessage("0", "3", msgBody);
			}
            	
			// 월정산(가상계좌)
			if("01".equals(cmd.substring(6))){
				monthVactExecute(cmd); // 가상계좌

				if(monthErr == 0) {
					msgBody = "[" + day + "] 월 가상계좌 정산대상 건수 : " +  formatter.format(monthSumCnt) + "건, 지급예정금액 : " + formatter.format(monthSumAmt) + "원 입니다.";
				}else {
					msgBody = day + " 월 가상계좌 정산 생성오류. 확인요망.";
				}
				
//				smsGw.sendMessage("0", "3", msgBody);
			// 월정산(영업라인)
			}else if("08".equals(cmd.substring(6))){
				monthExecute(cmd);	// 영업라인

				if(monthErr == 0) {
					msgBody = "[" + day + "] 월 영업라인 정산대상 건수 : " +  formatter.format(monthSumCnt) + "건, 지급예정금액 : " + formatter.format(monthSumAmt) + "원 입니다.";
				}else {
					msgBody = day + " 월 영업라인 정산 생성오류. 확인요망.";
				}

				monthRentExecute(cmd);	// 영업라인

				if(monthErr == 0) {
					msgBody = "[" + day + "] 월 월세 영업라인 정산대상 건수 : " +  formatter.format(monthSumCnt) + "건, 지급예정금액 : " + formatter.format(monthSumAmt) + "원 입니다.";
				}else {
					msgBody = day + " 월 월세 영업라인 정산 생성오류. 확인요망.";
				}
			}
		}else {
			logger.info("입력받은 날짜가 올바르지 않습니다. = {}",cmd);
		}
	}
	
	private void monthVactExecute(String stlDay) {
		try {
			logger.info("= == === === === === {} 월 영업대행 가상계좌 정산 시작 === === === === == =", stlDay.substring(0, 6));
			
			for(String grage : monthArray) {
				logger.info("= == === GRADE : {}", grage);
				ImplVactSettle stlDAO = getVactDAO(grage);
				if(stlDAO != null){
				
					List<SharedMap<String,Object>> settleList = stlDAO.getSettleList(stlDay);
					logger.info("MAKE SETTLE COUNT : {}", settleList.size());
					for(SharedMap<String,Object> data : settleList){
						String stlId = stlDAO.getSettleId();
						logger.info("STL_ID  	: {}",stlId);
						logger.info("MEMBER_ID 	: {}",data.getString("memberId"));
						logger.info("TAX ID 	: {}",CommonUtil.nToB(data.getString("taxId")));
						logger.info("STL_Fee 	: {}",data.getLong("stlFee"));
						
						data.put("stlId", stlId);
						data.put("grade", grage);
						data.put("status", "대기");
						data.put("payStatus", "대기");
//						data.put("stlDay", stlDay);
						data.put("stlAmt", data.getLong("stlFee"));
						data.put("payOutAmt", data.getLong("stlAmt"));
						data.put("payOutDay", "");
						data.put("summary", "");
						data.put("regId", "SYSTEM");
						data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
						data.remove("_idx");
						
						if(stlDAO.insertVactSettle(data)){
							String targetId = data.getString("memberId");
							
							stlDAO.setSettleToIdx(stlId,data.getString("stlDay"),targetId);
							stlDAO.updateTrxCap(stlId);
						}
					}
				}
				logger.info("GRADE : {} END === == =", grage);
			}
			logger.info("= == === === === === {} 월 영업대행 가상계좌 정산 종료 === === === === == =", stlDay.substring(0, 6));
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
			monthErr ++;
		}
	}

	private void vactMchtExcute(String stlDay) {
		try {
			if(new SettleMchtDAO().isRestDay(stlDay)) {
				logger.info("- -- --- ---- ---- ---- {} 일자 휴일 보류 ---- ---- ---- --- -- -", stlDay);
				return;
			}
			
			logger.info("= == === === === === {} 일자 가상계좌 정산 시작 === === === === == =", stlDay);
			
			SettleVactMchtDAO stlDAO = new SettleVactMchtDAO();
			
			List<SharedMap<String,Object>> settleList = stlDAO.getSettleList(stlDay);
			logger.info("MAKE VACT MCHT SETTLE COUNT : {}", settleList.size());
			for(SharedMap<String,Object> data : settleList){
				String stlId = stlDAO.getSettleId();
				logger.info("STL_ID  	: {}",stlId);
				logger.info("MEMBER_ID 	: {}",data.getString("mchtId"));
				logger.info("TAX ID 	: {}",CommonUtil.nToB(data.getString("taxId")));
				logger.info("STL_AMT 	: {}",data.getLong("stlAmount"));
						
				data.put("stlId", stlId);
				data.put("status", "대기");
				data.put("payOutAmt", data.getLong("stlAmount"));
				data.put("payStatus", "대기");
				data.put("stlDay", stlDay);
				data.put("payOutDay", "");
				data.put("regId", "SYSTEM");
				data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
				if(data.getString("stlType").indexOf("C+") > -1) {
					data.put("settleSvc", "충전정산");
				}else {
					data.put("settleSvc", "일반");
				}
				data.remove("stlType");
				data.remove("_idx");
						
				if(stlDAO.insertVactSettleMcht(data)){
					stlDAO.setSettleToIdx(stlId,stlDay,data.getString("mchtId"));
					stlDAO.updateTrxCap(stlId);
				}
			}
			
			logger.info("= == === === === === {} 일자 가상계좌 정산 종료 === === === === == =", stlDay);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			dayErr ++;
		}
	}

	public void monthExecute(String stlDay) {
		try {
			logger.info("= == === === === === {} 월 영업대행 정산 시작 === === === === == =", stlDay.substring(0, 6));
			
			for(String grage : monthArray) {
				logger.info("= == === GRADE : {}", grage);
				ImplSettle stlDAO = getDAO(grage);
				if(stlDAO != null){
				
					List<SharedMap<String,Object>> settleList = stlDAO.getSettleList(stlDay);
					logger.info("MAKE SETTLE COUNT : {}", settleList.size());
					for(SharedMap<String,Object> data : settleList){
						String stlId = stlDAO.getSettleId();
						logger.info("STL_ID  	: {}",stlId);
						logger.info("MEMBER_ID 	: {}",data.getString("memberId"));
						logger.info("TAX ID 	: {}",CommonUtil.nToB(data.getString("taxId")));
						logger.info("STL_AMT 	: {}",data.getLong("stlAmt"));
						
						data.put("stlId", stlId);
						data.put("grade", grage);
						data.put("status", "대기");
						data.put("payStatus", "대기");
//						data.put("stlDay", stlDay);
						data.put("payOutAmt", data.getLong("stlAmt"));
						data.put("payOutDay", "");
						data.put("summary", "");
						data.put("regId", "SYSTEM");
						data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
						data.remove("_idx");
						
						if(stlDAO.insertSettle(data)){
							String targetId = data.getString("memberId");
							
							stlDAO.setSettleToIdx(stlId,data.getString("stlDay"),targetId);
							stlDAO.updateTrxCap(stlId);
						}
						
						monthSumAmt += data.getLong("stlAmt");
						monthSumCnt += data.getInt("payCnt");
						monthSumCnt += data.getInt("rfdCnt");
					}
				}
				logger.info("GRADE : {} END === == =", grage);
			}
			logger.info("= == === === === === {} 월 영업대행 정산 종료 === === === === == =", stlDay.substring(0, 6));
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			monthErr ++;
		}
	}

	public void monthRentExecute(String stlDay) {
		try {
			logger.info("= == === === === === {} 월 월세앱 영업대행 정산 시작 === === === === == =", stlDay.substring(0, 6));

			for(String grage : monthArray) {
				logger.info("= == === GRADE : {}", grage);
				ImplRentSettle stlDAO = getRentDAO(grage);
				if(stlDAO != null){

					List<SharedMap<String,Object>> settleList = stlDAO.getSettleList(stlDay);
					logger.info("MAKE RENT SETTLE COUNT : {}", settleList.size());
					for(SharedMap<String,Object> data : settleList){
						String stlId = stlDAO.getSettleId();
						logger.info("STL_ID  	: {}",stlId);
						logger.info("MEMBER_ID 	: {}",data.getString("memberId"));
						logger.info("TAX ID 	: {}",CommonUtil.nToB(data.getString("taxId")));
						logger.info("STL_AMT 	: {}",data.getLong("stlAmt"));

						data.put("stlId", stlId);
						data.put("grade", grage);
						data.put("status", "대기");
						data.put("payStatus", "대기");
//						data.put("stlDay", stlDay);
						data.put("payOutAmt", data.getLong("stlAmt"));
						data.put("payOutDay", "");
						data.put("summary", "");
						data.put("regId", "SYSTEM");
						data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
						data.remove("_idx");

						if(stlDAO.insertRentSettle(data)){
							String targetId = data.getString("memberId");

							stlDAO.setSettleToIdx(stlId,data.getString("stlDay"),targetId);
							stlDAO.updateTrxCap(stlId);
						}

						monthSumAmt += data.getLong("stlAmt");
						monthSumCnt += data.getInt("payCnt");
						monthSumCnt += data.getInt("rfdCnt");
					}
				}
				logger.info("GRADE : {} END === == =", grage);
			}
			logger.info("= == === === === === {} 월 월세앱 영업대행 정산 종료 === === === === == =", stlDay.substring(0, 6));
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			monthErr ++;
		}
	}

	public void execute(String stlDay){
		try {
			if(new SettleMchtDAO().isRestDay(stlDay)) {
				logger.info("- -- --- ---- ---- ---- {} 일자 휴일 보류 ---- ---- ---- --- -- -", stlDay);
				return;
			}
			
			logger.info("= == === === === === {} 일자 정산 시작 === === === === == =", stlDay);
			
			for(String grage : grageArray) {
				logger.info("= == === GRADE : {}", grage);
				ImplSettle stlDAO = getDAO(grage);
				if(stlDAO != null){
				
					List<SharedMap<String,Object>> settleList = stlDAO.getSettleList(stlDay);
					logger.info("MAKE SETTLE COUNT : {}", settleList.size());
					for(SharedMap<String,Object> data : settleList){
						String stlId = stlDAO.getSettleId();
						logger.info("STL_ID  	: {}",stlId);
						logger.info("MEMBER_ID 	: {}",grage.equals("대표가맹점") ? data.getString("tmnId") : data.getString("memberId"));
						logger.info("TAX ID 	: {}",CommonUtil.nToB(data.getString("taxId")));
						logger.info("STL_AMT 	: {}",data.getLong("stlAmt"));
						
						data.put("stlId", stlId);
						data.put("grade", grage);
						data.put("status", "대기");
						data.put("payStatus", "대기");
						data.put("stlDay", stlDay);
						data.put("payOutDay", "");
						data.put("regId", "SYSTEM");
						data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
						data.remove("_idx");
						
						if(stlDAO.insertSettle(data)){
							String targetId = data.getString("memberId");
							if(grage.equals("가맹점")) {
								targetId = data.getString("taxId");
							} else if(grage.equals("대표가맹점")) {
								targetId = data.getString("tmnId");
							}
							stlDAO.setSettleToIdx(stlId,stlDay,targetId);
							stlDAO.updateTrxCap(stlId);
						}
						
						daySumAmt += data.getLong("stlAmt");
						daySumCnt += data.getInt("payCnt");
						daySumCnt += data.getInt("rfdCnt");
					}
				}
				logger.info("GRADE : {} END === == =", grage);
			}
			logger.info("= == === === === === {} 일자 정산 종료 === === === === == =", stlDay);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			dayErr ++;
		}
	}
		
	private ImplSettle getDAO(String grade) {
		ImplSettle stlDAO = null;
		if(grade.equals("에이전시")) {
			stlDAO = new SettleAgencyDAO();
		} else if(grade.equals("대행사")) {
			stlDAO = new SettleDistDAO();
		} /*else if(grade.equals("가맹점")) {
			stlDAO = new SettleMchtDAO();
		} */else if(grade.equals("지사")) {
			stlDAO = new SettleSalesDAO();
		} else if(grade.equals("대표가맹점")) {
			stlDAO = new SettleTmnDAO();
		}
		return stlDAO;
	}
	
	private ImplVactSettle getVactDAO(String grade) {
		ImplVactSettle stlDAO = null;
		if(grade.equals("에이전시")) {
			stlDAO = new SettleVactAgencyDAO();
		} else if(grade.equals("대행사")) {
			stlDAO = new SettleVactDistDAO();
		} /*else if(grade.equals("가맹점")) {
			stlDAO = new SettleMchtDAO();
		} */else if(grade.equals("지사")) {
			stlDAO = new SettleVactSalesDAO();
		}
		return stlDAO;
	}

	private ImplRentSettle getRentDAO(String grade) {
		ImplRentSettle stlDAO = null;
		if(grade.equals("에이전시")) {
			stlDAO = new SettleRentAgencyDAO();
		} else if(grade.equals("대행사")) {
			stlDAO = new SettleRentDistDAO();
		} /*else if(grade.equals("가맹점")) {
			stlDAO = new SettleMchtDAO();
		} */else if(grade.equals("지사")) {
			stlDAO = new SettleRentSalesDAO();
		}

		return stlDAO;
	}
	
	public static void main(String[] args){
		new Settle(args[0]);
	}
}
