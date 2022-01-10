package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class WalletSettleDAO extends DAO{

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.WalletSettleDAO.class );

	public WalletSettleDAO() {
	}
	/*
	public List<SharedMap<String,Object>> getSettleList(){
		String q = "SELECT A.ptnId, B.name AS ptnName, B.withholding, MIN(A.regDay) AS startDay, MAX(A.regDay) as endDay, "
				+ "COUNT(IF(A.trxType = '입금' && A.trxUnit = '카드',1,null)) AS chargeCardCnt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드' , A.grossAmount,0)) AS chargeCardAmt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드' , A.tax,0)) AS chargeCardTax, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.netFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.netFeeVat,0)) AS chargeCardNetFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.ptnFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.ptnFeeVat,0)) AS chargeCardPtnFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.vanFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.vanFeeVat,0)) AS chargeCardPgFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.benefit,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.benefitVat,0)) AS chargeCardBenefit, "
				+ "COUNT(IF(A.trxType = '입금' && A.trxUnit = '가상계좌',1,null)) AS chargeVactCnt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌' , A.grossAmount,0)) AS chargeVactAmt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌' , A.tax,0)) AS chargeVactTax, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.netFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.netFeeVat,0)) AS chargeVactNetFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.ptnFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.ptnFeeVat,0)) AS chargeVactPtnFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.bankFee,0)) AS chargeVactBankFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.benefit,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.benefitVat,0)) AS chargeVactBenefit, "
				+ "COUNT(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',1,null)) AS withdrawCnt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.grossAmount,0)) AS withdrawAmt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',netFee,0)) + SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.netFeeVat,0)) AS withdrawNetFee, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.ptnFee,0)) + SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.ptnFeeVat,0)) AS withdrawPtnFee, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.bankFee,0)) AS withdrawBankFee, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.benefit,0)) + SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.benefitVat,0)) AS withdrawBenefit, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체' and A.taxType = '과세' ,A.tax,0)) AS withholdingAmt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체' and A.taxType = '비과세' ,A.tax,0)) AS withholdingAmt2, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '월렛이체',A.ptnFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '월렛이체',A.ptnFeeVat,0))AS walletTransferPtnFee, "
				+ "SUM(ptnFee)+SUM(ptnFeeVat) + SUM(IF(A.trxType = '출금' AND B.withholding = '파트너' and A.taxType = '과세' ,A.tax,0)) AS stlAmt, "
				+ "SUM(ptnFee) AS ptnFee, "
				+ "SUM(ptnFeeVat) AS ptnFeeVat, "
				+ "B.bankCd, B.bankName, B.account, B.accntHolder FROM VW_WL_TRX A "
				+ "LEFT JOIN VW_WL_PTN B on A.ptnId = B.ptnId "
//				+ "LEFT JOIN WL_SETTLE_IDX C ON A.trxId = C.trxId "
				+"  AND A.trxDate <= DATE_FORMAT(LAST_DAY(DATE_ADD(NOW(), INTERVAL -1 MONTH)), '%Y%m%d') "
//				+ " AND C.trxId IS null"
				+"  GROUP BY A.ptnId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}*/
	
	public List<SharedMap<String,Object>> getSettleList(){
		String q = "SELECT A.ptnId, B.name AS ptnName, B.withholding, MIN(A.regDay) AS startDay, MAX(A.regDay) as endDay, "
				+ "COUNT(IF(A.trxType = '입금' && A.trxUnit = '카드',1,null)) AS chargeCardCnt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드' , A.grossAmount,0)) AS chargeCardAmt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.netFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.netFeeVat,0)) AS chargeCardNetFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.ptnFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.ptnFeeVat,0)) AS chargeCardPtnFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.vanFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.vanFeeVat,0)) AS chargeCardPgFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.benefit,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '카드', A.benefitVat,0)) AS chargeCardBenefit, "
				+ "COUNT(IF(A.trxType = '입금' && A.trxUnit = '가상계좌',1,null)) AS chargeVactCnt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌' , A.grossAmount,0)) AS chargeVactAmt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.netFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.netFeeVat,0)) AS chargeVactNetFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.ptnFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.ptnFeeVat,0)) AS chargeVactPtnFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.bankFee,0)) AS chargeVactBankFee, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.benefit,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '가상계좌', A.benefitVat,0)) AS chargeVactBenefit, "
				+ "COUNT(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',1,null)) AS withdrawCnt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.grossAmount,0)) AS withdrawAmt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',netFee,0)) + SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.netFeeVat,0)) AS withdrawNetFee, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.ptnFee,0)) + SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.ptnFeeVat,0)) AS withdrawPtnFee, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.bankFee,0)) AS withdrawBankFee, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.benefit,0)) + SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체',A.benefitVat,0)) AS withdrawBenefit, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체' and A.taxType = '과세' ,A.tax,0)) AS withholdingAmt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '계좌이체' and A.taxType = '비과세' ,A.tax,0)) AS withholdingAmt2, "
				+ "COUNT(IF(A.trxType = '입금' && A.trxUnit = '월렛이체',1,null)) AS chargeWalletTransferCnt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '월렛이체' , A.grossAmount,0)) AS chargeWalletTransferAmt, "
				+ "SUM(IF(A.trxType = '입금' && A.trxUnit = '월렛이체',A.ptnFee,0)) + SUM(IF(A.trxType = '입금' && A.trxUnit = '월렛이체',A.ptnFeeVat,0))AS chargeWalletTransferPtnFee, "
				+ "COUNT(IF(A.trxType = '출금' && A.trxUnit = '월렛이체',1,null)) AS withdrawWalletTransferCnt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '월렛이체' , A.grossAmount,0)) AS withdrawWalletTransferAmt, "
				+ "SUM(IF(A.trxType = '출금' && A.trxUnit = '월렛이체',A.tax,0)) withdrawWalletTransferTax, "
				+ "SUM(ptnFee)+SUM(ptnFeeVat) + SUM(IF(A.trxType = '출금' AND B.withholding = '파트너' and A.taxType = '과세' ,A.tax,0)) AS stlAmt, "
				+ "SUM(ptnFee) AS ptnFee, "
				+ "SUM(ptnFeeVat) AS ptnFeeVat, "
				+ "B.bankCd, B.bankName, B.account, B.accntHolder FROM VW_WL_TRX A "
				+ "LEFT JOIN VW_WL_PTN B on A.ptnId = B.ptnId "  
				+ "WHERE " 
				+ "A.regDay >= date_format(date_add(now(), interval -1 month),'%Y%m01') "
				+ "AND A.regDay <= DATE_FORMAT(LAST_DAY(DATE_ADD(NOW(), INTERVAL -1 MONTH)), '%Y%m%d') "
				+ "AND B.role = '배달대행'"
				+ "GROUP BY A.ptnId ";
				
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public boolean insertSettle(SharedMap<String,Object> data){
		
		super.setTable("WL_SETTLE");
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	public void setSettleToIdx(String stlId, String ptnId, String startDay, String endDay){
		String q = "INSERT INTO WL_SETTLE_IDX  "
				+"	SELECT '"+stlId+"' ,trxId, trxType, trxUnit "
				+"  FROM WL_TRX where  trxDate >= '"+startDay+"000000' "
				+ " AND trxDate <= '"+endDay+"235959' and ptnId ='"+ptnId+"' ";
		logger.info("set WL_SETTLE_IDX : {}",super.update(q));
		super.initRecord();
	}
	
	public String getSettleId(){
		return "S"+getFunction("FN_NEXTVAL2","WL_SETTLE");
	}
	
	public String getStlDay(){
		String query = "SELECT MIN(days) AS stlDay FROM PG_CODE_HOLIDAY WHERE days >= DATE_FORMAT(now(),'%Y%m10') AND status = 'no'";
		RecordSet rset = super.query(query);
		super.initRecord();
		return rset.getRow(0).getString("stlDay");
	}
	
	public List<SharedMap<String,Object>> getTaxList(){
		String q = "SELECT B.walletId, B.ptnId, B.userId, B.identity, B.ceo AS ceoName, B.name AS walletName, B.compName AS compName, B.addr1, B.addr2, B.category AS bizCategory, B.industry AS bizType, B.email, "
				+ "LEFT(MAX(A.regDay),6) AS trxMonth, MAX(A.regDay) AS endDay, COUNT(*) AS trxCnt, SUM(A.grossAmount) AS amt, sum(A.grossAmount) - SUM(A.tax) AS supplyAmt, sum(A.tax) AS tax "
				+ "FROM WL_TRX A LEFT JOIN WL_USER B ON A.walletId = B.walletId "
				+ "WHERE A.taxType = '과세' AND A.trxType = '출금' AND B.idType = '사업자' "
				+ "AND A.trxDate >= DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 MONTH), '%Y%m01') "
				+ "AND A.trxDate <= DATE_FORMAT(LAST_DAY(DATE_ADD(NOW(), INTERVAL -1 MONTH)), '%Y%m%d') "
				+"  GROUP BY B.walletId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public String getTaxId(){
		return "T"+getFunction("FN_NEXTVAL2","WL_TAX");
	}

	public boolean insertTax(SharedMap<String, Object> data) {
		super.setTable("WL_TAX");
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}

	public List<SharedMap<String, Object>> getWithholdingList() {
		String q = "SELECT B.walletId, B.ptnId, B.userId, B.identity, B.name, B.addr1, B.addr2, B.email, "
				+ "LEFT(MAX(A.regDay),6) AS trxMonth, MAX(A.regDay) AS endDay, COUNT(*) AS trxCnt, SUM(A.grossAmount) AS amt, sum(A.tax) AS tax "
				+ "FROM WL_TRX A LEFT JOIN WL_USER B ON A.walletId = B.walletId "
				+ "LEFT JOIN WL_PTN_MNG C ON A.ptnId = C.ptnId "
				+ "WHERE A.taxType = '과세' AND A.trxUnit = '계좌이체' AND A.trxType = '출금' AND B.idType = '개인' AND C.withholding = '본사' "
				+ "AND A.trxDate >= DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 MONTH), '%Y%m01') "
				+ "AND A.trxDate <= DATE_FORMAT(LAST_DAY(DATE_ADD(NOW(), INTERVAL -1 MONTH)), '%Y%m%d') "
				+"  GROUP BY B.walletId ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}

	public boolean insertWithholding(SharedMap<String, Object> data) {
		super.setTable("WL_WITHHOLDING");
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
		
	}
	
	public List<SharedMap<String, Object>> getShopVatList() {
		String q = "select 'PG수수료' AS taxType, B.walletId, B.ptnId, B.userId, B.identity, B.ceo AS ceoName, B.name AS walletName, B.compName AS compName, B.addr1, B.addr2, B.category AS bizCategory, B.industry AS bizType, B.email, "
				+ "A.trxCnt, A.amount, A.supplyAmt, A.tax, A.trxMonth, A.endDay from "
				+ "(select walletId, count(*) AS trxCnt, sum(amount) AS amount, sum(stlFee) As supplyAmt, sum(stlFeeVat) as tax, left(MAX(regDay),6) AS trxMonth, MAX(regDay) AS endDay "
				+ "from WL_TRX_SETTLE  "
				+ "WHERE 1=1 "
				+ "AND regDay >= DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 MONTH), '%Y%m01') "
				+ "AND regDay <= DATE_FORMAT(LAST_DAY(DATE_ADD(NOW(), INTERVAL -1 MONTH)), '%Y%m%d') "
				+ "group by walletId) A "
				+ "join VW_WL_USER B on A.walletId = B.walletId "
				+ "WHERE B.idType = '가맹점' ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public List<SharedMap<String, Object>> getSettleVatList() {
		String q = "select '지급수수료' AS taxType, B.walletId, B.ptnId, B.userId, B.identity, B.ceo AS ceoName, B.name AS walletName, B.compName AS compName, B.addr1, B.addr2, B.category AS bizCategory, B.industry AS bizType, B.email, "
				+ "A.trxCnt, A.amount, A.supplyAmt, A.tax, A.trxMonth, A.endDay from "
				+ "(select walletId, count(*) AS trxCnt, sum(amount) AS amount, sum(stlWalletSupplyAmt) As supplyAmt, sum(stlWalletVat) as tax, left(MAX(regDay),6) AS trxMonth, MAX(regDay) AS endDay "
				+ "from WL_TRX_SETTLE  "
				+ "WHERE 1=1 "
				+ "AND regDay >= DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 MONTH), '%Y%m01') "
				+ "AND regDay <= DATE_FORMAT(LAST_DAY(DATE_ADD(NOW(), INTERVAL -1 MONTH)), '%Y%m%d') "
				+ "group by walletId) A "
				+ "join VW_WL_USER B on A.walletId = B.walletId "
				+ "WHERE B.idType != '가맹점' ";
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public boolean insertSettleVat(SharedMap<String, Object> data) {
		super.setTable("WL_SETTLE_TAX");
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
}


