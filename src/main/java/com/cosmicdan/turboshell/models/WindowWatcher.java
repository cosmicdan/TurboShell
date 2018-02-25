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
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WinEventProc;
import lombok.extern.log4j.Log4j2;

/**
 * Active model (observable service) that hooks window-related events on the system - mostly the foreground window
 * properties. A loop that waits for callback messages runs in it's own thread.
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public class WindowWatcher extends ModelService {
	// Callback ID's
	public static final int PAYLOAD_WINDOW_TITLE = 0;

	private final SizedStack<WindowInfo> foregroundWindows = new SizedStack<>(10);

	public WindowWatcher() {
	}

	// all callbacks as class fields to avoid GC
	private HANDLE hookLocationChange = null;
	private HANDLE hookNameChange = null;
	private HANDLE hookForegroundChange = null;

	@Override
	protected final void serviceStart() {
		log.info("Starting...");
		final WinEventProc callback = new WinEventProcCallback();

		// hook window location changes
		hookLocationChange = SetWinEventHook(
				WinUserEx.EVENT_OBJECT_LOCATIONCHANGE,
				WinUserEx.EVENT_OBJECT_LOCATIONCHANGE,
				callback
		);

		hookNameChange = SetWinEventHook(
				WinUserEx.EVENT_OBJECT_NAMECHANGE,
				WinUserEx.EVENT_OBJECT_NAMECHANGE,
				callback
		);

		// hook foreground window changes
		hookForegroundChange = SetWinEventHook(
				WinUserEx.EVENT_SYSTEM_FOREGROUND,
				WinUserEx.EVENT_SYSTEM_FOREGROUND,
				callback
		);

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
	private HANDLE SetWinEventHook(final int eventMin, final int eventMax, final WinEventProc callback) {
		final int outOfContext = 0x0000;
		return User32.INSTANCE.SetWinEventHook(eventMin, eventMax, null, callback, 0, 0, outOfContext);
	}

	/**
	 * Shared callback for all window hooks we're interested in
	 */
	private class WinEventProcCallback implements WinEventProc {
		private WinEventProcCallback() {
		}

		@Override
		public final void callback(final HANDLE hWinEventHook,
								   final DWORD event,
								   final HWND hwnd,
								   final LONG idObject,
								   final LONG idChild,
								   final DWORD dwEventThread,
								   final DWORD dwmsEventTime) {
			if (WinUserEx.OBJID_WINDOW == idObject.longValue()) {
				final WindowInfo windowInfo = new WindowInfo(hwnd);
				if (windowInfo.isRealWindow()) {
					WindowEventResponse.invoke(event, WindowWatcher.this, windowInfo);
				}
			}
		}
	}

	private enum WindowEventResponse {
		EVENT_SYSTEM_FOREGROUND(WinUserEx.EVENT_SYSTEM_FOREGROUND) {
			@Override
			public void invoke(final WindowWatcher windowWatcher, final WindowInfo newWindowInfo) {
				//log.info("Foreground window changed");
				// If the new hWnd exists anywhere in the stack, remove it
				for (int i = 0; i < windowWatcher.foregroundWindows.size(); i++) {
					if (windowWatcher.foregroundWindows.get(i).getHWnd().equals(newWindowInfo.getHWnd())) {
						windowWatcher.foregroundWindows.remove(i);
						break;
					}
				}
				// add the fresh hwnd to top of the stack
				windowWatcher.foregroundWindows.push(newWindowInfo);
				// callback for window title update
				windowWatcher.runCallbacks(PAYLOAD_WINDOW_TITLE, newWindowInfo.getTitle());
			}
		},
		EVENT_OBJECT_LOCATIONCHANGE(WinUserEx.EVENT_OBJECT_LOCATIONCHANGE) {
			@Override
			public void invoke(final WindowWatcher windowWatcher, final WindowInfo newWindowInfo) {
				//log.info("A window location changed");
			}
		},
		EVENT_OBJECT_NAMECHANGE(WinUserEx.EVENT_OBJECT_NAMECHANGE) {
			@Override
			public void invoke(final WindowWatcher windowWatcher, final WindowInfo newWindowInfo) {
				// check if hWnd is the same as top of the stack (i.e. foreground), if not then ignore it
				if (windowWatcher.foregroundWindows.peek().getHWnd().equals(newWindowInfo.getHWnd())) {
					// get new title
					final WindowInfo foregroundWindowInfo = windowWatcher.foregroundWindows.peek();
					final String newTitle = foregroundWindowInfo.getTitle(Cache.SKIP);
					// update window title only if required
					if (!newTitle.equals(foregroundWindowInfo.getTitle())) {
						foregroundWindowInfo.setTitle(newTitle);
						windowWatcher.runCallbacks(PAYLOAD_WINDOW_TITLE, newTitle);
					}
				}
			}
		};

		private final long mEventConstant;

		WindowEventResponse(final long eventConstant) {
			mEventConstant = eventConstant;
		}

		@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
		static void invoke(final DWORD event, final WindowWatcher windowWatcher, final WindowInfo newWindowInfo) {
			for (final WindowEventResponse response : values()) {
				if (event.longValue() == response.mEventConstant) {
					response.invoke(windowWatcher, newWindowInfo);
				}
			}
			// some other event happened, ignore it
		}

		protected abstract void invoke(WindowWatcher windowWatcher, WindowInfo newWindowInfo);
	}
}
