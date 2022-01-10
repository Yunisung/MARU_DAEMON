package com.pgmate.dm.main;

import com.pgmate.lib.util.map.SharedCacheMap;

/**
 * @author Administrator
 *
 */
public class Cache {
	
	public static SharedCacheMap map	= new SharedCacheMap(10);	//CACHE TIMEOUT 5 MINUTES
}
