[CmdletBinding()]
param(
    [string]$OutputPath = "whisper-recorder-release.jks",
    [string]$Alias = "whisper-recorder"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function ConvertTo-PlainText {
    param([Parameter(Mandatory = $true)][Security.SecureString]$SecureValue)

    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Read-ConfirmedPassword {
    param([Parameter(Mandatory = $true)][string]$Label)

    while ($true) {
        $firstSecure = Read-Host "$Label" -AsSecureString
        $secondSecure = Read-Host "Confirm $Label" -AsSecureString
        $first = ConvertTo-PlainText $firstSecure
        $second = ConvertTo-PlainText $secondSecure

        if ([string]::IsNullOrWhiteSpace($first)) {
            Write-Warning "Password cannot be empty."
            continue
        }
        if ($first -ne $second) {
            Write-Warning "Passwords did not match. Try again."
            continue
        }
        return $first
    }
}

function Read-Password {
    param([Parameter(Mandatory = $true)][string]$Label)

    $secure = Read-Host "$Label" -AsSecureString
    $plain = ConvertTo-PlainText $secure
    if ([string]::IsNullOrWhiteSpace($plain)) {
        throw "$Label cannot be empty."
    }
    return $plain
}

function Get-SystemKeytool {
    $command = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if (-not $command) {
        $command = Get-Command keytool -ErrorAction SilentlyContinue
    }
    if ($command) {
        return $command.Source
    }
    return $null
}

function Get-AdoptiumArchitecture {
    $architecture = $env:PROCESSOR_ARCHITEW6432
    if ([string]::IsNullOrWhiteSpace($architecture)) {
        $architecture = $env:PROCESSOR_ARCHITECTURE
    }

    switch -Regex ($architecture) {
        "ARM64" { return "aarch64" }
        "AMD64|x86_64" { return "x64" }
        default { throw "Unsupported Windows architecture '$architecture'. Install a JDK manually and rerun this script." }
    }
}

function Invoke-KeytoolCaptured {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    # Windows PowerShell 5.1 can promote harmless native stderr (including
    # keytool's JKS advisory) to NativeCommandError when the script-wide
    # ErrorActionPreference is Stop. Capture it under Continue, then trust the
    # native process exit code instead.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $nativeOutput = @(& $Executable @Arguments 2>&1)
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousPreference
    }

    $text = ($nativeOutput | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
    if ($exitCode -ne 0) {
        if (-not [string]::IsNullOrWhiteSpace($text)) {
            Write-Host $text
        }
        throw "$FailureMessage (exit code $exitCode)."
    }
    return $text
}

$temporaryJdkRoot = $null
$downloadedTemporaryJdk = $false
$storePassword = $null
$keyPassword = $null
$createdNewKeystore = $false

try {
    if (-not [IO.Path]::IsPathRooted($OutputPath)) {
        $OutputPath = Join-Path (Get-Location) $OutputPath
    }
    $OutputPath = [IO.Path]::GetFullPath($OutputPath)

    $parent = Split-Path -Parent $OutputPath
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    $keytool = Get-SystemKeytool
    if ($keytool) {
        Write-Host "Using existing keytool: $keytool"
    }
    else {
        Write-Host "keytool was not found. Downloading a temporary portable Eclipse Temurin JDK 21..."
        Write-Host "Nothing will be installed system-wide and administrator access is not required."

        # Windows PowerShell 5.1 may otherwise negotiate an obsolete TLS version.
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        }
        catch {
            # PowerShell 7 uses the operating system TLS stack; no override is needed.
        }

        $arch = Get-AdoptiumArchitecture
        $apiUrl = "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=$arch&image_type=jdk&os=windows&vendor=eclipse"
        $headers = @{ "User-Agent" = "WhisperRecorder-v5.43-signing-setup" }
        $assets = @(Invoke-RestMethod -Uri $apiUrl -Headers $headers)
        if ($assets.Count -lt 1) {
            throw "Adoptium did not return a Windows JDK 21 package for architecture '$arch'."
        }

        $package = $assets[0].binary.package
        $downloadUrl = $package.link
        $expectedSha256 = $package.checksum
        if ([string]::IsNullOrWhiteSpace($downloadUrl) -or [string]::IsNullOrWhiteSpace($expectedSha256)) {
            throw "Adoptium returned an invalid JDK download response or omitted its SHA-256 checksum."
        }

        $temporaryJdkRoot = Join-Path ([IO.Path]::GetTempPath()) ("whisper-recorder-jdk-" + [Guid]::NewGuid().ToString("N"))
        $zipPath = Join-Path $temporaryJdkRoot "temurin-jdk.zip"
        $extractPath = Join-Path $temporaryJdkRoot "jdk"
        New-Item -ItemType Directory -Path $temporaryJdkRoot -Force | Out-Null
        New-Item -ItemType Directory -Path $extractPath -Force | Out-Null
        $downloadedTemporaryJdk = $true

        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -Headers $headers -UseBasicParsing

        $actualSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $zipPath).Hash
        if ($actualSha256 -ne $expectedSha256.ToUpperInvariant()) {
            throw "Downloaded JDK checksum did not match Adoptium metadata."
        }
        Write-Host "Temporary JDK download verified."

        Expand-Archive -LiteralPath $zipPath -DestinationPath $extractPath -Force
        $keytoolFile = Get-ChildItem -Path $extractPath -Filter "keytool.exe" -File -Recurse | Select-Object -First 1
        if (-not $keytoolFile) {
            throw "The downloaded JDK did not contain keytool.exe."
        }
        $keytool = $keytoolFile.FullName
        Write-Host "Using temporary keytool: $keytool"
    }

    Write-Host ""
    if (Test-Path -LiteralPath $OutputPath) {
        Write-Host "Existing release keystore detected. It will not be overwritten." -ForegroundColor Yellow
        Write-Host "  $OutputPath"
        Write-Host "Resuming setup from the existing key so its signing identity is preserved."
        Write-Host ""
        $storePassword = Read-Password "Existing keystore password"
    }
    else {
        Write-Host "Create the permanent Whisper Recorder signing key."
        Write-Host "Keep the .jks file and both passwords backed up permanently."
        Write-Host ""

        $storePassword = Read-ConfirmedPassword "Keystore password"
        $keyPassword = Read-ConfirmedPassword "Key password"

        $generationOutput = Invoke-KeytoolCaptured `
            -Executable $keytool `
            -Arguments @(
                "-genkeypair",
                "-keystore", $OutputPath,
                "-storetype", "JKS",
                "-storepass", $storePassword,
                "-keypass", $keyPassword,
                "-alias", $Alias,
                "-keyalg", "RSA",
                "-keysize", "4096",
                "-validity", "10000",
                "-dname", "CN=Whisper Recorder, O=Whisper Recorder, C=US"
            ) `
            -FailureMessage "keytool failed while creating the release keystore"
        $createdNewKeystore = $true
    }

    $keytoolOutput = Invoke-KeytoolCaptured `
        -Executable $keytool `
        -Arguments @("-list", "-v", "-keystore", $OutputPath, "-storepass", $storePassword, "-alias", $Alias) `
        -FailureMessage "keytool could not read the release keystore"

    $fingerprintMatch = [regex]::Match($keytoolOutput, "SHA256:\s*([0-9A-Fa-f:]+)")
    if (-not $fingerprintMatch.Success) {
        throw "Unable to read the certificate SHA-256 fingerprint from keytool."
    }
    $fingerprint = $fingerprintMatch.Groups[1].Value.ToUpperInvariant()
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($OutputPath))

    $copiedBase64 = $false
    try {
        Set-Clipboard -Value $base64
        $copiedBase64 = $true
    }
    catch {
        # Clipboard access can be unavailable in remote/noninteractive PowerShell.
    }

    Write-Host ""
    if ($createdNewKeystore) {
        Write-Host "Permanent release keystore created successfully:" -ForegroundColor Green
    }
    else {
        Write-Host "Existing permanent release keystore verified successfully:" -ForegroundColor Green
    }
    Write-Host "  $OutputPath"
    Write-Host "Alias:"
    Write-Host "  $Alias"
    Write-Host "Certificate SHA-256:"
    Write-Host "  $fingerprint"
    Write-Host ""
    Write-Host "Create/update these GitHub repository secrets:"
    if ($copiedBase64) {
        Write-Host "  ANDROID_KEYSTORE_BASE64      = [base64 copied to your clipboard]"
    }
    else {
        Write-Host "  ANDROID_KEYSTORE_BASE64      = run the command shown below"
    }
    Write-Host "  ANDROID_KEYSTORE_PASSWORD    = [the keystore password you entered]"
    Write-Host "  ANDROID_KEY_ALIAS             = $Alias"
    if ($createdNewKeystore) {
        Write-Host "  ANDROID_KEY_PASSWORD          = [the key password you entered]"
    }
    else {
        Write-Host "  ANDROID_KEY_PASSWORD          = [the ORIGINAL key password used when this keystore was created]"
    }
    Write-Host "  ANDROID_SIGNING_CERT_SHA256   = $fingerprint"
    Write-Host ""
    if (-not $copiedBase64) {
        Write-Host "Copy the keystore base64 with:"
        Write-Host ('  [Convert]::ToBase64String([IO.File]::ReadAllBytes("{0}")) | Set-Clipboard' -f $OutputPath)
        Write-Host ""
    }
    Write-Host "IMPORTANT: Make at least two secure backups of the .jks file and passwords."
    Write-Host "Do not commit the keystore or paste its base64 into source code."
}
finally {
    # Plaintext passwords exist only for the lifetime of this PowerShell process.
    $storePassword = $null
    $keyPassword = $null

    if ($downloadedTemporaryJdk -and $temporaryJdkRoot -and (Test-Path -LiteralPath $temporaryJdkRoot)) {
        Write-Host "Cleaning up the temporary JDK dependency..."
        Remove-Item -LiteralPath $temporaryJdkRoot -Recurse -Force -ErrorAction SilentlyContinue
        if (Test-Path -LiteralPath $temporaryJdkRoot) {
            Write-Warning "Temporary JDK cleanup could not remove: $temporaryJdkRoot"
        }
        else {
            Write-Host "Temporary JDK removed. No Java dependency was installed system-wide."
        }
    }
}
