package com.cosmicdan.turboshell.models;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Passive model (not observable) for requesting windows environment data on-demand.
 * @author Daniel 'CosmicDan' Connolly
 */
public class WindowsEnvironment {
	/////////////////// "Initialization-on-demand holder" singleton pattern
	public static WindowsEnvironment getInstance() { return LazyHolder.INSTANCE; }
	private WindowsEnvironment() {}
	private static class LazyHolder { static final WindowsEnvironment INSTANCE = new WindowsEnvironment();}
	///////////////////

	/**
	 * Get the work area starting x-position and width
	 * @return An int[] of two members:<br/>
	 * 		0 = Start x-position of work area (usually 0)<br/>
	 * 		1 = Width of work area
	 */
	public int[] getWorkAreaStartAndWidth() {
		int dwFlags = WinUser.MONITOR_DEFAULTTOPRIMARY;
		WinDef.POINT.ByValue pt = new WinDef.POINT.ByValue(0, 0);
		WinUser.HMONITOR hwMonitor = User32.INSTANCE.MonitorFromPoint(pt, dwFlags);
		WinUser.MONITORINFO mainMonitorInfo = new WinUser.MONITORINFO();
		User32.INSTANCE.GetMonitorInfo(hwMonitor, mainMonitorInfo);
		return new int[] {
				mainMonitorInfo.rcWork.left, mainMonitorInfo.rcWork.right - mainMonitorInfo.rcWork.left
		};
	}
}
