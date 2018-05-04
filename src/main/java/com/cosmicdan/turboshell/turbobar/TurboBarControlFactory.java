package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.gui.AdaptiveButton;
import com.cosmicdan.turboshell.models.TurboShellConfig;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.extern.log4j.Log4j2;

/**
 * Main factory for creating TurboBar controls
 * @author Daniel 'CosmicDan' Connolly
 */

@Log4j2(topic = "TurboBarControlFactory")
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
	Region newCenterPaddingRegion() {
		final Region centerPadding = new Region();
		HBox.setHgrow(centerPadding, Priority.ALWAYS);
		return centerPadding;
	}

	Region newVerticalSeparator() {
		final Region separator = new Region();
		separator.setMaxSize(1, Double.MAX_VALUE);
		separator.setPrefSize(1, Double.MAX_VALUE);
		HBox.setMargin(separator, new Insets(
				TurboShellConfig.getTurboBarVerticalSeparatorMarginY(), TurboShellConfig.getTurboBarVerticalSeparatorMarginX(),
				TurboShellConfig.getTurboBarVerticalSeparatorMarginY(), TurboShellConfig.getTurboBarVerticalSeparatorMarginX()));
		separator.setBackground(new Background(new BackgroundFill(
				Color.web(TurboShellConfig.getTurboBarVerticalSeparatorColor()), CornerRadii.EMPTY, Insets.EMPTY)));
		separator.setOpacity(TurboShellConfig.getTurboBarVerticalSeparatorOpacity());

		return separator;
	}

	Label newLabel() {
		Label label = new Label();
		label.setTextFill(Color.web(TurboShellConfig.getTextColorMain()));
		label.setFont(Font.font("Consolas", 11));
		label.setPadding(new Insets(5));
		label.setOpacity(1.0);
		return label;
	}

	/**
	 * Build a new transparent Button with no text and a single graphic state
	 */
	final <T extends Event> AdaptiveButton newGenericButton(final String imageResourcePath,
															final EventType<T> eventType,
															final EventHandler<? super T> eventHandler) {
		return newGenericButton("", new String[] {imageResourcePath}, eventType, eventHandler);
	}

	/**
	 * Build a new transparent Button with no text and multiple graphic states
	 */
	final <T extends Event> AdaptiveButton newGenericButton(final String[] imageResourcePaths,
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
