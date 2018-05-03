package com.cosmicdan.turboshell.models;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration storage
 * @author Daniel 'CosmicDan' Connolly
 */
@UtilityClass
public final class TurboShellConfig {
	@Getter private static final int mTurboBarHeight = 25;

	// Theme stuff
	@Getter private static final String mTurboBarCloseBgPrimary = "a03048";
	@Getter private static final String mTurboBarCloseBgSecondary = "e81123";

	@Getter private static final Set<String> mFullscreenHideExcludeClasses = new HashSet<>(
			Arrays.asList("WorkerW", "Progman")
	);
}
