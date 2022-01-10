package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class SettleVactMchtDAO extends SettleDAO implements ImplSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleVactMchtDAO.class );

	public SettleVactMchtDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String q = "SELECT A.*, (A.payFee + A.payFeeVat) +(A.rfdFee + A.rfdFeeVat) - A.distFee - A.agencyFee - A.salesFee - A.vanFee AS benefit, B.bankCd,B.bankName,B.account,B.accntHolder,B.taxId FROM( "
				+" SELECT mchtId, MIN(trxDay) AS startDay, MAX(trxDay) AS endDay, stlType, "
				+" SUM(IF(trxType = '입금',amount,0)) AS payAmt, "
				+" SUM(IF(trxType = '입금',stlFee,0)) AS payFee, "
				+" SUM(IF(trxType = '입금',stlFeeVat,0)) AS payFeeVat, "
				+" SUM(IF(trxType ='입금',1,0)) as payCnt, "
				+" SUM(IF(trxType = '취소',amount,0)) AS rfdAmt, "
				+" SUM(IF(trxType = '취소',stlFee,0)) AS rfdFee, "
				+" SUM(IF(trxType = '취소',stlFeeVat,0)) AS rfdFeeVat, "
				+" SUM(IF(trxType ='취소',1,0)) as rfdCnt, "
				+" SUM(stlDistFee)+SUM(stlDistFeeVat) AS distFee, "
				+" SUM(stlAgencyFee)+SUM(stlAgencyFeeVat) AS agencyFee, "
				+" SUM(stlSalesFee)+SUM(stlSalesFeeVat) AS salesFee, "
				+" SUM(vanFee) AS vanFee, "
				+" SUM(stlAmount) AS stlAmount "
				+" FROM VW_VACT_TRX WHERE stlDay = '"+stlDay+"' AND stlId = '' AND stlType != 'D+0' AND stlType !='A+1' AND stlType != 'C+0' AND settleTarget = 'Y' GROUP BY mchtId) A  " 
				+" LEFT JOIN PG_MCHT_TAX B ON A.mchtId = B.mchtId AND B.taxStatus = '사용' ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public boolean insertVactSettleMcht(SharedMap<String,Object> data){
		super.setTable("PG_VACT_SETTLE_MCHT");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	public void setSettleToIdx(String stlId,String stlDay,String mchtId){
		String q = "INSERT INTO PG_VACT_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,vactId,trxType"
				+"  FROM VW_VACT_TRX where stlDay ='"+stlDay+"' and mchtId = '"+mchtId+"' AND stlType != 'D+0' AND stlType !='A+1' AND stlType != 'C+0' AND settleTarget = 'Y' ";
		logger.info("set PG_VACT_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlId){
		String q = "UPDATE PG_VACT_TRX SET stlId='"+stlId+"' "
				+"	WHERE vactId in (SELECT vactId FROM PG_VACT_SETTLE_IDX WHERE stlId='"+stlId+"' )";
		logger.info("set PG_VACT_TRX : {}",super.update(q));
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
}
