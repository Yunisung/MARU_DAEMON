package com.pgmate.dm.main;

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChargeSettleRecovery {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.ChargeSettleRecovery.class);

    public static void main(String[] args){
        new ChargeSettleRecovery(args[0]);
    }

    public ChargeSettleRecovery(String trxId) {
        logger.info("============================");
        logger.info("충전정산 잔액복구 : {}", trxId);
        logger.info("============================");
        ChargeSettlePayOutDAO dao = new ChargeSettlePayOutDAO();
        List<SharedMap<String, Object>> errData = dao.getChargeSettleErr(trxId);

        if(errData.size() > 0) {
            SharedMap<String, Object> chargeData = dao.getChargeSettle(trxId);

            if(chargeData.getString("trxType").equals("출금")) {
                //실패거래건의 실출금액 조회
                long netAmt = chargeData.getLong("netAmount");
                //실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌
                dao.updateChargeSettleBalance(trxId, chargeData.getString("mchtId"), netAmt);
                //실패건 PG_CHARGE_SETTLE 테이블에서 삭제
                dao.deleteChargeSettle(trxId);

                // 출금 실패결과 noti 발송
                String mchtId = chargeData.getString("mchtId");
                SharedMap<String, Object> chargeMngMap = dao.getMchtChargeMng(mchtId);
                if(!CommonUtil.isNullOrSpace(chargeMngMap.getString("hookAddr"))) {
                    String payLoad = setPayLoad(chargeData, "출금실패", "9999", "가맹점 잔액 복구완료");
                    chargeData.put("payLoad", payLoad);
                    chargeData.put("trxType", "출금");
                    new ChargeSettleHook(chargeMngMap.getString("hookAddr"), chargeData, dao, "0").start();
                }

                logger.info("============================");
                logger.info("충전정산 복구완료");
                logger.info("가맹점 ID : {}", chargeData.getString("mchtId"));
                logger.info("복구금액 : {}", netAmt);
                logger.info("============================");
            } else {
                logger.info("============================");
                logger.info("충전정산 복구실패");
                logger.info("출금 거래건만 가능합니다");
                logger.info("============================");
            }
        } else {
            logger.info("============================");
            logger.info("충전정산 복구실패");
            logger.info("PG_CHARGE_SETTLE_ERR에 없는 거래번호입니다");
            logger.info("============================");
        }


    }

    public String setPayLoad(SharedMap<String, Object> sharedMap, String status, String resultCd, String resultMsg){
        SharedMap<String, String> payLoadMap = new SharedMap<String, String>();

        payLoadMap.put("mchtId",sharedMap.getString("mchtId"));
        payLoadMap.put("trxId",sharedMap.getString("trxId"));
        payLoadMap.put("trxDay",sharedMap.getString("trxDay"));
        payLoadMap.put("trxTime",sharedMap.getString("trxTime"));
        payLoadMap.put("status",status);
        payLoadMap.put("trackId",sharedMap.getString("trackId"));
        payLoadMap.put("resultCd",resultCd);
        payLoadMap.put("resultMsg",resultMsg);
        payLoadMap.put("amount",sharedMap.getString("amount"));
        String payLoad = CommonUtil.toQueryString(payLoadMap,"UTF-8");
        return payLoad;
    }
}
