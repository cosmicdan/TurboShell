package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.turbobar.TurboBarView.SysBtnResizeState;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.event.Event;

/**
 * Contract between TurboBar view and presenter
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings({"InterfaceNeverImplemented", "ClassIndependentOfModule", "PublicInnerClass"})
public interface TurboBarContract {
	@SuppressWarnings("CyclicClassDependency")
	interface ITurboBarView {
		void setPresenter(ITurboBarPresenter presenter);
		void setup(int xPos, int width, int barHeight, String css, String windowName);
		void refreshSize(final int xPos, final int width, final int barHeight);
		void updateSysBtnResize(SysBtnResizeState toState);
	}

	@SuppressWarnings("CyclicClassDependency")
	interface ITurboBarPresenter {
		ITurboBarView getTurboBarView();

		HWND getTurboBarHWnd();

		/**
		 * Used to set the view to top or bottom of z-order
		 * @param topmost Whether to set the view as topmost or not.
		 */
		void setTopmost(final boolean topmost);

		@FunctionalInterface
		interface ViewAction {
			void invoke(ITurboBarPresenter presenter, Event event);
		}
	}
}
