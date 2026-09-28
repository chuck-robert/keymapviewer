param([string]$outPath)

Add-Type @'
using System;
using System.Runtime.InteropServices;
public class WCap {
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
}
'@
Add-Type -AssemblyName System.Drawing,System.Windows.Forms

$proc = Get-Process | Where-Object { $_.ProcessName -eq 'java' -and $_.MainWindowTitle -like 'Minecraft*' -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if ($null -eq $proc) {
    Write-Output "no minecraft java window"
    exit 1
}
$hwnd = $proc.MainWindowHandle
Write-Output ("title: " + $proc.MainWindowTitle)
if ([WCap]::IsIconic($hwnd)) { [WCap]::ShowWindow($hwnd, 9) | Out-Null; Start-Sleep -Milliseconds 600 }
[WCap]::SetForegroundWindow($hwnd) | Out-Null
Start-Sleep -Milliseconds 600

$rect = New-Object WCap+RECT
[WCap]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$w = $rect.R - $rect.L
$h = $rect.B - $rect.T
if ($w -le 0 -or $h -le 0) { Write-Output "bad rect"; exit 1 }
Write-Output ("rect: $w x $h at ($($rect.L),$($rect.T))")

$bmp = New-Object System.Drawing.Bitmap $w, $h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($rect.L, $rect.T, 0, 0, (New-Object System.Drawing.Size $w, $h))
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Output "saved $outPath"