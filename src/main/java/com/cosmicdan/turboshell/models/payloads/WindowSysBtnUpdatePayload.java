package com.cosmicdan.turboshell.models.payloads;

import com.cosmicdan.turboshell.models.data.WindowInfo.Flag;
import lombok.Getter;

import java.util.EnumSet;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
public class WindowSysBtnUpdatePayload implements IPayload {
	@Getter
	private final EnumSet<Flag> mFlags;

	public WindowSysBtnUpdatePayload(final EnumSet<Flag> flags) {
		mFlags = flags.clone();
	}
}
