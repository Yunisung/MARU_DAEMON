package com.pgmate.dm.dao;

import com.pgmate.lib.util.map.SharedMap;

import java.util.List;

public interface ImplRentSettle {
	public List<SharedMap<String,Object>> getSettleList(String stlDay);
	public void setSettleToIdx(String stlId,String stlDay,String mchtId);
	public void updateTrxCap(String stlId);
	public boolean insertRentSettle(SharedMap<String,Object> data);
	public String getFunction(String function,String value);
	public String getSettleId();
}
