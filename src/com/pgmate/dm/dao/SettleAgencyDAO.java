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
public class SettleAgencyDAO extends SettleDAO implements ImplSettle {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettleAgencyDAO.class );

	public SettleAgencyDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String month = stlDay.substring(0, 6);
		String q = "SELECT A.agencyId as memberId,startDay,endDay,stlDay,payAmt,payCnt,payFee,payVat,rfdAmt,rfdCnt,rfdFee,rfdVat,stlFee,stlDiffFee,stlFee + stlDiffFee AS stlAmt,bankCd,bankName,account,accntHolder, '' as taxId FROM ( "
				+"	SELECT agencyId ,min(trxDay) startDay ,max(trxDay) endDay, stlAgencyDay as stlDay "
				+"  ,SUM(IF(capType ='매입', amount,0)) payAmt "
				+"  ,SUM(IF(capType ='매입', 1,0)) payCnt "
				+"  ,SUM(IF(capType ='매입', stlAgencyFee,0)) payFee "		// 총 수수료(증가)
				+"  ,SUM(IF(capType ='매입', stlFeeVat,0)) payVat "
				+"  ,SUM(IF(capType ='매입', 0,amount)) rfdAmt "
				+"  ,SUM(IF(capType ='매입', 0,1)) rfdCnt "
				+"  ,SUM(IF(capType ='매입', 0,stlAgencyFee)) rfdFee "		// 총 취소된 수수료(차감)
				+"  ,SUM(IF(capType ='매입', 0,stlFeeVat)) rfdVat "
				+"  ,SUM(stlAgencyFee) stlFee "								// 정산 금액 (수수료를 정산)
				+"  ,SUM(stlDiffAgencyFee) stlDiffFee "						// 영중소 차액정산 수수료 정산 금액
				+"  FROM VW_TRX_CAP_LIST where stlAgencyDay between '"+month+"01' and '"+month+"31' and stlAgencyId ='' group by agencyId, stlAgencyDay "
				+"  ) A , PG_MAM_AGENCY_MNG  B WHERE A.agencyId = B.agencyId  ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public void setSettleToIdx(String stlAgencyId,String stlAgencyDay,String id){
		String q = "INSERT INTO PG_SETTLE_IDX  "
				+"	SELECT '"+stlAgencyId+"' ,capId, capType as capStatus "
				+"  FROM VW_TRX_CAP_LIST where stlAgencyDay ='"+stlAgencyDay+"' and stlAgencyId ='' and agencyId ='"+id+"' ";
		logger.info("set PG_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlAgencyId){
		String q = "UPDATE PG_SETTLE_IDX A JOIN PG_TRX_CAP_DTL B ON B.capId = A.capId "
						+ "SET B.stlAgencyId='"+stlAgencyId+"' WHERE A.stlId='"+stlAgencyId+"' ";
		/*String q = "UPDATE PG_TRX_CAP_DTL SET stlAgencyId='"+stlAgencyId+"' "
				+"	WHERE capId in (SELECT capId FROM PG_SETTLE_IDX WHERE stlId='"+stlAgencyId+"' )";*/
		logger.info("set PG_TRX_CAP_DTL : {}",super.update(q));
		super.initRecord();
	}
}
