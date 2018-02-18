package com.cosmicdan.turboshell.models;

import lombok.Getter;

/**
 * TurboBar configuration storage
 */
public class TurboBarConfig {
	/////////////////// "Initialization-on-demand holder" singleton pattern
	public static TurboBarConfig getInstance() { return LazyHolder.INSTANCE; }
	private TurboBarConfig() {}
	private static class LazyHolder { static final TurboBarConfig INSTANCE = new TurboBarConfig();}
	///////////////////

	@Getter
	private final int mBarHeight = 25;
}
