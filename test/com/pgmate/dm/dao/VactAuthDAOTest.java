package com.pgmate.dm.dao;

import com.pgmate.lib.util.map.SharedMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class VactAuthDAOTest {

    private static Logger logger = LoggerFactory.getLogger(VactAuthDAOTest.class);

    VactAuthDAO vactAuthDAO;

    @Before
    public void init() {
        vactAuthDAO = new VactAuthDAO();
        vactAuthDAO.setDebug(true);
    }

    @Test
    public void 해지출금계좌_조회() {
        List<SharedMap<String, Object>> list = vactAuthDAO.getTerminateAccount(10);
        logger.debug("전체건수 trxId {}", list.size());
        assertTrue(list.size() > 0);
    }

    @Test
    public void 출금만료시_히스토리저장() {
        String issueId = "VI221124060118";
        vactAuthDAO.insertHtVactDtl(issueId, "0000", "사용자테스트");
    }

    @Test
    public void 수수료율확인() {
        long result = calcFeeVat(1000, 0.0168);
        logger.debug("수수료율확인 result {}", result);
    }

    public long calcVat(long amount){
        if(amount < 0){
            return -new Double(-amount *10 /100).longValue();
        }else{
            return new Double(amount *10 /100).longValue();
        }
    }
    public long calcFeeVat(long amount,double rate){
        rate = rateFormat(rate);
        long decimal = 10000;
        long fee = 0;
        if(amount < 0){
            fee = -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
        }else{
            fee = new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
        }
        //long vat = calcVat(fee);
        return fee;
    }

    private double rateFormat(double rate){
        String pattern = "#.#####";
        DecimalFormat format = new DecimalFormat(pattern);
        return new Double(format.format(rate)).doubleValue();
    }



}
