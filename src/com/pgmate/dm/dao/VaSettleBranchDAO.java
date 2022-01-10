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
public class VaSettleBranchDAO extends SettleVactDAO implements ImplVaSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.VaSettleBranchDAO.class );

	public VaSettleBranchDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String month = stlDay.substring(0, 6);
		String q = "SELECT A.branchId as memberId,A.startDay,A.endDay,A.stlDay,A.payAmt,A.payCnt,A.payFee,A.withdrawAmt,A.withdrawCnt,A.withdrawFee,A.stlFee,B.bankCd,B.bankName,B.account,B.accntHolder FROM ( "
				+"	SELECT branchId ,MIN(trxDay) startDay ,MAX(trxDay) endDay, stlBranchDay as stlDay "
				+"  ,SUM(IF(trxType ='입금', amount,0)) payAmt "
				+"  ,SUM(IF(trxType ='입금', 1,0)) payCnt "
				+"  ,SUM(IF(trxType ='입금', stlBranchFee,0)) payFee "		
				+"  ,SUM(IF(trxType ='출금', amount,0)) withdrawAmt "
				+"  ,SUM(IF(trxType ='출금', 1,0)) withdrawCnt "
				+"  ,SUM(IF(trxType ='출금', stlBranchFee,0)) withdrawFee "
				+"  ,SUM(stlBranchFee) stlFee "		
				+"  FROM VW_VA_TRX WHERE stlBranchDay BETWEEN '"+month+"01' and '"+month+"31' AND stlBranchId = '' group BY branchId, stlBranchDay "
				+"  ) A, VA_MAM_BRANCH B WHERE A.branchId = B.branchId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public boolean insertVaSettle(SharedMap<String,Object> data){
		super.setTable("VA_SETTLE");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	public void setSettleToIdx(String stlId,String stlDay,String branchId){
		String q = "INSERT INTO VA_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,trxId,trxType"
				+"  FROM VW_VA_TRX where stlBranchDay ='"+stlDay+"' and branchId = '"+branchId+"'";
		logger.info("set VA_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrx(String stlId){
		String q = "UPDATE VA_TRX_DTL SET stlBranchId='"+stlId+"' "
				+"	WHERE trxId in (SELECT trxId FROM VA_SETTLE_IDX WHERE stlId='"+stlId+"' )";
		logger.info("set VA_TRX_DTL : {}",super.update(q));
		super.initRecord();
	}
}
