package com.cosmicdan.turboshell.ui;

import com.cosmicdan.turboshell.models.TurboBarConfig;
import com.cosmicdan.turboshell.models.WindowsEnvironment;
import com.cosmicdan.turboshell.winapi.ShellAPIEx;
import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinApiError;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.glass.ui.Window;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import lombok.extern.log4j.Log4j2;

import java.net.URL;

/**
 * TurboBar presenter
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public class TurboBarPresenter implements TurboBarContract.Presenter {
	private static final String WINDOW_NAME = "TurboShell's TurboBar";
	private static final int turboBarFlags = WinUserEx.SWP_NOMOVE | WinUserEx.SWP_NOSIZE | WinUserEx.SWP_NOACTIVATE;
	private static final int WM_USER_APPBAR_CALLBACK = WinUserEx.WM_USER + 808;

	private final TurboBarContract.View mView;
	private final WinDef.HWND turboBarHWnd;

	// cached values to save unnecessary WinAPI calls
	private boolean isTopmost = false;

	public TurboBarPresenter(TurboBarContract.View view) {
		mView = view;
		mView.setPresenter(this);
		TurboBarConfig config = new TurboBarConfig();

		// gather data for building the initial TurboBar view...
		int turboBarHeight = config.getBarHeight();
		int[] workAreaStartAndWidth = WindowsEnvironment.getInstance().getWorkAreaStartAndWidth();
		URL cssResources = getClass().getResource("TurboBar.css");
		if (null == cssResources)
			throw new RuntimeException("Could not load TurboBar.css!");
		String css = cssResources.toExternalForm();
		// ...then create the JavaFX scene for TurboBar
		// TODO: TurboBar scene creation steals focus. Get the currently-active hWnd here, then re-activate it after setup()
		mView.setup(workAreaStartAndWidth[0], workAreaStartAndWidth[1], turboBarHeight, css);

		// cache TurboBar's hWnd for future native operations
		turboBarHWnd = new WinDef.HWND();
		turboBarHWnd.setPointer(new Pointer(Window.getWindows().get(0).getNativeWindow()));

		// apply desired window styles to the TurboBar (toolwindow = no taskbar entry)
		User32Ex.INSTANCE.SetWindowLongPtr(turboBarHWnd, WinUserEx.GWL_EXSTYLE, Pointer.createConstant(WinUserEx.WS_EX_TOOLWINDOW));

		// set topmost initially, along with other important window flags
		setTopmost(true);

		// setup the appbar...
		final ShellAPI.APPBARDATA appBarData = new ShellAPI.APPBARDATA.ByReference();
		appBarData.cbSize.setValue(appBarData.size());
		appBarData.hWnd = turboBarHWnd;
		appBarData.uCallbackMessage.setValue(WM_USER_APPBAR_CALLBACK);
		WinDef.UINT_PTR appBarResult = Shell32.INSTANCE.SHAppBarMessage(new WinDef.DWORD(ShellAPI.ABM_NEW), appBarData);
		if (1 == appBarResult.intValue()) {
			// set position
			appBarData.uEdge.setValue(ShellAPI.ABE_TOP);
			appBarData.rc.top = 0;
			appBarData.rc.left = workAreaStartAndWidth[0];
			appBarData.rc.bottom = turboBarHeight;
			appBarData.rc.right = workAreaStartAndWidth[1];
			Shell32.INSTANCE.SHAppBarMessage(new WinDef.DWORD(ShellAPI.ABM_SETPOS), appBarData); // always returns true
		} else {
			throw new RuntimeException("Error registering TurboBar with SHAppBarMessage!");
		}
		// ...and it's callback
		BaseTSD.LONG_PTR turboBarWinProcBase = User32.INSTANCE.GetWindowLongPtr(turboBarHWnd, User32.GWL_WNDPROC);
		WinUser.WindowProc turboBarWinProcCallback = new TurboBarWinProcCallback(turboBarWinProcBase);
		BaseTSD.LONG_PTR appBarCallbackResult = User32Ex.INSTANCE.SetWindowLongPtr(turboBarHWnd, User32.GWL_WNDPROC, turboBarWinProcCallback);
		if (0 == appBarCallbackResult.longValue()) {
			throw new WinApiError("Error setting TurboBar appbar callback!");
		}

		// finally, add a shutdown hook to cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("TurboBar shutdown...");
			Shell32.INSTANCE.SHAppBarMessage(new WinDef.DWORD(ShellAPI.ABM_REMOVE), appBarData);
		}));
	}

	@Override
	public String getWindowTitle() {
		return WINDOW_NAME;
	}

	private void setTopmost(final boolean topmost) {
		if (topmost == isTopmost)
			return;
		User32Ex.INSTANCE.SetWindowPos(turboBarHWnd, topmost ? WinUserEx.HWND_TOPMOST : WinUserEx.HWND_BOTTOM, 0, 0, 0, 0, turboBarFlags);
		//if (topmost) // Will put TurboBar above *other* topmost windows too. Probably not necessary...
		//	User32Ex.INSTANCE.SetWindowPos(turboBarHWnd, WinUserEx.HWND_TOP , 0, 0, 0, 0, turboBarFlags);
		isTopmost = topmost;
	}

	class TurboBarWinProcCallback implements WinUser.WindowProc {
		// Store the original window procedure for chain-calling it
		private final BaseTSD.LONG_PTR mTurboBarWinProcBase;

		TurboBarWinProcCallback(BaseTSD.LONG_PTR turboBarWinProcBase) {
			mTurboBarWinProcBase = turboBarWinProcBase;
		}

		@Override
		public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
			if (TurboBarPresenter.WM_USER_APPBAR_CALLBACK == uMsg) {
				//log.info(hWnd + "; " + uMsg + "; " + wParam.intValue() + "; " + lParam.intValue());
				switch (wParam.intValue()) {
					case ShellAPIEx.ABN_FULLSCREENAPP:
						boolean fullscreenEnter = (1 == lParam.intValue());
						// TODO: Ping the WindowWatcher model with fullscreenChange flag so it can react accordingly
						break;
				}
			}
			// pass it on...
			return User32Ex.INSTANCE.CallWindowProc(mTurboBarWinProcBase.toPointer(), turboBarHWnd, uMsg, wParam, lParam);
		}
	}
}
