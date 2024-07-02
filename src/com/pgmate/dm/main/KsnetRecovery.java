package com.pgmate.dm.main;

import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import javafx.beans.binding.ObjectExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class KsnetRecovery {
    private static Logger logger = LoggerFactory.getLogger( KsnetRecovery.class );
    private SmsGw smsGw = null;

    public KsnetRecovery() {
        smsGw = new SmsGw();
        vanTrxIdRecovery();
    }

    public static void main(String[] args) { new KsnetRecovery();}

    /**
     * 단말기 거래시 거래번호가 VAN거래번호로 들어오면서 영수증 조회 및 취소로직이 동작을 안함
     * KSNET노티 수신 받은후 거래번호만 업데이트
     */
    public void vanTrxIdRecovery() {
        try {
            List<SharedMap<String, Object>> ksnetList = getKsnetList();
            logger.info("KSNET 거래내역 : 총 {}건", ksnetList.size());

            if(ksnetList.size() == 0) {
                return;
            }

            for(SharedMap<String, Object> data : ksnetList) {
                String exeStatus = "대기";
                String summary = "";
                int retry = data.getInt("retry");

                if(!data.getString("status").equals("O")) {
                    exeStatus = "실패";
                    summary = "status 오류";
                }

                logger.info("KSNET 노티 처리 시작 : {}", data.getString("vanTransactionNo"));

                if(data.getString("approvalType").equals("1001") && data.getString("status").equals("O")) {
                    //승인
                    SharedMap<String, Object> payData = getPay(data);

                    if(CommonUtil.isNullOrSpace(payData.getString("trxId"))) {
                        //PAY에 없는 거래건 : 10회까지 재시도
                        retry = retry + 1;

                        if(retry == 10) {
                            exeStatus = "실패";
                            summary = "확인 안되는 거래건";
                        } else {
                            exeStatus = "대기";
                            summary = "확인 안되는 거래건";
                        }
                    } else {
                        //확인된거면 PAY에 업데이트
                        String trxId = payData.getString("trxId");
                        String vanTrxId = data.getString("transactionNo");

                        if(updateTrxPay(trxId, vanTrxId)) {
                            if(updateTrxCapture(trxId, vanTrxId)) {
                                exeStatus = "성공";
                                summary = trxId;
                            } else {
                                exeStatus = "실패";
                                summary = "DB 업데이트 오류 : CAP, CAP_DTL";
                            }
                        } else {
                            exeStatus = "실패";
                            summary = "DB 업데이트 오류 : PAY";
                        }
                    }

                } else if(data.getString("approvalType").equals("1011") && data.getString("status").equals("O")) {
                    //취소
                    SharedMap<String, Object> refundData = getRefund(data);

                    if(CommonUtil.isNullOrSpace(refundData.getString("trxId"))) {
                        //REFUND 에 없는 거래건 : 10회까지 재시도
                        retry = retry + 1;

                        if(retry == 10) {
                            exeStatus = "실패";
                            summary = "확인 안되는 거래건";
                        } else {
                            exeStatus = "대기";
                            summary = "확인 안되는 거래건";
                        }
                     } else {
                        //확인된거면 REFUND에 업데이트
                        String trxId = refundData.getString("trxId");
                        String vanTrxId = data.getString("transactionNo");

                        if(updateTrxRefund(trxId, vanTrxId)) {
                            if(updateTrxCapture(trxId, vanTrxId)) {
                                exeStatus = "성공";
                                summary = trxId;
                            } else {
                                exeStatus = "실패";
                                summary = "DB 업데이트 오류 : CAP, CAP_DTL";
                            }
                        } else {
                            exeStatus = "실패";
                            summary = "DB 업데이트 오류 : PAY";
                        }
                    }
                }


                //LOAD_KSNET 업데이트
                if(updateLoadKsnet(data.getString("transactionNo"), data.getString("approvalType"), exeStatus, summary, retry)) {
                    logger.info("KSNET 노티 처리 종료 : {}", data.getString("vanTransactionNo"));
                } else {
                    logger.info("KSNET 노티 처리 에러 : {}", data.getString("vanTransactionNo"));
                }
            }

        }catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public List<SharedMap<String, Object>> getKsnetList() {
        DAO dao = new DAO();
//		dao.setDebug(true);
        dao.setTable("PG_TRX_LOAD_KSNET");
        dao.setColumns("*");
        dao.addWhere("exeStatus = '대기'");
        dao.addWhere("retry < 10");
        dao.setOrderBy("tradeDate asc,tradeTime asc");
        RecordSet rset = dao.search();
        return rset.getRows();
    }

    public SharedMap<String, Object> getPay(SharedMap<String, Object> data) {
        DAO dao = new DAO();
        dao.setTable("PG_TRX_PAY");
        dao.setColumns("*");
        dao.addWhere("vanTrxId", data.getString("vanTransactionNo"));
        dao.addWhere("vanId", data.getString("storeId"));
        dao.addWhere("reqDay", data.getString("tradeDate"));
        dao.addWhere("authCd", data.getString("authNo"));
        RecordSet rset = dao.search();
        return rset.getRowFirst();
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

        String query = "UPDATE PG_TRX_LOAD_KSNET set exeStatus=?,exeDate=CURRENT_TIMESTAMP,summary=?, retry=? WHERE transactionNo=? and approvalType=?";

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
}
