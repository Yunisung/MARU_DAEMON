package com.pgmate.dm.dao;

import java.util.List;

import com.pgmate.lib.util.map.SharedMap;

public interface ImplVaSettle {
	public List<SharedMap<String,Object>> getSettleList(String stlDay);
	public void setSettleToIdx(String stlId,String stlDay,String mchtId);
	public void updateTrx(String stlId);
	public boolean insertVaSettle(SharedMap<String,Object> data);
	public String getFunction(String function,String value);
	public String getSettleId();
}
