package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class SettleMchtDAO extends SettleDAO implements ImplSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleMchtDAO.class );

	public SettleMchtDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String q = "SELECT A.mchtId as memberId,startDay,endDay,payAmt,payCnt,payFee,payVat,rfdAmt,rfdCnt,rfdFee,rfdVat,stlAmt,bankCd,bankName,account,accntHolder,A.taxId as taxId"
				+" FROM ( "
				+"	SELECT mchtId, taxId ,min(trxDay) startDay ,max(trxDay) endDay "
				+"  ,SUM(IF(capType ='매입', amount,0)) payAmt "
				+"  ,SUM(IF(capType ='매입', 1,0)) payCnt "
				+"  ,SUM(IF(capType ='매입', stlFee,0)) payFee "
				+"  ,SUM(IF(capType ='매입', stlFeeVat,0)) payVat "
				+"  ,SUM(IF(capType ='매입', 0,amount)) rfdAmt "
				+"  ,SUM(IF(capType ='매입', 0,1)) rfdCnt "
				+"  ,SUM(IF(capType ='매입', 0,stlFee)) rfdFee "
				+"  ,SUM(IF(capType ='매입', 0,stlFeeVat)) rfdVat "
				+"  ,SUM(stlAmount) stlAmt "
				+"  FROM VW_TRX_CAP where stlDay ='"+stlDay+"' and stlId ='' group by mchtId, taxId "
				+"  ) A , PG_MCHT_TAX B WHERE A.taxId = B.taxId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public void setSettleToIdx(String stlId,String stlDay,String taxId){
		String q = "INSERT INTO PG_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,capId,capType as capStatus "
				+"  FROM VW_TRX_CAP where stlDay ='"+stlDay+"' and stlId ='' and taxId ='"+taxId+"' ";
		logger.info("set PG_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlId){
		String q = "UPDATE PG_TRX_CAP_DTL SET stlId='"+stlId+"' "
				+"	WHERE capId in (SELECT capId FROM PG_SETTLE_IDX WHERE stlId='"+stlId+"' )";
		logger.info("set PG_TRX_CAP_DTL : {}",super.update(q));
		super.initRecord();
	}
	
	public boolean isRestDay(String stlDay){
		boolean bRes = false;
		
		String q = "SELECT status FROM PG_CODE_HOLIDAY WHERE days = '" + stlDay + "'";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		SharedMap<String,Object> res = rset.getRow(0);
		if(res != null && res.size() > 0) {
			bRes = res.getString("status").equals("yes") ? true : false; 
		}
		
		return bRes;
	}
	
	//가맹점 월 정산내역 이메일 전송 =======================================================
	/*
	 * 전송날짜 이전 내역 삭제
	 */
	/*public void deleteBefore(String getSendDay){
		String q = "DELETE FROM PG_NOTICE_SETTLE where regDay < '" + getSendDay + "'";
		
		super.update(q);
		super.initRecord();
	}*/
	
	/*
	 * 가맹점 전송 여부 확인
	 */
	public String checkSendList(String mchtId) {
		super.setTable("PG_NOTICE_SETTLE");
		super.setColumns("mchtId");
		super.addWhere("lower(mchtId)",mchtId,eq);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0){
			return "";
		}else{
			return rset.getRow(0).getString("mchtId");
		}
	}
	
	/*
	 * 이메일 전송할 가맹점 LIST
	 */
	public List<SharedMap<String,Object>> getSendMchtId(String startDay, String endDay){
		String q = "SELECT A.*	" + 
				"	  FROM	" + 
				"		(" + 
				"			SELECT DISTINCT A.mchtId AS mchtId	" + 
				"		  	  FROM VW_SETTLE_MCHT A JOIN PG_MCHT_SVC B ON A.mchtId = B.mchtId JOIN PG_MCHT_TAX C ON A.mchtId = C.mchtId	" + 
				"		 	 WHERE stlDay >= '" + startDay + "' and stlDay <= '" + endDay + "' AND emailStatus = '사용' AND (email IS NOT NULL OR email != '')	" + 
				"		) A LEFT join	" + 
				"		(" + 
				"			SELECT mchtId	" + 
				"			  FROM PG_NOTICE_SETTLE	" + 
				"		) B	" + 
				"		ON A.mchtId = B.mchtId	" + 
				"	WHERE B.mchtId IS NULL LIMIT 90;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	/*
	 * 전송할 가맹점 email address SELECT
	 */
	public String getEmail(String id){
		String q = "SELECT email FROM PG_MCHT_TAX WHERE mchtId = '" + id + "'";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRowFirst().getString("email");
	}
	
	/*
	 * 전송할 가맹점 nick SELECT
	 */
	public String getNick(String id){
		String q = "SELECT nick FROM PG_MCHT WHERE mchtId = '" + id + "'";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRowFirst().getString("nick");
	}
	
	/*
	 * 정산내역 전송 이력 INSERT 
	 */
	public boolean insertSettleList(SharedMap<String, Object> map) {
		super.setTable("PG_NOTICE_SETTLE");
		super.setRecord("mchtId"		, map.get("mchtId"));
		super.setRecord("name"			, map.get("name"));
		super.setRecord("email"			, map.get("email"));
		super.setRecord("startDay"		, map.get("startDay"));
		super.setRecord("endDay"		, map.get("endDay"));
		super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		
		boolean inserted = super.insert();
		logger.info("INSERT insertSettleList : {}, setParentId : {}", inserted, map.getString("mchtId"));

		super.initRecord();
		return inserted;
	}
	//가맹점 월 정산내역 이메일 전송 =======================================================
}
