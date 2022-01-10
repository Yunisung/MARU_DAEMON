package com.pgmate.dm.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.dm.dao.VactHookDAO;
import com.pgmate.lib.util.map.SharedMap;


public class VactHookRetry {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.dm.main.VactHookRetry.class );

	public VactHookRetry() {
		noti();
	}
	
	public void noti(){
		try {
			VactHookDAO dao = new VactHookDAO();
			
			List<SharedMap<String,Object>> notiList = dao.getRetryList();
			if(notiList.size() > 0) {
				logger.info("VactHook retry Count [{}]",notiList.size());
				for(SharedMap<String, Object> sharedMap:notiList){
					try{Thread.sleep(100);}catch(Exception e){};
					new VactHook(sharedMap,dao).start();
				}
			}
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		
	}
	
	public static void main(String[] args) {
		VactHookRetry d = new VactHookRetry();
	}

}
