package com.cosmicdan.turboshell.common.model.payload;

import lombok.Data;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
@Data
public class CalendarChangePayload implements IPayload {
	private final int mDayNum;
	private final int mMonthNum;
	private final int mYearNum;


	public CalendarChangePayload(int dayNum, int monthNum, int yearNum) {
		mDayNum = dayNum;
		mMonthNum = monthNum;
		mYearNum = yearNum;
	}
}
