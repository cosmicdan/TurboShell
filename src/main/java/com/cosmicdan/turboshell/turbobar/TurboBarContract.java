package com.cosmicdan.turboshell.turbobar;

import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.event.Event;

/**
 * Contract between TurboBar view and presenter
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings("InterfaceNeverImplemented")
public interface TurboBarContract {
	@SuppressWarnings("CyclicClassDependency")
	interface ITurboBarView {
		enum SysBtnMinimizeState {ENABLED, DISABLED}
		enum SysBtnResizeState {MAXIMIZE, RESTORE, DISABLED}
		enum SysBtnCloseAction {CANCEL, CLICK, PRIMARY_HELD, SECONDARY_HELD}

		void setup(ITurboBarPresenter presenter, int xPos, int width, int barHeight, String css, String windowName);
		void redraw(final int xPos, final int width, final int barHeight);
		void updateSysBtnMinimize(SysBtnMinimizeState toState);
		void updateSysBtnResize(SysBtnResizeState toState);
		void updateDateTime(String dateTime);
		void updateWindowTitle(String windowTitle);
	}

	@SuppressWarnings("CyclicClassDependency")
	interface ITurboBarPresenter {
		/**
		 * Required for AppBar callback.
		 * @return The TurboBar HWnd.
		 */
		HWND getTurboBarHWnd();

		/**
		 * Used to toggle topmost property for TurboBar, e.g. when a foreground app enters fullscreen mode.
		 * @param topmost Whether to set the view as topmost or not.
		 */
		void setTopmost(final boolean topmost);

		/**
		 * Called from the main thread
		 * @param view The pre-constructed view
		 */
		void setup(final ITurboBarView view);

		@FunctionalInterface
		interface ViewAction {
			void invoke(ITurboBarPresenter presenter, Event event);
		}
	}
}
