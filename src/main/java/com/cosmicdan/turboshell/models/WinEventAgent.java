package com.cosmicdan.turboshell.models;

import com.cosmicdan.turboshell.models.data.SizedStack;
import com.cosmicdan.turboshell.models.data.WindowInfo;
import com.cosmicdan.turboshell.models.data.WindowInfo.Cache;
import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WinEventProc;
import com.sun.jna.ptr.IntByReference;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Agent model for hooking and responding to WinEvents on the system, and also initiates windows-related events triggered by a
 * Presenter. Callbacks are processed in its own thread.
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings({"CyclicClassDependency", "Singleton"})
@Log4j2
public final class WinEventAgent extends AgentModel {
	public static final WinEventAgent INSTANCE = new WinEventAgent();

	public enum KillForegroundHardness{SOFT, HARD}

	// Callback ID's
	// TODO: Replace with objects
	public static final int PAYLOAD_WINDOW_TITLE = 0;
	public static final int PAYLOAD_WINDOW_SYSBTN = 1;

	private final SizedStack<WindowInfo> foregroundWindows = new SizedStack<>(10);
	@Setter	private HWND mInitialTopHwnd;

	// all callbacks as class fields to avoid GC
	private HANDLE hookLocationChange = null;
	private HANDLE hookNameChange = null;
	private HANDLE hookForegroundChange = null;

	private WinEventAgent() {}

	@SuppressWarnings("FeatureEnvy")
	@Override
	protected void serviceStart() {
		log.info("Starting...");
		final WinEventProc callback = new WinEventProcCallback();

		// hook window location changes
		hookLocationChange = setWinEventHook(
				WinUserEx.EVENT_OBJECT_LOCATIONCHANGE,
				WinUserEx.EVENT_OBJECT_LOCATIONCHANGE,
				callback
		);

		hookNameChange = setWinEventHook(
				WinUserEx.EVENT_OBJECT_NAMECHANGE,
				WinUserEx.EVENT_OBJECT_NAMECHANGE,
				callback
		);

		// hook foreground window changes
		hookForegroundChange = setWinEventHook(
				WinUserEx.EVENT_SYSTEM_FOREGROUND,
				WinUserEx.EVENT_SYSTEM_FOREGROUND,
				callback
		);

		// add the current foreground window to the stack, if possible
		final WindowInfo windowInfo = new WindowInfo(mInitialTopHwnd);
		final boolean isRealWindow = windowInfo.isRealWindow();
		if (isRealWindow)
			WindowEventResponse.EVENT_SYSTEM_FOREGROUND.invoke(windowInfo);

		// start runtime/message loop
		final MSG msg = new MSG();
		int result = -1;
		while (0 != result) {
			result = User32Ex.INSTANCE.GetMessage(msg, null, 0, 0);
			if (-1 == result) {
				log.error("Error in GetMessage! Aborting!");
				break;
			} else {
				User32Ex.INSTANCE.TranslateMessage(msg);
				User32Ex.INSTANCE.DispatchMessage(msg);
			}
		}

	}

	@Override
	protected void serviceStop() {
		User32.INSTANCE.UnhookWinEvent(hookLocationChange);
		User32.INSTANCE.UnhookWinEvent(hookNameChange);
		User32.INSTANCE.UnhookWinEvent(hookForegroundChange);
		log.info("Hooks unregistered");
	}

	/**
	 * Convenience method
	 */
	private static HANDLE setWinEventHook(final int eventMin, final int eventMax, final WinEventProc callback) {
		final int outOfContext = 0x0000;
		return User32.INSTANCE.SetWinEventHook(eventMin, eventMax, null, callback, 0, 0, outOfContext);
	}

	/**
	 * Shared callback for the window event hooks we're interested in
	 */
	private static final class WinEventProcCallback implements WinEventProc {
		private WinEventProcCallback() {}

		@Override
		public void callback(final HANDLE hWinEventHook,
								   final DWORD event,
								   final HWND hwnd,
								   final LONG idObject,
								   final LONG idChild,
								   final DWORD dwEventThread,
								   final DWORD dwmsEventTime) {
			if (WinUserEx.OBJID_WINDOW == idObject.longValue()) {
				final WindowInfo windowInfo = new WindowInfo(hwnd);
				if (windowInfo.isRealWindow()) {
					for (final WindowEventResponse response : WindowEventResponse.values()) {
						if (event.longValue() == response.mEventConstant) {
							response.invoke(windowInfo);
						}
					}
				}
			}
		}
	}

