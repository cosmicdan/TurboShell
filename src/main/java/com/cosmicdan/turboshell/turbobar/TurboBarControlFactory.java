package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.gui.AdaptiveButton;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Main factory for creating TurboBar controls
 * @author Daniel 'CosmicDan' Connolly
 */
class TurboBarControlFactory {
	private final int mTurboBarHeight;
	private final Class<?> mSourceClass;

	TurboBarControlFactory(final Class<?> sourceClass, final int turboBarHeight) {
		mTurboBarHeight = turboBarHeight;
		mSourceClass = sourceClass;
	}

	/**
	 * @return An auto-stretch region for HBox children, providing right-aligned controls for anything that follows.
	 */
	public static Region newCenterPaddingRegion() {
		final Region centerPadding = new Region();
		HBox.setHgrow(centerPadding, Priority.ALWAYS);
		return centerPadding;
	}

	/**
	 * Build a new transparent Button with no text and a single graphic state
	 */
	public final <T extends Event> AdaptiveButton newGenericButton(final String imageResourcePath,
																   final EventType<T> eventType,
																   final EventHandler<? super T> eventHandler) {
		return newGenericButton("", new String[] {imageResourcePath}, eventType, eventHandler);
	}

	/**
	 * Build a new transparent Button with no text and multiple graphic states
	 */
	public final <T extends Event> AdaptiveButton newGenericButton(final String[] imageResourcePaths,
																   final EventType<T> eventType,
																   final EventHandler<? super T> eventHandler) {
		return newGenericButton("", imageResourcePaths, eventType, eventHandler);
	}

	/**
	 * Build a new transparent Button with text and multiple graphic states
	 */
	private <T extends Event> AdaptiveButton newGenericButton(final String text,
															  final String[] imageResourcePaths,
															  final EventType<T> eventType,
															  final EventHandler<? super T> eventHandler) {
		final AdaptiveButton button = new AdaptiveButton(mSourceClass, text, imageResourcePaths);
		button.setPrefHeight(mTurboBarHeight);

		if (null != eventType) {
			button.addEventHandler(eventType, eventHandler);
		}
		return button;
	}
}
