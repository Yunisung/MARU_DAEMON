package com.pgmate.dm.dao;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class TotalDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Daemon.class );
	public TotalDAO() {
		super.setDebug(true);
	}
	
	public void getCapMerchantList(String curDate) throws Exception{
		super.setTable("PG_TOT_CAP");
		super.setWhere("capDay = '"+ curDate + "'");
		if(super.delete()) {
			logger.info("Delete PG_TOT_CAP {}", curDate);
		}
		super.initRecord();
		
		String q = "SELECT '"+curDate+"' as capDay, '"+curDate.substring(0,6)+"' as capMonth , "
				+"	mchtId,distId,agencyId,salesId FROM PG_MCHT WHERE  mchtId in (SELECT distinct(mchtId) FROM PG_MCHT_TMN WHERE activeDate <='"+curDate+"' ) "; 
		RecordSet rset = super.query(q);
		super.initRecord();
		
		HashMap<String,SharedMap<String,Object>> map = new HashMap<String,SharedMap<String,Object>>();
		for(int i=0;i<rset.size() ; i++){
			SharedMap<String,Object> data = rset.getRow(i);
			map.put(data.getString("mchtId"), data);
		}
		
		List<SharedMap<String,Object>> salesList = getSalesList(curDate);
		
		for(int i=0;i<salesList.size() ; i++){
			SharedMap<String,Object> data = salesList.get(i);
			SharedMap<String,Object> mapData = map.get(data.getString("mchtId"));
			if(mapData == null){
				logger.info("NULL salesList mchtId : {}",data.getString("mchtId"));
			} else {
				mapData.putAll(data);
			}
		}
		
		List<SharedMap<String,Object>> refundList = getRefundList(curDate);
		for(int i=0;i<refundList.size() ; i++){
			SharedMap<String,Object> data = refundList.get(i);
			SharedMap<String,Object> mapData = map.get(data.getString("mchtId"));
			if(mapData == null){
				logger.info("NULL refundList mchtId : {}",data.getString("mchtId"));
			} else {
				mapData.putAll(data);
			}
		}
		
		List<SharedMap<String,Object>> refundedList = getRefundedList(curDate);
		for(int i=0;i<refundedList.size() ; i++){
			SharedMap<String,Object> data = refundedList.get(i);
			SharedMap<String,Object> mapData = map.get(data.getString("mchtId"));
			if(mapData == null){
				logger.info("NULL refundedList mchtId : {}",data.getString("mchtId"));
			} else {
				mapData.putAll(data);
			}
		}
		
		for( String key : map.keySet() ){
			SharedMap<String,Object> data = map.get(key);
			super.setTable("PG_TOT_CAP");
			data.remove("_idx");
			data.put("distFee", data.getLong("saleStlDistFee")+data.getLong("rfdStlDistFee")+data.getLong("rfdedStlDistFee"));
			data.put("agencyFee", data.getLong("saleStlAgencyFee")+data.getLong("rfdStlAgencyFee")+data.getLong("rfdedStlAgencyFee"));
			data.put("salesFee", data.getLong("saleStlSalesFee")+data.getLong("rfdStlSalesFee")+data.getLong("rfdedStlSalesFee"));
			data.put("benefit", data.getLong("saleBenefit")+data.getLong("rfdBenefit")+data.getLong("rfdedBenefit"));
			
			for(String key2 : data.keySet()){
				super.setRecord(key2, data.get(key2));
			}
			logger.debug("insert mcht : {} , {}",key ,super.insert());
			super.initRecord();
			//i++;
		}
		
		//return i;
		
	}
	
	public void getTotCapByStlDay(String curDate){
		super.setDebug(true);
		super.setTable("PG_TOT_CAP_STLDAY");
		super.setWhere("stlDay = '"+ curDate + "'");
		if(super.delete()) {
			logger.info("Delete PG_TOT_CAP_STLDAY {}", curDate);
		}
		super.initRecord();
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO PG_TOT_CAP_STLDAY ");
		sb.append("SELECT null,mchtId, distId, agencyId, salesId, stlDay, ");
		sb.append("sum(if(capType = '매입',1,0)) AS payCnt, ");
		sb.append("sum(if(capType = '매입',amount,0)) AS payAmt, ");
		sb.append("sum(if(capType = '매입취소',1,0)) AS rfdCnt, ");
		sb.append("sum(if(capType = '매입취소',amount,0)) AS rfdAmt, ");
		sb.append("count(*) AS totalCnt, ");
		sb.append("sum(amount) AS totalAmt, ");
		sb.append("SUM(stlVanFee) AS stlVanFee, ");
		sb.append("SUM(stlVanAmount) AS stlVanAmt, ");
		sb.append("SUM(stlDiffVanAmt) AS stlDiffVanAmt, ");
		sb.append("SUM(stlFee) AS stlFee, ");
		sb.append("SUM(stlFeeVat) AS stlFeeVat, ");
		sb.append("SUM(stlAmount) AS stlAmt, ");
		sb.append("SUM(stlFee + stlFeeVat - stlVanFee) AS profit, ");
		sb.append("SUM(stlDistFee) AS stlDistFee, ");
		sb.append("SUM(stlDiffDistFee) AS stlDiffDistFee, ");
		sb.append("SUM(stlAgencyFee) AS stlAgencyFee, ");
		sb.append("SUM(stlDiffAgencyFee) AS stlDiffAgencyFee, ");
		sb.append("SUM(stlSalesFee) AS stlSalesFee, ");
		sb.append("SUM(stlDiffSalesFee) AS stlDiffSalesFee, ");
		sb.append("SUM(if(stlDiffStatus = '결과대기',stlDiffAmt,stlDiffVanAmt)) AS stlDiffAmt, "); 
		sb.append("SUM(benefit) AS benefit, ");
		sb.append("null ");
		sb.append("FROM VW_TRX_CAP_LIST ");
		sb.append("WHERE stlDay = '"+curDate+"' ");
		sb.append("GROUP BY mchtId, stlDay ");
		
		
		super.insert(sb.toString());
		super.initRecord();
		
		
		
	}
	
	public void getTotCapByCapDay(String curDate){
		super.setDebug(true);
		super.setTable("PG_TOT_CAP_CAPDAY");
		super.setWhere("capDay = '"+ curDate + "'");
		if(super.delete()) {
			logger.info("Delete PG_TOT_CAP_CAPDAY {}", curDate);
		}
		super.initRecord();
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO PG_TOT_CAP_CAPDAY ");
		sb.append("SELECT null,mchtId, distId, agencyId, salesId, regDay AS capDay, ");
		sb.append("sum(if(capType = '매입',1,0)) AS payCnt, ");
		sb.append("sum(if(capType = '매입',amount,0)) AS payAmt, ");
		sb.append("sum(if(capType = '매입취소',1,0)) AS rfdCnt, ");
		sb.append("sum(if(capType = '매입취소',amount,0)) AS rfdAmt, ");
		sb.append("count(*) AS totalCnt, ");
		sb.append("sum(amount) AS totalAmt, ");
		sb.append("SUM(stlVanFee) AS stlVanFee, ");
		sb.append("SUM(stlVanAmount) AS stlVanAmt, ");
		sb.append("SUM(stlDiffVanAmt) AS stlDiffVanAmt, ");
		sb.append("SUM(stlFee) AS stlFee, ");
		sb.append("SUM(stlFeeVat) AS stlFeeVat, ");
		sb.append("SUM(stlAmount) AS stlAmt, ");
		sb.append("SUM(stlFee + stlFeeVat - stlVanFee) AS profit, ");
		sb.append("SUM(stlDistFee) AS stlDistFee, ");
		sb.append("SUM(stlDiffDistFee) AS stlDiffDistFee, ");
		sb.append("SUM(stlAgencyFee) AS stlAgencyFee, ");
		sb.append("SUM(stlDiffAgencyFee) AS stlDiffAgencyFee, ");
		sb.append("SUM(stlSalesFee) AS stlSalesFee, ");
		sb.append("SUM(stlDiffSalesFee) AS stlDiffSalesFee, ");
		sb.append("SUM(if(stlDiffStatus = '결과대기',stlDiffAmt,stlDiffVanAmt)) AS stlDiffAmt, "); 
		sb.append("SUM(benefit) AS benefit, ");
		sb.append("null ");
		sb.append("FROM VW_TRX_CAP_LIST ");
		sb.append("WHERE regDay = '"+curDate+"' ");
		sb.append("GROUP BY mchtId, regDay ");
		
		
		super.insert(sb.toString());
		super.initRecord();
		
		
		
	}
	
	public List<SharedMap<String,Object>> getSalesList(String curDate){
		String q = "SELECT mchtId, MIN(stlDay) as stlDay,sum(amount) as saleAmount,count(1) as saleCount, sum(stlAmount) as saleStlAmount,sum(stlFee) as saleStlFee, "
				+"	sum(stlFeeVat) as saleStlFeeVat,sum(stlDistFee) as saleStlDistFee, "
				+"  sum(stlAgencyFee) as saleStlAgencyFee,sum(stlSalesFee) as saleStlSalesFee,sum(benefit) as saleBenefit "
				+"  FROM VW_TRX_CAP WHERE regDay = '"+curDate+"' and capType='매입' group by mchtId ";
		logger.debug(q);
		RecordSet rset = super.query(q);
		super.initRecord();
		
		
		return rset.getRows();
	}
	
	public List<SharedMap<String,Object>> getRefundList(String curDate){
		String q = "SELECT mchtId,MIN(stlDay) as stlDay,sum(amount) as rfdAmount,count(1) as rfdCount, sum(stlAmount) as rfdStlAmount,sum(stlFee) as rfdStlFee, "
				+"	sum(stlFeeVat) as rfdStlFeeVat,sum(stlDistFee) as rfdStlDistFee, "
				+"  sum(stlAgencyFee) as rfdStlAgencyFee,sum(stlSalesFee) as rfdStlSalesFee,sum(benefit) as rfdBenefit "
				+"  FROM VW_TRX_CAP WHERE regDay = '"+curDate+"' and capType='매입취소' and risk ='' group by mchtId ";
		logger.debug(q);
		RecordSet rset = super.query(q);
		super.initRecord();
		
		
		return rset.getRows();
	}
	
	public List<SharedMap<String,Object>> getRefundedList(String curDate){
		String q = "SELECT mchtId, MIN(stlDay) as stlDay,sum(amount) as rfdedAmount,count(1) as rfdedCount, sum(stlAmount) as rfdedStlAmount,sum(stlFee) as rfdedStlFee, "
				+"	sum(stlFeeVat) as rfdedStlFeeVat,sum(stlDistFee) as rfdedStlDistFee, "
				+"  sum(stlAgencyFee) as rfdedStlAgencyFee,sum(stlSalesFee) as rfdedStlSalesFee,sum(benefit) as rfdedBenefit "
				+"  FROM VW_TRX_CAP WHERE regDay = '"+curDate+"' and capType='매입취소' and risk !='' group by mchtId";
		logger.debug(q);
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	//SUB
	public void getCapTmnList(String curDate) throws Exception{
		logger.debug("=== START getCapTmnList {}", curDate);
		super.setTable("PG_TOT_CAP_SUB");
		super.setWhere("capDay = '"+ curDate + "'");
		if(super.delete()) {
			logger.info("Delete PG_TOT_CAP_SUB {}", curDate);
		}
		super.initRecord();
		
		String q = "SELECT '"+curDate+"' as capDay, '"+curDate.substring(0,6)+"' as capMonth , "
				+"	mchtId,tmnId FROM PG_MCHT_TMN WHERE tmnId in (SELECT distinct(tmnId) FROM PG_TRX_CAP_SUB WHERE regDay <= '"+curDate+"' ) "; 
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		HashMap<String,SharedMap<String,Object>> map = new HashMap<String,SharedMap<String,Object>>();
		for(int i=0;i<rset.size() ; i++){
			SharedMap<String,Object> data = rset.getRow(i);
			map.put(data.getString("tmnId"), data);
		}
		
		List<SharedMap<String,Object>> refundList = getSubRefundList(curDate);
		for(int i=0;i<refundList.size() ; i++){
			SharedMap<String,Object> data = refundList.get(i);
			SharedMap<String,Object> mapData = map.get(data.getString("tmnId"));
			if (mapData == null) {
				logger.info("NULL refundList tmnId", data.getString("tmnId"));
			} else {
				mapData.putAll(data);
			}
		}
		
		List<SharedMap<String,Object>> refundedList = getSubRefundedList(curDate);
		for(int i=0;i<refundedList.size() ; i++){
			SharedMap<String,Object> data = refundedList.get(i);
			SharedMap<String,Object> mapData = map.get(data.getString("tmnId"));
			if (mapData == null) {
				logger.info("NULL refundedList tmnId", data.getString("tmnId"));
			} else {
				mapData.putAll(data);
			}
		}
		
		List<SharedMap<String,Object>> salesList = getSubSalesList(curDate);
		for(int i=0;i<salesList.size() ; i++){
			SharedMap<String,Object> data = salesList.get(i);
			SharedMap<String, Object> mapData = map.get(data.getString("tmnId"));
			if (mapData == null) {
				logger.info("NULL salesList tmnId", data.getString("tmnId"));
			} else {
				mapData.putAll(data);
			}
		}
		
		for( String key : map.keySet() ){
			try	{
				SharedMap<String,Object> data = map.get(key);
				super.setTable("PG_TOT_CAP_SUB");
				data.remove("_idx");
				data.put("benefit", data.getLong("saleBenefit")+data.getLong("rfdBenefit")+data.getLong("rfdedBenefit"));
				
				for(String key2 : data.keySet()){
					super.setRecord(key2, data.get(key2));
				}
				logger.debug("insert tmn : {} , {}",key ,super.insert());
				super.initRecord();
			} catch (Exception ex) {
				logger.debug("EXCEPTION: {}", ex.getMessage());
			}
		}
		logger.debug("=== END getCapTmnList {}", curDate);
	}
	
	public List<SharedMap<String,Object>> getSubSalesList(String curDate){
		String q = "SELECT tmnId,mchtId,MIN(stlDay) as stlDay,sum(amount) as saleAmount,count(1) as saleCount, sum(stlAmount) as saleStlAmount,sum(stlFee) as saleStlFee, "
				+"	sum(stlFeeVat) as saleStlFeeVat,sum(benefit) as saleBenefit "
				+"  FROM VW_TRX_CAP_SUB WHERE regDay = '"+curDate+"' and capType='매입' group by mchtId,tmnId ";
		logger.debug(q);
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public List<SharedMap<String,Object>> getSubRefundList(String curDate){
		String q = "SELECT tmnId,mchtId,MIN(stlDay) as stlDay,sum(amount) as rfdAmount,count(1) as rfdCount, sum(stlAmount) as rfdStlAmount,sum(stlFee) as rfdStlFee, "
				+"	sum(stlFeeVat) as rfdStlFeeVat,sum(benefit) as rfdBenefit "
				+"  FROM VW_TRX_CAP_SUB WHERE regDay = '"+curDate+"' and capType='매입취소' group by mchtId,tmnId ";
		logger.debug(q);
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public List<SharedMap<String,Object>> getSubRefundedList(String curDate){
		String q = "SELECT tmnId,mchtId, MIN(stlDay) as stlDay,sum(amount) as rfdedAmount,count(1) as rfdedCount, sum(stlAmount) as rfdedStlAmount,sum(stlFee) as rfdedStlFee, "
				+"	sum(stlFeeVat) as rfdedStlFeeVat,sum(benefit) as rfdedBenefit "
				+"  FROM VW_TRX_CAP_SUB WHERE regDay = '"+curDate+"' and capType='정산취소' group by mchtId,tmnId";
		logger.debug(q);
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	public static void main(String[] args){
		String startDate = "20170310";
		
		try {
			for(int i=0 ; i < 100 ; i++){
				new TotalDAO().getCapMerchantList(CommonUtil.getOpDate(Calendar.DATE, i, startDate));
			}
		}catch(Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public void getPrf(String curDate) {
		super.setDebug(true);
		super.setTable("VA_PRF");
		super.setWhere("trxDay = '"+ curDate + "'");
		if(super.delete()) {
			logger.info("Delete VA_PRF {}", curDate);
		}
		super.initRecord();
		super.setDebug(true);
		StringBuffer sb = new StringBuffer();
		sb.append("INSERT INTO VA_PRF ");
		sb.append("SELECT \n" + 
				"null, \n" + 
				"ptnId,\n" + 
				"(select name from VA_PTN where VA_TRX.ptnId=VA_PTN.ptnId) AS name,\n" + 
				"SUM(if(trxType = '입금',1,0)) AS depCnt, \n" + 
				"SUM(if(trxType = '출금',1,0)) AS wthCnt, \n" + 
				"SUM(if(trxType = '입금',stlAmount,0)) AS depAmt, \n" + 
				"SUM(if(trxType = '출금',stlAmount,0)) AS wthAmt, \n" + 
				"COUNT(*) AS totTrxCnt, \n" + 
				"SUM(stlAmount) AS totTrxAmt, \n" + 
				"(select case when SUM(if(trxType = '입금',stlAmount,0)) = 0 then 0 \n" + 
				"else SUM(if(trxType = '입금',fee+feeVat,0))/SUM(if(trxType = '입금',stlAmount,0))\n" + 
				"end \n" + 
				"FROM dual) AS depFeeRate,\n" + 
				"SUM(if(trxType = '입금',fee+feeVat,0)) AS depFee,\n" + 
				"'고정액' AS wthFeeRate,\n" + 
				"SUM(if(trxType = '출금',fee+feeVat,0)) AS wthFee,\n" + 
				"(select case when SUM(stlAmount)=0 then 0 \n" + 
				"else  SUM(fee+feeVat)/SUM(stlAmount) \n" + 
				"end\n" + 
				"from dual) as totRate,\n" + 
				"SUM(fee+feeVat) AS totAmt,\n" + 
				"SUM(bankFee) AS salesCost,\n" + 
				"(select case when SUM(stlAmount)=0 then 0 \n" + 
				"else (SUM(fee+feeVat)-SUM(bankFee))/SUM(stlAmount) \n" + 
				"end \n" + 
				"from dual) as profitFee,\n" + 
				"(select SUM(fee+feeVat)-SUM(bankFee) from dual) AS profit,\n" + 
				"trxDay,\n" + 
				"regDay,\n" + 
				"null  ");
		sb.append("FROM VA_TRX ");
		sb.append("WHERE trxDay = '"+curDate+"' ");
		sb.append("GROUP BY ptnId, trxDay "
				+ "ORDER BY regDate asc");
		
		
		super.insert(sb.toString());
		super.initRecord();
	}
	
	//계정 정보(email, tel1, tel2) 중복 체크 =======================================================
	/*
	 * 기존 내역 삭제-항상 최신 데이터로 유지한다. (주 1회 배치)
	 */
	public void deleteBefore(){
		String q = "DELETE FROM PG_NOTICE_LIST";
		
		super.update(q);
		super.initRecord();
	}
	
	public List<SharedMap<String, Object>> checkDist(){
		String q = "SELECT DISTINCT distId AS id, name, email, tel1, tel2 FROM PG_MAM_DIST";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	public List<SharedMap<String, Object>> checkAgency(){
		String q = "SELECT DISTINCT agencyId AS id, name, email, tel1, tel2 FROM PG_MAM_AGENCY";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	public List<SharedMap<String, Object>> checkSales(){
		String q = "SELECT DISTINCT salesId AS id, name, email, tel1, tel2 FROM PG_MAM_SALES";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	public List<SharedMap<String, Object>> checkMcht(){
		String q = "SELECT A.mchtId AS id, A.name, B.email, A.tel1, A.tel2 FROM PG_MCHT A LEFT JOIN PG_MCHT_TAX B ON A.mchtId = B.mchtId	" + 
				"UNION	" + 
				"SELECT A.mchtId AS id, A.name, B.email, A.tel1, A.tel2 FROM PG_MCHT A RIGHT JOIN PG_MCHT_TAX B ON A.mchtId = B.mchtId	";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRows();
	}
	
	public void insertDistinct(SharedMap<String, Object> map) {
		super.setTable("PG_NOTICE_LIST");
		super.setRecord("parentId"		, map.get("id"));
		super.setRecord("name"			, map.get("name"));
		super.setRecord("grade"			, map.get("grade"));
		super.setRecord("email"			, map.get("email"));
		super.setRecord("tel1"			, map.get("tel1"));
		if(map.getString("tel2") != null) {
			super.setRecord("tel2", map.getString("tel2"));
		}
		super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		
		super.insert();
		super.initRecord();
	}
	
	public void checkDistinct(){
		String q = "UPDATE PG_NOTICE_LIST a, PG_NOTICE_LIST b SET a.email = NULL WHERE a.idx > b.idx AND a.email = b.email";
		super.query(q);
		super.initRecord();
		
		String q1 = "UPDATE PG_NOTICE_LIST a, PG_NOTICE_LIST b SET a.tel1 = NULL WHERE a.idx > b.idx AND a.tel1 = b.tel1";
		super.query(q1);
		super.initRecord();
		
		String q2 = "UPDATE PG_NOTICE_LIST a, PG_NOTICE_LIST b SET a.tel2 = NULL WHERE a.idx > b.idx AND a.tel2 = b.tel2";
		super.query(q2);
		super.initRecord();
	}		
	
	public void checkTel(){
		RecordSet rset;
		String tel2 = "";
		
		String q = "SELECT tel2 FROM PG_NOTICE_LIST WHERE (tel2 IS NOT NULL AND tel2 != '')";
		rset = super.query(q);
		
		if(rset.size() > 0) {
			super.initRecord();
			List<SharedMap<String, Object>> getTel2List = rset.getRows();
			
			for(SharedMap<String, Object> tel2List : getTel2List) {
				tel2 = tel2List.getString("tel2");
				
				String q1 = "SELECT tel1 FROM PG_NOTICE_LIST WHERE tel1 = '" + tel2 + "'";
				rset = super.query(q1);
				
				if(rset.size() > 0) {
					super.initRecord();
					List<SharedMap<String, Object>> getTel1List = rset.getRows();
					
					for(SharedMap<String, Object> tel1List : getTel1List) {
						q = "UPDATE PG_NOTICE_LIST SET tel2 ='' WHERE tel1 = '" + tel2 + "'";
						super.update(q);
						super.initRecord();
					}
				}
			}
		}
	}
	//계정 정보(email, tel1, tel2) 중복 체크 =======================================================
}
