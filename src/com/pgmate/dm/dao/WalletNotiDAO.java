package com.pgmate.dm.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;

public class WalletNotiDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.Daemon.class );
	public WalletNotiDAO() {
		super.setDebug(false);
	}
	

	public void updateWalletNoti(SharedMap<String, Object> sharedMap) {
		String query = "UPDATE WL_TRX_NOTI SET retry = retry + 1, status='"+sharedMap.getString("status")+"', resData = '"+sharedMap.getString("resData")+"', code = '"+sharedMap.getString("code")+"' WHERE  trxId = '"+sharedMap.getString("trxId")+"'";
		logger.info("update WL_TRX_NOTI : {}", super.update(query));

		super.initRecord();
	}
	

	public List<SharedMap<String, Object>> getNotiList() {
		super.setTable("WL_TRX_NOTI");
		super.setColumns("*");
		super.addWhere("status","전송실패",eq);
		super.addWhere("reTry < 10");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRows();
	}
	
}
