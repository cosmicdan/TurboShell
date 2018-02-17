#NoTrayIcon
#Region ;**** Directives created by AutoIt3Wrapper_GUI ****
#AutoIt3Wrapper_Icon=..\turboshell.ico
#AutoIt3Wrapper_Compression=4
#EndRegion ;**** Directives created by AutoIt3Wrapper_GUI ****
#cs ----------------------------------------------------------------------------

	AutoIt Version: 3.3.14.2
	Author:         Daniel 'CosmicDan' Connolly

	Script Function:
	Launcher for WinRun4J to populate Java field in ini file
	TODO: Replace WinRun4J completely with this launcher? I'm not sure if it's
	possible to call jvm.dll from AutoIt...

#ce ----------------------------------------------------------------------------

Global Const $STDERR_MERGED = 8
Global Const $STR_ENTIRESPLIT = 2
Global Const $MB_ICONERROR = 16
Global Const $MB_OK = 0
Global Const $MB_YESNO = 4
Global Const $MB_IDYES = 6
Global Const $FO_OVERWRITE = 2

Global Const $sTurboShellIniPath = @ScriptDir & "\bin\TurboShell.ini"
Global Const $iIniVmLinePrefix = "vm.location="

Global $sJvmDllPath = ""
Global $iIniVmLineNumber = -1 ; 0-based

; First make sure the TurboShell.ini file exists
If Not FileExists($sTurboShellIniPath) Then
	MsgBox(BitOR($MB_ICONERROR, $MB_OK), "Error", "TurboShell not found. Corrupt installation?")
	Exit
EndIf

; Check the ini file's for vm option
Global $asIniFileLines = FileReadToArray($sTurboShellIniPath)
For $i = 0 To UBound($asIniFileLines) - 1
	If StringInStr($asIniFileLines[$i], $iIniVmLinePrefix) Then
		$sJvmDllPath = StringReplace($asIniFileLines[$i], $iIniVmLinePrefix, "")
		If Not ($sJvmDllPath == "") And FileExists($sJvmDllPath) Then
			ConsoleWrite("Existing JVM DLL path is valid" & @CRLF)
			ExitLoop ; no need to continue
		Else
			$sJvmDllPath = "" ; clear invalid result
			$iIniVmLineNumber = $i
			$asIniFileLines[$iIniVmLineNumber] = $iIniVmLinePrefix
			WriteIniFile()
			ConsoleWrite("Existing JVM DLL path at line " & $iIniVmLineNumber & " is not valid" & @CRLF)
		EndIf
	EndIf
Next

If ($sJvmDllPath == "") And ($iIniVmLineNumber == -1) Then
	; there is no vm.location entry in the ini at all - append to the array
	Local $iArraySize = UBound($asIniFileLines)
	ReDim $asIniFileLines[$iArraySize + 1]
	$asIniFileLines[$iArraySize] = $iIniVmLinePrefix
	$iIniVmLineNumber = $iArraySize
	ConsoleWrite("No JVM DLL path entry specified - new entry appended at line " & $iArraySize & @CRLF)
EndIf

; Attempt 1 - find java via install information in registry (fastest)
If ($sJvmDllPath == "") Then
	; try JRE first
	$sJAVA_HOME = RegRead("HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment\1.8", "JavaHome")
	If Not @error Then
		Local $sJvmDllResult = GetJvmDllFromJavaPath($sJAVA_HOME)
		If Not ($sJvmDllResult == "") Then
			$sJvmDllPath = $sJvmDllResult
			ConsoleWrite("Java DLL found via JRE registry key: " & $sJvmDllPath & @CRLF)
		EndIf
	Else
		; try JDK
		$sJAVA_HOME = RegRead("HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\1.8", "JavaHome")
		If Not @error Then
			Local $sJvmDllResult = GetJvmDllFromJavaPath($sJAVA_HOME)
			If Not ($sJvmDllResult == "") Then
				$sJvmDllPath = $sJvmDllResult
				ConsoleWrite("Java DLL found via JDK registry key: " & $sJvmDllPath & @CRLF)
			EndIf
		EndIf
	EndIf
EndIf

