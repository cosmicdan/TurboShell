# TurboShell

## Requirements

* Windows 10
* Java 8 Runtime



## Features

### System buttons

 - Sends minimize, restore/maximize or close commands to the last-visible foreground window;
 - Holding primary-mouse on the close button will start a visual countdown - after elapsed, it will send a force-close to the foreground window. Similar to the initial function (I.e. Sends WM_QUIT when no hung detected) of "End Task" in Task Manager's default view;
 - Holding secondary-mouse on the close button will start a different visual countdown - after elapsed, will send a KILL signal to the foreground PROCESS. Similar to "End Task" in Task Manager's details view, or the default view if it detects that the app has hung, or a TASKKILL /F command. Be careful with this one!

### General features

- Primary-click on an empty space on the TurboBar will activate the most-recent maximized window. Secondary-click will activate the least-recent maximized window.