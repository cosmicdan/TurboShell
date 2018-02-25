package com.cosmicdan.turboshell.gui.controls;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
public class TurboBarControlFactory {
	private final int mTurboBarHeight;
	private final Class<?> mSourceClass;

	public TurboBarControlFactory(final Class<?> sourceClass, final int turboBarHeight) {
		mTurboBarHeight = turboBarHeight;
		mSourceClass = sourceClass;
	}

	/**
	 * @return An auto-stretch region for HBox children, providing right-aligned controls for anything that follows.
	 */
	public static final Region newCenterPaddingRegion() {
		final Region centerPadding = new Region();
		HBox.setHgrow(centerPadding, Priority.ALWAYS);
		return centerPadding;
	}

	/**
	 * Build a new transparent Button with only a single graphic
	 */
	public final AdaptiveButton newGenericButton(final String imageResourcePath, final Runnable clickAction) {
		return newGenericButton("", new String[] {imageResourcePath}, clickAction);
	}

	private AdaptiveButton newGenericButton(final String text, final String[] imageResourcePaths,
											final Runnable clickAction) {
		final AdaptiveButton button = new AdaptiveButton(mSourceClass, text, imageResourcePaths);
		button.setPrefHeight(mTurboBarHeight - 1);
		return button;
	}

	/*
	@FunctionalInterface
	public interface OnAction {
		void run(Object data);
	}
	*/
}
