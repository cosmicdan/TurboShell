package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.common.model.AgentModel.PayloadCallback;
import com.cosmicdan.turboshell.common.model.CalendarAgent;
import com.cosmicdan.turboshell.TurboShellConfig;
import com.cosmicdan.turboshell.winapi.model.WinEventAgent;
import com.cosmicdan.turboshell.winapi.model.WinEventAgent.KillForegroundHardness;
import com.cosmicdan.turboshell.winapi.model.WindowsEnvironment;
import com.cosmicdan.turboshell.winapi.model.WindowInfo.Flag;
import com.cosmicdan.turboshell.common.model.payload.CalendarChangePayload;
import com.cosmicdan.turboshell.common.model.payload.WindowSysBtnUpdatePayload;
import com.cosmicdan.turboshell.common.model.payload.WindowTitleChangePayload;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarPresenter;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarView;
import com.cosmicdan.turboshell.turbobar.TurboBarView.SysBtnMinimizeState;
import com.cosmicdan.turboshell.turbobar.TurboBarView.SysBtnResizeState;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.extern.log4j.Log4j2;

import java.net.URL;
import java.util.EnumSet;

/**
 * TurboBar presenter. Contains all the back-end logic for the View (updating and responding to it) as well as delegating between
 * view actions and models/agents.
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings({"FieldCanBeLocal", "CyclicClassDependency", "ClassWithTooManyDependencies"})
@Log4j2
public class TurboBarPresenter implements ITurboBarPresenter {
	private static final String WINDOW_NAME = "TurboShell's TurboBar";
	private static final int turboBarFlags = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE;
	private static final int WM_USER_APPBAR_CALLBACK = WinUser.WM_USER + 808;

	private HWND turboBarHWnd = null;
	private ITurboBarView turboBarView = null;

	// all callbacks as class fields to avoid GC
	private APPBARDATA appBarData = null;
	private TurboBarWinProcCallback winProcCallback = null;

	// cached values to save unnecessary WinAPI calls
	private boolean mIsTopmost = true;

	public TurboBarPresenter() {
	}

	@Override
	public final HWND getTurboBarHWnd() {
		return turboBarHWnd;
	}

	/*
	@Override
	public final ITurboBarView getTurboBarView() {
		return turboBarView;
	}
	*/

	@SuppressWarnings({"ReturnOfThis", "FeatureEnvy"})
	public final ITurboBarPresenter setup(final ITurboBarView view) {
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
		turboBarView = view;
		turboBarView.setup(workAreaXAndWidth[0], workAreaXAndWidth[1], turboBarHeight, css, WINDOW_NAME);
		User32.INSTANCE.SetForegroundWindow(initialTopHwnd);
		turboBarView.setPresenter(this);

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
		turboBarView.redraw(workAreaXAndWidth[0], workAreaXAndWidth[1], turboBarHeight);

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

	/**
	 * Helper method for registering the TurboBar as an AppBar.
	 * @param workAreaXAndWidth A provided value from {@link WindowsEnvironment#getWorkAreaXAndWidth()}
	 * @param turboBarHeight The height of the TurboBar, as retrieved from config.
	 * @return An instance of APPBARDATA, mostly use for unregistering on a shutdown hook.
	 * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/cc144177(v=vs.85).aspx">
	 *     Using Application Desktop Toolbars on MSDN</a>
	 */
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
		winProcCallback = new TurboBarWinProcCallback(turboBarWinProcBase, this);
		final LONG_PTR appBarCallbackResult = User32Ex.INSTANCE.SetWindowLongPtr(
				turboBarHWnd, WinUser.GWL_WNDPROC, winProcCallback);
		if (0 == appBarCallbackResult.longValue()) {
			throw new WinApiException("Error setting TurboBar appbar callback!");
		}
		return appBarData;
	}

	/**
	 * Setup models/agents that this Presenter will be communicating with.
	 * @param initialTopHwnd The initial foreground hWnd that was retrieved on TurboShell startup, so it can be re-activated after
	 *                       TurboShell steals focus
	 */
	@SuppressWarnings("FeatureEnvy")
	private void setupModelsAndObservers(final HWND initialTopHwnd) {
		WinEventAgent.INSTANCE.setInitialTopHwnd(initialTopHwnd);

		// window title changes
		WinEventAgent.INSTANCE.registerCallback(WindowTitleChangePayload.class,
				(PayloadCallback<WindowTitleChangePayload>) this::updateWindowTitle);

		// window sysbtn control updates
		WinEventAgent.INSTANCE.registerCallback(WindowSysBtnUpdatePayload.class,
				(PayloadCallback<WindowSysBtnUpdatePayload>) this::updateSysBtns);

		WinEventAgent.INSTANCE.start();

		// Calendar
		CalendarAgent.INSTANCE.registerCallback(CalendarChangePayload.class,
				(PayloadCallback<CalendarChangePayload>) this::updateDateTime);

		CalendarAgent.INSTANCE.start();
	}

	@Override
	public final void setTopmost(final boolean topmost) {
		if (topmost == mIsTopmost) // cache check to save API calls
			return;
		User32Ex.INSTANCE.SetWindowPos(
				turboBarHWnd,
				topmost ? WinUserEx.HWND_TOPMOST : WinUserEx.HWND_BOTTOM,
				0, 0, 0, 0, turboBarFlags
		);
		if (topmost) // Will put TurboBar above *other* topmost windows too. Might not be necessary but meh.
			User32Ex.INSTANCE.SetWindowPos(turboBarHWnd, WinUserEx.HWND_TOP , 0, 0, 0, 0, turboBarFlags);
		mIsTopmost = topmost;
	}

	//////////////////////////////////////////////////////////////
	// Self-managed logic - AppBar messages from environment
	//////////////////////////////////////////////////////////////

	/**
	 * Shared callback for AppBar events
	 */
	private static class TurboBarWinProcCallback implements WindowProc {
		// Store the original window procedure for chain-calling it
		private final LONG_PTR mTurboBarWinProcBase;
		private final ITurboBarPresenter mTurboBarPresenter;

		TurboBarWinProcCallback(final LONG_PTR turboBarWinProcBase, final ITurboBarPresenter turboBarPresenter) {
			mTurboBarWinProcBase = turboBarWinProcBase;
			mTurboBarPresenter = turboBarPresenter;
		}

		@Override
		public final LRESULT callback(final HWND hwnd, final int uMsg, final WPARAM wParam, final LPARAM lParam) {
			if (WM_USER_APPBAR_CALLBACK == uMsg) {
				//log.info("Invoke appbar callback...");
				//log.info(hWnd + "; " + uMsg + "; " + wParam.intValue() + "; " + lParam.intValue());
				for (final AppbarCallback callback : AppbarCallback.values()) {
					if (wParam.intValue() == callback.mAppbarCallbackConstant) {
						callback.invoke(mTurboBarPresenter, lParam);
					}
				}
			}
			// pass it on...
			return User32Ex.INSTANCE.CallWindowProc(mTurboBarWinProcBase.toPointer(), mTurboBarPresenter.getTurboBarHWnd(), uMsg, wParam, lParam);
		}
	}

	/**
	 * TurboBarWinProcCallback (AppBar callback) response logic
	 */
	@SuppressWarnings("Singleton")
	private enum AppbarCallback implements IAppbarCallback {
		ABN_FULLSCREENAPP(ShellAPIEx.ABN_FULLSCREENAPP, (ITurboBarPresenter turboBarPresenter, LPARAM lParam) -> {
			if (!WindowsEnvironment.isDesktopFocused()) {
				final boolean fullscreenExited = (1 != lParam.intValue());
				turboBarPresenter.setTopmost(fullscreenExited);
			}
		});

		private final int mAppbarCallbackConstant;
		private final IAppbarCallback mAppBarCallback;

		AppbarCallback(final int appbarCallbackConstant, final IAppbarCallback appBarCallback) {
			mAppbarCallbackConstant = appbarCallbackConstant;
			mAppBarCallback = appBarCallback;
		}

		@Override
		public void invoke(final ITurboBarPresenter turboBarPresenter, final LPARAM lParam) {
			mAppBarCallback.invoke(turboBarPresenter, lParam);
		}
	}

	@FunctionalInterface
	interface IAppbarCallback {
		void invoke(final ITurboBarPresenter turboBarPresenter, final LPARAM lParam);
	}


	//////////////////////////////////////////////////////////////
	// Model/agent-sourced or locally-sourced logic (e.g. reactions to environment changes)
	//////////////////////////////////////////////////////////////

	private void updateWindowTitle(final WindowTitleChangePayload payload) {
		turboBarView.updateWindowTitle(payload.getWindowTitle());
	}

	private void updateSysBtns(final WindowSysBtnUpdatePayload payload) {
		final EnumSet<Flag> currentWindowFlags = payload.getFlags();
		// check if window can be resized
		SysBtnResizeState newState = SysBtnResizeState.DISABLED;
		if (currentWindowFlags.contains(Flag.IS_MAXIMIZABLE)) {
			newState = currentWindowFlags.contains(Flag.IS_MAXIMIZED) ? SysBtnResizeState.RESTORE : SysBtnResizeState.MAXIMIZE;
		}
		turboBarView.updateSysBtnResize(newState);
		// check if window can be minimized
		turboBarView.updateSysBtnMinimize(currentWindowFlags.contains(Flag.IS_MINIMIZABLE) ? SysBtnMinimizeState.ENABLED : SysBtnMinimizeState.DISABLED);
	}


	private void updateDateTime(final CalendarChangePayload payload) {
		turboBarView.updateDateTime(String.format("%d-%02d-%02d", payload.getYearNum(), payload.getMonthNum(), payload.getDayNum()));
	}

	//////////////////////////////////////////////////////////////
	// View-sourced logic (i.e. user-invoked actions)
	//////////////////////////////////////////////////////////////
	enum MainButtonAction implements ViewAction {
		TURBO_MENU((ITurboBarPresenter presenter, Event event) -> {

		});

		private final ViewAction mViewAction;

		MainButtonAction(final ViewAction viewAction) {
			mViewAction = viewAction;
		}

		@Override
		public void invoke(final ITurboBarPresenter presenter, final Event event) {
			mViewAction.invoke(presenter, event);
		}
	}

	enum SystemAction implements ViewAction {
		ACTIVATE_LAST_MAXIMIZED((ITurboBarPresenter presenter, Event event) -> {
			if (((MouseEvent) event).getButton().equals(MouseButton.PRIMARY))
				WinEventAgent.INSTANCE.activateLastMaximizedWindow();
			else
				WinEventAgent.INSTANCE.activateFirstMaximizedWindow();
		});

		private final ViewAction mViewAction;

		SystemAction(final ViewAction viewAction) {
			mViewAction = viewAction;
		}

		@Override
		public void invoke(final ITurboBarPresenter presenter, final Event event) {
			mViewAction.invoke(presenter, event);
		}
	}

	/**
	 * SysButton actions
	 */
	enum SysBtnAction implements ViewAction {
		MINIMIZE((ITurboBarPresenter presenter, Event event) -> {
			WinEventAgent.INSTANCE.minimizeForeground();
		}),
		RESIZE((ITurboBarPresenter presenter, Event event) -> {
			WinEventAgent.INSTANCE.resizeForeground();
		}),
		CLOSE((ITurboBarPresenter presenter, Event event) -> {
			WinEventAgent.INSTANCE.closeForeground();
		}),
		FORCE_CLOSE((ITurboBarPresenter presenter, Event event) -> {
			WinEventAgent.INSTANCE.killForeground(KillForegroundHardness.SOFT);
		}),
		KILL((ITurboBarPresenter presenter, Event event) -> {
			WinEventAgent.INSTANCE.killForeground(KillForegroundHardness.HARD);
		});


		private final ViewAction mViewAction;

		SysBtnAction(final ViewAction viewAction) {
			mViewAction = viewAction;
		}

		@Override
		public void invoke(final ITurboBarPresenter presenter, final Event event) {
			mViewAction.invoke(presenter, event);
		}
	}
}
