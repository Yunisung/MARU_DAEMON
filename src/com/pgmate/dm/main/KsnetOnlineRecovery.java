package com.pgmate.dm.main;

import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import javafx.beans.binding.ObjectExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class KsnetOnlineRecovery {
	private static Logger logger = LoggerFactory.getLogger( KsnetOnlineRecovery.class );
	private SmsGw smsGw = null;

	public KsnetOnlineRecovery() {
		smsGw = new SmsGw();
		vanTrxIdRecovery();
	}

	public static void main(String[] args) { new KsnetOnlineRecovery();}

	/**
	 * 단말기 거래시 거래번호가 VAN거래번호로 들어오면서 영수증 조회 및 취소로직이 동작을 안함
	 * KSNET노티 수신 받은후 거래번호만 업데이트
	 */
	public void vanTrxIdRecovery() {
		try {
			List<SharedMap<String, Object>> ksnetOnlineList = getKsnetOnlineList();
			logger.info("KSNET ONLINE 거래내역 : 총 {}건", ksnetOnlineList.size());

			if(ksnetOnlineList.size() == 0) {
				return;
			}

			for(SharedMap<String, Object> data : ksnetOnlineList) {
				String exeStatus = "대기";
				String summary = "";

				boolean isStatus = data.getString("status").equals("O") ? true : false;

				int retry = data.getInt("retry");

				if(!data.getString("status").equals("O")) {
					exeStatus = "실패";
					summary = "status 오류";
				}

				if(CommonUtil.isNullOrSpace(data.getString("orderNumber"))) {
					exeStatus = "실패";
					summary = "orderNumber 오류";
				}

				logger.info("KSNET ONLINE 노티 처리 시작 : {}", data.getString("transactionNo"));

				if(data.getString("approvalType").equals("1001") && isStatus) {
					//승인
					if(getPayList(data).size() > 0) {
						exeStatus = "실패";
						summary = "기승인 거래건";
					}else {
						//신규 승인건 생성
						SharedMap<String, Object> pay = new SharedMap<>();
						SharedMap<String, Object> tmnMap = getMchtTmn(data.getString("identity"), data.getString("storeId"));

						if(tmnMap != null) {
							pay.put("trxId", getTrxId());
							pay.put("trxType", "KSON");
							pay.put("mchtId", tmnMap.getString("mchtId"));
							pay.put("tmnId", tmnMap.getString("tmnId"));
							pay.put("trackId", data.getString("orderNumber"));
							pay.put("amount", data.getLong("amount"));
							pay.put("installment", data.getString("installment"));
							pay.put("cardId", GenKey.genKeys(CPKEY.CARD, pay.getString("trxId")));
							pay.put("bin", data.getString("cardNo").substring(0,6));
							pay.put("last4", data.getString("cardNo").substring(data.getString("cardNo").length()-4));
							pay.put("status", "승인");
							pay.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, pay.getString("trxId")));

							Product product = new Product();
							product.price = Long.parseLong(data.getString("amount"));
							product.name = data.getString("prodName");
							product.qty = Integer.parseInt(data.getString("qty"));
							product.desc = data.getString("description");
							insertProduct(pay.getString("prodId"),product, data.getString("regDate"));

							Card card = new Card();
							card.cardId = pay.getString("cardId");
							card.bin = pay.getString("bin");
							card.last4 = pay.getString("last4");

							SharedMap<String, Object> issuerMap = getDBIssuer(card.bin);
							if(issuerMap != null) {
								card.cardType = issuerMap.getString("type");
								card.issuer = issuerMap.getString("issuer");
								card.acquirer = issuerMap.getString("acquirer");
							}

							String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16)));
							insertCard(card.cardId,encrypted);

							pay.put("cardType"	, card.cardType);
							pay.put("issuer"	, card.issuer);
							pay.put("acquirer"	, card.acquirer);

							pay.put("reqDay"	, data.getString("tradeDate"));
							pay.put("reqTime"	, data.getString("tradeTime"));
							pay.put("authCd"	, data.getString("authNo"));

							pay.put("resultCd"	, "0000");
							pay.put("resultMsg"	, "정상");

							pay.put("van"		, tmnMap.getString("van"));
							pay.put("vanId"		, data.getString("storeId"));
							pay.put("vanTrxId"	, data.getString("transactionNo"));
							pay.put("regDay"	, data.getString("tradeDate"));
							pay.put("regTime"	, data.getString("tradeTime"));

							if(insertTrxPay(pay)) {
								exeStatus = "성공";
								summary = pay.getString("trxId");
								insertTrxREQ(pay.getString("trxId"), card, pay, tmnMap);
								insertTrxRES(pay.getString("trxId"), pay);
								logger.info("승인 거래 등록 완료 : {}", summary);
							} else {
								exeStatus = "실패";
								summary = "중복거래건 : " + pay.getString("vanTrxId");
							}
						} else {
							exeStatus = "실패";
							summary = "매칭실패 : " + data.getString("identity");
						}


					}

				} else if(data.getString("approvalType").equals("1011") && isStatus) {
					//취소
					if(getRfdList(data).size() > 0) {
						exeStatus = "실패";
						summary = "기취소 거래건";
					} else {
						//신규 취소건 생성
						SharedMap<String, Object> tmnMap = getMchtTmn(data.getString("identity"), data.getString("storeId"));

						if(tmnMap != null) {
							SharedMap<String, Object> refund = new SharedMap<>();
							SharedMap<String, Object> rootTrxPayMap = getPayMap(data, tmnMap);

							if(rootTrxPayMap.size() > 0) {
								refund.put("trxId", getTrxId());
								refund.put("mchtId", tmnMap.getString("mchtId"));
								refund.put("tmnId", tmnMap.getString("tmnId"));
								refund.put("trackId", data.getString("orderNumber"));
								refund.put("status", "완료");

								long amount = - rootTrxPayMap.getLong("amount");
								refund.put("rfdAmount", amount);

								if(data.getLong("amount") == rootTrxPayMap.getLong("amount")) {
									refund.put("rfdAll", "전액");
								} else {
									refund.put("rfdAll", "부분");
								}

								refund.put("rfdVat", calcRootVat(amount));
								refund.put("cardId", rootTrxPayMap.getString("cardId"));
								refund.put("bin", rootTrxPayMap.getString("bin"));
								refund.put("last4", rootTrxPayMap.getString("last4"));
								refund.put("issuer"	, rootTrxPayMap.getString("issuer"));
								refund.put("acquirer"	, rootTrxPayMap.getString("acquirer"));
								refund.put("rootTrnDay", rootTrxPayMap.getString("reqDay"));
								refund.put("rootTrxId"	, rootTrxPayMap.getString("trxId"));
								refund.put("rootTrackId", rootTrxPayMap.getString("trackId"));
								refund.put("rootAmount", rootTrxPayMap.getLong("amount"));
								refund.put("rootVat"	, calcRootVat(rootTrxPayMap.getLong("amount")));

								refund.put("reqDay"	, data.getString("tradeDate"));
								refund.put("reqTime"	, data.getString("tradeTime"));
								refund.put("authCd"	, data.getString("authNo"));

								refund.put("resultCd"	, "0000");
								refund.put("resultMsg", "정상");
								refund.put("van"		, rootTrxPayMap.getString("van"));
								refund.put("vanId"		, rootTrxPayMap.getString("vanId"));
								refund.put("vanTrxId"	, data.getString("transactionNo"));
								refund.put("regDay", data.getString("tradeDate"));
								refund.put("regTime", data.getString("tradeTime"));

								if(insertTrxRfd(refund)) {
									updateTrxPay(rootTrxPayMap.getString("trxId"));
									updateTrxAdminRfd(refund.getString("trxId"), data.getString("transactionNo"));
									exeStatus = "성공";
									summary = refund.getString("trxId");
									logger.info("취소 거래 등록 완료 : {}", summary);
								} else {
									exeStatus = "실패";
									summary = "중복거래건 : " + refund.getString("vanTrxId");
								}
							}


						} else {
							exeStatus = "실패";
							summary = "매칭실패 : " + data.getString("identity");
						}
					}
				}

				//LOAD_KSNET 업데이트
				if(updateLoadKsnet(data.getString("transactionNo"), data.getString("approvalType"), exeStatus, summary, retry)) {
					logger.info("KSNET ONLINE 노티 처리 종료 : {}", data.getString("transactionNo"));
				} else {
					logger.info("KSNET ONLINE 노티 처리 에러 : {}", data.getString("transactionNo"));
				}
			}

		}catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void insertProduct(String prodId, Product product, String regDate) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PRD");
		logger.info("set product id : {}", prodId);
		dao.setRecord("prodId", prodId);//1개
		dao.setRecord("description", CommonUtil.nToB(product.desc));
		dao.setRecord("name", CommonUtil.nToB(product.name));
		dao.setRecord("price", product.price);
		dao.setRecord("qty", product.qty);
		dao.setRecord("regDate", regDate);
		dao.insert();
		dao.initRecord();
	}

	private void updateTrxAdminRfd(String trxId, String vanTrxId) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_ADMIN_RFD");
		dao.setRecord("trxId", trxId);
		dao.addWhere("vanTrxId", vanTrxId);
		logger.info("set PG_TRX_ADMIN_RFD : {}", dao.update());
		dao.initRecord();
	}

	public List<SharedMap<String, Object>> getKsnetOnlineList() {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_LOAD_KSNET_ONLINE");
		dao.setColumns("*");
		dao.addWhere("exeStatus = '대기'");
		dao.addWhere("retry < 10");
		dao.setOrderBy("tradeDate asc,tradeTime asc");
		RecordSet rset = dao.search();
		return rset.getRows();
	}

	public List<SharedMap<String, Object>> getPayList(SharedMap<String, Object> data) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY");
		dao.setColumns("*");
		dao.addWhere("vanTrxId", data.getString("transactionNo"));
		dao.addWhere("vanId", data.getString("storeId"));
		dao.addWhere("reqDay", data.getString("tradeDate"));
		dao.addWhere("authCd", data.getString("authNo"));
		RecordSet rset = dao.search();
		return rset.getRows();
	}

	public SharedMap<String, Object> getRefund(SharedMap<String, Object> data) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_RFD");
		dao.setColumns("*");
		dao.addWhere("vanTrxId", data.getString("vanTransactionNo"));
		dao.addWhere("vanId", data.getString("storeId"));
		dao.addWhere("reqDay", data.getString("tradeDate"));
		dao.addWhere("authCd", data.getString("authNo"));
		RecordSet rset = dao.search();
		return rset.getRowFirst();
	}

	public List<SharedMap<String,Object>> getRfdList(SharedMap<String, Object> data){
		DAO dao = new DAO();
		dao.setTable("PG_TRX_RFD");
		dao.setColumns("*");
		dao.addWhere("vanTrxId", data.getString("vanTransactionNo"));
		dao.addWhere("vanId", data.getString("storeId"));
		dao.addWhere("reqDay", data.getString("tradeDate"));
		dao.addWhere("authCd", data.getString("authNo"));
		RecordSet rset = dao.search();
		return rset.getRows();
	}

	public boolean updateTrxPay(String trxId, String vanTrxId) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY");
		dao.setRecord("vanTrxId", vanTrxId);
		dao.addWhere("trxId", trxId);
		boolean updated = dao.update();
		logger.info("TrxPay update : {}", updated);
		return updated;
	}

	public boolean updateTrxRefund(String trxId, String vanTrxId) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_RFD");
		dao.setRecord("vanTrxId", vanTrxId);
		dao.addWhere("trxId", trxId);
		boolean updated = dao.update();
		logger.info("TrxPay update : {}", updated);
		return updated;
	}

	public boolean updateTrxCapture(String trxId, String vanTrxId){
		DAO dao = new DAO();
		dao.setTable("PG_TRX_CAP A INNER JOIN PG_TRX_CAP_DTL B  ON A.capId = B.capId");
		dao.setRecord("B.vanTrxId", vanTrxId);
		dao.addWhere("A.trxId",trxId);
		boolean updated = dao.update();
		logger.info("trx capture update : {}",updated);
		return updated;
	}

	public boolean updateLoadKsnet(String transactionNo, String approvalType, String exeStatus, String summary,int retry) {
		int updated = 0;

		String query = "UPDATE PG_TRX_LOAD_KSNET_ONLINE set exeStatus=?,exeDate=CURRENT_TIMESTAMP,summary=?, retry=? WHERE transactionNo=? and approvalType=?";

		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);

			int i = 1;

			pstmt.setString(i++  , exeStatus);
			pstmt.setString(i++  , summary);
			pstmt.setInt(i++     , retry);
			pstmt.setString(i++  , transactionNo);
			pstmt.setString(i++  , approvalType);
			updated = pstmt.executeUpdate();
			conn.commit();
		}catch(Exception e){
			logger.debug("insert batch PG_TRX_LOAD_KSNET error : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(pstmt);
			db.close(conn);
		}

		if(updated > 0) {
			return true;
		} else {
			return false;
		}
	}

	private String getAcquirer(String code){
		if(code.equals("01")){
			return "비씨";
		}else if(code.equals("02")){
			return "국민";
		}else if(code.equals("03")){
			return "하나";
		}else if(code.equals("04")){
			return "삼성";
		}else if(code.equals("05")){
			return "신한";
		}else if(code.equals("08")){
			return "현대";
		}else if(code.equals("09")){
			return "롯데";
		}else if(code.equals("15")){
			return "농협";
		}else if(code.equals("24")){
			return "하나";
		}else if(code.equals("25")){
			return "해외";
		}else{
			return "기타";
		}
	}
	private String getIssuer(String code){
		if(code.equals("01")){
			return "비씨";
		}else if(code.equals("02")){
			return "국민";
		}else if(code.equals("03")){
			return "하나";
		}else if(code.equals("04")){
			return "삼성";
		}else if(code.equals("05")){
			return "신한";
		}else if(code.equals("08")){
			return "현대";
		}else if(code.equals("09")){
			return "롯데";
		}else if(code.equals("11")){
			return "한미";
		}else if(code.equals("12")){
			return "수협";
		}else if(code.equals("14")){
			return "우리";
		}else if(code.equals("15")){
			return "농협";
		}else if(code.equals("16")){
			return "제주";
		}else if(code.equals("17")){
			return "광주";
		}else if(code.equals("18")){
			return "전북";
		}else if(code.equals("19")){
			return "조흥";
		}else if(code.equals("23")){
			return "주택";
		}else if(code.equals("24")){
			return "하나";
		}else if(code.equals("25")){
			return "해외";
		}else if(code.equals("26")){
			return "씨티";
		}else{
			return "기타";
		}
	}

	public String getFunction(String function, String value) {
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
			logger.debug("sql error : {}, query : {}", t.getMessage(), query);
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}

	public String getTrxId() {
		return "T" + getFunction("FN_NEXTVAL2", "TRN");
	}

	public SharedMap<String,Object> getDBIssuer(String bin){
		String key = "PG_CODE_BIN_"+bin;
		SharedMap<String,Object> issuerMap = new SharedMap<String,Object>();
		if(CommonUtil.isNullOrSpace(bin)){
			return issuerMap;
		}

		if (Cache.map.containsKey(key)) {
			return Cache.map.getUnchecked(key);
		} else {

			DAO dao = new DAO();
			dao.setTable("PG_CODE_BIN");
			dao.addWhere("bin", bin, DAO.eq);
			dao.setColumns("*");
			RecordSet rset = dao.search();

			if(rset.size() == 0){
				issuerMap.put("bin", bin);
				issuerMap.put("issuer", "기타");
				issuerMap.put("type", "신용");
			}else{
				issuerMap = rset.getRowFirst();
			}

			return Cache.map.put(key, rset.getRow(0));
		}
	}

	public void insertCard(String cardId, String value) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_BOX");
		dao.setRecord("cardId", cardId);//1개
		dao.setRecord("value", value);
	}

	public SharedMap<String, Object> getMchtTmn(String identity, String vanId) {
		DAO dao = new DAO();
		dao.setDebug(true);
		dao.setTable("(SELECT A.identity, B.*, C.vanId FROM PG_MCHT A LEFT OUTER JOIN PG_MCHT_TMN B ON A.mchtId=B.mchtId LEFT OUTER JOIN PG_VAN C ON B.vanIdx=C.idx) D");
		dao.setColumns("D.*");
		dao.addWhere("identity", identity, DAO.eq);
		dao.addWhere("vanId", vanId, DAO.eq);
		dao.addWhere("status", "사용", DAO.eq);
		dao.setOrderBy("regDate desc");
		RecordSet rset = dao.search();
		if(rset.size() > 0) {
			return rset.getRowFirst();
		} else {
			return null;
		}
	}

	public boolean insertTrxPay(SharedMap<String,Object> map){
		String query = "insert into PG_TRX_PAY (trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,status,prodId,issuer,acquirer,reqDay,reqTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,regDay,regTime,regDate)  values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
		int inserted = 0;
		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			int i=1;

			pstmt.setString(i++, map.getString("trxId"));
			pstmt.setString(i++, map.getString("mchtId"));
			pstmt.setString(i++, map.getString("tmnId"));
			pstmt.setString(i++, map.getString("trackId"));
			pstmt.setString(i++, map.getString("payerName"));
			pstmt.setString(i++, map.getString("payerEmail"));
			pstmt.setString(i++, map.getString("payerTel"));
			pstmt.setLong(i++  , map.getLong("amount"));
			pstmt.setString(i++, map.getString("installment"));
			pstmt.setString(i++, map.getString("cardId"));
			pstmt.setString(i++, map.getString("cardType"));
			pstmt.setString(i++, map.getString("bin"));
			pstmt.setString(i++, map.getString("last4"));
			pstmt.setString(i++, map.getString("status"));
			pstmt.setString(i++, map.getString("prodId"));
			pstmt.setString(i++, map.getString("issuer"));
			pstmt.setString(i++, map.getString("acquirer"));
			pstmt.setString(i++, map.getString("reqDay"));
			pstmt.setString(i++, map.getString("reqTime"));
			pstmt.setString(i++, map.getString("authCd"));
			pstmt.setString(i++, map.getString("resultCd"));
			pstmt.setString(i++, map.getString("resultMsg"));
			pstmt.setString(i++, map.getString("van"));
			pstmt.setString(i++, map.getString("vanId"));
			pstmt.setString(i++, map.getString("vanTrxId"));
			pstmt.setString(i++, map.getString("regDay"));
			pstmt.setString(i++, map.getString("regTime"));
			inserted = pstmt.executeUpdate();
			conn.commit();
		}catch(Exception e){
			logger.debug("insert batch pay error : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(pstmt);
			db.close(conn);
		}

		if(inserted > 0){
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," create TRX_PAY");
			return true;
		}else{
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," fail   TRX_PAY");
			return false;
		}
	}

	public SharedMap<String,Object> getPayMap(SharedMap<String,Object> data, SharedMap<String, Object> tmnMap){
		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY");
		dao.addWhere("mchtId"	, tmnMap.getString("mchtId"));
		dao.addWhere("tmnId"	, tmnMap.getString("tmnId"));
		dao.addWhere("authCd"	, data.getString("authNo"));
		dao.addWhere("vanId"	, data.getString("storeId"));
		dao.addWhere("vanTrxId"	, data.getLong("transactionNo"));


		dao.setColumns("*");
		RecordSet rset = dao.search();
		return rset.getRowFirst();
	}

	public long calcRootVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /110).longValue();
		}else{
			return new Double(amount *10 /110).longValue();
		}
	}

	public boolean insertTrxRfd(SharedMap<String,Object> map){
		int inserted = 0;
		String query = "insert into PG_TRX_RFD (trxId,mchtId,tmnId,trackId,status,rfdType,rfdAll,rfdAmount,rfdVat,cardId,bin,last4,issuer,acquirer,rootTrnDay,rootTrxId,rootTrackId,rootAmount,rootVat,reqDay,reqTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,regDay,regTime,regDate) "
				+" values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";

		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;

		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);

			int i=1;
			pstmt.setString(i++, map.getString("trxId"));
			pstmt.setString(i++, map.getString("mchtId"));
			pstmt.setString(i++, map.getString("tmnId"));
			pstmt.setString(i++, map.getString("trackId"));
			pstmt.setString(i++, map.getString("status"));
			pstmt.setString(i++, map.getString("rfdType"));
			pstmt.setString(i++, map.getString("rfdAll"));
			pstmt.setLong(i++  , map.getLong("rfdAmount"));
			pstmt.setLong(i++, map.getLong("rfdVat"));
			pstmt.setString(i++, map.getString("cardId"));
			pstmt.setString(i++, map.getString("bin"));
			pstmt.setString(i++, map.getString("last4"));
			pstmt.setString(i++, map.getString("issuer"));
			pstmt.setString(i++, map.getString("acquirer"));
			pstmt.setString(i++, map.getString("rootTrnDay"));
			pstmt.setString(i++, map.getString("rootTrxId"));
			pstmt.setString(i++, map.getString("rootTrackId"));
			pstmt.setLong(i++  , map.getLong("rootAmount"));
			pstmt.setLong(i++, map.getLong("rootVat"));
			pstmt.setString(i++, map.getString("reqDay"));
			pstmt.setString(i++, map.getString("reqTime"));
			pstmt.setString(i++, map.getString("authCd"));
			pstmt.setString(i++, map.getString("resultCd"));
			pstmt.setString(i++, map.getString("resultMsg"));
			pstmt.setString(i++, map.getString("van"));
			pstmt.setString(i++, map.getString("vanId"));
			pstmt.setString(i++, map.getString("vanTrxId"));
			pstmt.setString(i++, map.getString("resultCd"));
			pstmt.setString(i++, map.getString("resultMsg"));
			pstmt.setString(i++, map.getString("regDay"));
			pstmt.setString(i++, map.getString("regTime"));

			inserted = pstmt.executeUpdate();
			conn.commit();
		}catch(Exception e){
			logger.debug("insert batch rfd error : {}",CommonUtil.getExceptionMessage(e));
		}finally{
			db.close(pstmt);
			db.close(conn);
		}

		if(inserted > 0){
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," create TRX_RFD");
			return true;
		}else{
			logger.info("vanTrxId =[{}], [{}],[{}]",map.getString("vanTrxId"),map.getString("trxId")," fail   TRX_RFD");
			return false;
		}
	}

	public void updateTrxPay(String trxId) {

		DAO dao = new DAO();
		dao.setTable("PG_TRX_PAY");
		dao.setRecord("status", "승인취소");
		dao.addWhere("trxId", trxId);
		logger.info("set TRX_PAY : {}", dao.update());
		dao.initRecord();
	}

	public void insertTrxREQ(String trxId,Card card,SharedMap<String,Object> data,SharedMap<String, Object> tmsKeyMap) {
		DAO dao = new DAO();

		dao.setTable("PG_TRX_REQ");
		dao.setRecord("trxId"			, trxId);
		dao.setRecord("trxType"		, data.getString("trxType"));
		dao.setRecord("mchtId"		, tmsKeyMap.getString("mchtId"));
		dao.setRecord("tmnId"			, tmsKeyMap.getString("tmnId"));
		dao.setRecord("trackId"		, data.getString("trackId"));
		dao.setRecord("payerName"		, data.getString("payerName"));
		dao.setRecord("payerEmail"	, data.getString("payerEmail"));
		dao.setRecord("payerTel"		, data.getString("payerTel"));
		dao.setRecord("amount"		, data.getLong("amount"));
		dao.setRecord("cardId"		, card.cardId);
		dao.setRecord("issuer"		, card.issuer);
		dao.setRecord("last4"			, card.last4);
		dao.setRecord("cardType"		, card.cardType);
		dao.setRecord("bin"			, card.bin);
		dao.setRecord("installment"	, CommonUtil.zerofill(card.installment,2));
		dao.setRecord("acquirer"		, card.acquirer);
		dao.setRecord("prodId"		, data.getString("prodId"));
		dao.setRecord("regDay"		, data.getString("regDay"));
		dao.setRecord("regTime"		, data.getString("regTime"));

		logger.info("set TRX_REQ : {}", dao.insert());
		dao.initRecord();

	}

	public void insertTrxRES(String trxId,SharedMap<String,Object> data) {
		DAO dao = new DAO();
		dao.setTable("PG_TRX_RES");
		dao.setRecord("trxId", trxId);
		dao.setRecord("authCd", data.getString("authCd"));
		dao.setRecord("resultCd",data.getString("resultCd"));
		dao.setRecord("resultMsg", data.getString("resultMsg"));
		dao.setRecord("van", data.getString("van"));
		dao.setRecord("vanId", data.getString("vanId"));
		dao.setRecord("vanTrxId", data.getString("vanTrxId"));
		dao.setRecord("vanResultCd", data.getString("resultCd"));
		dao.setRecord("vanResultMsg", data.getString("resultMsg"));
		dao.setRecord("regDay", data.getString("regDay"));
		dao.setRecord("regTime", data.getString("regTime"));
		logger.info("set TRX_RES : {}", dao.insert());

		dao.initRecord();

	}
}
