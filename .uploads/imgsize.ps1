Add-Type -AssemblyName System.Drawing
$path = 'f:\html\src\assets\guangdong-map.png'
try {
  $img = [System.Drawing.Image]::FromFile($path)
  $w = $img.Width
  $h = $img.Height
  $img.Dispose()
  "IMAGE_WIDTH=$w" | Out-File -FilePath 'f:\html\.uploads\imgsize_result.txt' -Encoding UTF8
  "IMAGE_HEIGHT=$h" | Out-File -FilePath 'f:\html\.uploads\imgsize_result.txt' -Append -Encoding UTF8
  "DONE" | Out-File -FilePath 'f:\html\.uploads\imgsize_result.txt' -Append -Encoding UTF8
} catch {
  "ERROR: $_" | Out-File -FilePath 'f:\html\.uploads\imgsize_result.txt' -Encoding UTF8
}
