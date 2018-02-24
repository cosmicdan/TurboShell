package com.cosmicdan.turboshell.ui.controls;

import com.cosmicdan.turboshell.ui.TurboBarView;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
public class TurboBarControlFactory {
	private final int mTurboBarHeight;
	private final Class mSourceClass;

	public TurboBarControlFactory(Class sourceClass, int turboBarHeight) {
		mTurboBarHeight = turboBarHeight;
		mSourceClass = sourceClass;
	}

	/**
	 * @return An auto-stretch region for HBox children, providing right-aligned controls for anything that follows.
	 */
	public Region newCenterPaddingRegion() {
		final Region centerPadding = new Region();
		HBox.setHgrow(centerPadding, Priority.ALWAYS);
		return centerPadding;
	}

	/**
	 * Build a new transparent Button with only a single graphic
	 * @param imageResourcePath
	 * @return The new button for adding to HBox children.
	 */
	public AdaptiveButton newGenericButton(String imageResourcePath) {
		return newGenericButton("", new String[] {imageResourcePath});
	}

	public AdaptiveButton newGenericButton(String text, String[] imageResourcePaths) {
		AdaptiveButton button = new AdaptiveButton(mSourceClass, text, imageResourcePaths);
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
