package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class RealTimePayOutDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.dao.RealTimePayOutDAO.class );
	
	public RealTimePayOutDAO() {
		super.setDebug(false);
	}
	
	/**
	 * 실시간 정산 승인거래 원장에서 실시간 출금 데이터로 보내지 않은 데이터중에 실시간정산 전송간격이 지난 거래건들 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getRealTimePayList(String payType){
		String q = "SELECT * "
				+"	  FROM PG_TRX_REALTIME_PAY "
				+"   WHERE NOW() > DATE_ADD(regDate, INTERVAL transferInterval MINUTE) and "
				+"   	   resultCd = '0000' and "
				+"   	   payType = '" + payType + "' and "
				+"		   sendYn = 'N' "
				+"   order by regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 실시간 출금 데이터에서 출금완료하지 않은 대상건들 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getPayOutList(String payType){
		String q = "SELECT * "
				+"	  FROM PG_REALTIME_PAYOUT "
				+"   WHERE (resultCd is null or resultCd != '0000') and payType = '" + payType + "' and sendCnt < '3' "
				+"   order by regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 실시간 출금 데이터성공한 전일자 데이터 거래번호 리스트 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getYesterdaytPayOutList(String yesterday){
		String q = "SELECT trxId, payOutDay "
				+"	  FROM PG_REALTIME_PAYOUT "
				+"   WHERE sendCheck = 'Y' and payOutDay = '" + yesterday + "'"
				+"   order by regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 가맹점 아이디로 가맹점 지불 및 정산(PG_MCHT_MNG) 테이블 데이터 조회
	 * @param mchtId : 가맹점 아이디
	 * @return
	 */
	public SharedMap<String, Object> getMchtMngByMchtId(String mchtId) {
		super.setTable("PG_MCHT_MNG");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getDistMngByNum(int distNum) {
		super.setTable("PG_MAM_DIST_MNG");
		super.setColumns("*");
		super.addWhere("num", distNum, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getAgencyMngByNum(int agencyNum) {
		super.setTable("PG_MAM_AGENCY_MNG");
		super.setColumns("*");
		super.addWhere("num", agencyNum, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getSalesMngByNum(int salesNum) {
		super.setTable("PG_MAM_SALES_MNG");
		super.setColumns("*");
		super.addWhere("num", salesNum, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getMcht(String mchtId) {
		super.setTable("PG_MCHT");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	/**
	 * 가맹점 아이디로 가맹점 가상계좌 관리 테이블(PG_MCHT_MNG_VACT) 테이블 데이터 조회
	 * @param mchtId : 가맹점 아이디
	 * @return
	 */
	public SharedMap<String, Object> getMchtMngVactByMchtId(String mchtId) {
		super.setTable("PG_MCHT_MNG_VACT");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	/**
	 * 가상계좌 거래내역(PG_VACT_TRX) 테이블 데이터 조회
	 * @param mchtId : 가맹점 아이디
	 * @return
	 */
	public SharedMap<String, Object> getVactTrx(String trxId) {
		super.setTable("PG_VACT_TRX");
		super.setColumns("*");
		super.addWhere("vactId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	/**
	 * 가맹점 아이디로 가맹점 TAX 정보(PG_MCHT_TAX) 테이블 데이터 조회
	 * @param mchtId : 가맹점 아이디
	 * @return
	 */
	public SharedMap<String, Object> getMchtTaxByMchtId(String mchtId) {
		super.setTable("PG_MCHT_TAX");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public String getSnedCnt(String trxId){
		super.setTable("PG_REALTIME_PAYOUT");
		super.setColumns("sendCnt");
		super.addWhere("trxId",trxId,eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("sendCnt");
	}
	
	/**
	 * 실시간 출금 데이터 (PG_REALTIME_PAYOUT) 테이블 INSERT
	 * @param data
	 * @return
	 */
	public boolean insertRealtimePayOut(SharedMap<String,Object> data){
		super.setTable("PG_REALTIME_PAYOUT");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	public boolean insertTrxPayOut(SharedMap<String,Object> data){
		super.setTable("PG_TRX_PAYOUT");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	/**
	 * 실시간 정산 승인거래 원장 (PG_TRX_REALTIME_PAY) 테이블 전송여부와 전송일시 업데이트 
	 * @param trxId : 거래번호
	 * @return
	 */
	public boolean updateSendCheck(String trxId){
		String q = "UPDATE PG_TRX_REALTIME_PAY "
				+ "    SET sendYn='Y', sendDate = NOW() "
				+"	WHERE trxid = '"+trxId+"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 실시간 출금 전송횟수 업데이트
	 * @param trxId : 거래번호
	 * @param payOutDay : 출금일자
	 * @param payOutTime : 출금시간
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean countUpdate(String trxId){
		String q = "UPDATE PG_REALTIME_PAYOUT "
				+ "    SET sendCnt=sendCnt+1"
				+ "	 WHERE trxid = '"+trxId+"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 실시간 출금 전송횟수 업데이트
	 * @param trxId : 거래번호
	 * @param payOutDay : 출금일자
	 * @param payOutTime : 출금시간
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean countMinusUpdate(String trxId){
		String q = "UPDATE PG_REALTIME_PAYOUT "
				+ "    SET sendCnt=sendCnt-1"
				+ "	 WHERE trxid = '"+trxId+"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 실시간 출금 결과 업데이트
	 * @param trxId : 거래번호
	 * @param payOutDay : 출금일자
	 * @param payOutTime : 출금시간
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean updatePayOutRes(String trxId, String payOutDay, String payOutTime, String bankCd, String bankName, String account, String accntHolder,
								   String resCd, String resMsg, String sendCheck){
		String q = "UPDATE PG_REALTIME_PAYOUT "
				+ "    SET payOutDay='"+payOutDay+"',payOutTime='"+payOutTime+"',bankCd='"+bankCd+"',bankName='"+bankName+"',account='"+account+"',accntHolder='"+accntHolder+"',"
				+ "        resultCd='"+resCd+"', resultMsg='"+resMsg+"', sendCheck='"+sendCheck+"'"
				+ "	 WHERE trxid = '"+trxId+"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	public boolean updateTrxPayOut(String trxId, SharedMap<String, Object> trxMap){
		String q = "UPDATE PG_TRX_PAYOUT "
		+ "    SET payOutDay='"+trxMap.getString("payOutDay")+"',payOutTime='"+trxMap.getString("payOutTime")+"',"
		+ " stlDistDay='"+trxMap.getString("stlDistDay")+"',stlAgencyDay='"+trxMap.getString("stlAgencyDay")+"',stlSalesDay='"+trxMap.getString("stlSalesDay")+"',"
		+ " bankCd='"+trxMap.getString("bankCd")+"',bankName='"+trxMap.getString("bankName")+"',account='"+trxMap.getString("account")+"',accntHolder='"+trxMap.getString("accntHolder")+"',sendCheck='"+trxMap.getString("sendCheck")+"'"
		+ "	 WHERE trxId = '"+trxId+"'";
		
		boolean updated =  super.update(q);
		
		super.initRecord();
		return updated;
	}
	
	/**
	 * 실시간 출금 결과 업데이트
	 * @param trxId : 거래번호
	 * @param payOutDay : 출금일자
	 * @param payOutTime : 출금시간
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean updatePayOutCapUpdate(String trxId, String payOutDay){
		String q = "UPDATE PG_TRX_CAP_DTL "
				+ "    SET stlStatus = '정산완료', risk = '', payOutDay='"+payOutDay+"'"
				+ "	 WHERE capId = "
				+ "		(SELECT b.capId "
				+ "		   FROM PG_TRX_CAP a inner join PG_TRX_CAP_DTL b on a.capId = b.capId"
				+ "		  WHERE a.trxId = '" + trxId + "'"
				+ "		)";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 실시간 출금 3회실패시 매입 상태 정산보류로 업데이트
	 * @param trxId : 거래번호
	 * @return
	 */
	public boolean updatePayOutCancelCapUpdate(String trxId){
		String q = "UPDATE PG_TRX_CAP_DTL "
				+ "    SET stlStatus = '정산보류'"
				+ "	 WHERE capId = "
				+ "		(SELECT b.capId "
				+ "		   FROM PG_TRX_CAP a inner join PG_TRX_CAP_DTL b on a.capId = b.capId"
				+ "		  WHERE a.trxId = '" + trxId + "'"
				+ "		)";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 데이터 암호화
	 */
	public String getAESEnc(String value){
		String query = "SELECT FN_AES_ENC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	public static String getFunction(String function, String value) {
		String returnVal = "";
		String query = "SELECT " + function + "(?) as val";

		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rset = null;

		try {

			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, value);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				returnVal = rset.getString(1);
			}
			conn.commit();
		} catch (Exception t) {
			
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}
	
	/**
	 * 정산아이디 생성
	 * @return
	 */
	public synchronized static String getSettleId() {
		return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
	}
	
	/**
	 * 매입원장에서 정산예정일자의 정산데이터 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getAutoSettleList(String stlDay, String stlType){
		String q = "select X.*, (payAmt - payFee - payVat) + (rfdAmt - rfdVat - rfdFee) as stlAmount "
				+"    from "
				+"	  (SELECT mchtId,stlDay,min(trxDay) startDay,max(trxDay) endDay, "
				+"		  	  SUM(IF(capType ='매입',amount,0)) as payAmt, SUM(IF(capType ='매입',stlFee,0)) as payFee, SUM(IF(capType ='매입',stlFeeVat,0)) as payVat, SUM(IF(capType ='매입',1,0)) as payCnt,"
				+"		  	  SUM(IF(capType ='매입취소',amount,0)) as rfdAmt, SUM(IF(capType ='매입취소',stlFee,0)) as rfdFee, SUM(IF(capType ='매입취소',stlFeeVat,0)) as rfdVat, SUM(IF(capType ='매입취소',1,0)) as rfdCnt,"
				+"		  	  MAX(stlRate) as stlRate, stlType"
				+"	 	 FROM VW_TRX_CAP "
				+"  	where stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and stlStatus = '정산대기'"
				+"  	group by mchtId, stlDay, stlType"
				+"  	order by mchtId) X;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 가상계좌 정산예정일자의 자동정산 정산데이터 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getAutoVactSettleList(String stlDay, String stlType){
		String q = "SELECT A.*, B.bankCd,B.bankName,B.account,B.accntHolder FROM("
				+" SELECT mchtId,stlDay, MIN(trxDay) AS startDay, MAX(trxDay) AS endDay, "
				+" SUM(IF(trxType = '입금',amount,0)) AS payAmt, "
				+" SUM(IF(trxType = '입금',stlFee,0)) AS payFee, "
				+" SUM(IF(trxType = '입금',stlFeeVat,0)) AS payVat, "
				+" SUM(IF(trxType = '입금',1,0)) as payCnt, "
				+" SUM(IF(trxType = '취소',amount,0)) AS rfdAmt, "
				+" SUM(IF(trxType = '취소',stlFee,0)) AS rfdFee, "
				+" SUM(IF(trxType = '취소',stlFeeVat,0)) AS rfdVat, "
				+" SUM(IF(trxType = '취소',1,0)) as rfdCnt, "
				+" SUM(stlAmount) AS stlAmount, "
				+" stlType "
				+" FROM VW_VACT_TRX  "
				+"WHERE stlDay = '"+stlDay+"' AND stlId = '' AND stlType = '" + stlType + "' AND settleTarget = 'Y' "
				+"GROUP BY mchtId "
				+"ORDER BY mchtId) A  " 
				+" LEFT JOIN PG_MCHT_TAX B ON A.mchtId = B.mchtId AND B.taxStatus = '사용' ";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 가상계좌 정산예정일자의 자동정산 인증수수료 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getAutoVactSettleOrgFeeList(String stlDay, String stlType){
		String q = "SELECT a.mchtId, SUM(fee) AS fee, c.bankCd, c.bankName, c.account, c.accntHolder"
				+" FROM PG_VACT_AUTH a, PG_VACT_AUTH_DTL b, PG_MCHT_TAX c "
				+"WHERE a.authId = b.authId and a.mchtId = c.mchtId and b.stlDay = '"+stlDay+"' AND b.stlType = '" + stlType + "' AND b.stlStatus != '정산완료'"
				+"GROUP BY a.mchtId ";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 자동정산 데이터 (PG_SETTLE_AUTO) 테이블 INSERT
	 * @param data
	 * @return
	 */
	public boolean insertSettleAuto(SharedMap<String,Object> data){
		super.setTable("PG_SETTLE_AUTO");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	/**
	 * 자동정산 출금 데이터에서 출금완료하지 않은 대상건들 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getAutoPayOutList(String stlDay, String stlType){
		String q = "SELECT * "
				+"	  FROM PG_SETTLE_AUTO "
				+"   WHERE stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and status = '지급대기' and (resultCd is null or resultCd != '0000')"
				+"   order by regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 자동정산 출금 결과 업데이트
	 * @param stlId : 정산번호
	 * @param payOutDay : 출금일자
	 * @param payOutTime : 출금시간
	 * @param resCd : 출금 결과코드
	 * @param resMsg : 출금 결과메세지
	 * @return
	 */
	public boolean updateAutoPayOutRes(String stlId, String stlStatus, String payOutDay, String payOutTime, String bankCd, String bankName, String account, String accntHolder, String resCd, String resMsg, String sendCheck){
		String q = "UPDATE PG_SETTLE_AUTO "
				+ "    SET status='"+stlStatus+"', payOutDay='"+payOutDay+"',payOutTime='"+payOutTime+"',bankCd='"+bankCd+"',bankName='"+bankName+"',account='"+account+"',accntHolder='"+accntHolder+"',"
				+ "        resultCd='"+resCd+"', resultMsg='"+resMsg+"', sendCnt=sendCnt+1, sendCheck='" + sendCheck + "'"
				+ "	 WHERE stlId = '"+stlId+"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	
	/**
	 * 자동정산 출금 결과 매입데이터 업데이트
	 * @param stlId : 정산번호
	 * @param stlDay : 정산예정일자
	 * @param stlType : 정산타입
	 * @param mchtId : 가맹점아이디
	 * @param payOutDay : 출금일자
	 * @return
	 */
	public boolean updateAutoPayOutCap(String stlId, String stlDay, String stlType, String mchtId, String payOutDay, String stlStatus){
		String q = "UPDATE PG_TRX_CAP_DTL "
				+ "    SET stlId = '" + stlId +"', stlStatus = '" + stlStatus + "', risk = '', payOutDay='"+payOutDay+"'"
				+ "	 WHERE capId in "
				+ "		("
				+ "		 select B.capId "
				+ "		   from PG_TRX_CAP A, PG_TRX_CAP_DTL B" 
				+ "		  where A.capId = B.capId and A.mchtId = '" + mchtId + "' and B.stlDay = '" + stlDay + "' and B.stlType = '" + stlType + "' and B.stlStatus != '정산완료'"
				+ "		)"; 
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	public String getAutoSnedCnt(String stlId){
		super.setTable("PG_SETTLE_AUTO");
		super.setColumns("sendCnt");
		super.addWhere("stlId",stlId,eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("sendCnt");
	}
	
	/**
	 * 펌 출금 거래건 전문번호
	 * @param idx
	 * @return
	 */
	public String getSeqNo(String trxId){
		String q = "SELECT seqNo"
				+"    FROM PG_FIRM_TRX "
				+"	 WHERE filler = '" + trxId + "'";

		RecordSet rset = super.query(q);

		super.initRecord();
		return rset.getRowFirst().getString("seqNo");
	}
	
	/**
	 * 펌 출금 거래건 전문번호
	 * @param idx
	 * @return
	 */
	public String getSeqNo2(String trxId){
		String q = "SELECT seqNo"
				+"    FROM PG_FIRM_TRX "
				+"	 WHERE recordInfo = '" + trxId + "'";

		RecordSet rset = super.query(q);

		super.initRecord();
		return rset.getRowFirst().getString("seqNo");
	}
	
	public void setSettleToIdx(String stlId,String stlDay,String mchtId, String stlType){
		String q = "INSERT INTO PG_VACT_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,vactId,trxType"
				+"  FROM VW_VACT_TRX where stlDay ='" + stlDay + "' AND mchtId = '" + mchtId + "' AND stlType = '" + stlType + "' AND settleTarget = 'Y' ";
		logger.info("set PG_VACT_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateStlId(String stlId){
		String q = "UPDATE PG_VACT_TRX SET stlId='"+stlId+"' "
				+"	WHERE vactId in (SELECT vactId FROM PG_VACT_SETTLE_IDX WHERE stlId='"+stlId+"' )";
		logger.info("set PG_VACT_TRX : {}",super.update(q));
		super.initRecord();
	}
	
	/**
	 * 이체할 모계좌정보 검색
	 * @return
	 */
	public List<SharedMap<String,Object>> getMaccntList(){
		String q = "SELECT * "
				+"	  FROM PG_MACCNT "
				+"   WHERE sendYn = 'Y' "
				+"   order by mAccntNo;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 매입원장에서 정산예정일자의 정산데이터 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getDailySettleList(String hour,String stlDay, String stlType){
		String q = "SELECT capId,mchtId,stlDay,trxDay, capType, stlRate, stlType, "
				+"		   IF(capType ='매입',amount,0) as payAmt, IF(capType ='매입',stlFee,0) as payFee, IF(capType ='매입',stlFeeVat,0) as payVat, IF(capType ='매입',1,0) as payCnt,"
				+"		   IF(capType ='매입취소',amount,0) as rfdAmt, IF(capType ='매입취소',stlFee,0) as rfdFee, IF(capType ='매입취소',stlFeeVat,0) as rfdVat, IF(capType ='매입취소',1,0) as rfdCnt"
				+"	 FROM VW_TRX_CAP ";
		
				if("09".equals(hour)) {
					q = q + "  WHERE trxDay < '" + stlDay + "' and stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and stlStatus = '정산대기'";	
				}else {
					q = q + "  WHERE trxDay = '" + stlDay + "' and stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and stlStatus = '정산대기'";	
				}
				
				q = q + "  ORDER BY mchtId, regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 자동정산 출금 결과 매입데이터 업데이트
	 * @param stlDay : 정산예정일자
	 * @param stlType : 정산타입
	 * @param mchtId : 가맹점아이디
	 * @param payOutDay : 출금일자
	 * @return
	 */
	public boolean updateDailyPayOutCap(String stlDay, String stlType, String mchtId, String payOutDay, String stlStatus, String hour){
		String q = "UPDATE PG_TRX_CAP_DTL "
				+ "    SET stlStatus = '" + stlStatus + "', risk = '', payOutDay='"+payOutDay+"'"
				+ "	 WHERE capId in "
				+ "		("
				+ "		 select B.capId "
				+ "		   from PG_TRX_CAP A, PG_TRX_CAP_DTL B" ;
		
				if("10".equals(hour)) {
					q = q + "  WHERE A.capId = B.capId and A.mchtId = '" + mchtId + "' and A.trxDay < '" + stlDay + "' and B.stlDay = '" + stlDay + "' and B.stlType = '" + stlType + "' and B.stlStatus != '지급완료' )";	
				}else {
					q = q + "  WHERE A.capId = B.capId and A.mchtId = '" + mchtId + "' and A.trxDay = '" + stlDay + "' and B.stlDay = '" + stlDay + "' and B.stlType = '" + stlType + "' and B.stlStatus != '지급완료' )";	
				}
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 당일정산 정산번호 매입데이터 업데이트
	 * @param capId : 매입번호
	 * @param stlId : 정산번호
	 * @return
	 */
	public boolean updateDailyStlIdCap(String capId, String stlId){
		String q = "UPDATE PG_TRX_CAP_DTL "
				+ "    SET stlId = '" + stlId +"'"
				+ "	 WHERE stlStatus = '지급완료' and capId = '" + capId +"'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 실시간정산 원거래건 조회
	 * @param mchtId
	 * @param authCd
	 * @return
	 */
	public SharedMap<String, Object> getRootRealTimePay(String mchtId, String authCd){
		super.setTable("PG_REALTIME_PAYOUT");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("authCd", authCd, eq);
		super.addWhere("trxType", "0", eq);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getTrxPayOut(String trxId){
		super.setTable("PG_TRX_PAYOUT");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	
	/**
	 * 가상계좌 정산예정일자의 당일정산 정산데이터 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getDailyVactSettleList(String hour,String stlDay, String stlType){
		String q = "SELECT A.*, B.bankCd,B.bankName,B.account,B.accntHolder FROM("
				+" SELECT trxDay, mchtId, stlDay, "
				+" IF(trxType = '입금',amount,0) AS payAmt, "
				+" IF(trxType = '입금',stlFee,0) AS payFee, "
				+" IF(trxType = '입금',stlFeeVat,0) AS payVat, "
				+" IF(trxType = '입금',1,0) as payCnt, "
				+" IF(trxType = '취소',amount,0) AS rfdAmt, "
				+" IF(trxType = '취소',stlFee,0) AS rfdFee, "
				+" IF(trxType = '취소',stlFeeVat,0) AS rfdVat, "
				+" IF(trxType = '취소',1,0) as rfdCnt, "
				+" stlAmount, stlType "
				+" FROM VW_VACT_TRX  ";
				
				if("09".equals(hour)) {
					q = q + "  WHERE trxDay < '" + stlDay + "' and stlDay = '"+stlDay+"' AND stlId = '' AND stlType = '" + stlType + "' AND settleTarget = 'Y' ";	
				}else {
					q = q + "  WHERE trxDay = '" + stlDay + "' and stlDay = '"+stlDay+"' AND stlId = '' AND stlType = '" + stlType + "' AND settleTarget = 'Y' ";
				}
		
				q = q + " ORDER BY mchtId) A  " 
				+" LEFT JOIN PG_MCHT_TAX B ON A.mchtId = B.mchtId AND B.taxStatus = '사용' "
				+" ORDER BY mchtId, regDate;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	/**
	 * 자동정산 정산번호 조회
	 * @param mchtId
	 * @param stlDay
	 * @param stlType
	 * @return
	 */
	public String getStlId(String mchtId, String stlDay, String stlType) {
		super.setTable("PG_SETTLE_AUTO");
		super.setColumns("stlId");
		super.addWhere("mchtId", mchtId);
		super.addWhere("stlDay", stlDay);
		super.addWhere("stlType", stlType);
		super.addWhere("status", "지급대기");
		super.addWhere("payType", "V");
		
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() != 0) {
			return rset.getRow(0).getString("stlId");
		} else {
			return "";
		}
	}
	
	/**
	 * 자동정산 인증수수료 및 출금 예정금액 업데이트
	 * @param stlId
	 * @param fee
	 * @param feeVat
	 * @return
	 */
	public boolean updateAuthFee(String stlId, long fee, long feeVat){
		String q = "UPDATE PG_SETTLE_AUTO"
				+ "    SET authFee = '" + fee +"', authFeeVat = '" + feeVat +"',payOutAmount = payOutAmount - '" + (fee + feeVat) + "'"
				+ "	 WHERE stlId = '" + stlId + "'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 인증 테이블 정산번호 업데이트
	 * @param stlId
	 * @param mchtId
	 * @param stlDay
	 * @param stlType
	 * @return
	 */
	public boolean updateAuthStlId(String stlId, String mchtId, String stlDay, String stlType){
		String q = "UPDATE PG_VACT_AUTH a, PG_VACT_AUTH_DTL b"
				+ "    SET b.stlId = '" + stlId +"'"
				+ "	 WHERE a.mchtId = '" + mchtId + "' and b.stlDay = '" + stlDay + "' and b.stlType = '" + stlType + "'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 인증테이블 정산상태 완료
	 * @param stlId
	 * @return
	 */
	public boolean updateAuthStlStatus(String stlId){
		String q = "UPDATE PG_VACT_AUTH_DTL"
				+ "    SET stlStatus = '정산완료'"
				+ "	 WHERE stlId = '" + stlId + "'";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	public String getSettleDay(String today) {	
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days >= '"+today+"' AND status ='no' limit 1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
}
