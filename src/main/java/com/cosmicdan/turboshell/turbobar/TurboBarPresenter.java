package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.models.TurboShellConfig;
import com.cosmicdan.turboshell.models.WinEventAgent;
import com.cosmicdan.turboshell.models.WindowsEnvironment;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.Presenter;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.View;
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
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.UINT_PTR;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import javafx.event.Event;
import lombok.extern.log4j.Log4j2;

import java.net.URL;

/**
 * TurboBar presenter. Contains all the back-end logic for a View (updating and responding it) as well as delegating between
 * views and models.
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings({"FieldCanBeLocal", "CyclicClassDependency", "ClassWithTooManyDependencies"})
@Log4j2
public class TurboBarPresenter implements Presenter {
	private static final String WINDOW_NAME = "TurboShell's TurboBar";
	private static final int turboBarFlags = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE;
	private static final int WM_USER_APPBAR_CALLBACK = WinUser.WM_USER + 808;

	private HWND turboBarHWnd = null;

	// all callbacks as class fields to avoid GC
	private APPBARDATA appBarData = null;
	private TurboBarWinProcCallback winProcCallback = null;

	// cached values to save unnecessary WinAPI calls
	private boolean mIsTopmost = false;

	// agents
	private static WinEventAgent winEventAgent = null;

	public TurboBarPresenter() {
	}

	@SuppressWarnings("ReturnOfThis")
	public final TurboBarPresenter setup(final View view) {
		// gather data for building the initial TurboBar view...
		final int turboBarHeight = TurboShellConfig.getTurboBarHeight();
		final int[] workAreaXAndWidth = WindowsEnvironment.getWorkAreaXAndWidth();
		final URL cssResources = getClass().getResource("TurboBar.css");
		if (null == cssResources)
			throw new RuntimeException("Could not load TurboBar.css!");
		final String css = cssResources.toExternalForm();
		// ...cache the currently active hWnd...
		final HWND initialTopHwnd = User32.INSTANCE.GetForegroundWindow();
		// ...then create the JavaFX scene for TurboBar
		view.setup(workAreaXAndWidth[0], workAreaXAndWidth[1], turboBarHeight, css, WINDOW_NAME);
		User32.INSTANCE.SetForegroundWindow(initialTopHwnd);
		view.setPresenter(this);

		// cache TurboBar's hWnd for future native operations
		turboBarHWnd = new HWND();
		turboBarHWnd.setPointer(new Pointer(Window.getWindows().get(0).getNativeWindow()));

		// (JavaFX workaround) apply desired window styles to the TurboBar to make it "undecorated" and not appear on taskbar nor alt-tab
		User32Ex.INSTANCE.SetWindowLongPtr(turboBarHWnd, WinUser.GWL_STYLE,
				Pointer.createConstant(WinUser.WS_VISIBLE | WinUser.WS_POPUP | WinUser.WS_CLIPCHILDREN)
		);
		User32Ex.INSTANCE.SetWindowLongPtr(turboBarHWnd, WinUser.GWL_EXSTYLE,
				Pointer.createConstant(WinUserEx.WS_EX_NOACTIVATE | WinUserEx.WS_EX_TOOLWINDOW | WinUserEx.WS_EX_TOPMOST)
		);

		// we need to re-set the size and position after setting window styles (JavaFX workaround)
		view.refreshSize(workAreaXAndWidth[0], workAreaXAndWidth[1], turboBarHeight);

		// setup the appbar...
		appBarData = setupAppbar(workAreaXAndWidth, turboBarHeight);

		// setup some models and register for observing
		setupModelsAndObservers(initialTopHwnd);

		// finally, add a shutdown hook to cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("TurboBar shutdown...");
			Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_REMOVE), appBarData);
		}));

		return this;
	}

	private APPBARDATA setupAppbar(final int[] workAreaXAndWidth, final int turboBarHeight) {
		appBarData = new ByReference();
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
		// ...and its callback
		final LONG_PTR turboBarWinProcBase = User32Ex.INSTANCE.GetWindowLongPtr(turboBarHWnd, WinUser.GWL_WNDPROC);
		winProcCallback = new TurboBarWinProcCallback(turboBarWinProcBase, turboBarHWnd);
		final LONG_PTR appBarCallbackResult = User32Ex.INSTANCE.SetWindowLongPtr(
				turboBarHWnd, WinUser.GWL_WNDPROC, winProcCallback);
		if (0 == appBarCallbackResult.longValue()) {
			throw new WinApiException("Error setting TurboBar appbar callback!");
		}
		return appBarData;
	}

	private void setupModelsAndObservers(HWND initialTopHwnd) {
		winEventAgent = new WinEventAgent(initialTopHwnd);
		winEventAgent.registerCallback(
				WinEventAgent.PAYLOAD_WINDOW_TITLE,
				(Object data) -> updateWindowTitle((String)data)
		);
		winEventAgent.start();
	}

	//////////////////////////////////////////////////////////////
	// Self-managed logic - AppBar messages from environment
	//////////////////////////////////////////////////////////////

	private static class TurboBarWinProcCallback implements WindowProc {
		// Store the original window procedure for chain-calling it
		private final LONG_PTR mTurboBarWinProcBase;
		private final HWND mTurboBarHWnd;

		TurboBarWinProcCallback(final LONG_PTR turboBarWinProcBase, final HWND turboBarHWnd) {
			mTurboBarWinProcBase = turboBarWinProcBase;
			mTurboBarHWnd = turboBarHWnd;
		}

		@Override
		public final LRESULT callback(final HWND hwnd, final int uMsg, final WPARAM wParam, final LPARAM lParam) {
			if (WM_USER_APPBAR_CALLBACK == uMsg) {
				//log.info("Invoke appbar callback...");
				//log.info(hWnd + "; " + uMsg + "; " + wParam.intValue() + "; " + lParam.intValue());
				for (final AppbarCallback callback : AppbarCallback.values()) {
					if (wParam.intValue() == callback.mAppbarCallbackConstant) {
						callback.invoke(lParam);
					}
				}
			}
			// pass it on...
			return User32Ex.INSTANCE.CallWindowProc(mTurboBarWinProcBase.toPointer(), mTurboBarHWnd, uMsg, wParam, lParam);
		}
	}

	@FunctionalInterface
	interface IAppbarCallback {
		void invoke(final LPARAM lParam);
	}

	@SuppressWarnings("Singleton")
	private enum AppbarCallback implements IAppbarCallback {
		ABN_FULLSCREENAPP(ShellAPIEx.ABN_FULLSCREENAPP, (LPARAM lParam) -> {
			final boolean fullscreenEntered = (1 == lParam.intValue());
			log.info("Fullscreen entered: {}", fullscreenEntered);
			// TODO: Ping the WinEventAgent model with fullscreenChange flag so it can react accordingly. Exclude desktop though!
		});

		private final int mAppbarCallbackConstant;
		private final IAppbarCallback mAppBarCallback;

		AppbarCallback(final int appbarCallbackConstant, final IAppbarCallback appBarCallback) {
			mAppbarCallbackConstant = appbarCallbackConstant;
			mAppBarCallback = appBarCallback;
		}

		@Override
		public void invoke(final LPARAM lParam) {
			mAppBarCallback.invoke(lParam);
		}
	}


	//////////////////////////////////////////////////////////////
	// Model-sourced logic (i.e. environment/data changes)
	//////////////////////////////////////////////////////////////

	private static void updateWindowTitle(final String windowTitle) {
		log.info("Got window title update: {}", windowTitle);
	}

	private void setTopmost(final boolean topmost) {
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

	private boolean isTopmost() {
		return mIsTopmost;
	}

	//////////////////////////////////////////////////////////////
	// View-sourced logic (i.e. user-invoked actions)
	//////////////////////////////////////////////////////////////

	/**
	 * Main entrypoint for view actions.
	 */
	/*
	@Override
	public final void doViewAction(final ViewAction action, final Event event) {
		action.invoke(event);
	}
	*/

	@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
	enum SysBtnAction implements ViewAction {
		MINIMIZE((Presenter presenter, Event event) -> {
			winEventAgent.minimizeForeground();
		}),
		RESIZE((Presenter presenter, Event event) -> {

		}),
		CLOSE((Presenter presenter, Event event) -> {

		});

		private final ViewAction mViewAction;

		SysBtnAction(final ViewAction viewAction) {
			mViewAction = viewAction;
		}

		@Override
		public void invoke(final Presenter presenter, final Event event) {
			mViewAction.invoke(presenter, event);
		}
	}
}
