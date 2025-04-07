package com.pgmate.dm.main;

import com.pgmate.dm.dao.VactRiskCheckDAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class VactRiskCheck {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VactRiskCheck.class );

    public VactRiskCheck() {
        vactTrxRiskCheck();
//        vactRegRiskCheck();
    }

    public void vactTrxRiskCheck() {
        VactRiskCheckDAO vactRiskCheckDao = new VactRiskCheckDAO();
        // 출금계좌 입금 횟수 10회, 금액 4000만원 초과
        List<SharedMap<String, Object>> withdrawAccountTotalList = vactRiskCheckDao.getWithdrawAccountTotalList();
        // 가상계좌 입금 횟수 10회, 금액 4000만원 초과
        List<SharedMap<String, Object>> accountTotalList = vactRiskCheckDao.getAccountTotalList();
        // 출금계좌 입금 1일 한도 3000만원 초과
        List<SharedMap<String, Object>> limitDayList = vactRiskCheckDao.getLimitDayList();
        // 출금계좌 입금 1회 한도 300만원 초과
        List<SharedMap<String, Object>> limitOnceList = vactRiskCheckDao.getLimitOnceList();
        // 출금계좌 입금 횟수 7회 초과
        List<SharedMap<String, Object>> limitCountList = vactRiskCheckDao.getLimitCountList();

        SharedMap<String, SharedMap> riskMap = new SharedMap<>();

        for(SharedMap<String, Object> withdrawAccountTotalMap : withdrawAccountTotalList) {
            withdrawAccountTotalMap.put("risk", "출금계좌 입금 횟수, 금액 초과");
            logger.info("vactId : {}, withdrawAccount : {}, risk : {}", withdrawAccountTotalMap.getString("vactId"), withdrawAccountTotalMap.getString("withdrawAccount"), withdrawAccountTotalMap.getString("risk"));
            riskMap.put(withdrawAccountTotalMap.getString("vactId"), withdrawAccountTotalMap);
        }
        for(SharedMap<String, Object> accountTotalMap : accountTotalList) {
            accountTotalMap.put("risk", "가상계좌 입금 횟수, 금액 초과");
            riskMap.put(accountTotalMap.getString("vactId"), accountTotalMap);
        }
        for(SharedMap<String, Object> limitDayMap : limitDayList) {
            limitDayMap.put("risk", "출금계좌 입금 1일 한도 초과");
            riskMap.put(limitDayMap.getString("vactId"), limitDayMap);
        }
        for(SharedMap<String, Object> limitOnceMap : limitOnceList) {
            limitOnceMap.put("risk", "출금계좌 입금 1회 한도 초과");
            riskMap.put(limitOnceMap.getString("vactId"), limitOnceMap);
        }
        for(SharedMap<String, Object> limitCountMap : limitCountList) {
            limitCountMap.put("risk", "출금계좌 입금 횟수 초과");
            riskMap.put(limitCountMap.getString("vactId"), limitCountMap);
        }

        vactRiskCheckDao.insertVactTrxRisk(riskMap);
    }

    private void vactRegRiskCheck() {
        VactRiskCheckDAO vactRiskCheckDao = new VactRiskCheckDAO();
        SharedMap<String, SharedMap> riskMap = new SharedMap<>();

        List<SharedMap<String, Object>> regLimitList = vactRiskCheckDao.getRegLimitList();
        logger.info("size : {}",regLimitList.size());
        for (SharedMap<String, Object> regLimitMap : regLimitList) {
            regLimitMap.put("risk", "출금계좌 발급 횟수 초과");
            riskMap.put(regLimitMap.getString("issueId"), regLimitMap);
        }

        vactRiskCheckDao.insertVactRegRisk(riskMap);
    }

    public static void main(String[] args) {
        new VactRiskCheck();
    }
}
