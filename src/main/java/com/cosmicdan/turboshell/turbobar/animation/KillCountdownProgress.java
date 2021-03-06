package com.cosmicdan.turboshell.turbobar.animation;

import com.cosmicdan.turboshell.common.control.AdaptiveButton;
import javafx.animation.Transition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2(topic = "KillCountdownProgress")
public class KillCountdownProgress extends Transition {
	public enum AnimationDirection{NORMAL, REVERSE}

	private ObjectProperty<Duration> mDuration = null;
	private static final Duration DEFAULT_DURATION = Duration.millis(400);

	private final AdaptiveButton mCtrlCloseButton;
	private final String mColorHex;
	private final AnimationDirection mAnimDirection;

	public KillCountdownProgress(final Duration duration, final AdaptiveButton ctrlCloseButton, final String colorHex, final AnimationDirection animDirection) {
		setDuration(duration);
		setCycleDuration(duration);
		mCtrlCloseButton = ctrlCloseButton;
		mColorHex = colorHex;
		mAnimDirection = animDirection;
	}

	@Override
	public final void stop() {
		super.stop();
		mCtrlCloseButton.setStyle(null);
	}

	@Override
	public final void interpolate(final double frac) {
		int progressPercent = (int) (frac * 100);
		if (AnimationDirection.REVERSE == mAnimDirection)
			progressPercent = 100 - progressPercent;
		mCtrlCloseButton.setStyle("-fx-background-color: linear-gradient(" +
				"from 0% 0% to " + mCtrlCloseButton.getWidth() + "px 0px, " +
				'#' + mColorHex + " 0%, #" + mColorHex + ' ' + progressPercent + "%, " +
				"transparent " + progressPercent + "%, transparent 100%);"
		);
	}

	private void setDuration(final Duration value) {
		if ((null != mDuration) || (!DEFAULT_DURATION.equals(value))) {
			getDurationProperty().set(value);
		}
	}

	private ObjectProperty<Duration> getDurationProperty() {
		if (null == mDuration) {
			//noinspection AnonymousInnerClassWithTooManyMethods,OverlyComplexAnonymousInnerClass
			mDuration = new ObjectPropertyBase<Duration>(DEFAULT_DURATION) {
				private static final String NAME = "duration";

				@Override
				public void invalidated() {
					try {
						setCycleDuration(DEFAULT_DURATION);
					} catch (final IllegalArgumentException exception) {
						if (isBound()) {
							unbind();
						}
						set(getCycleDuration());
						throw exception;
					}
				}

				@Override
				public Object getBean() {
					return KillCountdownProgress.this;
				}

				@Override
				public String getName() {
					return NAME;
				}
			};
		}
		return mDuration;
	}
}
