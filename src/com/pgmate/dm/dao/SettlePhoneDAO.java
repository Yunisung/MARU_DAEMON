package com.pgmate.dm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class SettlePhoneDAO extends DAO{
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.SettlePhoneDAO.class );
	
	public SettlePhoneDAO() {
		super.setDebug(false);
	}
	
	/**
	 * 가맹점 아이디로 가맹점 TAX 정보(PG_MCHT_TAX) 테이블 데이터 조회
	 * @param mchtId : 가맹점 아이디
	 * @return
	 */
	public SharedMap<String, Object> getMchtTaxByMchtId(String mchtId) {
		super.setTable("PG_MCHT_TAX");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	/**
	 * 데이터 암호화
	 */
	public String getAESEnc(String value){
		String query = "SELECT FN_AES_ENC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	/**
	 *데이터 복호화
	 */
	public String getAESDec(String value){
		String query = "SELECT FN_AES_DEC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	public static String getFunction(String function, String value) {
		String returnVal = "";
		String query = "SELECT " + function + "(?) as val";

		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rset = null;

		try {

			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, value);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				returnVal = rset.getString(1);
			}
			conn.commit();
		} catch (Exception t) {
			
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}
	
	/**
	 * 정산아이디 생성
	 * @return
	 */
	public synchronized static String getSettleId() {
		return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
	}
	
	/**
	 * 자동정산 출금 결과 매입데이터 업데이트
	 * @param stlId : 정산번호
	 * @param stlDay : 정산예정일자
	 * @param stlType : 정산타입
	 * @param mchtId : 가맹점아이디
	 * @param payOutDay : 출금일자
	 * @return
	 */
	public boolean updatePhoneCapUpdate(String stlId, String stlDay, String stlType, String mchtId, String payOutDay, String stlStatus){
		String q = "UPDATE PG_PHONE_CAP "
				+ "    SET stlId = '" + stlId +"', stlStatus = '" + stlStatus + "', payOutDay='"+payOutDay+"'"
				+ "	 WHERE mchtId = '" + mchtId + "' and stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and stlStatus != '정산완료';";
		
		boolean updateed =  super.update(q);

		super.initRecord();
		return updateed;
	}
	
	/**
	 * 휴대폰 매입원장에서 정산예정일자의 정산데이터 조회
	 * @return
	 */
	public List<SharedMap<String,Object>> getPhoneSettleList(String stlDay){
		String q = "select X.* "
				+"    from "
				+"	  (SELECT mchtId,stlDay,min(trxDay) startDay,max(trxDay) endDay, "
				+"		  	  SUM(IF(capType ='매입',amount,0)) as payAmt, SUM(IF(capType ='매입',stlFee,0)) as payFee, SUM(IF(capType ='매입',stlFeeVat,0)) as payVat, SUM(IF(capType ='매입',1,0)) as payCnt,"
				+"		  	  SUM(IF(capType ='매입취소',amount,0)) as rfdAmt, SUM(IF(capType ='매입취소',stlFee,0)) as rfdFee, SUM(IF(capType ='매입취소',stlFeeVat,0)) as rfdVat, SUM(IF(capType ='매입취소',1,0)) as rfdCnt,"
				+"			  SUM(stlAmount) as stlAmount, sum(benefit) as benefit, MAX(stlRate) as stlRate, stlType"
				+"	 	 FROM PG_PHONE_CAP "
				+"  	where stlDay = '" + stlDay + "' and stlType = '0' and stlStatus = '정산대기'"
				+"  	group by mchtId, stlDay, stlType"
				+"  	order by mchtId) X;";
		
		RecordSet rset = super.query(q);
		super.initRecord();
		
		return rset.getRows();
	}
	
	/**
	 * 휴대폰정산 데이터 (PG_SETTLE_PHONE) 테이블 INSERT
	 * @param data
	 * @return
	 */
	public boolean insertSettlePhone(SharedMap<String,Object> data){
		super.setTable("PG_SETTLE_PHONE");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
}
