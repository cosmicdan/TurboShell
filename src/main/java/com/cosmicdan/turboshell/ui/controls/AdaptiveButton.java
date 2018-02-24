package com.cosmicdan.turboshell.ui.controls;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.extern.log4j.Log4j2;

import java.net.URL;

/**
 * @author Daniel 'CosmicDan' Connolly
 */

@Log4j2
public class AdaptiveButton extends Button {
	private final ImageView[] mImageViews;

	protected AdaptiveButton(Class mSourceClass, String text, String[] imageResourcePaths) {
		super(text);
		mImageViews = new ImageView[imageResourcePaths.length];
		if (mImageViews.length > 0) {
			for (int i = 0; i < mImageViews.length; i++) {
				URL imageUrl = mSourceClass.getResource(imageResourcePaths[i]);
				if (null == imageUrl) {
					throw new RuntimeException("Resource not found: " + mSourceClass.getPackage() + "/" + imageResourcePaths[i]);
				}
				final ImageView newImage = new ImageView(new Image(imageUrl.toExternalForm()));
				mImageViews[i] = newImage;
			}
			setGraphic(mImageViews[0]);
		}
		if (!text.isEmpty())
			setText(text, true);
	}

	public void setText(String text, boolean addLeftPadding) {
		// dodgy way of adding space between icon and label
		setText(addLeftPadding ? "  " : "".concat(text));
	}

	public void setClickAction(Runnable runnable) {
		addEventFilter(MouseEvent.MOUSE_CLICKED, event -> runnable.run());
	}
}
