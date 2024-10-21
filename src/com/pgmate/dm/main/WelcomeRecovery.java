package com.pgmate.dm.main;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class WelcomeRecovery {
    private static Logger logger = LoggerFactory.getLogger(com.pgmate.dm.main.WelcomeRecovery.class);

    public WelcomeRecovery() {
        try {
            Recovery();
        }catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new WelcomeRecovery();
    }

    public void Recovery() {
        try {
            List<SharedMap<String, Object>> welcomeList = getWelcomeList();
            logger.info("웰컴 거래내역 : 총 {}건", welcomeList.size());

            if(welcomeList.size() == 0) {
                return;
            }

            for(SharedMap<String, Object> data : welcomeList) {
                String exeStatus = "성공";
                String summary = "";

                logger.info("웰컴 노티 처리 시작 : {}", data.getString("tid"));

                if(!data.getString("transStatus").equals("00")) {
                    exeStatus = "실패";
                    summary = "거래결과 실패건";
                }

                SharedMap<String, Object> tmnMap = getMchtTmn(data.getString("offTerminalNum"));
                if(tmnMap == null) {
                    exeStatus = "실패";
                    summary = "터미널ID 조회 오류 " + data.getString("offTerminalNum");
                }

                if(tmnMap != null && data.getString("transStatus").equals("00")) {
                    if(data.getString("transType").equals("00")) {
                        //승인
                        if(getPayList(data.getString("tid")).size() > 0) {
                            exeStatus = "실패";
                            summary = "기승인 거래건";
                        } else {
                            SharedMap<String,Object> pay = new SharedMap<String,Object>();
                            pay.put("trxId", getTrxId());
                            pay.put("mchtId", tmnMap.getString("mchtId"));
                            pay.put("tmnId", tmnMap.getString("tmnId"));
                            pay.put("trackId", getTrackId());
                            pay.put("amount", data.getLong("transAmount"));
                            pay.put("installment", data.getString("cardQuota"));
                            pay.put("cardId", GenKey.genKeys(CPKEY.CARD, pay.getString("trxId")));
                            pay.put("bin", data.getString("cardNumber").substring(0, 6));
                            pay.put("last4", data.getString("cardNumber").substring(data.getString("cardNumber").length()-4));
                            pay.put("status", "승인");
                            pay.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, pay.getString("trxId")));

                            Card card = new Card();
                            card.cardId = pay.getString("cardId");
                            card.installment = pay.getInt("installment");
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

                            pay.put("reqDay"	, data.getString("transDate"));
                            pay.put("reqTime"	, data.getString("transTime"));
                            pay.put("authCd"	, data.getString("cardApplNumber"));

                            pay.put("resultCd"	, "0000");
                            pay.put("resultMsg"	, data.getString("resultMessage"));

                            pay.put("van"		, tmnMap.getString("van"));
                            pay.put("vanId"		, data.getString("mid"));
                            pay.put("vanTrxId"	, data.getString("tid"));
                            pay.put("regDay"	, data.getString("transDate"));
                            pay.put("regTime"	, data.getString("transTime"));

                            if(insertTrxPay(pay)) {
                                exeStatus = "성공";
                                summary = pay.getString("trxId");
                                logger.info("승인 거래 등록 완료 : {}", summary);
                            }else {
                                exeStatus = "실패";
                                summary = "중복거래건 : " + pay.getString("vanTrxId");
                            }
                        }
                    } else if(data.getString("transType").equals("01") || data.getString("transType").equals("02")) {
                        //취소
                        if(getRfdList(data.getString("tid")).size() > 0) {
                            exeStatus = "실패";
                            summary = "기취소 거래건";
                        } else {
                            SharedMap<String,Object> refund = new SharedMap<String,Object>();
                            SharedMap<String,Object> rootTrxPayMap = getPayMap(data, tmnMap);
                            if(rootTrxPayMap.size() > 0) {
                                refund.put("trxId", getTrxId());
                                refund.put("mchtId", tmnMap.getString("mchtId"));
                                refund.put("tmnId", tmnMap.getString("tmnId"));
                                refund.put("trackId", getTrackId());
                                refund.put("status", "완료");

                                long amount = - rootTrxPayMap.getLong("amount");
                                refund.put("rfdAmount", amount);

                                if(data.getLong("transAmount") == rootTrxPayMap.getLong("amount")) {
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
                                refund.put("reqDay"	, data.getString("transDate"));
                                refund.put("reqTime"	, data.getString("transTime"));
                                refund.put("authCd"	, data.getString("cardApplNumber"));
                                refund.put("resultCd"	, "0000");
                                refund.put("resultMsg", data.getString("resultMessage"));
                                refund.put("van"		, rootTrxPayMap.getString("van"));
                                refund.put("vanId"		, rootTrxPayMap.getString("vanId"));
                                refund.put("vanTrxId"	, data.getString("tid"));
                                refund.put("regDay"	, data.getString("transDate"));
                                refund.put("regTime"	, data.getString("transTime"));

                                if(insertTrxRfd(refund)) {
                                    updateTrxPay(rootTrxPayMap.getString("trxId"));
                                    exeStatus = "성공";
                                    summary = refund.getString("trxId");
                                    logger.info("취소 거래 등록 완료 : {}", summary);
                                } else {
                                    exeStatus = "실패";
                                    summary = "중복거래건 : " + refund.getString("vanTrxId");
                                }

                            } else {
                                exeStatus = "실패";
                                summary = "원거래 없음";
                            }
                        }
                    } else {
                        logger.info("처리 안되는 거래구분코드 : {}", data.getString("transType"));
                        exeStatus = "실패";
                        summary = "처리 안되는 구분코드 " + data.getString("transType");
                    }
                }

                data.put("exeStatus", exeStatus);
                data.put("summary", summary);

                if(updateLoadWelcome(data)) {
                    logger.info("웰컴 노티 처리 종료 : {}", data.getString("tid"));
                } else {
                    logger.error("웰컴 노티 처리 종료 : {}", data.getString("tid"));
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public List<SharedMap<String, Object>> getWelcomeList() {
        DAO dao = new DAO();
        dao.setTable("PG_TRX_LOAD_WELCOME");
        dao.setColumns("*");
        dao.addWhere("exeStatus = '대기'");
        dao.setOrderBy("transDate asc, transTime asc");
        RecordSet rset = dao.search();
        return rset.getRows();
    }

    public SharedMap<String, Object> getMchtTmn(String tmnId) {
        String key = "PG_MCHT_TMN_" + tmnId;

        if (Cache.map.containsKey(key)) {
            Cache.map.get(key);
            return Cache.map.getUnchecked(key);
        } else {
            DAO dao = new DAO();
            dao.setTable("PG_MCHT_TMN");
            dao.setColumns("*");
            dao.addWhere("tmnId", tmnId, DAO.eq);
            RecordSet rset = dao.search();

            if(rset.size() > 0){
                return Cache.map.put(key, rset.getRow(0));

            }else{
                return null;
            }
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

    public String getTrackId() {
        return "TX" + getFunction("FN_NEXTVAL2", "TRACKID");
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

    public boolean updateLoadWelcome(SharedMap<String, Object> map) {
        int updated = 0;

        String query = "UPDATE PG_TRX_LOAD_WELCOME set exeStatus=?,exeDate=CURRENT_TIMESTAMP,summary=? WHERE tid =? and transType=?";

        DBManager db = null ;
        Connection conn = null;
        PreparedStatement pstmt = null;

        try{
            db 		= DBFactory.getInstance();
            conn	= db.getConnection();
            pstmt	= conn.prepareStatement(query);

            int i = 1;

            pstmt.setString(i++  , map.getString("exeStatus"));
            pstmt.setString(i++  , map.getString("summary"));
            pstmt.setString(i++, map.getString("tid"));
            pstmt.setString(i++  , map.getString("transType"));
            updated = pstmt.executeUpdate();
            conn.commit();
        }catch(Exception e){
            logger.debug("insert batch PG_TRX_LOAD_WELCOME error : {}",CommonUtil.getExceptionMessage(e));
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

    public List<SharedMap<String,Object>> getPayList(String vanTrxId){

        DAO dao = new DAO();
        dao.setTable("PG_TRX_PAY");
        dao.setColumns("*");
        dao.addWhere("vanTrxId",vanTrxId);
        RecordSet rset = dao.search();
        return rset.getRows();
    }

    public List<SharedMap<String,Object>> getRfdList(String vanTrxId){

        DAO dao = new DAO();
        dao.setTable("PG_TRX_RFD");
        dao.setColumns("*");
        dao.addWhere("vanTrxId",vanTrxId);
        RecordSet rset = dao.search();
        return rset.getRows();
    }

    public SharedMap<String,Object> getPayMap(SharedMap<String,Object> data, SharedMap<String, Object> tmnMap){
        DAO dao = new DAO();
        dao.setTable("PG_TRX_PAY");
        dao.addWhere("mchtId"	, tmnMap.getString("mchtId"));
        dao.addWhere("tmnId"	, tmnMap.getString("tmnId"));
        dao.addWhere("authCd"	, data.getString("cardApplNumber"));
        dao.addWhere("vanId"	, data.getString("mid"));
        dao.addWhere("vanTrxId"	, data.getLong("tid"));


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
}
