package com.pgmate.dm.main;

import com.pgmate.dm.dao.RealTimePayOutDAO;
import com.pgmate.dm.dao.VactAuthDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CreateBy PYS
 * 가상계좌 인증 수수료 정산하는게 없어서 만듬
 * 현재 충전정산만 쓰기때문에 수수료를 PG_MCHT_BALANCE에 UPDATE하는 방향으로 개발
 */
public class VactAuthPayOut {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private SmsGw smsGw = null;

    private String stlDay = "";
    private String stlType = "";
    private String msgBody = "";

    public static void main(String[] args) { new VactAuthPayOut(); }

    public VactAuthPayOut() {
        logger.info("==============================");
        logger.info("VactAuthPayOut Start");
        smsGw = new SmsGw();

        vactAuthFee();
        vactAuthPayOut();

        logger.info("VactAuthPayOut End");
        logger.info("==============================");
    }

    public void vactAuthFee() {

        stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
        //현재는 충전정산만 사용, 나중에 바뀔수 있음.
        stlType = "C+0";

        VactAuthDAO dao = new VactAuthDAO();

        List<SharedMap<String,Object>> getVactAuthFeeList = dao.getVactAuthFeeList(stlDay, stlType);
        //mchtId, fee, bankCd, bankName, account, acctHolder
        logger.info("getVactAuthFeeList COUNT : {}", getVactAuthFeeList.size());

        if(getVactAuthFeeList.size() > 0) {
            logger.info("=================================================");
            logger.info("{} 통합인증 수수료 처리 시작", stlDay);
            logger.info("=================================================");

            for(SharedMap<String, Object> data : getVactAuthFeeList) {

                String stlId = dao.getStlId(data.getString("mchtId"), stlDay, stlType);

                if(!"".equals(stlId)) {
                    //충전정산은 하루뒤에 정산하므로 이쪽으로 오기는 힘들듯
                    dao.updateAuthFee(stlId, data.getLong("fee"), calcVat(data.getLong("fee")));
                    dao.updateAuthStlId(stlId, data.getString("mchtId"), stlDay, stlType);

                    logger.info("가상계좌 인증 수수료 UPDATE");
                    logger.info("stlId  : {}",stlId);
                    logger.info("mchtId : {}",data.getString("mchtId"));
                    logger.info("orgFee : {}",data.getString("fee"));
                    logger.info("==================================================");
                }else {
                    SharedMap<String,Object> mchtMngVactMap	= dao.getMchtMngVactByMchtId(data.getString("mchtId"));
                    //PG_CHARGE_SETTLE_AUTO에 넣을 데이터 세팅

                    SharedMap<String,Object> settleData = new SharedMap<>();
                    stlId = dao.getSettleId();

                    settleData.put("stlId", stlId);
                    settleData.put("mchtId", data.getString("mchtId"));
                    settleData.put("status", "지급대기");
                    settleData.put("stlDay", stlDay);
                    settleData.put("payType", "V");
                    settleData.put("startDay", stlDay);
                    settleData.put("endDay", stlDay);

                    settleData.put("authFee", data.getLong("fee"));
                    settleData.put("authFeeVat", calcVat(data.getLong("fee")));
                    settleData.put("totalAuthFee", data.getLong("fee") + calcVat(data.getLong("fee")));

                    settleData.put("payAmt", 0);
                    settleData.put("payFee", 0);
                    settleData.put("payVat", 0);
                    settleData.put("payCnt", 0);

                    settleData.put("rfdAmt", 0);
                    settleData.put("rfdFee", 0);
                    settleData.put("rfdVat", 0);
                    settleData.put("rfdCnt", 0);

                    settleData.put("stlAmount", 0);
                    settleData.put("stlRate", mchtMngVactMap.getDouble("rate"));
                    settleData.put("stlType", stlType);
                    settleData.put("regId", "SYSTEM");

                    logger.info("가상계좌 인증 수수료 INSERT");
                    logger.info("stlId  	  : {}",stlId);
                    logger.info("mchtId  	  : {}",data.getString("mchtId"));
                    logger.info("totalAuthFee : {}",settleData.getLong("totalAuthFee"));

                    if(dao.insertSettleAuto(settleData)) {
                        dao.updateAuthStlId(stlId, data.getString("mchtId"), stlDay, stlType);
                    } else {
                        msgBody = "PG_CHARGE_SETTLE_AUTO VACT INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
                    }

                    if(!"".equals(msgBody)) {
                        logger.info(msgBody);
                        smsGw.sendMessage("0", "4", msgBody);
                    }

                    logger.info("==================================================");
                }
            }

        }
    }

