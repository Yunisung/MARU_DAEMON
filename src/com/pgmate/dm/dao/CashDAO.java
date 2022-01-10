package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.popbill.api.cashbill.CashbillInfo;

/**
 * @author Administrator
 *
 */
public class CashDAO extends DAO {

	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.CashDAO.class );
	/**
	 * 
	 */
	public CashDAO() {
		// TODO Auto-generated constructor stub
	}

	public List<SharedMap<String,Object>> getList(){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM PG_TRX_CASH A LEFT OUTER JOIN PG_TRX_CASH_DTL B");
		sb.append(" ON A.trxId = B.trxId  WHERE B.isNew ='N' AND B.resultCd in ('300','301','302','303','400','401','402','403')"
				+ " AND A.regDay > DATE_FORMAT(date_add(now(), interval -7 day),'%Y%m%d')");
		sb.append(" order by A.trxId asc");
		return super.query(sb.toString()).getRows();
		
	}
	
	
	public void insertCashDtl(CashbillInfo cbi){
		updateCashDtl(cbi.getMgtKey());
		super.setTable("PG_TRX_CASH_DTL");
		super.setRecord("trxId", cbi.getMgtKey());
		super.setRecord("isNew", "N");
		if(cbi.getStateCode() == 301 || cbi.getStateCode() == 401 ){
			super.setRecord("progress","전송전");
		}else if(cbi.getStateCode() == 302 || cbi.getStateCode() == 402 ){
			super.setRecord("progress","전송대기");
		}else if(cbi.getStateCode() == 303 || cbi.getStateCode() == 403 ){
			super.setRecord("progress","전송중");
		}else if(cbi.getStateCode() == 304 || cbi.getStateCode() == 404 ){
			super.setRecord("progress","전송완료");
		}else if(cbi.getStateCode() == 305 || cbi.getStateCode() == 405 ){
			super.setRecord("progress","전송오류");
		}
		super.setRecord("resultCd", CommonUtil.toString(cbi.getStateCode()));
		if(CommonUtil.isNullOrSpace(cbi.getNtsresultCode())){
			super.setRecord("resultMsg", cbi.getStateDT());
		}else{
			super.setRecord("resultMsg", cbi.getNtsresultDT());
		}
		super.setRecord("ntsCd", CommonUtil.nToB(cbi.getNtsresultCode()));
		super.setRecord("ntsMsg",  CommonUtil.nToB(cbi.getNtsresultMessage()));
		
		logger.info("insert cash dtl , trxId : {},{}",cbi.getMgtKey(),super.insert());
		
		super.initRecord();
		if(!CommonUtil.isNullOrSpace(cbi.getConfirmNum())){
			updateCash(cbi.getMgtKey(),cbi.getConfirmNum());
		}
	}
	
	
	private void updateCashDtl(String trxId){
		logger.info("update cash dtl , trxId : {}",trxId);
		super.update("UPDATE PG_TRX_CASH_DTL set isNew = '' WHERE trxId ='"+trxId+"'");
		super.initRecord();
	}
	
	private void updateCash(String trxId,String authCd){
		logger.info("update cash trxId : {}, authCd : {}",trxId,authCd);
		super.update("UPDATE PG_TRX_CASH set authCd = '"+authCd+"' WHERE trxId ='"+trxId+"'");
		super.initRecord();
	}

}
