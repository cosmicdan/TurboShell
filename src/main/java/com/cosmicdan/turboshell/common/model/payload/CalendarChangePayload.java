package com.cosmicdan.turboshell.common.model.payload;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
public class CalendarChangePayload implements IPayload {
	private final int mDayNum;
	private final int mMonthNum;
	private final int mYearNum;


	public CalendarChangePayload(final int dayNum, final int monthNum, final int yearNum) {
		mDayNum = dayNum;
		mMonthNum = monthNum;
		mYearNum = yearNum;
	}

	public String getStringForView() {
		return String.format("%d-%02d-%02d", mYearNum, mMonthNum,mDayNum);
	}
}
