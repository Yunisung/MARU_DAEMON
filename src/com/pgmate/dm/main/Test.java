package com.pgmate.dm.main;

import java.util.Calendar;
import java.util.regex.Pattern;

import com.pgmate.dm.dao.TotalDAO;
import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class Test {

	/**
	 * 
	 */
	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args){
		String cmd2 = CommonUtil.getCurrentDate("yyyyMMdd");
		System.out.println(cmd2);
		if(Pattern.matches("^[0-9]{8}$", cmd2)){
			for(int i = -7; i <= -1; i++) {
				String curDate = CommonUtil.getOpDate(Calendar.DATE, i,cmd2);
				System.out.println(curDate);
			}
		}else {
			System.out.println(cmd2);
		}
	}

}
