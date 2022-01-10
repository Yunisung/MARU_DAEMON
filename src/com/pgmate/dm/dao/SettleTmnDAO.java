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
public class SettleTmnDAO extends SettleDAO implements ImplSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleTmnDAO.class );

	public SettleTmnDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String q = "SELECT A.tmnId as tmnId,A.mchtId AS mchtId,startDay,endDay,payAmt,payCnt,payFee,payVat,rfdAmt,rfdCnt,rfdFee,rfdVat,stlAmt,bankCd,bankName,account,accntHolder,B.rate as tmnRate"
				+" FROM ( "
				+"	SELECT tmnId, mchtId ,min(trxDay) startDay ,max(trxDay) endDay "
				+"  ,SUM(IF(capType ='매입', amount,0)) payAmt "
				+"  ,SUM(IF(capType ='매입', 1,0)) payCnt "
				+"  ,SUM(IF(capType ='매입', stlFee,0)) payFee "
				+"  ,SUM(IF(capType ='매입', stlFeeVat,0)) payVat "
				+"  ,SUM(IF(capType ='매입', 0,amount)) rfdAmt "
				+"  ,SUM(IF(capType ='매입', 0,1)) rfdCnt "
				+"  ,SUM(IF(capType ='매입', 0,stlFee)) rfdFee "
				+"  ,SUM(IF(capType ='매입', 0,stlFeeVat)) rfdVat "
				+"  ,SUM(stlAmount) stlAmt "
				+"  FROM VW_TRX_CAP_SUB where stlDay ='"+stlDay+"' and stlId ='' group by tmnId "
				+"  ) A , PG_MCHT_TMN_DTL B WHERE A.tmnId = B.tmnId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public void setSettleToIdx(String stlId,String stlDay,String tmnId){
		String q = "INSERT INTO PG_SETTLE_SUB_IDX "
				+"	SELECT '"+stlId+"' ,capId,capType as capStatus "
				+"  FROM VW_TRX_CAP_SUB where stlDay ='"+stlDay+"' and stlId ='' and tmnId ='"+tmnId+"' ";
		logger.info("set PG_SETTLE_SUB_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlId){
		String q = "UPDATE PG_SETTLE_SUB_IDX A JOIN PG_TRX_CAP_SUB B ON B.capId = A.capId "
						+ "SET B.stlId='"+stlId+"' WHERE A.stlId='"+stlId+"' ";
		/*String q = "UPDATE PG_TRX_CAP_SUB SET stlId='"+stlId+"' "
				+"	WHERE capId in (SELECT capId FROM PG_SETTLE_SUB_IDX WHERE stlId='"+stlId+"' )";*/
		logger.info("set PG_TRX_CAP_SUB : {}",super.update(q));
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
