package com.cosmicdan.turboshell.gui.controls;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.net.URL;

/**
 * @author Daniel 'CosmicDan' Connolly
 */

@Log4j2
public class AdaptiveButton extends Button {
	public enum AddTextPadding {LEFT, RIGHT, BOTH}

	@SuppressWarnings("ObjectAllocationInLoop")
	AdaptiveButton(final Class<?> mSourceClass, final String text, final String[] imageResourcePaths) {
		super(text);
		final ImageView[] mImageViews = new ImageView[imageResourcePaths.length];
		if (0 < mImageViews.length) {
			for (int i = 0; i < mImageViews.length; i++) {
				final URL imageUrl = mSourceClass.getResource(imageResourcePaths[i]);
				if (null == imageUrl) {
					throw new RuntimeException("Resource not found: " + mSourceClass.getPackage() + File.separator + imageResourcePaths[i]);
				}
				final ImageView newImage = new ImageView(new Image(imageUrl.toExternalForm()));
				mImageViews[i] = newImage;
			}
			setGraphic(mImageViews[0]);
		}
		if (!text.isEmpty())
			setText(text, AddTextPadding.LEFT);
	}

	private void setText(final String text, final AddTextPadding padding) {
		// dodgy way of adding space between icon and label
		String newText = text;
		if ((AddTextPadding.LEFT.equals(padding)) || (AddTextPadding.BOTH.equals(padding)))
			newText = "  " + newText;
		if ((AddTextPadding.RIGHT.equals(padding)) || (AddTextPadding.BOTH.equals(padding)))
			newText += "  ";
		setText(newText);
	}

	public final void setClickAction(final Runnable runnable) {
		addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> runnable.run());
	}
}