; Attempt 2 - find java at JAVA_HOME (little bit slower)
If ($sJvmDllPath == "") Then
	Local $sJAVA_HOME = EnvGet("JAVA_HOME")
	If Not ($sJAVA_HOME == "") Then
		If FileExists($sJAVA_HOME & "\bin\java.exe") Then
			Local $sJvmDllResult = GetJvmDllFromJavaPath($sJAVA_HOME)
			If Not ($sJvmDllResult == "") Then
				$sJvmDllPath = $sJvmDllResult
				ConsoleWrite("Java DLL found via JAVA_HOME: " & $sJvmDllPath & @CRLF)
			EndIf
		EndIf
	EndIf
EndIf

; Attempt 3 - find java on PATH (slowest)
If ($sJvmDllPath == "") Then
	Local $iPID = Run(@ComSpec & ' /C where java.exe', @ScriptDir, @SW_HIDE, $STDERR_MERGED)
	ProcessWaitClose($iPID)
	Local $sWhereResult = StdoutRead($iPID)
	If (StringInStr($sWhereResult, @CRLF)) Then
		Local $sWhereResultNormalized = StringReplace($sWhereResult, @CRLF, "|")
		Local $asWhereResults = StringSplit($sWhereResultNormalized, "|")
		For $i = 1 To $asWhereResults[0]
			If (IsJava8Exe($asWhereResults[$i])) Then
				Local $sJvmDllResult = GetJvmDllFromJavaPath(StringReplace($asWhereResults[$i], "\bin\java.exe", ""))
				If Not ($sJvmDllResult == "") Then
					$sJvmDllPath = $sJvmDllResult
					ConsoleWrite("Java DLL found via PATH: " & $sJvmDllPath & @CRLF)
					ExitLoop
				EndIf
			EndIf
		Next
	EndIf
EndIf


If ($sJvmDllPath == "") Then
	Local $sResult = MsgBox(BitOR($MB_ICONERROR, $MB_YESNO), "Java 1.8 not found", "TurboShell requires Java 1.8 to run." & @CRLF & @CRLF & _
			"Would you like to visit the Java Download page now?")
	If ($sResult == $MB_IDYES) Then ShellExecute("https://java.com/en/download/")
Else
	If Not ($iIniVmLineNumber == -1) Then
		; ini file needs to be updated
		$asIniFileLines[$iIniVmLineNumber] = $iIniVmLinePrefix & $sJvmDllPath
		WriteIniFile()
		ConsoleWrite("Updated TurboShell.ini" & @CRLF)
	EndIf
	ConsoleWrite("Calling " & @ScriptDir & "\bin\TurboShell.exe..." & @CRLF)
	RunWait(@ScriptDir & "\bin\TurboShell.exe", @ScriptDir)
	; TODO: Logging? Kill signal detection?
EndIf

; done. Helper functions below.

Func IsJava8Exe($sExePath)
	$iPID = Run($sExePath & " -version", @ScriptDir, @SW_HIDE, $STDERR_MERGED)
	ProcessWaitClose($iPID)
	Local $sJavaOutput = StdoutRead($iPID)
	Local $asJavaVersionLines = StringSplit(StringReplace($sJavaOutput, @CRLF, "|"), "|")
	For $j = 1 To $asJavaVersionLines[0]
		If (StringInStr($asJavaVersionLines[$j], "java version")) Then
			If (StringInStr($asJavaVersionLines[$j], '"1.8.0')) Then
				Return True
			EndIf
		EndIf
	Next
	Return False
EndFunc   ;==>IsJava8Exe

Func GetJvmDllFromJavaPath($sPath)
	; check for private JRE in JDK
	Local $sPathCheck = $sPath & "\jre\bin\server\jvm.dll"
	If FileExists($sPathCheck) Then Return $sPathCheck
	; check regular JRE
	$sPathCheck = $sPath & "\bin\server\jvm.dll"
	If FileExists($sPathCheck) Then Return $sPathCheck
	; not found :(
	Return ""
EndFunc   ;==>GetJvmDllFromJavaPath

Func WriteIniFile()
	Local $hIniFile = FileOpen($sTurboShellIniPath, $FO_OVERWRITE)
	For $i = 0 To UBound($asIniFileLines) - 1
		FileWriteLine($hIniFile, $asIniFileLines[$i])
	Next
	FileClose($hIniFile)
EndFunc   ;==>WriteIniFile

