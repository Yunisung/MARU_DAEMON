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
public class SettleRentDistDAO extends SettleRentDAO implements ImplRentSettle {

	private static Logger logger = LoggerFactory.getLogger( SettleRentDistDAO.class );

	public SettleRentDistDAO() {
		super.setDebug(false);
	}
	
	public List<SharedMap<String,Object>> getSettleList(String stlDay){
		String month = stlDay.substring(0, 6);
		String q = "SELECT A.distId as memberId,startDay,endDay,stlDay,payAmt,payCnt,payFee,payVat,rfdAmt,rfdCnt,rfdFee,rfdVat,stlFee,stlDiffFee,stlFee + stlDiffFee AS stlAmt,bankCd,bankName,account,accntHolder, '' as taxId FROM ( "
				+"	SELECT distId ,min(trxDay) startDay ,max(trxDay) endDay, stlDistDay as stlDay "
				+"  ,SUM(IF(capType ='매입', amount,0)) payAmt "
				+"  ,SUM(IF(capType ='매입', 1,0)) payCnt "
				+"  ,SUM(IF(capType ='매입', stlDistFee,0)) payFee "		// 총 수수료(증가)
				+"  ,SUM(IF(capType ='매입', stlFeeVat,0)) payVat "
				+"  ,SUM(IF(capType ='매입', 0,amount)) rfdAmt "
				+"  ,SUM(IF(capType ='매입', 0,1)) rfdCnt "
				+"  ,SUM(IF(capType ='매입', 0,stlDistFee)) rfdFee "		// 총 취소된 수수료(차감)
				+"  ,SUM(IF(capType ='매입', 0,stlFeeVat)) rfdVat "
				+"  ,SUM(stlDistFee) stlFee "								// 정산 금액 (수수료를 정산)
				+"  ,SUM(stlDiffDistFee) stlDiffFee "						// 차액정산 금액
				+"  FROM VW_TRX_CAP_LIST WHERE stlStatus='정산완료' AND stlDistDay between '"+month+"01' and '"+month+"31' and stlDistId ='' and serviceType='월세앱' group by distId, stlDistDay "
				+"  ) A , PG_MAM_DIST_MNG  B WHERE A.distId = B.distId  ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public void setSettleToIdx(String stlDistId,String stlDistDay,String id){
		String q = "INSERT INTO PG_RENT_SETTLE_IDX  "
				+"	SELECT '"+stlDistId+"' ,capId, capType as capStatus "
				+"  FROM VW_TRX_CAP_LIST where stlDistDay ='"+stlDistDay+"' and stlDistId ='' and distId ='"+id+"' ";
		logger.info("set PG_RENT_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public void updateTrxCap(String stlDistId){
		String q = "UPDATE PG_RENT_SETTLE_IDX A JOIN PG_TRX_CAP_DTL B ON B.capId = A.capId "
						+ "SET B.stlDistId='"+stlDistId+"' WHERE A.stlId='"+stlDistId+"' ";
		/*String q = "UPDATE PG_TRX_CAP_DTL SET stlDistId='"+stlDistId+"' "
					+"	WHERE capId in (SELECT capId FROM PG_SETTLE_IDX WHERE stlId='"+stlDistId+"' )";*/
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
