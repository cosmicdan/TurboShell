package com.cosmicdan.turboshell.ui;

import com.cosmicdan.turboshell.ui.base.MvpPresenter;
import com.cosmicdan.turboshell.ui.base.MvpView;

/**
 * Contract between TurboBar view and presenter
 * @author Daniel 'CosmicDan' Connolly
 */
interface TurboBarContract {
	enum SysBtnAction {MINIMIZE, RESIZE, CLOSE}

	interface View extends MvpView<Presenter> {
		void setup(int xPos, int width, int barHeight, String css);
	}

	interface Presenter extends MvpPresenter {
		String getWindowName();

		void doSysbtnAction(SysBtnAction action);
	}
}
