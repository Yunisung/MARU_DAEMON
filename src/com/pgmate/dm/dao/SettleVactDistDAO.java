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
public class SettleVactDistDAO extends SettleVactDAO implements ImplVactSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleVactDistDAO.class );

	public SettleVactDistDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String month = stlDay.substring(0, 6);
		String q = "SELECT A.distId as memberId,A.startDay,A.endDay,A.stlDay,A.payAmt,A.payCnt,A.payFee,A.payVat,A.rfdAmt,A.rfdCnt,A.rfdFee,A.rfdVat,A.stlFee,B.bankCd,B.bankName,B.account,B.accntHolder, '' as taxId FROM ( "
				+"	SELECT distId ,MIN(trxDay) startDay ,MAX(trxDay) endDay, stlDistDay as stlDay "
				+"  ,SUM(IF(trxType ='입금', amount,0)) payAmt "
				+"  ,SUM(IF(trxType ='입금', 1,0)) payCnt "
				+"  ,SUM(IF(trxType ='입금', stlDistFee,0)) payFee "		
				+"  ,SUM(IF(trxType ='입금', stlDistFeeVat,0)) payVat "
				+"  ,SUM(IF(trxType ='입금', 0,amount)) rfdAmt "
				+"  ,SUM(IF(trxType ='입금', 0,1)) rfdCnt "
				+"  ,SUM(IF(trxType ='입금', 0,stlDistFee)) rfdFee "		
				+"  ,SUM(IF(trxType ='입금', 0,stlDistFeeVat)) rfdVat "
				+"  ,SUM(stlDistFee + stlDistFeeVat) stlFee "								// 정산 금액 (수수료를 정산)
				+"  FROM VW_VACT_TRX WHERE stlDistDay BETWEEN '"+month+"01' and '"+month+"31' AND stlDistId = '' AND settleTarget = 'Y' group BY distId, stlDistDay "
				+"  ) A, PG_MAM_DIST_MNG B WHERE A.distId = B.distId  ";
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
	
	public void setSettleToIdx(String stlId,String stlDay,String distId){
		String q = "INSERT INTO PG_VACT_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,vactId,trxType"
				+"  FROM VW_VACT_TRX where stlDistDay ='"+stlDay+"' and distId = '"+distId+"' and settleTarget = 'Y'";
		logger.info("set PG_VACT_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlId){
		String q = "UPDATE PG_VACT_TRX SET stlDistId='"+stlId+"' "
				+"	WHERE vactId in (SELECT vactId FROM PG_VACT_SETTLE_IDX WHERE stlId='"+stlId+"' )";
		logger.info("set PG_VACT_TRX : {}",super.update(q));
		super.initRecord();
	}
}
