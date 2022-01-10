package com.pgmate.dm.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.bean.SmsToken;
import com.pgmate.lib.dao.DAO;

/**
 * @author Administrator
 *
 */
public class InfoBankSmsTokenDAO extends DAO{

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.InfoBankSmsTokenDAO.class );

	public InfoBankSmsTokenDAO() {
	}
	
	public void updateToken(SmsToken token) {
		String query = "UPDATE PG_SMS_TOKEN SET "
				+ "`schema` = '"+token.schema+"', "
				+ "`expired` = '"+token.expired+"' ,"
				+ "`accessToken` = '"+token.accessToken+"', "
				+ "`regDate` = CURRENT_TIMESTAMP() "
				+ "WHERE 1 = 1 ;"; 
		
		
		logger.info("UPDATE PG_SMS_TOKEN [{}]",super.update(query));
		super.initRecord();
	}

}


