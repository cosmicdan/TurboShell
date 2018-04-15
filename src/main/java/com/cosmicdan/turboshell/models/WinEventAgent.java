package com.cosmicdan.turboshell.models;

import com.cosmicdan.turboshell.models.data.SizedStack;
import com.cosmicdan.turboshell.models.data.WindowInfo;
import com.cosmicdan.turboshell.models.data.WindowInfo.Cache;
import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WinEventProc;
import lombok.extern.log4j.Log4j2;

/**
 * Agent model for hooking and responding to WinEvents on the system, and also initiates windows-related events triggered by a
 * Presenter. Callbacks are processed in its own thread.
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings("CyclicClassDependency")
@Log4j2
public class WinEventAgent extends AgentModel {
	// Callback ID's
	public static final int PAYLOAD_WINDOW_TITLE = 0;

	private final SizedStack<WindowInfo> foregroundWindows = new SizedStack<>(10);
	private final HWND mInitialTopHwnd;

	// all callbacks as class fields to avoid GC
	private HANDLE hookLocationChange = null;
	private HANDLE hookNameChange = null;
	private HANDLE hookForegroundChange = null;

	public WinEventAgent(final HWND initialTopHwnd) {
		mInitialTopHwnd = initialTopHwnd;
	}

	@SuppressWarnings("FeatureEnvy")
	@Override
	protected final void serviceStart() {
		log.info("Starting...");
		final WinEventProc callback = new WinEventProcCallback(this);

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

		// add the current foreground window to the stack
		addCandidateToForegroundStack(new DWORD(WinUserEx.EVENT_SYSTEM_FOREGROUND), mInitialTopHwnd);

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
	protected final void serviceStop() {
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
	 * Shared callback for all window hooks we're interested in
	 */
	private static class WinEventProcCallback implements WinEventProc {
		private final WinEventAgent mWinEventAgent;

		private WinEventProcCallback(WinEventAgent winEventAgent) {
			mWinEventAgent = winEventAgent;
		}

		@Override
		public void callback(final HANDLE hWinEventHook,
								   final DWORD event,
								   final HWND hwnd,
								   final LONG idObject,
								   final LONG idChild,
								   final DWORD dwEventThread,
								   final DWORD dwmsEventTime) {
			if (WinUserEx.OBJID_WINDOW == idObject.longValue()) {
				mWinEventAgent.addCandidateToForegroundStack(event, hwnd);
			}
		}
	}

	/**
	 * Called by WinEventProcCallback when foreground window changes to add it to the foreground window stack,
	 * if it's valid. Also called on startup.
	 * @param event Should be WinUserEx.EVENT_SYSTEM_FOREGROUND
	 * @param hWnd The hWnd that has come to the foreground
	 * @return true if isRealWindow was true, otherwise false
	 */
	private boolean addCandidateToForegroundStack(final DWORD event, final HWND hWnd) {
		final WindowInfo windowInfo = new WindowInfo(hWnd);
		final boolean isRealWindow = windowInfo.isRealWindow();
		if (isRealWindow)
			WindowEventResponse.invoke(event, this, windowInfo);
		return isRealWindow;
	}

	@SuppressWarnings("FeatureEnvy")
	private enum WindowEventResponse {
		EVENT_SYSTEM_FOREGROUND(WinUserEx.EVENT_SYSTEM_FOREGROUND) {
			@Override
			public void invoke(final WinEventAgent winEventAgent, final WindowInfo newWindowInfo) {
				//log.info("Foreground window changed");
				// If the new hWnd exists anywhere in the stack, remove it
				for (int i = 0; i < winEventAgent.foregroundWindows.size(); i++) {
					if (winEventAgent.foregroundWindows.get(i).getHWnd().equals(newWindowInfo.getHWnd())) {
						winEventAgent.foregroundWindows.remove(i);
						break;
					}
				}
				// add the fresh hwnd to top of the stack
				winEventAgent.foregroundWindows.push(newWindowInfo);
				// callback for window title update
				winEventAgent.runCallbacks(PAYLOAD_WINDOW_TITLE, newWindowInfo.getTitle());
			}
		},
		EVENT_OBJECT_LOCATIONCHANGE(WinUserEx.EVENT_OBJECT_LOCATIONCHANGE) {
			@Override
			public void invoke(final WinEventAgent winEventAgent, final WindowInfo newWindowInfo) {
				//log.info("A window location changed");
			}
		},
		EVENT_OBJECT_NAMECHANGE(WinUserEx.EVENT_OBJECT_NAMECHANGE) {
			@Override
			public void invoke(final WinEventAgent winEventAgent, final WindowInfo newWindowInfo) {
				// check if hWnd is the same as top of the stack (i.e. foreground), if not then ignore it
				if (!winEventAgent.foregroundWindows.isEmpty() &&
						winEventAgent.foregroundWindows.peek().getHWnd().equals(newWindowInfo.getHWnd())) {
					// get new title
					final WindowInfo foregroundWindowInfo = winEventAgent.foregroundWindows.peek();
					final String newTitle = foregroundWindowInfo.getTitle(Cache.SKIP);
					// update window title only if required
					if (!newTitle.equals(foregroundWindowInfo.getTitle())) {
						foregroundWindowInfo.setTitle(newTitle);
						winEventAgent.runCallbacks(PAYLOAD_WINDOW_TITLE, newTitle);
					}
				}
			}
		};

		private final long mEventConstant;

		WindowEventResponse(final long eventConstant) {
			mEventConstant = eventConstant;
		}

		@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
		static void invoke(final DWORD event, final WinEventAgent winEventAgent, final WindowInfo newWindowInfo) {
			for (final WindowEventResponse response : values()) {
				if (event.longValue() == response.mEventConstant) {
					response.invoke(winEventAgent, newWindowInfo);
				}
			}
			// some other event happened, ignore it
		}

		protected abstract void invoke(WinEventAgent winEventAgent, WindowInfo newWindowInfo);
	}

	//////////////////////////////////////////////////////////////
	// Presenter-requested actions (i.e. user-invoked)
	//////////////////////////////////////////////////////////////

	public void minimizeForeground() {
		if (!foregroundWindows.isEmpty()) {
			User32Ex.INSTANCE.ShowWindowAsync(foregroundWindows.peek().getHWnd(), WinUser.SW_MINIMIZE);
		}
	}
}
