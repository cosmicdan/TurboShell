package com.cosmicdan.turboshell.models.payloads;

import lombok.Data;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
@Data
public class WindowTitleChangePayload implements IPayload {
	private final String mWindowTitle;

	public WindowTitleChangePayload(final String windowTitle) {
		mWindowTitle = windowTitle;
	}
}
