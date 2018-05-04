package com.cosmicdan.turboshell.models;

import com.cosmicdan.turboshell.models.payloads.CalendarChangePayload;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2(topic = "CalendarAgent")
public final class CalendarAgent extends AgentModel {
	public static final CalendarAgent INSTANCE = new CalendarAgent();

	private static final long UPDATE_RATE_MS = 500;

	private volatile boolean shouldRun = true;

	public CalendarAgent() {}

	@SuppressWarnings("BusyWait")
	@Override
	protected void serviceStart() {
		log.info("Starting...");

		int lastDayNum = -1;
		int lastMonthNum = -1;
		int lastYearNum = -1;

		int currentDayNum;
		int currentMonthNum;
		int currentYearNum;

		while (shouldRun) {
			final LocalDateTime dateTime = LocalDateTime.now();
			currentDayNum = dateTime.getDayOfMonth();
			currentMonthNum = dateTime.getMonthValue();
			currentYearNum = dateTime.getYear();

			if ((currentDayNum != lastDayNum) || (currentMonthNum != lastMonthNum) || (currentYearNum != lastYearNum)) {
				lastDayNum = currentDayNum;
				lastMonthNum = currentMonthNum;
				lastYearNum = currentYearNum;
				//noinspection ObjectAllocationInLoop
				INSTANCE.runCallbacks(new CalendarChangePayload(lastDayNum, lastMonthNum, lastYearNum));
			}


			try {
				Thread.sleep(UPDATE_RATE_MS);
			} catch (final InterruptedException ignored) {}
		}
	}

	@Override
	protected void serviceStop() {
		shouldRun = false;
	}
}
