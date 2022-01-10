package com.pgmate.dm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.CashDAO;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.popbill.api.PopbillException;
import com.popbill.api.cashbill.CashbillInfo;
import com.popbill.api.cashbill.CashbillServiceImp;

/**
 * @author Administrator
 *
 */
public class CashDaemon{

	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.CashDaemon.class );
	
	
	private CashDAO cashDAO = null;
	
	public CashDaemon() {
		cashDAO = new CashDAO();
	}
	

	
	
	public void getList(){
		CashbillServiceImp cbs = new CashbillServiceImp();
		cbs.setLinkID("CYREXPAY");
		cbs.setSecretKey("ctf4uzjhR/4AvpdRpPA3EP/tQGDPvRW1pFX9KK2D3QI=");
		cbs.setTest(false);
		//SELECT * FROM PG_TRX_CASH A LEFT OUTER JOIN PG_TRX_CASH_DTL B
		//ON A.trxId = B.trxId and B.isNew ='N' AND B.resultCd in ('300','301','302','303')
		//INFO  확인 후 결과 처리.....
		
		
		List<SharedMap<String,Object>> list = cashDAO.getList();
		logger.info("list : {}",list.size());
		
		
		for(SharedMap<String,Object> data :  list){
			logger.info("issuer : {}, trxId : {}",data.getString("issuer"),data.getString("trxId"));
			
				long code = 1;
				String message = "";
				CashbillInfo cashbillInfo = null;
				try {
					cashbillInfo = cbs.getInfo(data.getString("issuer"), data.getString("trxId"));
					
					logger.info(GsonUtil.toJson(cashbillInfo, true, "yyyyMMddHHmmss"));
					cashDAO.insertCashDtl(cashbillInfo);
				} catch (PopbillException pe) {
					code = pe.getCode();
					message = pe.getMessage();
				}
				logger.info("code : {},message :{}",code,message);
				
			
			
			
			
		}
		
		
	
		
	}
	

	public static void main(String[] args) {
		CashDaemon c = new CashDaemon();
		c.getList();
	}

}
