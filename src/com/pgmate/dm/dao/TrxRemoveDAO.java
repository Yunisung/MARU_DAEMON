package com.pgmate.dm.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class TrxRemoveDAO extends DAO {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.TrxRemoveDAO.class );
	
	public TrxRemoveDAO() {
		super.setDebug(true);
	}
	
	public boolean removeTrxPay(String bin, String last4) {
		if(bin.length() != 6) return false;
		return super.update("DELETE FROM PG_TRX_PAY WHERE reqDay > DATE_FORMAT(CURRENT_TIMESTAMP() + INTERVAL - 7 DAY, '%Y%m%d') AND bin = '" + bin + "' AND last4 = '" + last4 + "' ");
	}
	
	public boolean removeTrxRfd(String bin, String last4) {
		if(bin.length() != 6) return false;
		return super.update("DELETE FROM PG_TRX_RFD WHERE reqDay > DATE_FORMAT(CURRENT_TIMESTAMP() + INTERVAL - 7 DAY, '%Y%m%d') AND bin = '" + bin + "' AND last4 = '" + last4 + "' ");
	}
}
