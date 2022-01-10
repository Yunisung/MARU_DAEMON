package com.pgmate.dm.main;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.ImplVaSettle;
import com.pgmate.dm.dao.VaSettleAgencyDAO;
import com.pgmate.dm.dao.VaSettleBranchDAO;
import com.pgmate.dm.dao.VaSettleDistDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class VaSettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VaSettle.class );
	private static String[] monthArray = {"대행사","에이전시","지사"};

	private int monthErr = 0;
	private long monthSumAmt = 0;
	private long monthSumCnt = 0;
	private String msgBody = "";
	private String day = "";
	private SmsGw smsGw = null;
			
	public VaSettle(String cmd) {
		logger.info("cmd : {} ",cmd);
		
		if(Pattern.matches("^[0-9]{8}$", cmd)){
			DecimalFormat formatter = new DecimalFormat("###,###");
			
			day = cmd.substring(0,4) + "년 " + cmd.substring(4,6) + "월 " + cmd.substring(6) + "일";
			
			smsGw = new SmsGw();
			
			// 월정산(영업대행)
			if("12".equals(cmd.substring(6))){
				monthVaExecute(cmd); // 가상계좌

				if(monthErr == 0) {
					msgBody = "[" + day + "] 월 가상계좌대행 서비스 정산대상 건수 : " +  formatter.format(monthSumCnt) + "건, 지급예정금액 : " + formatter.format(monthSumAmt) + "원 입니다.";
				}else {
					msgBody = day + " 월정산 생성오류. 확인요망.";
				}
				
				logger.info(msgBody);
				smsGw.sendMessage("0", "3", msgBody);
			}
		}else {
			logger.info("입력받은 날짜가 올바르지 않습니다. = {}",cmd);
		}
	}
	
	private void monthVaExecute(String stlDay) {
		try {
			logger.info("= == === === === === {} 월 영업대행 가상계좌 대행서비스 정산 시작 === === === === == =", stlDay.substring(0, 6));
			
			for(String grage : monthArray) {
				logger.info("= == === GRADE : {}", grage);
				ImplVaSettle stlDAO = getVaDAO(grage);
				if(stlDAO != null){
				
					List<SharedMap<String,Object>> settleList = stlDAO.getSettleList(stlDay);
					logger.info("MAKE SETTLE COUNT : {}", settleList.size());
					for(SharedMap<String,Object> data : settleList){
						String stlId = stlDAO.getSettleId();
						logger.info("STL_ID  	: {}",stlId);
						logger.info("MEMBER_ID 	: {}",data.getString("memberId"));
						logger.info("STL_Fee 	: {}",data.getLong("stlFee"));
						
						data.put("stlId", stlId);
						data.put("grade", grage);
						data.put("status", "대기");
						data.put("payStatus", "대기");
						data.put("stlAmt", data.getLong("stlFee"));
						data.put("payOutAmt", data.getLong("stlAmt"));
						data.put("payOutDay", "");
						data.put("summary", "");
						data.put("regId", "SYSTEM");
						data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
						data.remove("_idx");
						
						if(stlDAO.insertVaSettle(data)){
							String targetId = data.getString("memberId");
							
							stlDAO.setSettleToIdx(stlId,data.getString("stlDay"),targetId);
							stlDAO.updateTrx(stlId);
						}else {
							msgBody = "영업대행 가상계좌 대행서비스 정산 VaSettle Insert 실패 : [" + grage + "][" + stlId + "][" + data.getString("memberId") + "]";
							logger.info(msgBody);
							smsGw.sendMessage("0", "3", msgBody);
						}
						monthSumAmt += data.getLong("stlAmt");
						monthSumCnt += data.getLong("payCnt");
						monthSumCnt += data.getLong("withdrawCnt");
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

	private ImplVaSettle getVaDAO(String grade) {
		ImplVaSettle stlDAO = null;
		if(grade.equals("에이전시")) {
			stlDAO = new VaSettleAgencyDAO();
		} else if(grade.equals("대행사")) {
			stlDAO = new VaSettleDistDAO();
		} else if(grade.equals("지사")) {
			stlDAO = new VaSettleBranchDAO();
		}
		return stlDAO;
	}
	
	public static void main(String[] args){
		new VaSettle(args[0]);
	}
}
