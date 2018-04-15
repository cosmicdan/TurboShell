package com.cosmicdan.turboshell.gui;

import com.cosmicdan.turboshell.gui.base.MvpPresenter;
import com.cosmicdan.turboshell.gui.base.MvpView;
import javafx.event.Event;

/**
 * Contract between TurboBar view and presenter
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings({"InterfaceNeverImplemented", "ClassIndependentOfModule", "PublicInnerClass"})
public interface TurboBarContract {
	interface View extends MvpView<Presenter> {
		void setup(int xPos, int width, int barHeight, String css, String windowName);
	}

	interface Presenter extends MvpPresenter {
		@FunctionalInterface
		interface ViewAction {
			void invoke(Presenter presenter, Event event);
		}
	}
}
