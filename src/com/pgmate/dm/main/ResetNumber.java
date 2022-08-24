package com.pgmate.dm.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.ResetNumDAO;

public class ResetNumber {
	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Daemon.class );
	
	public static void main(String[] args) {
		logger.info("Reset Start !!!");
		
		ResetNumDAO dao = new ResetNumDAO();
		dao.updateNumber();
	}
}
