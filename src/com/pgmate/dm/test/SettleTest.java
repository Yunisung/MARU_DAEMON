package com.pgmate.dm.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.pgmate.dm.main.Settle;

public class SettleTest {

	public static void main(String[] args) {
		new Settle("20180806");
		/*String startDay = "20171201";
		String endDay = "20171227";
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		try {
			Date date = df.parse(startDay);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			
			for (int j = 1; j < 110; j++) {
				cal.add(Calendar.DATE, 1);
				String strDate = df.format(cal.getTime());
				if(strDate.equals(endDay)) {
					break;
				}
				//int dayNum = cal.get(Calendar.DAY_OF_WEEK);
				new Settle(strDate);
				System.out.println("========================== " + j);
			}
		}catch (Exception e) {
			// TODO: handle exception
		}*/
	}
}
