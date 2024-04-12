package com.pgmate.dm.dao;

import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Administrator
 *
 */
public class SettleRentSalesDAO extends SettleRentDAO implements ImplRentSettle {

	private static Logger logger = LoggerFactory.getLogger( SettleRentSalesDAO.class );

	public SettleRentSalesDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String month = stlDay.substring(0, 6);
		String q = "SELECT A.salesId as memberId,startDay,endDay,stlDay,payAmt,payCnt,payFee,payVat,rfdAmt,rfdCnt,rfdFee,rfdVat,stlFee,stlDiffFee, stlFee+ stlDiffFee AS stlAmt,bankCd,bankName,account,accntHolder, '' as taxId FROM ( "
				+"	SELECT salesId ,min(trxDay) startDay ,max(trxDay) endDay, stlSalesDay as stlDay "
				+"  ,SUM(IF(capType ='매입', amount,0)) payAmt "
				+"  ,SUM(IF(capType ='매입', 1,0)) payCnt "
				+"  ,SUM(IF(capType ='매입', stlSalesFee,0)) payFee "		// 총 수수료(증가)
				+"  ,SUM(IF(capType ='매입', stlFeeVat,0)) payVat "
				+"  ,SUM(IF(capType ='매입', 0,amount)) rfdAmt "
				+"  ,SUM(IF(capType ='매입', 0,1)) rfdCnt "
				+"  ,SUM(IF(capType ='매입', 0,stlSalesFee)) rfdFee "		// 총 취소된 수수료(차감)
				+"  ,SUM(IF(capType ='매입', 0,stlFeeVat)) rfdVat "
				+"  ,SUM(stlSalesFee) stlFee "								// 정산 금액 (수수료를 정산)
				+"  ,SUM(stlDiffSalesFee) stlDiffFee "						// 영중소 차액정산 수수료 정산 금액
				+"  FROM VW_TRX_CAP_LIST where payOutDay between '"+month+"01' and '"+month+"31' and stlSalesId ='' and serviceType='월세앱' group by salesId, SUBSTRING(payOutDay,1,6) "
				+"  ) A , PG_MAM_SALES_MNG  B WHERE A.salesId = B.salesId  ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public void setSettleToIdx(String stlSalesId,String stlSalesDay,String id){
		String q = "INSERT INTO PG_RENT_SETTLE_IDX  "
				+"	SELECT '"+stlSalesId+"' ,capId, capType as capStatus "
				+"  FROM VW_TRX_CAP_LIST where stlSalesDay ='"+stlSalesDay+"' and stlSalesId ='' and salesId ='"+id+"' ";
		logger.info("set PG_RENT_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlSalesId){
		String q = "UPDATE PG_RENT_SETTLE_IDX A JOIN PG_TRX_CAP_DTL B ON B.capId = A.capId "
						+ "SET B.stlSalesId='"+stlSalesId+"' WHERE A.stlId='"+stlSalesId+"' ";
		/*String q = "UPDATE PG_TRX_CAP_DTL SET stlSalesId='"+stlSalesId+"' "
				+"	WHERE capId in (SELECT capId FROM PG_SETTLE_IDX WHERE stlId='"+stlSalesId+"' )";*/
		logger.info("set PG_TRX_CAP_DTL : {}",super.update(q));
		super.initRecord();
	}

	@Override
	public boolean insertRentSettle(SharedMap<String, Object> data) {
		super.setTable("PG_RENT_SETTLE");

		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}

		boolean inserted =  super.insert();

		super.initRecord();
		return inserted;
	}
}
