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
public class SettleVactSalesDAO extends SettleVactDAO implements ImplVactSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleVactSalesDAO.class );

	public SettleVactSalesDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String month = stlDay.substring(0, 6);
		String q = "SELECT A.salesId as memberId,A.startDay,A.endDay,A.stlDay,A.payAmt,A.payCnt,A.payFee,A.payVat,A.rfdAmt,A.rfdCnt,A.rfdFee,A.rfdVat,A.stlFee,B.bankCd,B.bankName,B.account,B.accntHolder, '' as taxId FROM ( "
				+"	SELECT salesId ,MIN(trxDay) startDay ,MAX(trxDay) endDay, stlSalesDay as stlDay "
				+"  ,SUM(IF(trxType ='입금', amount,0)) payAmt "
				+"  ,SUM(IF(trxType ='입금', 1,0)) payCnt "
				+"  ,SUM(IF(trxType ='입금', stlSalesFee,0)) payFee "		
				+"  ,SUM(IF(trxType ='입금', stlSalesFeeVat,0)) payVat "
				+"  ,SUM(IF(trxType ='입금', 0,amount)) rfdAmt "
				+"  ,SUM(IF(trxType ='입금', 0,1)) rfdCnt "
				+"  ,SUM(IF(trxType ='입금', 0,stlSalesFee)) rfdFee "		
				+"  ,SUM(IF(trxType ='입금', 0,stlSalesFeeVat)) rfdVat "
				+"  ,SUM(stlSalesFee + stlSalesFeeVat) stlFee "								// 정산 금액 (수수료를 정산)
				+"  FROM VW_VACT_TRX WHERE stlSalesDay BETWEEN '"+month+"01' and '"+month+"31' AND stlSalesId = '' AND settleTarget = 'Y' group BY salesId, stlSalesDay "
				+"  ) A, PG_MAM_SALES_MNG B WHERE A.salesId = B.salesId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public boolean insertVactSettle(SharedMap<String,Object> data){
		super.setTable("PG_VACT_SETTLE");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	public void setSettleToIdx(String stlId,String stlDay,String salesId){
		String q = "INSERT INTO PG_VACT_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,vactId,trxType"
				+"  FROM VW_VACT_TRX where stlSalesDay ='"+stlDay+"' and salesId = '"+salesId+"' and settleTarget = 'Y'";
		logger.info("set PG_VACT_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlId){
		String q = "UPDATE PG_VACT_TRX SET stlSalesId='"+stlId+"' "
				+"	WHERE vactId in (SELECT vactId FROM PG_VACT_SETTLE_IDX WHERE stlId='"+stlId+"' )";
		logger.info("set PG_VACT_TRX : {}",super.update(q));
		super.initRecord();
	}
}
