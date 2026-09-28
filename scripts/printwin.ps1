param([string]$outPath)

Add-Type @'
using System;
using System.Runtime.InteropServices;
public class WCap2 {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdcBlt, uint nFlags);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
}
'@
Add-Type -AssemblyName System.Drawing

$proc = Get-Process | Where-Object { $_.ProcessName -eq 'java' -and $_.MainWindowTitle -like 'Minecraft*' -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if ($null -eq $proc) { Write-Output "no minecraft window"; exit 1 }
$hwnd = $proc.MainWindowHandle
Write-Output ("title: " + $proc.MainWindowTitle)
if ([WCap2]::IsIconic($hwnd)) { [WCap2]::ShowWindow($hwnd, 9) | Out-Null; Start-Sleep -Milliseconds 800 }
[WCap2]::SetForegroundWindow($hwnd) | Out-Null

$rect = New-Object WCap2+RECT
[WCap2]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$w = $rect.R - $rect.L
$h = $rect.B - $rect.T
if ($w -le 0 -or $h -le 0) { Write-Output "bad rect"; exit 1 }

$bmp = New-Object System.Drawing.Bitmap $w, $h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
$ok = [WCap2]::PrintWindow($hwnd, $hdc, 2)
$g.ReleaseHdc($hdc)
$g.Dispose()
Write-Output ("PrintWindow ok: " + $ok)
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "saved $outPath"