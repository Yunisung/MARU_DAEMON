package com.pgmate.dm.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.TrxRemoveDAO;
/*
 * Yhbae
 * 2018-08-03
 * 테스트 거래를 하루에 한번 삭제
 */
public class RemoveTestTrx {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.RemoveTestTrx.class );
	private TrxRemoveDAO dao = null;
	private static String[] CARDS = new String[] {"424242","4242"};
	
	public RemoveTestTrx() {
		dao = new TrxRemoveDAO();
		logger.debug("REMOVE TARGET BIN = [{}], last4 = [{}]", CARDS[0], CARDS[1]);
		logger.debug("REMOVE PG_TRX_PAY, {} ",dao.removeTrxPay(CARDS[0], CARDS[1]));
		logger.debug("REMOVE PG_TRX_RFD, {} ",dao.removeTrxRfd(CARDS[0], CARDS[1]));
		logger.debug("= REMOVE END");
		System.exit(1);
	}
	

	
	public static void main(String[] args) {
		new RemoveTestTrx();
		
	}

}