	/**
	 * WinEventProcCallback (window event hooks) response logic
	 */
	enum WindowEventResponse implements IWindowEventResponse {
		EVENT_SYSTEM_FOREGROUND(WinUserEx.EVENT_SYSTEM_FOREGROUND, (WindowInfo newWindowInfo) -> {
			log.info("Foreground window changed");
			addOrUpdateWindowStack(newWindowInfo);
			// run callbacks
			INSTANCE.runCallbacks(PAYLOAD_WINDOW_TITLE, newWindowInfo.getTitle());
			INSTANCE.runCallbacks(PAYLOAD_WINDOW_SYSBTN,
					newWindowInfo.isMaximized(), newWindowInfo.canMaximize(), newWindowInfo.hasMinimizeButton());
		}),
		EVENT_OBJECT_LOCATIONCHANGE(WinUserEx.EVENT_OBJECT_LOCATIONCHANGE, (WindowInfo newWindowInfo) -> {
			//log.info("A window location changed");
			addOrUpdateWindowStack(newWindowInfo);
			INSTANCE.runCallbacks(PAYLOAD_WINDOW_SYSBTN,
					newWindowInfo.isMaximized(), newWindowInfo.canMaximize(), newWindowInfo.hasMinimizeButton());
		}),
		EVENT_OBJECT_NAMECHANGE(WinUserEx.EVENT_OBJECT_NAMECHANGE, (WindowInfo newWindowInfo) -> {
			// check if hWnd is the same as top of the stack (i.e. foreground), if not then ignore it
			if (!INSTANCE.foregroundWindows.isEmpty() &&
					INSTANCE.foregroundWindows.peek().getHWnd().equals(newWindowInfo.getHWnd())) {
				// get new title
				final WindowInfo foregroundWindowInfo = INSTANCE.foregroundWindows.peek();
				if (foregroundWindowInfo.refreshTitle())
					INSTANCE.runCallbacks(PAYLOAD_WINDOW_TITLE, foregroundWindowInfo.getTitle());
			}
		});

		/**
		 * Add a new WindowInfo to the foregroundWindows stack
		 * @param newWindowInfo The new WindowInfo object to add
		 * @return true if an existing WindowInfo hWnd was detected (and removed) from the stack, otherwise false
		 */
		private static boolean addOrUpdateWindowStack(final WindowInfo newWindowInfo) {
			// If this hWnd exists anywhere in the stack, remove it first
			boolean isReplaced = false;
			for (int i = 0; i < INSTANCE.foregroundWindows.size(); i++) {
				if (INSTANCE.foregroundWindows.get(i).getHWnd().equals(newWindowInfo.getHWnd())) {
					INSTANCE.foregroundWindows.remove(i);
					isReplaced = true;
					break;
				}
			}
			// add the fresh hwnd to top of the stack
			INSTANCE.foregroundWindows.push(newWindowInfo);
			return isReplaced;
		}

		private final int mEventConstant;
		private final IWindowEventResponse mWindowEventResponse;

		WindowEventResponse(final int eventConstant, final IWindowEventResponse windowEventResponse) {
			mEventConstant = eventConstant;
			mWindowEventResponse = windowEventResponse;
		}

		@Override
		public void invoke(final WindowInfo newWindowInfo) {
			mWindowEventResponse.invoke(newWindowInfo);
		}
	}

	@FunctionalInterface
	interface IWindowEventResponse {
		void invoke(WindowInfo newWindowInfo);
	}

	//////////////////////////////////////////////////////////////
	// Presenter-requested actions (i.e. user-invoked)
	//////////////////////////////////////////////////////////////

	/**
	 * Perform minimize on the foreground window.
	 */
	public void minimizeForeground() {
		if (!foregroundWindows.isEmpty()) {
			User32Ex.INSTANCE.ShowWindowAsync(foregroundWindows.peek().getHWnd(), WinUser.SW_MINIMIZE);
		}
	}

	/**
	 * Perform resize (restore/maximize) on the foreground window.
	 */
	public void resizeForeground() {
		if (!foregroundWindows.isEmpty()) {
			final WindowInfo foregroundWindow = foregroundWindows.peek();
			User32Ex.INSTANCE.ShowWindowAsync(foregroundWindow.getHWnd(),
					foregroundWindow.isMaximized() ? WinUser.SW_RESTORE : WinUser.SW_MAXIMIZE);
		}
	}

	/**
	 * Politely close the foreground window.
	 */
	public void closeForeground() {
		if (!foregroundWindows.isEmpty()) {
			// CRASHES! Issue reported - https://github.com/java-native-access/jna/issues/905
			//	- Not really a big deal, this doesn't need to be especially fast. Just use interface mapping.
			//User32Ex.INSTANCE.PostMessageW(Environment.getInstance().getLastActiveHwnd(), WinUser.WM_CLOSE, null, null);
			User32.INSTANCE.PostMessage(foregroundWindows.peek().getHWnd(), WinUser.WM_CLOSE, null, null);
		}
	}

	/**
	 * Kill the foreground window.
	 * @param hardness If HARD, will force kill it via TerminateProcess. Otherwise sends WM_QUIT.
	 */
	public void killForeground(final KillForegroundHardness hardness) {
		if (!foregroundWindows.isEmpty()) {
			final HWND foregroundWindow = foregroundWindows.peek().getHWnd();
			if (KillForegroundHardness.HARD == hardness) {
				final IntByReference pid = new IntByReference();
				User32.INSTANCE.GetWindowThreadProcessId(foregroundWindow, pid);
				final HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_TERMINATE, false, pid.getValue());
				final boolean result = Kernel32.INSTANCE.TerminateProcess(hProcess, 0);
				log.info("Called TerminateProcess on hWnd {}; result = {} (GetLastError = {})",
						foregroundWindow, result, Kernel32.INSTANCE.GetLastError());
			} else {
				User32.INSTANCE.PostMessage(foregroundWindow, WinUser.WM_QUIT, null, null);
				log.info("Sent WM_QUIT message to hWnd " + foregroundWindow);
			}
		}
	}
}
