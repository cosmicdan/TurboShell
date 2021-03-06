package com.cosmicdan.turboshell.winapi.model;

import com.cosmicdan.turboshell.TurboShellConfig;
import com.cosmicdan.turboshell.winapi.User32Ex;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT.ByValue;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HMONITOR;
import com.sun.jna.platform.win32.WinUser.MONITORINFO;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Passive model (non-observable Singleton) for requesting windows environment data on-demand.
 * @author Daniel 'CosmicDan' Connolly
 */
@UtilityClass
@Log4j2(topic = "WindowsEnvironment")
public final class WindowsEnvironment extends User32Ex {
	private static final int CLASS_NAME_CHAR_LENGTH = 512;

	/**
	 * Get the work area starting x-position and width
	 * @return An int[] of two members:<br>
	 * 		0 = Start x-position of work area (usually 0)<br>
	 * 		1 = Width of work area
	 */
	public static int[] getWorkAreaXAndWidth() {
		final int dwFlags = WinUser.MONITOR_DEFAULTTOPRIMARY;
		final ByValue pt = new ByValue(0, 0);
		final HMONITOR hwMonitor = User32.INSTANCE.MonitorFromPoint(pt, dwFlags);
		final MONITORINFO mainMonitorInfo = new MONITORINFO();
		User32.INSTANCE.GetMonitorInfo(hwMonitor, mainMonitorInfo);
		return new int[] {
				mainMonitorInfo.rcWork.left, mainMonitorInfo.rcWork.right - mainMonitorInfo.rcWork.left
		};
	}

	public static boolean isDesktopFocused() {
		boolean isDesktop = false;
		final HWND foregroundHWnd = USER32.GetForegroundWindow();

		final char[] windowClassNameChar = new char[CLASS_NAME_CHAR_LENGTH];
		USER32.GetClassName(foregroundHWnd, windowClassNameChar, CLASS_NAME_CHAR_LENGTH);
		final String windowClassName = Native.toString(windowClassNameChar);
		if (TurboShellConfig.getFullscreenHideExcludeClasses().contains(windowClassName))
			isDesktop = true;
		return isDesktop;
	}
}
