package com.cosmicdan.turboshell.gui.controls;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;
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
	public static Region newCenterPaddingRegion() {
		final Region centerPadding = new Region();
		HBox.setHgrow(centerPadding, Priority.ALWAYS);
		return centerPadding;
	}

	/**
	 * Build a new transparent Button with only a single graphic
	 */
	public final <T extends Event> AdaptiveButton newGenericButton(final String imageResourcePath,
																   final EventType<T> eventType,
																   final EventHandler<? super T> eventHandler) {
		return newGenericButton("", new String[] {imageResourcePath}, eventType, eventHandler);
	}

	private <T extends Event> AdaptiveButton newGenericButton(final String text,
															  final String[] imageResourcePaths,
															  final EventType<T> eventType,
															  final EventHandler<? super T> eventHandler) {
		final AdaptiveButton button = new AdaptiveButton(mSourceClass, text, imageResourcePaths);
		button.setPrefHeight(mTurboBarHeight - 1);

		button.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {

		});

		button.addEventHandler(eventType, eventHandler);
		return button;
	}
}
