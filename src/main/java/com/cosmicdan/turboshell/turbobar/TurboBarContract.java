package com.cosmicdan.turboshell.turbobar;

import javafx.event.Event;

/**
 * Contract between TurboBar view and presenter
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings({"InterfaceNeverImplemented", "ClassIndependentOfModule", "PublicInnerClass"})
public interface TurboBarContract {
	interface ITurboBarView {
		void setPresenter(ITurboBarPresenter presenter);
		void setup(int xPos, int width, int barHeight, String css, String windowName);
		void refreshSize(final int xPos, final int width, final int barHeight);
	}

	@SuppressWarnings("MarkerInterface")
	interface ITurboBarPresenter {
		@FunctionalInterface
		interface ViewAction {
			void invoke(ITurboBarPresenter presenter, Event event);
		}
	}
}
