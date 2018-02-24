package com.cosmicdan.turboshell.models;

import com.cosmicdan.turboshell.models.data.SizedStack;
import com.cosmicdan.turboshell.models.data.WindowInfo;
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
	// Callback ID's
	public static final int PAYLOAD_WINDOW_TITLE = 0;

	private final SizedStack foregroundWindows = new SizedStack<WindowInfo>(10);

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
				WindowInfo windowInfo = new WindowInfo(hWnd);
				if (windowInfo.getStyleInfo().isReal()) {
					WindowEvent windowEvent = WindowEvent.getEvent((int) event.longValue());
					windowEvent.run(WindowWatcher.this, windowInfo);
				}
			}
		}
	}


	///////////////////
	// Window event constants.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/dd318066(v=vs.85).aspx
	///////////////////
	enum WindowEvent {
		EVENT_SYSTEM_FOREGROUND(WinUserEx.EVENT_SYSTEM_FOREGROUND) {
			@Override
			public void run(final WindowWatcher windowWatcher, WindowInfo windowInfo) {
				//log.info("Foreground window changed");
				windowWatcher.foregroundWindows.push(windowInfo);
				windowWatcher.updateWindowTitle(windowInfo.getHWnd());
			}
		},
		EVENT_OBJECT_LOCATIONCHANGE(WinUserEx.EVENT_OBJECT_LOCATIONCHANGE) {
			@Override
			public void run(final WindowWatcher windowWatcher, WindowInfo windowInfo) {
				//log.info("A window location changed");
			}
		},
		EVENT_OBJECT_NAMECHANGE(WinUserEx.EVENT_OBJECT_NAMECHANGE) {
			@Override
			public void run(final WindowWatcher windowWatcher, WindowInfo windowInfo) {
				//log.info("A window title changed");
				windowWatcher.updateWindowTitle(windowInfo.getHWnd());
			}
		};

		private final int mEventConstant;

		WindowEvent(int eventConstant) {
			mEventConstant = eventConstant;
		}

		public static WindowEvent getEvent(int eventConstant) {
			for (WindowEvent windowEvent : values()) {
				if (eventConstant == windowEvent.mEventConstant) {
					return windowEvent;
				}
			}
			throw new RuntimeException("Unrecognized WindowEvent constant: " + eventConstant);
		}

		public abstract void run(WindowWatcher windowWatcher, WindowInfo windowInfo);
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
		runCallbacks(PAYLOAD_WINDOW_TITLE, windowTitle);
	}

}
