package com.cosmicdan.turboshell;

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

	//////////////////////////////////////////////////////////////
	// Theme
	//////////////////////////////////////////////////////////////

	// General
	@Getter private static final String mTextColorMain = "bbb";

	// SysBtns
	@Getter private static final String mTurboBarCloseBgPrimary = "a03048";
	@Getter private static final String mTurboBarCloseBgSecondary = "e81123";

	// Seperators
	@Getter private static final double mTurboBarVerticalSeparatorMarginX = 5.0;
	@Getter private static final double mTurboBarVerticalSeparatorMarginY = 5.0;
	@Getter private static final String mTurboBarVerticalSeparatorColor = "ffffff";
	@Getter private static final double mTurboBarVerticalSeparatorOpacity = 0.2;

	@Getter private static final Set<String> mFullscreenHideExcludeClasses = new HashSet<>(
			Arrays.asList("WorkerW", "Progman")
	);
}