    public void vactAuthPayOut() {
        //PG_CHARGE_SETTLE_AUTO에 있는걸 정산완료 처리한다.
        stlType = "C+0";
        stlDay = CommonUtil.getCurrentDate("yyyMMdd");



        VactAuthDAO dao = new VactAuthDAO();
        List<SharedMap<String, Object>> getVactAuthPayOutList = dao.getVactAuthPayOutList(stlDay, stlType);

        logger.info( "통합인증 수수료 정산 대상 건수 : {}", getVactAuthPayOutList.size());

        if(getVactAuthPayOutList.size() > 0) {
            logger.info("=================================================");
            logger.info("{} 통합인증 수수료 정산 시작", stlDay);
            logger.info("=================================================");

            for(SharedMap<String, Object> data : getVactAuthPayOutList) {
                boolean errFlag = false;
                String stlStatus = "지급완료";

                if(data.getLong("totalAuthFee") > 0) {
                    //1. 해당금액을 PG_CHARGE_SETTLE에 출금으로 추가하기
                    //2. PG_VACT_AUTH_DTL에 정산완료 처리
                    //3. PG_CHARGE_SETTLE_AUTO에 지급완료 처리

                    SharedMap<String, Object> mchtBalance = dao.getMchtBalance(data.getString("mchtId"));

                    long balance = mchtBalance.getLong("balance") - data.getLong("totalAuthFee");

                    //PG_CHARGE_SETTLE 데이터 만들기
                    SharedMap<String, Object> trxMap = new SharedMap<>();
                    trxMap.put("trxId", dao.getVactId());
                    trxMap.put("mchtId", data.getString("mchtId"));
                    trxMap.put("trxType", "출금");
                    trxMap.put("trxUnit", "인증수수료");

                    String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
                    trxMap.put("trxDay", regDate.substring(0, 8));
                    trxMap.put("trxTime", regDate.substring(8));
                    trxMap.put("amount", data.getLong("totalAuthFee"));
                    trxMap.put("fee", "0");
                    trxMap.put("feeVat", "0");
                    trxMap.put("bankFee", "0");

                    trxMap.put("netAmount", data.getLong("totalAuthFee"));
                    trxMap.put("balance", balance);
                    trxMap.put("trackId", data.getString("stlId"));

                    trxMap.put("summary", "가상계좌 인증 수수료 대금");
                    trxMap.put("regId", "SYSTEM");
                    trxMap.put("regDay", regDate.substring(0, 8));

                    if(dao.insertChargeSettle(trxMap)) {
                        //2. PG_VACT_AUTH_DTL에 정산완료 처리
                        //3. PG_CHARGE_SETTLE_AUTO에 지급완료 처리
                        if(dao.updateAuthDtl(data.getString("stlId"))) {
                            if(!dao.updateChargeSettleAuto(data.getString("stlId"))) {
                                msgBody = "PG_CHARGE_SETTLE_AUTO 지급완료처리 실패. 확인요망 [" + stlDay + "][" + data.getString("stlId") + "]";
                            }
                        } else {
                            msgBody = "PG_VACT_AUTH_DTL 정산완료처리 실패. 확인요망 [" + stlDay + "][" + data.getString("stlId") + "]";
                        }
                    } else {
                        msgBody = "PG_CHARGE_SETTLE VACT INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
                    }

                    if(!"".equals(msgBody)) {
                        logger.info(msgBody);
                        smsGw.sendMessage("0", "4", msgBody);
                    }

                    logger.info("==================================================");

                }
            }
        }


    }


    public long calcVat(long fee){
        if(fee < 0){
            return -new Double(-fee *10 /100).longValue();
        }else{
            return new Double(fee *10 /100).longValue();
        }
    }
}
