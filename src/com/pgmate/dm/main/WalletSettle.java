package com.pgmate.dm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.WalletSettleDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class WalletSettle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.WalletSettle.class );
	
	public WalletSettle() {
			execute();
	}
	
	public void execute(){
		
		
		
			WalletSettleDAO stlDAO = new WalletSettleDAO();
			if(stlDAO != null){
				logger.info("= == === === === === 월렛 정산 시작 === === === === == =");
					
				List<SharedMap<String,Object>> settleList = stlDAO.getSettleList();
				logger.info("MAKE SETTLE COUNT : {}", settleList.size());
				for(SharedMap<String,Object> data : settleList){
					String stlId = stlDAO.getSettleId();
					logger.info("STL_ID  	: {}",stlId);
					logger.info("STL_PTN_AMT 	: {}",data.getLong("stlAmt"));
					
					data.put("stlId", stlId);
					data.put("status", "지급대기");
					data.put("stlDay", stlDAO.getStlDay());
					data.put("regId", "SYSTEM");
					data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					data.remove("_idx");
					
					if(stlDAO.insertSettle(data)){
						String ptnId = data.getString("ptnId");
						stlDAO.setSettleToIdx(stlId,ptnId,data.getString("startDay"),data.getString("endDay"));
					}
				}
				
				logger.info("= == === === === === 월렛 정산 종료 === === === === == =");
				
				logger.info("= == === === === === 배달대행 사업자 월렛 부가세 정산 시작 === === === === == =");
				
				List<SharedMap<String,Object>> taxList = stlDAO.getTaxList();
				logger.info("MAKE TAX COUNT : {}", taxList.size());
				for(SharedMap<String,Object> data : taxList){
					String taxId = stlDAO.getTaxId();
					logger.info("TAX_ID  	: {}",taxId);
					
					data.put("taxId", taxId);
					data.put("regId", "SYSTEM");
					data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					data.remove("_idx");
					
					stlDAO.insertTax(data);
				}
				logger.info("= == === === === === 배달대행 사업자 월렛 부가세 정산 종료 === === === === == =");

				logger.info("= == === === === === 배달대행 개인 월렛 원천징수액 정산 시작 === === === === == =");
				
				List<SharedMap<String,Object>> withholdingList = stlDAO.getWithholdingList();
				logger.info("MAKE TAX COUNT : {}", withholdingList.size());
				for(SharedMap<String,Object> data : withholdingList){
					String taxId = stlDAO.getTaxId();
					logger.info("TAX_ID  	: {}",taxId);
					
					data.put("taxId", taxId);
					data.put("regId", "SYSTEM");
					data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					data.remove("_idx");
					
					stlDAO.insertWithholding(data);
				}
				logger.info("= == === === === === 배달대행 개인 월렛 원천징수액 정산 종료 === === === === == =");
				
				logger.info("= == === === === === 분리정산 가맹점 월렛 PG정산 수수료 부가세 정산 시작 === === === === == =");
				
				List<SharedMap<String,Object>> settleShopTaxList = stlDAO.getShopVatList();
				logger.info("MAKE TAX COUNT : {}", settleShopTaxList.size());
				for(SharedMap<String,Object> data : settleShopTaxList){
					String taxId = stlDAO.getTaxId();
					logger.info("TAX_ID  	: {}",taxId);
					
					data.put("taxId", taxId);
					data.put("regId", "SYSTEM");
					data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					data.remove("_idx");
					
					stlDAO.insertSettleVat(data);
				}
				logger.info("= == === === === === 분리정산 가맹점 월렛 PG정산 수수료 부가세 정산 종료 === === === === == =");

				logger.info("= == === === === === 분리정산 총판, 딜러 월렛 지급수수료 부가세 정산 시작 === === === === == =");
				
				List<SharedMap<String,Object>> settleTaxList = stlDAO.getSettleVatList();
				logger.info("MAKE TAX COUNT : {}", settleTaxList.size());
				for(SharedMap<String,Object> data : settleTaxList){
					String taxId = stlDAO.getTaxId();
					logger.info("TAX_ID  	: {}",taxId);
					
					data.put("taxId", taxId);
					data.put("regId", "SYSTEM");
					data.put("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					data.remove("_idx");
					
					stlDAO.insertSettleVat(data);
				}
				logger.info("= == === === === === 분리정산 총판, 딜러 월렛 지급수수료 부가세 정산 종료 === === === === == =");
			}
		
	}
		
	
	public static void main(String[] args){
		new WalletSettle();
	}

}
