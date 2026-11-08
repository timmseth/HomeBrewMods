# Downloads Copernicus GLO-90 (90 m) elevation tiles for the big mountain ranges into tools\raw\glo90.
# Public AWS Open Data bucket, no account. Resumable: re-run to continue; ocean tiles are skipped.
# Run:  powershell -ExecutionPolicy Bypass -File tools\get_glo90.ps1
$ProgressPreference = 'SilentlyContinue'
$out = Join-Path $PSScriptRoot 'raw\glo90'
New-Item -ItemType Directory -Force -Path $out | Out-Null
# name, south, north, west, east (whole degrees)
$regions = @(
  @('Alps',      43, 48,    5,   17),
  @('Rockies',   31, 60, -125, -104),
  @('Himalaya',  26, 40,   70,  100),
  @('Andes',    -56, 11,  -82,  -62),
  @('Caucasus',  40, 44,   38,   50)
)
$base = 'https://copernicus-dem-90m.s3.amazonaws.com'
foreach ($r in $regions) {
  Write-Host "== $($r[0]) =="
  for ($lat = $r[1]; $lat -lt $r[2]; $lat++) {
    for ($lon = $r[3]; $lon -lt $r[4]; $lon++) {
      $ns = if ($lat -ge 0) { 'N' } else { 'S' }; $ew = if ($lon -ge 0) { 'E' } else { 'W' }
      $n = "Copernicus_DSM_COG_30_{0}{1:D2}_00_{2}{3:D3}_00_DEM" -f $ns, [math]::Abs($lat), $ew, [math]::Abs($lon)
      $f = Join-Path $out "$n.tif"
      if ((Test-Path $f) -or (Test-Path "$f.none")) { continue }
      try { Invoke-WebRequest -UseBasicParsing -Uri "$base/$n/$n.tif" -OutFile "$f.part"; Move-Item "$f.part" $f; Write-Host "  got $n" }
      catch { New-Item -ItemType File -Path "$f.none" | Out-Null; Remove-Item "$f.part" -ErrorAction SilentlyContinue }
    }
  }
}
Write-Host "done"
