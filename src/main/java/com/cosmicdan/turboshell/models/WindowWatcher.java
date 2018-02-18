package com.cosmicdan.turboshell.models;

import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import lombok.extern.log4j.Log4j2;

/**
 * Active model (observable service) that hooks window-related events on the system - mostly the foreground window properties.
 * A loop that waits for callback messages runs in it's own thread.
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public class WindowWatcher extends ModelService {
	private final Object windowWatcherLock = new Object();

	///////////////////
	// Observable payload fields
	///////////////////

	public static final int PAYLOAD_WINDOW_TITLE = 0;
	private final ObservablePayload mForegroundWindowTitle = new ObservablePayload(PAYLOAD_WINDOW_TITLE, this);


	
	///////////////////
	// Thread related things
	///////////////////

	@Override
	public ModelServiceThread getThread() {
		return new WindowWatcherThread();
	}

	private class WindowWatcherThread extends ModelServiceThread {
		private WinNT.HANDLE hookLocationOrNameChange;
		private WinNT.HANDLE hookForegroundChange;

		@Override
		public void serviceStart() {
			log.info("Starting...");
			WinUser.WinEventProc callback = new WinEventProcCallback();

			// hook window location and name changes
			hookLocationOrNameChange = SetWinEventHook(
					WinUserEx.EVENT_OBJECT_LOCATIONCHANGE,
					WinUserEx.EVENT_OBJECT_NAMECHANGE,
					callback
			);

			// hook foreground window changes
			hookForegroundChange = SetWinEventHook(
					WinUserEx.EVENT_SYSTEM_FOREGROUND,
					WinUserEx.EVENT_SYSTEM_FOREGROUND,
					callback
			);

			WinUser.MSG msg = new WinUser.MSG();
			int result = -1;
			while (0 != result) {
				result = User32Ex.INSTANCE.GetMessage(msg, null, 0, 0);
				if (result == -1) {
					log.error("Error in GetMessage! Aborting!");
					break;
				} else {
					User32Ex.INSTANCE.TranslateMessage(msg);
					User32Ex.INSTANCE.DispatchMessage(msg);
				}
			}

		}

		@Override
		public void serviceStop() {
			User32.INSTANCE.UnhookWinEvent(hookLocationOrNameChange);
			User32.INSTANCE.UnhookWinEvent(hookForegroundChange);
			log.info("Hooks unregistered");
		}

		/**
		 * Convenience method
		 */
		private WinNT.HANDLE SetWinEventHook(int eventMin, int eventMax, WinUser.WinEventProc callback) {
			final int WINEVENT_OUTOFCONTEXT = 0x0000;
			return User32.INSTANCE.SetWinEventHook(eventMin, eventMax, null, callback, 0, 0, WINEVENT_OUTOFCONTEXT);
		}
	}

	/**
	 * Shared callback for all window hooks we're interested in
	 */
	private class WinEventProcCallback implements WinUser.WinEventProc {
		@Override
		public void callback(WinNT.HANDLE hWinEventHook,
							 WinDef.DWORD event,
							 WinDef.HWND hWnd,
							 WinDef.LONG idObject,
							 WinDef.LONG idChild,
							 WinDef.DWORD dwEventThread,
							 WinDef.DWORD dwmsEventTime) {
			if (WinUserEx.OBJID_WINDOW == idObject.longValue()) {
				switch ((int) event.longValue()) {
					case WinUserEx.EVENT_SYSTEM_FOREGROUND:
						//log.info("Foreground window changed");
						break;
					case WinUserEx.EVENT_OBJECT_LOCATIONCHANGE:
						//log.info("Foreground window location changed");
						break;
					case WinUserEx.EVENT_OBJECT_NAMECHANGE:
						//log.info("Foreground window name changed");
						updateWindowTitle(hWnd);
						break;
					default:
						log.warn("WinEventProc callback somehow got an unknown event: " + Long.toHexString(event.longValue()));
						break;
				}
			}
		}

		private void updateWindowTitle(WinDef.HWND hWnd) {
			// get the title for the new window
			final int titleLength = User32Ex.INSTANCE.GetWindowTextLength(hWnd) + 1;
			final char[] title = new char[titleLength];
			final int length = User32Ex.INSTANCE.GetWindowText(hWnd, title, title.length);
			String windowTitle = "[No title/process]";
			if (length > 0)
				windowTitle = new String(title);
			// TODO: else set process name to title?
			//log.info("Title refresh to '" + windowTitle + "'");
			mForegroundWindowTitle.set(windowTitle);
		}
	}
}
