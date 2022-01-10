package com.pgmate.dm.dao;

import java.util.List;

import com.pgmate.lib.util.map.SharedMap;

public interface ImplVactSettle {
	public List<SharedMap<String,Object>> getSettleList(String stlDay);
	public void setSettleToIdx(String stlId,String stlDay,String mchtId);
	public void updateTrxCap(String stlId);
	public boolean insertVactSettle(SharedMap<String,Object> data);
	public String getFunction(String function,String value);
	public String getSettleId();
}
