package com.pgmate.dm.main;

import com.pgmate.dm.dao.TotalAuthDAO;
import com.pgmate.dm.util.SmsGw;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Create By PYS
 * 통합인증 수수료 자동 정산기능
 * 통합인증 수수료는 PG_TOTAL_AUTH에 존재
 */
public class TotalAuthPayOut {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private SmsGw smsGw = null;

    private String stlDay = "";
    private String day = "";

    public static void main(String[] args) { new TotalAuthPayOut();}

    public TotalAuthPayOut() {
        smsGw = new SmsGw();
        stlDay = CommonUtil.getCurrentDate("yyyyMMdd");
        day = stlDay.substring(0,4) + "년 " + stlDay.substring(4,6) + "월 " + stlDay.substring(6) + "일";

        logger.info("=== === === [{}] 통합인증 수수료 정산 시작 === === ===", day);

        totalAuthSettleExcute();
        logger.info("=== === === [{}] 통합인증 수수료 정산 종료 === === ===", day);
        logger.info("=== === === [{}] 통합인증 수수료 정산 차감 시작 === === ===", day);
        totalAuthPayOut();

        logger.info("=== === === [{}] 통합인증 수수료 정산 차감 종료 === === ===", day);
    }

    public void totalAuthSettleExcute() {
        TotalAuthDAO dao = new TotalAuthDAO();

        List<SharedMap<String, Object>> getTotalAuthFeeList = dao.getTotalAuthFeeList(stlDay);

        if(getTotalAuthFeeList.size() > 0) {

            for(SharedMap<String, Object> data : getTotalAuthFeeList) {

                SharedMap<String, Object> settleData = new SharedMap<>();
                String stlId = dao.getSettleId();

                String mchtId = data.getString("mchtId");
                String stlType = data.getString("stlType");

                settleData.put("stlId", stlId);
                settleData.put("mchtId", mchtId);
                settleData.put("status", "지급대기");
                settleData.put("stlDay", stlDay);
                settleData.put("payType", "V");
                settleData.put("startDay", stlDay);
                settleData.put("endDay", stlDay);

                settleData.put("authFee", data.getLong("authFee"));
                settleData.put("authFeeVat", data.getLong("authFeeVat"));
                settleData.put("totalAuthFee", data.getLong("authFee") + data.getLong("authFeeVat"));

                settleData.put("payAmt", 0);
                settleData.put("payFee", 0);
                settleData.put("payVat", 0);
                settleData.put("payCnt", 0);

                settleData.put("rfdAmt", 0);
                settleData.put("rfdFee", 0);
                settleData.put("rfdVat", 0);
                settleData.put("rfdCnt", 0);

                settleData.put("stlAmount", 0);
                settleData.put("stlRate", 0);
                settleData.put("stlType", stlType);
                settleData.put("regId", "SYSTEM");

                if(dao.insertSettleAuto(settleData)) {
                    dao.updateTotalAuthStlId(stlId, mchtId, stlDay, stlType);
                } else {
                    logger.info("통합인증 수수료 정산 에러. 확인요망 [{}][{}][{}]",stlId, mchtId, stlType);
                }

            }
        }

    }

    public void totalAuthPayOut() {
        String errMsg = "";
        String stlType = "B+1";

        TotalAuthDAO dao = new TotalAuthDAO();
        List<SharedMap<String, Object>> getTotalAuthPayOutList = dao.getTotalAuthPayOutList(stlDay, stlType);

        if(getTotalAuthPayOutList.size() > 0) {
            for(SharedMap<String, Object> data : getTotalAuthPayOutList) {
                if(data.getLong("totalAuthFee") > 0) {

                    SharedMap<String, Object> mchtBalance = dao.getMchtBalance(data.getString("mchtId"));
                    long balance = mchtBalance.getLong("balance") - data.getLong("totalAuthFee");

                    //PG_CHARGE_SETTLE 데이터 만들기
                    SharedMap<String, Object> trxMap = new SharedMap<>();
                    trxMap.put("trxId", dao.getChargeSettleTrxId());
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
                        if(dao.updateTotalAuth(data.getString("stlId"))) {
                            if(!dao.updateChargeSettleAuto(data.getString("stlId"))) {
                                errMsg = "PG_CHARGE_SETTLE_AUTO 지급완료처리 실패. 확인요망 [" + stlDay + "][" + data.getString("stlId") + "]";
                            }
                        } else {
                            errMsg = "PG_TOTAL_AUTH 정산완료처리 실패. 확인요망 [" + stlDay + "][" + data.getString("stlId") + "]";
                        }
                    } else {
                        errMsg = "PG_CHARGE_SETTLE  INSERT 실패. 확인요망 [" + stlDay + "][" + data.getString("mchtId") + "]";
                    }

                    if(!"".equals(errMsg)) {
                        logger.info(errMsg);
                    }
                }
            }
        }

    }
}
