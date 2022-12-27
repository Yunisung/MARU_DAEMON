package com.pgmate.dm.main;

import com.pgmate.dm.dao.ChargeSettlePayOutDAO;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        SharedMap<String, Object> chargeData = dao.getChargeSettle(trxId);

        if(chargeData.getString("trxType").equals("출금")) {
            //실패거래건의 실출금액 조회
            long netAmt = chargeData.getLong("netAmount");
            //실패거래건의 실출금액 만큼 해당 계정의 이후 결제건의 잔액에 더해줌
            dao.updateChargeSettleBalance(trxId, chargeData.getString("mchtId"), netAmt);
            //실패건 PG_CHARGE_SETTLE 테이블에서 삭제
            dao.deleteChargeSettle(trxId);

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

    }
}
