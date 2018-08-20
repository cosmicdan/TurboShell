package com.cosmicdan.turboshell;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
	@Getter
	static final int mTurboBarHeight = 25;

	//////////////////////////////////////////////////////////////
	// Theme
	//////////////////////////////////////////////////////////////

	// General
	@Getter
	static final String mTextColorMain = "bbb";

	// SysBtns
	@Getter
	static final String mTurboBarCloseBgPrimary = "a03048";
	@Getter
	static final String mTurboBarCloseBgSecondary = "e81123";

	// Seperators
	private static final double mTurboBarVerticalSeparatorMarginX = 5.0;
	private static final double mTurboBarVerticalSeparatorMarginY = 5.0;
	private static final String mTurboBarVerticalSeparatorColor = "ffffff";
	private static final double mTurboBarVerticalSeparatorOpacity = 0.2;

	@Getter
	static final Set<String> mFullscreenHideExcludeClasses = new HashSet<>(
			Arrays.asList("WorkerW", "Progman")
	);

	public static void styleTurboBarSeparator(final Region separator) {
		HBox.setMargin(separator, new Insets(
				mTurboBarVerticalSeparatorMarginY, mTurboBarVerticalSeparatorMarginX,
				mTurboBarVerticalSeparatorMarginY, mTurboBarVerticalSeparatorMarginX));
		separator.setBackground(new Background(new BackgroundFill(
				Color.web(mTurboBarVerticalSeparatorColor), CornerRadii.EMPTY, Insets.EMPTY)));
		separator.setOpacity(mTurboBarVerticalSeparatorOpacity);
	}
}
