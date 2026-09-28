Add-Type @'
using System;
using System.Runtime.InteropServices;
public class Win32F {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
}
'@
Add-Type -AssemblyName System.Windows.Forms

$target = Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' } | Select-Object -First 1
if ($target -eq $null) {
    Write-Output "no minecraft window found"
    exit 1
}
Write-Output ("window: " + $target.MainWindowTitle)
[Win32F]::ShowWindow($target.MainWindowHandle, 4) | Out-Null
[Win32F]::SetForegroundWindow($target.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 800
$fg = [Win32F]::GetForegroundWindow()
Write-Output ("foreground now: " + $fg)
[System.Windows.Forms.SendKeys]::SendWait('k')
Write-Output "sent k"