package com.pgmate.dm.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;

/**
 * Create By PYS <
 * RealTimePayOutDAO Copy
 * >
 */
public class VactAuthDAO extends DAO {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public VactAuthDAO() { super.setDebug(false); }

    public List<SharedMap<String,Object>> getVactAuthFeeList(String stlDay, String stlType){
        String q = "SELECT a.mchtId, SUM(fee) AS fee, c.bankCd, c.bankName, c.account, c.accntHolder"
                +" FROM PG_VACT_AUTH a, PG_VACT_AUTH_DTL b, PG_MCHT_TAX c "
                +"WHERE a.authId = b.authId and a.mchtId = c.mchtId and b.stlDay = '"+stlDay+"' AND b.stlType = '" + stlType + "' AND b.stlStatus != '정산완료'"
                +"GROUP BY a.mchtId ";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public String getStlId(String mchtId, String stlDay, String stlType) {
        super.setTable("PG_CHARGE_SETTLE_AUTO");
        super.setColumns("stlId");
        super.addWhere("mchtId", mchtId);
        super.addWhere("stlDay", stlDay);
        super.addWhere("stlType", stlType);
        super.addWhere("status", "지급대기");
        super.addWhere("payType", "V");

        RecordSet rset = super.search();
        super.initRecord();
        if (rset.size() != 0) {
            return rset.getRow(0).getString("stlId");
        } else {
            return "";
        }
    }

    public boolean updateAuthFee(String stlId, long fee, long feeVat){
        String q = "UPDATE PG_CHARGE_SETTLE_AUTO"
                + "    SET authFee = '" + fee +"', authFeeVat = '" + feeVat +"',totalAuthFee = totalAuthFee + '" + (fee + feeVat) + "'"
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateAuthDtl(String stlId){
        String q = "UPDATE PG_VACT_AUTH_DTL"
                + "    SET stlStatus = '정산완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    public boolean updateChargeSettleAuto(String stlId) {
        String q = "UPDATE PG_CHARGE_SETTLE_AUTO"
                + "    SET status = '지급완료' "
                + "	 WHERE stlId = '" + stlId + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    /**
     * 인증 테이블 정산번호 업데이트
     * @param stlId
     * @param mchtId
     * @param stlDay
     * @param stlType
     * @return
     */
    public boolean updateAuthStlId(String stlId, String mchtId, String stlDay, String stlType){
        String q = "UPDATE PG_VACT_AUTH a, PG_VACT_AUTH_DTL b"
                + "    SET b.stlId = '" + stlId +"'"
                + "	 WHERE a.authId=b.authId and a.mchtId = '" + mchtId + "' and b.stlDay = '" + stlDay + "' and b.stlType = '" + stlType + "'";

        boolean updateed =  super.update(q);

        super.initRecord();
        return updateed;
    }

    /**
     * 가맹점 아이디로 가맹점 가상계좌 관리 테이블(PG_MCHT_MNG_VACT) 테이블 데이터 조회
     * @param mchtId : 가맹점 아이디
     * @return
     */
    public SharedMap<String, Object> getMchtMngVactByMchtId(String mchtId) {
        super.setTable("PG_MCHT_MNG_VACT");
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
     * 정산아이디 생성
     * @return
     */
    public synchronized static String getSettleId() {
        return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
    }

    public synchronized static String getVactId(){
        return "V" + getFunction("FN_NEXTVAL2", "VACT");
    }

    /**
     * 자동정산 데이터 (PG_SETTLE_AUTO) 테이블 INSERT
     * @param data
     * @return
     */
    public boolean insertSettleAuto(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE_AUTO");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }


    /**
     * 가상계좌 인증 정산 데이터 (PG_CHARGE_SETTLE) 테이블 INSERT
     * @param data
     * @return
     */
    public boolean insertChargeSettle(SharedMap<String,Object> data){
        super.setTable("PG_CHARGE_SETTLE");

        for(String key : data.keySet()){
            super.setRecord(key, data.get(key));
        }

        boolean inserted =  super.insert();

        super.initRecord();
        return inserted;
    }

    /**
     * 충전 자동정산 출금 데이터에서 출금완료하지 않은 대상건들 조회
     * @return
     */
    public List<SharedMap<String,Object>> getVactAuthPayOutList(String stlDay, String stlType){
        String q = "SELECT * "
                +"	  FROM PG_CHARGE_SETTLE_AUTO "
                +"   WHERE stlDay = '" + stlDay + "' and stlType = '" + stlType + "' and status = '지급대기'"
                +"   order by regDate;";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    public SharedMap<String, Object> getMchtBalance(String mchtId) {
        super.setTable("PG_MCHT_BALANCE");
        super.setColumns("*");
        super.addWhere("mchtId",mchtId ,eq);
        super.setOrderBy("");
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    /**
     * 해지할 가상계좌목록 조회
     * @return
     */
    public List<SharedMap<String, Object>> getTerminateAccount() {
        String q = "SELECT * "
                +"	  FROM PG_VACT_DTL "
                +"   WHERE status = '입금횟수초과' OR status='사용자만료' "
                +"   order by regDate;";

        RecordSet rset = super.query(q);
        super.initRecord();

        return rset.getRows();
    }

    /**
     * 출금계좌정보 조회
     * @param account
     * @return
     */
    public SharedMap<String, Object> getVactReg(String account) {
        super.setTable("PG_VACT_REG");
        super.setColumns("*");
        super.addWhere("account", account, eq);
        RecordSet rset = super.search();
        super.initRecord();
        if(rset.size() > 0){
            return rset.getRow(0);
        }else{
            return new SharedMap<String,Object>();
        }

    }

    /**
     * 가상계좌 출금계좌 이력 테이블 저장 (HT_VACT_REG)
     * @param mchtId
     * @param bankCd
     * @param account
     * @param trxType
     * @param withdrawBankCd
     * @param withdrawAccount
     * @param holderName
     * @param trackId
     * @param udf1
     * @param udf2
     * @param resultCd
     * @param resultMsg
     * @return
     */
    public boolean insertHtVactReg(String mchtId, String bankCd, String account, String trxType, String withdrawBankCd, String withdrawAccount,
                                   String holderName,String trackId, String udf1, String udf2, String resultCd, String resultMsg){
        boolean insert = false;

        try {
            String encAccnt = getAESEnc(withdrawAccount);

            super.setTable("HT_VACT_REG");
            super.setRecord("mchtId", mchtId);
            super.setRecord("bankCd", bankCd);
            super.setRecord("account", account);
            super.setRecord("trxType", trxType);
            super.setRecord("withdrawBankCd", withdrawBankCd);
            super.setRecord("withdrawAccount", encAccnt);
            super.setRecord("holderName", holderName);
            super.setRecord("trackId", trackId);
            super.setRecord("udf1", udf1);
            super.setRecord("udf2", udf2);
            super.setRecord("resultCd", resultCd);
            super.setRecord("resultMsg", resultMsg);
            super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
            super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));

            insert = super.insert();
            logger.info("set insertHtVactReg insert : [{}][{}][{}][{}]", mchtId,account,trxType,insert);

            super.initRecord();
        }catch(Exception ex) {
            ex.printStackTrace();
            //logger.error("insertHtVactReg Exception : {}", ex.getMessage());
            logger.error("insertHtVactReg Exception : {}", ex);
        }

        return insert;
    }

    /**
     * HT_VACT_DTL테이블에 저장
     * @param issueId	가상계좌발급번호
     * @return
     */
    public boolean insertHtVactDtl(String issueId, String resultCd, String resultMsg){
        SharedMap<String,Object> map = getVactDtl(issueId);
        super.setTable("HT_VACT_DTL");

        super.setRecord("issueId", 			map.getString("issueId"));							// 가상계좌발급번호
        super.setRecord("account", 			map.getString("account"));							// 계좌번호
        super.setRecord("vactType", 		map.getString("vactType"));						// 발행용도 임시,영구,월렛
        super.setRecord("status", 			map.getString("status"));							// 계좌상태  할당,사용만료,기한만료
        super.setRecord("mchtId", 			map.getString("mchtId"));							// 가맹점아이디
        super.setRecord("holderName", 		map.getString("holderName"));						// IR방식의 예금주명 기본값없으면 PG_MCHT_MNG_VACT.holderName 사용
        super.setRecord("amount", 			CommonUtil.parseLong(map.getString("amount")));	// 입금 예상 금액 0 : 제한없음 , 그외는 금액 일치 시
        super.setRecord("oper", 			map.getString("oper"));							// 0 이외의 금액에 대해서 eq, gt 보다클때,ge 크거나같을때,  lt 작을때,le 작거나 같을때
        super.setRecord("trackId", 			map.getString("trackId"));							// 임시,영구의 경우 가맹점 주문번호, 월렛의 경우 터미널ID
        super.setRecord("depositCnt", 		map.getInt("depositCnt"));							// 입금횟수
        super.setRecord("depositLimitCnt", 	map.getInt("depositLimitCnt"));					// 입금제한횟수
        super.setRecord("expireAt", 		map.getString("expireAt"));						// 만료예상시간
        super.setRecord("expireDate", 		map.getTimestamp("expireDate"));					// 만료일자
        super.setRecord("udf1",				map.getString("udf1"));							// 가맹점 사용 필드1
        super.setRecord("udf2", 			map.getString("udf2"));							// 가맹점 사용 필드2
        super.setRecord("reason", 			map.getString("reason"));							// 변경사유
        super.setRecord("resultCd", 		resultCd);												// 응답코드
        super.setRecord("resultMsg", 		resultMsg);												// 응답메세지
        super.setRecord("regId", 		"SYSTEM");											// 등록자아이디
        super.setRecord("regDay", 			CommonUtil.getCurrentDate("yyyyMMdd"));			// 등록일

        boolean insert = super.insert();
        logger.info("set HT_VACT_DTL insert : {}", insert);

        super.initRecord();
        return insert;
    }

    public SharedMap<String,Object> getVactDtl(String issueId) {
        super.setTable("PG_VACT_DTL");
        super.addWhere("issueId", issueId, eq);

        super.setLimit(1);

        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public boolean deleteVactDtl(String issueId){
        super.setTable("PG_VACT_DTL");
        super.addWhere("issueId", issueId);
        boolean deleted = super.delete();
        super.initRecord();

        return deleted;
    }

    public boolean deleteVactReg(String account, String withdrawBankCd, String withdrawAccount, String holderName) {
        String encAccnt = getAESEnc(withdrawAccount);

        super.setTable("PG_VACT_REG");
        super.addWhere("account", account);
        super.addWhere("withdrawBankCd", withdrawBankCd);
        super.addWhere("withdrawAccount", encAccnt);
        super.addWhere("holderName", holderName);

        boolean deleted = super.delete();
        super.initRecord();
        logger.info("set deleteVactReg delete : [{}][{}][{}][{}][{}]", account, withdrawBankCd, withdrawAccount, holderName, deleted);

        return deleted;
    }

    public boolean updateVactDtl(String issueId, String holderName) {
        super.setTable("PG_VACT_DTL");

        super.setRecord("status", "대기");
        super.setRecord("amount", 0);
        super.setRecord("oper", "ge");
        super.setRecord("trackId", "");
        super.setRecord("holderName", holderName);
        super.setRecord("depositCnt", 0);
        super.setRecord("udf1", "");
        super.setRecord("udf2", "");
        super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
        super.addWhere("issueId", 	issueId);
        boolean update = super.update();
        logger.info("set PG_VACT_DTL close : {}",update );

        super.initRecord();
        return update;
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

}
