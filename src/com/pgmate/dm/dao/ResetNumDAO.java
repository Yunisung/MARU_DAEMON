package com.pgmate.dm.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;

public class ResetNumDAO extends DAO{
	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.dao.ResetNumDAO.class );
	
	public void updateNumber() {
		logger.info("reset number !!! " );
		
		super.update("UPDATE PG_SEQ SET curVal=1 WHERE NAME='BANK'");
		super.initRecord();
	}
}
