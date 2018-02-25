package com.cosmicdan.turboshell.gui;

import com.cosmicdan.turboshell.models.TurboBarConfig;
import com.cosmicdan.turboshell.models.WindowWatcher;
import com.cosmicdan.turboshell.models.WindowsEnvironment;
import com.cosmicdan.turboshell.gui.TurboBarContract.Presenter;
import com.cosmicdan.turboshell.gui.TurboBarContract.View;
import com.cosmicdan.turboshell.winapi.ShellAPIEx;
import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinApiException;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.glass.ui.Window;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.ShellAPI.APPBARDATA;
import com.sun.jna.platform.win32.ShellAPI.APPBARDATA.ByReference;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.UINT_PTR;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import lombok.extern.log4j.Log4j2;

import java.net.URL;

/**
 * TurboBar presenter
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public class TurboBarPresenter implements Presenter {
	private static final String WINDOW_NAME = "TurboShell's TurboBar";
	private static final int turboBarFlags = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE;
	private static final int WM_USER_APPBAR_CALLBACK = WinUser.WM_USER + 808;

	private HWND turboBarHWnd;

	// all callbacks as class fields to avoid GC
	private APPBARDATA appBarData;
	private TurboBarWinProcCallback winProcCallback;

	// cached values to save unnecessary WinAPI calls
	private boolean mIsTopmost = false;

	public TurboBarPresenter() {}

	public TurboBarPresenter setup(final View view, final WindowsEnvironment winEnv) {
		final TurboBarConfig config = new TurboBarConfig();

		// gather data for building the initial TurboBar view...
		final int turboBarHeight = config.getBarHeight();
		final int[] workAreaXAndWidth = WindowsEnvironment.getWorkAreaXAndWidth();
		final URL cssResources = getClass().getResource("TurboBar.css");
		if (null == cssResources)
			throw new RuntimeException("Could not load TurboBar.css!");
		final String css = cssResources.toExternalForm();
		// ...then create the JavaFX scene for TurboBar
		// TODO: TurboBar scene creation steals focus. Get the currently-active hWnd here, then re-activate it after setup()
		view.setup(workAreaXAndWidth[0], workAreaXAndWidth[1], turboBarHeight, css, WINDOW_NAME);
		view.setPresenter(this);

		// cache TurboBar's hWnd for future native operations
		turboBarHWnd = new HWND();
		turboBarHWnd.setPointer(new Pointer(Window.getWindows().get(0).getNativeWindow()));

		// apply desired window styles to the TurboBar (toolwindow = no taskbar entry)
		User32Ex.INSTANCE.SetWindowLongPtr(turboBarHWnd, WinUser.GWL_EXSTYLE,
				Pointer.createConstant(WinUserEx.WS_EX_NOACTIVATE | WinUserEx.WS_EX_TOOLWINDOW | WinUserEx.WS_EX_TOPMOST)
		);

		// set topmost initially, along with other important window flags
		setTopmost(true);

		// setup the appbar...
		appBarData = setupAppbar(workAreaXAndWidth, turboBarHeight);

		// setup some models and register for observing
		setupModelsAndObservers();

		// finally, add a shutdown hook to cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("TurboBar shutdown...");
			Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_REMOVE), appBarData);
		}));
		return this;
	}

	private APPBARDATA setupAppbar(int[] workAreaXAndWidth, int turboBarHeight) {
		final APPBARDATA appBarData = new ByReference();
		appBarData.cbSize.setValue(appBarData.size());
		appBarData.hWnd = turboBarHWnd;
		appBarData.uCallbackMessage.setValue(WM_USER_APPBAR_CALLBACK);
		final UINT_PTR appBarResult = Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_NEW), appBarData);
		if (1 == appBarResult.intValue()) {
			// set position
			appBarData.uEdge.setValue(ShellAPI.ABE_TOP);
			appBarData.rc.top = 0;
			appBarData.rc.left = workAreaXAndWidth[0];
			appBarData.rc.bottom = turboBarHeight;
			appBarData.rc.right = workAreaXAndWidth[1];
			Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_SETPOS), appBarData); // always returns true
		} else {
			throw new RuntimeException("Error registering TurboBar with SHAppBarMessage!");
		}
		// ...and it's callback
		final LONG_PTR turboBarWinProcBase = User32Ex.INSTANCE.GetWindowLongPtr(
				turboBarHWnd, WinUser.GWL_WNDPROC);
		winProcCallback = new TurboBarWinProcCallback(turboBarWinProcBase);
		final LONG_PTR appBarCallbackResult = User32Ex.INSTANCE.SetWindowLongPtr(
				turboBarHWnd, WinUser.GWL_WNDPROC, winProcCallback);
		if (0 == appBarCallbackResult.longValue()) {
			throw new WinApiException("Error setting TurboBar appbar callback!");
		}
		return appBarData;
	}

	private void setupModelsAndObservers() {
		final WindowWatcher windowWatcher = new WindowWatcher();
		windowWatcher.registerCallback(
				WindowWatcher.PAYLOAD_WINDOW_TITLE,
				(Object data) -> updateWindowTitle((String)data)
		);
		windowWatcher.start();
	}

	//////////////////////////////////////////////////////////////
	// Self-managed logic - AppBar messages from environment
	//////////////////////////////////////////////////////////////

	private class TurboBarWinProcCallback implements WindowProc {
		// Store the original window procedure for chain-calling it
		private final LONG_PTR mTurboBarWinProcBase;

		TurboBarWinProcCallback(final LONG_PTR turboBarWinProcBase) {
			mTurboBarWinProcBase = turboBarWinProcBase;
		}

		@Override
		public final LRESULT callback(final HWND hwnd, final int uMsg, final WPARAM wParam, final LPARAM lParam) {
			if (WM_USER_APPBAR_CALLBACK == uMsg) {
				//log.info(hWnd + "; " + uMsg + "; " + wParam.intValue() + "; " + lParam.intValue());
				log.info("Invoke appbar callback...");
				AppbarCallbackResponse.invoke(wParam, lParam);
			}
			// pass it on...
			return User32Ex.INSTANCE.CallWindowProc(mTurboBarWinProcBase.toPointer(), turboBarHWnd, uMsg, wParam, lParam);
		}
	}

	private enum AppbarCallbackResponse {
		ABN_FULLSCREENAPP(ShellAPIEx.ABN_FULLSCREENAPP) {
			@Override
			public void invoke(final LPARAM lParam) {
				final boolean fullscreenEntered = (1 == lParam.intValue());
				log.info("Fullscreen entered: " + fullscreenEntered);
				// TODO: Ping the WindowWatcher model with fullscreenChange flag so it can react accordingly
			}
		};

		private final int mAppbarCallbackConst;

		AppbarCallbackResponse(final int appbarCallbackConst) {
			mAppbarCallbackConst = appbarCallbackConst;
		}

		@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
		static void invoke(final WPARAM wParam, final LPARAM lParam) {
			for (final AppbarCallbackResponse response : values()) {
				if (wParam.intValue() == response.mAppbarCallbackConst) {
					response.invoke(lParam);
				}
			}
			// some other event happened, ignore it
		}

		protected abstract void invoke(LPARAM lParam);
	}

	//////////////////////////////////////////////////////////////
	// Model-sourced logic (i.e. environment/data changes)
	//////////////////////////////////////////////////////////////

	private void updateWindowTitle(final String windowTitle) {
		log.info("Got window title update: " + windowTitle);
	}

	private final void setTopmost(final boolean topmost) {
		if (topmost == mIsTopmost)
			return;
		User32Ex.INSTANCE.SetWindowPos(
				turboBarHWnd,
				topmost ? WinUserEx.HWND_TOPMOST : WinUserEx.HWND_BOTTOM,
				0, 0, 0, 0, turboBarFlags
		);
		//if (topmost) // Will put TurboBar above *other* topmost windows too. Probably not necessary...
		//	User32Ex.INSTANCE.SetWindowPos(turboBarHWnd, WinUserEx.HWND_TOP , 0, 0, 0, 0, turboBarFlags);
		mIsTopmost = topmost;
	}

	private final boolean isTopmost() {
		return mIsTopmost;
	}

	//////////////////////////////////////////////////////////////
	// View-sourced logic (i.e. user-invoked actions)
	//////////////////////////////////////////////////////////////

	/**
	 * Main entrypoint for view actions.
	 */
	@Override
	public final void doViewAction(final ViewAction action) {
		action.invoke();
	}

	enum SysBtnAction implements ViewAction {
		MINIMIZE {
			@Override
			public void invoke() {
				log.info("Minimize fired!");
			}
		},
		RESIZE {
			@Override
			public void invoke() {

			}
		},
		CLOSE {
			@Override
			public void invoke() {

			}
		}
	}
}
