#requires -Version 5.1
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Import-Module (Join-Path $Root 'NpuBuilder.psm1') -Force

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
[System.Windows.Forms.Application]::SetCompatibleTextRenderingDefault($false)

$script:AppVersion = '0.4.96'
$script:WorkspaceRoot = Join-Path $Root 'workspace'
$script:CurrentWorkspace = New-NpuWorkspace -Root $script:WorkspaceRoot
$script:BridgePath = Join-Path $Root 'python\aihub_bridge.py'
$script:ModelToolsPath = Join-Path $Root 'python\model_tools.py'
$script:ParakeetExportPath = Join-Path $Root 'python\parakeet_export.py'
$script:ParakeetContractPath = Join-Path $Root 'python\parakeet_contract.py'
$script:ParakeetProjectorPath = Join-Path $Root 'python\parakeet_projector.py'
$script:ParakeetPredictorPath = Join-Path $Root 'python\parakeet_predictor.py'
$script:ParakeetJointPath = Join-Path $Root 'python\parakeet_joint.py'
$script:ParakeetControllerPath = Join-Path $Root 'python\parakeet_controller.py'
$script:RuntimeBenchmarkPath = Join-Path $Root 'python\runtime_benchmark.py'
$script:PackageBuilderPath = Join-Path $Root 'python\package_builder.py'
$script:HfDeployPath = Join-Path $Root 'python\hf_deploy.py'
$script:OmiExperimentalHfPath = Join-Path $Root 'python\omi_hf_publisher.py'
$script:OmiQnnContextInfoPath = Join-Path $Root 'python\omi_qnn_context_info.py'
$script:OmiToolsPath = Join-Path $Root 'python\omi_med_npu.py'
$script:QwenAsrResearchPath = Join-Path $Root 'python\qwen3_asr_research.py'
$script:QwenAsrExportPath = Join-Path $Root 'python\qwen3_asr_export.py'
$script:QwenAsrStageFPath = Join-Path $Root 'python\qwen3_asr_stage_f.py'
$script:QwenAsrStageGPath = Join-Path $Root 'python\qwen3_asr_stage_g.py'
$script:QwenAsrStageHPath = Join-Path $Root 'python\qwen3_asr_stage_h.py'
$script:QwenAsrStageIPath = Join-Path $Root 'python\qwen3_asr_stage_i.py'
$script:QwenAsrStageJPath = Join-Path $Root 'python\qwen3_asr_stage_j.py'
$script:QwenAsrStageJ2Path = Join-Path $Root 'python\qwen3_asr_stage_j2.py'
$script:QwenAsrStageJBatchPath = Join-Path $Root 'python\qwen3_asr_stage_j_batch.py'
$script:QwenAsrStageJFollowupPath = Join-Path $Root 'python\qwen3_asr_stage_j12_j13.py'
$script:QwenAsrStageJ14Path = Join-Path $Root 'python\qwen3_asr_stage_j14.py'
$script:QwenAsrStageJ15J16Path = Join-Path $Root 'python\qwen3_asr_stage_j15_j16.py'
$script:QwenAsrStageJ17J19Path = Join-Path $Root 'python\qwen3_asr_stage_j17_j19.py'
$script:QwenAsrStageJ20J22Path = Join-Path $Root 'python\qwen3_asr_stage_j20_j22.py'
$script:QwenAsrStageJ23J25Path = Join-Path $Root 'python\qwen3_asr_stage_j23_j25.py'
$script:QwenAsrStageJ26J28Path = Join-Path $Root 'python\qwen3_asr_stage_j26_j28.py'
$script:QwenAsrStageJ29J31Path = Join-Path $Root 'python\qwen3_asr_stage_j29_j31.py'
$script:QwenAsrStageJ32J34Path = Join-Path $Root 'python\qwen3_asr_stage_j32_j34.py'
$script:QwenAsrStageJ35J37Path = Join-Path $Root 'python\qwen3_asr_stage_j35_j37.py'
$script:QwenAsrStageJ38J40Path = Join-Path $Root 'python\qwen3_asr_stage_j38_j40.py'
$script:QwenAsrStageJ41J43Path = Join-Path $Root 'python\qwen3_asr_stage_j41_j43.py'
$script:QwenAsrStageJ44J46Path = Join-Path $Root 'python\qwen3_asr_stage_j44_j46.py'
$script:QwenAsrStageJ47J49Path = Join-Path $Root 'python\qwen3_asr_stage_j47_j49.py'
$script:QwenAsrStageJ50Path = Join-Path $Root 'python\qwen3_asr_stage_j50.py'
$script:QwenAsrStageJ51J53Path = Join-Path $Root 'python\qwen3_asr_stage_j51_j53.py'
$script:QwenAsrStageJ54J56Path = Join-Path $Root 'python\qwen3_asr_stage_j54_j56.py'
$script:QwenAsrStageJ57J59Path = Join-Path $Root 'python\qwen3_asr_stage_j57_j59.py'
$script:QwenAsrStageJ60J62Path = Join-Path $Root 'python\qwen3_asr_stage_j60_j62.py'
$script:QwenAsrStageJ63J65Path = Join-Path $Root 'python\qwen3_asr_stage_j63_j65.py'
$script:QwenAsrStageJ66J68Path = Join-Path $Root 'python\qwen3_asr_stage_j66_j68.py'
$script:QwenAsrStageJ69J71Path = Join-Path $Root 'python\qwen3_asr_stage_j69_j71.py'
$script:QwenAsrStageJ72J73Path = Join-Path $Root 'python\qwen3_asr_stage_j72_j73.py'
$script:QwenAsrStageJ74Path = Join-Path $Root 'python\qwen3_asr_stage_j74.py'
$script:QwenAsrStageJ75J76Path = Join-Path $Root 'python\qwen3_asr_stage_j75_j76.py'
$script:QwenAsrStageJ77J79Path = Join-Path $Root 'python\qwen3_asr_stage_j77_j79.py'
$script:QwenAsrStageJ81Path = Join-Path $Root 'python\qwen3_asr_stage_j81.py'
$script:QwenAsrStageJ82J83Path = Join-Path $Root 'python\qwen3_asr_stage_j82_j83.py'
$script:QwenAsrStageJ84J85Path = Join-Path $Root 'python\qwen3_asr_stage_j84_j85.py'
$script:QwenAsrStageJ86Path = Join-Path $Root 'python\qwen3_asr_stage_j86.py'
$script:QwenAsrStageK1Path = Join-Path $Root 'python\qwen3_asr_stage_k1.py'
$script:QwenAsrStageK2Path = Join-Path $Root 'python\qwen3_asr_stage_k2.py'
$script:QwenAsrStageK3Path = Join-Path $Root 'python\qwen3_asr_stage_k3.py'
$script:QwenAsrStageK4Path = Join-Path $Root 'python\qwen3_asr_stage_k4.py'
$script:QwenShareToChatPath = Join-Path $Root 'python\qwen_share_to_chat.py'
$script:LlamaPhoneBenchPath = Join-Path $Root 'python\llama_phone_benchmark.py'
$script:QwenHybridQualifierPath = Join-Path $Root 'python\qwen_hybrid_qualifier.py'
$script:LlamaPhoneBenchCache = Join-Path $Root 'workspace\llama-phone-benchmark\cache'
$script:LlamaPhoneBenchResults = Join-Path $Root 'workspace\llama-phone-benchmark\results'
$script:BuilderQualityGatePath = Join-Path $Root 'python\builder_quality_gate.py'
$script:QwenAsrStageEPath = Join-Path $Root 'python\qwen3_asr_stage_e.py'
$script:QwenAsrWorkspace = Join-Path $Root 'workspace\qwen3-asr'
$script:OmiNativePath = Join-Path $Root 'python\omi_native_export.py'
$script:OmiTdtExportPath = Join-Path $Root 'python\omi_tdt_export.py'
$script:OmiTdtHtpPath = Join-Path $Root 'python\omi_tdt_htp.py'
$script:OmiTdtTranscribePath = Join-Path $Root 'python\omi_tdt_transcribe.py'
$script:OmiTdtS26TranscribePath = Join-Path $Root 'python\omi_tdt_s26_transcribe.py'
$script:OmiFp32S26ParityPath = Join-Path $Root 'python\omi_fp32_s26_parity.py'
$script:OmiTdtReferenceReviewPath = Join-Path $Root 'python\omi_tdt_gpu_reference_review.py'
$script:OmiGatePath = Join-Path $Root 'python\omi_gui_gate.py'
$script:OmiModelDir = Join-Path $Root 'models\source\omi-med-stt-v1'
$script:OmiWorkspace = Join-Path $Root 'workspace\omi-med-stt-v1'
$script:OmiRunFolder = ''
$script:LastOmiParityOutputFolder = ''
$script:CloudProbeOk = $false
$script:SettingsDir = Join-Path $Root 'config'
$script:SettingsPath = Join-Path $script:SettingsDir 'user-settings.json'
$script:DeviceSerial = ''
$script:CalibrationManifest = ''
$script:LastCompiledArtifact = ''
$script:IsBusy = $false
$script:LastErrorText = ''
$script:SensitiveOutputValues = New-Object System.Collections.Generic.List[string]
$script:LogsRoot = Join-Path $Root 'logs'
try {
    New-Item -ItemType Directory -Path $script:LogsRoot -Force | Out-Null
    $script:SessionLogPath = Join-Path $script:LogsRoot ('session-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
} catch {
    $script:SessionLogPath = Join-Path ([IO.Path]::GetTempPath()) ('WhisperRecorder-NPU-Builder-session-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
}

function New-Label {
    param([string]$Text,[int]$X,[int]$Y,[int]$W=145,[int]$H=24,[switch]$Bold)
    $c = New-Object System.Windows.Forms.Label
    $c.Text = $Text
    $c.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @([int]$X,[int]$Y)
    $c.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @([int]$W,[int]$H)
    if ($Bold) { $c.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @($c.Font,[System.Drawing.FontStyle]::Bold) }
    return $c
}

function New-TextBox {
    param([int]$X,[int]$Y,[int]$W=520,[int]$H=25,[bool]$Password=$false,[bool]$ReadOnly=$false)
    $c = New-Object System.Windows.Forms.TextBox
    $c.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @([int]$X,[int]$Y)
    $c.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @([int]$W,[int]$H)
    $c.ReadOnly = $ReadOnly
    if ($Password) { $c.UseSystemPasswordChar = $true }
    return $c
}

function New-Button {
    param([string]$Text,[int]$X,[int]$Y,[int]$W=110,[int]$H=30)
    $c = New-Object System.Windows.Forms.Button
    $c.Text = $Text
    $c.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @([int]$X,[int]$Y)
    $c.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @([int]$W,[int]$H)
    return $c
}

function New-StatusLabel {
    param([int]$X,[int]$Y,[int]$W=220)
    $c = New-Label 'Not checked' $X $Y $W 24
    $c.ForeColor = [System.Drawing.Color]::DimGray
    return $c
}

function Set-Status {
    param($Label,[string]$Text,[ValidateSet('good','warn','bad','neutral')][string]$State='neutral')
    $Label.Text = $Text
    switch ($State) {
        'good' { $Label.ForeColor = [System.Drawing.Color]::ForestGreen }
        'warn' { $Label.ForeColor = [System.Drawing.Color]::DarkOrange }
        'bad'  { $Label.ForeColor = [System.Drawing.Color]::Firebrick }
        default { $Label.ForeColor = [System.Drawing.Color]::DimGray }
    }
}

function Choose-Exe {
    param($TextBox,[string]$Title)
    $d = New-Object System.Windows.Forms.OpenFileDialog
    $d.Title = $Title
    $d.Filter = 'Executable (*.exe)|*.exe|All files (*.*)|*.*'
    if ($TextBox.Text -and (Test-Path -LiteralPath $TextBox.Text)) {
        $d.InitialDirectory = Split-Path -Parent $TextBox.Text
    }
    if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { $TextBox.Text = $d.FileName }
}

function Choose-File {
    param($TextBox,[string]$Title,[string]$Filter='All files (*.*)|*.*')
    $d = New-Object System.Windows.Forms.OpenFileDialog
    $d.Title = $Title
    $d.Filter = $Filter
    if ($TextBox.Text -and (Test-Path -LiteralPath $TextBox.Text)) {
        $d.InitialDirectory = Split-Path -Parent $TextBox.Text
    }
    if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { $TextBox.Text = $d.FileName }
}

function Choose-Folder {
    param($TextBox,[string]$Description)
    $d = New-Object System.Windows.Forms.FolderBrowserDialog
    $d.Description = $Description
    if ($TextBox.Text -and (Test-Path -LiteralPath $TextBox.Text)) { $d.SelectedPath = $TextBox.Text }
    if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { $TextBox.Text = $d.SelectedPath }
}

function Open-Folder {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { throw "Folder not found: $Path" }
    Start-Process -FilePath 'explorer.exe' -ArgumentList ('"{0}"' -f $Path) | Out-Null
}

function Open-Url {
    param([Parameter(Mandatory=$true)][string]$Url)
    Start-Process -FilePath $Url | Out-Null
}

function Register-SensitiveOutputValue {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return }
    if ($Value.Length -lt 6) { return }
    if (-not $script:SensitiveOutputValues.Contains($Value)) { [void]$script:SensitiveOutputValues.Add($Value) }
}

function Protect-OutputText {
    param([string]$Text)
    $safe = [string]$Text
    foreach ($value in $script:SensitiveOutputValues) {
        if (-not [string]::IsNullOrEmpty($value)) { $safe = $safe.Replace($value,'***') }
    }
    return $safe
}

function Write-UiOutputLine {
    param([string]$Line,[switch]$SessionLog)
    if ([string]::IsNullOrEmpty($Line)) { return }
    $Line = Protect-OutputText $Line
    if ($SessionLog) {
        try { $Line | Add-Content -LiteralPath $script:SessionLogPath -Encoding UTF8 } catch {}
    }
    foreach ($name in @('txtConsole','txtLog')) {
        $v = Get-Variable -Name $name -Scope Script -ErrorAction SilentlyContinue
        if ($null -ne $v -and $null -ne $v.Value) {
            try {
                $box = $v.Value
                $box.AppendText($Line + [Environment]::NewLine)
                $box.SelectionStart = $box.TextLength
                $box.ScrollToCaret()
            } catch {}
        }
    }
    try { [System.Windows.Forms.Application]::DoEvents() } catch {}
}

function Append-Log {
    param([string]$Message)
    $line = '[{0}] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'),$Message
    Write-UiOutputLine -Line $line -SessionLog
}

function Append-ProcessOutput {
    param([string]$Message)
    # External command output is intentionally persisted too, so a support ZIP
    # contains the same evidence the user saw in the live console.
    $line = '[{0}] {1}' -f (Get-Date -Format 'HH:mm:ss.fff'),$Message
    Write-UiOutputLine -Line $line -SessionLog
}

function Show-Error {
    param($ErrorObject)
    $message = 'Unknown error.'
    $detail = ''
    if ($ErrorObject -is [System.Management.Automation.ErrorRecord]) {
        $message = $ErrorObject.Exception.Message
        $detail = ($ErrorObject | Out-String)
        if ($ErrorObject.ScriptStackTrace) { $detail += [Environment]::NewLine + 'SCRIPT STACK:' + [Environment]::NewLine + $ErrorObject.ScriptStackTrace }
    } elseif ($ErrorObject -is [Exception]) {
        $message = $ErrorObject.Message
        $detail = $ErrorObject.ToString()
    } else {
        $message = [string]$ErrorObject
        $detail = $message
    }
    $message = Protect-OutputText $message
    $detail = Protect-OutputText $detail
    $script:LastErrorText = $detail
    Append-Log ('ERROR: ' + $message)
    foreach ($line in ($detail -split "`r?`n")) { if ($line) { Append-Log ('  ' + $line) } }

    $supportPath = ''
    try {
        if ((Get-Variable -Name txtPython -Scope Script -ErrorAction SilentlyContinue) -and
            (Get-Variable -Name txtAdb -Scope Script -ErrorAction SilentlyContinue) -and
            (Get-Variable -Name txtFfmpeg -Scope Script -ErrorAction SilentlyContinue)) {
            $supportPath = Join-Path $script:LogsRoot ('WhisperRecorder-NPU-Builder-error-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
            [void](Export-BuilderSupportBundle -BuilderRoot $Root -Workspace $script:CurrentWorkspace -OutputZip $supportPath -PythonPath $txtPython.Text -AdbPath $txtAdb.Text -FfmpegPath $txtFfmpeg.Text -LastErrorText $script:LastErrorText)
            Append-Log ('Automatic error support bundle: ' + $supportPath)
        }
    } catch {
        Append-Log ('Could not create automatic support bundle: ' + $_.Exception.Message)
        $supportPath = ''
    }

    $extra = if ($supportPath) { "`r`n`r`nAn attachable support ZIP was created automatically:`r`n$supportPath" } else { "`r`n`r`nA detailed log was saved. Use Logs -> Create support ZIP and attach that ZIP here." }
    [System.Windows.Forms.MessageBox]::Show($message + $extra,'WhisperRecorder NPU Builder',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Error) | Out-Null
}

function Set-Busy {
    param([bool]$Busy,[string]$Text='')
    $script:IsBusy = $Busy
    $form.UseWaitCursor = $Busy
    if ($Busy) {
        $lblGlobalState.Text = if ($Text) { $Text } else { 'Working...' }
        $lblGlobalState.ForeColor = [System.Drawing.Color]::DarkOrange
    } else {
        $lblGlobalState.Text = 'Ready'
        $lblGlobalState.ForeColor = [System.Drawing.Color]::ForestGreen
    }
    [System.Windows.Forms.Application]::DoEvents()
}

function Set-InstallProgress {
    param([int]$Step,[int]$Total,[string]$Text,[ValidateSet('normal','good','bad')][string]$State='normal')
    $percent = if ($Total -gt 0) { [Math]::Min(100,[Math]::Max(0,[int](($Step * 100) / $Total))) } else { 0 }
    if ((Get-Variable -Name prgInstall -Scope Script -ErrorAction SilentlyContinue) -and $null -ne $prgInstall) { $prgInstall.Value = $percent }
    if ((Get-Variable -Name lblInstallDetail -Scope Script -ErrorAction SilentlyContinue) -and $null -ne $lblInstallDetail) {
        $lblInstallDetail.Text = $Text
        switch ($State) {
            'good' { $lblInstallDetail.ForeColor = [System.Drawing.Color]::ForestGreen }
            'bad' { $lblInstallDetail.ForeColor = [System.Drawing.Color]::Firebrick }
            default { $lblInstallDetail.ForeColor = [System.Drawing.Color]::DimGray }
        }
    }
    Append-Log $Text
}

# Package ONLY the new, already-sanitized Stage J ZIP(s) from this action.
# Never include raw device output, compiler logs, local JSON or model binaries.
function Publish-QwenChatBundle {
    param([string]$Stage,[string[]]$ExistingNames)
    if ($Stage -notin @('J72','J73','J74','J75','J77','J78','J80','J81','J82','J84','J85','J86')) { return }
    $stageDir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    if (-not (Test-Path -LiteralPath $stageDir -PathType Container)) { return }
    $num = $Stage.Substring(1)
    $qualityNum = if ($Stage -eq 'J78') { '79' } elseif ($Stage -eq 'J82') { '83' } else { $num }
    $fresh = @(Get-ChildItem -LiteralPath $stageDir -Filter '*.zip' -File -ErrorAction SilentlyContinue | Where-Object {
        ($ExistingNames -notcontains $_.Name) -and (
            $_.Name -match ('^stage-j' + $num + '-[a-z0-9-]+-[0-9]{8}-[0-9]{6}-[0-9]{3}\.zip$') -or
            $_.Name -match ('^builder-quality-postflight-j' + $qualityNum + '-[0-9]{8}-[0-9]{6}-[0-9]{3}\.zip$'))
    } | Sort-Object LastWriteTime -Descending)
    $stageZip = @($fresh | Where-Object { $_.Name -like ('stage-j' + $num + '-*.zip') } | Select-Object -First 1)
    $qualityZip = @($fresh | Where-Object { $_.Name -like ('builder-quality-postflight-j' + $qualityNum + '-*.zip') } | Select-Object -First 1)
    if ($stageZip.Count -eq 0 -and $qualityZip.Count -eq 0) { return }
    $shareDir = Join-Path $stageDir 'share-to-chat'
    New-Item -ItemType Directory -Path $shareDir -Force | Out-Null
    $output = Join-Path $shareDir ('Qwen-' + $Stage + '-share-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
    $arguments = @($script:QwenShareToChatPath,'--stage',$Stage,'--output',$output)
    if ($stageZip.Count -gt 0) { $arguments += @('--stage-zip',$stageZip[0].FullName) }
    if ($qualityZip.Count -gt 0) { $arguments += @('--postflight-zip',$qualityZip[0].FullName) }
    $python = Get-LocalBuilderPython
    $result = Invoke-ProcessCapture -FilePath $python -Arguments $arguments -TimeoutSeconds 45
    if ($result.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $output -PathType Leaf)) {
        throw ('Chat bundle verification failed for ' + $Stage + '. Original local receipts remain intact.')
    }
    Append-Log ('Ready to drag into chat: ' + $output)
    # Select the exact file, rather than opening the large audit directory.
    try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $output) | Out-Null }
    catch { Open-Folder $shareDir }
}

function Invoke-UiAction {
    param([string]$Name,[scriptblock]$Action,[string]$ShareQwenStage='')
    if ($script:IsBusy) { return }
    $existingStageZips = @()
    if ($ShareQwenStage -in @('J72','J73','J74','J75','J77','J78','J80','J81','J82','J84','J85','J86')) {
        $stageDir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $existingStageZips = @(Get-ChildItem -LiteralPath $stageDir -Filter '*.zip' -File -ErrorAction SilentlyContinue | ForEach-Object { $_.Name })
    }
    try {
        Set-Busy $true $Name
        Append-Log $Name
        & $Action
        Append-Log ("Completed: " + $Name)
    } catch {
        Show-Error $_
    } finally {
        if ($ShareQwenStage -in @('J72','J73','J74','J75','J77','J78','J80','J81','J82','J84','J85','J86')) {
            try { Publish-QwenChatBundle -Stage $ShareQwenStage -ExistingNames $existingStageZips }
            catch { Append-Log ('Automatic share ZIP unavailable: ' + $_.Exception.Message) }
        }
        Set-Busy $false
    }
}

function Load-Settings {
    if (-not (Test-Path -LiteralPath $script:SettingsPath)) { return $null }
    try { return (Get-Content -LiteralPath $script:SettingsPath -Raw | ConvertFrom-Json) } catch { return $null }
}

function Save-Settings {
    try {
        New-Item -ItemType Directory -Path $script:SettingsDir -Force | Out-Null
        $data = [ordered]@{
            omiRunFolder = $script:OmiRunFolder
            omiLastParityOutputFolder = $script:LastOmiParityOutputFolder
            omiStep5JSourceJson = $txtOmiCompareS26Json.Text
            omiReferenceAudio = $txtOmiAudio.Text
            omiRepoId = $txtOmiRepo.Text
            python = $txtPython.Text
            adb = $txtAdb.Text
            ffmpeg = $txtFfmpeg.Text
            audioFolder = $txtAudioFolder.Text
            llamaPhoneReferenceAudio = $txtPhoneAudio.Text
            onnx = $txtOnnx.Text
            modelRecipe = $cmbRecipe.SelectedItem
            qairtVersion = $txtQairt.Text
            modelId = $txtModelId.Text
            modelName = $txtModelName.Text
            lastContext = $txtCompiled.Text
            lastProjectorContext = $txtProjectorCompiled.Text
            lastPredictorContext = $txtPredictorCompiled.Text
            lastJointContext = $txtJointCompiled.Text
            hfPackageFolder = $txtHfPackageFolder.Text
            hfDeploymentFolder = $txtHfDeploymentFolder.Text
            hfRepoId = $txtHfRepoId.Text
            hfRevision = $txtHfRevision.Text
            hfVisibility = $cmbHfVisibility.SelectedItem
        }
        $data | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $script:SettingsPath -Encoding UTF8
    } catch {
        Append-Log ('Could not save settings: ' + $_.Exception.Message)
    }
}

function Get-SavedSetting {
    param($Object,[string]$Name)
    if ($null -eq $Object) { return $null }
    $prop = $Object.PSObject.Properties[$Name]
    if ($null -eq $prop) { return $null }
    return $prop.Value
}

function Apply-SavedSettings {
    # Be tolerant of settings files created by older NPU Builder versions.
    # Under StrictMode, directly reading a missing PSCustomObject property throws.
    $s = Load-Settings
    if ($null -eq $s) { return }
    $v = Get-SavedSetting $s 'python'; if ($v) { $txtPython.Text = [string]$v }
    $v = Get-SavedSetting $s 'adb'; if ($v) { $txtAdb.Text = [string]$v }
    $v = Get-SavedSetting $s 'ffmpeg'; if ($v) { $txtFfmpeg.Text = [string]$v }
    $v = Get-SavedSetting $s 'audioFolder'; if ($v) { $txtAudioFolder.Text = [string]$v }
    $v = Get-SavedSetting $s 'llamaPhoneReferenceAudio'; if ($v) { $txtPhoneAudio.Text = [string]$v }
    $v = Get-SavedSetting $s 'onnx'; if ($v) { $txtOnnx.Text = [string]$v }
    $v = Get-SavedSetting $s 'omiRunFolder'; if ($v -and (Test-Path -LiteralPath ([string]$v) -PathType Container)) { $script:OmiRunFolder = [string]$v }
    $v = Get-SavedSetting $s 'omiLastParityOutputFolder'; if ($v -and (Test-Path -LiteralPath (Join-Path ([string]$v) 'omi-fp32-onnx-vs-s26-npu-parity.json') -PathType Leaf)) { $script:LastOmiParityOutputFolder = [string]$v }
    $v = Get-SavedSetting $s 'omiReferenceAudio'; if ($v) { $txtOmiAudio.Text = [string]$v }
    $v = Get-SavedSetting $s 'omiRepoId'; if ($v) { $txtOmiRepo.Text = [string]$v }
    $v = Get-SavedSetting $s 'modelRecipe'; if ($v -and $cmbRecipe.Items.Contains([string]$v)) { $cmbRecipe.SelectedItem = [string]$v }
    $v = Get-SavedSetting $s 'qairtVersion'; if ($v) { $txtQairt.Text = [string]$v }
    $v = Get-SavedSetting $s 'modelId'; if ($v) { $txtModelId.Text = [string]$v }
    $v = Get-SavedSetting $s 'modelName'; if ($v) { $txtModelName.Text = [string]$v }
    $v = Get-SavedSetting $s 'lastContext'; if ($v -and (Test-Path -LiteralPath ([string]$v) -PathType Leaf)) { $txtCompiled.Text = [string]$v; $script:LastCompiledArtifact = [string]$v }
    $v = Get-SavedSetting $s 'lastProjectorContext'; if ($v -and (Test-Path -LiteralPath ([string]$v) -PathType Leaf)) { $txtProjectorCompiled.Text = [string]$v }
    $v = Get-SavedSetting $s 'lastPredictorContext'; if ($v -and (Test-Path -LiteralPath ([string]$v) -PathType Leaf)) { $txtPredictorCompiled.Text = [string]$v }
    $v = Get-SavedSetting $s 'lastJointContext'; if ($v -and (Test-Path -LiteralPath ([string]$v) -PathType Leaf)) { $txtJointCompiled.Text = [string]$v }
    $v = Get-SavedSetting $s 'hfPackageFolder'; if ($v) { $txtHfPackageFolder.Text = [string]$v }
    $v = Get-SavedSetting $s 'hfDeploymentFolder'; if ($v) { $txtHfDeploymentFolder.Text = [string]$v }
    $v = Get-SavedSetting $s 'hfRepoId'; if ($v) { $txtHfRepoId.Text = [string]$v }
    $v = Get-SavedSetting $s 'hfRevision'; if ($v) { $txtHfRevision.Text = [string]$v }
    $v = Get-SavedSetting $s 'hfVisibility'; if ($v -and $cmbHfVisibility.Items.Contains([string]$v)) { $cmbHfVisibility.SelectedItem = [string]$v }
    # Restored paths are rechecked before use; partial/failed runs never autofill Step 5J.
    $v = Get-SavedSetting $s 'omiStep5JSourceJson'; if ($v -and (Test-OmiStep5JSourceReport -Path ([string]$v))) { $txtOmiCompareS26Json.Text = [string]$v }
}

function Prefer-ProjectLocalTools {
    # Settings intentionally survive an in-place upgrade, but an older saved path
    # may still point at a system or previous install. Whenever the Builder has a
    # working project-local copy, prefer it automatically so overwrite upgrades
    # converge on the self-contained tools folder without requiring manual Browse.
    $hubPython = Join-Path $Root 'tools\qai-hub-env\Scripts\python.exe'
    $basePython = Join-Path $Root 'tools\python311\python.exe'
    $localAdb = Join-Path $Root 'tools\android-platform-tools\platform-tools\adb.exe'
    $localFfmpegRoot = Join-Path $Root 'tools\ffmpeg\current'

    if (Test-Path -LiteralPath $hubPython) {
        $txtPython.Text = $hubPython
    } elseif (Test-Path -LiteralPath $basePython) {
        $txtPython.Text = $basePython
    }
    if (Test-Path -LiteralPath $localAdb) { $txtAdb.Text = $localAdb }
    if (Test-Path -LiteralPath $localFfmpegRoot) {
        $localFfmpeg = Get-ChildItem -LiteralPath $localFfmpegRoot -Filter 'ffmpeg.exe' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($localFfmpeg) { $txtFfmpeg.Text = $localFfmpeg.FullName }
    }
}

function Update-ToolStatuses {
    if ($txtPython.Text -and (Test-Path -LiteralPath $txtPython.Text)) {
        if (Test-CompatiblePython -PythonPath $txtPython.Text) { Set-Status $stPython 'Python 3.11 ready' 'good' } else { Set-Status $stPython 'AI Hub Python 3.11 needed' 'warn' }
    } else { Set-Status $stPython 'Missing' 'bad' }
    if ($txtAdb.Text -and (Test-Path -LiteralPath $txtAdb.Text)) { Set-Status $stAdb 'ADB ready' 'good' } else { Set-Status $stAdb 'Missing' 'bad' }
    if ($txtFfmpeg.Text -and (Test-Path -LiteralPath $txtFfmpeg.Text)) { Set-Status $stFfmpeg 'FFmpeg ready' 'good' } else { Set-Status $stFfmpeg 'Missing' 'warn' }
    if ($txtPython.Text -and (Test-Path -LiteralPath $txtPython.Text)) {
        try {
            $hubTest = Test-PythonAiHubClient -PythonPath $txtPython.Text
            if ($hubTest.ExitCode -eq 0) { Set-Status $stHub 'qai_hub installed' 'good' } else { Set-Status $stHub 'qai_hub not installed' 'warn' }
        } catch { Set-Status $stHub 'qai_hub not installed' 'warn' }
    } else { Set-Status $stHub 'Needs Python' 'warn' }
}

function Auto-DetectTools {
    $t = Get-DefaultToolPaths
    if ($t.Python) { $txtPython.Text = $t.Python }
    if ($t.Adb) { $txtAdb.Text = $t.Adb }
    if ($t.Ffmpeg) { $txtFfmpeg.Text = $t.Ffmpeg }
    Update-ToolStatuses
    Append-Log ("Auto-detect: Python={0}; ADB={1}; FFmpeg={2}" -f $txtPython.Text,$txtAdb.Text,$txtFfmpeg.Text)
}

function Refresh-Device {
    if (-not $txtAdb.Text) { throw 'Select adb.exe first.' }
    $devices = Get-AdbDevices -AdbPath $txtAdb.Text
    $usable = @($devices | Where-Object { $_.State -eq 'device' })
    if ($usable.Count -eq 0) { throw 'No authorized ADB device found. Enable USB debugging, connect the phone, and accept the RSA prompt.' }
    $script:DeviceSerial = $usable[0].Serial
    $i = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
    $lines = @(
        "Serial:          $($i.Serial)",
        "Model:           $($i.Model)",
        "Android:         $($i.Android) (SDK $($i.AndroidSdk))",
        "ABI:             $($i.Abi)",
        "ro.soc.model:    $($i.SocModel)",
        "Board platform:  $($i.BoardPlatform)",
        "Hardware:        $($i.Hardware)",
        '',
        "Expected target: SM-S942U / SM8850 / SoC 87 / Hexagon V81",
        "AI Hub target:   $($i.AiHubDevice)",
        "Target match:    $($i.IsTargetDevice)"
    )
    $txtDevice.Text = ($lines -join [Environment]::NewLine)
    $lblDetectedModel.Text = if ($i.Model) { $i.Model } else { '(unknown)' }
    $lblDetectedSerial.Text = $i.Serial
    if ($i.IsTargetDevice) {
        Set-Status $stDevice 'SM-S942U connected' 'good'
    } else {
        Set-Status $stDevice ("Connected: " + $i.Model) 'warn'
    }
}

function Ensure-AudioFolderSelection {
    $folder = [string]$txtAudioFolder.Text
    if ([string]::IsNullOrWhiteSpace($folder) -or -not (Test-Path -LiteralPath $folder -PathType Container)) {
        Append-Log 'No valid .ogg calibration folder is selected. Opening the folder picker...'
        Choose-Folder $txtAudioFolder 'Select folder containing .ogg recordings'
        $folder = [string]$txtAudioFolder.Text
    }

    if ([string]::IsNullOrWhiteSpace($folder) -or -not (Test-Path -LiteralPath $folder -PathType Container)) {
        Set-Status $stAudio 'Choose an .ogg folder' 'warn'
        Append-Log 'Audio operation cancelled: no .ogg folder was selected.'
        return $false
    }

    return $true
}

function Scan-Audio {
    if (-not (Ensure-AudioFolderSelection)) { return }
    $folder = [string]$txtAudioFolder.Text
    Append-Log ('Scanning .ogg folder: ' + $folder)
    $items = @(Get-OggInventory -Folder $folder -FfmpegPath $txtFfmpeg.Text)
    $listAudio.Items.Clear()
    $totalBytes = 0L
    $totalDuration = 0.0
    $knownDuration = $true
    foreach ($item in $items) {
        $totalBytes += [int64]$item.Bytes
        if ($null -eq $item.DurationSeconds) { $knownDuration = $false } else { $totalDuration += [double]$item.DurationSeconds }
        $durationText = ''
        if ($null -ne $item.DurationSeconds) { $durationText = [TimeSpan]::FromSeconds([double]$item.DurationSeconds).ToString('hh\:mm\:ss') }
        $row = New-Object -TypeName System.Windows.Forms.ListViewItem -ArgumentList @([string]$item.Name)
        [void]$row.SubItems.Add(('{0:N2}' -f $item.MiB))
        [void]$row.SubItems.Add($durationText)
        $row.Tag = $item.Path
        [void]$listAudio.Items.Add($row)
    }
    $summary = "{0} .ogg file(s), {1:N1} MiB" -f $items.Count,($totalBytes / 1MB)
    if ($knownDuration -and $items.Count -gt 0) { $summary += (", " + [TimeSpan]::FromSeconds($totalDuration).ToString('hh\:mm\:ss') + ' total') }
    $lblAudioSummary.Text = $summary
    if ($items.Count -gt 0) { Set-Status $stAudio 'Calibration audio ready' 'good' } else { Set-Status $stAudio 'No .ogg files found' 'warn' }
}


function Get-LocalBuilderPython {
    $python = Join-Path $Root 'tools\python311\python.exe'
    if ((Test-Path -LiteralPath $python -PathType Leaf) -and (Test-CompatiblePython -PythonPath $python)) { return $python }
    if ((Get-Variable -Name txtPython -Scope Script -ErrorAction SilentlyContinue) -and $txtPython.Text -and (Test-Path -LiteralPath $txtPython.Text -PathType Leaf) -and (Test-CompatiblePython -PythonPath $txtPython.Text)) { return $txtPython.Text }
    return (Install-PythonFromUi)
}

function Ensure-HuggingFaceUploaderPython {
    $envRoot = Join-Path $Root 'tools\hf-env'
    $envPython = Join-Path $envRoot 'Scripts\python.exe'
    $base = Get-LocalBuilderPython
    if (-not (Test-Path -LiteralPath $envPython -PathType Leaf)) {
        Append-Log ('[Hugging Face] Creating isolated uploader environment: ' + $envRoot)
        New-Item -ItemType Directory -Path (Split-Path -Parent $envRoot) -Force | Out-Null
        $venv = Invoke-ProcessCapture -FilePath $base -Arguments @('-m','venv',$envRoot) -TimeoutSeconds 300
        if ($venv.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $envPython -PathType Leaf)) {
            throw "Could not create the Builder-private Hugging Face uploader environment.`r`n$($venv.StdErr)`r`n$($venv.StdOut)"
        }
    }
    $check = Invoke-ProcessCapture -FilePath $envPython -Arguments @('-c','import huggingface_hub; from huggingface_hub import HfApi; assert hasattr(HfApi,"update_repo_settings"),"Need newer huggingface_hub"; print(getattr(huggingface_hub,"__version__","installed"))') -TimeoutSeconds 60 -DisplayCommand ('"' + $envPython + '" -c "import huggingface_hub"')
    if ($check.ExitCode -ne 0) {
        Append-Log '[Hugging Face] Installing official huggingface_hub package into the isolated uploader environment...'
        $pip = Invoke-ProcessCapture -FilePath $envPython -Arguments @('-m','pip','install','--upgrade','huggingface_hub') -TimeoutSeconds 1800
        if ($pip.ExitCode -ne 0) { throw "huggingface_hub installation failed.`r`n$($pip.StdErr)`r`n$($pip.StdOut)" }
        $check = Invoke-ProcessCapture -FilePath $envPython -Arguments @('-c','import huggingface_hub; from huggingface_hub import HfApi; assert hasattr(HfApi,"update_repo_settings"),"Need newer huggingface_hub"; print(getattr(huggingface_hub,"__version__","installed"))') -TimeoutSeconds 60 -DisplayCommand ('"' + $envPython + '" -c "import huggingface_hub"')
        if ($check.ExitCode -ne 0) { throw 'huggingface_hub is still unavailable after installation.' }
    }
    Append-Log ('[Hugging Face] Uploader environment ready: ' + $envPython)
    return $envPython
}

function Get-HuggingFaceRepoUrl {
    param([string]$RepoId)
    $value = [string]$RepoId
    if ([string]::IsNullOrWhiteSpace($value) -or $value -notmatch '^[^/\s]+/[^/\s]+$') { throw 'Enter a Hugging Face repository as namespace/name.' }
    return ('https://huggingface.co/' + $value)
}

function Confirm-InstallSources {
    $text = @"
The Builder will install/update prerequisites you explicitly requested:

- ADB: Google's current Android SDK Platform-Tools ZIP
- FFmpeg: Gyan.dev Windows release build linked by ffmpeg.org, with SHA-256 verification
- Python 3.11: private runtime installed under tools\\python311 from CPython's official NuGet build package
- Qualcomm AI Hub: an isolated tools\\qai-hub-env virtual environment via pip install qai-hub

No Qualcomm QAIRT/QNN SDK is installed. AI Hub credentials are not written to Builder logs or support ZIPs.

Continue?
"@
    return ([System.Windows.Forms.MessageBox]::Show($text,'Install prerequisites',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information) -eq [System.Windows.Forms.DialogResult]::Yes)
}

function Install-PythonFromUi {
    # The Builder owns its Python runtime. Do not borrow or modify a system Python.
    $localPython = Join-Path $Root 'tools\python311\python.exe'
    if ((Test-Path -LiteralPath $localPython) -and (Test-CompatiblePython -PythonPath $localPython)) {
        Append-Log ('Builder-local Python already ready: ' + $localPython)
        return $localPython
    }
    Append-Log 'Installing private Builder Python 3.11 under tools\python311...'
    $r = Install-AiHubPython311 -BuilderRoot $Root
    Append-Log ('Python ready: ' + $r.Path + ' (source: ' + $r.Source + ')')
    return $r.Path
}

function Install-AdbFromUi {
    Append-Log 'Downloading current Android SDK Platform-Tools from Google...'
    $r = Install-PortableAdb -BuilderRoot $Root
    $txtAdb.Text = $r.Path
    Append-Log ('ADB ready: ' + $r.Path)
    Append-Log ('ADB download SHA-256: ' + $r.Sha256)
    return $r.Path
}

function Install-FfmpegFromUi {
    Append-Log 'Downloading FFmpeg release essentials build...'
    $r = Install-PortableFfmpeg -BuilderRoot $Root
    $txtFfmpeg.Text = $r.Path
    Append-Log ('FFmpeg ready: ' + $r.Path)
    Append-Log ('FFmpeg verified SHA-256: ' + $r.Sha256)
    return $r.Path
}

function Install-AiHubFromUi {
    # Always build the AI Hub venv from the private project-local Python runtime.
    # This keeps the automatic toolchain relocatable with the project folder.
    $base = Join-Path $Root 'tools\python311\python.exe'
    if (-not (Test-Path -LiteralPath $base) -or -not (Test-CompatiblePython -PythonPath $base)) { $base = Install-PythonFromUi }
    Append-Log ('Creating/updating isolated Qualcomm AI Hub Python environment from: ' + $base)
    $r = Install-AiHubEnvironment -BuilderRoot $Root -BasePythonPath $base
    $txtPython.Text = $r.PythonPath
    Append-Log ('AI Hub environment ready: ' + $r.PythonPath)
    Append-Log ($r.Version)
    return $r.PythonPath
}

function Ensure-AiHubEnvironmentFromUi {
    # A working project-local qai-hub environment should never be blocked merely
    # because it was created by an older Builder before the private CPython base
    # was added. Use it immediately for token/probe/compile actions. The explicit
    # Install / repair workflow will migrate it to tools\python311 when requested.
    $localBase = Join-Path $Root 'tools\python311\python.exe'
    $portableEnv = Join-Path $Root 'tools\qai-hub-env\Scripts\python.exe'

    if (Test-Path -LiteralPath $portableEnv) {
        try {
            $test = Test-PythonAiHubClient -PythonPath $portableEnv
            if ((Test-CompatiblePython -PythonPath $portableEnv) -and $test.ExitCode -eq 0) {
                $txtPython.Text = $portableEnv
                if (-not (Test-Path -LiteralPath $localBase)) {
                    Append-Log '[AI Hub] Existing project-folder environment is working. Private CPython base has not been migrated yet; Install / repair everything can migrate it later.'
                }
                return $portableEnv
            }
        } catch {}
    }

    Append-Log 'Repairing/migrating the Builder-local Python and Qualcomm AI Hub environment automatically.'
    return (Install-AiHubFromUi)
}

function Ensure-ModelToolsFromUi {
    $python = Ensure-AiHubEnvironmentFromUi
    $test = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import onnx; print(onnx.__version__)') -TimeoutSeconds 60
    if ($test.ExitCode -ne 0) {
        Append-Log 'ONNX helper is not installed in the existing Builder environment; installing it automatically...'
        $install = Invoke-ProcessCapture -FilePath $python -Arguments @('-m','pip','install','--upgrade','onnx') -TimeoutSeconds 1800
        if ($install.ExitCode -ne 0) { throw ('Could not install ONNX model tooling.' + [Environment]::NewLine + $install.StdErr + [Environment]::NewLine + $install.StdOut) }
        $test = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import onnx; print(onnx.__version__)') -TimeoutSeconds 60
        if ($test.ExitCode -ne 0) { throw ('ONNX model tooling installed but import verification failed.' + [Environment]::NewLine + $test.StdErr + [Environment]::NewLine + $test.StdOut) }
    }
    return $python
}


function Ensure-ParakeetExportEnvironmentFromUi {
    $base = Join-Path $Root 'tools\python311\python.exe'
    if (-not (Test-Path -LiteralPath $base) -or -not (Test-CompatiblePython -PythonPath $base)) { $base = Install-PythonFromUi }
    $envPython = Join-Path $Root 'tools\parakeet-export-env\Scripts\python.exe'
    if (Test-Path -LiteralPath $envPython) {
        $testCode = "import torch, transformers, onnx, onnxruntime, numpy, scipy, librosa; from transformers import AutoModelForTDT, AutoProcessor; assert torch.__version__.split('+')[0] == '2.14.0', torch.__version__; assert transformers.__version__ == '5.9.0', transformers.__version__; print('torch=' + str(torch.__version__) + '; transformers=' + transformers.__version__)"
        $test = Invoke-ProcessCapture -FilePath $envPython -Arguments @('-c',$testCode) -TimeoutSeconds 90
        if ($test.ExitCode -eq 0) { return $envPython }
    }
    $answer = [System.Windows.Forms.MessageBox]::Show("Preparing Parakeet on native Windows needs a separate project-local CPU export environment (PyTorch + Transformers + ONNX Runtime).`r`n`r`nThis is a one-time download and may take several minutes, but it does not install NeMo or the Qualcomm SDK.`r`n`r`nInstall/repair it now?",'Parakeet export environment',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information)
    if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { throw 'Parakeet export environment setup was cancelled.' }
    $r = Install-ParakeetExportEnvironment -BuilderRoot $Root -BasePythonPath $base
    Append-Log ('Parakeet export environment ready: ' + $r.PythonPath)
    Append-Log $r.Version
    return $r.PythonPath
}

function Install-AllPrerequisites {
    if (-not (Confirm-InstallSources)) { Append-Log 'Prerequisite installation cancelled by user.'; return }
    $total = 4
    try {
        Set-InstallProgress 0 $total 'Starting prerequisite setup...'

        Set-InstallProgress 1 $total '[1/4] Checking/installing private Builder Python 3.11...'
        $localPython = Join-Path $Root 'tools\python311\python.exe'
        if (-not (Test-Path -LiteralPath $localPython) -or -not (Test-CompatiblePython -PythonPath $localPython)) {
            $base = Install-PythonFromUi
        } else {
            $base = $localPython
            Append-Log ('[1/4] Builder-local Python already ready: ' + $base)
        }
        $txtPython.Text = $base

        Set-InstallProgress 2 $total '[2/4] Checking/installing project-local Android Platform-Tools (ADB)...'
        $localAdb = Join-Path $Root 'tools\android-platform-tools\platform-tools\adb.exe'
        if (-not (Test-Path -LiteralPath $localAdb)) {
            [void](Install-AdbFromUi)
        } else {
            $txtAdb.Text = $localAdb
            Append-Log ('[2/4] Project-local ADB already ready: ' + $localAdb)
        }

        Set-InstallProgress 3 $total '[3/4] Checking/installing project-local FFmpeg...'
        $localFfmpegRoot = Join-Path $Root 'tools\ffmpeg\current'
        $localFfmpeg = $null
        if (Test-Path -LiteralPath $localFfmpegRoot) {
            $localFfmpeg = Get-ChildItem -LiteralPath $localFfmpegRoot -Filter 'ffmpeg.exe' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        }
        if ($null -eq $localFfmpeg) {
            [void](Install-FfmpegFromUi)
        } else {
            $txtFfmpeg.Text = $localFfmpeg.FullName
            Append-Log ('[3/4] Project-local FFmpeg already ready: ' + $localFfmpeg.FullName)
        }

        Set-InstallProgress 4 $total '[4/4] Creating/repairing Qualcomm AI Hub environment...'
        [void](Install-AiHubFromUi)
        Update-ToolStatuses
        Save-Settings
        Set-Status $lblSetupState 'Automatic setup complete' 'good'
        Set-InstallProgress 4 $total 'Prerequisite setup completed successfully.' 'good'
        $tabs.SelectedTab = $tabSetup
        [System.Windows.Forms.MessageBox]::Show("Automatic prerequisites are ready.`r`n`r`nNext:`r`n1. Sign in to Qualcomm AI Hub and copy your API token.`r`n2. Paste it in Step 2 and click Configure / verify token.`r`n3. Connect the S26 with USB debugging enabled and click Refresh phone.",'Prerequisites installed',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    } catch {
        Set-InstallProgress $prgInstall.Value 100 ('Setup failed: ' + $_.Exception.Message) 'bad'
        throw
    }
}

function Create-SupportBundle {
    $out = Join-Path $script:LogsRoot ('WhisperRecorder-NPU-Builder-support-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
    $p = Export-BuilderSupportBundle -BuilderRoot $Root -Workspace $script:CurrentWorkspace -OutputZip $out -PythonPath $txtPython.Text -AdbPath $txtAdb.Text -FfmpegPath $txtFfmpeg.Text -LastErrorText $script:LastErrorText
    Append-Log ('Support bundle created: ' + $p)
    [System.Windows.Forms.MessageBox]::Show("Support ZIP created:`r`n$p`r`n`r`nAttach this ZIP here when you want me to diagnose a failure. AI Hub tokens are excluded.",'Support bundle',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
}

# -------------------- Form shell --------------------
$form = New-Object System.Windows.Forms.Form
$form.Text = "WhisperRecorder NPU Builder v$script:AppVersion"
$form.StartPosition = 'Manual'
$form.AutoScaleMode = [System.Windows.Forms.AutoScaleMode]::Dpi
$form.AutoScaleDimensions = New-Object -TypeName System.Drawing.SizeF -ArgumentList @([single]96,[single]96)
$form.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Segoe UI',[single]9)
$form.MinimumSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(920,560)
$form.MaximizeBox = $true

# Intentionally leave visible desktop padding above and below the GUI.
$wa = [System.Windows.Forms.Screen]::PrimaryScreen.WorkingArea
$formWidth = [Math]::Min(1120,[Math]::Max(920,$wa.Width - 160))
$formHeight = [Math]::Min(780,[Math]::Max(560,$wa.Height - 140))
$form.ClientSize = New-Object -TypeName System.Drawing.Size -ArgumentList @([int]$formWidth,[int]$formHeight)
# PowerShell 5.1 can parse arithmetic inside New-Object constructor syntax as an
# Object[] expression. Calculate scalar coordinates first, then pass explicit args.
$formLeft = [int]($wa.Left + [Math]::Max(0,[int](($wa.Width - $form.Width) / 2)))
$formTop = [int]($wa.Top + [Math]::Max(0,[int](($wa.Height - $form.Height) / 2)))
$form.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @($formLeft,$formTop)

$header = New-Object System.Windows.Forms.Panel
$header.Dock = 'Top'
$header.Height = 50
$form.Controls.Add($header)

$lblTitle = New-Label 'WhisperRecorder NPU Builder' 16 10 420 30 -Bold
$lblTitle.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Segoe UI',[single]14,[System.Drawing.FontStyle]::Bold)
$header.Controls.Add($lblTitle)
$lblGlobalState = New-Label 'Ready' ($formWidth - 170) 16 130 24
$lblGlobalState.Anchor = 'Top,Right'
$lblGlobalState.TextAlign = 'MiddleRight'
$lblGlobalState.ForeColor = [System.Drawing.Color]::ForestGreen
$header.Controls.Add($lblGlobalState)

$mainSplit = New-Object System.Windows.Forms.SplitContainer
$mainSplit.Dock = 'Fill'
$mainSplit.Orientation = [System.Windows.Forms.Orientation]::Horizontal
$mainSplit.FixedPanel = [System.Windows.Forms.FixedPanel]::None
$mainSplit.SplitterWidth = 6
# Give the control a realistic size before applying min sizes/splitter distance;
# this avoids SplitterDistance range errors before the form's first layout pass.
$mainSplit.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @([int]$formWidth,[int]($formHeight - $header.Height))
$mainSplit.Panel1MinSize = 280
$mainSplit.Panel2MinSize = 100
$desiredSplit = [Math]::Max(300,[int]($mainSplit.Height - 195))
$maxSplit = [Math]::Max($mainSplit.Panel1MinSize,[int]($mainSplit.Height - $mainSplit.Panel2MinSize - $mainSplit.SplitterWidth))
$mainSplit.SplitterDistance = [Math]::Min($desiredSplit,$maxSplit)
$form.Controls.Add($mainSplit)

# Persistent workflow navigation. This bar never changes when the active page changes,
# so the user always has an obvious route home, backward/forward, or directly to any page.
$workflowNav = New-Object System.Windows.Forms.Panel
# v0.3.1: use explicit anchored layout rather than competing Top/Fill docking.
# At 125-175% Windows DPI, the old Dock order could let the TabControl cover
# the entire navigation strip even though the strip existed and was brought to
# front.  Explicit bounds keep navigation permanently visible.
$workflowNav.Dock = 'None'
$workflowNav.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(0,0)
$workflowNav.Height = 44
$workflowNav.Width = [Math]::Max(1,$mainSplit.Panel1.ClientSize.Width)
$workflowNav.Anchor = 'Top,Left,Right'
$workflowNav.Padding = New-Object System.Windows.Forms.Padding -ArgumentList @(6,5,6,4)
$workflowNav.BackColor = [System.Drawing.SystemColors]::ControlLight
$mainSplit.Panel1.Controls.Add($workflowNav)

$btnNavHome = New-Button 'Dashboard' 8 7 105 30
$workflowNav.Controls.Add($btnNavHome)
$btnNavPrev = New-Button '< Previous' 121 7 105 30
$workflowNav.Controls.Add($btnNavPrev)
$btnNavNext = New-Button 'Next >' 234 7 105 30
$workflowNav.Controls.Add($btnNavNext)
$lblNavGo = New-Label 'Go to:' 360 11 45 22 -Bold
$workflowNav.Controls.Add($lblNavGo)
$cmbNavPage = New-Object System.Windows.Forms.ComboBox
$cmbNavPage.DropDownStyle = 'DropDownList'
$cmbNavPage.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(410,8)
$cmbNavPage.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(210,25)
$workflowNav.Controls.Add($cmbNavPage)
$lblNavCurrent = New-Label 'Dashboard' 640 11 245 22 -Bold
$lblNavCurrent.Anchor = 'Top,Right'
$lblNavCurrent.TextAlign = 'MiddleRight'
$workflowNav.Controls.Add($lblNavCurrent)

$tabs = New-Object System.Windows.Forms.TabControl
$tabs.Dock = 'None'
$tabs.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(0,$workflowNav.Height)
$tabs.Width = [Math]::Max(1,$mainSplit.Panel1.ClientSize.Width)
$tabs.Height = [Math]::Max(1,$mainSplit.Panel1.ClientSize.Height - $workflowNav.Height)
$tabs.Anchor = 'Top,Bottom,Left,Right'
# Keep every workflow page discoverable at high DPI / narrower window widths.
# The persistent Go to control is the primary navigator; tab headers may wrap.
$tabs.Multiline = $true
$tabs.SizeMode = [System.Windows.Forms.TabSizeMode]::Normal
$mainSplit.Panel1.Controls.Add($tabs)
$workflowNav.BringToFront()

$mainSplit.Panel1.Add_Resize({
    try {
        $workflowNav.Width = [Math]::Max(1,$mainSplit.Panel1.ClientSize.Width)
        $tabs.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(0,$workflowNav.Height)
        $tabs.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @([Math]::Max(1,$mainSplit.Panel1.ClientSize.Width),[Math]::Max(1,$mainSplit.Panel1.ClientSize.Height - $workflowNav.Height))
    } catch {}
})

$consoleToolbar = New-Object System.Windows.Forms.Panel
$consoleToolbar.Dock = 'Top'
$consoleToolbar.Height = 32
$mainSplit.Panel2.Controls.Add($consoleToolbar)
$lblConsoleTitle = New-Label 'Live command output' 8 6 170 22 -Bold
$consoleToolbar.Controls.Add($lblConsoleTitle)
$btnConsoleClear = New-Button 'Clear' 620 2 70 27
$btnConsoleClear.Anchor = 'Top,Right'
$consoleToolbar.Controls.Add($btnConsoleClear)
$btnConsoleCopy = New-Button 'Copy all' 700 2 80 27
$btnConsoleCopy.Anchor = 'Top,Right'
$consoleToolbar.Controls.Add($btnConsoleCopy)
$btnConsoleLogs = New-Button 'Open logs' 790 2 90 27
$btnConsoleLogs.Anchor = 'Top,Right'
$consoleToolbar.Controls.Add($btnConsoleLogs)

$txtConsole = New-Object System.Windows.Forms.RichTextBox
$txtConsole.Dock = 'Fill'
$txtConsole.ReadOnly = $true
$txtConsole.WordWrap = $false
$txtConsole.BackColor = [System.Drawing.Color]::FromArgb(24,24,24)
$txtConsole.ForeColor = [System.Drawing.Color]::Gainsboro
$txtConsole.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$mainSplit.Panel2.Controls.Add($txtConsole)
$consoleToolbar.BringToFront()
$header.BringToFront()

# Route every Invoke-ProcessCapture stdout/stderr line into the persistent console.
Set-NpuOutputHandler { param($Line) Append-ProcessOutput $Line }

# -------------------- Dashboard --------------------
$tabDashboard = New-Object System.Windows.Forms.TabPage
$tabDashboard.Text = 'Dashboard'
$tabDashboard.AutoScroll = $true
$tabDashboard.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,720)
[void]$tabs.TabPages.Add($tabDashboard)

$tabDashboard.Controls.Add((New-Label 'Target device' 25 25 160 24 -Bold))
$stDevice = New-StatusLabel 210 25 280
$tabDashboard.Controls.Add($stDevice)
$tabDashboard.Controls.Add((New-Label 'Python' 25 65 160 24 -Bold))
$stPython = New-StatusLabel 210 65 280
$tabDashboard.Controls.Add($stPython)
$tabDashboard.Controls.Add((New-Label 'ADB' 25 105 160 24 -Bold))
$stAdb = New-StatusLabel 210 105 280
$tabDashboard.Controls.Add($stAdb)
$tabDashboard.Controls.Add((New-Label 'FFmpeg' 25 145 160 24 -Bold))
$stFfmpeg = New-StatusLabel 210 145 280
$tabDashboard.Controls.Add($stFfmpeg)
$tabDashboard.Controls.Add((New-Label 'Calibration audio' 25 185 160 24 -Bold))
$stAudio = New-StatusLabel 210 185 280
$tabDashboard.Controls.Add($stAudio)
$tabDashboard.Controls.Add((New-Label 'AI Hub client' 25 225 160 24 -Bold))
$stHub = New-StatusLabel 210 225 280
$tabDashboard.Controls.Add($stHub)

$btnDashDetect = New-Button 'Detect tools' 540 25 150 34
$tabDashboard.Controls.Add($btnDashDetect)
$btnDashDevice = New-Button 'Refresh phone' 705 25 150 34
$tabDashboard.Controls.Add($btnDashDevice)
$btnDashScan = New-Button 'Scan .ogg' 540 72 150 34
$tabDashboard.Controls.Add($btnDashScan)
$btnDashWorkspace = New-Button 'Open workspace' 705 72 150 34
$tabDashboard.Controls.Add($btnDashWorkspace)
$btnDashLogs = New-Button 'View logs' 540 119 150 34
$tabDashboard.Controls.Add($btnDashLogs)
$btnDashSettings = New-Button 'Setup' 705 119 150 34
$tabDashboard.Controls.Add($btnDashSettings)
$btnDashSupport = New-Button 'Create support ZIP' 540 166 315 34
$tabDashboard.Controls.Add($btnDashSupport)
$btnDashCloud = New-Button 'Open Model + AI Hub' 540 213 150 34
$tabDashboard.Controls.Add($btnDashCloud)
$btnDashProbe = New-Button 'Probe AI Hub' 705 213 150 34
$tabDashboard.Controls.Add($btnDashProbe)
$btnDashDeploy = New-Button 'Open Deploy & Test' 540 260 315 34
$tabDashboard.Controls.Add($btnDashDeploy)

$tabDashboard.Controls.Add((New-Label 'AI Hub authentication' 25 305 200 24 -Bold))
$tabDashboard.Controls.Add((New-Label 'API token' 25 340 90 24 -Bold))
$txtDashToken = New-TextBox 115 337 450 25 $true $false
$tabDashboard.Controls.Add($txtDashToken)
$btnDashConfigureToken = New-Button 'Configure / Verify' 580 334 155 32
$tabDashboard.Controls.Add($btnDashConfigureToken)
$btnDashGetToken = New-Button 'Get API token' 745 334 110 32
$tabDashboard.Controls.Add($btnDashGetToken)
$lblDashHubAuth = New-StatusLabel 115 373 740
$tabDashboard.Controls.Add($lblDashHubAuth)
$lblDashTokenHint = New-Label 'Paste a Qualcomm AI Hub token here on first use. The token is masked, never saved in Builder settings, and redacted from logs.' 115 397 740 38
$lblDashTokenHint.ForeColor = [System.Drawing.Color]::DimGray
$tabDashboard.Controls.Add($lblDashTokenHint)

$tabDashboard.Controls.Add((New-Label 'Current workspace' 25 450 160 24 -Bold))
$txtWorkspace = New-TextBox 25 480 830 25 $false $true
$txtWorkspace.Text = $script:CurrentWorkspace
$tabDashboard.Controls.Add($txtWorkspace)
$tabDashboard.Controls.Add((New-Label 'Workflow' 25 530 160 24 -Bold))
$lblWorkflow = New-Label '1. Setup / authenticate  ->  2. Connect S26  ->  3. Prepare model  ->  4. Reuse an existing QNN context or build one if needed  ->  5. Deploy & Test.' 25 560 850 55
$tabDashboard.Controls.Add($lblWorkflow)
$lblSdkFree = New-Label 'Existing compiled .bin contexts are reusable. Deploy & Test is always available and rescans all Builder workspaces automatically.' 25 625 850 45 -Bold
$tabDashboard.Controls.Add($lblSdkFree)

# -------------------- Setup --------------------
$tabSetup = New-Object System.Windows.Forms.TabPage
$tabSetup.Text = 'Setup'
$tabSetup.AutoScroll = $true
$tabSetup.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,815)
[void]$tabs.TabPages.Add($tabSetup)

$tabSetup.Controls.Add((New-Label 'Step 1 - Install / repair prerequisites' 25 18 330 26 -Bold))
$btnInstallAll = New-Button 'Install / repair everything' 25 52 220 36
$tabSetup.Controls.Add($btnInstallAll)
$btnDetect = New-Button 'Detect existing tools' 260 52 165 36
$tabSetup.Controls.Add($btnDetect)
$btnSetupSupport = New-Button 'Create support ZIP' 440 52 160 36
$tabSetup.Controls.Add($btnSetupSupport)
$lblSetupState = New-StatusLabel 620 58 260
$tabSetup.Controls.Add($lblSetupState)

$lblInstallDetail = New-Label 'Idle - click Install / repair everything to begin.' 25 92 580 20
$lblInstallDetail.ForeColor = [System.Drawing.Color]::DimGray
$tabSetup.Controls.Add($lblInstallDetail)
$prgInstall = New-Object System.Windows.Forms.ProgressBar
$prgInstall.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(620,94)
$prgInstall.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(260,14)
$prgInstall.Minimum = 0
$prgInstall.Maximum = 100
$prgInstall.Value = 0
$tabSetup.Controls.Add($prgInstall)

$tabSetup.Controls.Add((New-Label 'Python' 25 115 110 24 -Bold))
$txtPython = New-TextBox 140 112 555
$tabSetup.Controls.Add($txtPython)
$btnPython = New-Button 'Browse...' 705 109 85
$tabSetup.Controls.Add($btnPython)
$btnInstallPython = New-Button 'Install 3.11' 790 109 100
$tabSetup.Controls.Add($btnInstallPython)

$tabSetup.Controls.Add((New-Label 'ADB' 25 158 110 24 -Bold))
$txtAdb = New-TextBox 140 155 555
$tabSetup.Controls.Add($txtAdb)
$btnAdb = New-Button 'Browse...' 705 152 85
$tabSetup.Controls.Add($btnAdb)
$btnInstallAdb = New-Button 'Install' 800 152 90
$tabSetup.Controls.Add($btnInstallAdb)

$tabSetup.Controls.Add((New-Label 'FFmpeg' 25 201 110 24 -Bold))
$txtFfmpeg = New-TextBox 140 198 555
$tabSetup.Controls.Add($txtFfmpeg)
$btnFfmpeg = New-Button 'Browse...' 705 195 85
$tabSetup.Controls.Add($btnFfmpeg)
$btnInstallFfmpeg = New-Button 'Install' 800 195 90
$tabSetup.Controls.Add($btnInstallFfmpeg)

$tabSetup.Controls.Add((New-Label 'AI Hub client' 25 244 110 24 -Bold))
$lblSetupHub = New-StatusLabel 140 244 240
$tabSetup.Controls.Add($lblSetupHub)
$btnInstallHub = New-Button 'Install / update qai-hub' 390 238 180 34
$tabSetup.Controls.Add($btnInstallHub)
$btnTestHub = New-Button 'Test / repair' 580 238 120 34
$tabSetup.Controls.Add($btnTestHub)
$btnSaveSettings = New-Button 'Save paths' 710 238 120 34
$tabSetup.Controls.Add($btnSaveSettings)

$tabSetup.Controls.Add((New-Label 'Step 2 - Qualcomm AI Hub account' 25 300 330 26 -Bold))
$tabSetup.Controls.Add((New-Label 'API token' 25 338 110 24 -Bold))
$txtSetupToken = New-TextBox 140 335 555 25 $true $false
$tabSetup.Controls.Add($txtSetupToken)
$btnConfigureToken = New-Button 'Configure / verify token' 705 332 160 32
$tabSetup.Controls.Add($btnConfigureToken)

# Keep explanatory text on its own rows. The previous fixed-coordinate layout
# placed a two-line privacy label on top of the following button row at normal
# and high-DPI scaling.
$lblTokenPrivacy = New-Label "Your token is passed only to Qualcomm's configuration command. It is never saved in user-settings.json, session logs, or support ZIPs." 140 374 725 48
$lblTokenPrivacy.ForeColor = [System.Drawing.Color]::DimGray
$tabSetup.Controls.Add($lblTokenPrivacy)

$btnOpenAiHub = New-Button 'Open AI Hub setup page' 140 430 190 32
$tabSetup.Controls.Add($btnOpenAiHub)
$lblAiHubPageHint = New-Label 'Use this only if you still need to sign in or copy an API token.' 345 436 520 28
$lblAiHubPageHint.ForeColor = [System.Drawing.Color]::DimGray
$tabSetup.Controls.Add($lblAiHubPageHint)

# Explicit navigation is intentionally duplicated here so the user never has to
# hunt for the later tab header, especially on high-DPI displays.
$btnSetupOpenCloud = New-Button 'Open Model + AI Hub' 140 478 180 32
$tabSetup.Controls.Add($btnSetupOpenCloud)
$btnSetupProbeCloud = New-Button 'Probe AI Hub now' 330 478 155 32
$tabSetup.Controls.Add($btnSetupProbeCloud)
$lblSetupCloudHint = New-Label 'After authentication is verified, continue here to check the Galaxy S26 cloud target.' 140 516 725 40
$lblSetupCloudHint.ForeColor = [System.Drawing.Color]::DimGray
$tabSetup.Controls.Add($lblSetupCloudHint)

$tabSetup.Controls.Add((New-Label 'Step 3 - Connect Galaxy S26' 25 575 330 26 -Bold))
$btnInstallSamsungDriver = New-Button 'Install Samsung USB driver' 140 608 210 32
$tabSetup.Controls.Add($btnInstallSamsungDriver)
$btnOpenSamsungDriver = New-Button 'Driver page' 360 608 105 32
$tabSetup.Controls.Add($btnOpenSamsungDriver)
$btnSetupRefreshPhone = New-Button 'Refresh phone' 475 608 130 32
$tabSetup.Controls.Add($btnSetupRefreshPhone)
$lblPhoneGuide = New-Label "On the phone: enable Developer options -> USB debugging, connect by USB, then accept the RSA authorization prompt. Install Samsung's USB driver only if Windows does not expose the device to ADB." 25 650 855 62
$tabSetup.Controls.Add($lblPhoneGuide)

$lblSetupGuide = New-Label 'Automatic setup keeps Python, ADB, FFmpeg, and qai-hub inside this project folder under tools. Existing system Python/FFmpeg installations are left untouched. A full local Qualcomm QAIRT/QNN SDK is still not required.' 25 730 855 58 -Bold
$tabSetup.Controls.Add($lblSetupGuide)

# -------------------- Device --------------------
$tabDevice = New-Object System.Windows.Forms.TabPage
$tabDevice.Text = 'Galaxy S26 / ADB'
$tabDevice.AutoScroll = $true
[void]$tabs.TabPages.Add($tabDevice)

$btnRefreshDevice = New-Button 'Refresh device' 25 22 140
$tabDevice.Controls.Add($btnRefreshDevice)
$btnDiag = New-Button 'Collect diagnostics' 180 22 150
$tabDevice.Controls.Add($btnDiag)
$btnAdbShell = New-Button 'ADB shell' 345 22 120
$tabDevice.Controls.Add($btnAdbShell)
$lblDetectedModel = New-Label '(not detected)' 500 25 180 24 -Bold
$tabDevice.Controls.Add($lblDetectedModel)
$lblDetectedSerial = New-Label '' 690 25 200 24
$tabDevice.Controls.Add($lblDetectedSerial)

$txtDevice = New-Object System.Windows.Forms.TextBox
$txtDevice.Multiline = $true
$txtDevice.ReadOnly = $true
$txtDevice.ScrollBars = 'Vertical'
$txtDevice.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]10)
$txtDevice.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,70)
$txtDevice.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,390)
$txtDevice.Anchor = 'Top,Left,Right,Bottom'
$tabDevice.Controls.Add($txtDevice)
$lblTarget = New-Label 'Expected target: SM-S942U / SM8850 / Qualcomm SoC model 87 / Hexagon V81' 25 480 850 30 -Bold
$tabDevice.Controls.Add($lblTarget)

# -------------------- Audio --------------------
$tabAudio = New-Object System.Windows.Forms.TabPage
$tabAudio.Text = '.ogg Calibration'
$tabAudio.AutoScroll = $true
[void]$tabs.TabPages.Add($tabAudio)

$lblAudioGuide = New-Label 'Step 1: choose the folder containing your lecture .ogg files. The Builder will scan subfolders too. Step 2: prepare 16 kHz PCM only when calibration data is needed.' 25 12 865 38
$tabAudio.Controls.Add($lblAudioGuide)
$tabAudio.Controls.Add((New-Label 'OGG folder' 25 57 125 24 -Bold))
$txtAudioFolder = New-TextBox 155 54 620
$tabAudio.Controls.Add($txtAudioFolder)
$btnAudioFolder = New-Button 'Browse...' 790 52 100
$tabAudio.Controls.Add($btnAudioFolder)
$btnScanAudio = New-Button 'Scan .ogg' 155 96 120
$tabAudio.Controls.Add($btnScanAudio)
$btnPrepareAudio = New-Button 'Prepare 16 kHz PCM' 290 96 170
$tabAudio.Controls.Add($btnPrepareAudio)
$btnOpenAudioOut = New-Button 'Open output' 475 96 120
$tabAudio.Controls.Add($btnOpenAudioOut)
$lblAudioSummary = New-Label 'No folder scanned.' 615 101 275 24
$tabAudio.Controls.Add($lblAudioSummary)

$listAudio = New-Object System.Windows.Forms.ListView
$listAudio.View = 'Details'
$listAudio.FullRowSelect = $true
$listAudio.GridLines = $true
$listAudio.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,142)
$listAudio.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,330)
$listAudio.Anchor = 'Top,Left,Right,Bottom'
[void]$listAudio.Columns.Add('File',590)
[void]$listAudio.Columns.Add('MiB',90)
[void]$listAudio.Columns.Add('Duration',130)
$tabAudio.Controls.Add($listAudio)
$lblAudio = New-Label 'Source recordings remain .ogg. Temporary calibration data is mono 16 kHz float32 PCM and is written only to the current workspace.' 25 490 850 45
$tabAudio.Controls.Add($lblAudio)

# -------------------- Model / Cloud --------------------
$tabCloud = New-Object System.Windows.Forms.TabPage
$tabCloud.Text = 'Model + AI Hub'
$tabCloud.AutoScroll = $true
[void]$tabs.TabPages.Add($tabCloud)

$tabCloud.Controls.Add((New-Label 'Guided NPU compile' 25 14 250 28 -Bold))
$lblCloudIntro = New-Label 'Choose a model. The Builder downloads/prepares what Qualcomm AI Hub needs, validates it on Windows, then compiles it for the Galaxy S26. Manual ONNX selection is only an advanced fallback.' 25 43 865 42
$tabCloud.Controls.Add($lblCloudIntro)

$tabCloud.Controls.Add((New-Label 'STEP 1 - Choose and prepare the model' 25 92 360 25 -Bold))
$cmbRecipe = New-Object System.Windows.Forms.ComboBox
$cmbRecipe.DropDownStyle = 'DropDownList'
[void]$cmbRecipe.Items.Add('HTP Proof-of-Life (recommended)')
[void]$cmbRecipe.Items.Add('NVIDIA Parakeet TDT 0.6B v3')
[void]$cmbRecipe.Items.Add('Omi Med STT v1 (Parakeet v2)')
[void]$cmbRecipe.Items.Add('Advanced - Custom ONNX')
$cmbRecipe.SelectedIndex = 0
$cmbRecipe.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,122)
$cmbRecipe.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(330,25)
$tabCloud.Controls.Add($cmbRecipe)

$btnPrepareModel = New-Button 'Create proof ONNX' 370 119 190 32
$tabCloud.Controls.Add($btnPrepareModel)
$btnExportParakeet = New-Button '2. Prepare encoder' 570 119 165 32
$btnExportParakeet.Visible = $false
$tabCloud.Controls.Add($btnExportParakeet)
$btnOpenModelSource = New-Button 'Open model folder' 745 119 145 32
$tabCloud.Controls.Add($btnOpenModelSource)

$lblModelSourceDescription = New-Label '' 25 158 865 55
$tabCloud.Controls.Add($lblModelSourceDescription)

$tabCloud.Controls.Add((New-Label 'Source/checkpoint' 25 219 125 22 -Bold))
$txtSourceModel = New-TextBox 155 216 635 25 $false $true
$tabCloud.Controls.Add($txtSourceModel)
$btnOpenOfficialModel = New-Button 'Source page' 800 214 90 29
$tabCloud.Controls.Add($btnOpenOfficialModel)

$lblReferenceAudioTitle = New-Label 'Reference .ogg' 25 254 125 22 -Bold
$tabCloud.Controls.Add($lblReferenceAudioTitle)
$txtReferenceAudio = New-TextBox 155 251 635 25 $false $true
$tabCloud.Controls.Add($txtReferenceAudio)
$btnReferenceAudio = New-Button 'Choose...' 800 249 90 29
$tabCloud.Controls.Add($btnReferenceAudio)

$tabCloud.Controls.Add((New-Label 'ONNX to compile' 25 289 125 22 -Bold))
$txtOnnx = New-TextBox 155 286 635 25 $false $true
$tabCloud.Controls.Add($txtOnnx)
$btnOnnx = New-Button 'Browse...' 800 284 90 29
$tabCloud.Controls.Add($btnOnnx)
$lblModelStatus = New-StatusLabel 25 316 865
$tabCloud.Controls.Add($lblModelStatus)

$tabCloud.Controls.Add((New-Label 'STEP 2 - Verify Qualcomm AI Hub target' 25 347 360 25 -Bold))
$tabCloud.Controls.Add((New-Label 'Target' 25 378 80 22 -Bold))
$txtTarget = New-TextBox 105 375 300 25 $false $true
$txtTarget.Text = 'Samsung Galaxy S26 (Family)'
$tabCloud.Controls.Add($txtTarget)
$btnProbeCloud = New-Button 'Probe AI Hub' 420 372 135 32
$tabCloud.Controls.Add($btnProbeCloud)
$lblProbeStatus = New-StatusLabel 570 377 320
$tabCloud.Controls.Add($lblProbeStatus)

$tabCloud.Controls.Add((New-Label 'STEP 3 - Compile for SM8850 / V81' 25 419 360 25 -Bold))
$tabCloud.Controls.Add((New-Label 'Precision' 25 450 75 22 -Bold))
$cmbPrecision = New-Object System.Windows.Forms.ComboBox
$cmbPrecision.DropDownStyle = 'DropDownList'
[void]$cmbPrecision.Items.Add('float16')
[void]$cmbPrecision.Items.Add('int8 (not enabled yet)')
$cmbPrecision.SelectedIndex = 0
$cmbPrecision.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(105,447)
$cmbPrecision.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(170,25)
$tabCloud.Controls.Add($cmbPrecision)
$tabCloud.Controls.Add((New-Label 'QAIRT' 295 450 55 22 -Bold))
$txtQairt = New-TextBox 350 447 105
$txtQairt.Text = 'default'
$tabCloud.Controls.Add($txtQairt)
$btnCompileCloud = New-Button 'Build QNN context' 475 443 170 34
$tabCloud.Controls.Add($btnCompileCloud)
$btnOpenCompiled = New-Button 'Open compiled' 655 443 135 34
$tabCloud.Controls.Add($btnOpenCompiled)
$lblCompileStatus = New-StatusLabel 800 450 90
$tabCloud.Controls.Add($lblCompileStatus)

$btnPrepareProjector = New-Button 'Prepare encoder projector' 25 488 215 34
$btnPrepareProjector.Visible = $false
$tabCloud.Controls.Add($btnPrepareProjector)
$btnPreparePredictor = New-Button 'Prepare TDT predictor' 250 488 215 34
$btnPreparePredictor.Visible = $false
$tabCloud.Controls.Add($btnPreparePredictor)
$btnPrepareJoint = New-Button 'Prepare TDT joint' 475 488 215 34
$btnPrepareJoint.Visible = $false
$tabCloud.Controls.Add($btnPrepareJoint)
$btnValidateController = New-Button 'Validate TDT controller' 700 488 190 34
$btnValidateController.Visible = $false
$tabCloud.Controls.Add($btnValidateController)
$btnUseExistingContext = New-Button 'Use existing context -> Deploy' 25 532 245 34
$tabCloud.Controls.Add($btnUseExistingContext)
$lblExistingContext = New-StatusLabel 280 539 610
$tabCloud.Controls.Add($lblExistingContext)

$lblCloudNext = New-Label 'All four Parakeet neural graphs + controller are validated. The linked projector+predictor+joint QNN context candidate is validated too; v0.3.21 hardens the Hugging Face deployment workflow by tolerating the deterministic package ZIP sidecar while still excluding it from deployment; it retains the canonical deployment-folder generator/uploader while preserving the four proven individual contexts and linked-decoder evidence.' 25 576 865 48 -Bold
$tabCloud.Controls.Add($lblCloudNext)

$txtCloudResult = New-Object System.Windows.Forms.TextBox
$txtCloudResult.Multiline = $true
$txtCloudResult.ReadOnly = $true
$txtCloudResult.ScrollBars = 'Vertical'
$txtCloudResult.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$txtCloudResult.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,634)
$txtCloudResult.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,120)
$txtCloudResult.Anchor = 'Top,Left,Right,Bottom'
$tabCloud.Controls.Add($txtCloudResult)

# Retain an invisible temporary-token field for compatibility with the bridge.
# Normal use relies on the persisted qai-hub profile configured on Setup.
$txtToken = New-TextBox 0 0 1 1 $true $false
$txtToken.Visible = $false
$tabCloud.Controls.Add($txtToken)

function Update-CloudWorkflowState {
    $choice = [string]$cmbRecipe.SelectedItem
    $isParakeet = ($choice -eq 'NVIDIA Parakeet TDT 0.6B v3')
    if ($choice -eq 'Omi Med STT v1 (Parakeet v2)') {
        $btnPrepareModel.Text = 'Open Omi workflow'
        $btnPrepareModel.Enabled = $true
        foreach ($control in @($btnExportParakeet,$btnPrepareProjector,$btnPreparePredictor,$btnPrepareJoint,$btnValidateController,$lblReferenceAudioTitle,$txtReferenceAudio,$btnReferenceAudio,$btnOnnx,$btnUseExistingContext)) { $control.Visible = $false }
        $btnOpenOfficialModel.Visible = $true
        $lblModelSourceDescription.Text = 'Omi v1 is a separate Parakeet v2 medical model. Use the Omi Med NPU tab for native Windows setup, canonical checkpoint download, short encoder ONNX validation and experimental Qualcomm compile. No v3 graph weights or decoder are reused.'
        $checkpoint = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        $txtSourceModel.Text = if (Test-Path -LiteralPath $checkpoint -PathType Leaf) { $checkpoint } else { '' }
        $txtOnnx.Clear()
        $btnCompileCloud.Enabled = $false
        Set-Status $lblCompileStatus 'Use Omi page' 'warn'
        Set-Status $lblModelStatus 'Omi-specific guided workflow' 'warn'
        Set-Status $lblExistingContext 'Separate Omi package is not yet Android-ready' 'warn'
        return
    }
    $btnUseExistingContext.Visible = $true
    $lblReferenceAudioTitle.Visible = $isParakeet
    $txtReferenceAudio.Visible = $isParakeet
    $btnReferenceAudio.Visible = $isParakeet
    $btnExportParakeet.Visible = $isParakeet
    $btnPrepareProjector.Visible = $isParakeet
    $btnPreparePredictor.Visible = $isParakeet
    $btnPrepareJoint.Visible = $isParakeet
    $btnValidateController.Visible = $isParakeet
    $btnUseExistingContext.Visible = $true

    switch ($choice) {
        'HTP Proof-of-Life (recommended)' {
            $btnPrepareModel.Text = 'Create proof ONNX'
            $btnPrepareModel.Enabled = $true
            $btnOnnx.Visible = $false
            $btnOpenOfficialModel.Visible = $false
            $lblModelSourceDescription.Text = 'Tiny deterministic graph used only to prove the Qualcomm HTP path. You already passed this milestone; keep it available for regression testing, but Parakeet is now the recommended next engineering step.'
            $proof = Join-Path $script:CurrentWorkspace 'source\proof\whisperrecorder-htp-proof.onnx'
            if (Test-Path -LiteralPath $proof -PathType Leaf) { $txtSourceModel.Text = $proof; $txtOnnx.Text = $proof }
            else { $txtSourceModel.Clear(); $txtOnnx.Clear() }
        }
        'NVIDIA Parakeet TDT 0.6B v3' {
            $btnPrepareModel.Text = '1. Download model'
            $btnPrepareModel.Enabled = $true
            $btnExportParakeet.Text = '2. Prepare encoder'
            $btnOnnx.Visible = $false
            $btnOpenOfficialModel.Visible = $true
            $lblModelSourceDescription.Text = 'Native-Windows path: the Builder downloads NVIDIA''s official Transformers/safetensors checkpoint, then exports a fixed 40-second FastConformer host-mask-v2 graph. Windows computes the three subsampling masks explicitly, so HTP no longer derives valid length from an INT32 scalar. The graph is validated against canonical PyTorch SDPA using one real .ogg lecture.'
            $cache = Join-Path $Root 'models\source\parakeet-tdt-0.6b-v3-transformers'
            if ((Test-Path -LiteralPath (Join-Path $cache 'model.safetensors') -PathType Leaf)) { $txtSourceModel.Text = $cache }
            else { $txtSourceModel.Clear() }
            $encoder = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-encoder-40s-mask-v2.onnx'
            $projector = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-encoder-projector-500f.onnx'
            $predictor = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-tdt-predictor-step.onnx'
            $joint = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-tdt-joint-step.onnx'
            $currentCompileTarget = [string]$txtOnnx.Text
            $currentCompileTargetValid = (-not [string]::IsNullOrWhiteSpace($currentCompileTarget)) -and (Test-Path -LiteralPath $currentCompileTarget) -and ($currentCompileTarget -match '(?:parakeet-encoder-(?:40s-mask-v2|projector-500f)|parakeet-tdt-(?:predictor|joint)-step)\.onnx$')
            if (-not $currentCompileTargetValid) {
                if (Test-Path -LiteralPath $joint -PathType Leaf) { $txtOnnx.Text = $joint }
                elseif (Test-Path -LiteralPath $predictor -PathType Leaf) { $txtOnnx.Text = $predictor }
                elseif (Test-Path -LiteralPath $projector -PathType Leaf) { $txtOnnx.Text = $projector }
                elseif (Test-Path -LiteralPath $encoder -PathType Container) { $txtOnnx.Text = $encoder }
                else { $txtOnnx.Clear() }
            }
            if ([string]::IsNullOrWhiteSpace($txtReferenceAudio.Text) -and $txtAudioFolder.Text -and (Test-Path -LiteralPath $txtAudioFolder.Text)) {
                $ogg = Get-ChildItem -LiteralPath $txtAudioFolder.Text -Filter '*.ogg' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($ogg) { $txtReferenceAudio.Text = $ogg.FullName }
            }
        }
        default {
            $btnPrepareModel.Text = 'Manual ONNX mode'
            $btnPrepareModel.Enabled = $false
            $btnOnnx.Visible = $true
            $btnOnnx.Text = 'Browse ONNX...'
            $btnOpenOfficialModel.Visible = $false
            $lblModelSourceDescription.Text = 'Advanced mode. Choose an existing ONNX graph yourself. External-weight Parakeet packages are prepared automatically by the normal Parakeet workflow.'
            if ($txtOnnx.Text -match 'whisperrecorder-htp-proof\.onnx$' -or $txtOnnx.Text -match '(?:parakeet-encoder-(?:40s-mask-v2|projector-500f)|parakeet-tdt-(?:predictor|joint)-step)\.onnx$') { $txtOnnx.Clear() }
        }
    }

    $onnxReady = (-not [string]::IsNullOrWhiteSpace($txtOnnx.Text)) -and (Test-Path -LiteralPath $txtOnnx.Text)
    $isProjectorOnnx = ($isParakeet -and $txtOnnx.Text -match 'parakeet-encoder-projector-500f\.onnx$')
    $isPredictorOnnx = ($isParakeet -and $txtOnnx.Text -match 'parakeet-tdt-predictor-step\.onnx$')
    $isJointOnnx = ($isParakeet -and $txtOnnx.Text -match 'parakeet-tdt-joint-step\.onnx$')
    $btnCompileCloud.Enabled = ($onnxReady -and $script:CloudProbeOk -and $cmbPrecision.SelectedIndex -eq 0)
    if ($onnxReady) {
        $readyText = if ($isJointOnnx) { 'TDT joint ONNX + token/duration decision desktop validation ready' } elseif ($isPredictorOnnx) { 'TDT predictor-core ONNX + recurrent-state desktop validation ready' } elseif ($isProjectorOnnx) { 'Encoder projector ONNX + desktop validation ready' } elseif ($isParakeet) { 'Encoder mask-v2 ONNX + desktop validation ready' } else { 'ONNX ready' }
        Set-Status $lblModelStatus $readyText 'good'
    } elseif ($isParakeet -and (-not [string]::IsNullOrWhiteSpace($txtSourceModel.Text)) -and (Test-Path -LiteralPath $txtSourceModel.Text)) {
        Set-Status $lblModelStatus 'Official model downloaded; reuse validated graphs or run TDT controller validation' 'warn'
    } else { Set-Status $lblModelStatus 'Not prepared' 'warn' }
    if ($script:CloudProbeOk) { Set-Status $lblProbeStatus 'Galaxy S26 target verified' 'good' } else { Set-Status $lblProbeStatus 'Probe required only for a new build' 'warn' }

    $reusable = @()
    if ($choice -eq 'HTP Proof-of-Life (recommended)') {
        $reusable = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { $_.Name -match '^whisperrecorder-htp-proof-sm8850-v81-fp16\.bin$' })
    } elseif ($isParakeet) {
        if ($isJointOnnx) {
            $reusable = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { $_.Name -match '^parakeet-tdt-joint-step-sm8850-v81-fp16\.bin$' })
        } elseif ($isPredictorOnnx) {
            $reusable = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { $_.Name -match '^parakeet-tdt-predictor-step-sm8850-v81-fp16\.bin$' })
        } elseif ($isProjectorOnnx) {
            $reusable = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { $_.Name -match '^parakeet-encoder-projector-500f-sm8850-v81-fp16\.bin$' })
        } else {
            $reusable = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { $_.Name -match '^parakeet-encoder-40s-mask-v2-sm8850-v81-fp16\.bin$' })
        }
    }
    if ($reusable.Count -gt 0) {
        $btnUseExistingContext.Enabled = $true
        Set-Status $lblExistingContext ('Found ' + $reusable.Count) 'good'
        $btnCompileCloud.Text = 'Rebuild QNN context'
        Set-Status $lblCompileStatus 'Existing .bin reusable' 'good'
    } else {
        $btnUseExistingContext.Enabled = $false
        Set-Status $lblExistingContext 'None found' 'warn'
        $btnCompileCloud.Text = 'Build QNN context'
        if ($btnCompileCloud.Enabled) { Set-Status $lblCompileStatus 'Ready to build' 'good' } else { Set-Status $lblCompileStatus 'Waiting' 'warn' }
    }
}

# -------------------- Qwen3-ASR (new independent research path) --------------------
$tabQwenAsr = New-Object System.Windows.Forms.TabPage
$tabQwenAsr.Text = 'Qwen3 ASR NPU'
$tabQwenAsr.AutoScroll = $true
$tabQwenAsr.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,7200)
[void]$tabs.TabPages.Add($tabQwenAsr)
$tabQwenAsr.Controls.Add((New-Label 'QWEN3 ASR 0.6B / 1.7B - Snapdragon S26 research' 25 16 830 28 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'INDEPENDENT from Omi Med and Parakeet v3. First verify official Qwen3-ASR source and ONNX graph contracts. Stage F confirmed S26 QNN encoder. Stage G proves real-speech hybrid parity and packages an experimental release. Android S26 QNN encoder works; decoder still runs on CPU.' 25 48 850 56))
$tabQwenAsr.Controls.Add((New-Label 'Select model variant' 25 127 205 25 -Bold))
$cmbQwenAsrSize = New-Object System.Windows.Forms.ComboBox
$cmbQwenAsrSize.DropDownStyle = 'DropDownList'
$cmbQwenAsrSize.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(228,123)
$cmbQwenAsrSize.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(160,29)
[void]$cmbQwenAsrSize.Items.Add('0.6B')
[void]$cmbQwenAsrSize.Items.Add('1.7B')
$cmbQwenAsrSize.SelectedIndex = 0
$tabQwenAsr.Controls.Add($cmbQwenAsrSize)
$tabQwenAsr.Controls.Add((New-Label 'Stage A - Inspect official model repository (small config, no large download)' 25 191 845 30 -Bold))
$btnQwenAsrInspect = New-Button 'Inspect selected source (online)' 25 235 320 39
$tabQwenAsr.Controls.Add($btnQwenAsrInspect)
$lblQwenAsrInspect = New-StatusLabel 359 242 490
$tabQwenAsr.Controls.Add($lblQwenAsrInspect)
$tabQwenAsr.Controls.Add((New-Label 'Official models: Qwen/Qwen3-ASR-0.6B and Qwen/Qwen3-ASR-1.7B. Pins Hugging Face commit, verifies speech ASR architecture, and inventories weight files without downloading them.' 25 286 845 58))
$tabQwenAsr.Controls.Add((New-Label 'Stage B - Download original source weights (OPTIONAL, several GB)' 25 378 845 29 -Bold))
$btnQwenAsrDownload = New-Button 'Download selected official weights' 25 424 320 39
$tabQwenAsr.Controls.Add($btnQwenAsrDownload)
$lblQwenAsrDownload = New-StatusLabel 359 431 490
$tabQwenAsr.Controls.Add($lblQwenAsrDownload)
$tabQwenAsr.Controls.Add((New-Label 'Requires a completed Stage A inventory. Separate model folders, pinned revisions. Do NOT start unless needed: source weights may consume 1-4+ GB per model.' 25 479 845 58))
$tabQwenAsr.Controls.Add((New-Label 'Stage C - Export original Qwen3-ASR to ONNX (Windows CPU)' 25 553 845 28 -Bold))
$btnQwenAsrExport = New-Button 'Export selected model to ONNX (CPU)' 25 598 340 40
$tabQwenAsr.Controls.Add($btnQwenAsrExport)
$btnQwenAsrVerifyExisting = New-Button 'Verify existing ONNX (no rerun)' 383 598 370 40
$tabQwenAsr.Controls.Add($btnQwenAsrVerifyExisting)
$lblQwenAsrExport = New-StatusLabel 25 646 816
$tabQwenAsr.Controls.Add($lblQwenAsrExport)
$tabQwenAsr.Controls.Add((New-Label 'For completed exports, click Verify existing ONNX (no rerun) first. Export uses isolated CPU Python and can consume several GB of RAM/disk. Verifying files does NOT establish Qualcomm compatibility.' 25 687 845 66))
$tabQwenAsr.Controls.Add((New-Label 'Stage D - Audit ONNX graphs (not QNN compilation)' 25 782 845 28 -Bold))
$txtQwenAsrOnnx = New-TextBox 25 824 648 30
$tabQwenAsr.Controls.Add($txtQwenAsrOnnx)
$btnQwenAsrBrowse = New-Button 'Browse...' 686 822 155 33
$tabQwenAsr.Controls.Add($btnQwenAsrBrowse)
$btnQwenAsrAudit = New-Button 'Audit ONNX encoder/init/step' 25 874 320 39
$tabQwenAsr.Controls.Add($btnQwenAsrAudit)
$lblQwenAsrAudit = New-StatusLabel 359 882 490
$tabQwenAsr.Controls.Add($lblQwenAsrAudit)
$tabQwenAsr.Controls.Add((New-Label 'This checks graph names, inputs/outputs, ONNX ops and external weights. You must still establish PyTorch parity, QNN op support, compilability, KV-cache semantics, S26 numerical accuracy and full transcription.' 25 924 845 74))
$btnQwenAsrOpenFolder = New-Button 'Open Qwen3-ASR reports / files' 25 1042 320 39
$tabQwenAsr.Controls.Add($btnQwenAsrOpenFolder)
$btnQwenAsrOfficial = New-Button 'Official Qwen3-ASR GitHub' 359 1042 237 39
$tabQwenAsr.Controls.Add($btnQwenAsrOfficial)
$btnQwenAsrExporter = New-Button 'ONNX exporter reference' 609 1042 232 39
$tabQwenAsr.Controls.Add($btnQwenAsrExporter)
$tabQwenAsr.Controls.Add((New-Label 'Not included in this stage: NPU .bin creation, QNN EPContext, Qualcomm runtime, or working Android ASR. Models never overwrite Omi / Parakeet paths.' 25 1112 845 65))

function Get-QwenAsrModelFolder {
    $size = [string]$cmbQwenAsrSize.SelectedItem
    if ($size -ne '0.6B' -and $size -ne '1.7B') { throw 'Select a supported Qwen3-ASR model.' }
    $slug = if ($size -eq '0.6B') { '0.6b' } else { '1.7b' }
    return (Join-Path $script:QwenAsrWorkspace $slug)
}

# Restore the completed ONNX export when the Builder starts or model size changes.
# A text field can be blank even when Stage C and Stage D succeeded in a prior
# session. Never require the user to re-export multi-GB graphs for that reason.
function Restore-QwenAsrOnnxFolder {
    $modelFolder = Get-QwenAsrModelFolder
    $canonical = Join-Path $modelFolder 'onnx-fp32'
    if (Test-Path -LiteralPath $canonical -PathType Container) {
        $txtQwenAsrOnnx.Text = $canonical
    } else {
        $txtQwenAsrOnnx.Clear()
    }
}
$cmbQwenAsrSize.Add_SelectedIndexChanged({
    try { Restore-QwenAsrOnnxFolder } catch { Show-Error $_ }
})
Restore-QwenAsrOnnxFolder

$btnQwenAsrInspect.Add_Click({
    Invoke-UiAction 'Inspecting official Qwen3-ASR source...' {
        $size = [string]$cmbQwenAsrSize.SelectedItem
        $folder = Get-QwenAsrModelFolder
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        $python = Ensure-HuggingFaceUploaderPython
        $report = Join-Path $folder 'official-source-inventory.json'
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrResearchPath, 'inspect-source', '--size', $size, '--output', $report) -TimeoutSeconds 300
        if ($r.ExitCode -ne 0) { throw ('Qwen3-ASR official source inspection failed: ' + $r.StdErr + $r.StdOut) }
        $data = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        Set-Status $lblQwenAsrInspect ('Official ASR source verified: ' + $data.source_commit.Substring(0,12)) 'good'
        Append-Log ('[Qwen3 ASR] Report: ' + $report)
    }
})
$btnQwenAsrDownload.Add_Click({
    Invoke-UiAction 'Downloading pinned official Qwen3-ASR source weights...' {
        $size = [string]$cmbQwenAsrSize.SelectedItem
        $folder = Get-QwenAsrModelFolder
        $inventory = Join-Path $folder 'official-source-inventory.json'
        if (-not (Test-Path -LiteralPath $inventory -PathType Leaf)) { throw 'Run Stage A inspection for this model first.' }
        $data = Get-Content -LiteralPath $inventory -Raw | ConvertFrom-Json
        $message = ('Download original Qwen3-ASR ' + $size + " checkpoint?`r`n`r`n" + 'Destination: ' + $folder + "`r`n" + 'Reported weight bytes: ' + $data.weights_bytes_reported + "`r`n`r`n" + 'This is source-weight download only, NOT an NPU model.')
        $answer = [System.Windows.Forms.MessageBox]::Show($message,'Confirm multi-GB model download',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { Append-Log '[Qwen3 ASR] Source download cancelled.'; return }
        $python = Ensure-HuggingFaceUploaderPython
        $dest = Join-Path $folder 'original-source'
        $report = Join-Path $folder 'official-source-download.json'
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrResearchPath,'download-source','--size',$size,'--source-report',$inventory,'--folder',$dest,'--output',$report) -TimeoutSeconds 21600
        if ($r.ExitCode -ne 0) { throw ('Qwen3-ASR weight download failed: ' + $r.StdErr + $r.StdOut) }
        Set-Status $lblQwenAsrDownload 'Source downloaded (NOT compiled)' 'good'
        Append-Log ('[Qwen3 ASR] Download receipt: ' + $report)
    }
})

function Ensure-QwenAsrExportPython {
    # Separate virtualenv: never install PyTorch/Qwen into Omi, Parakeet or HF uploader environments.
    $base = Get-LocalBuilderPython
    $envRoot = Join-Path $Root 'tools\qwen-asr-export-env'
    $python = Join-Path $envRoot 'Scripts\python.exe'
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
        Append-Log '[Qwen3 ASR] Creating isolated native-Windows ONNX exporter environment...'
        $r = Invoke-ProcessCapture -FilePath $base -Arguments @('-m','venv',$envRoot) -TimeoutSeconds 300
        if ($r.ExitCode -ne 0) { throw ('Qwen ONNX exporter venv failed: ' + $r.StdErr + $r.StdOut) }
    }
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) { throw ('Missing Qwen ONNX Python environment: ' + $python) }
    $ready = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import torch, transformers, qwen_asr, onnx, onnxscript, huggingface_hub, accelerate, soundfile, librosa; print("Qwen ONNX deps ready")') -TimeoutSeconds 75
    if ($ready.ExitCode -ne 0) {
        Append-Log '[Qwen3 ASR] Installing PyTorch CPU/Qwen-ASR/ONNX prerequisites in Qwen-only venv (may take several minutes and GBs)...'
        # Upstream dependency set plus qwen-asr and accelerate, required by its model loader.
        $install = Invoke-ProcessCapture -FilePath $python -Arguments @('-m','pip','install','--upgrade','qwen-asr','accelerate','onnx','onnxruntime','onnxscript','numpy','soundfile','librosa','huggingface_hub') -TimeoutSeconds 7200
        if ($install.ExitCode -ne 0) { throw ('Qwen ONNX dependency setup failed: ' + $install.StdErr + $install.StdOut) }
        $ready = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import torch, transformers, qwen_asr, onnx, onnxscript, huggingface_hub, accelerate, soundfile, librosa; print("Qwen ONNX deps ready")') -TimeoutSeconds 90
        if ($ready.ExitCode -ne 0) { throw ('Qwen ONNX dependencies still unavailable: ' + $ready.StdErr + $ready.StdOut) }
    }
    return $python
}

# J47-J48 reuses existing ONNX Runtime; it NEVER needs the heavyweight
# torch/transformers/Qwen export import probe.  On a busy Windows PC the
# export-stack import can exceed its 75-second watchdog even when every
# decoder dependency is healthy.  No installation or re-export here.
function Ensure-QwenAsrOnnxRuntimePython {
    $python = Join-Path $Root 'tools\qwen-asr-export-env\Scripts\python.exe'
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
        throw 'J47_RUNTIME_PYTHON_MISSING: existing Qwen ONNX Python environment is missing. Restore the verified tools environment; do not re-export or redownload model weights.'
    }
    $probe = Join-Path $Root 'python\qwen_onnx_runtime_preflight.py'
    if (-not (Test-Path -LiteralPath $probe -PathType Leaf)) { throw 'J47_RUNTIME_PREFLIGHT_SCRIPT_MISSING' }
    $ready = Invoke-ProcessCapture -FilePath $python -Arguments @($probe) -TimeoutSeconds 40
    if ($ready.ExitCode -ne 0) {
        # A probe may fail due to missing modules or unusual Python startup.
        # Fail with an actionable message; never auto-repair the environment
        # during a read-only workflow, and never invoke the full export stack.
        throw 'J47_ONNX_RUNTIME_PREFLIGHT_BLOCKED: existing Qwen venv cannot discover numpy, onnx and onnxruntime. Check its Python environment; no automatic package install or model re-export was attempted.'
    }
    return $python
}

$btnQwenAsrExport.Add_Click({
    Invoke-UiAction 'Exporting official Qwen3-ASR to FP32 ONNX on Windows CPU...' {
        $size = [string]$cmbQwenAsrSize.SelectedItem
        $folder = Get-QwenAsrModelFolder
        $inventory = Join-Path $folder 'official-source-inventory.json'
        if (-not (Test-Path -LiteralPath $inventory -PathType Leaf)) { throw 'Run Stage A source inspection first, to pin the official checkpoint commit.' }
        $data = Get-Content -LiteralPath $inventory -Raw | ConvertFrom-Json
        $expected = if ($size -eq '0.6B') { 'Qwen/Qwen3-ASR-0.6B' } else { 'Qwen/Qwen3-ASR-1.7B' }
        if ($data.repo_id -ne $expected -or $data.source_commit -notmatch '^[a-fA-F0-9]{40}$') { throw 'Official checkpoint inventory does not match the selected model or lacks pinned commit.' }
        $onnxFolder = Join-Path $folder 'onnx-fp32'
        # Never overwrite a completed export. Incomplete folders from a failed
        # third-party export may be archived, with the user's confirmation.
        $verifiedExportReceipt = Join-Path $folder 'onnx-export-receipt.json'
        $priorIncompleteReceipt = $false
        if (Test-Path -LiteralPath $verifiedExportReceipt -PathType Leaf) {
            try {
                $existingReceipt = Get-Content -LiteralPath $verifiedExportReceipt -Raw | ConvertFrom-Json
                $priorIncompleteReceipt = ($existingReceipt.status -eq 'onnx_export_incomplete')
            } catch {
                throw ('ONNX receipt is unreadable. Preserve it and inspect manually before retrying: ' + $verifiedExportReceipt)
            }
            if (-not $priorIncompleteReceipt) {
                throw ('An ONNX export receipt already exists. Preserve completed results; use Stage D to audit or move the existing export and receipt aside before starting a new export: ' + $verifiedExportReceipt)
            }
        }
        $archiveIncompleteOnnx = $priorIncompleteReceipt
        if (Test-Path -LiteralPath $onnxFolder -PathType Container) {
            if (@(Get-ChildItem -LiteralPath $onnxFolder -Force -ErrorAction SilentlyContinue).Count -gt 0) { $archiveIncompleteOnnx = $true }
        }
        $retryNotice = if ($archiveIncompleteOnnx) {
            "INCOMPLETE PREVIOUS EXPORT FOUND: The existing ONNX folder will be MOVED to a dated backup. No files will be deleted. Original checkpoint weights and exporter environment will be reused.`r`n`r`n"
        } else { '' }
        $message = ('Export official Qwen3-ASR ' + $size + " to FP32 ONNX on Windows CPU?`r`n`r`n" +
            $retryNotice +
            'Official checkpoint: ' + $expected + '@' + $data.source_commit.Substring(0,12) + "`r`n" +
            'Output: ' + $onnxFolder + "`r`n`r`n" +
            'This downloads and executes independent third-party exporter Python source from GitHub. It may install several GB of dependencies, and use several more GB of disk and system RAM. 1.7B may require over 16 GB RAM and 20 GB free disk. Existing Omi/Parakeet files are unchanged.' + "`r`n`r`n" +
            'CPU mode only. These ONNX files are NOT Snapdragon/QNN binaries. Continue?')
        if ([System.Windows.Forms.MessageBox]::Show($message,'Confirm Qwen3-ASR ONNX export',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning) -ne [System.Windows.Forms.DialogResult]::Yes) { Append-Log '[Qwen3 ASR] Export cancelled.'; return }
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        $basePython = Get-LocalBuilderPython
        $exporterReceipt = Join-Path $folder 'third-party-exporter-provenance.json'
        # Reuse the exact pinned exporter revision from the prior attempt,
        # rather than silently switching GitHub revisions during a retry.
        $fetchArgs = @($script:QwenAsrExportPath,'fetch-exporter','--tools',(Join-Path $Root 'tools\qwen3-asr-exporter'),'--output',$exporterReceipt)
        if (Test-Path -LiteralPath $exporterReceipt -PathType Leaf) {
            $priorExporter = Get-Content -LiteralPath $exporterReceipt -Raw | ConvertFrom-Json
            if ([string]$priorExporter.exporter_commit -match '^[0-9a-fA-F]{40}$') {
                $fetchArgs += @('--revision',[string]$priorExporter.exporter_commit)
            }
        }
        $fetch = Invoke-ProcessCapture -FilePath $basePython -Arguments $fetchArgs -TimeoutSeconds 600
        if ($fetch.ExitCode -ne 0) { throw ('Pinned exporter download failed: ' + $fetch.StdErr + $fetch.StdOut) }
        $source = Get-Content -LiteralPath $exporterReceipt -Raw | ConvertFrom-Json
        Append-Log ('[Qwen3 ASR] Pinned independent exporter code: ' + $source.exporter_url)
        $python = Ensure-QwenAsrExportPython
        $weights = Join-Path $folder 'original-source'
        $receipt = Join-Path $folder 'official-source-download.json'
        $weightsValid = $false
        if ((Test-Path -LiteralPath $receipt -PathType Leaf) -and @(Get-ChildItem -LiteralPath $weights -Filter '*.safetensors' -ErrorAction SilentlyContinue).Count -gt 0) {
            $old = Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
            $weightsValid = ($old.repo_id -eq $expected -and $old.source_commit -eq $data.source_commit)
        }
        if (-not $weightsValid) {
            Append-Log '[Qwen3 ASR] Downloading pinned official checkpoint for this export. Reusing Stage B source folder...'
            $dl = Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrResearchPath,'download-source','--size',$size,'--source-report',$inventory,'--folder',$weights,'--output',$receipt) -TimeoutSeconds 21600
            if ($dl.ExitCode -ne 0) { throw ('Pinned Qwen source download failed: ' + $dl.StdErr + $dl.StdOut) }
        }
        # Move incomplete output out of the way AFTER confirmation and
        # dependencies are ready; never delete it or overwrite it in place.
        if ($archiveIncompleteOnnx) {
            $backupPath = Join-Path $folder ('onnx-fp32-incomplete-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
            if (Test-Path -LiteralPath $backupPath) { throw ('Refusing to overwrite ONNX backup: ' + $backupPath) }
            if (Test-Path -LiteralPath $onnxFolder -PathType Container) {
                Move-Item -LiteralPath $onnxFolder -Destination $backupPath -ErrorAction Stop
            } else {
                New-Item -ItemType Directory -Path $backupPath -ErrorAction Stop | Out-Null
            }
            if ($priorIncompleteReceipt) {
                Move-Item -LiteralPath $verifiedExportReceipt -Destination (Join-Path $backupPath 'onnx-export-receipt.json') -ErrorAction Stop
            }
            Append-Log ('[Qwen3 ASR] Previous incomplete export archived (not deleted): ' + $backupPath)
        }
        # Model weights are passed as an explicit local directory to avoid accidentally loading a newer main revision.
        New-Item -ItemType Directory -Path $onnxFolder -Force | Out-Null
        $entry = Join-Path ([string]$source.folder) 'export.py'
        if (-not (Test-Path -LiteralPath $entry -PathType Leaf)) { throw 'Pinned exporter entrypoint missing after download.' }
        Append-Log '[Qwen3 ASR] Starting official-checkpoint FP32 CPU export. This can take a long time...'
        # PyTorch's modern ONNX exporter prints non-ASCII status symbols.
        # Windows PowerShell 5.1 redirects child output with a legacy console
        # code page unless the Python process is explicitly configured.
        $run = Invoke-ProcessCapture -FilePath $python -Arguments @($entry,'--model',$weights,'--output',$onnxFolder,'--device','cpu','--dtype','fp32','--opset','17') -TimeoutSeconds 21600 -Environment @{ 'PYTHONUNBUFFERED'='1'; 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'HF_HUB_DISABLE_TELEMETRY'='1' }
        if ($run.ExitCode -ne 0) { throw ('Qwen3-ASR ONNX export failed; partial exports are retained for troubleshooting, not approved: ' + $run.StdErr + $run.StdOut) }
        $verifyReport = Join-Path $folder 'onnx-export-receipt.json'
        $verify = Invoke-ProcessCapture -FilePath $basePython -Arguments @($script:QwenAsrExportPath,'verify-export','--size',$size,'--folder',$onnxFolder,'--exporter-receipt',$exporterReceipt,'--output',$verifyReport) -TimeoutSeconds 90
        if ($verify.ExitCode -ne 0) { throw ('Qwen ONNX export incomplete: ' + $verify.StdErr + $verify.StdOut) }
        $txtQwenAsrOnnx.Text = $onnxFolder
        Set-Status $lblQwenAsrExport 'FP32 ONNX files generated; audit next' 'good'
        Append-Log ('[Qwen3 ASR] ONNX output: ' + $onnxFolder)
        Append-Log ('[Qwen3 ASR] Export receipt: ' + $verifyReport)
    }
})

$btnQwenAsrVerifyExisting.Add_Click({
    Invoke-UiAction 'Verifying existing Qwen3-ASR ONNX files without re-exporting...' {
        $size = [string]$cmbQwenAsrSize.SelectedItem
        $folder = Get-QwenAsrModelFolder
        $onnxFolder = Join-Path $folder 'onnx-fp32'
        if (-not (Test-Path -LiteralPath $onnxFolder -PathType Container)) {
            throw ('No existing ONNX export found for ' + $size + ': ' + $onnxFolder)
        }
        $exporterReceipt = Join-Path $folder 'third-party-exporter-provenance.json'
        if (-not (Test-Path -LiteralPath $exporterReceipt -PathType Leaf)) {
            throw ('Cannot verify model provenance: missing pinned exporter receipt: ' + $exporterReceipt)
        }
        $basePython = Get-LocalBuilderPython
        $report = Join-Path $folder 'onnx-export-receipt.json'
        $verify = Invoke-ProcessCapture -FilePath $basePython -Arguments @($script:QwenAsrExportPath,'verify-export','--size',$size,'--folder',$onnxFolder,'--exporter-receipt',$exporterReceipt,'--output',$report) -TimeoutSeconds 120
        if ($verify.ExitCode -ne 0) {
            throw ('Existing ONNX output still incomplete; no model files were changed. Inspect receipt: ' + $report + '. ' + $verify.StdOut + $verify.StdErr)
        }
        $txtQwenAsrOnnx.Text = $onnxFolder
        Set-Status $lblQwenAsrExport 'Existing ONNX files detected; run Stage D' 'good'
        Append-Log ('[Qwen3 ASR] Existing ONNX export verified without running PyTorch: ' + $report)
    }
})

$btnQwenAsrBrowse.Add_Click({ Choose-Folder $txtQwenAsrOnnx 'Select folder with Qwen3-ASR encoder.onnx, decoder_init.onnx, decoder_step.onnx' })
$btnQwenAsrAudit.Add_Click({
    Invoke-UiAction 'Auditing Qwen3-ASR ONNX graph structure...' {
        if ([string]::IsNullOrWhiteSpace([string]$txtQwenAsrOnnx.Text) -or -not (Test-Path -LiteralPath $txtQwenAsrOnnx.Text -PathType Container)) {
            throw 'Select an existing ONNX model directory first, or click Verify existing ONNX (no rerun).'
        }
        $size = [string]$cmbQwenAsrSize.SelectedItem
        $folder = Get-QwenAsrModelFolder
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        $python = Ensure-HuggingFaceUploaderPython
        $check = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import onnx; print(onnx.__version__)') -TimeoutSeconds 60
        if ($check.ExitCode -ne 0) {
            Append-Log '[Qwen3 ASR] Installing onnx for metadata-only graph inspection (no Qualcomm SDK)...'
            $install = Invoke-ProcessCapture -FilePath $python -Arguments @('-m','pip','install','onnx') -TimeoutSeconds 1800
            if ($install.ExitCode -ne 0) { throw ('Unable to install ONNX graph inspector: ' + $install.StdErr + $install.StdOut) }
        }
        $report = Join-Path $folder 'onnx-structure-audit.json'
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrResearchPath,'audit-onnx','--size',$size,'--folder',$txtQwenAsrOnnx.Text,'--output',$report) -TimeoutSeconds 600
        if ($r.ExitCode -ne 0) { throw ('Qwen3-ASR ONNX inspection failed: ' + $r.StdErr + $r.StdOut) }
        $data = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($data.status -ne 'onnx_structure_audited') { throw ('ONNX structure incomplete; see ' + $report) }
        Set-Status $lblQwenAsrAudit '3 graph structures found (QNN NOT tested)' 'warn'
        Append-Log ('[Qwen3 ASR] ONNX inspection report: ' + $report)
    }
})
$btnQwenAsrOpenFolder.Add_Click({
    try {
        $folder = Get-QwenAsrModelFolder
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        Open-Folder $folder
    } catch { Show-Error $_ }
})
$btnQwenAsrOfficial.Add_Click({ try { Open-Url 'https://github.com/QwenLM/Qwen3-ASR' } catch { Show-Error $_ } })
$btnQwenAsrExporter.Add_Click({ try { Open-Url 'https://github.com/andrewleech/qwen3-asr-onnx' } catch { Show-Error $_ } })

# -------------------- Qwen3-ASR Stage E (independent validation and encoder preparation) --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage E1 - Original PyTorch vs FP32 ONNX: same audio, same tokens' 25 1215 850 29 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Select a short WAV or OGG. For OGG, the Builder decodes the FIRST 30 seconds as temporary 16 kHz mono WAV using FFmpeg, then deletes it. CPU validation may use substantial RAM.' 25 1254 835 51))
$txtQwenAsrAudio = New-TextBox 25 1318 645 30
$tabQwenAsr.Controls.Add($txtQwenAsrAudio)
$btnQwenAsrAudioBrowse = New-Button 'Browse WAV/OGG...' 684 1316 157 34
$tabQwenAsr.Controls.Add($btnQwenAsrAudioBrowse)
$btnQwenAsrParity = New-Button 'Run original vs ONNX validation (CPU)' 25 1365 367 42
$tabQwenAsr.Controls.Add($btnQwenAsrParity)
$lblQwenAsrParity = New-StatusLabel 405 1374 440
$tabQwenAsr.Controls.Add($lblQwenAsrParity)
$tabQwenAsr.Controls.Add((New-Label 'Stage E2 - Plan fixed NPU shapes (separate report; no original ONNX changes)' 25 1440 845 29 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Mel frames (NOT seconds)' 25 1480 236 26))
$txtQwenMelFrames = New-TextBox 264 1479 89 28
$txtQwenMelFrames.Text = '1200'
$tabQwenAsr.Controls.Add($txtQwenMelFrames)
$tabQwenAsr.Controls.Add((New-Label 'Prefill tokens' 377 1480 120 25))
$txtQwenPrefill = New-TextBox 500 1479 87 28
$txtQwenPrefill.Text = '256'
$tabQwenAsr.Controls.Add($txtQwenPrefill)
$tabQwenAsr.Controls.Add((New-Label 'Past tokens' 604 1480 110 25))
$txtQwenPast = New-TextBox 722 1479 115 28
$txtQwenPast.Text = '512'
$tabQwenAsr.Controls.Add($txtQwenPast)
$btnQwenAsrShape = New-Button 'Generate fixed-shape and KV cache plan' 25 1521 367 39
$tabQwenAsr.Controls.Add($btnQwenAsrShape)
$lblQwenAsrShape = New-StatusLabel 405 1529 440
$tabQwenAsr.Controls.Add($lblQwenAsrShape)
$tabQwenAsr.Controls.Add((New-Label 'Stage E3 - Screen unsupported/dynamic ONNX operators (NOT a Qualcomm compile)' 25 1589 845 28 -Bold))
$btnQwenAsrOps = New-Button 'Screen graphs for Qualcomm HTP risks' 25 1630 367 39
$tabQwenAsr.Controls.Add($btnQwenAsrOps)
$lblQwenAsrOps = New-StatusLabel 405 1638 440
$tabQwenAsr.Controls.Add($lblQwenAsrOps)
$tabQwenAsr.Controls.Add((New-Label 'Stage E4 - Create separate SM8850 encoder ONNX compilation candidate' 25 1699 845 30 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Writes a new large FP32 encoder.onnx with the selected fixed mel input. DOES NOT compile QNN or change source ONNX. QNN compiler may reject dynamic internal operators. No SDK download required for preparation.' 25 1740 838 75))
$btnQwenAsrCandidate = New-Button 'Prepare encoder candidate for Qualcomm' 25 1824 367 40
$tabQwenAsr.Controls.Add($btnQwenAsrCandidate)
$lblQwenAsrCandidate = New-StatusLabel 405 1832 440
$tabQwenAsr.Controls.Add($lblQwenAsrCandidate)
$btnQwenAsrStageEFolder = New-Button 'Open Stage E validation / candidate folder' 25 1896 367 40
$tabQwenAsr.Controls.Add($btnQwenAsrStageEFolder)
$tabQwenAsr.Controls.Add((New-Label 'Stage E ends with a non-destructive ONNX candidate. Continue below to Stage F for Qualcomm compilation and a single-encoder S26 proof. Decoder and full ASR remain separate.' 25 1970 835 65))

function Get-QwenStageEData {
    $size = [string]$cmbQwenAsrSize.SelectedItem
    $modelFolder = Get-QwenAsrModelFolder
    $canonical = Join-Path $modelFolder 'onnx-fp32'
    $requested = [string]$txtQwenAsrOnnx.Text
    $onnxFolder = ''
    # Test-Path -LiteralPath '' throws a PowerShell parameter-binding error.
    # Prefer an explicitly selected existing export, otherwise recover the
    # size-specific canonical export already verified in Stage C/D.
    if (-not [string]::IsNullOrWhiteSpace($requested)) {
        $requested = $requested.Trim()
        if (Test-Path -LiteralPath $requested -PathType Container) { $onnxFolder = $requested }
    }
    if ([string]::IsNullOrWhiteSpace($onnxFolder) -and (Test-Path -LiteralPath $canonical -PathType Container)) {
        $onnxFolder = $canonical
        if (-not [string]::Equals($requested, $canonical, [System.StringComparison]::OrdinalIgnoreCase)) {
            Append-Log ('[Qwen3 ASR] Stage E recovered existing ONNX export: ' + $onnxFolder)
        }
    }
    $audit = Join-Path $modelFolder 'onnx-structure-audit.json'
    if (-not (Test-Path -LiteralPath $audit -PathType Leaf)) { throw ('Run Stage D ONNX audit first: ' + $audit) }
    if ([string]::IsNullOrWhiteSpace($onnxFolder)) {
        throw ('No ONNX export folder found for Qwen3-ASR ' + $size + '. Use Verify existing ONNX (no rerun), or select a valid Stage D folder. Expected: ' + $canonical)
    }
    foreach ($graph in @('encoder.onnx','decoder_init.onnx','decoder_step.onnx')) {
        if (-not (Test-Path -LiteralPath (Join-Path $onnxFolder $graph) -PathType Leaf)) {
            throw ('Stage E cannot find ' + $graph + ' in ' + $onnxFolder + '. Choose the complete Stage C ONNX export folder, not the model weights directory.')
        }
    }
    if (-not [string]::Equals([string]$txtQwenAsrOnnx.Text, $onnxFolder, [System.StringComparison]::OrdinalIgnoreCase)) {
        $txtQwenAsrOnnx.Text = $onnxFolder
    }
    $outDir = Join-Path $modelFolder 'stage-e-sm8850'
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    return @{ Size = $size; ModelFolder = $modelFolder; Onnx = $onnxFolder; Audit = $audit; OutDir = $outDir }
}
# Share one audio-picker implementation between the earlier E/G tests and the
# combined J workflow. A recording is only selected by the user; do not scan
# personal folders or persist their audio into receipts/settings.
function Select-QwenAsrValidationAudio {
    param(
        [System.Windows.Forms.TextBox]$PathField,
        [string]$Title = 'Choose a short Qwen3-ASR validation recording'
    )
    $dlg = New-Object System.Windows.Forms.OpenFileDialog
    try {
        $dlg.Filter = 'Audio (*.wav;*.ogg)|*.wav;*.ogg|WAV (*.wav)|*.wav|OGG (*.ogg)|*.ogg'
        $dlg.Title = $Title
        $dlg.CheckFileExists = $true
        $dlg.CheckPathExists = $true
        $dlg.Multiselect = $false
        $existing = ([string]$PathField.Text).Trim()
        if ($existing -and (Test-Path -LiteralPath $existing -PathType Leaf)) {
            $dlg.FileName = $existing
        }
        if ($dlg.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { return $false }
        if (-not (Test-Path -LiteralPath $dlg.FileName -PathType Leaf)) { return $false }
        if ([IO.Path]::GetExtension($dlg.FileName).ToLowerInvariant() -notin @('.ogg','.wav')) { return $false }
        $PathField.Text = $dlg.FileName
        return $true
    } finally { $dlg.Dispose() }
}
$btnQwenAsrAudioBrowse.Add_Click({
    try { [void](Select-QwenAsrValidationAudio -PathField $txtQwenAsrAudio) }
    catch { Show-Error $_ }
})
$btnQwenAsrParity.Add_Click({
    Invoke-UiAction 'Running original Qwen3-ASR PyTorch versus FP32 ONNX (CPU)...' {
        $p = Get-QwenStageEData
        $audio = [string]$txtQwenAsrAudio.Text
        if ([string]::IsNullOrWhiteSpace($audio) -or -not (Test-Path -LiteralPath $audio -PathType Leaf)) { throw 'Choose a short WAV or OGG recording first.' }
        $ext = [IO.Path]::GetExtension($audio).ToLowerInvariant()
        if ($ext -notin @('.wav','.ogg')) { throw 'Stage E1 accepts .wav and .ogg recordings.' }
        $sourceFolder = Join-Path $p.ModelFolder 'original-source'
        $sourceReceipt = Join-Path $p.ModelFolder 'official-source-download.json'
        if (-not (Test-Path -LiteralPath $sourceReceipt -PathType Leaf)) { throw 'Missing original source receipt: use Stage C export/source download first.' }
        $downloadInfo = Get-Content -LiteralPath $sourceReceipt -Raw | ConvertFrom-Json
        $inventoryInfo = Get-Content -LiteralPath (Join-Path $p.ModelFolder 'official-source-inventory.json') -Raw | ConvertFrom-Json
        if ($downloadInfo.source_commit -ne $inventoryInfo.source_commit) { throw 'Original Qwen weights revision differs from pinned Stage A inventory.' }
        $exporter = Get-Content -LiteralPath (Join-Path $p.ModelFolder 'third-party-exporter-provenance.json') -Raw | ConvertFrom-Json
        $validator = Join-Path ([string]$exporter.folder) 'validate.py'
        if (-not (Test-Path -LiteralPath $validator -PathType Leaf)) { throw 'Pinned independent exporter validation script not found. Do not use an unpinned replacement.' }
        $python = Ensure-QwenAsrExportPython
        $decodeNote = if ($ext -eq '.ogg') { 'The first 30 seconds will be decoded with FFmpeg as temporary 16 kHz mono PCM WAV, then deleted.' } else { 'The original WAV will be passed directly to the validator.' }
        $message = ('Run original Qwen3-ASR ' + $p.Size + ' PyTorch vs FP32 ONNX on Windows CPU?' + "`r`n`r`n" +
            $decodeNote + "`r`n" + 'May require substantial RAM and several minutes. No model binaries or source audio will be altered. Audio: ' + $audio)
        if ([System.Windows.Forms.MessageBox]::Show($message,'Confirm long CPU validation',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question) -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $validatorAudio = $audio
        $temporaryWav = $null
        $conversion = 'none_original_wav'
        $temporaryValidator = $null
        try {
            # The pinned third-party validator expects embed_tokens_shape in
            # config.json, but this export omits it. Derive and verify the shape
            # from the audited decoder and original FP32 embedding byte count.
            # Patch only a transient sibling of validate.py, never the export.
            $candidateValidator = Join-Path ([string]$exporter.folder) ('validate-wr-compat-' + [guid]::NewGuid().ToString('N') + '.py')
            $compatReceipt = Join-Path $p.OutDir 'validator-embedding-compat.json'
            $prep = Invoke-ProcessCapture -FilePath (Get-LocalBuilderPython) -Arguments @($script:QwenAsrStageEPath,'validator-compat','--size',$p.Size,'--audit',$p.Audit,'--onnx-dir',$p.Onnx,'--upstream-validator',$validator,'--compat-validator',$candidateValidator,'--output',$compatReceipt) -TimeoutSeconds 120
            if ($prep.ExitCode -ne 0) { throw ('Stage E1 embedding metadata preflight failed; no model files modified: ' + $prep.StdErr + $prep.StdOut) }
            $compatDetails = Get-Content -LiteralPath $compatReceipt -Raw | ConvertFrom-Json
            if ($compatDetails.patched_temporary_copy) {
                $temporaryValidator = $candidateValidator
                $validator = $candidateValidator
                Append-Log ('[Qwen3 ASR] Stage E1: verified raw FP32 embedding shape and created temporary validator compatibility copy. Original config/weights untouched: ' + $compatReceipt)
            }
            if ($ext -eq '.ogg') {
                $ffmpeg = [string]$txtFfmpeg.Text
                if ([string]::IsNullOrWhiteSpace($ffmpeg) -or -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf)) { throw 'FFmpeg is required for OGG audio. Install/repair FFmpeg in Setup, then retry Stage E1.' }
                # Never modify the user's source recording. Use a unique throwaway WAV.
                $temporaryWav = Join-Path $p.OutDir ('qwen-stage-e1-' + [guid]::NewGuid().ToString('N') + '.wav')
                $args = @('-nostdin','-hide_banner','-loglevel','error','-y','-i',$audio,'-map','0:a:0','-t','30','-ac','1','-ar','16000','-c:a','pcm_s16le',$temporaryWav)
                $dec = Invoke-ProcessCapture -FilePath $ffmpeg -Arguments $args -TimeoutSeconds 600
                if ($dec.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $temporaryWav -PathType Leaf)) {
                    throw ('FFmpeg could not decode the selected OGG recording: ' + $dec.StdErr + $dec.StdOut)
                }
                if ((Get-Item -LiteralPath $temporaryWav).Length -le 44) { throw 'FFmpeg decoded no audio samples from the selected OGG.' }
                $validatorAudio = $temporaryWav
                $conversion = 'ffmpeg_ogg_first_30s_pcm_s16le_16000hz_mono'
                Append-Log '[Qwen3 ASR] Stage E1: OGG first 30s decoded to temporary 16 kHz mono WAV; original audio unchanged.'
            }
            $r = Invoke-ProcessCapture -FilePath $python -Arguments @($validator,'--onnx-dir',$p.Onnx,'--audio',$validatorAudio,'--model',$sourceFolder,'--device','cpu') -TimeoutSeconds 21600 -Environment @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'PYTHONUNBUFFERED'='1' }
            $log = Join-Path $p.OutDir 'pytorch-vs-onnx-validator.log'
            $utf8 = New-Object System.Text.UTF8Encoding -ArgumentList $false
            [System.IO.File]::WriteAllText($log, ($r.StdOut + "`r`n" + $r.StdErr), $utf8)
            $report = Join-Path $p.OutDir 'pytorch-vs-onnx-parity.json'
            $basePython = Get-LocalBuilderPython
            $c = Invoke-ProcessCapture -FilePath $basePython -Arguments @($script:QwenAsrStageEPath,'parity-receipt','--size',$p.Size,'--audit',$p.Audit,'--audio',$validatorAudio,'--source-audio',$audio,'--audio-preparation',$conversion,'--log',$log,'--exit-code',([string]$r.ExitCode),'--output',$report) -TimeoutSeconds 90
            if ($c.ExitCode -notin @(0,2) -or -not (Test-Path -LiteralPath $report -PathType Leaf)) { throw ('Stage E1 could not generate a parity receipt: ' + $c.StdErr + $c.StdOut) }
            $data = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            if ($data.status -eq 'exact_pytorch_onnx_token_parity_pass') {
                Set-Status $lblQwenAsrParity 'Exact token parity on selected audio' 'good'
            } else {
                Set-Status $lblQwenAsrParity ($data.status + ' - review log') 'warn'
            }
            Append-Log ('[Qwen3 ASR] Stage E1 parity receipt: ' + $report)
            if ($r.ExitCode -ne 0) { throw ('Stage E1 original/ONNX validation process failed. Read: ' + $log) }
            if ($c.ExitCode -ne 0) { throw ('Stage E1 parity was not established. Review: ' + $report) }
        } finally {
            if ($temporaryValidator -and (Test-Path -LiteralPath $temporaryValidator -PathType Leaf)) {
                Remove-Item -LiteralPath $temporaryValidator -Force -ErrorAction SilentlyContinue
                if (Test-Path -LiteralPath $temporaryValidator -PathType Leaf) {
                    Append-Log ('WARNING: temporary Stage E1 validator copy was not removed: ' + $temporaryValidator)
                } else {
                    Append-Log '[Qwen3 ASR] Temporary Stage E1 validator compatibility copy deleted.'
                }
            }
            if ($temporaryWav -and (Test-Path -LiteralPath $temporaryWav -PathType Leaf)) {
                Remove-Item -LiteralPath $temporaryWav -Force -ErrorAction SilentlyContinue
                if (Test-Path -LiteralPath $temporaryWav -PathType Leaf) {
                    Append-Log ('WARNING: temporary Stage E1 WAV was not removed; delete it manually: ' + $temporaryWav)
                } else {
                    Append-Log '[Qwen3 ASR] Temporary Stage E1 WAV deleted.'
                }
            }
        }
    }
})
$btnQwenAsrShape.Add_Click({
    Invoke-UiAction 'Planning fixed Qwen3-ASR tensor and KV-cache shapes...' {
        $p = Get-QwenStageEData
        $melFrames = [int]$txtQwenMelFrames.Text
        $prefill = [int]$txtQwenPrefill.Text
        $past = [int]$txtQwenPast.Text
        $report = Join-Path $p.OutDir 'shape-profile.json'
        $py = Get-LocalBuilderPython
        $r = Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageEPath,'shape-plan','--size',$p.Size,'--audit',$p.Audit,'--mel-frames',([string]$melFrames),'--prefill-tokens',([string]$prefill),'--past-tokens',([string]$past),'--output',$report) -TimeoutSeconds 120
        if ($r.ExitCode -ne 0) { throw ('Qwen3 shape planning failed: ' + $r.StdErr + $r.StdOut) }
        Set-Status $lblQwenAsrShape 'Shape/KV plan saved - NOT compiled' 'warn'
        Append-Log ('[Qwen3 ASR] Stage E2: ' + $report)
    }
})
$btnQwenAsrOps.Add_Click({
    Invoke-UiAction 'Screening Qwen3-ASR graphs for Qualcomm HTP compatibility risks...' {
        $p = Get-QwenStageEData
        $report = Join-Path $p.OutDir 'operator-compatibility-screen.json'
        $r = Invoke-ProcessCapture -FilePath (Get-LocalBuilderPython) -Arguments @($script:QwenAsrStageEPath,'ops-audit','--size',$p.Size,'--audit',$p.Audit,'--output',$report) -TimeoutSeconds 120
        if ($r.ExitCode -ne 0) { throw ('Qwen3 graph risk screening failed: ' + $r.StdErr + $r.StdOut) }
        Set-Status $lblQwenAsrOps 'Graph risks captured - QNN NOT proven' 'warn'
        Append-Log ('[Qwen3 ASR] Stage E3: ' + $report)
    }
})
$btnQwenAsrCandidate.Add_Click({
    Invoke-UiAction 'Preparing Qwen3-ASR SM8850 fixed-mel encoder candidate...' {
        $p = Get-QwenStageEData
        $melFrames = [int]$txtQwenMelFrames.Text
        $profile = Join-Path $p.OutDir 'shape-profile.json'
        if (-not (Test-Path -LiteralPath $profile -PathType Leaf)) { throw 'Run Stage E2 shape planning first.' }
        $parity = Join-Path $p.OutDir 'pytorch-vs-onnx-parity.json'
        $parityGood = $false
        if (Test-Path -LiteralPath $parity -PathType Leaf) {
            $parityData = Get-Content -LiteralPath $parity -Raw | ConvertFrom-Json
            $parityGood = $parityData.status -eq 'exact_pytorch_onnx_token_parity_pass'
        }
        $extraArgs = @()
        if (-not $parityGood) {
            $answer = [System.Windows.Forms.MessageBox]::Show('PyTorch-versus-ONNX exact token parity is NOT established. Prepare an encoder candidate for investigation anyway? This is NOT an accuracy-qualified NPU model.', 'Unvalidated candidate only', [System.Windows.Forms.MessageBoxButtons]::YesNo, [System.Windows.Forms.MessageBoxIcon]::Warning)
            if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
            $extraArgs += '--allow-unvalidated'
        }
        if ([System.Windows.Forms.MessageBox]::Show(('Create a separate large fixed-input encoder ONNX candidate for SM8850? Source ONNX remains unchanged. Mel frames: ' + $melFrames + '. Extra disk: approx. original encoder size.'),'Prepare Qwen encoder candidate',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question) -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $report = Join-Path $p.OutDir 'encoder-aihub-preparation.json'
        $python = Ensure-QwenAsrExportPython
        $candidateArgs = @($script:QwenAsrStageEPath,'prepare-encoder','--size',$p.Size,'--audit',$p.Audit,'--onnx-dir',$p.Onnx,'--mel-frames',([string]$melFrames),'--parity-receipt',$parity,'--output',$report) + $extraArgs
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $candidateArgs -TimeoutSeconds 3600 -Environment @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8' }
        if ($r.ExitCode -ne 0) { throw ('Qwen encoder handoff preparation failed: ' + $r.StdErr + $r.StdOut) }
        Set-Status $lblQwenAsrCandidate 'Separate encoder candidate ready - QNN NOT compiled' 'warn'
        Append-Log ('[Qwen3 ASR] Stage E4: ' + $report)
    }
})
$btnQwenAsrStageEFolder.Add_Click({
    try { $p = Get-QwenStageEData; Open-Folder $p.OutDir } catch { Show-Error $_ }
})

# ---------------- Qwen3-ASR Stage F: Qualcomm-documented QNN context baseline ----------------
$tabQwenAsr.Controls.Add((New-Label 'Stage F - Qualcomm SM8850/V81 ENCODER test (0.6B only; QNN, not full transcription)' 25 2050 845 40 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Verify compiled QAIRT 2.50 vs native runner 2.51 compatibility; exact Galaxy S26 Workbench target required. FP16 HTP, single encoder, mel [1,128,1200], no INT8 or CPU fallback. All work stays separate from Omi / Parakeet.' 25 2096 844 71))
$btnQwenStageFPreflight = New-Button 'F1 - Verify fixed encoder vs original FP32 ONNX' 25 2186 428 42
$tabQwenAsr.Controls.Add($btnQwenStageFPreflight)
$lblQwenStageFPreflight = New-StatusLabel 473 2195 356
$tabQwenAsr.Controls.Add($lblQwenStageFPreflight)
$btnQwenStageFProbe = New-Button 'F2 - Check Galaxy S26 + QAIRT 2.50 in AI Hub' 25 2250 428 42
$tabQwenAsr.Controls.Add($btnQwenStageFProbe)
$lblQwenStageFProbe = New-StatusLabel 473 2259 356
$tabQwenAsr.Controls.Add($lblQwenStageFProbe)
$btnQwenStageFCompile = New-Button 'F3 - Compile + link encoder on Qualcomm AI Hub' 25 2314 428 42
$tabQwenAsr.Controls.Add($btnQwenStageFCompile)
$lblQwenStageFCompile = New-StatusLabel 473 2323 356
$tabQwenAsr.Controls.Add($lblQwenStageFCompile)
$tabQwenAsr.Controls.Add((New-Label 'F3 submits about 752 MB to Qualcomm Workbench. It requires your AI Hub authentication and can take a long time. It never recompiles the decoder or overwrites an existing QNN binary.' 25 2373 840 59))
$btnQwenStageFDevice = New-Button 'F4 - Run encoder on S26 NPU + clear phone files' 25 2451 428 42
$tabQwenAsr.Controls.Add($btnQwenStageFDevice)
$lblQwenStageFDevice = New-StatusLabel 473 2460 356
$tabQwenAsr.Controls.Add($lblQwenStageFDevice)
$btnQwenStageFOpen = New-Button 'Open Stage F results folder' 25 2517 428 42
$tabQwenAsr.Controls.Add($btnQwenStageFOpen)
# Independent recovery control on the Qwen tab. Does NOT need the ONNX export,
# downloaded QNN context, cloud login, Python, or a successful F4 result.
$btnQwenStageFClear = New-Button 'Clear previous S26 test files (manual)' 25 2580 428 42
$tabQwenAsr.Controls.Add($btnQwenStageFClear)
$lblQwenStageFClear = New-StatusLabel 473 2589 356
$tabQwenAsr.Controls.Add($lblQwenStageFClear)
$tabQwenAsr.Controls.Add((New-Label 'F1 uses deterministic synthetic mel for graph equivalence. F4 uses the SAME input on Hexagon V81; outputs are compared numerically against CPU FP32. Real-audio transcription and Qwen decoder are NOT validated here.' 25 2647 835 71))

# Single authoritative Stage F2 target-probe location shared by F2/F3 and
# optional J17 evidence. Get-QwenStageEData returns ModelFolder, NOT Folder.
# J17 must not call Get-QwenStageFData because that requires an encoder E4
# receipt even when a decoder-only read-only audit is otherwise valid.
function Get-QwenStageFProbePath {
    param([Parameter(Mandatory=$true)][string]$ModelFolder)
    if ([string]::IsNullOrWhiteSpace($ModelFolder)) {
        throw 'Stage F probe lookup requires the Qwen model folder.'
    }
    return (Join-Path (Join-Path $ModelFolder 'stage-f-sm8850-mel1200') 'qwen-aihub-target-probe.json')
}

function Get-QwenStageFData {
    $p = Get-QwenStageEData
    if ($p.Size -ne '0.6B') { throw 'Stage F is 0.6B encoder only. Choose 0.6B first. 1.7B must be validated separately.' }
    $e4 = Join-Path $p.OutDir 'encoder-aihub-preparation.json'
    if (-not (Test-Path -LiteralPath $e4 -PathType Leaf)) { throw 'Stage E4 encoder candidate receipt missing. Run E4 first.' }
    $e4Data = Get-Content -LiteralPath $e4 -Raw | ConvertFrom-Json
    if ([string]$e4Data.status -ne 'encoder_fixed_input_candidate_created_not_qnn_compiled' -or [string]$e4Data.variant -ne '0.6B') {
        throw 'Stage E4 is not a successful Qwen 0.6B candidate receipt.'
    }
    if (@($e4Data.input_contract.mel) -join ',' -ne '1,128,1200') { throw 'Stage F baseline requires exact mel [1,128,1200]. Stage E4 used a different length; rebuild an explicitly chosen test candidate.' }
    $candidate = [string]$e4Data.encoder_candidate
    if ([string]::IsNullOrWhiteSpace($candidate) -or -not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw 'Existing 0.6B fixed-input encoder ONNX not found; keep your Stage E4 workspace.'
    }
    $folder = Join-Path $p.ModelFolder 'stage-f-sm8850-mel1200'
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    return @{ Folder=$folder; Candidate=$candidate; E4=$e4; ModelFolder=$p.ModelFolder }
}

$btnQwenStageFPreflight.Add_Click({
    Invoke-UiAction 'Qwen F1 CPU FP32 specialized encoder correctness preflight...' {
        $p=Get-QwenStageFData
        $f1=Join-Path $p.Folder 'encoder-fp32-candidate-parity.json'
        if (Test-Path -LiteralPath $f1 -PathType Leaf) {
            $prev = Get-Content -LiteralPath $f1 -Raw | ConvertFrom-Json
            if ($prev.status -eq 'fp32_candidate_numerical_parity_pass' -and
                -not [string]::IsNullOrWhiteSpace([string]$prev.input_raw) -and
                -not [string]::IsNullOrWhiteSpace([string]$prev.reference_npy) -and
                (Test-Path -LiteralPath ([string]$prev.input_raw) -PathType Leaf) -and
                (Test-Path -LiteralPath ([string]$prev.reference_npy) -PathType Leaf)) {
                Set-Status $lblQwenStageFPreflight 'Existing F1 PASS; reuse tested inputs' 'good'
                Append-Log ('[Qwen F1] Reusing completed FP32 reference; F3/F4 verify SHA256: '+$f1)
                return
            }
        }
        $stale=@('encoder-fp32-candidate-parity.json','encoder-test-mel-fp32.raw','encoder-fp32-reference.npy') | ForEach-Object { Join-Path $p.Folder $_ } | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf }
        if (@($stale).Count -gt 0) {
            if ([System.Windows.Forms.MessageBox]::Show('A previous FP32 preflight was interrupted or did not pass. Archive its small test files into a timestamped backup, then retry? Original ONNX exports and the 752 MB candidate will NOT be touched.',
                    'Archive incomplete Stage F1 preflight',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question) -ne [System.Windows.Forms.DialogResult]::Yes) { return }
            $backup=Join-Path $p.Folder ('f1-incomplete-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
            New-Item -ItemType Directory -Path $backup -Force | Out-Null
            foreach ($file in $stale) { Move-Item -LiteralPath $file -Destination $backup -ErrorAction Stop }
            Append-Log ('[Qwen F1] Archived failed/incomplete preflight to '+$backup)
        }
        $py=Ensure-QwenAsrExportPython
        $r=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageFPath,'preflight','--e4',$p.E4,'--candidate',$p.Candidate,'--out',$p.Folder) -Environment @{ 'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8' } -TimeoutSeconds 5400
        if ($r.ExitCode -ne 0) { throw ('Qwen F1 fixed-shape FP32 check failed: '+$r.StdErr + $r.StdOut) }
        Set-Status $lblQwenStageFPreflight 'Same-input FP32 ONNX equivalence PASS' 'good'
        Append-Log ('[Qwen F1] '+$f1)
    }
})
$btnQwenStageFProbe.Add_Click({
    Invoke-UiAction 'Checking exact Qualcomm Workbench Galaxy S26 target and QAIRT version...' {
        $p=Get-QwenStageFData
        Register-SensitiveOutputValue $txtToken.Text
        $py=Ensure-AiHubEnvironmentFromUi
        $report=Get-QwenStageFProbePath -ModelFolder $p.ModelFolder
        $r=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageFPath,'probe','--report',$report) -Environment @{ 'QAI_HUB_API_TOKEN'=[string]$txtToken.Text; 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8' } -TimeoutSeconds 180
        if ($r.ExitCode -ne 0) { throw ('Qwen F2 Workbench device/version validation failed (no compile submitted): '+$r.StdErr + $r.StdOut) }
        Set-Status $lblQwenStageFProbe 'Exact S26 / QAIRT 2.50 verified' 'good'
        Append-Log ('[Qwen F2] '+$report)
    }
})
$btnQwenStageFCompile.Add_Click({
    Invoke-UiAction 'Submitting Qwen 0.6B encoder FP16 compile + link to Qualcomm AI Hub...' {
        $p=Get-QwenStageFData
        $f1=Join-Path $p.Folder 'encoder-fp32-candidate-parity.json'
        $f2=Get-QwenStageFProbePath -ModelFolder $p.ModelFolder
        if (-not (Test-Path -LiteralPath $f1 -PathType Leaf) -or -not (Test-Path -LiteralPath $f2 -PathType Leaf)) { throw 'Complete Stage F1 and F2 before submitting.' }
        $known=@(Get-ChildItem -LiteralPath $p.Folder -Filter 'qwen-encoder-cloud-compile.json' -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
            try { (Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json).status -eq 'qnn_context_compiled_device_execution_pending' } catch { $false }
        })
        if ($known.Count -gt 0) { throw 'Successful encoder compile already exists. Do not submit or pay for a duplicate; proceed to F4.' }
        if ([System.Windows.Forms.MessageBox]::Show(('Submit the verified Qwen3-ASR 0.6B encoder (~752 MB) to Qualcomm AI Hub now?' + "`r`n`r`n" +
             'TARGET: Samsung Galaxy S26 (no fallback)' + "`r`n" + 'QAIRT: 2.50 (must be supported)' + "`r`n" +
             'QNN HTP FP16; optimization O=3; no INT8; mel [1,128,1200]' + "`r`n`r`n" +
             'Cloud charges/quotas and significant upload time may apply. Encoder ONLY; not yet an Android model.'),
             'Authorize paid/cloud encoder compilation',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question) -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        Register-SensitiveOutputValue $txtToken.Text
        $py=Ensure-AiHubEnvironmentFromUi
        $runFolder=Join-Path $p.Folder ('compile-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $runFolder -Force | Out-Null
        $bin=Join-Path $runFolder 'qwen3-asr-0.6b-encoder-sm8850-v81-fp16.bin'
        $receipt=Join-Path $runFolder 'qwen-encoder-cloud-compile.json'
        $r=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageFPath,'compile','--e4',$p.E4,'--candidate',$p.Candidate,'--f1',$f1,'--probe',$f2,'--binary',$bin,'--report',$receipt) -Environment @{ 'QAI_HUB_API_TOKEN'=[string]$txtToken.Text; 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8' } -TimeoutSeconds 9000
        if ($r.ExitCode -ne 0) { throw ('Qwen F3 Qualcomm compile/link FAILED; see receipt '+$receipt+': '+$r.StdErr + $r.StdOut) }
        Set-Status $lblQwenStageFCompile 'Compiled context ready; F4 device test needed' 'warn'
        Append-Log ('[Qwen F3] '+$receipt)
    }
})
$btnQwenStageFDevice.Add_Click({
    Invoke-UiAction 'Testing Qwen 0.6B encoder context on Galaxy S26 Hexagon and cleaning files...' {
        $p=Get-QwenStageFData
        $f1=Join-Path $p.Folder 'encoder-fp32-candidate-parity.json'
        $files=@(Get-ChildItem -LiteralPath $p.Folder -Filter 'qwen-encoder-cloud-compile.json' -Recurse -File | Sort-Object LastWriteTime -Descending)
        $cloud=''
        foreach ($file in $files) {
            try { $info=Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json; if ($info.status -eq 'qnn_context_compiled_device_execution_pending') { $cloud=$file.FullName; break } } catch { }
        }
        if (-not $cloud) { throw 'Stage F3 compiled QNN context not found. Run Qualcomm cloud compile first.' }
        $runtime=Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Set the existing QNN Android Runtime folder in Deploy & Test first. Missing: '+($runtime.Missing -join ', ')) }
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Select your authorized Samsung S26 in Deploy & Test first.' }
        $device=Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'Wrong ADB phone selected; expected Samsung SM-S942U.' }
        $python=Ensure-OmiTdtTranscriptEnvironmentFromUi
        $runFolder=Join-Path $p.Folder ('s26-test-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $runFolder -Force | Out-Null
        $manifest=Join-Path $runFolder 'qnn-runtime-local-paths.json'
        $bundle=[ordered]@{}
        foreach ($entry in $runtime.Files.GetEnumerator()) { $bundle[[string]$entry.Key]=[string]$entry.Value }
        $utf8NoBom=New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
        [IO.File]::WriteAllText($manifest,($bundle | ConvertTo-Json -Depth 4),$utf8NoBom)
        $verifiedClean=$false
        # Independent GUI-level cleanup runs even if the Python test fails or times out.
        # Save a separate receipt; it must not be confused with encoder numerical PASS.
        $cleanupReceipt=Join-Path $runFolder 'qwen-f4-gui-phone-cleanup.json'
        $cleanupEvidence=[ordered]@{ schema=1; test='qwen_f4'; device_model=[string]$device.Model; verified_clean=$false; removed_count=0; freed_mib=0; error='not_attempted'; scope='Builder-owned /data/local/tmp temporary staging only' }
        try {
            $r=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageFPath,'device','--f1',$f1,'--compile-receipt',$cloud,'--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$manifest,'--out',$runFolder) -Environment @{ 'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8' } -TimeoutSeconds 1800
        } finally {
            try {
                $cleanup=Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                $verifiedClean=[bool]$cleanup.VerifiedClean
                $cleanupEvidence.verified_clean=$verifiedClean
                $cleanupEvidence.removed_count=[int]$cleanup.RemovedCount
                $cleanupEvidence.freed_mib=[double]$cleanup.FreedMiB
                $cleanupEvidence.error=''
                Append-Log ('[Qwen F4] Independent S26 cleanup scan verified: '+[string]$verifiedClean+'; additional folders removed: '+[string]$cleanup.RemovedCount)
                if ($verifiedClean) { Set-Status $lblQwenStageFClear 'Phone test files: verified clear' 'good' }
            } catch {
                $cleanupEvidence.error=$_.Exception.Message
                Set-Status $lblQwenStageFClear 'Phone cleanup unverified - use manual button' 'bad'
                Append-Log ('[Qwen F4] Cleanup failed, click Clear previous S26 test files: '+$_.Exception.Message)
            } finally {
                $utf8NoBom=New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
                [IO.File]::WriteAllText($cleanupReceipt,($cleanupEvidence | ConvertTo-Json -Depth 5),$utf8NoBom)
            }
        }
        if (-not $verifiedClean) { throw ('S26 temp file cleanup could not be verified. Reconnect and use Clear previous S26 test files. F4 cannot qualify. Receipt: '+$cleanupReceipt) }
        if ($r.ExitCode -ne 0) {
            Set-Status $lblQwenStageFDevice 'Encoder/device check FAIL; phone files cleared' 'bad'
            throw ('Qwen F4 encoder-only device test failed (phone cleanup VERIFIED). See '+$runFolder+': '+$r.StdErr + $r.StdOut)
        }
        Set-Status $lblQwenStageFDevice 'S26 encoder test PASS + phone cleanup verified' 'good'
        Append-Log ('[Qwen F4] Device report: '+(Join-Path $runFolder 'qwen-s26-encoder-device-test.json'))
    }
})
$btnQwenStageFOpen.Add_Click({
    try { $p=Get-QwenStageFData; Open-Folder $p.Folder } catch { Show-Error $_ }
})



# -------------------- Qwen3-ASR Stage G: one-click hybrid transcription + packaging --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage G - REAL speech hybrid -> Android handoff -> Hugging Face (0.6B)' 25 2745 845 30 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'ONE action runs 12 s of .ogg/.wav through S26 FP16 QNN encoder + Windows FP32 ONNX decoder, checks token parity vs original FP32, verifies phone cleanup, then stages decoder, compiled encoder, manifest, checksums and Android spec. No new cloud compile.' 25 2784 843 74))
$btnQwenStageGAll = New-Button 'G1 - Real-speech parity + prepare HF/Android package' 25 2876 462 44
$tabQwenAsr.Controls.Add($btnQwenStageGAll)
$lblQwenStageGAll = New-StatusLabel 500 2886 340
$tabQwenAsr.Controls.Add($lblQwenStageGAll)
$tabQwenAsr.Controls.Add((New-Label 'Hugging Face model repo (namespace/model; short name or legacy Stage G name)' 25 2948 830 28))
$txtQwenStageGRepo = New-TextBox 25 2981 660 28
$txtQwenStageGRepo.Text = 'drinnird/qwen3-asr-0.6b-snapdragon-sm8850-v81'
$tabQwenAsr.Controls.Add($txtQwenStageGRepo)
$tabQwenAsr.Controls.Add((New-Label 'Repository visibility' 25 3020 170 25 -Bold))
$cmbQwenStageGVisibility = New-Object System.Windows.Forms.ComboBox
$cmbQwenStageGVisibility.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(205,3017)
$cmbQwenStageGVisibility.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(145,29)
$cmbQwenStageGVisibility.DropDownStyle = 'DropDownList'
[void]$cmbQwenStageGVisibility.Items.Add('private')
[void]$cmbQwenStageGVisibility.Items.Add('public')
$cmbQwenStageGVisibility.SelectedItem = 'private'
$tabQwenAsr.Controls.Add($cmbQwenStageGVisibility)
$tabQwenAsr.Controls.Add((New-Label 'PUBLIC = anyone can download the experimental model. Choose deliberately.' 367 3018 470 31))
$tabQwenAsr.Controls.Add((New-Label 'Hugging Face WRITE token (required for G2 / G3; never saved)' 25 3061 695 25 -Bold))
$txtQwenStageGToken = New-TextBox 25 3091 525 29 $true $false
$tabQwenAsr.Controls.Add($txtQwenStageGToken)
$btnQwenStageGGetToken = New-Button 'Get HF token' 561 3089 135 32
$tabQwenAsr.Controls.Add($btnQwenStageGGetToken)
$btnQwenStageGGetToken.Add_Click({ try { Open-Url 'https://huggingface.co/settings/tokens' } catch { Show-Error $_ } })
$btnQwenStageGClearToken = New-Button 'Clear token' 704 3089 126 32
$tabQwenAsr.Controls.Add($btnQwenStageGClearToken)
$btnQwenStageGClearToken.Add_Click({ $txtQwenStageGToken.Clear() })
$tabQwenAsr.Controls.Add((New-Label 'Masked token stays local and is never saved to settings. G2 creates/uploads; G3 changes visibility WITHOUT reuploading. Never send tokens to chat/support.' 25 3125 838 45))
$btnQwenStageGPublish = New-Button 'G2 - Upload verified EXPERIMENTAL package' 25 3183 462 44
$tabQwenAsr.Controls.Add($btnQwenStageGPublish)
$lblQwenStageGPublish = New-StatusLabel 500 3192 345
$tabQwenAsr.Controls.Add($lblQwenStageGPublish)
$btnQwenStageGVisibility = New-Button 'G3 - Change existing uploaded repo visibility' 25 3244 462 44
$tabQwenAsr.Controls.Add($btnQwenStageGVisibility)
$lblQwenStageGVisibility = New-StatusLabel 500 3253 345
$tabQwenAsr.Controls.Add($lblQwenStageGVisibility)
$btnQwenStageGOpen = New-Button 'Open Stage G results / Android instructions' 25 3306 462 43
$tabQwenAsr.Controls.Add($btnQwenStageGOpen)
$tabQwenAsr.Controls.Add((New-Label 'Privacy: test transcripts and original recordings stay LOCAL. Public release occurs ONLY after upload verification and explicit approval. Existing populated repositories are never overwritten. Experimental HYBRID only: QNN decoder, Android EPContext and long recordings remain unvalidated. Manual S26 cleanup is above.' 25 3358 842 93))

function Get-QwenStageGData {
    $p=Get-QwenStageFData
    $e=Get-QwenStageEData
    $f4=''
    foreach ($file in @(Get-ChildItem -LiteralPath $p.Folder -Filter 'qwen-s26-encoder-device-test.json' -Recurse -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $info=Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
            $guiClean=Join-Path $file.DirectoryName 'qwen-f4-gui-phone-cleanup.json'
            if ($info.status -eq 'encoder_device_provisional_numeric_pass' -and [bool]$info.device_files_removed -and
                (Test-Path -LiteralPath $guiClean -PathType Leaf) -and
                [bool]((Get-Content -LiteralPath $guiClean -Raw | ConvertFrom-Json).verified_clean)) {
                $f4=$file.FullName; break
            }
        } catch { }
    }
    if (-not $f4) { throw 'Need a successful, cleanup-verified S26 Stage F4 encoder test first. Do not recompile; run F4.' }
    $cloud=''
    foreach ($file in @(Get-ChildItem -LiteralPath $p.Folder -Filter 'qwen-encoder-cloud-compile.json' -Recurse -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $entry=Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
            $f4data=Get-Content -LiteralPath $f4 -Raw | ConvertFrom-Json
            if ($entry.status -eq 'qnn_context_compiled_device_execution_pending' -and $entry.context_sha256 -eq $f4data.compile_context_sha256) {
                $cloud=$file.FullName; break
            }
        } catch { }
    }
    if (-not $cloud) { throw 'Cannot find original Stage F3 compile receipt for the encoder verified in F4; preserve the existing context.' }
    $exporterReceipt=Join-Path $p.ModelFolder 'third-party-exporter-provenance.json'
    if (-not (Test-Path -LiteralPath $exporterReceipt -PathType Leaf)) { throw 'Stage C pinned exporter provenance receipt missing.' }
    $exporter=Get-Content -LiteralPath $exporterReceipt -Raw | ConvertFrom-Json
    if ($exporter.exporter_commit -ne '4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50') { throw 'Existing Qwen ONNX exporter revision differs from verified FP32 export.' }
    $folder=Join-Path $p.ModelFolder 'stage-g-hybrid-deployment'
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    return @{ F4=$f4; Cloud=$cloud; Exporter=[string]$exporter.folder; ExporterReceipt=$exporterReceipt; Onnx=$e.Onnx; Folder=$folder }
}

$btnQwenStageGAll.Add_Click({
    Invoke-UiAction 'Stage G: full hybrid real-speech parity, phone cleanup and experimental package preparation...' {
        $p=Get-QwenStageGData
        $audio=([string]$txtQwenAsrAudio.Text).Trim()
        if (-not $audio -or -not (Test-Path -LiteralPath $audio -PathType Leaf) -or [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.ogg','.wav')) {
            throw 'Select the actual real-speech .ogg or .wav recording using the Stage E audio picker above.'
        }
        $ffmpeg=([string]$txtFfmpeg.Text).Trim()
        if (-not $ffmpeg -or -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf)) { throw 'FFmpeg executable missing; select the existing FFmpeg in Builder Setup.' }
        # Never repeat a completed multi-GB package/test merely because G1 was clicked
        # again with the same audio and compiled encoder. A different audio can be
        # selected for an additional validation run without destroying the first.
        $audioHash=(Get-FileHash -LiteralPath $audio -Algorithm SHA256).Hash.ToLowerInvariant()
        $cloudData=Get-Content -LiteralPath $p.Cloud -Raw | ConvertFrom-Json
        $existingPackages=@(Get-ChildItem -LiteralPath $p.Folder -Directory -Filter 'experimental-hybrid-release-*' -ErrorAction SilentlyContinue | Where-Object {
            $manifestFile=Join-Path $_.FullName 'model-manifest.json'
            try { (Test-Path -LiteralPath $manifestFile -PathType Leaf) -and
                  (Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json).status -eq 'experimental_hybrid_real_speech_pass_android_pending' }
            catch { $false }
        })
        if ($existingPackages.Count -gt 0) {
            foreach ($existingRun in @(Get-ChildItem -LiteralPath $p.Folder -Directory -Filter 'real-speech-*' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                $receiptPath=Join-Path $existingRun.FullName 'qwen-real-speech-hybrid-validation.json'
                $cleanupPath=Join-Path $existingRun.FullName 'qwen-g1-gui-phone-cleanup.json'
                if (-not (Test-Path -LiteralPath $receiptPath -PathType Leaf) -or -not (Test-Path -LiteralPath $cleanupPath -PathType Leaf)) { continue }
                try {
                    $old=Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
                    $clean=Get-Content -LiteralPath $cleanupPath -Raw | ConvertFrom-Json
                    if ($old.status -eq 's26_encoder_cpu_decoder_real_speech_exact_token_pass' -and
                        [string]$old.audio_source_sha256 -eq $audioHash -and
                        [string]$old.compile_context_sha256 -eq [string]$cloudData.context_sha256 -and
                        [bool]$clean.verified_clean) {
                        Set-Status $lblQwenStageGAll 'Prior real-speech PASS / package reused' 'good'
                        Append-Log ('[Qwen G1] Reusing successful S26 hybrid test on identical audio; no repeat phone transfer or multi-GB release package. Folder: '+$existingRun.FullName)
                        Append-Log ('[Qwen G1] Existing package: '+$existingPackages[0].FullName+'; G2 independently re-audits every file before upload.')
                        return
                    }
                } catch { }
            }
        }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            ('Use existing Qwen 0.6B QNN encoder and original FP32 ONNX decoder with the FIRST 12 s of the selected audio?'+"`r`n`r`n"+
             'Only LOCAL real-speech test output is saved. No audio/transcripts will enter the model package or HF repo.'+"`r`n"+
             'On success, this ONE click stages multi-GB decoder weights (hardlinks where possible) and Android instructions.'+"`r`n"+
             'No Qualcomm cloud charges, no re-download or re-export. This may require considerable Windows RAM/disk space.'+"`r`n`r`n"+
             'Continue?'), 'Confirm full Qwen hybrid validation and packaging',
            [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Select an authorized SM-S942U in Deploy & Test first.' }
        $device=Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'Expected original Samsung S26 SM-S942U; refusing unexpected phone.' }
        $runtime=Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Existing QNN runtime bundle incomplete: '+($runtime.Missing -join ', ')) }
        $python=Ensure-QwenAsrExportPython
        $dep=Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import onnxruntime, tokenizers, numpy, soundfile, torch, librosa; print("Qwen hybrid dependencies ready")') -TimeoutSeconds 120
        if ($dep.ExitCode -ne 0) { throw ('Qwen hybrid CPU dependencies unavailable in existing export environment: '+$dep.StdErr) }
        $run=Join-Path $p.Folder ('real-speech-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $run -Force | Out-Null
        $manifest=Join-Path $run 'qnn-runtime-local-paths.json'
        $bundle=[ordered]@{}
        foreach ($entry in $runtime.Files.GetEnumerator()) { $bundle[[string]$entry.Key]=[string]$entry.Value }
        $utf8=New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
        [IO.File]::WriteAllText($manifest,($bundle | ConvertTo-Json -Depth 4),$utf8)
        $verifiedClean=$false
        try {
            $argsG=@($script:QwenAsrStageGPath,'real-speech','--audio',$audio,'--ffmpeg',$ffmpeg,'--onnx-dir',$p.Onnx,
                 '--exporter',$p.Exporter,'--exporter-receipt',$p.ExporterReceipt,'--f4',$p.F4,'--compile-receipt',$p.Cloud,
                 '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$manifest,'--out',$run)
            $tested=Invoke-ProcessCapture -FilePath $python -Arguments $argsG -Environment @{ 'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'PYTHONUNBUFFERED'='1' } -TimeoutSeconds 21600
        } finally {
            $cleanupReceipt=Join-Path $run 'qwen-g1-gui-phone-cleanup.json'
            $evidence=[ordered]@{ schema=1; test='qwen_g1_real_speech'; verified_clean=$false; removed_count=0; error='not_attempted'; scope='Builder-owned temporary S26 folders only' }
            try {
                $clean=Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                $verifiedClean=[bool]$clean.VerifiedClean
                $evidence.verified_clean=$verifiedClean
                $evidence.removed_count=[int]$clean.RemovedCount
                $evidence.error=''
                if ($verifiedClean) { Set-Status $lblQwenStageFClear 'Phone test files: verified clear' 'good' }
                Append-Log ('[Qwen G1] Independent S26 temp cleanup verified: '+$verifiedClean)
            } catch {
                $evidence.error=$_.Exception.Message
                Set-Status $lblQwenStageFClear 'Phone cleanup unverified - use manual button' 'bad'
                Append-Log ('[Qwen G1] Recovery: '+$evidence.error)
            } finally { [IO.File]::WriteAllText($cleanupReceipt,($evidence|ConvertTo-Json -Depth 5),$utf8) }
        }
        if (-not $verifiedClean) { throw ('Phone temporary files NOT confirmed removed. Use manual Clear S26 test files. Receipt: '+$cleanupReceipt) }
        if ($tested.ExitCode -ne 0) { throw ('Qwen G1 real-speech hybrid failed; see LOCAL report '+$run+': '+$tested.StdErr+$tested.StdOut) }
        $real=Join-Path $run 'qwen-real-speech-hybrid-validation.json'
        if (-not (Test-Path -LiteralPath $real -PathType Leaf) -or
            (Get-Content -LiteralPath $real -Raw | ConvertFrom-Json).status -ne 's26_encoder_cpu_decoder_real_speech_exact_token_pass') {
            throw 'Qwen G1 did not produce a verified hybrid exact-token-parity PASS receipt; refusing to package.'
        }
        Append-Log ('[Qwen G1] Real speech NPU encoder + CPU decoder EXACT token parity: '+$real)
        $release=Join-Path $p.Folder ('experimental-hybrid-release-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $packed=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageGPath,'package','--onnx-dir',$p.Onnx,
                 '--f4',$p.F4,'--compile-receipt',$p.Cloud,'--real-speech',$real,'--output-dir',$release) -Environment @{ 'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8' } -TimeoutSeconds 21600
        if ($packed.ExitCode -ne 0) { throw ('Qwen stage G package preparation did not pass; do not upload. '+$packed.StdErr+$packed.StdOut) }
        Set-Status $lblQwenStageGAll 'Hybrid token parity PASS; HF package ready' 'good'
        Append-Log ('[Qwen G1] Experimental package and Android handoff: '+$release)
        [System.Windows.Forms.MessageBox]::Show(
            ('Qwen S26 encoder + CPU decoder produced the same tokens as original FP32 on this recording.'+"`r`n"+
             'Phone temporary files verified cleared.'+"`r`n"+'Experimental package prepared at:'+"`r`n"+$release+"`r`n`r`n"+
             'The Android app has NOT yet been tested. G2 is optional and uploads only after a separate confirmation.'),
            'Stage G hybrid milestone reached', [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnQwenStageGPublish.Add_Click({
    Invoke-UiAction 'Publishing hash-audited EXPERIMENTAL Qwen hybrid assets to Hugging Face...' {
        $p=Get-QwenStageGData
        $repo=([string]$txtQwenStageGRepo.Text).Trim()
        if ($repo -notmatch '^[a-zA-Z0-9][\w.-]*/qwen3-asr-0\.6b-snapdragon-sm8850(-v81|-v81-hybrid-experimental|v81-fp16)$') {
            throw 'Choose a dedicated Qwen repo, preferably namespace/qwen3-asr-0.6b-snapdragon-sm8850-v81. Omi and Parakeet repositories remain protected.'
        }
        $visibility=[string]$cmbQwenStageGVisibility.SelectedItem
        if ($visibility -notin @('private','public')) { throw 'Select PRIVATE or PUBLIC repository visibility.' }
        $releases=@(Get-ChildItem -LiteralPath $p.Folder -Directory -Filter 'experimental-hybrid-release-*' | Sort-Object LastWriteTime -Descending | Where-Object {
            $manifestFile=Join-Path $_.FullName 'model-manifest.json'
            try { (Test-Path -LiteralPath $manifestFile -PathType Leaf) -and
                  (Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json).status -eq 'experimental_hybrid_real_speech_pass_android_pending' }
            catch { $false }
        })
        if ($releases.Count -eq 0) { throw 'Run G1 first; no verified real-speech Qwen hybrid package is available.' }
        $release=$releases[0].FullName
        $base=Get-LocalBuilderPython
        $verify=Invoke-ProcessCapture -FilePath $base -Arguments @($script:QwenAsrStageGPath,'audit','--folder',$release) -TimeoutSeconds 21600
        if ($verify.ExitCode -ne 0) { throw ('Local experimental model file hashes/manifest failed verification: '+$verify.StdErr+$verify.StdOut) }
        $warning = if ($visibility -eq 'public') { 'PUBLIC: anyone on the internet may download and redistribute the released model files.' } else { 'PRIVATE: only authorized account members can view the repository.' }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            ('Upload verified experimental Qwen hybrid model package to Hugging Face '+$visibility.ToUpperInvariant()+' repository?' + "`r`n`r`n"+
            $repo+"`r`n"+$warning+"`r`n"+
            'Uploads into a PRIVATE repo first; only after remote file verification is it made PUBLIC (if selected).' + "`r`n"+
            'FP16 S26 QNN encoder + FP32 CPU ONNX decoder, several GB.'+"`r`n"+
            'Never overwrites an occupied remote repo. No recordings, transcripts, or QNN runtime libraries included.'+"`r`n"+
            'Android integration is NOT verified. Upload quotas may apply. Continue?'),
            ('Confirm EXPERIMENTAL '+$visibility.ToUpperInvariant()+' Qwen HF upload'),
            [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $token=([string]$txtQwenStageGToken.Text).Trim()
        if ([string]::IsNullOrWhiteSpace($token)) { throw 'Paste a Hugging Face WRITE token into the masked Qwen Stage G token box. Tokens are not saved.' }
        Register-SensitiveOutputValue $token
        $envPrivate=@{ 'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'; 'WR_HF_TOKEN'=$token }
        $hfPython=Ensure-HuggingFaceUploaderPython
        $receipt=Join-Path $p.Folder 'qwen-hf-upload-receipt.json'
        $legacy=Join-Path $p.Folder 'qwen-private-hf-upload-receipt.json'
        if ((Test-Path -LiteralPath $receipt -PathType Leaf) -or (Test-Path -LiteralPath $legacy -PathType Leaf)) {
            throw 'Existing Qwen upload receipt found. Do not upload twice: use G3 to change visibility without reuploading.'
        }
        $uploaded=Invoke-ProcessCapture -FilePath $hfPython -Arguments @($script:QwenAsrStageGPath,'publish','--folder',$release,
                  '--repo-id',$repo,'--receipt',$receipt,'--visibility',$visibility) -Environment $envPrivate -TimeoutSeconds 21600 -DisplayCommand ('Qwen experimental Hugging Face '+$visibility+' upload (credentials hidden)')
        if ($uploaded.ExitCode -ne 0) { throw ('HF upload/visibility verification failed. If remote files were uploaded, DO NOT reupload; inspect receipt and use G3: '+$uploaded.StdErr+$uploaded.StdOut) }
        $receiptData=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ($receiptData.status -ne 'experimental_qwen_hybrid_hf_upload_remote_files_verified' -or $receiptData.visibility -ne $visibility -or $receiptData.repo_id -ne $repo) {
            throw 'Missing confirmed Qwen HF upload/visibility receipt.'
        }
        $txtQwenStageGToken.Clear()
        Set-Status $lblQwenStageGPublish ('HF '+$visibility.ToUpperInvariant()+' remote files verified') 'good'
        Append-Log ('[Qwen G2] '+$visibility.ToUpperInvariant()+' HF experimental repository: https://huggingface.co/'+$repo)
    }
})
$btnQwenStageGVisibility.Add_Click({
    Invoke-UiAction 'Changing verified Qwen Hugging Face repository visibility (no upload)...' {
        $p=Get-QwenStageGData
        $repo=([string]$txtQwenStageGRepo.Text).Trim()
        if ($repo -notmatch '^[a-zA-Z0-9][\w.-]*/qwen3-asr-0\.6b-snapdragon-sm8850(-v81|-v81-hybrid-experimental|v81-fp16)$') {
            throw 'Choose the previously uploaded Qwen repository. Omi/Parakeet repo names are blocked.'
        }
        $visibility=[string]$cmbQwenStageGVisibility.SelectedItem
        if ($visibility -notin @('private','public')) { throw 'Select PRIVATE or PUBLIC repository visibility.' }
        $receipt=Join-Path $p.Folder 'qwen-hf-upload-receipt.json'
        if (-not (Test-Path -LiteralPath $receipt -PathType Leaf)) {
            $receipt=Join-Path $p.Folder 'qwen-private-hf-upload-receipt.json'
        }
        if (-not (Test-Path -LiteralPath $receipt -PathType Leaf)) { throw 'No verified Stage G2 Hugging Face upload receipt found. Run G2 once first.' }
        $deployed=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ($deployed.repo_id -ne $repo) { throw 'Selected repository does not match verified G2 upload receipt.' }
        $releases=@(Get-ChildItem -LiteralPath $p.Folder -Directory -Filter 'experimental-hybrid-release-*' | Sort-Object LastWriteTime -Descending | Where-Object {
            $f=Join-Path $_.FullName 'model-manifest.json'
            try { (Test-Path -LiteralPath $f -PathType Leaf) -and
                  (Get-Content -LiteralPath $f -Raw | ConvertFrom-Json).status -eq 'experimental_hybrid_real_speech_pass_android_pending' }
            catch { $false }
        })
        if ($releases.Count -eq 0) { throw 'Original Stage G release folder is missing; cannot verify files before making them public.' }
        $release=$releases[0].FullName
        $message=if ($visibility -eq 'public') {
            'Make the existing, verified Qwen experimental model repository PUBLIC? Anyone will be able to download its model files. NO model files will be uploaded again.'
        } else {
            'Make the existing Qwen experimental model repository PRIVATE? NO model files will be uploaded again.'
        }
        $confirm=[System.Windows.Forms.MessageBox]::Show(($message+"`r`n`r`n"+$repo+"`r`n"+'Existing files and local checksums will be checked before any visibility change.'),
            'Confirm Hugging Face visibility change',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $token=([string]$txtQwenStageGToken.Text).Trim()
        if ([string]::IsNullOrWhiteSpace($token)) { throw 'Paste Hugging Face WRITE token in Stage G to change repository visibility.' }
        Register-SensitiveOutputValue $token
        $hfPython=Ensure-HuggingFaceUploaderPython
        $changedReceipt=Join-Path $p.Folder ('qwen-hf-visibility-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff')+'.json')
        $changed=Invoke-ProcessCapture -FilePath $hfPython -Arguments @($script:QwenAsrStageGPath,'set-visibility',
            '--folder',$release,'--repo-id',$repo,'--upload-receipt',$receipt,'--receipt',$changedReceipt,
            '--visibility',$visibility) -Environment @{ 'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'WR_HF_TOKEN'=$token } -TimeoutSeconds 21600 -DisplayCommand ('Qwen HF visibility change: '+$visibility+' (credentials hidden, no upload)')
        if ($changed.ExitCode -ne 0) { throw ('Unable to verify/change Qwen repo visibility: '+$changed.StdErr+$changed.StdOut) }
        $confirmed=Get-Content -LiteralPath $changedReceipt -Raw | ConvertFrom-Json
        if ($confirmed.status -ne 'experimental_qwen_hybrid_hf_visibility_verified' -or $confirmed.visibility -ne $visibility -or $confirmed.repo_id -ne $repo) {
            throw 'Missing confirmation of requested Qwen HF visibility.'
        }
        $txtQwenStageGToken.Clear()
        Set-Status $lblQwenStageGVisibility ('HF visibility verified: '+$visibility.ToUpperInvariant()) 'good'
        Append-Log ('[Qwen G3] Repo '+$visibility.ToUpperInvariant()+': https://huggingface.co/'+$repo)
    }
})
$btnQwenStageGOpen.Add_Click({
    try {
        $p=Get-QwenStageGData
        Open-Folder $p.Folder
    } catch { Show-Error $_ }
})

# -------------------- Qwen3 Stage H: lossless compact HF model release --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage H - Losslessly SHRINK the 0.6B package (shared FP32 decoder weights)' 25 3485 850 30 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'H1 shares identical FP32 weights and retains unique decoder-step tensors losslessly in ONE combined sidecar. Original package and NPU encoder are untouched. Verifies ONNX graph load, logits, and CPU token parity before packaging. Requires free disk space and RAM.' 25 3520 848 77))
$btnQwenStageHCompact = New-Button 'H1 - Lossless decoder weight sharing + verify' 25 3610 474 43
$tabQwenAsr.Controls.Add($btnQwenStageHCompact)
$lblQwenStageHCompact = New-StatusLabel 510 3617 335
$tabQwenAsr.Controls.Add($lblQwenStageHCompact)
$btnQwenStageHBranch = New-Button 'H2 - Upload compact HF BRANCH (leave main untouched)' 25 3666 474 43
$tabQwenAsr.Controls.Add($btnQwenStageHBranch)
$lblQwenStageHBranch = New-StatusLabel 510 3674 335
$tabQwenAsr.Controls.Add($lblQwenStageHBranch)
$tabQwenAsr.Controls.Add((New-Label 'H2 creates branch compact-fp32-v2 in the SAME repository. It removes the redundant sidecars ON THAT BRANCH ONLY. Existing Android v6.5.23 still needs v1 files on main. H2 never replaces main or changes public/private visibility. Android needs a future schema-v2 installer before switching branches.' 25 3725 849 85))
$btnQwenStageHOpen = New-Button 'Open Stage H compact results' 25 3820 372 40
$tabQwenAsr.Controls.Add($btnQwenStageHOpen)

function Get-QwenStageHPaths {
    $p = Get-QwenStageGData
    $old = @(Get-ChildItem -LiteralPath $p.Folder -Directory -Filter 'experimental-hybrid-release-*' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Where-Object {
        $mf = Join-Path $_.FullName 'model-manifest.json'
        if (-not (Test-Path -LiteralPath $mf -PathType Leaf)) { return $false }
        try {
            $m = Get-Content -LiteralPath $mf -Raw | ConvertFrom-Json
            return ([int]$m.schema_version -eq 1 -and $m.status -eq 'experimental_hybrid_real_speech_pass_android_pending')
        } catch { return $false }
    })
    if ($old.Count -eq 0) { throw 'No successful original Stage G G1 package found. First finish G1; DO NOT recompile.' }
    $compact = @(Get-ChildItem -LiteralPath $p.Folder -Directory -Filter 'experimental-hybrid-compact-release-*' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Where-Object {
        $mf=Join-Path $_.FullName 'model-manifest.json'
        if (-not (Test-Path -LiteralPath $mf -PathType Leaf)) { return $false }
        try { $m=Get-Content -LiteralPath $mf -Raw | ConvertFrom-Json; return ([int]$m.schema_version -eq 2) } catch { return $false }
    })
    return @{ StageG=$p; Original=$old[0].FullName; Compact=$compact }
}

$btnQwenStageHCompact.Add_Click({
    Invoke-UiAction 'Creating lossless shared-weight Qwen compact package (no Qualcomm or phone)' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'Stage H is available for validated 0.6B only.' }
        $h=Get-QwenStageHPaths
        if ($h.Compact.Count -gt 0) {
            $existing=$h.Compact[0].FullName
            $python=Ensure-QwenAsrExportPython
            $verified=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageHPath,'audit','--folder',$existing) -Environment @{'PYTHONUTF8'='1'} -TimeoutSeconds 21600
            if ($verified.ExitCode -ne 0) { throw 'Existing compact release failed audit. Preserve it and inspect: '+$verified.StdErr+$verified.StdOut }
            Set-Status $lblQwenStageHCompact 'Already compact + hash verified' 'good'
            Append-Log ('[Qwen H1] Reusing intact compact package: '+$existing)
            return
        }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            ('Convert the existing Stage G decoder package WITHOUT modifying originals?' + "`r`n`r`n" +
             'Matching FP32 tensors reuse their exact bytes; unmatched large tensors are appended (not dropped). Savings depend on real tensor overlap. The new ONNX graphs are checked and run on CPU against original tokens and logits.' + "`r`n" +
             'No phone files touched, no Qualcomm job and NO HF upload in H1. Original package retained for rollback.'),
            'Confirm lossless Qwen weight sharing',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        $deps=Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import onnx, onnxruntime, numpy; print("Compact decoder dependencies ready")') -TimeoutSeconds 60
        if ($deps.ExitCode -ne 0) { throw ('Original Qwen ONNX environment missing packages: '+$deps.StdErr) }
        $dest=Join-Path $h.StageG.Folder ('experimental-hybrid-compact-release-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $run=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageHPath,'compact','--source-release',$h.Original,'--output-dir',$dest) -Environment @{'PYTHONUTF8'='1';'PYTHONUNBUFFERED'='1'} -TimeoutSeconds 21600
        if ($run.ExitCode -ne 0) { throw ('Qwen H1 compact parity/packaging failed. Original release unaffected. See '+$dest+': '+$run.StdErr+$run.StdOut) }
        $result=Get-Content -LiteralPath (Join-Path (Split-Path $dest -Parent) ((Split-Path $dest -Leaf)+'-compact-receipt.json')) -Raw | ConvertFrom-Json
        if ($result.status -ne 'lossless_shared_weights_compact_package_verified' -or $result.weight_sharing.saved_bytes -le 0 -or
            $result.decoder_smoke.status -ne 'exact_cpu_onnx_decoder_token_smoke_pass') { throw 'Compaction success receipt missing; do not upload.' }
        Set-Status $lblQwenStageHCompact ('Saved '+[math]::Round($result.weight_sharing.saved_bytes/1GB,2)+' GiB') 'good'
        Append-Log ('[Qwen H1] Compact release: '+$dest+'; saved bytes '+$result.weight_sharing.saved_bytes)
        [System.Windows.Forms.MessageBox]::Show(('Lossless Stage H1 saved '+[math]::Round($result.weight_sharing.saved_bytes/1GB,2)+
           ' GiB. Original Stage G remains untouched. Android v6.5.23 CANNOT use compact schema v2 without an app update.'),
           'Compact Qwen package verified',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnQwenStageHBranch.Add_Click({
    Invoke-UiAction 'Publishing verified compact Qwen package on isolated HF branch (not main)...' {
        $h=Get-QwenStageHPaths
        if ($h.Compact.Count -eq 0) { throw 'First run H1 to create the compact model.' }
        $compact=$h.Compact[0].FullName
        $repo=([string]$txtQwenStageGRepo.Text).Trim()
        if ($repo -notmatch '^[a-zA-Z0-9][\w.-]*/qwen3-asr-0\.6b-snapdragon-sm8850(-v81|-v81-hybrid-experimental|v81-fp16)$') {
            throw 'H2 is restricted to dedicated Qwen repositories.'
        }
        $python=Ensure-QwenAsrExportPython
        $aud=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageHPath,'audit','--folder',$compact) -TimeoutSeconds 21600
        if ($aud.ExitCode -ne 0) { throw ('Compact package audit failed: '+$aud.StdErr+$aud.StdOut) }
        $token=$txtQwenStageGToken.Text.Trim()
        if (-not $token) { throw 'Enter your Hugging Face WRITE token in the masked Stage G field above.' }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            ('Publish the compact Qwen package to HF repository '+$repo+' branch compact-fp32-v2?' + "`r`n`r`n" +
             'This sends a shared multi-GB decoder file. The existing main branch, all previous Android downloads and repo visibility REMAIN UNCHANGED.' + "`r`n" +
             'Requires previously uploaded verified Stage G v1 release on main. The new branch requires a future Android schema-v2 client.'),
            'Authorize Qwen compact BRANCH upload',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $hf=Ensure-HuggingFaceUploaderPython
        $receipt=Join-Path $h.StageG.Folder 'qwen-hf-compact-branch-upload-receipt.json'
        if (Test-Path -LiteralPath $receipt) { throw 'Existing compact branch receipt detected; refusing repeat upload.' }
        $res=Invoke-ProcessCapture -FilePath $hf -Arguments @($script:QwenAsrStageHPath,'publish-branch',
             '--source-release',$h.Original,'--compact-release',$compact,'--repo-id',$repo,'--receipt',$receipt) -Environment @{
             'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'WR_HF_TOKEN'=$token } -TimeoutSeconds 21600 -DisplayCommand 'Qwen H2 HF compact BRANCH upload (token hidden)'
        if ($res.ExitCode -ne 0) { throw ('Compact HF branch upload not confirmed. DO NOT repeat blindly: '+$res.StdErr+$res.StdOut) }
        $r=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ($r.status -ne 'compact_hf_branch_upload_verified' -or -not [bool]$r.main_unchanged) { throw 'Missing remote branch verification receipt' }
        $txtQwenStageGToken.Clear()
        Set-Status $lblQwenStageHBranch 'Compact HF branch verified' 'good'
        Append-Log ('[Qwen H2] https://huggingface.co/'+$repo+'/tree/compact-fp32-v2 (main unchanged)')
    }
})
$btnQwenStageHOpen.Add_Click({ try { $p=Get-QwenStageGData; Open-Folder $p.Folder } catch { Show-Error $_ } })


# -------------------- Qwen release control + optional smaller experimental model --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage H3 - Publish verified compact v2 as Hugging Face MAIN (no branch prerequisite)' 25 3880 850 28 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Backs up the old main under stage-g-v1-backup-<SHA>, pins the original revision, then atomically updates model files, manifest and deletes old decoder sidecars. Does not change repository visibility. Existing v6.5.24 installs use compact-fp32-v2; Android v6.5.25 can download main.' 25 3910 850 70))
$btnQwenStageHMain=New-Button 'H3 - Replace HF main with VERIFIED compact v2' 25 3990 475 43
$tabQwenAsr.Controls.Add($btnQwenStageHMain)
$lblQwenStageHMain=New-StatusLabel 510 3998 340
$tabQwenAsr.Controls.Add($lblQwenStageHMain)
$btnQwenStageHMain.Add_Click({
    Invoke-UiAction 'Publishing verified compact Qwen release to HF main with rollback...' {
        $h=Get-QwenStageHPaths
        if ($h.Compact.Count -eq 0) { throw 'Run Stage H1 first and confirm its parity report.' }
        $compact=$h.Compact[0].FullName
        $repo=([string]$txtQwenStageGRepo.Text).Trim()
        if ($repo -notmatch '^[a-zA-Z0-9][\w.-]*/qwen3-asr-0\.6b-snapdragon-sm8850(-v81|-v81-hybrid-experimental|v81-fp16)$') {
            throw 'Publishing main is restricted to dedicated Qwen repositories.'
        }
        $token=([string]$txtQwenStageGToken.Text).Trim()
        if (-not $token) { throw 'Enter the HF WRITE token in masked Qwen Stage G field.' }
        $python=Ensure-QwenAsrExportPython
        $aud=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageHPath,'audit','--folder',$compact) -TimeoutSeconds 21600
        if ($aud.ExitCode -ne 0) { throw ('Compact validation failed; no upload: '+$aud.StdErr+$aud.StdOut) }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
          ('REPLACE HF main for '+$repo+' with the VERIFIED Stage H1 compact decoder?' + "`r`n`r`n"+
           'This will remove decoder_init.onnx.data and decoder_step.onnx.data from the new main revision, ' +
           'create a rollback branch from existing main, and upload shared decoder_weights.data. ' +
           'The old Stage G version will remain locally and in the rollback branch. ' +
           'Android v6.5.24 still uses compact-fp32-v2. No model is changed on your phone.'),
          'Authorize replacing Qwen HF MAIN',[System.Windows.Forms.MessageBoxButtons]::YesNo,
          [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $hf=Ensure-HuggingFaceUploaderPython
        $receipt=Join-Path $h.StageG.Folder 'qwen-hf-compact-MAIN-upload-receipt.json'
        if (Test-Path -LiteralPath $receipt) { throw 'Prior main deployment receipt already exists; check it before repeating.' }
        $res=Invoke-ProcessCapture -FilePath $hf -Arguments @($script:QwenAsrStageHPath,'publish-main',
           '--source-release',$h.Original,'--compact-release',$compact,'--repo-id',$repo,'--receipt',$receipt,
           '--acknowledge','REPLACE_QWEN_MAIN_WITH_VERIFIED_COMPACT_V2') -Environment @{
           'WR_HF_TOKEN'=$token;'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 21600 -DisplayCommand 'Qwen H3 pinned Hugging Face main promotion (HF token hidden)'
        if ($res.ExitCode -ne 0) { throw ('H3 main publish unverified: '+$res.StdErr+$res.StdOut) }
        $r=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ($r.status -notin @('compact_hf_main_replacement_verified','compact_hf_main_already_verified')) {
            throw 'Missing verified main publication receipt'
        }
        $txtQwenStageGToken.Clear()
        Set-Status $lblQwenStageHMain 'Compact main verified' 'good'
        Append-Log ('[Qwen H3] Verified compact MAIN: https://huggingface.co/'+$repo+'/tree/main')
    }
})
$tabQwenAsr.Controls.Add((New-Label 'Stage I - OPTIONAL smaller decoder package: FP16 weight STORAGE, CPU operators stay FP32' 25 4070 850 30 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'I1 changes decoder weight precision on disk and inserts FP16->FP32 casts. Experimental: checks CPU graph loading + exact synthetic AND real-audio decoder tokens before accepting. Does NOT upload or replace HF main. Android schema v3 adaptation required after successful PC parity.' 25 4105 850 79))
$btnQwenStageIPack=New-Button 'I1 - Further shrink decoder (experimental, verify real audio)' 25 4200 475 45
$tabQwenAsr.Controls.Add($btnQwenStageIPack)
$lblQwenStageIPack=New-StatusLabel 510 4208 335
$tabQwenAsr.Controls.Add($lblQwenStageIPack)
$btnQwenStageIPack.Add_Click({
    Invoke-UiAction 'Creating experimental FP16-storage Qwen decoder and checking real-audio parity...' {
        $h=Get-QwenStageHPaths
        if ($h.Compact.Count -eq 0) { throw 'Stage I needs a verified Stage H1 compact package.' }
        $audio=([string]$txtQwenAsrAudio.Text).Trim()
        if (-not $audio -or -not (Test-Path -LiteralPath $audio -PathType Leaf) -or [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.ogg','.wav')) {
            throw 'Select your real .ogg or .wav speech sample in the existing Qwen Stage E/G audio picker.'
        }
        $data=Get-QwenStageGData
        $ffmpeg=([string]$txtFfmpeg.Text).Trim()
        if (-not $ffmpeg -or -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf)) { throw 'Select FFmpeg in Builder Setup.' }
        $python=Ensure-QwenAsrExportPython
        $dep=Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import onnx, onnxruntime, numpy, tokenizers, librosa; print("FP16 storage dependencies ready")') -TimeoutSeconds 90
        if ($dep.ExitCode -ne 0) { throw ('Qwen Python environment missing dependency: '+$dep.StdErr) }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
          ('Convert Stage H compact decoder weight STORAGE to FP16 in a SEPARATE package?'+"`r`n`r`n"+
           'This changes numerical values and may affect words. It must pass real speech exact-token CPU parity; it will NOT be uploaded to HF or installed in Android automatically. ' +
           'Keep the original compact v2 as your main release until a v3 Android runtime test passes.'),
          'Confirm experimental Qwen size reduction',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $dest=Join-Path $h.StageG.Folder ('experimental-hybrid-fp16-storage-'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $r=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageIPath,'build',
          '--compact-release',$h.Compact[0].FullName,'--output-dir',$dest,'--audio',$audio,
          '--ffmpeg',$ffmpeg,'--onnx-dir',$data.Onnx,'--exporter',$data.Exporter) -Environment @{
          'PYTHONUTF8'='1';'PYTHONUNBUFFERED'='1'} -TimeoutSeconds 21600
        if ($r.ExitCode -ne 0) { throw ('Stage I1 cannot be qualified; original compact remains intact. '+$r.StdErr+$r.StdOut) }
        $report=Join-Path $h.StageG.Folder ((Split-Path $dest -Leaf)+'-fp16-storage-receipt.json')
        $verified=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($verified.status -ne 'experimental_fp16_storage_1_audio_parity_pass_not_android_ready') {
            throw 'Stage I result not approved; do not publish'
        }
        Set-Status $lblQwenStageIPack ('Saved '+[math]::Round($verified.additional_saved_bytes/1GB,2)+' GiB MORE') 'good'
        Append-Log ('[Qwen I1] Experimental (NOT Android ready) result: '+$report)
    }
})


# -------------------- Stage I2 pinned HF main publication (FP16 schema v3) --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage I2 - OPTIONAL: Publish verified I1 FP16-storage package to Hugging Face MAIN' 25 4280 850 30 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'WARNING: Android v6.5.33 source supports schema v3; test installed APK and runtime before promoting remote main. I2 preserves current HF main under a rollback branch, checks the complete FP16 v3 package SHA-256, and publishes it with an explicit confirmation. Still experimental: S26 decoder testing pending.' 25 4317 850 73))
$btnQwenStageIMain=New-Button 'I2 - Publish VERIFIED FP16 schema v3 to HF MAIN' 25 4405 475 45
$tabQwenAsr.Controls.Add($btnQwenStageIMain)
$lblQwenStageIMain=New-StatusLabel 510 4413 340
$tabQwenAsr.Controls.Add($lblQwenStageIMain)
$btnQwenStageIMain.Add_Click({
    Invoke-UiAction 'Publishing Stage I FP16 decoder to Qwen HF main with rollback...' {
        $h=Get-QwenStageHPaths
        if ($h.Compact.Count -eq 0) { throw 'Stage H1 compact package is required as the v3 provenance source.' }
        $data=Get-QwenStageGData
        $receiptFiles=@(Get-ChildItem -LiteralPath $data.Folder -Filter '*-fp16-storage-receipt.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        if ($receiptFiles.Count -eq 0) { throw 'No Stage I1 receipt found. Run I1 and verify it passed.' }
        $candidateReceipt=Get-Content -LiteralPath $receiptFiles[0].FullName -Raw | ConvertFrom-Json
        if ($candidateReceipt.status -ne 'experimental_fp16_storage_1_audio_parity_pass_not_android_ready') {
            throw 'Latest Stage I receipt is not a passing real-speech comparison.'
        }
        $candidate=([string]$candidateReceipt.package_dir).Trim()
        if (-not $candidate -or -not (Test-Path -LiteralPath $candidate -PathType Container)) {
            throw 'Stage I package folder no longer exists.'
        }
        $repo=([string]$txtQwenStageGRepo.Text).Trim()
        if ($repo -notmatch '^[a-zA-Z0-9][\w.-]*/qwen3-asr-0\.6b-snapdragon-sm8850(-v81|-v81-hybrid-experimental|v81-fp16)$') {
            throw 'Publishing main is restricted to dedicated Qwen repositories.'
        }
        $token=([string]$txtQwenStageGToken.Text).Trim()
        if (-not $token) { throw 'Enter HF WRITE token in the masked Qwen Stage G field.' }
        $python=Ensure-QwenAsrExportPython
        $aud=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageIPath,'audit','--folder',$candidate) -TimeoutSeconds 21600
        if ($aud.ExitCode -ne 0) { throw ('FP16 schema v3 audit failed; no upload: '+$aud.StdErr+$aud.StdOut) }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
           ('Publish Stage I1 FP16-storage schema v3 to HF MAIN for '+$repo+'?' + "`r`n`r`n"+
            'This makes the prior v1/v2 decoder unavailable at MAIN, but a rollback branch will preserve it. '+
            'Android v6.5.25 will NOT be able to download v3. Upgrade the Android source to v6.5.26 first. '+
            'This FP16 model only passed limited Windows transcription parity and is still experimental. '+
            'No audio or transcript is uploaded.'),
           'Authorize Experimental Qwen FP16 Main Publication',[System.Windows.Forms.MessageBoxButtons]::YesNo,
           [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        # Stage I imports NumPy on module load, so use the existing Qwen
        # ONNX environment (already proven by I1), not the lightweight HF venv.
        # That environment also contains huggingface_hub for publishing.
        $hf=$python
        $receipt=Join-Path $data.Folder 'qwen-hf-fp16-MAIN-upload-receipt.json'
        if (Test-Path -LiteralPath $receipt) { throw 'Previous I2 receipt exists; inspect it before repeating.' }
        $res=Invoke-ProcessCapture -FilePath $hf -Arguments @($script:QwenAsrStageIPath,'publish-main',
          '--source-release',$h.Original,'--compact-release',$h.Compact[0].FullName,
          '--fp16-release',$candidate,'--repo-id',$repo,'--receipt',$receipt,
          '--acknowledge','REPLACE_QWEN_MAIN_WITH_EXPERIMENTAL_FP16_SCHEMA_V3') -Environment @{
          'WR_HF_TOKEN'=$token;'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 21600 -DisplayCommand 'Qwen I2 pinned FP16 HF main promotion (token hidden)'
        if ($res.ExitCode -ne 0) { throw ('I2 main upload unverified; inspect HF repository BEFORE retrying: '+$res.StdErr+$res.StdOut) }
        $verified=Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ($verified.status -notin @('fp16_schema_v3_main_replacement_verified','fp16_schema_v3_main_already_verified')) {
            throw 'I2 remote verification receipt invalid'
        }
        $txtQwenStageGToken.Clear()
        Set-Status $lblQwenStageIMain 'FP16 v3 HF main verified' 'good'
        Append-Log ('[Qwen I2] Verified experimental FP16 MAIN: https://huggingface.co/'+$repo+'/tree/main')
    }
})

# -------------------- Combined J1 + J2 + J3 + J4 + J5 + J6 + J7: one-click offline workflow --------------------
$tabQwenAsr.Controls.Add((New-Label 'Decoder Stage J: historical tools and current NPU experiments' 25 5470 855 32 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Historical tests are kept for reference; recent J72-J79 actions are near the bottom of the scrollable list. Do not repeat previous work unless specifically required by a new diagnostic.' 25 5510 840 75))
$btnQwenJBatch = New-Button 'RUN J1-J11 (one click, offline)' 25 4645 495 48
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($btnQwenJBatch)
$lblQwenJBatch = New-StatusLabel 540 4656 310
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($lblQwenJBatch)
$chkQwenJBatchForce = New-Object System.Windows.Forms.CheckBox
$chkQwenJBatchForce.Text = 'Force J1/J2 rerun (ignore verified receipts)'
$chkQwenJBatchForce.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,4710)
$chkQwenJBatchForce.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(430,28)
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($chkQwenJBatchForce)
# The Stage E audio field is several screens above the combined J button.
# Expose the SAME picker locally, and prompt inline when no valid audio exists.
$btnQwenJBatchAudio = New-Button 'Choose J2 OGG/WAV...' 25 4747 230 36
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($btnQwenJBatchAudio)
$lblQwenJBatchAudio = New-Label 'No J2 recording selected' 265 4751 255 32
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($lblQwenJBatchAudio)
$script:QwenAudioTip = New-Object System.Windows.Forms.ToolTip
$txtQwenAsrAudio.Add_TextChanged({
    $chosen = ([string]$txtQwenAsrAudio.Text).Trim()
    if ($chosen -and (Test-Path -LiteralPath $chosen -PathType Leaf) -and
        [IO.Path]::GetExtension($chosen).ToLowerInvariant() -in @('.wav','.ogg')) {
        $lblQwenJBatchAudio.Text = 'J2 recording: ' + [IO.Path]::GetFileName($chosen)
        $script:QwenAudioTip.SetToolTip($lblQwenJBatchAudio, $chosen)
    } else {
        $lblQwenJBatchAudio.Text = 'No valid J2 recording selected'
        $script:QwenAudioTip.SetToolTip($lblQwenJBatchAudio, 'Select an existing .ogg or .wav recording')
    }
})
$btnQwenJBatchAudio.Add_Click({
    try { [void](Select-QwenAsrValidationAudio -PathField $txtQwenAsrAudio -Title 'Choose OGG/WAV for one-click J1 + J2 + J3') }
    catch { Show-Error $_ }
})
# Optional extra clips use the SAME J2 processing pipeline; never scan private folders.
# Keep audio path selection local to this GUI session (not persisted or exported).
$script:QwenJExtraAudios = @()
$btnQwenJBatchExtra = New-Button 'Add 1-2 clips (0 selected)' 525 4747 310 36
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($btnQwenJBatchExtra)
$btnQwenJBatchExtra.Add_Click({
    $dlg = New-Object System.Windows.Forms.OpenFileDialog
    try {
        $dlg.Filter = 'Audio (*.wav;*.ogg)|*.wav;*.ogg|WAV (*.wav)|*.wav|OGG (*.ogg)|*.ogg'
        $dlg.Title = 'Select up to TWO additional distinct speech clips for Qwen J4'
        $dlg.CheckFileExists = $true
        $dlg.CheckPathExists = $true
        $dlg.Multiselect = $true
        if ($dlg.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { return }
        $chosen = @($dlg.FileNames)
        if ($chosen.Count -gt 2) {
            [void][System.Windows.Forms.MessageBox]::Show('Select at most 2 additional clips (3 total).', 'Qwen J4', [System.Windows.Forms.MessageBoxButtons]::OK)
            return
        }
        foreach ($clip in $chosen) {
            if (-not (Test-Path -LiteralPath $clip -PathType Leaf) -or
                [IO.Path]::GetExtension($clip).ToLowerInvariant() -notin @('.ogg','.wav')) {
                throw 'J4 accepts only existing local OGG or WAV files.'
            }
        }
        $script:QwenJExtraAudios = @($chosen)
        $btnQwenJBatchExtra.Text = ('Add 1-2 clips (' + $chosen.Count + ' selected)')
    } catch { Show-Error $_ }
    finally { $dlg.Dispose() }
})
# Test without running giant models, cloud jobs, ADB, or installing new tools.
# The standard-library release gate exercises the existing Stage J unit suite.
$btnQwenJSelfTests = New-Button 'Run offline Builder self-tests' 535 4701 300 36
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($btnQwenJSelfTests)
$btnQwenJSelfTests.Add_Click({
    Invoke-UiAction 'Running offline Builder source, wiring and J regression tests...' {
        $qwenPy = Join-Path $Root 'tools\qwen-asr-export-env\Scripts\python.exe'
        if (-not (Test-Path -LiteralPath $qwenPy -PathType Leaf)) {
            $qwenPy = ([string]$txtPython.Text).Trim()
        }
        if (-not $qwenPy -or -not (Test-Path -LiteralPath $qwenPy -PathType Leaf)) {
            throw 'No existing Python environment for offline self-tests. Configure Python in Setup; self-test never downloads dependencies.'
        }
        # Self-tests use pytest-style functions/fixtures; do not run unittest.
        # Repair ONLY the isolated existing Python environment, after consent.
        $pytestProbe = Invoke-ProcessCapture -FilePath $qwenPy -Arguments @('-c','import pytest; print(pytest.__version__)') -TimeoutSeconds 30
        if ($pytestProbe.ExitCode -ne 0) {
            $managedQwenPy = Join-Path $Root 'tools\qwen-asr-export-env\Scripts\python.exe'
            if (-not [string]::Equals($qwenPy, $managedQwenPy, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'pytest is missing in the selected external Python. Configure the isolated Qwen export environment through Setup; Builder will not pip-install into an unmanaged Python.'
            }
            $approval = [System.Windows.Forms.MessageBox]::Show(
                'Offline Builder tests require the small pytest test-runner package. Install pinned pytest 8.4.2 into the existing isolated Python environment? This uses pip/network if not cached. Models, phone and Qualcomm services are not touched.',
                'Repair offline test environment',
                [System.Windows.Forms.MessageBoxButtons]::YesNo,
                [System.Windows.Forms.MessageBoxIcon]::Question)
            if ($approval -ne [System.Windows.Forms.DialogResult]::Yes) {
                Set-Status $lblQwenJBatch 'Tests need pytest; installation declined' 'warn'
                Append-Log 'Builder self-tests not run: pytest is missing; user declined optional install.'
                return
            }
            $setup = Invoke-ProcessCapture -FilePath $qwenPy -Arguments @('-m','pip','install','--disable-pip-version-check','--no-input','pytest==8.4.2') -TimeoutSeconds 300
            if ($setup.ExitCode -ne 0) {
                throw 'Test-only pytest installation failed. No model changes; check the local command log, network access, or Python pip availability.'
            }
            $pytestProbe = Invoke-ProcessCapture -FilePath $qwenPy -Arguments @('-c','import pytest; print(pytest.__version__)') -TimeoutSeconds 30
            if ($pytestProbe.ExitCode -ne 0) { throw 'pytest package was installed but cannot be imported in the selected Builder environment.' }
        }
        $folder = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        $report = Join-Path $folder ('builder-quality-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $test = Invoke-ProcessCapture -FilePath $qwenPy -Arguments @($script:BuilderQualityGatePath,'--mode','full','--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 360
        if ($test.ExitCode -ne 0) {
            $code = 'QG_TEST_RUNNER_FAILED'
            $failedNames = ''
            if (Test-Path -LiteralPath $report -PathType Leaf) {
                try {
                    $failed = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
                    if ($failed.failure_code) { $code = [string]$failed.failure_code }
                    if ($failed.test_summary -and $failed.test_summary.failed_test_ids) {
                        $failedNames = (@($failed.test_summary.failed_test_ids) | Select-Object -First 3) -join ', '
                    }
                } catch { }
            }
            Append-Log ('Offline self-tests FAILED: ' + $code + $(if ($failedNames) { ' (' + $failedNames + ')' } else { '' }))
            throw ('Builder self-test ' + $code + $(if ($failedNames) { ': ' + $failedNames } else { '' }) + '. Diagnostic ZIP and private local test details beside: ' + $report + '. No model modifications.')
        }
        $selfCheck = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($selfCheck.status -ne 'passed') { throw 'Builder offline self-test receipt does not report PASS.' }
        Set-Status $lblQwenJBatch 'Builder self-tests PASS; no inference run' 'good'
        Append-Log ('Offline Builder self-tests PASS: ' + $report)
    }
})
$btnQwenJBatch.Add_Click({
    # Prompt before invoking the tracked action so Cancel does not appear as
    # a successful J run in the session log or generate a failure archive.
    $selectedAudio = ([string]$txtQwenAsrAudio.Text).Trim()
    if (-not $selectedAudio -or -not (Test-Path -LiteralPath $selectedAudio -PathType Leaf) -or
        [IO.Path]::GetExtension($selectedAudio).ToLowerInvariant() -notin @('.ogg','.wav')) {
        try {
            Append-Log '[Qwen combined] No valid J2 recording selected; opening shared audio picker.'
            if (-not (Select-QwenAsrValidationAudio -PathField $txtQwenAsrAudio -Title 'Choose OGG/WAV for one-click J1 + J2 + J3')) {
                Set-Status $lblQwenJBatch 'Audio not selected; no stages started' 'warn'
                Append-Log '[Qwen combined] Audio selection cancelled or invalid; no stage, cloud, or phone work performed.'
                return
            }
        } catch { Show-Error $_; return }
    }
    Invoke-UiAction 'Running resumable Qwen offline J1 through J11; no cloud or phone actions...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'Combined Qwen J workflow supports the verified 0.6B ONNX export only.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Select existing Qwen 0.6B ONNX export in Stage D first.' }
        $audio=([string]$txtQwenAsrAudio.Text).Trim()
        if (-not $audio -or -not (Test-Path -LiteralPath $audio -PathType Leaf) -or
            [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.ogg','.wav')) {
            throw 'The selected recording is no longer a readable local .ogg or .wav file.'
        }
        foreach ($extra in @($script:QwenJExtraAudios)) {
            if (-not (Test-Path -LiteralPath $extra -PathType Leaf) -or
                [IO.Path]::GetExtension($extra).ToLowerInvariant() -notin @('.ogg','.wav')) {
                throw 'One of the additional recordings was moved or is not OGG/WAV. Re-select clips.'
            }
        }
        $ffmpeg=([string]$txtFfmpeg.Text).Trim()
        if (-not $ffmpeg -or -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf)) { throw 'Configure FFmpeg in Setup.' }
        $provenance=Join-Path $p.ModelFolder 'third-party-exporter-provenance.json'
        if (-not (Test-Path -LiteralPath $provenance -PathType Leaf)) { throw 'Pinned Stage C exporter provenance missing.' }
        $exporterData=Get-Content -LiteralPath $provenance -Raw | ConvertFrom-Json
        if ($exporterData.exporter_commit -ne '4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50') { throw 'Unsupported exporter revision.' }
        $exporter=[string]$exporterData.folder
        if (-not (Test-Path -LiteralPath $exporter -PathType Container)) { throw 'Pinned Stage C exporter folder unavailable.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $prior=''
        $g1folder=Join-Path $p.ModelFolder 'stage-g-hybrid-deployment'
        if (Test-Path -LiteralPath $g1folder -PathType Container) {
            $sha=(Get-FileHash -LiteralPath $audio -Algorithm SHA256).Hash.ToLowerInvariant()
            foreach ($f in @(Get-ChildItem -LiteralPath $g1folder -Filter 'qwen-real-speech-hybrid-validation.json' -Recurse -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $old=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($old.status -eq 's26_encoder_cpu_decoder_real_speech_exact_token_pass' -and
                        $old.audio_source_sha256 -eq $sha -and $old.token_exact) { $prior=$f.FullName; break }
                } catch { }
            }
        }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            ('Run J1-J11 together: contract audit, real-speech parity, KV transfer risk, cache planning, isolated last-logits candidate, and J7 FP32 CPU parity (if 3 clips validated)?' + "`r`n`r`n" +
             'Matching J1/J2 receipts are reused, including up to two older clips to fill the 3-clip envelope. New J2 runs can use several GB of PC RAM; no audio uploaded.' + "`r`n" +
             'No network, cloud compilation or cost, uploads, phone changes, or original model/weight edits. Original decoder weights may have multiple hardlinks: J7 runtime parity does NOT override ONNX hardlink security warning or authorize compilation. J6 creates a small additional ONNX graph BESIDE original decoder_init.onnx to safely reference existing external weights, not a full decoder copy. J7 CPU prefill parity, J8 original FP32 first-step replay, and J9 bounded-cache safety simulation, J10 step delta-output rewrite, and J11 real FP32 step parity are included. FP16/NPU KV design and compilation approval remain pending. One summary ZIP even on failure.'),
            'Qwen combined offline decoder workflow', [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        # Lightweight validation before expensive CPU decoder work. Fails safely
        # with an automatic bounded audit ZIP if model dependencies/paths drift.
        $qgPath = Join-Path $dir ('builder-quality-preflight-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $qgArgs=@($script:BuilderQualityGatePath,'--mode','quick','--output',$qgPath,
            '--onnx-dir',$p.Onnx,'--audio',$audio,'--ffmpeg',$ffmpeg,
            '--exporter',$exporter,'--exporter-receipt',$provenance)
        $qg=Invoke-ProcessCapture -FilePath $python -Arguments $qgArgs -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($qg.ExitCode -ne 0) {
            $qgReceipt=Get-Content -LiteralPath $qgPath -Raw | ConvertFrom-Json
            throw ('Preflight ' + [string]$qgReceipt.failure_code + ': ' + $qgPath + ' (diagnostic ZIP beside it). Original weights untouched.')
        }
        $report=Join-Path $dir ('stage-j-combined-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJBatchPath,'run','--onnx-dir',$p.Onnx,
            '--audio',$audio,'--ffmpeg',$ffmpeg,'--exporter',$exporter,
            '--exporter-receipt',$provenance,'--output',$report)
        foreach ($extra in @($script:QwenJExtraAudios)) { $args += @('--audio', [string]$extra) }
        if ($prior) { $args+=@('--g1-receipt',$prior) }
        if ($chkQwenJBatchForce.Checked) { $args+='--force' }
        $res=Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('Combined J diagnostics missing: '+$report) }
        Append-Log ('Combined decoder offline diagnostics (review before sharing): '+$archive)
        if ($res.ExitCode -ne 0) {
            $blocked = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            $code = [string]$blocked.failure_code
            $stage = [string]$blocked.failure_stage
            if (-not $code) { $code='UNCLASSIFIED' }
            if (-not $stage) { $stage='UNKNOWN' }
            Set-Status $lblQwenJBatch ('Blocked '+$stage+': '+$code) 'warn'
            throw ('Combined J1-J11 blocked at '+$stage+' ('+$code+'). Detailed exception in LOCAL JSON; sanitized ZIP: '+$archive)
        }
        $result=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($result.status -notin @('offline_stages_passed_compilation_not_started',
                                    'offline_stages_completed_with_j6_checker_warning_compilation_not_started',
                                    'offline_j7_prefill_parity_pass_compilation_not_started',
                                    'offline_j9_cache_plan_pass_compilation_not_started',
                                    'offline_j11_delta_parity_pass_compilation_not_started') -or
            @($result.stages).Count -ne 11 -or $result.j3.compiler_ready -ne $false -or $result.j4.compiler_ready -ne $false -or $result.j5.compile_submission_allowed -ne $false -or $result.j6.compile_submission_allowed -ne $false -or $result.j7.compile_submission_allowed -ne $false -or $result.j8.compile_submission_allowed -ne $false -or $result.j9.compile_submission_allowed -ne $false -or $result.j10.compile_submission_allowed -ne $false -or $result.j11.compile_submission_allowed -ne $false) {
            throw 'Combined workflow unexpected status; QNN compilation NOT approved.'
        }
        if ($result.status -eq 'offline_j11_delta_parity_pass_compilation_not_started') {
            # Independent postflight validates stage ordering, exact numerical
            # gates, compiler lock and scrubbed diagnostic archive integrity.
            $postPath = Join-Path $dir ('builder-quality-postflight-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
            $post=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,
                '--mode','postflight','--output',$postPath,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
            if ($post.ExitCode -ne 0) {
                $postReceipt=Get-Content -LiteralPath $postPath -Raw | ConvertFrom-Json
                throw ('Independent J7 postflight blocked: ' + [string]$postReceipt.failure_code + '. ' + $postPath)
            }
            Set-Status $lblQwenJBatch 'J7/J8/J11 FP32 parity PASS; compile LOCKED' 'good'
            Append-Log ('J7 prefill parity, J8 CPU step, J9 cache safety and J11 delta parity passed; J6 checker warning remains. '+$report)
        } elseif ($result.status -eq 'offline_stages_completed_with_j6_checker_warning_compilation_not_started') {
            Set-Status $lblQwenJBatch 'J6 checker warning; candidate NOT verified' 'warn'
            Append-Log ('J6 ONNX checker rejected candidate; J7 parity and compiler gates remain CLOSED. See local JSON '+$report)
        } elseif ($result.j5.three_clip_evidence_collected -eq $true) {
            Set-Status $lblQwenJBatch 'J1-J11 offline prerequisites complete' 'good'
        } else {
            Set-Status $lblQwenJBatch 'J1-J5 passed; J6 requires 3 clips' 'warn'
        }
        Append-Log ('Combined J1-J11 complete; stages were resumed where appropriate. Receipt: '+$report)
    }
})

# -------------------- One-click J12+J13 continuation (reuse verified J11) --------------------
# Second combined action avoids rerunning the successful J1-J11 decoder pass.
$btnQwenJFollowup = New-Button 'RUN J12+J13 (offline)' 545 4720 310 47
# Qwen grid adds this control after all handlers have been registered: $tabQwenAsr.Controls.Add($btnQwenJFollowup)
$btnQwenJFollowup.Add_Click({
    $audio=([string]$txtQwenAsrAudio.Text).Trim()
    if (-not $audio -or -not (Test-Path -LiteralPath $audio -PathType Leaf) -or
        [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.ogg','.wav')) {
        try {
            if (-not (Select-QwenAsrValidationAudio -PathField $txtQwenAsrAudio -Title 'Choose previously validated J11 OGG/WAV')) { return }
        } catch { Show-Error $_; return }
    }
    Invoke-UiAction 'Running combined Qwen J12/J13 sequential CPU parity and KV transport analysis; no cloud or phone...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J12/J13 supports the verified Qwen 0.6B only.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original Stage D ONNX directory missing.' }
        $audio=([string]$txtQwenAsrAudio.Text).Trim()
        $ffmpeg=([string]$txtFfmpeg.Text).Trim()
        $provenance=Join-Path $p.ModelFolder 'third-party-exporter-provenance.json'
        if (-not (Test-Path -LiteralPath $audio -PathType Leaf) -or
            -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf) -or
            -not (Test-Path -LiteralPath $provenance -PathType Leaf)) {
            throw 'J12/J13 requires previously selected audio, FFmpeg and verified Stage C provenance.'
        }
        $record=Get-Content -LiteralPath $provenance -Raw | ConvertFrom-Json
        if ($record.exporter_commit -ne '4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50') { throw 'Pinned exporter revision mismatch.' }
        $exporter=[string]$record.folder
        if (-not (Test-Path -LiteralPath $exporter -PathType Container)) { throw 'Pinned exporter folder missing.' }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'Run eight consecutive FP32 decoder steps against the isolated KV-delta graph and calculate cache-transfer costs? Uses matching J11 receipts; sequential multi-GB CPU sessions may take memory/time. No uploads, QNN compilation, ADB, downloads, or source model modifications.',
            'Qwen J12+J13 offline validation', [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $report=Join-Path $dir ('stage-j-followup-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJFollowupPath,'run','--onnx-dir',$p.Onnx,'--audio',$audio,
                  '--ffmpeg',$ffmpeg,'--exporter',$exporter,'--exporter-receipt',$provenance,'--output',$report)
        $process=Invoke-ProcessCapture -FilePath $python -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $bundle=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $bundle -PathType Leaf)) { throw 'J12/J13 automatic diagnostic ZIP missing.' }
        Append-Log ('Qwen J12/J13 diagnostic ZIP (review before sharing): '+$bundle)
        if ($process.ExitCode -ne 0) {
            $failure=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            throw ('J12/J13 blocked at '+[string]$failure.failure_stage+' ('+[string]$failure.failure_code+'). See private receipt '+$report)
        }
        $post=Join-Path $dir ('builder-quality-postflight-j13-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $verify=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j13',
              '--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($verify.ExitCode -ne 0) { throw ('J12/J13 independent postflight failed. See '+$post) }
        Set-Status $lblQwenJFollowup 'J12/J13 FP32 PASS; compile LOCKED' 'good'
        Append-Log ('J12/J13 validated; QNN compilation not authorized. Report: '+$report)
    }
})


# -------------------- v0.3.84 responsive Qwen stage action panel --------------------
# One parent grid owns the action rows.  Never mix free-floating Y coordinates
# in the J1-J14 action area: these overlapped at Windows 125/150% DPI.
$btnQwenJ14 = New-Button 'RUN J14-J16 (offline, one click)' 0 0 400 46
$btnQwenJ17 = New-Button 'RUN J17-J19 (QNN preparation, offline)' 0 0 400 46
$btnQwenJ20 = New-Button 'RUN J20-J21 (operator probes, offline)' 0 0 400 46
$btnQwenJ22 = New-Button 'J22 - Compile small probes (Qualcomm cloud; approval)' 0 0 400 46
$btnQwenJ23 = New-Button 'RUN J23-J25 (real decoder block, offline)' 0 0 400 46
$btnQwenJ26 = New-Button 'RUN J26-J28 (FP16 real block, offline)' 0 0 400 46
$btnQwenJ29 = New-Button 'RUN J29-J31 (real FP16 QNN graph audit, offline)' 0 0 400 46
$btnQwenJ32 = New-Button 'RUN J32-J34 (real Slice/Expand contracts, offline)' 0 0 400 46
$lblQwenJ32 = New-StatusLabel 0 0 300
$btnQwenJ35 = New-Button 'RUN J35-J37 (Expand shape proofs, offline)' 0 0 400 46
$lblQwenJ35 = New-StatusLabel 0 0 300
$btnQwenJ38 = New-Button 'RUN J38-J40 (Expand provenance + local QAIRT, offline)' 0 0 400 46
$lblQwenJ38 = New-StatusLabel 0 0 300
$btnQwenJ41 = New-Button 'RUN J41-J42 (shared Expand probe, offline)' 0 0 400 46
$lblQwenJ41 = New-StatusLabel 0 0 300
$btnQwenJ43 = New-Button 'J43 - Compile shared probe (cloud; approval)' 0 0 400 46
$lblQwenJ43 = New-StatusLabel 0 0 300
$btnQwenJ44 = New-Button 'RUN J44-J45 (INT64-free fixed mask, offline)' 0 0 400 46
$lblQwenJ44 = New-StatusLabel 0 0 300
$btnQwenJ46 = New-Button 'J46 - Compile fixed mask probe (cloud; approval)' 0 0 400 46
$lblQwenJ46 = New-StatusLabel 0 0 300
$btnQwenJ47 = New-Button 'RUN J47-J48 (real FP16 block upload preflight, offline)' 0 0 400 46
$lblQwenJ47 = New-StatusLabel 0 0 300
$btnQwenJ49 = New-Button 'J49 - Compile REAL FP16 block (cloud + weight approval)' 0 0 400 46
$lblQwenJ49 = New-StatusLabel 0 0 300
$btnQwenJ50 = New-Button 'J50 - Diagnose existing FAILED J49 job (read-only)' 0 0 400 46
$lblQwenJ50 = New-StatusLabel 0 0 300
$btnQwenJ51 = New-Button 'RUN J51-J53 (REAL FP16 fixed Expand, offline)' 0 0 400 46
$lblQwenJ51 = New-StatusLabel 0 0 300
$btnQwenJ54 = New-Button 'RUN J54-J56 (static FP16 graph/liveness audit, offline)' 0 0 400 46
$lblQwenJ54 = New-StatusLabel 0 0 300
$btnQwenJ57 = New-Button 'RUN J57 (stage static FP16 position-8, offline)' 0 0 400 46
$lblQwenJ57 = New-StatusLabel 0 0 300
$btnQwenJ58 = New-Button 'J58-J59 Compile pos8 layer (CLOUD + weights approval)' 0 0 400 46
$lblQwenJ58 = New-StatusLabel 0 0 300
$btnQwenJ60 = New-Button 'RUN J60-J62 (fix QAIRT Reshape shape, offline)' 0 0 400 46
$lblQwenJ60 = New-StatusLabel 0 0 300
$btnQwenJ63 = New-Button 'RUN J63 (stage J61 Reshape graph, offline)' 0 0 400 46
$lblQwenJ63 = New-StatusLabel 0 0 300
$btnQwenJ64 = New-Button 'J64-J65 Compile NEW Reshape graph (CLOUD + weight consent)' 0 0 400 46
$lblQwenJ64 = New-StatusLabel 0 0 300
$btnQwenJ66 = New-Button 'RUN J66 (freeze ALL remaining Reshapes, offline)' 0 0 400 46
$lblQwenJ66 = New-StatusLabel 0 0 300
$btnQwenJ67 = New-Button 'J67-J68 Compile all-static Reshape (CLOUD + consent)' 0 0 400 46
$lblQwenJ67 = New-StatusLabel 0 0 300
$btnQwenJ69 = New-Button 'RUN J69 (fix HTP rank-0 integer Add, OFFLINE)' 0 0 400 46
$lblQwenJ69 = New-StatusLabel 0 0 300
$btnQwenJ70 = New-Button 'J70-J71 Compile scalar-Add candidate (CLOUD + consent)' 0 0 400 46
$lblQwenJ70 = New-StatusLabel 0 0 300
$btnQwenJ72 = New-Button 'J72 Download linked QNN model (NO compile)' 0 0 400 46
$lblQwenJ72 = New-StatusLabel 0 0 300
$btnQwenJ73 = New-Button 'J73 Run layer 0 on Galaxy S26 (HTP)' 0 0 400 46
$lblQwenJ73 = New-StatusLabel 0 0 300
$btnQwenJ74 = New-Button 'J74 Benchmark repeated S26 HTP layer 0 (offline)' 0 0 400 46
$lblQwenJ74 = New-StatusLabel 0 0 300
$btnQwenJ75 = New-Button 'J75-J76 Fuse real decoder layers 0+1 and check CPU parity (offline)' 0 0 400 46
$lblQwenJ75 = New-StatusLabel 0 0 300
$btnQwenJ77 = New-Button 'J77 Prepare fused 2-layer position-8 graph (offline)' 0 0 400 46
$lblQwenJ77 = New-StatusLabel 0 0 300
$btnQwenJ80 = New-Button 'J80 Audit two-layer ONNX input shapes (OFFLINE)' 0 0 400 46
$lblQwenJ80 = New-StatusLabel 0 0 300
$btnQwenJ81 = New-Button 'J81 Fix exact HTP scalar Add in fused 2-layer graph (OFFLINE)' 0 0 400 46
$lblQwenJ81 = New-StatusLabel 0 0 300
$btnQwenJ78 = New-Button 'J78-J79 Compile fused 2-layer graph (CLOUD + consent)' 0 0 400 46
$btnQwenJ82 = New-Button 'J82-J83 Compile corrected J81 fused 2-layer graph (CLOUD + consent)' 0 0 400 46
$btnQwenJ84 = New-Button 'J84 Download existing J83 fused context (NO compile)' 0 0 400 46
$lblQwenJ84 = New-StatusLabel 0 0 300
$btnQwenJ85 = New-Button 'J85 Validate + benchmark fused 2 layers on S26 (HTP)' 0 0 400 46
$lblQwenJ85 = New-StatusLabel 0 0 300
$btnQwenJ86 = New-Button 'J86 Tune original Qwen CPU decoder (HYBRID, OFFLINE)' 0 0 400 46
$lblQwenJ86 = New-StatusLabel 0 0 300
$lblQwenJ82 = New-StatusLabel 0 0 300
$lblQwenJ78 = New-StatusLabel 0 0 300
$lblQwenJ29 = New-StatusLabel 0 0 300
$lblQwenJ26 = New-StatusLabel 0 0 300
$lblQwenJ23 = New-StatusLabel 0 0 300
$lblQwenJ20 = New-StatusLabel 0 0 300
$lblQwenJ22 = New-StatusLabel 0 0 300
$lblQwenJ17 = New-StatusLabel 0 0 300
$lblQwenJFollowup = New-StatusLabel 0 0 300
$lblQwenJ14 = New-StatusLabel 0 0 300
$lblQwenJSelfTests = New-Label 'Offline regression suite (no model processing)' 0 0 310 35
$lblQwenJ14Hint = New-Label 'Audits J14, then builds bounded cache graph and checks CPU parity.' 0 0 310 42
$layoutQwenActions = New-Object System.Windows.Forms.TableLayoutPanel
$layoutQwenActions.Name = 'QwenStageActionsResponsiveGrid'
$layoutQwenActions.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,5650)
$layoutQwenActions.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(832,3660)
$layoutQwenActions.ColumnCount = 2
$layoutQwenActions.RowCount = 45
$layoutQwenActions.GrowStyle = [System.Windows.Forms.TableLayoutPanelGrowStyle]::FixedSize
$layoutQwenActions.CellBorderStyle = [System.Windows.Forms.TableLayoutPanelCellBorderStyle]::None
$layoutQwenActions.Anchor = ([System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right)
$layoutQwenActions.Padding = New-Object -TypeName System.Windows.Forms.Padding -ArgumentList @(0,6,0,6)
[void]$layoutQwenActions.ColumnStyles.Add((New-Object -TypeName System.Windows.Forms.ColumnStyle -ArgumentList @([System.Windows.Forms.SizeType]::Percent, [single]59)))
[void]$layoutQwenActions.ColumnStyles.Add((New-Object -TypeName System.Windows.Forms.ColumnStyle -ArgumentList @([System.Windows.Forms.SizeType]::Percent, [single]41)))
foreach ($i in 0..44) {
    [void]$layoutQwenActions.RowStyles.Add((New-Object -TypeName System.Windows.Forms.RowStyle -ArgumentList @([System.Windows.Forms.SizeType]::Absolute,[single]80)))
}
$gridChildren = @(
    @($btnQwenJBatch,          0,0), @($lblQwenJBatch,         1,0),
    @($btnQwenJFollowup,       0,1), @($lblQwenJFollowup,      1,1),
    @($btnQwenJ14,            0,2), @($lblQwenJ14,            1,2),
    @($btnQwenJ17,            0,3), @($lblQwenJ17,            1,3),
    @($btnQwenJ20,            0,4), @($lblQwenJ20,            1,4),
    @($btnQwenJ22,            0,5), @($lblQwenJ22,            1,5),
    @($btnQwenJ23,            0,6), @($lblQwenJ23,            1,6),
    @($btnQwenJ26,            0,7), @($lblQwenJ26,            1,7),
    @($btnQwenJ29,            0,8), @($lblQwenJ29,            1,8),
    @($btnQwenJ32,            0,9), @($lblQwenJ32,            1,9),
    @($btnQwenJ35,            0,10), @($lblQwenJ35,           1,10),
    @($btnQwenJ38,            0,11), @($lblQwenJ38,           1,11),
    @($btnQwenJ41,            0,12), @($lblQwenJ41,           1,12),
    @($btnQwenJ43,            0,13), @($lblQwenJ43,           1,13),
    @($btnQwenJ44,            0,14), @($lblQwenJ44,           1,14),
    @($btnQwenJ46,            0,15), @($lblQwenJ46,           1,15),
    @($btnQwenJ47,            0,16), @($lblQwenJ47,           1,16),
    @($btnQwenJ49,            0,17), @($lblQwenJ49,           1,17),
    @($btnQwenJ50,            0,18), @($lblQwenJ50,           1,18),
    @($btnQwenJ51,            0,19), @($lblQwenJ51,           1,19),
    @($btnQwenJ54,            0,20), @($lblQwenJ54,           1,20),
    @($btnQwenJ57,            0,21), @($lblQwenJ57,           1,21),
    @($btnQwenJ58,            0,22), @($lblQwenJ58,           1,22),
    @($btnQwenJ60,            0,23), @($lblQwenJ60,           1,23),
    @($btnQwenJ63,            0,24), @($lblQwenJ63,           1,24),
    @($btnQwenJ64,            0,25), @($lblQwenJ64,           1,25),
    @($btnQwenJ66,            0,26), @($lblQwenJ66,           1,26),
    @($btnQwenJ67,            0,27), @($lblQwenJ67,           1,27),
    @($btnQwenJ69,            0,28), @($lblQwenJ69,           1,28),
    @($btnQwenJ70,            0,29), @($lblQwenJ70,           1,29),
    @($btnQwenJ72,            0,30), @($lblQwenJ72,           1,30),
    @($btnQwenJ73,            0,31), @($lblQwenJ73,           1,31),
    @($btnQwenJ74,            0,32), @($lblQwenJ74,           1,32),
    @($btnQwenJ75,            0,33), @($lblQwenJ75,           1,33),
    @($btnQwenJ77,            0,34), @($lblQwenJ77,           1,34),
    @($btnQwenJ80,            0,35), @($lblQwenJ80,           1,35),
    @($btnQwenJ78,            0,36), @($lblQwenJ78,           1,36),
    @($btnQwenJ81,            0,37), @($lblQwenJ81,           1,37),
    @($btnQwenJ82,            0,38), @($lblQwenJ82,           1,38),
    @($btnQwenJ84,            0,39), @($lblQwenJ84,           1,39),
    @($btnQwenJ85,            0,40), @($lblQwenJ85,           1,40),
    @($btnQwenJ86,            0,41), @($lblQwenJ86,           1,41),
    @($btnQwenJSelfTests,     0,42), @($lblQwenJSelfTests,    1,42),
    @($btnQwenJBatchAudio,    0,43), @($lblQwenJBatchAudio,   1,43),
    @($btnQwenJBatchExtra,    0,44), @($chkQwenJBatchForce,  1,44)
)
foreach ($cell in $gridChildren) {
    $control=$cell[0]
    $control.Dock=[System.Windows.Forms.DockStyle]::Fill
    $control.Margin=New-Object -TypeName System.Windows.Forms.Padding -ArgumentList @(5,5,5,5)
    if ($control -is [System.Windows.Forms.Label]) {
        $control.AutoEllipsis = $true
        $control.TextAlign = [System.Drawing.ContentAlignment]::MiddleLeft
    }
    if ($control -is [System.Windows.Forms.Button]) {
        $control.AutoEllipsis = $true
        $control.UseMnemonic = $false
    }
    $layoutQwenActions.Controls.Add($control,[int]$cell[1],[int]$cell[2])
}
$tabQwenAsr.Controls.Add($layoutQwenActions)
$script:QwenAudioTip.SetToolTip($lblQwenJBatchAudio, 'Selected J2 recording (full path remains local)')

$btnQwenJ14.Add_Click({
    # J15/J16 needs the SAME selected audio as J12/J13, not another picker.
    $audio=([string]$txtQwenAsrAudio.Text).Trim()
    if (-not $audio -or -not (Test-Path -LiteralPath $audio -PathType Leaf) -or
        [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.ogg','.wav')) {
        try {
            if (-not (Select-QwenAsrValidationAudio -PathField $txtQwenAsrAudio -Title 'Choose previously validated J12 recording for J15/J16')) { return }
        } catch { Show-Error $_; return }
    }
    Invoke-UiAction 'Auditing Qwen J14 KV-input/output interface; no cloud, phone or model writes...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J14 requires the validated 0.6B model.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'J14 original ONNX folder missing.' }
        $folder=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $receipts=@(Get-ChildItem -LiteralPath $folder -Filter 'stage-j-followup-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        if ($receipts.Count -eq 0) { throw 'Run J12+J13 first; no existing local followup receipt found.' }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $folder ('stage-j14-kv-interface-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ14Path,'--j13-receipt',$receipts[0].FullName,'--onnx-dir',$p.Onnx,'--output',$report)
        $process=Invoke-ProcessCapture -FilePath $python -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 300
        $bundle=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $bundle -PathType Leaf)) { throw 'J14 diagnostic ZIP missing.' }
        Append-Log ('J14 offline diagnostic archive (review before sharing): '+$bundle)
        if ($process.ExitCode -ne 0) {
            $failure=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            Set-Status $lblQwenJ14 ('J14 blocked: '+[string]$failure.failure_code) 'warn'
            throw ('J14 blocked: '+[string]$failure.failure_code+'. See local receipt '+$report)
        }
        $post=Join-Path $folder ('builder-quality-postflight-j14-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $verification=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j14','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($verification.ExitCode -ne 0) { throw ('J14 independent postflight failed; see '+$post) }
        Set-Status $lblQwenJ14 'J14 I/O audited; hardware gate LOCKED' 'good'
        Append-Log ('J14 contract checked. Still NOT an NPU-resident KV implementation: '+$report)
        # J15 isolated bounded-host cache input; J16 REAL eight-step FP32 parity.
        # This stage does NOT implement NPU-resident KV: compiler gate stays closed.
        $audio=([string]$txtQwenAsrAudio.Text).Trim()
        $ffmpeg=([string]$txtFfmpeg.Text).Trim()
        $provenance=Join-Path $p.ModelFolder 'third-party-exporter-provenance.json'
        if (-not (Test-Path -LiteralPath $audio -PathType Leaf) -or
            -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf) -or
            -not (Test-Path -LiteralPath $provenance -PathType Leaf)) {
            throw 'J15/J16 needs the selected audio, FFmpeg and Stage C provenance. Original models remain untouched.'
        }
        $record=Get-Content -LiteralPath $provenance -Raw | ConvertFrom-Json
        if ($record.exporter_commit -ne '4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50') { throw 'J16 exporter revision mismatch.' }
        $exporter=[string]$record.folder
        if (-not (Test-Path -LiteralPath $exporter -PathType Container)) { throw 'J16 exporter folder missing.' }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'Run a NEW isolated bounded-host KV graph and compare up to eight decoder steps on the existing FP32 CPU? The source weights stay intact. This may use several GB of RAM and minutes of CPU time. There is NO cloud or phone operation.',
            'Qwen J15/J16 offline experiment', [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $bounded=Join-Path $folder ('stage-j15-j16-bounded-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ15J16Path,'--j14-receipt',$report,'--onnx-dir',$p.Onnx,
                  '--audio',$audio,'--ffmpeg',$ffmpeg,'--exporter',$exporter,
                  '--exporter-receipt',$provenance,'--output',$bounded)
        $run=Invoke-ProcessCapture -FilePath $python -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $boundedZip=[IO.Path]::ChangeExtension($bounded,'.zip')
        if (-not (Test-Path -LiteralPath $boundedZip -PathType Leaf)) { throw 'J15/J16 automatic diagnostic ZIP missing.' }
        Append-Log ('J15/J16 diagnostic archive (review before sharing): '+$boundedZip)
        if ($run.ExitCode -ne 0) {
            $failure=Get-Content -LiteralPath $bounded -Raw | ConvertFrom-Json
            Set-Status $lblQwenJ14 ('J15/J16 blocked: '+[string]$failure.failure_code) 'warn'
            throw ('J15/J16 blocked: '+[string]$failure.failure_code+'. See local receipt '+$bounded)
        }
        $post16=Join-Path $folder ('builder-quality-postflight-j16-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $checked=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j16',
                  '--output',$post16,'--combined-receipt',$bounded) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($checked.ExitCode -ne 0) { throw ('J15/J16 postflight failed. See local receipt '+$post16) }
        Set-Status $lblQwenJ14 'J14-J16 FP32 parity PASS; QNN LOCKED' 'good'
        Append-Log ('J15/J16 bounded-host interface parity complete. NPU cache residency NOT established: '+$bounded)

    }
})

$btnQwenJ17.Add_Click({
    Invoke-UiAction 'J17-J19: QNN graph ABI and native mock cache-owner audit; no cloud, phone or model writes...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J17-J19 requires the verified 0.6B source.' }
        $paths=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $paths.Onnx -PathType Container)) { throw 'J17 original ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $receipts=@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j15-j16-bounded-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        $previous = $null
        foreach ($item in $receipts) {
            try {
                $old=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($old.status -eq 'fp32_bounded_host_interface_parity_passed_compiler_locked') { $previous=$item; break }
            } catch { }
        }
        if ($null -eq $previous) { throw 'Run J14-J16 first or restore its successful JSON and ZIP receipt; model files stay untouched.' }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j17-j19-qnn-ready-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ17J19Path,'--j16-receipt',$previous.FullName,'--onnx-dir',$paths.Onnx,
                '--native-dir',(Join-Path $Root 'native'),'--output',$report)
        # J17 probe is optional: use the same canonical Stage F2 path, without requiring E4.
        $stageF=Get-QwenStageFProbePath -ModelFolder $paths.ModelFolder
        if (Test-Path -LiteralPath $stageF -PathType Leaf) { $args+=@('--stage-f-probe',$stageF) }
        $run=Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 600
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw 'J17-J19 diagnostic ZIP missing; check local logs.' }
        Append-Log ('J17-J19 shareable diagnostics (review before sharing): '+$archive)
        if ($run.ExitCode -ne 0) {
            $failure=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            Set-Status $lblQwenJ17 ('J17-J19 blocked: '+[string]$failure.failure_code) 'warn'
            throw ('J17-J19 stopped safely: '+[string]$failure.failure_code+'. Local receipt '+$report)
        }
        $post=Join-Path $dir ('builder-quality-postflight-j19-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $verify=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j19',
                '--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($verify.ExitCode -ne 0) { throw ('J17-J19 independent postflight blocked; review '+$post) }
        Set-Status $lblQwenJ17 'QNN ABI planned; cache mock PASS; compile LOCKED' 'good'
        Append-Log ('J17-J19 completed offline preparation only: '+$report+'. No native QNN cache or decoder compiled.')
    }
})

# J22 uses the existing Qualcomm AI Hub client environment, NOT the ONNX/export
# environment used by J20/J21. Check availability BEFORE cloud consent.
# Never install packages without a separate, explicit confirmation.
function Get-QwenJ22AiHubPython {
    $hubEnvPython = Join-Path $Root 'tools\qai-hub-env\Scripts\python.exe'
    if (Test-Path -LiteralPath $hubEnvPython -PathType Leaf) {
        try {
            $check = Test-PythonAiHubClient -PythonPath $hubEnvPython
            if ((Test-CompatiblePython -PythonPath $hubEnvPython) -and $check.ExitCode -eq 0) {
                Append-Log '[J22] Reusing existing Builder AI Hub client environment.'
                return $hubEnvPython
            }
        } catch {
            Append-Log '[J22] Existing AI Hub environment could not pass client import check.'
        }
    }
    $repairConsent = [System.Windows.Forms.MessageBox]::Show(
        'J22 requires the lightweight qai_hub Python client in the Builder-managed AI Hub environment. It is missing or cannot load. Repair it using the existing Setup workflow? This may download/install the client but will NOT submit a cloud job or modify model files. Choose No to stop safely and repair later from Setup.',
        'J22 prerequisite: AI Hub environment repair',
        [System.Windows.Forms.MessageBoxButtons]::YesNo,
        [System.Windows.Forms.MessageBoxIcon]::Question)
    if ($repairConsent -ne [System.Windows.Forms.DialogResult]::Yes) {
        Append-Log '[J22] AI Hub environment repair declined; no cloud submission attempted.'
        return $null
    }
    $python = Ensure-AiHubEnvironmentFromUi
    if (-not $python -or -not (Test-Path -LiteralPath $python -PathType Leaf)) {
        throw 'J22_AI_HUB_ENVIRONMENT_NOT_FOUND: Setup > Install/repair AI Hub client.'
    }
    $check = Test-PythonAiHubClient -PythonPath $python
    if ($check.ExitCode -ne 0) {
        throw 'J22_AI_HUB_CLIENT_MISSING: Setup > Install/repair AI Hub client in Builder-managed environment.'
    }
    Append-Log '[J22] Builder-managed AI Hub client import verified; submission remains approval-gated.'
    return $python
}

# v0.3.87 J20/J21 synthetic operations probes; J22 is a separate confirmed cloud action.
$btnQwenJ20.Add_Click({
    Invoke-UiAction 'J20/J21: create and CPU-verify tiny synthetic ONNX operator probes (offline)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J20 is limited to Qwen 0.6B SM8850/V81 evidence.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $receipts=@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j17-j19-qnn-ready-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        $previous=$null
        foreach ($item in $receipts) {
            try {
                $old=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($old.status -eq 'offline_qnn_interface_prototype_pass_compiler_locked') { $previous=$item; break }
            } catch { }
        }
        if ($null -eq $previous) { throw 'Run J17-J19 first or restore its successful receipt and ZIP.' }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j20-j21-operator-probes-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageJ20J22Path,'offline','--j19-receipt',$previous.FullName,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 600
        $zip=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $zip -PathType Leaf)) { throw 'J20/J21 diagnostic ZIP missing.' }
        Append-Log ('J20/J21 synthetic ONNX audit ZIP (review before sharing): '+$zip)
        if ($run.ExitCode -ne 0) {
            $failure=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            throw ('J20/J21 blocked: '+[string]$failure.failure_code+'. Local details: '+$report)
        }
        $post=Join-Path $dir ('builder-quality-postflight-j21-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $verified=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j21','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($verified.ExitCode -ne 0) { throw ('Independent J21 gate blocked; see '+$post) }
        Set-Status $lblQwenJ20 'Synthetic CPU probes PASS; Qualcomm untested' 'good'
        Append-Log ('QNN microprobes ready for OPTIONAL cloud experiment: '+$report)
    }
})
$btnQwenJ22.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J22 requires Qwen 0.6B evidence.'; return }
    # Refuse converter-rejected dynamic Slice probes BEFORE cloud approval.
    $probeDir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $probeReady=$false
    foreach ($probe in @(Get-ChildItem -LiteralPath $probeDir -Filter 'stage-j20-j21-operator-probes-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $data=Get-Content -LiteralPath $probe.FullName -Raw | ConvertFrom-Json
            if ($data.status -eq 'synthetic_operator_cpu_parity_pass_compiler_locked' -and
                $data.probe_contract_revision -eq 3) { $probeReady=$true; break }
        } catch { }
    }
    if (-not $probeReady) { Show-Error 'J22 requires the new fixed-mask attention probe. Run J20-J21 offline once in v0.3.91 before cloud approval.'; return }
    try { $hubPython = Get-QwenJ22AiHubPython } catch { Show-Error $_; return }
    if (-not $hubPython) { return }
    $confirmation=[System.Windows.Forms.MessageBox]::Show(
        'Submit up to THREE SMALL synthetic ONNX operator probes (reuse prior Gather jobs after read-only remote verification where possible) to Qualcomm AI Hub for the EXACT Samsung Galaxy S26, QAIRT 2.50, with --truncate_64bit_io? This contacts a remote service and might consume credits. No Qwen weights or recordings will be uploaded. This does NOT compile the Qwen decoder or prove KV residency. Continue?',
        'Explicit approval: Qualcomm cloud submission',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($confirmation -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J22: uploading only tiny synthetic operator probes to Qualcomm after explicit approval...' {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $receipts=@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j20-j21-operator-probes-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        $previous=$null
        foreach ($item in $receipts) {
            try {
                $old=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($old.status -eq 'synthetic_operator_cpu_parity_pass_compiler_locked' -and $old.probe_contract_revision -eq 3) { $previous=$item; break }
            } catch { }
        }
        if ($null -eq $previous) { throw 'J22 requires a validated J20/J21 JSON and ZIP; run J20/J21 offline first.' }
        $python=$hubPython
        $report=Join-Path $dir ('stage-j22-cloud-operator-probes-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        # A previous Gather compile+link may be rechecked remotely and reused.
        # Never skip verification solely on local JSON text or stale graph hashes.
        $args=@($script:QwenAsrStageJ20J22Path,'cloud','--offline-receipt',$previous.FullName,'--approved-qualcomm-submission','--output',$report)
        $priorCloud=@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j22-cloud-operator-probes-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        foreach ($oldFile in $priorCloud) {
            try {
                $old=Get-Content -LiteralPath $oldFile.FullName -Raw | ConvertFrom-Json
                if ($old.target.soc -eq 'SM8850' -and $old.probes.Count -ge 1 -and
                    $old.probes[0].probe -eq 'int64_gather_fp16' -and
                    $old.probes[0].outcome -eq 'compile_and_link_status_verified') {
                    $args+=@('--previous-cloud-receipt',$oldFile.FullName)
                    Append-Log ('[J22] Rechecking prior successful Gather job IDs; no new Gather submission if verified: '+$oldFile.FullName)
                    break
                }
            } catch { }
        }
        $run=Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $zip=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $zip -PathType Leaf)) { throw 'J22 diagnostics ZIP missing; check Builder logs.' }
        Append-Log ('Qualcomm synthetic operator result ZIP (review before sharing): '+$zip)
        $post=Join-Path $dir ('builder-quality-postflight-j22-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $verified=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j22','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($verified.ExitCode -ne 0) { throw ('Independent J22 status/privacy gate failed; inspect '+$post) }
        Append-Log ('Independent J22 status/privacy postflight verified: '+$post)
        if ($run.ExitCode -ne 0) {
            $failure=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            Set-Status $lblQwenJ22 'Cloud probe blocked/unsupported; review receipt' 'warn'
            throw ('J22 blocked/failed; inspect local JSON receipt and sanitized ZIP: '+$report)
        }
        Set-Status $lblQwenJ22 'All probe compile + link statuses verified; decoder unqualified' 'warn'
        Append-Log ('Synthetic operator compile AND link statuses verified; decoder remains unqualified: '+$report)
    }
})

# J23-J25: real ONNX layer-0 extraction and CPU reference comparison ONLY.
# Deliberately no Qualcomm cloud action for weight-bearing decoder graphs.
$btnQwenJ23.Add_Click({
    Invoke-UiAction 'J23-J25: inspect real Qwen decoder layer, run original-model CPU comparison, report QNN blockers (offline only)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J23 requires the previously qualified 0.6B export.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX directory missing; do not re-export.' }
        $python=Ensure-QwenAsrExportPython
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $question=[System.Windows.Forms.MessageBox]::Show(
            'J23-J25 extracts actual first-layer ONNX nodes beside the ORIGINAL decoder and runs a CPU comparison with the unchanged weights. This can load multiple GB of original FP32 data and take several minutes. It WILL NOT upload model weights, submit cloud jobs, write to the phone, re-export models or claim NPU-resident KV. Proceed?',
            'Real Qwen decoder block (OFFLINE)',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($question -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $report=Join-Path $dir ('stage-j23-j25-real-block-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ23J25Path,'--onnx-dir',$p.Onnx,'--output',$report)
        $result=Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('J23/J25 diagnostic ZIP missing: '+$report) }
        Append-Log ('J23-J25 shareable diagnostics (no weights/audio): '+$archive)
        if ($result.ExitCode -ne 0) {
            $receipt=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
            Set-Status $lblQwenJ23 ('Real block gated: '+[string]$receipt.status) 'warn'
            throw ('J23-J25 blocked safely. See local receipt '+$report+'. No cloud jobs.')
        }
        $post=Join-Path $dir ('builder-quality-postflight-j25-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $verified=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j25',
                '--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($verified.ExitCode -ne 0) { throw ('J25 independent postflight failed: '+$post) }
        Set-Status $lblQwenJ23 'Real block FP32 PASS; FP16/QNN still LOCKED' 'good'
        Append-Log ('J23-J25 CPU reference verified; J25 model-weight cloud approval not implemented: '+$report)
    }
})

# J26-J28: true local FP16 weighted layer candidate, CPU parity at nonzero KV,
# QNN compilation plan only. Explicitly NEVER uploads weights or submits jobs.
$btnQwenJ26.Add_Click({
    Invoke-UiAction 'J26-J28: create isolated FP16 decoder block and compare CPU numerics (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J26 requires the qualified 0.6B decoder.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX directory missing. Preserve source weights.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $dir -PathType Container)) { throw 'J23-J25 receipts missing; run J23-J25 first.' }
        $prior=$null
        foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j23-j25-real-block-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($obj.stage -eq 'J23+J24+J25' -and $obj.status -eq 'real_block_fp32_parity_passed_compilation_locked' -and $obj.j24.fp32_parity_passed -eq $true) {
                    $prior=$item.FullName
                    break
                }
            } catch { Append-Log ('Skipping unreadable J23 receipt: '+$item.Name) }
        }
        if (-not $prior) { throw 'Successful J23-J25 JSON receipt not found. Preserve older stage receipts.' }
        $answer=[System.Windows.Forms.MessageBox]::Show(
          'J26-J28 makes a NEW isolated FP16 copy of ONLY layer-0 weights and runs original-model CPU parity at three token positions with nonzero KV. It does not alter original weights, submit Qualcomm jobs, access the phone, or unlock model-weight uploads. This can take several minutes and use significant RAM. Proceed?',
          'J26-J28 real FP16 candidate (OFFLINE)',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j26-j28-fp16-block-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ26J28Path,'--onnx-dir',$p.Onnx,'--j25-receipt',$prior,'--output',$report)
        $ran=Invoke-ProcessCapture -FilePath $python -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('J26-J28 diagnostic ZIP missing; see local output: '+$report) }
        Append-Log ('J26-J28 sanitized diagnostic ZIP (NO model weights): '+$archive)
        if ($ran.ExitCode -ne 0) {
            Set-Status $lblQwenJ26 'FP16 gate blocked; inspect local receipt' 'warn'
            throw ('J26-J28 blocked safely; inspect local JSON '+$report+'. No Qualcomm jobs.')
        }
        $post=Join-Path $dir ('builder-quality-postflight-j28-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $checked=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j28','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($checked.ExitCode -ne 0) { throw ('Independent J28 postflight failed; inspect '+$post) }
        Set-Status $lblQwenJ26 'FP16 CPU PASS; real QNN compile still LOCKED' 'good'
        Append-Log ('J26-J28 FP16 CPU verification complete, no cloud approval granted: '+$report)
    }
})

# J29-J31: audit REAL weighted FP16 graph for static no-op Slice/Expand,
# verify exact FP16 parity at nonzero KV, and prepare a NO-UPLOAD QAIRT plan.
$btnQwenJ29.Add_Click({
    Invoke-UiAction 'J29-J31: conservative real FP16 graph rewrites and exact CPU parity (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J29 requires the qualified 0.6B decoder.' }
        $p=Get-QwenStageEData
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX directory unavailable.' }
        if (-not (Test-Path -LiteralPath $dir -PathType Container)) { throw 'J23 and J28 receipts missing.' }
        $j25=$null
        foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j23-j25-real-block-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($obj.stage -eq 'J23+J24+J25' -and $obj.status -eq 'real_block_fp32_parity_passed_compilation_locked' -and $obj.j24.fp32_parity_passed -eq $true) { $j25=$item.FullName; break }
            } catch { Append-Log ('Skipping invalid J25 receipt: '+$item.Name) }
        }
        $j28=$null
        foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j26-j28-fp16-block-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($obj.stage -eq 'J26+J27+J28' -and $obj.status -eq 'real_fp16_block_cpu_parity_passed_compilation_locked' -and $obj.j27.fp16_cpu_parity_passed -eq $true) { $j28=$item.FullName; break }
            } catch { Append-Log ('Skipping invalid J28 receipt: '+$item.Name) }
        }
        if (-not $j25 -or -not $j28) { throw 'Verified J23 and J28 success receipts required. Do not delete JSON receipts.' }
        $yes=[System.Windows.Forms.MessageBox]::Show(
            'J29-J31 will inspect your REAL FP16 decoder block, rewrite ONLY statically proven no-op Slice/Expand operations, and run exact FP16 CPU parity at three nonzero KV lengths. It can use significant memory/time. Remaining dynamic operators BLOCK real-weight Qualcomm submission. NO model weights uploaded, cloud jobs submitted, or phone/model files changed. Proceed?',
            'J29-J31 (OFFLINE; no model upload)',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($yes -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j29-j31-qnn-real-block-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ29J31Path,'--onnx-dir',$p.Onnx,'--j25-receipt',$j25,'--j28-receipt',$j28,'--output',$report)
        $ran=Invoke-ProcessCapture -FilePath $python -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('J29-J31 diagnostic ZIP missing: '+$report) }
        Append-Log ('J29-J31 sanitized diagnostics, no ONNX or weights: '+$archive)
        if ($ran.ExitCode -ne 0) {
            Set-Status $lblQwenJ29 'Real FP16 graph gate blocked; inspect receipt' 'warn'
            throw ('J29-J31 blocked safely. Local details: '+$report+'. No cloud jobs.')
        }
        $post=Join-Path $dir ('builder-quality-postflight-j31-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j31','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($gate.ExitCode -ne 0) { throw ('J31 independent postflight blocked: '+$post) }
        $receipt=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if (@($receipt.j29.remaining_risk_ops.PSObject.Properties | Where-Object { [int]$_.Value -gt 0 }).Length -gt 0) {
            Set-Status $lblQwenJ29 'FP16 parity PASS; dynamic QAIRT ops still BLOCKED' 'warn'
        } else {
            Set-Status $lblQwenJ29 'FP16 parity PASS; weighted upload still LOCKED' 'good'
        }
        Append-Log ('J29-J31 complete (no cloud permission granted): '+$report)
    }
})

# J32-J34: per-node real FP16 Slice/Expand contracts, proof-limited local graph
# rewrites, exact FP16 parity and locked Qualcomm preparation. No uploads.
$btnQwenJ32.Add_Click({
    Invoke-UiAction 'J32-J34: inspect real Slice/Expand contracts and test exact FP16 parity (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J32 requires the qualified 0.6B decoder.' }
        $p=Get-QwenStageEData
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX directory unavailable.' }
        if (-not (Test-Path -LiteralPath $dir -PathType Container)) { throw 'J25/J28/J31 receipts missing.' }
        $j25=$null; $j28=$null; $j31=$null
        foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j23-j25-real-block-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($obj.stage -eq 'J23+J24+J25' -and $obj.status -eq 'real_block_fp32_parity_passed_compilation_locked' -and $obj.j24.fp32_parity_passed -eq $true) { $j25=$item.FullName; break }
            } catch { Append-Log ('Skipping invalid J25 receipt: '+$item.Name) }
        }
        foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j26-j28-fp16-block-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($obj.stage -eq 'J26+J27+J28' -and $obj.status -eq 'real_fp16_block_cpu_parity_passed_compilation_locked' -and $obj.j27.fp16_cpu_parity_passed -eq $true) { $j28=$item.FullName; break }
            } catch { Append-Log ('Skipping invalid J28 receipt: '+$item.Name) }
        }
        foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j29-j31-qnn-real-block-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                if ($obj.stage -eq 'J29+J30+J31' -and $obj.status -eq 'real_fp16_rewrite_parity_passed_compilation_locked' -and $obj.j30.parity_passed -eq $true) { $j31=$item.FullName; break }
            } catch { Append-Log ('Skipping invalid J31 receipt: '+$item.Name) }
        }
        if (-not $j25 -or -not $j28 -or -not $j31) { throw 'Successful J25, J28, J31 JSON receipts are required. Do not delete JSON receipts.' }
        $yes=[System.Windows.Forms.MessageBox]::Show(
            'J32-J34 inspects each REAL FP16 Slice/Expand operation, applies ONLY proof-limited local graph rewrites, and compares exact FP16 CPU outputs at three nonzero KV lengths. Processing can require significant RAM and time. No original weights, cloud submissions, or phone files will change. Proceed?',
            'J32-J34 (OFFLINE; cloud LOCKED)',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($yes -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j32-j34-real-node-contracts-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ32J34Path,'--onnx-dir',$p.Onnx,'--j25-receipt',$j25,'--j28-receipt',$j28,'--j31-receipt',$j31,'--output',$report)
        $ran=Invoke-ProcessCapture -FilePath $python -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('J32-J34 shareable ZIP missing. Local report: '+$report) }
        Append-Log ('J32-J34 sanitized per-node diagnostics (no graph or weights): '+$archive)
        if ($ran.ExitCode -ne 0) {
            Set-Status $lblQwenJ32 'J32-J34 safely blocked; inspect local receipt' 'warn'
            throw ('J32-J34 stopped safely: '+$report+'. No cloud jobs.')
        }
        $post=Join-Path $dir ('builder-quality-postflight-j34-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $python -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j34','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($gate.ExitCode -ne 0) { throw ('J34 independent postflight blocked: '+$post) }
        $receipt=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        $riskCount=@($receipt.j32.inspected_nodes | Where-Object { $_.decision -eq 'unresolved' }).Count
        Set-Status $lblQwenJ32 ('FP16 exact PASS; '+$riskCount+' real QNN risks remain; cloud LOCKED') 'warn'
        Append-Log ('J32-J34 per-node audit complete; no cloud permission: '+$report)
    }
})

# J35-J37: shape-proven Expand -> constant-repeat Tile, constant Slice audit,
# exact FP16 CPU parity and independent cloud-locked diagnostic postflight.
$btnQwenJ35.Add_Click({
    Invoke-UiAction 'J35-J37: qualify real Slice and Expand shape chains (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J35 requires the qualified 0.6B decoder.' }
        $p=Get-QwenStageEData
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX directory unavailable.' }
        if (-not (Test-Path -LiteralPath $dir -PathType Container)) { throw 'Previous successful Stage J receipts missing.' }
        $j25=$null; $j28=$null; $j31=$null; $j34=$null
        foreach ($spec in @(
            @('stage-j23-j25-real-block-*.json','J23+J24+J25','real_block_fp32_parity_passed_compilation_locked','j25'),
            @('stage-j26-j28-fp16-block-*.json','J26+J27+J28','real_fp16_block_cpu_parity_passed_compilation_locked','j28'),
            @('stage-j29-j31-qnn-real-block-*.json','J29+J30+J31','real_fp16_rewrite_parity_passed_compilation_locked','j31'),
            @('stage-j32-j34-real-node-contracts-*.json','J32+J33+J34','real_block_contracts_fp16_parity_passed_compilation_locked','j34')
        )) {
            $found=$null
            foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter $spec[0] -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                    if ($obj.stage -eq $spec[1] -and $obj.status -eq $spec[2] -and
                        $obj.compile_submission_allowed -eq $false) { $found=$item.FullName; break }
                } catch { Append-Log ('Ignoring malformed Stage J receipt: '+$item.Name) }
            }
            if (-not $found) { throw ('Required successful '+$spec[1]+' receipt missing. Preserve local JSON receipts.') }
            switch ($spec[3]) { 'j25' {$j25=$found} 'j28' {$j28=$found} 'j31' {$j31=$found} 'j34' {$j34=$found} }
        }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'J35-J37 audits constant real Slice nodes and symbolically proves any Expand -> Tile rewrite, then compares exact FP16 CPU outputs at three nonzero KV lengths. Tile remains UNQUALIFIED for Qualcomm. May take several minutes and substantial RAM. No original model changes, cloud jobs, or phone actions. Proceed?',
            'J35-J37 (OFFLINE; cloud LOCKED)',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j35-j37-shape-proof-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ35J37Path,'--onnx-dir',$p.Onnx,'--j25-receipt',$j25,'--j28-receipt',$j28,'--j31-receipt',$j31,'--j34-receipt',$j34,'--output',$report)
        $ran=Invoke-ProcessCapture -FilePath $py -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('J35-J37 diagnostic ZIP missing: '+$report) }
        Append-Log ('J35-J37 sanitized shape proof diagnostics: '+$archive)
        if ($ran.ExitCode -ne 0) {
            Set-Status $lblQwenJ35 'J35-J37 blocked safely; inspect local report' 'warn'
            throw ('J35-J37 stopped: '+$report+'. No cloud jobs.')
        }
        $post=Join-Path $dir ('builder-quality-postflight-j37-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j37','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($gate.ExitCode -ne 0) { throw ('J37 independent postflight blocked: '+$post) }
        $receipt=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        $count=[int]$receipt.j35.rewrites
        Set-Status $lblQwenJ35 ('Exact FP16 PASS; '+$count+' proven rewrite(s); QNN LOCKED') 'warn'
        Append-Log ('J35-J37 complete. No Qualcomm qualification or upload approval: '+$report)
    }
})

# J38-J40: shape-provenance trace, only proven rewrites, FP16 parity,
# optional LOCAL converter; neither remote model upload nor cloud compile.
$btnQwenJ38.Add_Click({
    Invoke-UiAction 'J38-J40: trace real Expand shape provenance (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J38 requires qualified 0.6B decoder.' }
        $p=Get-QwenStageEData
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container) -or -not (Test-Path -LiteralPath $dir -PathType Container)) { throw 'Prior ONNX and Stage J workspace required.' }
        $receipts=@{}
        foreach ($spec in @(
            @('j25','stage-j23-j25-real-block-*.json','J23+J24+J25','real_block_fp32_parity_passed_compilation_locked'),
            @('j28','stage-j26-j28-fp16-block-*.json','J26+J27+J28','real_fp16_block_cpu_parity_passed_compilation_locked'),
            @('j31','stage-j29-j31-qnn-real-block-*.json','J29+J30+J31','real_fp16_rewrite_parity_passed_compilation_locked'),
            @('j34','stage-j32-j34-real-node-contracts-*.json','J32+J33+J34','real_block_contracts_fp16_parity_passed_compilation_locked'),
            @('j37','stage-j35-j37-shape-proof-*.json','J35+J36+J37','real_fp16_shape_proof_parity_passed_cloud_locked')
        )) {
            $found=$null
            foreach ($item in @(Get-ChildItem -LiteralPath $dir -Filter $spec[1] -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $obj=Get-Content -LiteralPath $item.FullName -Raw | ConvertFrom-Json
                    if ($obj.stage -eq $spec[2] -and $obj.status -eq $spec[3] -and $obj.compile_submission_allowed -eq $false) { $found=$item.FullName;break }
                } catch { Append-Log ('Skipping invalid '+$spec[0]+' receipt: '+$item.Name) }
            }
            if (-not $found) { throw ('Successful '+$spec[0]+' JSON receipt missing. Older ZIPs need not be retained.') }
            $receipts[$spec[0]]=$found
        }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'J38-J40 traces both real Expand target shape chains, rewrites ONLY proven-safe patterns, and runs exact FP16 CPU parity. If a LOCAL QAIRT converter is installed, J40 will attempt a local-only ONNX conversion with existing private weights, without cloud jobs or uploads. No original model files are modified. This may need substantial RAM. Proceed?',
            'J38-J40 (LOCAL ONLY; cloud LOCKED)',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j38-j40-expand-provenance-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $params=@($script:QwenAsrStageJ38J40Path,'--onnx-dir',$p.Onnx,'--j25-receipt',$receipts['j25'],
                  '--j28-receipt',$receipts['j28'],'--j31-receipt',$receipts['j31'],
                  '--j34-receipt',$receipts['j34'],'--j37-receipt',$receipts['j37'],'--output',$report)
        $ran=Invoke-ProcessCapture -FilePath $py -Arguments $params -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $archive=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) { throw ('J38-J40 sanitized diagnostics missing: '+$report) }
        Append-Log ('J38-J40 sanitized shape provenance ZIP: '+$archive)
        if ($ran.ExitCode -ne 0) {
            Set-Status $lblQwenJ38 'J38-J40 blocked; inspect private receipt' 'warn'
            throw ('J38-J40 safely blocked: '+$report+'. No cloud jobs.')
        }
        $post=Join-Path $dir ('builder-quality-postflight-j40-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j40','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($gate.ExitCode -ne 0) { throw ('J40 independent postflight blocked: '+$post) }
        $receipt=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        $unresolved=@($receipt.j38.expand_shape_traces | Where-Object { $_.proof_complete -eq $false }).Count
        Set-Status $lblQwenJ38 ('FP16 exact PASS; '+$unresolved+' unresolved Expand shapes; local converter: '+[string]$receipt.j40.status+'; cloud LOCKED') 'warn'
        Append-Log ('J38-J40 completed; no cloud permission: '+$report)
    }
})

# J41/J42: model-free, structurally derived shared Expand operator probe.
# Reuse existing successful J40 receipt; no original model or QAIRT SDK needed.
$btnQwenJ41.Add_Click({
    Invoke-UiAction 'J41-J42: weight-free shared Expand CPU probe (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J41 requires the qualified Qwen 0.6B evidence.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $dir -PathType Container)) { throw 'Existing Stage J workspace missing.' }
        $previous=$null
        foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j38-j40-expand-provenance-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                if ($v.stage -eq 'J38+J39+J40' -and $v.status -eq 'real_fp16_expand_provenance_parity_passed_cloud_locked' -and $v.compile_submission_allowed -eq $false) { $previous=$f.FullName;break }
            } catch { }
        }
        if (-not $previous) { throw 'Valid J40 JSON receipt missing. Older ZIP files may be deleted.' }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'Build one tiny WEIGHT-FREE ONNX probe matching the observed Add -> Reshape -> Concat -> Expand shape-chain topology. J42 tests valid cache lengths including 255 and blocks overflow. This uses SYNTHETIC constants, not the exact Qwen dimensions. No uploads, cloud jobs, or original model changes. Continue?',
            'J41-J42 OFFLINE', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j41-j42-shared-expand-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ41J43Path,'offline','--j40-receipt',$previous,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 600
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J41-J42 diagnostic ZIP missing.' }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ41 'CPU probe blocked; inspect local report' 'warn';throw ('J41-J42 safely blocked: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j42-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j42','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('Independent J42 verification failed: '+$post) }
        Set-Status $lblQwenJ41 'Synthetic CPU probe PASS; cloud not submitted' 'good'
        Append-Log ('J41-J42 shareable diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J43 is deliberately a DIFFERENT button and requires a NEW explicit approval.
$btnQwenJ43.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J43 only qualifies SM8850/V81 0.6B evidence.'; return }
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $previous=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j41-j42-shared-expand-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J41+J42' -and $v.status -eq 'weight_free_shared_expand_cpu_passed_cloud_unsubmitted' -and $v.compile_submission_allowed -eq $false) { $previous=$f.FullName;break }
        } catch { }
    }
    if (-not $previous) { Show-Error 'J43 requires successful J41-J42 offline receipt first.';return }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $approval=[System.Windows.Forms.MessageBox]::Show(
        'EXPLICIT CLOUD APPROVAL: submit ONE tiny WEIGHT-FREE synthetic shared-Expand ONNX graph to Qualcomm AI Hub for Samsung Galaxy S26 (SM8850/V81, QAIRT 2.50). Cloud service may consume credits. NO Qwen model weights, audio or decoder files will be uploaded. Compiler and linker results will be checked independently. The result does NOT qualify the real decoder or NPU-resident KV cache. Submit?',
        'J43 Qualcomm cloud submission - separate approval',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($approval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J43: submit ONLY the weight-free Expand probe to Qualcomm after approval...' {
        $report=Join-Path $dir ('stage-j43-shared-expand-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:QwenAsrStageJ41J43Path,'cloud','--j42-receipt',$previous,'--approve-cloud','--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J43 diagnostic ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j43-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j43','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J43 independent cloud status/privacy gate failed: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ43 'Compile/link failed; see report' 'warn';throw ('J43 stopped: '+$report) }
        Set-Status $lblQwenJ43 'Synthetic compile + link PASS; decoder unqualified' 'warn'
        Append-Log ('J43 sanitized cloud diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})


# J44-J45: fixed-capacity alternative. J43 verified failed linking INT64.
# Only synthetic ONNX is written; no Qwen weights are read or uploaded.
$btnQwenJ44.Add_Click({
    Invoke-UiAction 'J44-J45: FP16 fixed-cache masked broadcast probe (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J44 requires the qualified Qwen 0.6B evidence.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $previous=$null
        foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j43-shared-expand-cloud-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                if ($v.stage -eq 'J43' -and $v.status -eq 'link_failed' -and $v.error_category -eq 'int64_link_boundary_unsupported' -and $v.compile_status.verified_success -eq $true) { $previous=$f.FullName;break }
            } catch { }
        }
        if (-not $previous) { throw 'J44 requires the actual J43 link-failure JSON receipt. Old audit ZIPs are optional.' }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'Build one tiny synthetic FP16 fixed-256 broadcast plus runtime mask probe with NO INT64 tensor anywhere. J45 compares its valid-token prefix and padded zeros to the dynamic Expand CPU reference. This does NOT prove real Qwen decoder compatibility. No uploads or model changes. Continue?',
            'J44-J45 OFFLINE',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j44-j45-fixed-mask-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ44J46Path,'offline','--j43-receipt',$previous,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 600
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J44-J45 diagnostic ZIP missing.' }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ44 'CPU probe blocked; see report' 'warn';throw ('J44-J45 safely blocked: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j45-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j45','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('Independent J45 validation failed: '+$post) }
        Set-Status $lblQwenJ44 'FP16 fixed-mask CPU PASS; cloud not submitted' 'good'
        Append-Log ('J44-J45 shareable diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J46 is a separate consented cloud action, not part of offline J44-J45.
$btnQwenJ46.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J46 only qualifies SM8850/V81 0.6B evidence.';return }
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $previous=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j44-j45-fixed-mask-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J44+J45' -and $v.status -eq 'int64_free_fixed_mask_cpu_passed_cloud_unsubmitted' -and $v.compile_submission_allowed -eq $false) { $previous=$f.FullName;break }
        } catch { }
    }
    if (-not $previous) { Show-Error 'J46 requires the successful J44-J45 offline receipt.';return }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $approval=[System.Windows.Forms.MessageBox]::Show(
        'EXPLICIT CLOUD APPROVAL: Submit ONE small weight-free FP16 fixed-capacity masked broadcast probe to Qualcomm AI Hub for Samsung Galaxy S26 (SM8850/V81, QAIRT 2.50). Cloud may consume credits. NO Qwen model weights or recordings. Check compile AND link separately. This tests an ALTERNATIVE design, NOT dynamic Expand or real decoder qualification. Submit?',
        'J46 Qualcomm cloud submission - separate approval',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($approval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J46: submit ONLY the tiny approved synthetic fixed-mask probe...' {
        $report=Join-Path $dir ('stage-j46-fixed-mask-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:QwenAsrStageJ44J46Path,'cloud','--j45-receipt',$previous,'--approve-cloud','--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J46 diagnostic ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j46-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j46','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('Independent J46 cloud status/privacy gate failed: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ46 'Compile/link failed; see report' 'warn';throw ('J46 stopped: '+$report) }
        Set-Status $lblQwenJ46 'Fixed-mask synthetic compile+link PASS; decoder unqualified' 'warn'
        Append-Log ('J46 sanitized cloud diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J47/J48 is OFFLINE ONLY. It stages only the already validated 12.6MB
# real FP16 layer-0 graph/sidecar and profiles genuine position-8 boundary
# inputs; no Qualcomm submission, model upload, phone action, or model rewrite.
$btnQwenJ47.Add_Click({
    Invoke-UiAction 'J47-J48: stage verified real FP16 decoder block privately (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J47 requires the validated 0.6B model.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original Qwen ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $items=@(
            [pscustomobject]@{Filter='stage-j23-j25-real-block-*.json';Stage='J23+J24+J25';Status='real_block_fp32_parity_passed_compilation_locked'},
            [pscustomobject]@{Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked'},
            [pscustomobject]@{Filter='stage-j38-j40-expand-provenance-*.json';Stage='J38+J39+J40';Status='real_fp16_expand_provenance_parity_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j46-fixed-mask-cloud-*.json';Stage='J46';Status='fixed_masked_fp16_compile_and_link_verified'}
        )
        $matched=@()
        foreach ($item in $items) {
            $receipt=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $item.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $item.Stage -and $v.status -eq $item.Status) { $receipt=$f.FullName;break }
                } catch { }
            }
            if (-not $receipt) { throw ('Successful previous JSON receipt missing: '+$item.Filter+'. Older ZIPs are optional.') }
            $matched += [string]$receipt
        }
        $ok=[System.Windows.Forms.MessageBox]::Show(
            'Prepare an isolated REAL Qwen FP16 layer-0 ONNX package (about 12.6 MB), using verified existing candidate weights. J48 measures concrete boundary tensors at position 8 with nonzero KV; this may take minutes and several GB RAM. NO cloud uploads, model changes or phone actions. Continue?',
            'J47-J48 REAL BLOCK OFFLINE',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($ok -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j47-j48-real-fp16-package-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ47J49Path,'offline','--onnx-dir',$p.Onnx,
                '--j25-receipt',$matched[0],'--j28-receipt',$matched[1],
                '--j40-receipt',$matched[2],'--j46-receipt',$matched[3],'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 3600
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J47-J48 sanitized diagnostic missing.' }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ47 'Real FP16 staging blocked; inspect local receipt' 'warn';throw ('J47-J48 safely blocked: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j48-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j48','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J48 independent receipt gate failed: '+$post) }
        Set-Status $lblQwenJ47 'Real FP16 input profile PASS; no uploads' 'good'
        Append-Log ('J47-J48 safe ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J49 has TWO separate positive approvals. Unlike prior SYNTHETIC cloud
# probes, this action uploads ~12.6MB of REAL Qwen model-derived weights.
$btnQwenJ49.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J49 requires 0.6B evidence.';return }
    $p=Get-QwenStageEData
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $receipt=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j47-j48-real-fp16-package-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J47+J48' -and $v.status -eq 'real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked' -and $v.compile_submission_allowed -eq $false) { $receipt=$f.FullName;break }
        } catch { }
    }
    if (-not $receipt) { Show-Error 'J49 requires successful J47-J48 offline receipt.';return }
    $approveModel=[System.Windows.Forms.MessageBox]::Show(
        'MODEL-WEIGHT UPLOAD APPROVAL: This J49 action will upload a copy of the REAL Qwen3-ASR layer-0 FP16 ONNX graph and approximately 12.6 MB of MODEL-DERIVED WEIGHTS to Qualcomm AI Hub. This is NOT weight-free and cannot be undone once submitted. No recording is sent. Authorize model-derived-weight upload?',
        'J49: authorize real model weights',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($approveModel -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $approveCloud=[System.Windows.Forms.MessageBox]::Show(
        'SEPARATE CLOUD APPROVAL: Submit the verified real FP16 layer-0 graph to Qualcomm for Samsung Galaxy S26 (SM8850/V81, QAIRT 2.50)? Compilation/linking can FAIL on dynamic Expand or INT64 tensors, and may consume cloud credits. This is NOT full decoder or NPU-cache qualification. Submit one compile + link experiment?',
        'J49: authorize cloud compile and charges',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($approveCloud -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J49: explicit approved REAL FP16 decoder block cloud qualification...' {
        $report=Join-Path $dir ('stage-j49-real-fp16-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ47J49Path,'cloud','--onnx-dir',$p.Onnx,
                '--j48-receipt',$receipt,'--approve-model-weights','--approve-cloud','--output',$report)
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J49 diagnostic ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j49-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j49','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J49 independent cloud status/privacy gate failed: '+$post) }
        if ($run.ExitCode -ne 0) {
            Set-Status $lblQwenJ49 'Qualcomm compile FAILED; J50 can diagnose existing job' 'warn'
            Append-Log ('J49 compile/link failed safely; J49 status ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
            return  # Expected remote compile failure is NOT a Builder GUI exception.
        }
        Set-Status $lblQwenJ49 'REAL layer-0 compile+link PASS; NPU runtime unverified' 'good'
        Append-Log ('J49 sanitized status ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J50 reuses the existing failed J49 job only. The AI Hub API is called for
# READ-ONLY job-log retrieval if the already downloaded local log is missing.
# No compile/link, upload, cloud-cost or weight approval occurs here.
$btnQwenJ50.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J50 requires the validated Qwen 0.6B model.';return }
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $receipt=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j49-real-fp16-cloud-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J49' -and $v.status -eq 'compile_failed' -and $v.compile_status.code -eq 'FAILED' -and $v.decoder_compiler_ready -eq $false) { $receipt=$f.FullName;break }
        } catch { }
    }
    if (-not $receipt) { Show-Error 'J50 requires the failed real-weight J49 JSON receipt. No new jobs will be submitted.';return }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    Invoke-UiAction 'J50: diagnose existing J49 Qualcomm compiler log (READ-ONLY; no jobs)...' {
        $report=Join-Path $dir ('stage-j50-existing-compile-diagnosis-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ50Path,'--j49-receipt',$receipt,'--read-existing-cloud-log','--output',$report)
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 360
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J50 report ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j50-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j50','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J50 independent receipt gate failed: '+$post) }
        $j=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($run.ExitCode -eq 0 -and $j.status -eq 'compiler_log_diagnosed') {
            Set-Status $lblQwenJ50 ('J49 diagnosis: '+$j.failure_category) 'good'
        } else {
            Set-Status $lblQwenJ50 'Existing J49 log not available; no new jobs' 'warn'
        }
        Append-Log ('J50 safe read-only diagnostic ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J51-J53: offline profile-specific rewrite of BOTH actual FP16 Expand shape
# operands. Uses the already-verified position-8 real decoder feed (J48) and
# prior J50 actual Qualcomm converter failure. No cloud upload or compilation.
$btnQwenJ51.Add_Click({
    Invoke-UiAction 'J51-J53: specialize REAL FP16 Expand shapes at position 8 (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J51 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $specs=@(
            [pscustomobject]@{ Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked' },
            [pscustomobject]@{ Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked' },
            [pscustomobject]@{ Filter='stage-j50-existing-compile-diagnosis-*.json';Stage='J50';Status='compiler_log_diagnosed' }
        )
        $receipts=@()
        foreach ($entry in $specs) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) {
                        if ($entry.Stage -ne 'J50' -or $v.failure_category -eq 'expand_dynamic_shape_unsupported') {
                            $found=$f.FullName;break
                        }
                    }
                } catch { }
            }
            if (-not $found) { throw ('J51 requires successful prior JSON receipt: '+$entry.Filter+'. Older ZIPs are optional.') }
            $receipts += [string]$found
        }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'Specialize the TWO real FP16 decoder Expand shape inputs for the MEASURED position-8 profile only, then require exact ONNX Runtime parity with the unchanged FP16 block. Other positions/cache lengths are NOT qualified. This is local-only and may use substantial RAM. No cloud jobs or original-model modifications. Continue?',
            'J51-J53 PROFILE-LIMITED OFFLINE TEST',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j51-j53-static-expand-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ51J53Path,'--onnx-dir',$p.Onnx,
                '--j28-receipt',$receipts[0],'--j48-receipt',$receipts[1],
                '--j50-receipt',$receipts[2],'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 3600
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J51-J53 sanitized ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j53-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j53','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J53 independent safety/privacy gate failed: '+$post) }
        if ($run.ExitCode -ne 0) {
            Set-Status $lblQwenJ51 'Position-8 specialization blocked; inspect safe report' 'warn'
            Append-Log ('J51-J53 blocked safely: '+$report)
            return
        }
        Set-Status $lblQwenJ51 'Position-8 REAL FP16 Expand parity PASS; cloud locked' 'good'
        Append-Log ('J51-J53 safe diagnostic ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J54-J56: read-only provenance/graph topology/converter risk audit. The only
# permitted output is an offline receipt. No Qualcomm submission path exists.
$btnQwenJ54.Add_Click({
    Invoke-UiAction 'J54-J56: audit the ACTUAL position-8 FP16 graph (OFFLINE, READ-ONLY)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J54 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $specs=@(
            [pscustomobject]@{ Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked' },
            [pscustomobject]@{ Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked' },
            [pscustomobject]@{ Filter='stage-j51-j53-static-expand-*.json';Stage='J51+J52+J53';Status='position8_specialized_fp16_cpu_parity_passed_cloud_locked' }
        )
        $receipts=@()
        foreach ($entry in $specs) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) { $found=$f.FullName;break }
                } catch { }
            }
            if (-not $found) { throw ('J54 requires prior successful JSON receipt: '+$entry.Filter) }
            $receipts += [string]$found
        }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            'Inspect the existing J51 candidate and REAL FP16 sidecar without changing any model. Verify only two Expand inputs changed, inspect any surviving output-live dynamic shape chain, and write a SAFE JSON + ZIP. This does NOT submit to Qualcomm, grant weight-upload consent, or prove compatibility. Continue?',
            'J54-J56 OFFLINE READ-ONLY GRAPH AUDIT',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j54-j56-static-audit-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ54J56Path,'--onnx-dir',$p.Onnx,
                '--j28-receipt',$receipts[0],'--j48-receipt',$receipts[1],
                '--j53-receipt',$receipts[2],'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 300
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J54-J56 sanitized ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j56-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j56','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J56 independent privacy/safety gate failed: '+$post) }
        if ($run.ExitCode -ne 0) {
            Set-Status $lblQwenJ54 'Structural check blocked; inspect offline receipt' 'warn'
            Append-Log ('J54-J56 blocked safely: '+$report)
            return
        }
        Set-Status $lblQwenJ54 'Exact 2-edge graph delta verified; cloud locked' 'good'
        Append-Log ('J54-J56 sanitized diagnostic ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J57: ONLY isolated preparation and exact real FP16 provenance validation.
# Fresh paid/weights approval is NOT inherited from any historical cloud job.
$btnQwenJ57.Add_Click({
    Invoke-UiAction 'J57: prepare exact-profile real FP16 model package (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J57 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $specs=@(
            [pscustomobject]@{Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked'},
            [pscustomobject]@{Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j51-j53-static-expand-*.json';Stage='J51+J52+J53';Status='position8_specialized_fp16_cpu_parity_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j54-j56-static-audit-*.json';Stage='J54+J55+J56';Status='position8_real_graph_structurally_audited_cloud_locked'}
        )
        $receipts=@()
        foreach ($entry in $specs) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) { $found=$f.FullName;break }
                } catch { }
            }
            if (-not $found) { throw ('J57 requires successful previous JSON receipt: '+$entry.Filter) }
            $receipts += [string]$found
        }
        $ok=[System.Windows.Forms.MessageBox]::Show(
            'J57 copies the verified position-8 real FP16 graph and approximately 12.6 MB model-derived weights to a new LOCAL private Builder folder for a possible later compiler trial. It preserves all originals. NO Qualcomm submission, no uploads, no cloud charges, no phone changes. Continue?',
            'J57 OFFLINE private package only',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($ok -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j57-pos8-private-package-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ57J59Path,'offline','--onnx-dir',$p.Onnx,
                '--j28-receipt',$receipts[0],'--j48-receipt',$receipts[1],
                '--j53-receipt',$receipts[2],'--j56-receipt',$receipts[3],
                '--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 300
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J57 sanitized ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j57-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j57','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J57 independent private-package gate failed: '+$post) }
        if ($run.ExitCode -ne 0) {
            Set-Status $lblQwenJ57 'Private staging safely blocked; inspect receipt' 'warn'
            Append-Log ('J57 blocked; safe diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
            return
        }
        Set-Status $lblQwenJ57 'Real position-8 upload package ready LOCALLY; cloud locked' 'good'
        Append-Log ('J57 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J58/J59: ONE newly approved Qualcomm trial on the J51-specialized
# position-8 graph, never the unchanged J49 graph. Two opt-in dialogs are
# mandatory: MODEL-DERIVED WEIGHTS and separately PAID CLOUD COMPUTE.
$btnQwenJ58.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J58 requires Qwen 0.6B evidence.';return }
    $p=Get-QwenStageEData
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $receipt=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j57-pos8-private-package-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J57' -and $v.status -eq 'position8_verified_private_upload_package_cloud_locked' -and
                $v.j57.graph_sha256 -eq 'ee96300b7217ff9056e01f76570a576023b886b914b0e60f335e3f7b9424b6df' -and
                $v.compile_submission_allowed -eq $false) { $receipt=$f.FullName;break }
        } catch { }
    }
    if (-not $receipt) { Show-Error 'Run successful offline J57 before any new Qualcomm trial.';return }
    $approveWeights=[System.Windows.Forms.MessageBox]::Show(
        'NEW MODEL-WEIGHT UPLOAD CONSENT: The NEXT action would upload the verified position-8 real Qwen FP16 layer-0 ONNX graph (SHA-256 begins ee96300b) and approximately 12.6 MB of MODEL-DERIVED FP16 weights to Qualcomm AI Hub. This is not the old J49 graph, not a synthetic probe, and cannot be undone after upload. NO recording is sent. Approve this specific upload?',
        'J58 explicit NEW model-weight upload consent',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($approveWeights -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $approveCloud=[System.Windows.Forms.MessageBox]::Show(
        'SEPARATE PAID CLOUD CONSENT: Qualcomm AI Hub QAIRT 2.50 may charge credits for one compile/link experiment on exact Samsung Galaxy S26 SM8850/HTP V81, position 8 ONLY. Cost is not known. Converter may fail because shape-producing output paths and Reciprocal ops remain. Successful compile/link would NOT demonstrate Android runtime, decoder speed, arbitrary cache positions or NPU-resident KV. Approve one new attempt?',
        'J59 explicit NEW Qualcomm cloud-cost consent',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($approveCloud -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J58-J59: newly approved real weighted position-8 Qualcomm compile/link...' {
        $report=Join-Path $dir ('stage-j58-j59-pos8-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ57J59Path,'cloud','--onnx-dir',$p.Onnx,
                '--j57-receipt',$receipt,'--approve-model-weights','--approve-paid-cloud',
                '--output',$report)
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw ('J58-J59 missing diagnostic ZIP; possible in-flight cloud submission; DO NOT RETRY. Inspect local JSON: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j59-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j59','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J59 independent status/privacy gate failed; DO NOT RETRY CLOUD: '+$post) }
        if ($run.ExitCode -ne 0) {
            Set-Status $lblQwenJ58 'Qualcomm real position-8 graph NOT qualified; see receipt' 'warn'
            Append-Log ('J58-J59 failed/blocked: '+[IO.Path]::ChangeExtension($report,'.zip'))
            return
        }
        Set-Status $lblQwenJ58 'Position-8 real FP16 layer compiled/linked; phone untested' 'good'
        Append-Log ('J58-J59 safe status ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# -------------------- Stage J1: offline Qwen decoder preflight, NO cloud job --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage J1 - Verify REAL decoder graph/KV contract (offline; no QNN compilation)' 25 4795 850 32 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Reads the existing decoder init/step ONNX graph METADATA (external weights stay on disk). Lists actual tensors, KV shapes, dynamic dimensions and QNN operator risks. Does NOT guess fixed profiles, upload models, run audio, modify originals or incur cloud cost. Stage J2 will require bounded speech-derived vectors before any paid compile.' 25 4842 844 89))
$btnQwenStageJPreflight=New-Button 'J1 - Audit decoder contract (offline)' 25 4942 440 45
$tabQwenAsr.Controls.Add($btnQwenStageJPreflight)
$lblQwenStageJPreflight=New-StatusLabel 483 4952 360
$tabQwenAsr.Controls.Add($lblQwenStageJPreflight)
$btnQwenStageJOpen=New-Button 'Open J1 reports' 25 5010 225 40
$tabQwenAsr.Controls.Add($btnQwenStageJOpen)
$btnQwenStageJPreflight.Add_Click({
    Invoke-UiAction 'Auditing Qwen J1 decoder graph contracts without cloud or phone access...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J1 uses the already-exported and tested 0.6B decoder; 1.7B must be qualified separately.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Original verified ONNX directory missing; select original decoder export in Qwen Stage D.' }
        $py=Ensure-HuggingFaceUploaderPython
        $check=Invoke-ProcessCapture -FilePath $py -Arguments @('-c','import onnx; print(onnx.__version__)') -TimeoutSeconds 60
        if ($check.ExitCode -ne 0) { throw 'J1 metadata inspector requires the existing onnx Python package. Run Qwen Stage D audit first; no source re-export is required.' }
        $out=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        New-Item -ItemType Directory -Path $out -Force | Out-Null
        $report=Join-Path $out ('stage-j1-decoder-contract-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $r=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJPath,'preflight','--onnx-dir',$p.Onnx,'--output',$report) -TimeoutSeconds 900
        if ($r.ExitCode -ne 0) { throw ('J1 decoder contract audit BLOCKED; automatic ZIP will include the J1 failure JSON. See ' + $report + ' ' + $r.StdErr + $r.StdOut) }
        $result=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($result.status -ne 'decoder_contract_audited_not_compiled') { throw ('J1 unexpected receipt: ' + $result.status) }
        Set-Status $lblQwenStageJPreflight 'J1 audited; J2/QNN NOT validated' 'warn'
        Append-Log ('Qwen J1 contract/risks (no cloud spend): ' + $report)
        $zip=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $zip -PathType Leaf)) { throw 'J1 did not create the automatic diagnostic ZIP; do not accept the audit.' }
        Append-Log ('Qwen J1 automatic audit ZIP: ' + $zip)
    }
})
$btnQwenStageJOpen.Add_Click({ try {
    $p=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    New-Item -ItemType Directory -Path $p -Force | Out-Null
    Open-Folder $p
} catch { Show-Error $_ } })

# -------------------- Stage J2: real speech CPU shape/KV profiling, NO cloud / ADB --------------------
$tabQwenAsr.Controls.Add((New-Label 'Stage J2 - Measured FP32 prefill / KV step shapes + exact token replay (offline)' 25 5083 855 32 -Bold))
$tabQwenAsr.Controls.Add((New-Label 'Reuses Stage G pinned ONNX preprocessing/decoder on the SELECTED .ogg/.wav. Captures actual 12-second prompt length, KV-cache dimensions, per-step CPU timing and exact token parity against a matching Stage G receipt or the original Stage G decoder replay. Never exports audio/transcripts/tokens to support ZIP; no QNN compile/phone/cloud. Original FP32 sessions may need substantial PC RAM.' 25 5125 845 105))
$btnQwenStageJ2Profile=New-Button 'J2 - Profile real-speech FP32 decoder (offline)' 25 5241 476 45
$tabQwenAsr.Controls.Add($btnQwenStageJ2Profile)
$lblQwenStageJ2Profile=New-StatusLabel 516 5252 335
$tabQwenAsr.Controls.Add($lblQwenStageJ2Profile)
$btnQwenStageJ2Open=New-Button 'Open J1/J2 reports' 25 5311 225 40
$tabQwenAsr.Controls.Add($btnQwenStageJ2Open)
$tabQwenAsr.Controls.Add((New-Label 'Stage J2 passes only a single CPU trace; several different speech recordings and a separate compatibility/compiler gate are still required for NPU acceleration. Do not delete or move large ONNX decoder files.' 25 5370 842 68))
$btnQwenStageJ2Profile.Add_Click({
    Invoke-UiAction 'Qwen J2: deriving real speech FP32 decoder input/KV shapes and verifying exact token replay...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J2 is exclusively the established Qwen3-ASR-0.6B FP32 export.' }
        $p=Get-QwenStageEData
        $audio=([string]$txtQwenAsrAudio.Text).Trim()
        if (-not $audio -or -not (Test-Path -LiteralPath $audio -PathType Leaf) -or
            [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.ogg','.wav')) {
            throw 'J2: choose a local .ogg or .wav real-speech recording using the existing Qwen audio picker.'
        }
        $ffmpeg=([string]$txtFfmpeg.Text).Trim()
        if (-not $ffmpeg -or -not (Test-Path -LiteralPath $ffmpeg -PathType Leaf)) { throw 'J2: configure existing FFmpeg executable in Setup.' }
        $provenance=Join-Path $p.ModelFolder 'third-party-exporter-provenance.json'
        if (-not (Test-Path -LiteralPath $provenance -PathType Leaf)) { throw 'J2: pinned Stage C exporter receipt missing. Reverify source, do not re-export.' }
        $exporterData=Get-Content -LiteralPath $provenance -Raw | ConvertFrom-Json
        if ($exporterData.exporter_commit -ne '4aee2ae0c355c7bc33bd5c09befec9e9eabd4b50') { throw 'J2: unrecognized exporter revision.' }
        $exporter=[string]$exporterData.folder
        if (-not (Test-Path -LiteralPath $exporter -PathType Container)) { throw 'J2: pinned exporter source folder unavailable. Restore original pinned source.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $j1=''
        foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j1-decoder-contract-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try { if ((Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json).status -eq 'decoder_contract_audited_not_compiled') { $j1=$f.FullName; break } } catch { }
        }
        if (-not $j1) { throw 'J2 requires a successful J1 real graph audit. Run the J1 button above first.' }
        $prior=''
        $folder=Join-Path $p.ModelFolder 'stage-g-hybrid-deployment'
        if (Test-Path -LiteralPath $folder -PathType Container) {
            $sha=(Get-FileHash -LiteralPath $audio -Algorithm SHA256).Hash.ToLowerInvariant()
            foreach ($f in @(Get-ChildItem -LiteralPath $folder -Filter 'qwen-real-speech-hybrid-validation.json' -Recurse -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $old=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($old.status -eq 's26_encoder_cpu_decoder_real_speech_exact_token_pass' -and
                        $old.audio_source_sha256 -eq $sha -and $old.token_exact) { $prior=$f.FullName; break }
                } catch { }
            }
        }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            ('J2 will run the existing FP32 CPU encoder + 28-layer decoder on the first 12 s of this real recording.'+"`r`n"+
             'Reuses existing ONNX files; may use several GB of RAM. NO cloud fees, phone activity, model changes or uploads.'+"`r`n"+
             'Only local JSON metadata and a redacted diagnostic ZIP will be created. Continue?'),
            'Qwen J2 original FP32 profiling', [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $python=Ensure-QwenAsrExportPython
        $report=Join-Path $dir ('stage-j2-speech-shape-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $j2Args=@($script:QwenAsrStageJ2Path,'profile','--onnx-dir',$p.Onnx,'--j1',$j1,
            '--audio',$audio,'--ffmpeg',$ffmpeg,'--exporter',$exporter,
            '--exporter-receipt',$provenance,'--output',$report)
        if ($prior) { $j2Args += @('--g1-receipt',$prior) }
        $r=Invoke-ProcessCapture -FilePath $python -Arguments $j2Args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 14400
        $zip=[IO.Path]::ChangeExtension($report,'.zip')
        if (-not (Test-Path -LiteralPath $zip -PathType Leaf)) { throw ('J2 failed to create audit ZIP; review local receipt: '+$report+$r.StdErr) }
        if ($r.ExitCode -ne 0) { throw ('J2 shape trace/parity BLOCKED. Local receipt: '+$report+'; redacted audit ZIP: '+$zip+' '+$r.StdErr) }
        $result=Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ($result.status -ne 'fp32_speech_trace_parity_pass_compilation_not_started' -or -not $result.reference_exact) {
            throw ('J2 unexpected parity receipt: '+$report)
        }
        Set-Status $lblQwenStageJ2Profile 'J2 FP32 parity PASS; QNN not compiled' 'good'
        Append-Log ('Qwen J2 offline CPU speech/KV trace: '+$report)
        Append-Log ('Qwen J2 shareable bounded diagnostic ZIP (review before sharing): '+$zip)
    }
})
$btnQwenStageJ2Open.Add_Click({ try {
    $p=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    New-Item -ItemType Directory -Path $p -Force | Out-Null
    Open-Folder $p
} catch { Show-Error $_ } })

# J60-J62: read the existing J58 failed compile log, measure the REAL
# runtime Reshape target, and perform exact FP16 CPU parity, all offline.
$btnQwenJ60.Add_Click({
    Invoke-UiAction 'J60-J62: diagnose original J58 failure and test one Reshape edge (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J60 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Existing ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $needed=@(
            [pscustomobject]@{Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked'},
            [pscustomobject]@{Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j51-j53-static-expand-*.json';Stage='J51+J52+J53';Status='position8_specialized_fp16_cpu_parity_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j57-pos8-private-package-*.json';Stage='J57';Status='position8_verified_private_upload_package_cloud_locked'},
            [pscustomobject]@{Filter='stage-j58-j59-pos8-cloud-*.json';Stage='J58+J59';Status='compile_failed'}
        )
        $evidence=@()
        foreach ($entry in $needed) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) { $found=$f.FullName;break }
                } catch { }
            }
            if (-not $found) { throw ('J60-J62 requires existing successful/failed receipt: '+$entry.Filter) }
            $evidence += [string]$found
        }
        $logFolder=Join-Path $dir (([IO.Path]::GetFileNameWithoutExtension($evidence[4])) + '-local-compiler-logs')
        $compileLog=Join-Path $logFolder 'jp3o9rxmp.log'
        if (-not (Test-Path -LiteralPath $compileLog -PathType Leaf)) { throw 'Original J58 compile log jp3o9rxmp.log not found. Keep it local; do not rerun Qualcomm.' }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            'J60-J62 evaluates a measured position-8 Reshape shape on your existing FP16 decoder, writes a NEW isolated ONNX candidate and exact CPU-parity diagnostics. No cloud jobs, uploads, model replacements, phone actions, or original weight edits. Continue?',
            'J60-J62 OFFLINE Reshape boundary only', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j60-j62-reshape-boundary-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ60J62Path,'--onnx-dir',$p.Onnx,
            '--j28-receipt',$evidence[0],'--j48-receipt',$evidence[1],
            '--j53-receipt',$evidence[2],'--j57-receipt',$evidence[3],
            '--j59-receipt',$evidence[4],'--compiler-log',$compileLog,'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 480
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J60-J62 sanitized diagnostic ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j62-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j62','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J62 independent offline gate failed: '+$post) }
        if ($run.ExitCode -ne 0) {
            Set-Status $lblQwenJ60 'Real Reshape trial safely blocked; inspect receipt' 'warn'
            Append-Log ('J60-J62 blocked; sanitized ZIP: '+[IO.Path]::ChangeExtension($report,'.zip'))
            return
        }
        Set-Status $lblQwenJ60 'Position-8 one-edge Reshape CPU parity passed; CLOUD LOCKED' 'good'
        Append-Log ('J60-J62 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J63: NEW J61-specific SHA pinned position-8 private package, OFFLINE.
$btnQwenJ63.Add_Click({
    Invoke-UiAction 'J63: prepare corrected Reshape graph PRIVATELY (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J63 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Existing ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $needed=@(
            [pscustomobject]@{Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j60-j62-reshape-boundary-*.json';Stage='J60+J61+J62';Status='position8_reshape_boundary_cpu_parity_passed_cloud_locked'}
        )
        $evidence=@()
        foreach ($entry in $needed) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try { $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) { $found=$f.FullName;break }
                } catch { }
            }
            if (-not $found) { throw ('J63 requires real success receipt: '+$entry.Filter) }
            $evidence += [string]$found
        }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            'J63 copies the EXACT J61 corrected Reshape graph and unchanged 12.6 MB FP16 sidecar into an isolated PRIVATE Builder folder. This is OFFLINE: no uploads, cloud jobs, charges or phone changes. Continue?',
            'J63 offline private staging ONLY',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j63-reshape-package-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ63J65Path,'offline','--onnx-dir',$p.Onnx,
            '--j48-receipt',$evidence[0],'--j62-receipt',$evidence[1],'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 210
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J63 sanitized ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j63-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j63','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J63 independent offline gate failed: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ63 'J63 safely blocked; inspect receipt' 'warn';return }
        Set-Status $lblQwenJ63 'J61 candidate privately staged; CLOUD LOCKED' 'good'
        Append-Log ('J63 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J64-J65: separate opt-in ONLY for the new J61 SHA; never borrow J58 consent.
$btnQwenJ64.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J64 requires Qwen 0.6B.';return }
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $receipt=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j63-reshape-package-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try { $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J63' -and $v.status -eq 'j61_position8_candidate_privately_staged_cloud_locked' -and
                $v.j63.graph_sha256 -eq 'fc276af0626f4fdef4bd1f77bda64157e23c73f7ce26ae321a9d3081a56be19e') {
                $receipt=$f.FullName;break }
        } catch { }
    }
    if (-not $receipt) { Show-Error 'Run successful OFFLINE J63 before submitting the NEW J61 graph.';return }
    # Never retry a possible upload. ONE manually approved retry is permitted only
    # for the known v0.4.16 wrong-interpreter error: qai_hub missing BEFORE upload.
    # Keep the original failed receipt/ZIP untouched for auditing.
    $priorCloudReceipts=@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j64-j65-reshape-cloud-*.json' -File -ErrorAction SilentlyContinue)
    if ($priorCloudReceipts.Count -gt 1) {
        Show-Error 'Multiple J64-J65 receipts exist. Inspect them manually; no cloud retry permitted.';return
    }
    if ($priorCloudReceipts.Count -eq 1) {
        $safePreflightFailure=$false
        try {
            $old=Get-Content -LiteralPath $priorCloudReceipts[0].FullName -Raw | ConvertFrom-Json
            $safePreflightFailure=($old.stage -eq 'J64+J65' -and $old.status -eq 'blocked' -and
                $old.cloud_submission_attempted -eq $false -and
                $old.execution.cloud_jobs -eq 0 -and $old.execution.weight_uploads -eq 0 -and
                $old.execution.phone_changes -eq 0 -and $old.execution.model_modifications -eq 0 -and
                $old.failure_code -eq 'RuntimeError' -and
                [string]$old.details_local_only.message -like 'qai_hub Python client is not installed*')
        } catch { $safePreflightFailure=$false }
        if (-not $safePreflightFailure) {
            Show-Error 'A previous J64-J65 attempt may have submitted jobs. NO RETRY: inspect all statuses manually.';return
        }
        Append-Log '[J64] One explicit retry eligible: prior qai_hub import failure, zero submissions verified. Previous receipt retained.'
    }
    # J64 must use the same isolated, import-verified qai_hub environment as J58.
    # Verify/repair prerequisites before asking permission for any cloud submission.
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $weightApproval=[System.Windows.Forms.MessageBox]::Show(
        'NEW approval: upload the PRIVATE model-derived FP16 layer weights (~12.6 MB) and the J61 corrected position-8 graph to Qualcomm AI Hub? This is NOT covered by prior J58 approval.',
        'J64 separate model upload consent',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($weightApproval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    $computeApproval=[System.Windows.Forms.MessageBox]::Show(
        'NEW approval: submit ONE compile/link trial on Qualcomm AI Hub for the J61 SHA-pinned graph. Your free-tier quota may apply; verify any applicable compute terms yourself. This is NOT phone execution and is NOT a repeat of J58. Proceed?',
        'J65 separate Qualcomm compute consent',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($computeApproval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J64-J65: explicitly approved NEW J61 graph Qualcomm compile/link...' {
        $p=Get-QwenStageEData
        $py=$hubPython
        $report=Join-Path $dir ('stage-j64-j65-reshape-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ63J65Path,'cloud','--onnx-dir',$p.Onnx,'--j63-receipt',$receipt,
            '--output',$report,'--approve-model-weights','--approve-cloud-compute')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 7200
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw ('J64-J65 receipt ZIP missing; job may be in flight. NEVER blindly retry: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j65-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j65','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J65 postflight failed; DO NOT RETRY CLOUD: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ64 'Qualcomm J61 candidate NOT qualified; check report' 'warn';return }
        Set-Status $lblQwenJ64 'New J61 layer compiled + linked; phone untested' 'good'
        Append-Log ('J64-J65 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J66: one offline run freezes ALL remaining nonconstant Reshape shape edges.
$btnQwenJ66.Add_Click({
    Invoke-UiAction 'J66: measure ALL remaining dynamic Reshapes (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J66 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'Existing ONNX folder missing.' }
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $needed=@(
            [pscustomobject]@{Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked'},
            [pscustomobject]@{Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j60-j62-reshape-boundary-*.json';Stage='J60+J61+J62';Status='position8_reshape_boundary_cpu_parity_passed_cloud_locked'},
            [pscustomobject]@{Filter='stage-j64-j65-reshape-cloud-*.json';Stage='J64+J65';Status='compile_failed'}
        )
        $evidence=@()
        foreach ($entry in $needed) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try {
                    $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) { $found=$f.FullName;break }
                } catch { }
            }
            if (-not $found) { throw ('J66 needs existing evidence: '+$entry.Filter) }
            $evidence += [string]$found
        }
        $logFolder=Join-Path $dir (([IO.Path]::GetFileNameWithoutExtension($evidence[3])) + '-local-compiler-logs')
        $compileLog=Join-Path $logFolder 'jprx0m30p.log'
        if (-not (Test-Path -LiteralPath $compileLog -PathType Leaf)) { throw 'J64 compiler log jprx0m30p.log not found. Preserve it locally; do not retry the same cloud job.' }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            'J66 evaluates every remaining dynamic Reshape shape edge in the REAL J61 position-8 FP16 layer, stages a NEW isolated graph only if three-case, eight-output exact CPU parity passes. OFFLINE: zero cloud jobs or weight uploads. Continue?',
            'J66 OFFLINE model validation',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j66-all-reshape-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ66J68Path,'offline','--onnx-dir',$p.Onnx,
            '--j28-receipt',$evidence[0],'--j48-receipt',$evidence[1],
            '--j62-receipt',$evidence[2],'--j65-receipt',$evidence[3],
            '--compiler-log',$compileLog,'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 900
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J66 sanitized receipt ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j66-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j66','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J66 independent postflight failed: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ66 'Offline stage blocked: inspect J66 ZIP' 'warn';return }
        Set-Status $lblQwenJ66 'All remaining Reshape edges validated, cloud locked' 'good'
        Append-Log ('J66 sanitized result: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J67-J68: separately approved NEW candidate, never reuse previous J64 consent.
$btnQwenJ67.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J67 requires Qwen 0.6B.';return }
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $receipt=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j66-all-reshape-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try { $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J66' -and $v.status -eq 'position8_all_dynamic_reshape_edges_cpu_parity_passed_cloud_locked') { $receipt=$f.FullName;break }
        } catch { }
    }
    if (-not $receipt) { Show-Error 'Run successful OFFLINE J66 before Qualcomm compilation.';return }
    if (@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j67-j68-all-reshape-cloud-*.json' -File -ErrorAction SilentlyContinue).Count -gt 0) {
        Show-Error 'A J67-J68 submission may have occurred. Review its status; automatic retries are forbidden.';return
    }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $weightApproval=[System.Windows.Forms.MessageBox]::Show(
        'NEW model upload approval: send the J66 position-8 FP16 graph and its ~12.6 MB model-derived weights to Qualcomm AI Hub? Previous J64 approval is NOT reused.',
        'J67 fresh model upload approval',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($weightApproval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    $cloudApproval=[System.Windows.Forms.MessageBox]::Show(
        'NEW Qualcomm compile/link submission approval for one J66 position-8 candidate. AI Hub may be free under your account terms. This does NOT run the graph on your S26. Proceed?',
        'J68 fresh cloud authorization',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($cloudApproval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J67-J68: ONE approved real FP16 all-Reshape Qualcomm compile/link...' {
        $p=Get-QwenStageEData
        $report=Join-Path $dir ('stage-j67-j68-all-reshape-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ66J68Path,'cloud','--onnx-dir',$p.Onnx,
            '--j66-receipt',$receipt,'--output',$report,'--approve-model-weights','--approve-cloud-compute')
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 7200
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw ('J67-J68 receipt ZIP missing. Cloud status UNCERTAIN, do not retry: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j68-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j68','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J68 postflight failed; DO NOT blindly retry: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ67 'QAIRT compile/link failed; inspect detailed log' 'warn';return }
        Set-Status $lblQwenJ67 'QNN layer compiled + linked; phone not yet tested' 'good'
        Append-Log ('J67-J68 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J69: link-log-grounded rank-zero integer Add investigation, OFFLINE only.
$btnQwenJ69.Add_Click({
    Invoke-UiAction 'J69: validate actual scalar Add boundary (OFFLINE)...' {
        if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { throw 'J69 requires Qwen 0.6B.' }
        $p=Get-QwenStageEData
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $needed=@(
            @{Filter='stage-j26-j28-fp16-block-*.json';Stage='J26+J27+J28';Status='real_fp16_block_cpu_parity_passed_compilation_locked'},
            @{Filter='stage-j47-j48-real-fp16-package-*.json';Stage='J47+J48';Status='real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'},
            @{Filter='stage-j66-all-reshape-*.json';Stage='J66';Status='position8_all_dynamic_reshape_edges_cpu_parity_passed_cloud_locked'},
            @{Filter='stage-j67-j68-all-reshape-cloud-*.json';Stage='J67+J68';Status='link_failed'}
        )
        $evidence=@()
        foreach ($entry in $needed) {
            $found=$null
            foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $entry.Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
                try { $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
                    if ($v.stage -eq $entry.Stage -and $v.status -eq $entry.Status) { $found=$f.FullName;break }
                } catch { }
            }
            if (-not $found) { throw ('J69 needs prior receipt: '+$entry.Filter) }
            $evidence += [string]$found
        }
        $linkFolder=Join-Path $dir (([IO.Path]::GetFileNameWithoutExtension($evidence[3])) + '-local-compiler-logs')
        $linkLog=Join-Path $linkFolder 'jg9oow6mg.log'
        if (-not (Test-Path -LiteralPath $linkLog -PathType Leaf)) { throw 'J68 link log jg9oow6mg.log is missing in the local compiler-log folder.' }
        $confirm=[System.Windows.Forms.MessageBox]::Show(
            'Measure actual integer scalar Add outputs, freeze only stable position-8 values, and verify exact CPU parity on all eight graph outputs. OFFLINE: no Qualcomm jobs or uploads. Continue?',
            'J69 OFFLINE validation',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j69-scalar-add-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ69J71Path,'offline','--onnx-dir',$p.Onnx,
           '--j28-receipt',$evidence[0],'--j48-receipt',$evidence[1],
           '--j66-receipt',$evidence[2],'--j68-receipt',$evidence[3],
           '--link-log',$linkLog,'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 900
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw 'J69 sanitized report ZIP missing.' }
        $post=Join-Path $dir ('builder-quality-postflight-j69-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j69','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J69 independent postflight failed: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ69 'J69 blocked: inspect structural diagnostic ZIP' 'warn';return }
        Set-Status $lblQwenJ69 'Position-8 scalar Add exact parity passed' 'good'
        Append-Log ('J69 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J70-J71: fresh consent for NEW graph; J67-J68 approval is NOT reused.
$btnQwenJ70.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J70 requires Qwen 0.6B.';return }
    $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    $receipt=$null
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j69-scalar-add-*.json' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try { $v=Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq 'J69' -and $v.status -eq 'position8_integer_scalar_add_cpu_parity_passed_cloud_locked') { $receipt=$f.FullName;break }
        } catch { }
    }
    if (-not $receipt) { Show-Error 'Successful OFFLINE J69 is required before cloud compilation.';return }
    if (@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j70-j71-scalar-add-cloud-*.json' -File -ErrorAction SilentlyContinue).Count -gt 0) {
        Show-Error 'A J70-J71 submission may already exist. Review its status; automatic retry is forbidden.';return
    }
    try { $hubPython=Get-QwenJ22AiHubPython } catch { Show-Error $_;return }
    if (-not $hubPython) { return }
    $weightApproval=[System.Windows.Forms.MessageBox]::Show(
        'Upload the NEW verified J69 position-8 graph and ~12.6 MB model-derived FP16 weights to Qualcomm AI Hub? Prior consent does not apply.',
        'J70 new weight upload approval',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($weightApproval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    $cloudApproval=[System.Windows.Forms.MessageBox]::Show(
        'Submit ONE new compile/link trial on Qualcomm AI Hub for the J69 graph? The job does NOT execute on the Galaxy S26.',
        'J71 new Qualcomm job approval',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($cloudApproval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'J70-J71: ONE newly approved Qualcomm compile/link...' {
        $p=Get-QwenStageEData
        $report=Join-Path $dir ('stage-j70-j71-scalar-add-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ69J71Path,'cloud','--onnx-dir',$p.Onnx,
               '--j69-receipt',$receipt,'--output',$report,'--approve-model-weights','--approve-cloud-compute')
        $run=Invoke-ProcessCapture -FilePath $hubPython -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 7200
        if (-not (Test-Path -LiteralPath ([IO.Path]::ChangeExtension($report,'.zip')) -PathType Leaf)) { throw ('J70-J71 diagnostic ZIP missing; cloud state uncertain: '+$report) }
        $post=Join-Path $dir ('builder-quality-postflight-j71-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j71','--output',$post,'--combined-receipt',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J71 independent postflight failed; do NOT retry blindly: '+$post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ70 'Qualcomm job failed: inspect detailed log' 'warn';return }
        Set-Status $lblQwenJ70 'QNN layer compiled + linked; device still untested' 'good'
        Append-Log ('J70-J71 sanitized diagnostic: '+[IO.Path]::ChangeExtension($report,'.zip'))
    }
})


# J72: download the EXISTING successful J71 QNN context binary privately.
# Uses Qualcomm's configured client only to read a completed job; NEVER compiles.
function Find-QwenJ72Receipt {
    param([string]$Filter,[string]$Stage,[string]$Status)
    $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
    foreach ($f in @(Get-ChildItem -LiteralPath $dir -Filter $Filter -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $v = Get-Content -LiteralPath $f.FullName -Raw | ConvertFrom-Json
            if ($v.stage -eq $Stage -and $v.status -eq $Status) { return [string]$f.FullName }
        } catch { }
    }
    throw ('J72/J73 needs successful prior receipt: ' + $Filter)
}
$btnQwenJ72.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J72 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J72: download previously linked QNN binary (NO compilation)...' -ShareQwenStage 'J72' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j69 = Find-QwenJ72Receipt 'stage-j69-scalar-add-*.json' 'J69' 'position8_integer_scalar_add_cpu_parity_passed_cloud_locked'
        $j71 = Find-QwenJ72Receipt 'stage-j70-j71-scalar-add-cloud-*.json' 'J70+J71' 'position8_scalar_add_real_fp16_compile_link_passed_runtime_unqualified'
        $private = Join-Path $dir 'private-j72-linked-context'
        New-Item -ItemType Directory -Path $private -Force | Out-Null
        $hubPython = Get-QwenJ22AiHubPython
        if (-not $hubPython) { throw 'The existing AI Hub Python interpreter must be selected.' }
        $report = Join-Path $dir ('stage-j72-download-linked-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run = Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:QwenAsrStageJ72J73Path,'download','--j69-receipt',$j69,'--j71-receipt',$j71,'--private-dir',$private,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1200
        if ($run.ExitCode -ne 0) { throw ('J72 download blocked. Inspect stage-j72 JSON and ZIP. ' + $run.StdErr) }
        $post = Join-Path $dir ('builder-quality-postflight-j72-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate = Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j72','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw 'J72 independent postflight failed; do not use unverified context.' }
        Set-Status $lblQwenJ72 'Linked context downloaded privately' 'good'
        Append-Log ('J72 sanitized receipt: ' + [IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J73: reuse previously installed S26 ADB and Qualcomm runtime tools. Executes
# the QNN HTP library with NATIVE FP16 + integer tensors (not the float32-only
# Omi/Parakeet comparison code). No Android app install or ASR transcription.
$btnQwenJ73.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J73 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J73: run linked position-8 Qwen layer on actual S26 HTP...' -ShareQwenStage 'J73' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j28 = Find-QwenJ72Receipt 'stage-j26-j28-fp16-block-*.json' 'J26+J27+J28' 'real_fp16_block_cpu_parity_passed_compilation_locked'
        $j48 = Find-QwenJ72Receipt 'stage-j47-j48-real-fp16-package-*.json' 'J47+J48' 'real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'
        $j69 = Find-QwenJ72Receipt 'stage-j69-scalar-add-*.json' 'J69' 'position8_integer_scalar_add_cpu_parity_passed_cloud_locked'
        $j71 = Find-QwenJ72Receipt 'stage-j70-j71-scalar-add-cloud-*.json' 'J70+J71' 'position8_scalar_add_real_fp16_compile_link_passed_runtime_unqualified'
        $j72 = Find-QwenJ72Receipt 'stage-j72-download-linked-*.json' 'J72' 'linked_qnn_context_downloaded_private_runtime_unqualified'
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Select your authorized Galaxy S26 in Setup/Deploy first.' }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'J73 requires the actual authorized SM-S942U/SM-S942U1 phone.' }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Missing Android QNN runtime files: ' + ($runtime.Missing -join ', ')) }
        if ([bool]$chkBridgeTemperatureMatch.Checked -and -not $useQuant) {
            $description += "`r`n`r`nTemperature-matched option: pause up to 4 minutes before each later profile to approach the initial BATTERY temperature (not CPU/SoC temperature). If it cannot cool, stop remaining trials and verify cleanup. This can add up to 12 minutes."
        }
        $choice = [System.Windows.Forms.MessageBox]::Show('Run ONE position-8 Qwen FP16 decoder layer via libQnnHtp.so on the selected Galaxy S26? The Builder will temporarily stage the private context and test tensors over ADB, compare all 8 outputs, and remove ONLY its uniquely named staging folder. This is NOT full ASR transcription. Continue?', 'J73 phone execution consent', [System.Windows.Forms.MessageBoxButtons]::YesNo, [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $p = Get-QwenStageEData
        $private = Join-Path $dir 'private-j72-linked-context'
        $py = Ensure-QwenAsrOnnxRuntimePython
        $report = Join-Path $dir ('stage-j73-s26-layer0-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $manifest = Join-Path $private ('j73-runtime-paths-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        # This LOCAL-only manifest contains tool paths; never put it in shareable receipts.
        $runtime.Files | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifest -Encoding UTF8
        try {
            $args = @($script:QwenAsrStageJ72J73Path,'device','--onnx-dir',$p.Onnx,'--j28-receipt',$j28,'--j48-receipt',$j48,'--j69-receipt',$j69,'--j71-receipt',$j71,'--j72-receipt',$j72,'--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$manifest,'--private-dir',$private,'--output',$report)
            $run = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1500
        } finally {
            if (Test-Path -LiteralPath $manifest) { Remove-Item -LiteralPath $manifest -Force }
        }
        if (-not (Test-Path -LiteralPath $report)) { throw 'J73 did not produce a device receipt. Review Windows local logs; verify device scratch cleanup.' }
        $post = Join-Path $dir ('builder-quality-postflight-j73-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate = Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j73','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J73 postflight failed: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ73 'HTP proof blocked: check J73 diagnostic' 'warn'; return }
        Set-Status $lblQwenJ73 'Single-layer S26 HTP numerical screen passed' 'good'
        Append-Log ('J73 sanitized receipt: ' + [IO.Path]::ChangeExtension($report,'.zip'))
    }
})


# J74: repeat the already-proven layer-0 context 8 and 40 times on one
# authorized SM-S942U, checking first/last outputs each batch. No cloud,
# recompute, APK/app change or KV-residency claim. Device-local timestamps
# include context startup and file IO; report separately from ADB overhead.
$btnQwenJ74.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J74 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J74: benchmark repeated position-8 Qwen HTP layer...' -ShareQwenStage 'J74' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j28 = Find-QwenJ72Receipt 'stage-j26-j28-fp16-block-*.json' 'J26+J27+J28' 'real_fp16_block_cpu_parity_passed_compilation_locked'
        $j48 = Find-QwenJ72Receipt 'stage-j47-j48-real-fp16-package-*.json' 'J47+J48' 'real_fp16_weighted_block_staged_cpu_profile_passed_cloud_locked'
        $j69 = Find-QwenJ72Receipt 'stage-j69-scalar-add-*.json' 'J69' 'position8_integer_scalar_add_cpu_parity_passed_cloud_locked'
        $j71 = Find-QwenJ72Receipt 'stage-j70-j71-scalar-add-cloud-*.json' 'J70+J71' 'position8_scalar_add_real_fp16_compile_link_passed_runtime_unqualified'
        $j72 = Find-QwenJ72Receipt 'stage-j72-download-linked-*.json' 'J72' 'linked_qnn_context_downloaded_private_runtime_unqualified'
        $j73 = Find-QwenJ72Receipt 'stage-j73-s26-layer0-*.json' 'J73' 'position8_single_layer_s26_htp_provisional_numeric_pass_not_full_decoder'
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Select an authorized Galaxy S26 in Setup/Deploy first.' }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'J74 requires the real SM-S942U device.' }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Missing QNN runtime files: ' + ($runtime.Missing -join ', ')) }
        $consent = [System.Windows.Forms.MessageBox]::Show('Benchmark the existing position-8 Qwen layer via HTP with 8 and 40 identical graph calls? The Builder stages temporary tensors, compares all outputs of all 48 graph calls and removes its device folder. This measures context+I/O+inference time, NOT full decoder performance, and may briefly heat the phone. Continue?', 'J74 S26 repeated HTP benchmark', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($consent -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $p = Get-QwenStageEData
        $private = Join-Path $dir 'private-j72-linked-context'
        $py = Ensure-QwenAsrOnnxRuntimePython
        $report = Join-Path $dir ('stage-j74-s26-benchmark-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $manifest = Join-Path $private ('j74-runtime-paths-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $runtime.Files | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifest -Encoding UTF8
        try {
            $args = @($script:QwenAsrStageJ74Path,'--onnx-dir',$p.Onnx,'--j28-receipt',$j28,'--j48-receipt',$j48,'--j69-receipt',$j69,'--j71-receipt',$j71,'--j72-receipt',$j72,'--j73-receipt',$j73,'--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$manifest,'--private-dir',$private,'--output',$report)
            $run = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1800
        } finally {
            if (Test-Path -LiteralPath $manifest) { Remove-Item -LiteralPath $manifest -Force }
        }
        if (-not (Test-Path -LiteralPath $report)) { throw 'J74 did not produce a receipt. Verify device scratch cleanup.' }
        $post = Join-Path $dir ('builder-quality-postflight-j74-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate = Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j74','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J74 independent postflight failed: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ74 'Benchmark blocked: review J74 diagnostic ZIP' 'warn'; return }
        Set-Status $lblQwenJ74 'Repeated S26 HTP benchmark completed; review timing scope' 'good'
        Append-Log ('J74 sanitized benchmark receipt: ' + [IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J75-J76: original FP32 two-layer fusion candidate with original-source
# CPU parity and theoretical KV transfer accounting. No cloud or ADB use.
$btnQwenJ75.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J75 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J75-J76: verify real fused layer0+1 against original decoder (offline)' -ShareQwenStage 'J75' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j74 = Find-QwenJ72Receipt 'stage-j74-s26-benchmark-*.json' 'J74' 'repeated_position8_s26_htp_benchmark_and_sampled_parity_pass_not_full_decoder'
        $p = Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'J75 original ONNX workspace not found.' }
        $answer = [System.Windows.Forms.MessageBox]::Show('Extract ONE graph containing real decoder layers 0 and 1, using original FP32 weights in place, and compare to the original decoder on three CPU positions? CPU reference runs can use significant RAM and take several minutes. No Qualcomm upload or phone changes. Continue?', 'J75-J76 real two-layer fusion experiment', [System.Windows.Forms.MessageBoxButtons]::YesNo, [System.Windows.Forms.MessageBoxIcon]::Information)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py = Ensure-QwenAsrOnnxRuntimePython
        $report = Join-Path $dir ('stage-j75-j76-fused-fp32-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args = @($script:QwenAsrStageJ75J76Path,'--onnx-dir',$p.Onnx,'--j74-receipt',$j74,'--output',$report)
        $run = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1800
        if (-not (Test-Path -LiteralPath $report)) { throw 'J75-J76 failed before creating its diagnostic receipt.' }
        $post = Join-Path $dir ('builder-quality-postflight-j75-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate = Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j75','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J75 independent safety postflight failed: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ75 'Two-layer fusion blocked; review share ZIP' 'warn'; return }
        Set-Status $lblQwenJ75 'Two-layer real FP32 fusion parity passed; not NPU-ready' 'good'
        Append-Log ('J75 sanitized two-layer parity diagnostic: ' + [IO.Path]::ChangeExtension($report,'.zip'))
    }
})

# J77: isolated position-8 shape closure for real FP16 fused layers 0+1.
# No Qualcomm API access and no original model edits.
$btnQwenJ77.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J77 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J77: specialize real fused two-layer FP16 position-8 (offline)' -ShareQwenStage 'J77' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $prior = Find-QwenJ72Receipt 'stage-j75-j76-fused-fp32-*.json' 'J75' 'real_two_layer_fp32_and_fp16_cpu_parity_passed_cloud_locked'
        $p = Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'J77 original ONNX workspace unavailable.' }
        if ([bool]$chkBridgeTemperatureMatch.Checked) {
            $description += "`r`n`r`nTemperature-matched option: pause up to 4 minutes before each later profile to approach the initial BATTERY temperature (not CPU/SoC temperature). If it cannot cool, stop remaining trials and verify cleanup. This can add up to 12 minutes."
        }
        $choice = [System.Windows.Forms.MessageBox]::Show('Specialize only the proven position-8 real fused layers 0+1 FP16 graph? This can use significant PC memory. Exact three-case FP16 parity is required. No phone changes or cloud jobs. Continue?', 'J77 fused FP16 offline qualification', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py = Ensure-QwenAsrOnnxRuntimePython
        $report = Join-Path $dir ('stage-j77-fused-static-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run = Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ77J79Path,'offline','--onnx-dir',$p.Onnx,'--j75-receipt',$prior,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1800
        if (-not (Test-Path -LiteralPath $report)) { throw 'J77 did not produce a receipt; inspect the local Windows log.' }
        $post = Join-Path $dir ('builder-quality-postflight-j77-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate = Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j77','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J77 independent postflight blocked: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ77 'Fused shape closure blocked; send one J77 share ZIP' 'warn'; return }
        Set-Status $lblQwenJ77 'Two-layer FP16 position-8 exact parity passed, cloud locked' 'good'
    }
})

# J80: read-only local shape-contract audit. This fixes no model files,
# uploads no weights, and never starts a Qualcomm job.
$btnQwenJ80.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J80 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J80: read-only audit of the J77 QNN input shapes' -ShareQwenStage 'J80' -Action {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $prior=Find-QwenJ72Receipt 'stage-j75-j76-fused-fp32-*.json' 'J75' 'real_two_layer_fp32_and_fp16_cpu_parity_passed_cloud_locked'
        $prepared=Find-QwenJ72Receipt 'stage-j77-fused-static-*.json' 'J77' 'fused01_position8_shape_closed_exact_fp16_parity_cloud_locked'
        $p=Get-QwenStageEData
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j80-input-profile-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ77J79Path,'profile','--onnx-dir',$p.Onnx,'--j75-receipt',$prior,'--j77-receipt',$prepared,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 300
        if (-not (Test-Path -LiteralPath $report)) { throw 'J80 did not produce a receipt; inspect Windows logs.' }
        $post=Join-Path $dir ('builder-quality-postflight-j80-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j80','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J80 independent postflight blocked: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ80 'Input-shape audit blocked; send one J80 ZIP' 'warn'; return }
        Set-Status $lblQwenJ80 'Graph rank/dtype profile checked, scalar corrected; no cloud work' 'good'
    }
})

# J81: exact source node_add_250 rank-zero Add correction in a private
# isolated fused graph. Read-only source; no Qualcomm API or device activity.
$btnQwenJ81.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J81 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J81: freeze proven rank-zero integer Add, exact fused FP16 parity (OFFLINE)' -ShareQwenStage 'J81' -Action {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $prior=Find-QwenJ72Receipt 'stage-j75-j76-fused-fp32-*.json' 'J75' 'real_two_layer_fp32_and_fp16_cpu_parity_passed_cloud_locked'
        $prepared=Find-QwenJ72Receipt 'stage-j77-fused-static-*.json' 'J77' 'fused01_position8_shape_closed_exact_fp16_parity_cloud_locked'
        $profile=Find-QwenJ72Receipt 'stage-j80-input-profile-*.json' 'J80' 'verified_fused01_onnx_input_profile_corrected_no_cloud_jobs'
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'J81 original ONNX workspace unavailable.' }
        $choice=[System.Windows.Forms.MessageBox]::Show('Run one local position-8-only fused FP16 scalar Add correction and EXACTLY compare all ten outputs on three CPU cases? This may take several minutes and allocate substantial RAM. An independent new private candidate will be produced; no cloud upload, job or phone modifications. Continue?', 'J81 offline fused scalar Add parity gate', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j81-fused-scalar-add-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ81Path,'--onnx-dir',$p.Onnx,'--j75-receipt',$prior,'--j77-receipt',$prepared,'--j80-receipt',$profile,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1800
        if (-not (Test-Path -LiteralPath $report)) { throw 'J81 did not produce a receipt; inspect Windows logs.' }
        $post=Join-Path $dir ('builder-quality-postflight-j81-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j81','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J81 independent safety postflight blocked: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ81 'Scalar Add correction blocked; share the J81 ZIP' 'warn'; return }
        Set-Status $lblQwenJ81 'Fused FP16 scalar Add exact CPU parity; Qualcomm link unverified' 'good'
    }
})


# J78/79: separate affirmative consent for real weighted Qualcomm cloud job.
$btnQwenJ78.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J78 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J78-J79: compile/link TWO-LAYER fused real FP16 decoder (cloud)' -ShareQwenStage 'J78' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $prior = Find-QwenJ72Receipt 'stage-j75-j76-fused-fp32-*.json' 'J75' 'real_two_layer_fp32_and_fp16_cpu_parity_passed_cloud_locked'
        $prepared = Find-QwenJ72Receipt 'stage-j77-fused-static-*.json' 'J77' 'fused01_position8_shape_closed_exact_fp16_parity_cloud_locked'
        $profile = Find-QwenJ72Receipt 'stage-j80-input-profile-*.json' 'J80' 'verified_fused01_onnx_input_profile_corrected_no_cloud_jobs'
        # Exactly one known completed FAILED compile, or a known no-upload failure,
        # can be admitted by Python. Unknown/unconfirmed jobs are never retried.
        $earlierJ78 = @(Get-ChildItem -LiteralPath $dir -Filter 'stage-j78-j79-fused-cloud-*.json' -File -ErrorAction SilentlyContinue | Sort-Object Name)
        if ($earlierJ78.Count -gt 2) { throw 'Unreviewed J78 cloud receipt history: manual review required.' }
        $seenPreupload = $false
        $seenFailedCompile = $false
        for ($j78Index = 0; $j78Index -lt $earlierJ78.Count; $j78Index++) {
            $old=Get-Content -LiteralPath $earlierJ78[$j78Index].FullName -Raw -Encoding UTF8 | ConvertFrom-Json
            # A known pre-upload failure has no compile/link URL members.
            # Inspect the property collection; directly reading absent fields
            # crashes in PowerShell 5.1 StrictMode before the approval prompts.
            $noUpload=($old.stage -eq 'J78+J79' -and $old.status -eq 'blocked' -and
                $old.failure_code -eq 'J77_FP16_PACKAGE_CHANGED' -and
                $old.cloud_submission_attempted -ceq $false -and $old.execution.cloud_jobs -eq 0 -and
                $old.execution.weight_uploads -eq 0 -and
                -not ($old.PSObject.Properties.Name -contains 'compile_job_url') -and
                -not ($old.PSObject.Properties.Name -contains 'link_job_url'))
            $knownCompileFailure=($old.stage -eq 'J78+J79' -and $old.status -eq 'compile_failed' -and
                $old.cloud_submission_attempted -ceq $true -and $old.compile_status.code -eq 'FAILED' -and
                $old.compile_status.verified_success -ceq $false -and
                $old.graph_sha256 -eq 'ccba9fac4e4df90764601ce299fa4ece80d4013fdc019938ded906de07d524fb' -and
                $old.compile_job_url -eq 'https://workbench.aihub.qualcomm.com/jobs/jp0o9vl2p/' -and
                $old.link_job_url -eq 'https://workbench.aihub.qualcomm.com/jobs/jp8jr4zz5/' -and
                $old.execution.cloud_jobs -eq 2 -and $old.execution.weight_uploads -eq 1 -and
                $old.execution.phone_changes -eq 0 -and $old.execution.model_modifications -eq 0)
            if (-not ($noUpload -or $knownCompileFailure)) {
                throw 'Prior J78 cloud history is not safely qualified for another submission.'
            }
            if ($earlierJ78.Count -eq 2) {
                # Exactly the old no-upload failure followed by the reviewed
                # jp0o9vl2p failed compile; never accept two real submissions.
                if (($j78Index -eq 0 -and -not $noUpload) -or
                    ($j78Index -eq 1 -and -not $knownCompileFailure)) {
                    throw 'J78 historical receipt order/status is not approved for retry.'
                }
            }
            if ($noUpload) { $seenPreupload = $true }
            if ($knownCompileFailure) { $seenFailedCompile = $true }
        }
        if ($earlierJ78.Count -eq 2 -and -not ($seenPreupload -and $seenFailedCompile)) {
            throw 'J78 approved two-receipt history not established.'
        }
        if ($earlierJ78.Count -gt 0) {
            Append-Log 'Reviewed historical J78 receipts without deleting them. New upload and cloud consent still required.'
        }
        $consent1=[System.Windows.Forms.MessageBox]::Show('Authorize uploading ~44 MB of model-derived Qwen FP16 weights for ONE real two-layer position-8 Qualcomm compile/link trial? This consent applies to this job only.', 'J78 model weight upload approval', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($consent1 -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $consent2=[System.Windows.Forms.MessageBox]::Show('Authorize this Qualcomm AI Hub compile/link submission? Your access may be free, but quota/billing terms are determined by Qualcomm. No phone changes and no full-decoder claim. Continue?', 'J78 Qualcomm cloud compute approval', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($consent2 -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $p = Get-QwenStageEData
        $py = Get-QwenJ22AiHubPython
        if (-not $py) { throw 'Select the working Qualcomm AI Hub Python client in Setup.' }
        $report=Join-Path $dir ('stage-j78-j79-fused-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ77J79Path,'cloud','--onnx-dir',$p.Onnx,'--j75-receipt',$prior,'--j77-receipt',$prepared,'--j80-receipt',$profile,'--approve-model-weights','--approve-cloud-compute','--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 2400
        if (-not (Test-Path -LiteralPath $report)) { throw 'J78 cloud status unknown: review AI Hub jobs manually before any retry.' }
        $post=Join-Path $dir ('builder-quality-postflight-j79-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j79','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J79 independent postflight failed; review private status: ' + $report) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ78 'Qualcomm trial blocked or failed; review J78 one-ZIP receipt' 'warn'; return }
        Set-Status $lblQwenJ78 'Fused 2-layer Qualcomm compile/link passed; S26 run still needed' 'good'
    }
})


# J82/J83: NEW one-shot, consent-gated Qualcomm trial of the independently
# verified J81 scalar-Add-corrected fused graph. No J78 history deletion/retry.
$btnQwenJ82.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J82 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J82-J83: compile/link J81 corrected fused layers 0+1 (explicit cloud consent)' -ShareQwenStage 'J82' -Action {
        $dir = Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $prior = Find-QwenJ72Receipt 'stage-j75-j76-fused-fp32-*.json' 'J75' 'real_two_layer_fp32_and_fp16_cpu_parity_passed_cloud_locked'
        $prepared = Find-QwenJ72Receipt 'stage-j77-fused-static-*.json' 'J77' 'fused01_position8_shape_closed_exact_fp16_parity_cloud_locked'
        $profile = Find-QwenJ72Receipt 'stage-j80-input-profile-*.json' 'J80' 'verified_fused01_onnx_input_profile_corrected_no_cloud_jobs'
        $corrected = Find-QwenJ72Receipt 'stage-j81-fused-scalar-add-*.json' 'J81' 'fused01_position8_exact_rankzero_add_frozen_offline_cloud_locked'
        if (@(Get-ChildItem -LiteralPath $dir -Filter 'stage-j82-j83-fused-cloud-*.json' -File -ErrorAction SilentlyContinue).Count -gt 0) {
            throw 'Prior J82 cloud attempt exists. Review Qualcomm jobs manually; no automatic retry.'
        }
        $p = Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'J82 original ONNX workspace unavailable.' }
        $consent1=[System.Windows.Forms.MessageBox]::Show('Authorize uploading approximately 44 MB of model-derived Qwen FP16 weights for ONE NEW J82 two-layer position-8 Qualcomm compile/link trial? This is separate from the failed J78 attempt. Continue?', 'J82 model weight upload approval', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($consent1 -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $consent2=[System.Windows.Forms.MessageBox]::Show('Authorize ONE Qualcomm AI Hub compile/link submission for the exact J81-corrected fused graph? Quota and billing are controlled by Qualcomm. No phone changes. Continue?', 'J82 Qualcomm cloud compute approval', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($consent2 -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py = Get-QwenJ22AiHubPython
        if (-not $py) { throw 'Select the working Qualcomm AI Hub Python client in Setup.' }
        $report=Join-Path $dir ('stage-j82-j83-fused-cloud-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ82J83Path,'--onnx-dir',$p.Onnx,'--j75-receipt',$prior,'--j77-receipt',$prepared,'--j80-receipt',$profile,'--j81-receipt',$corrected,'--approve-model-weights','--approve-cloud-compute','--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 2400
        if (-not (Test-Path -LiteralPath $report)) { throw 'J82 cloud status unknown. Review AI Hub before any resubmission.' }
        $post=Join-Path $dir ('builder-quality-postflight-j83-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j83','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J83 independent safety postflight blocked: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ82 'J82 Qualcomm compile or link failed/blocked; share one J82 ZIP' 'warn'; return }
        Set-Status $lblQwenJ82 'J81 fused context compile/link passed; S26 hardware check still required' 'good'
    }
})


# J84: retrieve the already SUCCESS-linked J83 context. No cloud submission.
$btnQwenJ84.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J84 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J84: download existing J83 linked QNN context only' -ShareQwenStage 'J84' -Action {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j83=Find-QwenJ72Receipt 'stage-j82-j83-fused-cloud-*.json' 'J82+J83' 'fused01_corrected_scalar_add_qualcomm_compile_link_passed_device_unqualified'
        $j80=Find-QwenJ72Receipt 'stage-j80-input-profile-*.json' 'J80' 'verified_fused01_onnx_input_profile_corrected_no_cloud_jobs'
        $private=Join-Path $dir 'private-j84-fused-context'
        New-Item -ItemType Directory -Path $private -Force | Out-Null
        $py=Get-QwenJ22AiHubPython
        if (-not $py) { throw 'Select the existing Qualcomm AI Hub Python environment in Setup.' }
        $report=Join-Path $dir ('stage-j84-fused-download-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $run=Invoke-ProcessCapture -FilePath $py -Arguments @($script:QwenAsrStageJ84J85Path,'download','--j83-receipt',$j83,'--j80-receipt',$j80,'--private-dir',$private,'--output',$report) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1200
        if (-not (Test-Path -LiteralPath $report)) { throw 'J84 download outcome unknown; inspect local files before retry.' }
        $post=Join-Path $dir ('builder-quality-postflight-j84-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j84','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J84 independent postflight failed: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ84 'Context download blocked; share J84 ZIP' 'warn'; return }
        Set-Status $lblQwenJ84 'Existing J83 context verified and stored privately' 'good'
    }
})

# J85: phone-only, validated position-8 fused block; 48 repeated calls.
$btnQwenJ85.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J85 requires Qwen 0.6B.'; return }
    Invoke-UiAction -Name 'J85: test 10 native QNN outputs and benchmark fused 0+1 S26 HTP' -ShareQwenStage 'J85' -Action {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j74=Find-QwenJ72Receipt 'stage-j74-s26-benchmark-*.json' 'J74' 'repeated_position8_s26_htp_benchmark_and_sampled_parity_pass_not_full_decoder'
        $j75=Find-QwenJ72Receipt 'stage-j75-j76-fused-fp32-*.json' 'J75' 'real_two_layer_fp32_and_fp16_cpu_parity_passed_cloud_locked'
        $j77=Find-QwenJ72Receipt 'stage-j77-fused-static-*.json' 'J77' 'fused01_position8_shape_closed_exact_fp16_parity_cloud_locked'
        $j80=Find-QwenJ72Receipt 'stage-j80-input-profile-*.json' 'J80' 'verified_fused01_onnx_input_profile_corrected_no_cloud_jobs'
        $j81=Find-QwenJ72Receipt 'stage-j81-fused-scalar-add-*.json' 'J81' 'fused01_position8_exact_rankzero_add_frozen_offline_cloud_locked'
        $j83=Find-QwenJ72Receipt 'stage-j82-j83-fused-cloud-*.json' 'J82+J83' 'fused01_corrected_scalar_add_qualcomm_compile_link_passed_device_unqualified'
        $j84=Find-QwenJ72Receipt 'stage-j84-fused-download-*.json' 'J84' 'j83_fused01_linked_context_downloaded_private_device_unqualified'
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Select the authorized S26 via Setup/Deploy first.' }
        $phone=Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $phone.IsTargetDevice) { throw 'J85 requires the authorized Samsung SM-S942U.' }
        $runtime=Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Android QNN runtime missing: ' + ($runtime.Missing -join ', ')) }
        $consent=[System.Windows.Forms.MessageBox]::Show('Run 48 identical position-8 fused Qwen layer 0+1 QNN calls on your S26? This temporarily stages the privately linked context and ten test tensors, validates all 480 native outputs, measures device-side wall time and removes the uniquely named device scratch directory. The phone may warm. No app install, cloud job, changing token positions, or true NPU kernel timing. Continue?', 'J85 S26 hardware benchmark approval', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($consent -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $p=Get-QwenStageEData
        $private=Join-Path $dir 'private-j84-fused-context'
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j85-fused-s26-benchmark-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $manifest=Join-Path $private ('j85-runtime-paths-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $runtime.Files | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifest -Encoding UTF8
        try {
            $args=@($script:QwenAsrStageJ84J85Path,'device','--onnx-dir',$p.Onnx,'--j75-receipt',$j75,'--j77-receipt',$j77,'--j80-receipt',$j80,'--j81-receipt',$j81,'--j83-receipt',$j83,'--j84-receipt',$j84,'--j74-receipt',$j74,'--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$manifest,'--private-dir',$private,'--output',$report)
            $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1800
        } finally {
            if (Test-Path -LiteralPath $manifest) { Remove-Item -LiteralPath $manifest -Force }
        }
        if (-not (Test-Path -LiteralPath $report)) { throw 'J85 did not write a receipt; inspect private device session before any retry.' }
        $post=Join-Path $dir ('builder-quality-postflight-j85-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j85','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J85 independent postflight blocked: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ85 'Device validation blocked; share one J85 ZIP' 'warn'; return }
        Set-Status $lblQwenJ85 'Fused 0+1 S26 48-call provisional parity and timings verified' 'good'
    }
})

# J86: read-only CPU decoder tuning against verified J74/J85 reference.
# Full 28-layer original FP32 step; NO app, cloud, phone, model writes.
$btnQwenJ86.Add_Click({
    if ([string]$cmbQwenAsrSize.SelectedItem -ne '0.6B') { Show-Error 'J86 requires the existing Qwen 0.6B export.'; return }
    Invoke-UiAction -Name 'J86: original Qwen FP32 CPU hybrid decoder microbenchmark (OFFLINE)' -ShareQwenStage 'J86' -Action {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        $j74=Find-QwenJ72Receipt 'stage-j74-s26-benchmark-*.json' 'J74' 'repeated_position8_s26_htp_benchmark_and_sampled_parity_pass_not_full_decoder'
        $j85=Find-QwenJ72Receipt 'stage-j85-fused-s26-benchmark-*.json' 'J85' 'fused01_position8_s26_repeated_htp_provisional_numeric_pass_not_full_decoder'
        $p=Get-QwenStageEData
        if (-not (Test-Path -LiteralPath $p.Onnx -PathType Container)) { throw 'J86 original FP32 ONNX workspace unavailable.' }
        $approval=[System.Windows.Forms.MessageBox]::Show('Profile original Qwen 28-layer FP32 CPU decoder step on this WINDOWS computer with 2 versus 4 ORT threads and KV sizes 8 and 64. Multiple large CPU sessions may take several minutes and substantial RAM. No phone, cloud, app installation or model modifications. Desktop CPU results are NOT S26 performance. Continue?', 'J86 read-only hybrid CPU experiment', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Information)
        if ($approval -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $py=Ensure-QwenAsrOnnxRuntimePython
        $report=Join-Path $dir ('stage-j86-hybrid-cpu-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $args=@($script:QwenAsrStageJ86Path,'--onnx-dir',$p.Onnx,'--j74-receipt',$j74,'--j85-receipt',$j85,'--output',$report)
        $run=Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 2400
        if (-not (Test-Path -LiteralPath $report)) { throw 'J86 did not produce a receipt. Inspect the private Windows diagnostic.' }
        $post=Join-Path $dir ('builder-quality-postflight-j86-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.json')
        $gate=Invoke-ProcessCapture -FilePath $py -Arguments @($script:BuilderQualityGatePath,'--mode','postflight-j86','--output',$post,'--combined-receipt',$report) -TimeoutSeconds 90
        if ($gate.ExitCode -ne 0) { throw ('J86 independent safety postflight blocked: ' + $post) }
        if ($run.ExitCode -ne 0) { Set-Status $lblQwenJ86 'CPU experiment blocked; share the J86 ZIP' 'warn'; return }
        Set-Status $lblQwenJ86 'Windows CPU step parity/timing passed; Android optimization unverified' 'good'
    }
})

$btnQwenStageJDeleteAuditZips=New-Button 'Delete J1-J79 audit ZIPs (manual)' 260 5311 337 40
$tabQwenAsr.Controls.Add($btnQwenStageJDeleteAuditZips)
$btnQwenStageJDeleteAuditZips.Add_Click({
    Invoke-UiAction 'Deleting only Qwen stage J1-J79 generated audit ZIP archives...' {
        $folder=Join-Path $script:QwenAsrWorkspace 'stage-j-decoder'
        if (-not (Test-Path -LiteralPath $folder -PathType Container)) { Append-Log 'No J1/J2 ZIPs found.'; return }
        $items=@(Get-ChildItem -LiteralPath $folder -Filter '*.zip' -File -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match '^(stage-j[12]-(decoder-contract|speech-shape)|stage-j-combined|stage-j-followup|stage-j14-kv-interface|stage-j15-j16-bounded|stage-j17-j19-qnn-ready|stage-j20-j21-operator-probes|stage-j22-cloud-operator-probes|stage-j23-j25-real-block|stage-j26-j28-fp16-block|stage-j29-j31-qnn-real-block|stage-j32-j34-real-node-contracts|stage-j35-j37-shape-proof|stage-j38-j40-expand-provenance|stage-j41-j42-shared-expand|stage-j43-shared-expand-cloud|stage-j44-j45-fixed-mask|stage-j46-fixed-mask-cloud|stage-j47-j48-real-fp16-package|stage-j49-real-fp16-cloud|stage-j50-existing-compile-diagnosis|stage-j51-j53-static-expand|stage-j54-j56-static-audit|stage-j57-pos8-private-package|stage-j58-j59-pos8-cloud|stage-j60-j62-reshape-boundary|stage-j63-reshape-package|stage-j64-j65-reshape-cloud|stage-j66-all-reshape|stage-j67-j68-all-reshape-cloud|stage-j69-scalar-add|stage-j70-j71-scalar-add-cloud|stage-j72-download-linked|stage-j73-s26-layer0|stage-j74-s26-benchmark|stage-j75-j76-fused-fp32|stage-j77-fused-static|stage-j78-j79-fused-cloud|builder-quality-(preflight|postflight|postflight-j13|postflight-j14|postflight-j16|postflight-j19|postflight-j21|postflight-j25|postflight-j28|postflight-j31|postflight-j34|postflight-j37|postflight-j40|postflight-j42|postflight-j43|postflight-j45|postflight-j46|postflight-j48|postflight-j49|postflight-j50|postflight-j53|postflight-j56|postflight-j57|postflight-j59|postflight-j62|postflight-j63|postflight-j65|postflight-j66|postflight-j68|postflight-j69|postflight-j71|postflight-j72|postflight-j73|postflight-j74|postflight-j75|postflight-j77|postflight-j79|[0-9]{8}))-[0-9]{8}-[0-9]{6}-[0-9]{3}(-clip0[23])?\.zip$'
        })
        if ($items.Count -eq 0) { Append-Log 'No J1/J2 generated audit ZIPs found.'; return }
        $answer=[System.Windows.Forms.MessageBox]::Show(
            ('Delete only '+$items.Count+' J1-J79 audit ZIP(s)? Original ONNX models, source, and JSON receipts remain untouched.'),
            'Confirm J1/J2 ZIP deletion',[System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        foreach ($f in $items) { Remove-Item -LiteralPath $f.FullName -Force -ErrorAction Stop }
        Append-Log ('Deleted '+$items.Count+' Qwen J audit ZIP(s), preserving originals and JSON receipts.')
    }
})

# Keep the legacy J1/J2 controls in their original nonoverlapping section
# (4795..5438). The action grid starts at 5650; never shift legacy controls
# into the grid. Fixed 80px rows + scrolling protect 100/125/150% DPI.
$btnQwenStageJDeleteAuditZips.Top = 9400
$tabQwenAsr.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,10000)



# -------------------- K-series saved + automatic path discovery --------------------
# Local preferences stay in Builder/config. They are NEVER included in diagnostic ZIPs.
$script:QwenOptimizePathsPath = Join-Path $script:SettingsDir 'qwen-optimize-paths.json'
$script:QwenOptimizeSaved = @{ ModelPackage=''; QairtFolder='' }
try {
    if (Test-Path -LiteralPath $script:QwenOptimizePathsPath -PathType Leaf) {
        $settings = Get-Content -LiteralPath $script:QwenOptimizePathsPath -Raw | ConvertFrom-Json
        if ($settings.ModelPackage) { $script:QwenOptimizeSaved.ModelPackage = [string]$settings.ModelPackage }
        if ($settings.QairtFolder) { $script:QwenOptimizeSaved.QairtFolder = [string]$settings.QairtFolder }
    }
} catch { Append-Log '[K] Saved-path preferences unreadable; will discover existing files.' }

function Test-QwenOptimizePackage {
    param([string]$Folder)
    if (-not $Folder -or -not (Test-Path -LiteralPath $Folder -PathType Container)) { return $false }
    foreach ($name in @('model-manifest.json','decoder_init.onnx','decoder_step.onnx','decoder_weights_fp16.data','encoder-qnn-fp16.bin')) {
        if (-not (Test-Path -LiteralPath (Join-Path $Folder $name) -PathType Leaf)) { return $false }
    }
    return $true
}

function Save-QwenOptimizePaths {
    try {
        New-Item -ItemType Directory -Path $script:SettingsDir -Force | Out-Null
        $data=@{ ModelPackage=([string]$script:QwenOptimizeSaved.ModelPackage); QairtFolder=([string]$script:QwenOptimizeSaved.QairtFolder) }
        $temp=$script:QwenOptimizePathsPath+'.partial'
        $data | ConvertTo-Json -Depth 2 | Set-Content -LiteralPath $temp -Encoding UTF8
        Move-Item -LiteralPath $temp -Destination $script:QwenOptimizePathsPath -Force
    } catch { Append-Log '[K] Could not save path choices; discovery will still work this session.' }
}

function Resolve-QwenOptimizePackage {
    $selected=([string]$txtQwenK1Package.Text).Trim()
    if (Test-QwenOptimizePackage $selected) { return $selected }
    $saved=[string]$script:QwenOptimizeSaved.ModelPackage
    if (Test-QwenOptimizePackage $saved) { $txtQwenK1Package.Text=$saved; return $saved }
    $stageG=Join-Path (Join-Path $script:QwenAsrWorkspace '0.6b') 'stage-g-hybrid-deployment'
    if (Test-Path -LiteralPath $stageG -PathType Container) {
        $candidates=@(Get-ChildItem -LiteralPath $stageG -Directory -Filter 'experimental-hybrid-fp16-storage-*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending)
        foreach ($candidate in $candidates) {
            if (Test-QwenOptimizePackage $candidate.FullName) {
                $txtQwenK1Package.Text=$candidate.FullName
                $script:QwenOptimizeSaved.ModelPackage=$candidate.FullName
                Save-QwenOptimizePaths
                return $candidate.FullName
            }
        }
    }
    return ''
}

function Test-QwenOptimizeSdkRoot {
    param([string]$Folder)
    return ($Folder -and (Test-Path -LiteralPath (Join-Path $Folder 'bin') -PathType Container) -and
        (Test-Path -LiteralPath (Join-Path $Folder 'lib') -PathType Container))
}

function Find-QwenOptimizeNestedSdk {
    param([string]$Folder)
    if (-not $Folder -or -not (Test-Path -LiteralPath $Folder -PathType Container)) { return '' }
    if (Test-QwenOptimizeSdkRoot $Folder) { return $Folder }
    # Look only in QAIRT/version-named subdirectories, never recurse through models or docs.
    $level=@([string]$Folder)
    for ($depth=0; $depth -lt 5; $depth++) {
        $next=@()
        foreach ($d in $level) {
            foreach ($child in @(Get-ChildItem -LiteralPath $d -Directory -ErrorAction SilentlyContinue)) {
                if ($child.Name -match '^(qairt|qnn|qualcomm|sdk|v?\d+\.\d+\.\d+(\.\d+)?)$') {
                    if (Test-QwenOptimizeSdkRoot $child.FullName) { return $child.FullName }
                    $next+= $child.FullName
                }
            }
        }
        $level=@($next | Select-Object -First 50)
        if ($level.Count -eq 0) { break }
    }
    return ''
}

function Resolve-QwenOptimizeQairt {
    $choices=@(([string]$txtQwenK4Qairt.Text).Trim(),
               ([string]$script:QwenOptimizeSaved.QairtFolder),
               (Join-Path $Root 'tools'),(Join-Path $Root 'qairt'),(Join-Path $Root 'sdk'))
    foreach ($choice in $choices) {
        $resolved=Find-QwenOptimizeNestedSdk $choice
        if ($resolved) {
            $txtQwenK4Qairt.Text=$resolved
            $script:QwenOptimizeSaved.QairtFolder=$resolved
            Save-QwenOptimizePaths
            return $resolved
        }
    }
    return ''
}

# -------------------- Qwen3 ASR - compact hybrid optimization (NEW) --------------------
# Keep Stage J historical R&D intact on its original tab; K1 is read-only.
$tabQwenOptimize = New-Object System.Windows.Forms.TabPage
$tabQwenOptimize.Text = 'Qwen ASR - Optimize'
$tabQwenOptimize.AutoScroll = $true
$tabQwenOptimize.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,2360)
[void]$tabs.TabPages.Add($tabQwenOptimize)
$tabQwenOptimize.Controls.Add((New-Label 'QWEN3-ASR 0.6B | HYBRID STORAGE + DECODER OPTIMIZATION' 25 16 850 32 -Bold))
$tabQwenOptimize.Controls.Add((New-Label 'Stage K1 reads an EXISTING verified Stage I package. It never downloads, exports, compiles, uploads, changes model files or stages files on the S26. Future K experiments must verify device cleanup in a finally block before accepting results.' 25 57 850 72))
$tabQwenOptimize.Controls.Add((New-Label 'Stage I model package (auto-discovered; Browse only if needed)' 25 158 820 27 -Bold))
$txtQwenK1Package = New-TextBox 25 192 653 30
$tabQwenOptimize.Controls.Add($txtQwenK1Package)
$btnQwenK1Browse = New-Button 'Browse...' 690 189 148 36
$tabQwenOptimize.Controls.Add($btnQwenK1Browse)
$btnQwenK1Browse.Add_Click({
    try {
        Choose-Folder $txtQwenK1Package 'Select the existing Stage I package containing model-manifest.json'
        if (Test-QwenOptimizePackage $txtQwenK1Package.Text) {
            $script:QwenOptimizeSaved.ModelPackage=$txtQwenK1Package.Text; Save-QwenOptimizePaths
        }
    } catch { Show-Error $_ }
})
$btnQwenK1Find = New-Button 'Find existing Stage I package' 25 245 315 40
$tabQwenOptimize.Controls.Add($btnQwenK1Find)
$lblQwenK1Found = New-StatusLabel 359 253 475
$tabQwenOptimize.Controls.Add($lblQwenK1Found)
$btnQwenK1Find.Add_Click({
    try {
        $folder=Resolve-QwenOptimizePackage
        if (-not $folder) { throw 'No existing Stage I package found. Browse to an existing package; no download or K1 rerun is required.' }
        Set-Status $lblQwenK1Found 'Model folder selected automatically; K1 is optional' 'good'
    } catch { Show-Error $_ }
})
# Prefill from existing files or previous validated selection at startup.
$null=Resolve-QwenOptimizePackage
$tabQwenOptimize.Controls.Add((New-Label 'K1 - Read-only file inventory, package schema, duplicate weight indicators and embedding savings' 25 326 844 34 -Bold))
$tabQwenOptimize.Controls.Add((New-Label 'Every run generates ONE safe-to-share ZIP, even when blocked. The exact ZIP is selected in Explorer for drag-and-drop to ChatGPT. Detailed private files/weights are NOT included. K1 does not hash multi-GB files, test runtime parity, or measure S26 RAM.' 25 370 848 79))
$btnQwenK1Run = New-Button 'RUN K1: audit existing Qwen package (OFFLINE)' 25 465 490 46
$tabQwenOptimize.Controls.Add($btnQwenK1Run)
$lblQwenK1Run = New-StatusLabel 530 473 315
$tabQwenOptimize.Controls.Add($lblQwenK1Run)
$btnQwenK1Run.Add_Click({
    Invoke-UiAction -Name 'K1 Qwen package size / runtime contract inventory (read only, no phone)' -Action {
        $folder = Resolve-QwenOptimizePackage
        $share = Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        New-Item -ItemType Directory -Path $share -Force | Out-Null
        $output = Join-Path $share ('Qwen-K1-audit-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
        # K1 never auto-installs Python or downloads tooling: use the already
        # configured interpreter, falling back only to an existing portable one.
        $python = ([string]$txtPython.Text).Trim()
        if (-not $python -or -not (Test-Path -LiteralPath $python -PathType Leaf)) {
            $python = Join-Path $Root 'tools\python311\python.exe'
        }
        if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
            throw 'K1_PYTHON_MISSING: select an installed Python in Builder Setup; K1 will not download it.'
        }
        $result = $null
        try {
            # Always invoke the script, even with a missing package, so it creates a
            # sanitized failure ZIP. The python runtime itself is the only prerequisite.
            $result = Invoke-ProcessCapture -FilePath $python -Arguments @($script:QwenAsrStageK1Path,'--package',$folder,'--output',$output) -TimeoutSeconds 120
            if (-not (Test-Path -LiteralPath $output -PathType Leaf)) { throw 'K1_ZIP_NOT_CREATED: inspect Builder error support bundle.' }
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            $verify = [System.IO.Compression.ZipFile]::OpenRead($output)
            try {
                $names = @($verify.Entries | ForEach-Object { $_.FullName })
                if ($names.Count -ne 4 -or @($names | Where-Object { $_ -notin @('README.txt','audit.json','file-breakdown.csv','stop-go-criteria.json') }).Count -gt 0) {
                    throw 'K1_ZIP_CONTENT_VALIDATION_FAILED'
                }
            } finally { $verify.Dispose() }
            Append-Log ('[K1] Drag this verified diagnostic ZIP to chat: ' + $output)
            if ($result.ExitCode -eq 0) {
                Set-Status $lblQwenK1Run 'Audit ready; review ZIP findings' 'good'
            } else {
                Set-Status $lblQwenK1Run 'Audit blocked; send ZIP for diagnosis' 'warn'
            }
        } finally {
            if (Test-Path -LiteralPath $output -PathType Leaf) {
                try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $output) | Out-Null }
                catch { Open-Folder $share }
            } else { Open-Folder $share }
        }
    }
})
$btnQwenK1Results = New-Button 'Open K1 diagnostic ZIP folder' 25 531 330 39
$tabQwenOptimize.Controls.Add($btnQwenK1Results)
$btnQwenK1Results.Add_Click({
    try {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Open-Folder $dir
    } catch { Show-Error $_ }
})
$btnQwenK1Delete = New-Button 'Delete K1 report ZIPs (manual)' 371 531 310 39
$tabQwenOptimize.Controls.Add($btnQwenK1Delete)
$btnQwenK1Delete.Add_Click({
    Invoke-UiAction 'Deleting only K1 shareable diagnostic archives...' {
        $dir=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        if (-not (Test-Path -LiteralPath $dir -PathType Container)) { return }
        $items=@(Get-ChildItem -LiteralPath $dir -File -Filter 'Qwen-K1-audit-*.zip' -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match '^Qwen-K1-audit-[0-9]{8}-[0-9]{6}-[0-9]{3}\.zip$'
        })
        if ($items.Count -eq 0) { Append-Log 'No K1 ZIPs to remove.'; return }
        $choice=[System.Windows.Forms.MessageBox]::Show(('Delete '+$items.Count+' K1 diagnostic ZIP(s)? Model files, Builder workspace and other reports will remain untouched.'), 'Delete K1 diagnostic archives',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        foreach ($item in $items) { Remove-Item -LiteralPath $item.FullName -Force }
        Append-Log ('Deleted '+$items.Count+' K1 diagnostic ZIP(s); all model files preserved.')
    }
})
$btnQwenK1ManualPhoneCleanup = New-Button 'S26: Clear old Builder test files (manual)' 25 591 385 39
$tabQwenOptimize.Controls.Add($btnQwenK1ManualPhoneCleanup)
$btnQwenK1ManualPhoneCleanup.Add_Click({
    $tabs.SelectedTab = $tabQwenAsr
    $btnQwenStageFClear.PerformClick()
})
$tabQwenOptimize.Controls.Add((New-Label 'STOP / GO POLICY: prioritize a candidate only if its expected storage saving is material or if measured S26 latency, accuracy and thermals justify it. Stop a runtime pathway after two comparable phone runs show <5% sustained end-to-end improvement or unacceptable quality / memory / thermal regressions. No endless retries for compilation alone.' 25 651 847 104))
$tabQwenOptimize.Controls.Add((New-Label 'S26 SAFETY: K1 is offline and makes NO phone files. Future K device tests must use the existing allowlisted staging and independent cleanup verification, including failure/cancellation. For interrupted historical Stage J tests, use Qwen3 ASR NPU > Clear previous S26 test files (manual).' 25 779 847 85))

# -------------------- K2: isolated FP16 embedding candidate and INT8 feasibility --------------------
$tabQwenOptimize.Controls.Add((New-Label 'K2A / K2B - Compact embeddings and INT8 decoder feasibility (OFFLINE)' 25 920 844 33 -Bold))
$tabQwenOptimize.Controls.Add((New-Label 'Uses the same selected Stage I folder above. Both actions first SHA-256 verify all manifest-listed model files (multi-GB: allow several minutes). K2A creates ONE isolated 311 MB FP16 embedding candidate; no originals are edited. K2B is a graph/runtime feasibility screen only and creates NO INT8 weights.' 25 959 845 85))
$btnQwenK2A = New-Button 'RUN K2A: verify + FP16 embedding candidate' 25 1056 520 43
$tabQwenOptimize.Controls.Add($btnQwenK2A)
$script:lblQwenK2A = New-StatusLabel 560 1064 280
$tabQwenOptimize.Controls.Add($script:lblQwenK2A)
$btnQwenK2B = New-Button 'RUN K2B: verify + INT8 decoder feasibility' 25 1118 520 43
$tabQwenOptimize.Controls.Add($btnQwenK2B)
$script:lblQwenK2B = New-StatusLabel 560 1126 280
$tabQwenOptimize.Controls.Add($script:lblQwenK2B)
$tabQwenOptimize.Controls.Add((New-Label 'Safety: K2A files are NOT deployable until a new Android embedding loader and real-audio token parity are verified. K2B must NOT be mistaken for INT8 conversion or a speed benchmark. Both write a sanitized ZIP on success or model-validation failure; Excel/Explorer auto-selects the report, never the weights.' 25 1173 845 86))
function Invoke-QwenK2Stage {
    param([ValidateSet('embeddings','decoder-feasibility')][string]$Mode)
    $package=Resolve-QwenOptimizePackage
    $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
    $candidates=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\candidates'
    New-Item -ItemType Directory -Path $share -Force | Out-Null
    $stamp=Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $stage= if ($Mode -eq 'embeddings') {'K2A'} else {'K2B'}
    $report=Join-Path $share ('Qwen-'+$stage+'-audit-'+$stamp+'.zip')
    $python=([string]$txtPython.Text).Trim()
    if (-not $python -or -not (Test-Path -LiteralPath $python -PathType Leaf)) {
        $python=Join-Path $Root 'tools\python311\python.exe'
    }
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
        throw 'K2_PYTHON_MISSING: configure an installed Python in Setup; no dependencies will be downloaded.'
    }
    if ($Mode -eq 'embeddings') {
        $answer=[System.Windows.Forms.MessageBox]::Show(
          'K2A verifies the full Stage I model against SHA-256 and creates an approximately 311 MB FP16 embedding candidate in the Builder-owned stage-k-model-optimization/candidates folder. It does NOT modify originals, update Android, or run the phone. Continue?',
          'K2A experimental conversion consent',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Question)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    }
    $k2Argv=@($script:QwenAsrStageK2Path,'--mode',$Mode,'--package',$package,'--output',$report)
    if ($Mode -eq 'embeddings') {
        $candidate=Join-Path $candidates ('K2A-fp16-embedding-'+$stamp)
        $k2Argv+=@('--candidate-dir',$candidate)
    }
    try {
        # Python writes the sanitized share ZIP on success OR blocked conversion/validation.
        # Full SHA-256 over ~2.5 GB must not be subjected to the short K1 timeout.
        $run=Invoke-ProcessCapture -FilePath $python -Arguments $k2Argv -TimeoutSeconds 3600
        if (-not (Test-Path -LiteralPath $report -PathType Leaf)) {
            throw 'K2_REPORT_NOT_CREATED: inspect the Builder error support bundle.'
        }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $check=[System.IO.Compression.ZipFile]::OpenRead($report)
        try {
            $names=@($check.Entries | ForEach-Object { $_.FullName })
            if ($names.Count -ne 3 -or @($names | Where-Object {
                $_ -notin @('README.txt','result.json','stop-go-criteria.json')
            }).Count -gt 0) { throw 'K2_REPORT_UNEXPECTED_CONTENTS' }
        } finally { $check.Dispose() }
        Append-Log ('['+$stage+'] Share ONLY this diagnostic ZIP: '+$report)
        $lbl=if ($Mode -eq 'embeddings') {$script:lblQwenK2A} else {$script:lblQwenK2B}
        if ($run.ExitCode -eq 0) {
            Set-Status $lbl 'ZIP created; review findings' 'good'
        } else { Set-Status $lbl 'Blocked: send ZIP for diagnosis' 'warn' }
    } finally {
        if (Test-Path -LiteralPath $report -PathType Leaf) {
            try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $report) | Out-Null }
            catch { Open-Folder $share }
        } else { Open-Folder $share }
    }
}
$btnQwenK2A.Add_Click({ Invoke-UiAction -Name 'K2A verified FP16 embedding candidate (offline)' -Action { Invoke-QwenK2Stage -Mode 'embeddings' } })
$btnQwenK2B.Add_Click({ Invoke-UiAction -Name 'K2B INT8 decoder feasibility screen (offline)' -Action { Invoke-QwenK2Stage -Mode 'decoder-feasibility' } })
$btnQwenK2Open = New-Button 'Open K2 report ZIP folder' 25 1281 310 38
$tabQwenOptimize.Controls.Add($btnQwenK2Open)
$btnQwenK2Open.Add_Click({
    $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
    New-Item -ItemType Directory -Path $share -Force | Out-Null
    Open-Folder $share
})
$btnQwenK2Delete = New-Button 'Delete K2 diagnostic ZIPs (manual)' 353 1281 365 38
$tabQwenOptimize.Controls.Add($btnQwenK2Delete)
$btnQwenK2Delete.Add_Click({
    Invoke-UiAction 'Deleting only Builder-owned K2 diagnostic ZIPs...' {
        $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        if (-not (Test-Path -LiteralPath $share -PathType Container)) { return }
        $items=@(Get-ChildItem -LiteralPath $share -File -Filter 'Qwen-K2*-audit-*.zip' -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match '^Qwen-K2[AB]-audit-[0-9]{8}-[0-9]{6}-[0-9]{3}\.zip$'
        })
        if ($items.Count -eq 0) { Append-Log 'No K2 diagnostic ZIPs found'; return }
        $choice=[System.Windows.Forms.MessageBox]::Show(
            ('Delete '+$items.Count+' K2 report ZIP(s)? No candidate tensors, model files, J-stage receipts, or phone data will be deleted.'),
            'Delete K2 diagnostic ZIPs',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        foreach ($item in $items) { Remove-Item -LiteralPath $item.FullName -Force -ErrorAction Stop }
        Append-Log ('Removed '+$items.Count+' K2 reports; protected model files retained.')
    }
})
$tabQwenOptimize.Controls.Add((New-Label 'CLEANUP POLICY: K2 is offline, creates zero phone files, and does not call ADB. Future K-series phone runs require try/finally deletion of allowlisted Builder-only staging plus independent verification, including failures and cancellation. Manual S26 cleanup remains above.' 25 1351 845 100))

# -------------------- K3: portable INT8 arithmetic probe (no SDK/ORT install) --------------------
$tabQwenOptimize.Controls.Add((New-Label 'K3 - INT8 numerical / host CPU matrix probe (OFFLINE, NO NEW INSTALLS)' 25 1470 850 35 -Bold))
$tabQwenOptimize.Controls.Add((New-Label 'Reuses the existing Builder Python + NumPy and ONNX (if available). Verifies Stage I hashes and samples at most 256x256 real decoder FP16 weights for INT8-vs-FP32 numerical comparison. Falls back to explicitly labeled synthetic inputs when ONNX sampling is unavailable. Windows NumPy times do NOT predict S26 speed or prove native INT8 kernels.' 25 1511 844 105))
$btnQwenK3 = New-Button 'RUN K3: portable INT8 math qualification (NO INSTALL)' 25 1630 550 45
$tabQwenOptimize.Controls.Add($btnQwenK3)
$script:lblQwenK3 = New-StatusLabel 595 1638 244
$tabQwenOptimize.Controls.Add($script:lblQwenK3)
$btnQwenK3.Add_Click({
    Invoke-UiAction -Name 'K3 verified portable INT8 numerical probe (no Qualcomm / phone)' -Action {
        $folder=Resolve-QwenOptimizePackage
        $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        New-Item -ItemType Directory -Path $share -Force | Out-Null
        $report=Join-Path $share ('Qwen-K3-audit-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
        $python=([string]$txtPython.Text).Trim()
        if (-not $python -or -not (Test-Path -LiteralPath $python -PathType Leaf)) {
            $python=Join-Path $Root 'tools\python311\python.exe'
        }
        if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
            throw 'K3_PYTHON_MISSING: reuse the SAME Python already used for K2; K3 will not download or install anything.'
        }
        try {
            $out=Invoke-ProcessCapture -FilePath $python -Arguments @(
                $script:QwenAsrStageK3Path,'--package',$folder,'--output',$report
            ) -TimeoutSeconds 3600
            if (-not (Test-Path -LiteralPath $report -PathType Leaf)) {
                throw 'K3_DIAGNOSTIC_ZIP_MISSING: inspect Builder support logs.'
            }
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            $archive=[System.IO.Compression.ZipFile]::OpenRead($report)
            try {
                $names=@($archive.Entries | ForEach-Object { $_.FullName })
                if ($names.Count -ne 3 -or @($names | Where-Object {
                    $_ -notin @('README.txt','result.json','decision.json')
                }).Count -gt 0) { throw 'K3_ZIP_CONTENT_INVALID' }
            } finally { $archive.Dispose() }
            Append-Log ('[K3] Share ONLY this ZIP (no weights): '+$report)
            if ($out.ExitCode -eq 0) {
                Set-Status $script:lblQwenK3 'Numeric ZIP ready - send to chat' 'good'
            } else {
                Set-Status $script:lblQwenK3 'Blocked - send diagnostic ZIP' 'warn'
            }
        } finally {
            if (Test-Path -LiteralPath $report -PathType Leaf) {
                try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $report) | Out-Null }
                catch { Open-Folder $share }
            } else { Open-Folder $share }
        }
    }
})
$btnQwenK3Open = New-Button 'Open K3 diagnostic ZIP folder' 25 1710 335 39
$tabQwenOptimize.Controls.Add($btnQwenK3Open)
$btnQwenK3Open.Add_Click({
    $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
    New-Item -ItemType Directory -Path $share -Force | Out-Null
    Open-Folder $share
})
$btnQwenK3Delete = New-Button 'Delete K3 report ZIPs (manual)' 385 1710 355 39
$tabQwenOptimize.Controls.Add($btnQwenK3Delete)
$btnQwenK3Delete.Add_Click({
    Invoke-UiAction 'Deleting only K3 report ZIPs...' {
        $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        if (-not (Test-Path -LiteralPath $share -PathType Container)) { return }
        $items=@(Get-ChildItem -LiteralPath $share -File -Filter 'Qwen-K3-audit-*.zip' -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match '^Qwen-K3-audit-[0-9]{8}-[0-9]{6}-[0-9]{3}\.zip$'
        })
        if ($items.Count -eq 0) { Append-Log 'No K3 ZIPs to delete.'; return }
        $choice=[System.Windows.Forms.MessageBox]::Show(
          ('Delete '+$items.Count+' K3 ZIP(s)? Models, candidates and prior J/K reports stay untouched.'),
          'Delete K3 diagnostic ZIPs',[System.Windows.Forms.MessageBoxButtons]::YesNo,
          [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        foreach ($item in $items) { Remove-Item -LiteralPath $item.FullName -Force -ErrorAction Stop }
        Append-Log ('Deleted '+$items.Count+' K3 diagnostic ZIP(s).')
    }
})
$tabQwenOptimize.Controls.Add((New-Label 'K3 changes NO model files, does not call ADB, and creates NO phone staging. QAIRT may remain extracted in your existing folder. Only an actual supported ARM decoder kernel + S26 accuracy/latency tests can justify full model INT8 conversion.' 25 1770 844 83))

# -------------------- K4: portable runtime qualification (NO INSTALL) --------------------
$tabQwenOptimize.Controls.Add((New-Label 'K4 - Inventory available native runtimes and qualify an executable INT8 CPU pathway (OFFLINE)' 25 1875 845 35 -Bold))
$tabQwenOptimize.Controls.Add((New-Label 'QAIRT auto-discovers nested version/qairt/version folders; Browse once only if located elsewhere. K4 inspects native runtime headers, architecture and roles. QNN CPU is a REFERENCE backend, not proof of Snapdragon INT8 speed. If ONNX Runtime is already available in the selected Python, a small REAL host QLinearMatMul execution is attempted. Neither result proves S26 decoding performance.' 25 1920 846 110))
$txtQwenK4Qairt = New-TextBox 25 2045 650 29
$tabQwenOptimize.Controls.Add($txtQwenK4Qairt)
$btnQwenK4Qairt = New-Button 'Browse QAIRT...' 688 2041 150 37
$tabQwenOptimize.Controls.Add($btnQwenK4Qairt)
$btnQwenK4Qairt.Add_Click({
    try {
        Choose-Folder $txtQwenK4Qairt 'Select existing extracted QAIRT folder (optional)'
        $resolved=Resolve-QwenOptimizeQairt
        if (-not $resolved) { Append-Log '[K4] The selected folder does not contain a recognizable nested QAIRT SDK; report will record this.' }
    } catch { Show-Error $_ }
})
$null=Resolve-QwenOptimizeQairt
$btnQwenK4 = New-Button 'RUN K4: inspect portable decoder runtimes (NO INSTALL)' 25 2102 555 47
$tabQwenOptimize.Controls.Add($btnQwenK4)
$script:lblQwenK4 = New-StatusLabel 594 2110 245
$tabQwenOptimize.Controls.Add($script:lblQwenK4)
$btnQwenK4.Add_Click({
    Invoke-UiAction -Name 'K4 portable native CPU runtime inventory (no phone/SDK install/cloud)' -Action {
        $folder=Resolve-QwenOptimizePackage
        $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        New-Item -ItemType Directory -Path $share -Force | Out-Null
        $report=Join-Path $share ('Qwen-K4-audit-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
        $python=([string]$txtPython.Text).Trim()
        if (-not $python -or -not (Test-Path -LiteralPath $python -PathType Leaf)) {
            $python=Join-Path $Root 'tools\python311\python.exe'
        }
        if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
            throw 'K4_PYTHON_MISSING: use the existing portable Python used by K2/K3; K4 installs nothing.'
        }
        $argv=@($script:QwenAsrStageK4Path,'--package',$folder,'--builder-root',$Root,'--output',$report)
        $qairt=Resolve-QwenOptimizeQairt
        if ($qairt) { $argv+=@('--runtime-folder',$qairt) }
        try {
            $out=Invoke-ProcessCapture -FilePath $python -Arguments $argv -TimeoutSeconds 3600
            if (-not (Test-Path -LiteralPath $report -PathType Leaf)) {
                throw 'K4_REPORT_ZIP_MISSING: inspect Builder support log.'
            }
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            $archive=[System.IO.Compression.ZipFile]::OpenRead($report)
            try {
                $names=@($archive.Entries | ForEach-Object { $_.FullName })
                if ($names.Count -ne 4 -or @($names | Where-Object {
                    $_ -notin @('README.txt','result.json','decision.json','runtime-inventory.json')
                }).Count -gt 0) { throw 'K4_REPORT_CONTENT_INVALID' }
            } finally { $archive.Dispose() }
            Append-Log ('[K4] Share ONLY this diagnostic ZIP: '+$report)
            if ($out.ExitCode -eq 0) {
                Set-Status $script:lblQwenK4 'Inventory ready; send ZIP to chat' 'good'
            } else {
                Set-Status $script:lblQwenK4 'Blocked; send diagnostic ZIP' 'warn'
            }
        } finally {
            if (Test-Path -LiteralPath $report -PathType Leaf) {
                try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $report) | Out-Null }
                catch { Open-Folder $share }
            } else { Open-Folder $share }
        }
    }
})
$btnQwenK4Open = New-Button 'Open K4 diagnostic ZIP folder' 25 2167 335 39
$tabQwenOptimize.Controls.Add($btnQwenK4Open)
$btnQwenK4Open.Add_Click({
    $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
    New-Item -ItemType Directory -Path $share -Force | Out-Null
    Open-Folder $share
})
$btnQwenK4Delete = New-Button 'Delete K4 report ZIPs (manual)' 385 2167 355 39
$tabQwenOptimize.Controls.Add($btnQwenK4Delete)
$btnQwenK4Delete.Add_Click({
    Invoke-UiAction 'Deleting only K4 report ZIPs...' {
        $share=Join-Path $script:QwenAsrWorkspace 'stage-k-model-optimization\share-to-chat'
        if (-not (Test-Path -LiteralPath $share -PathType Container)) { return }
        $items=@(Get-ChildItem -LiteralPath $share -File -Filter 'Qwen-K4-audit-*.zip' -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match '^Qwen-K4-audit-[0-9]{8}-[0-9]{6}-[0-9]{3}\.zip$'
        })
        if ($items.Count -eq 0) { Append-Log 'No K4 ZIPs to delete.'; return }
        $choice=[System.Windows.Forms.MessageBox]::Show(
          ('Delete '+$items.Count+' K4 ZIP(s)? Models, original files and other diagnostic reports will be preserved.'),
          'Delete K4 diagnostic ZIPs',[System.Windows.Forms.MessageBoxButtons]::YesNo,
          [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        foreach ($item in $items) { Remove-Item -LiteralPath $item.FullName -Force -ErrorAction Stop }
        Append-Log ('Deleted '+$items.Count+' K4 diagnostic ZIP(s).')
    }
})
$tabQwenOptimize.Controls.Add((New-Label 'K4 creates NO S26 staging, invokes NO QAIRT or native model runtime tools, and does NOT generate INT8 weights. It checks already-present libraries without loading them. If the optional host ORT kernel executes, its timing is NOT a target-ARM benchmark. Stop quantized conversion unless a production-compatible ARM64 backend and device benchmark are demonstrated.' 25 2225 845 112))

# -------------------- Omi Med NPU (Parakeet v2) --------------------
$tabOmi = New-Object System.Windows.Forms.TabPage
$tabOmi.Text = 'Omi Med NPU'
$tabOmi.AutoScroll = $true
$tabOmi.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,2830)
[void]$tabs.TabPages.Add($tabOmi)
$tabOmi.Controls.Add((New-Label 'OMI MED STT V1 - Guided Galaxy S26 NPU conversion' 25 16 760 28 -Bold))
$lblOmiIntro = New-Label 'This workflow is separate from the validated Parakeet v3 NPU path. It uses the original Omi v2 NeMo checkpoint with all 24 medical adapters. The GUI runs the tools; do not use a terminal.' 25 50 850 46
$tabOmi.Controls.Add($lblOmiIntro)

$tabOmi.Controls.Add((New-Label 'STEP 1 - Setup native Windows export environment (one-time)' 25 110 680 28 -Bold))
$btnOmiSetup = New-Button 'Install / repair Windows Python' 25 146 255 36
$tabOmi.Controls.Add($btnOmiSetup)
$lblOmiSetup = New-StatusLabel 295 153 585
$tabOmi.Controls.Add($lblOmiSetup)
$lblOmiSetupDetail = New-Label 'Reuses the native Windows Parakeet PyTorch + Transformers environment and adds PyYAML. No WSL2, Ubuntu, or NeMo installation. A short encoder export is experimental.' 25 192 855 48
$tabOmi.Controls.Add($lblOmiSetupDetail)

$tabOmi.Controls.Add((New-Label 'STEP 2 - Download and verify canonical Omi checkpoint' 25 245 680 28 -Bold))
$btnOmiDownload = New-Button 'Download original Omi model' 25 281 255 36
$tabOmi.Controls.Add($btnOmiDownload)
$btnOmiSourcePage = New-Button 'Source repository' 292 281 152 36
$tabOmi.Controls.Add($btnOmiSourcePage)
$lblOmiDownload = New-StatusLabel 462 289 420
$tabOmi.Controls.Add($lblOmiDownload)
$txtOmiNemo = New-TextBox 25 328 850 26 $false $true
$tabOmi.Controls.Add($txtOmiNemo)

$tabOmi.Controls.Add((New-Label 'STEP 3 - Native ONNX export and medical adapter verification' 25 370 710 28 -Bold))
$tabOmi.Controls.Add((New-Label 'Non-sensitive reference audio (.wav/.ogg)' 25 404 320 24))
$txtOmiAudio = New-TextBox 25 431 700 27
$tabOmi.Controls.Add($txtOmiAudio)
$btnOmiAudio = New-Button 'Browse...' 735 429 120 31
$tabOmi.Controls.Add($btnOmiAudio)
$btnOmiExport = New-Button 'Export + test Omi encoder' 25 476 260 36
$tabOmi.Controls.Add($btnOmiExport)
$lblOmiExport = New-StatusLabel 295 483 580
$tabOmi.Controls.Add($lblOmiExport)

$tabOmi.Controls.Add((New-Label 'STEP 4 - Upload ONNX to Qualcomm AI Hub and compile' 25 539 750 28 -Bold))
$btnOmiCompile = New-Button 'Compile qualified encoder on AI Hub' 25 574 300 36
$tabOmi.Controls.Add($btnOmiCompile)
$lblOmiCompile = New-StatusLabel 345 582 530
$tabOmi.Controls.Add($lblOmiCompile)
$lblOmiCompileGuide = New-Label 'Uses the qualified SHORT encoder candidate. Not yet compared with canonical NeMo or tested on the S26; long-audio local attention and TDT decoder are NOT implemented.' 25 620 850 52
$tabOmi.Controls.Add($lblOmiCompileGuide)

$tabOmi.Controls.Add((New-Label 'STEP 5 - Validate the existing compiled encoder on Galaxy S26 HTP' 25 683 825 28 -Bold))
$btnOmiHtpTest = New-Button 'Test Omi encoder on S26 NPU' 25 721 280 38
$tabOmi.Controls.Add($btnOmiHtpTest)
$lblOmiHtpTest = New-StatusLabel 320 731 550
$tabOmi.Controls.Add($lblOmiHtpTest)
$lblOmiHtpGuide = New-Label 'Tests the SAME compiled encoder (no cloud recompile). If numerical limits fail, the raw S26 output stays on Windows for offline analysis. Phone test staging is cleaned after execution.' 25 770 855 42
$tabOmi.Controls.Add($lblOmiHtpGuide)
$btnOmiAnalyze = New-Button 'Analyze saved S26 output (offline)' 25 816 298 37
$tabOmi.Controls.Add($btnOmiAnalyze)
$lblOmiAnalyze = New-StatusLabel 335 825 535
$tabOmi.Controls.Add($lblOmiAnalyze)
$tabOmi.Controls.Add((New-Label 'STEP 5B - Test a DIFFERENT recording (diagnostic only; same compiled .bin)' 25 867 835 25 -Bold))
$txtOmiSecondAudio = New-TextBox 25 900 700 27
$tabOmi.Controls.Add($txtOmiSecondAudio)
$btnOmiSecondBrowse = New-Button 'Browse...' 735 898 120 31
$tabOmi.Controls.Add($btnOmiSecondBrowse)
$btnOmiSecond = New-Button 'Compare second recording on S26' 25 938 315 37
$tabOmi.Controls.Add($btnOmiSecond)
$lblOmiSecond = New-StatusLabel 357 947 515
$tabOmi.Controls.Add($lblOmiSecond)
$tabOmi.Controls.Add((New-Label 'Uses the same 12-second encoder context and new separate ONNX tensors. Does not recompile, overwrite the original reference, or qualify the model. S26 staging is cleaned.' 25 982 852 51))
$tabOmi.Controls.Add((New-Label 'STEP 5C - Inspect Omi TDT decoder architecture (no cloud compilation)' 25 1036 845 28 -Bold))
$btnOmiDecoderProbe = New-Button 'Inspect decoder weights + contract' 25 1074 318 37
$tabOmi.Controls.Add($btnOmiDecoderProbe)
$lblOmiDecoderProbe = New-StatusLabel 361 1083 508
$tabOmi.Controls.Add($lblOmiDecoderProbe)
$tabOmi.Controls.Add((New-Label 'Reads source decoder/joint shapes and NeMo configuration on Windows. This is NOT a decoder export or a transcript validation.' 25 1120 850 35))
$tabOmi.Controls.Add((New-Label 'STEP 5D - Export original Omi v2 TDT predictor + joint ONNX (desktop)' 25 1180 850 28 -Bold))
$btnOmiTdtExport = New-Button 'Export + validate Omi v2 decoder graphs' 25 1216 350 38
$tabOmi.Controls.Add($btnOmiTdtExport)
$lblOmiTdtExport = New-StatusLabel 387 1226 490
$tabOmi.Controls.Add($lblOmiTdtExport)
$tabOmi.Controls.Add((New-Label 'Uses original Omi LSTM and joint weights; checks nonzero recurrent states and ONNX Runtime parity. No S26 compilation yet. Host token embedding included. Does not qualify full transcription.' 25 1264 855 48))
$tabOmi.Controls.Add((New-Label 'STEP 5E - Compile Omi v2 predictor + joint on AI Hub (FP16)' 25 1335 840 28 -Bold))
$btnOmiTdtCompile = New-Button 'Compile Omi v2 decoder QNN contexts' 25 1373 340 38
$tabOmi.Controls.Add($btnOmiTdtCompile)
$lblOmiTdtCompile = New-StatusLabel 385 1382 485
$tabOmi.Controls.Add($lblOmiTdtCompile)
$tabOmi.Controls.Add((New-Label 'Uses the already validated ONNX predictor + joint graphs. Reuses both contexts on subsequent runs. Encoder is NOT recompiled.' 25 1418 850 32))
$tabOmi.Controls.Add((New-Label 'STEP 5F - Compare predictor + joint against S26 Hexagon NPU' 25 1461 835 28 -Bold))
$btnOmiTdtDevice = New-Button 'Test Omi v2 decoder on S26 NPU' 25 1498 340 38
$tabOmi.Controls.Add($btnOmiTdtDevice)
$btnOmiTdtRecover = New-Button 'Recover saved Step 5F result (offline)' 390 1498 342 38
$tabOmi.Controls.Add($btnOmiTdtRecover)
$lblOmiTdtDevice = New-StatusLabel 25 1549 850
$tabOmi.Controls.Add($lblOmiTdtDevice)
$tabOmi.Controls.Add((New-Label 'The offline recovery rechecks SAVED QNN output tensors and context/reference hashes without using ADB. Device tests use phone auto-clean. No full transcript is qualified.' 25 1586 850 42))
$tabOmi.Controls.Add((New-Label 'STEP 5G - First desktop Omi v2 TDT transcript (CPU ONNX, 12s window)' 25 1650 850 28 -Bold))
$tabOmi.Controls.Add((New-Label 'Non-sensitive .wav/.ogg to transcribe (optional; defaults to Step 3 audio)' 25 1684 755 24))
$txtOmiTranscriptAudio = New-TextBox 25 1710 700 27
$tabOmi.Controls.Add($txtOmiTranscriptAudio)
$btnOmiTranscriptBrowse = New-Button 'Browse...' 735 1708 120 31
$tabOmi.Controls.Add($btnOmiTranscriptBrowse)
$btnOmiTranscript = New-Button 'Generate desktop Omi candidate transcript' 25 1749 366 38
$tabOmi.Controls.Add($btnOmiTranscript)
$lblOmiTranscript = New-StatusLabel 408 1760 460
$tabOmi.Controls.Add($lblOmiTranscript)
$tabOmi.Controls.Add((New-Label 'Original Omi v2 ONNX encoder + LSTM + joint + NeMo SentencePiece. No S26 test or cloud compile. First 12 seconds only. Candidate text, NOT NeMo or clinical quality parity.' 25 1795 850 51))
$tabOmi.Controls.Add((New-Label 'STEP 5H - First S26 Omi v2 TDT transcript (three HTP contexts, Windows controller)' 25 1860 850 28 -Bold))
$btnOmiS26Transcript = New-Button 'Transcribe 12s using S26 NPU' 25 1897 331 39
$tabOmi.Controls.Add($btnOmiS26Transcript)
$lblOmiS26Transcript = New-StatusLabel 373 1907 505
$tabOmi.Controls.Add($lblOmiS26Transcript)
$tabOmi.Controls.Add((New-Label 'Uses audio selected above, runs encoder + each predictor/joint step on S26 HTP; compares against desktop ONNX. Contexts staged once, but qnn-net-run reloads them per token step. Experimental and potentially slow; full model NOT qualified.' 25 1945 850 60))
$tabOmi.Controls.Add((New-Label 'STEP 5I - S26 multi-window diagnostic (1-3 independent 12s windows)' 25 2034 850 28 -Bold))
$tabOmi.Controls.Add((New-Label 'Start time in seconds (0 for audio beginning)' 25 2066 450 25))
$txtOmiMultiStart = New-TextBox 25 2093 115 27
$txtOmiMultiStart.Text = '0'
$tabOmi.Controls.Add($txtOmiMultiStart)
$tabOmi.Controls.Add((New-Label 'Number of 12s windows (1-3)' 165 2066 350 25))
$txtOmiMultiCount = New-TextBox 165 2093 115 27
$txtOmiMultiCount.Text = '3'
$tabOmi.Controls.Add($txtOmiMultiCount)
$btnOmiMultiS26 = New-Button 'Compare consecutive S26 windows' 305 2088 318 38
$tabOmi.Controls.Add($btnOmiMultiS26)
$lblOmiMultiS26 = New-StatusLabel 25 2135 840
$tabOmi.Controls.Add($lblOmiMultiS26)
$tabOmi.Controls.Add((New-Label 'Select the actual matching .wav/.ogg file in Step 5G. Binaries stay staged ONCE per multi-window run, but each QNN graph call still starts qnn-net-run. Separate windows reset LSTM context. No seamless long-form transcript or NeMo parity claim.' 25 2170 850 60))

$tabOmi.Controls.Add((New-Label 'STEP 5J - Offline opening check against WhisperApp original Omi GPU text' 25 2265 850 28 -Bold))
$tabOmi.Controls.Add((New-Label 'S26 candidate JSON (auto-filled from successful Step 5I; Browse to override)' 25 2294 795 24))
$txtOmiCompareS26Json = New-TextBox 25 2320 700 27
$tabOmi.Controls.Add($txtOmiCompareS26Json)
$btnOmiCompareS26Browse = New-Button 'Browse...' 735 2319 120 31
$tabOmi.Controls.Add($btnOmiCompareS26Browse)
$tabOmi.Controls.Add((New-Label 'WhisperApp original Omi v2 GPU transcript (.txt)' 25 2352 760 24))
$txtOmiGpuText = New-TextBox 25 2378 700 27
$tabOmi.Controls.Add($txtOmiGpuText)
$btnOmiGpuTextBrowse = New-Button 'Browse...' 735 2377 120 31
$tabOmi.Controls.Add($btnOmiGpuTextBrowse)
$btnOmiGpuReferenceReview = New-Button 'Check GPU-reference opening (offline)' 25 2418 370 35
$tabOmi.Controls.Add($btnOmiGpuReferenceReview)
$lblOmiGpuReview = New-StatusLabel 414 2427 456
$tabOmi.Controls.Add($lblOmiGpuReview)
$tabOmi.Controls.Add((New-Label 'A text-only check cannot prove the source audio is identical or calculate true WER without timestamps and manually verified ground truth. It flags suspiciously DIFFERENT openings; no model qualification.' 25 2455 850 54))

$tabOmi.Controls.Add((New-Label 'STEP 6 - Package and publish to Hugging Face (full-model gate)' 25 2535 750 28 -Bold))
$tabOmi.Controls.Add((New-Label 'Repository (namespace/name)' 25 2572 240 24))
$txtOmiRepo = New-TextBox 25 2601 430 27
$tabOmi.Controls.Add($txtOmiRepo)
$tabOmi.Controls.Add((New-Label 'HF write token (not saved)' 480 2572 290 24))
$txtOmiToken = New-TextBox 480 2601 390 27 $true $false
$tabOmi.Controls.Add($txtOmiToken)
$btnOmiPublish = New-Button 'Publish validated Omi NPU package' 25 2644 290 36
$tabOmi.Controls.Add($btnOmiPublish)
$btnOmiFolder = New-Button 'Open Omi workspace' 330 2644 185 36
$tabOmi.Controls.Add($btnOmiFolder)
$lblOmiPublish = New-StatusLabel 530 2651 345
$tabOmi.Controls.Add($lblOmiPublish)
$lblOmiPublishGuide = New-Label 'Publication is intentionally locked until all Omi neural graphs run on V81, S26 outputs and complete transcripts are validated, and an Android-compatible Omi package exists. No encoder-only package can be passed off as production-ready.' 25 2692 850 64
$tabOmi.Controls.Add($lblOmiPublishGuide)

# Separate opt-in experimental MODEL DISTRIBUTION, not the Step 6 production
# qualification gate. Reuses existing Omi repository and Hugging Face token.
$tabOmi.Controls.Add((New-Label 'STEP 7 - Publish experimental Omi v2 S26 NPU MODEL FILES' 25 2780 845 28 -Bold))
$tabOmi.Controls.Add((New-Label 'Enter username/repository above, or just your username: Step 7 proposes the Omi S26 repo name and asks you to confirm. Existing HF token or login is reused. S26 evidence required; not production-qualified.' 25 2813 855 55))
$chkOmiHfPublic = New-Object System.Windows.Forms.CheckBox
$chkOmiHfPublic.Text = 'PUBLIC repository (unchecked = private; existing repository visibility must match)'
$chkOmiHfPublic.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,2875)
$chkOmiHfPublic.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(830,25)
$chkOmiHfPublic.Checked = $false
$tabOmi.Controls.Add($chkOmiHfPublic)
$chkOmiHfReplace = New-Object System.Windows.Forms.CheckBox
$chkOmiHfReplace.Text = 'Replace ALL existing files in this dedicated Omi S26 Hugging Face repo (extra confirmation)'
$chkOmiHfReplace.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,2908)
$chkOmiHfReplace.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(845,26)
$chkOmiHfReplace.Checked = $false
$tabOmi.Controls.Add($chkOmiHfReplace)
$btnOmiHfExperimental = New-Button 'Deploy Omi S26 model to Hugging Face' 25 2947 365 39
$tabOmi.Controls.Add($btnOmiHfExperimental)
$lblOmiHfExperimental = New-StatusLabel 410 2955 460
$tabOmi.Controls.Add($lblOmiHfExperimental)
$tabOmi.Controls.Add((New-Label 'Uploads ONLY encoder.bin, predictor.bin, joint.bin, token_embedding.npy, original tokenizer.model, manifest, checksums, license and model card. NO patient audio, logs, runtime .so files, canonical .nemo, or Parakeet v3 files. This does not unlock the production publish gate.' 25 2999 855 82))
# Step 8: export authoritative QNN context metadata with official Qualcomm host utility.
# Keep the three binary contexts and the published model repository untouched.
$tabOmi.Controls.Add((New-Label 'STEP 8 - OPTIONAL official QNN context metadata (utility required)' 25 3124 845 28 -Bold))
$tabOmi.Controls.Add((New-Label 'This reads the three EXISTING compiled .bin files with Qualcomm qnn-context-binary-utility.exe. It writes encoder-info.json, predictor-info.json, joint-info.json and QAIRT compile provenance. No recompilation, phone, or HF token.' 25 3157 845 58))
$tabOmi.Controls.Add((New-Label 'Official Windows x64 QAIRT QNN context utility (.exe)' 25 3226 575 24 -Bold))
$txtOmiQnnContextUtility = New-TextBox 25 3256 655
$tabOmi.Controls.Add($txtOmiQnnContextUtility)
$btnOmiQnnUtilityBrowse = New-Button 'Browse...' 697 3253 150 31
$tabOmi.Controls.Add($btnOmiQnnUtilityBrowse)
$btnOmiQnnExportInfo = New-Button 'Export 3 QNN graph info files' 25 3309 335 40
$tabOmi.Controls.Add($btnOmiQnnExportInfo)
$lblOmiQnnExportInfo = New-StatusLabel 377 3316 482
$tabOmi.Controls.Add($lblOmiQnnExportInfo)
$tabOmi.Controls.Add((New-Label 'The official Qualcomm QAIRT host utility must be available locally; it is not part of the AI Hub Python client. Exported JSON uses ACTUAL graph names, shapes and QNN data types read from each context. Never guessed from ONNX. Browse to the SDK bin\x86_64-windows-msvc executable if it is not auto-detected.' 25 3360 845 88))
# Step 9: SDK-free alternative for ORT-QNN compatibility, NOT a Qualcomm binary-metadata extractor.
$tabOmi.Controls.Add((New-Label 'STEP 9 - SDK-FREE S26 ORT-QNN joint.bin loading / inference probe' 25 3510 845 28 -Bold))
$tabOmi.Controls.Add((New-Label 'One-click export of a DEBUG-ONLY Android probe built on WhisperRecorder v6.5.14 source. Select joint.bin on your phone, test ORT-QNN session creation + one inference (CPU fallback disabled), and share JSON. NO QAIRT SDK or recompilation.' 25 3544 850 75))
$btnOmiAndroidOrtProbe = New-Button 'Export Android joint probe (no QAIRT SDK)' 25 3634 405 39
$tabOmi.Controls.Add($btnOmiAndroidOrtProbe)
$lblOmiAndroidOrtProbe = New-StatusLabel 449 3643 415
$tabOmi.Controls.Add($lblOmiAndroidOrtProbe)
$tabOmi.Controls.Add((New-Label 'The exported code requires building and installing a DEBUG Android APK in your Android Studio / GitHub Actions workflow. It is not an already-built APK and it does NOT create official Qualcomm context info.graphs JSON. Use Step 8 only if authentic binary metadata becomes necessary.' 25 3688 850 79))
# Step 10: FP32 desktop ONNX vs already-compiled FP16 S26 QNN, never NeMo GPU.
$tabOmi.Controls.Add((New-Label 'STEP 10 - FP32 ONNX CPU vs S26 NPU original Omi v2 transcript parity' 25 3830 850 28 -Bold))
$tabOmi.Controls.Add((New-Label 'The existing Step 5I runner already compares FP32 ONNX encoder/predictor/joint against all three QNN contexts on identical 12s audio windows. Analyze that evidence now, or run a fresh 1-3 window test. Text parity does NOT waive numerical gates or prove NeMo/clinical parity.' 25 3864 850 76))
$btnOmiParitySaved = New-Button 'Analyze completed Step 5I report (offline)' 25 3953 402 39
$tabOmi.Controls.Add($btnOmiParitySaved)
$btnOmiParityFresh = New-Button 'Run NEW S26 test + compare (slow)' 445 3953 405 39
$tabOmi.Controls.Add($btnOmiParityFresh)
$lblOmiParity = New-StatusLabel 25 4006 848
$tabOmi.Controls.Add($lblOmiParity)
$tabOmi.Controls.Add((New-Label 'Saved: uses Step 5J JSON above, automatically populated by Step 5I, and works with the phone disconnected. New run: uses Step 5G audio, Step 5I start/count and the selected S26; calls qnn-net-run many times. Results saved as JSON + TXT in a new workspace folder.' 25 4045 850 76))
$btnOmiParityOpenResults = New-Button 'Open latest Step 10 results folder' 25 4130 318 39
$tabOmi.Controls.Add($btnOmiParityOpenResults)
$lblOmiParityResults = New-StatusLabel 359 4138 496
$tabOmi.Controls.Add($lblOmiParityResults)
$btnOmiClearDeviceTests = New-Button 'Clear previous S26 test files' 25 4182 318 39
$tabOmi.Controls.Add($btnOmiClearDeviceTests)
$lblOmiClearDeviceTests = New-StatusLabel 359 4190 496
$tabOmi.Controls.Add($lblOmiClearDeviceTests)
$tabOmi.Controls.Add((New-Label 'Removes ONLY Builder-owned staging folders (including abandoned 1.2 GB Omi contexts); does not touch recordings, apps, downloads, or on-PC results.' 25 4234 850 55))
$tabOmi.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,4300)


function Ensure-OmiNativeEnvironmentFromUi {
    # Reuse the same proven native Windows PyTorch/Transformers environment as
    # Parakeet v3. Do not install or invoke WSL or NVIDIA NeMo.
    $python = Ensure-ParakeetExportEnvironmentFromUi
    $test = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import yaml, torch, onnx, onnxruntime; from transformers.models.parakeet.modeling_parakeet import ParakeetEncoder; print("Omi Windows deps ready")') -TimeoutSeconds 120
    if ($test.ExitCode -ne 0) {
        Append-Log '[Omi] Installing PyYAML in the existing project-local Windows export environment...'
        $pip = Invoke-ProcessCapture -FilePath $python -Arguments @('-m','pip','install','--upgrade','PyYAML') -TimeoutSeconds 1800
        if ($pip.ExitCode -ne 0) { throw ('Windows-native Omi dependency install failed: ' + $pip.StdErr + $pip.StdOut) }
        $test = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import yaml, torch, onnx, onnxruntime; from transformers.models.parakeet.modeling_parakeet import ParakeetEncoder; print("Omi Windows deps ready")') -TimeoutSeconds 120
        if ($test.ExitCode -ne 0) { throw ('Omi native environment import test failed: ' + $test.StdErr) }
    }
    return $python
}

function Ensure-OmiTdtTranscriptEnvironmentFromUi {
    $python = Ensure-OmiNativeEnvironmentFromUi
    $probe = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import sentencepiece; print("Omi SentencePiece ready")') -TimeoutSeconds 120
    if ($probe.ExitCode -ne 0) {
        Append-Log '[Omi] Installing original NeMo SentencePiece tokenizer support in the existing Windows Python environment...'
        $installed = Invoke-ProcessCapture -FilePath $python -Arguments @('-m','pip','install','sentencepiece') -TimeoutSeconds 1800
        if ($installed.ExitCode -ne 0) { throw ('SentencePiece dependency install failed: ' + $installed.StdErr + $installed.StdOut) }
        $probe = Invoke-ProcessCapture -FilePath $python -Arguments @('-c','import sentencepiece') -TimeoutSeconds 120
        if ($probe.ExitCode -ne 0) { throw ('SentencePiece import failed after install: ' + $probe.StdErr) }
    }
    return $python
}

function Get-OmiLatestTdtDesktopFolder {
    # Do not rely on the transient current-session workspace. Step 5D has a
    # durable receipt and artifacts in the original Omi run folder.
    $roots = @()
    if ($script:OmiRunFolder -and (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) { $roots += [string]$script:OmiRunFolder }
    if ((Test-Path -LiteralPath $script:OmiWorkspace -PathType Container) -and ($roots -notcontains [string]$script:OmiWorkspace)) { $roots += [string]$script:OmiWorkspace }
    $candidates = @()
    foreach ($base in $roots) {
        $candidates += @(Get-ChildItem -LiteralPath $base -Directory -Filter 'omi-tdt-desktop-*' -Recurse -ErrorAction SilentlyContinue)
    }
    foreach ($candidate in @($candidates | Sort-Object LastWriteTime -Descending)) {
        $receipt = Join-Path $candidate.FullName 'omi-tdt-desktop-validation.json'
        if (-not (Test-Path -LiteralPath $receipt -PathType Leaf)) { continue }
        try {
            $r = Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
            if ([string]$r.status -ne 'qualified_desktop_omi_v2_tdt_neural_graphs_only' -or [bool]$r.checks.publishable) { continue }
            $valid = $true
            foreach ($item in @(@('omi-tdt-predictor-v2.onnx',[string]$r.graphs.predictor.sha256),@('omi-tdt-joint-v2.onnx',[string]$r.graphs.joint.sha256),@('omi-tdt-token-embedding-v2.npy',[string]$r.token_embedding.sha256))) {
                $path = Join-Path $candidate.FullName ([string]$item[0])
                if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { $valid = $false; break }
                if ((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$item[1]) { $valid = $false; break }
            }
            if ($valid) { return $candidate.FullName }
        } catch { continue }
    }
    throw 'No hash-verified Omi v2 desktop predictor + joint found. Complete Step 5D in this workspace first.'
}

function Test-OmiStep5JSourceReport {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    try {
        $proof = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
        if ([string]$proof.status -eq 'omi_v2_s26_multiwindow_candidate_not_long_form_qualified') {
            return ([bool]$proof.on_device_execution -and [bool]$proof.all_windows_completed -and
                -not [bool]$proof.publishable -and -not [bool]$proof.full_transcript_pass -and
                -not [bool]$proof.native_android_persistent_qnn -and @($proof.windows).Count -ge 1)
        }
        if ([string]$proof.status -eq 'candidate_omi_v2_12s_s26_htp_transcript_not_model_qualified') {
            return ([bool]$proof.on_device_execution -and -not [bool]$proof.checks.publishable -and
                -not [bool]$proof.checks.full_transcript_pass -and
                -not [bool]$proof.checks.canonical_nemo_transcript_parity_tested)
        }
    } catch { return $false }
    return $false
}

function Get-OmiLatestCompletedStep5IReport {
    # Search only this Omi run's direct child folders: avoid scanning huge model files
    # and avoid selecting another run's report or an incomplete multi-window session.
    if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder) -or
        -not (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) { return '' }
    $folders = @(Get-ChildItem -LiteralPath $script:OmiRunFolder -Directory -Filter 'omi-tdt-multiwindow-*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 40)
    foreach ($folder in $folders) {
        $candidate = Join-Path $folder.FullName 'omi-tdt-s26-multiwindow-diagnostic.json'
        if (Test-OmiStep5JSourceReport -Path $candidate) { return $candidate }
    }
    return ''
}

function Restore-OmiStep5JSource {
    # Prefer the newest fully completed Step 5I output on launch; preserve an
    # existing manually selected valid Step 5H report when no Step 5I exists.
    $candidate = Get-OmiLatestCompletedStep5IReport
    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        if ([string]$txtOmiCompareS26Json.Text -ne $candidate) {
            $txtOmiCompareS26Json.Text = $candidate
            Append-Log ('Step 5J automatically selected saved Step 5I JSON: ' + $candidate)
        }
        return
    }
    if (-not (Test-OmiStep5JSourceReport -Path ([string]$txtOmiCompareS26Json.Text))) {
        $txtOmiCompareS26Json.Clear()
    }
}

function Set-OmiStep5JSourceFromCompletedRun {
    param([string]$Path)
    if (-not (Test-OmiStep5JSourceReport -Path $Path)) { throw 'Step 5J cannot select an incomplete or unverified S26 report.' }
    $txtOmiCompareS26Json.Text = $Path
    Save-Settings
    Append-Log ('Step 5J JSON field automatically filled: ' + $Path)
}

function Update-OmiState {
    $checkpoint = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
    $txtOmiNemo.Text = $checkpoint
    if (Test-Path -LiteralPath $checkpoint -PathType Leaf) { Set-Status $lblOmiDownload 'Checkpoint found; verify before export' 'warn' }
    else { Set-Status $lblOmiDownload 'Not downloaded' 'warn' }
    $omiNativePy = Join-Path $Root 'tools\parakeet-export-env\Scripts\python.exe'
    if (Test-Path -LiteralPath $omiNativePy -PathType Leaf) { Set-Status $lblOmiSetup 'Native Python available (click to verify)' 'warn' }
    else { Set-Status $lblOmiSetup 'Install Windows Python dependencies' 'warn' }
    $btnOmiCompile.Enabled = $false
    $btnOmiHtpTest.Enabled = $false
    $btnOmiAnalyze.Enabled = $false
    $btnOmiSecond.Enabled = $false
    $btnOmiDecoderProbe.Enabled = $false
    $btnOmiTdtExport.Enabled = $false
    $btnOmiTdtCompile.Enabled = $false
    $btnOmiTdtDevice.Enabled = $false
    $btnOmiTdtRecover.Enabled = $false
    $btnOmiTranscript.Enabled = $false
    $btnOmiS26Transcript.Enabled = $false
    $btnOmiMultiS26.Enabled = $false
    Set-Status $lblOmiMultiS26 'Compiled encoder and decoder required; select matching audio above' 'warn'
    Set-Status $lblOmiS26Transcript 'Verified encoder and compiled decoder required' 'warn'
    Set-Status $lblOmiTranscript 'Validated encoder and desktop v2 decoder required' 'warn'
    Set-Status $lblOmiTdtCompile 'Validated desktop decoder required' 'warn'
    Set-Status $lblOmiTdtDevice 'Compiled decoder contexts required' 'warn'
    Set-Status $lblOmiTdtExport 'Original Omi source extraction required' 'warn'
    Set-Status $lblOmiSecond 'Compile encoder to enable a second-sample test' 'warn'
    Set-Status $lblOmiDecoderProbe 'Extract original Omi weights first' 'warn'
    Set-Status $lblOmiHtpTest 'Compile Omi encoder before S26 test' 'warn'
    Set-Status $lblOmiAnalyze 'Saved S26 output required' 'warn'
    if (Test-Path -LiteralPath (Join-Path $script:OmiModelDir 'native-extracted\model_weights.ckpt') -PathType Leaf) {
        $btnOmiDecoderProbe.Enabled = $true
        $btnOmiTdtExport.Enabled = $true
        Set-Status $lblOmiDecoderProbe 'Omi checkpoint available for decoder inspection' 'good'
        Set-Status $lblOmiTdtExport 'Ready: original v2 decoder weights available' 'good'
    }
    if ($script:OmiRunFolder -and (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) {
        $qualificationPath = Join-Path $script:OmiRunFolder 'omi-encoder-qualification.json'
        $qualOk = $false
        if (Test-Path -LiteralPath $qualificationPath -PathType Leaf) {
            try {
                $qual = Get-Content -LiteralPath $qualificationPath -Raw | ConvertFrom-Json
                $qualOk = ([string]$qual.status -eq 'qualified_desktop_encoder_only' -and [bool]$qual.adapter_preservation_confirmed)
            } catch { $qualOk = $false }
        }
        if ($qualOk) {
            Set-Status $lblOmiExport 'Short-input ONNX parity PASS (NeMo parity pending)' 'good'
            $btnOmiCompile.Enabled = $true
        } else { Set-Status $lblOmiExport 'Export not qualified - see Logs' 'warn' }
        $omiCompileFile = Join-Path $script:OmiRunFolder 'omi-encoder-cloud-compile.json'
        if (Test-Path -LiteralPath $omiCompileFile -PathType Leaf) {
            try {
                $omiCloud = Get-Content -LiteralPath $omiCompileFile -Raw | ConvertFrom-Json
                if ([string]$omiCloud.status -eq 'encoder_context_compiled_unvalidated' -and (Test-Path -LiteralPath ([string]$omiCloud.context) -PathType Leaf)) {
                    $btnOmiHtpTest.Enabled = $true
                    $btnOmiSecond.Enabled = $true
                    Set-Status $lblOmiSecond 'Ready: select another nonsensitive recording' 'good'
                    $savedOmiTestsDir = Join-Path $script:OmiRunFolder 'device'
                    if (Test-Path -LiteralPath $savedOmiTestsDir -PathType Container) {
                        $savedOmiComparisons = @(Get-ChildItem -LiteralPath $savedOmiTestsDir -Filter 'comparison.json' -File -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Directory.Name -like 'omi-encoder-htp-*' })
                        if ($savedOmiComparisons.Count -gt 0) {
                            $btnOmiAnalyze.Enabled = $true
                            Set-Status $lblOmiAnalyze 'Saved HTP tensors found; offline analysis available' 'good'
                        }
                    }
                    Set-Status $lblOmiCompile 'Encoder compiled; reusable for HTP test' 'good'
                    $omiProof = Join-Path $script:OmiRunFolder 'omi-encoder-htp-result.json'
                    if (Test-Path -LiteralPath $omiProof -PathType Leaf) {
                        Set-Status $lblOmiHtpTest 'Previous HTP report present; click to reverify' 'warn'
                    } else {
                        Set-Status $lblOmiHtpTest 'S26 execution pending' 'warn'
                    }
                } else { Set-Status $lblOmiCompile 'Compilation record invalid or .bin missing' 'bad' }
            } catch { Set-Status $lblOmiCompile 'Compilation record unreadable' 'bad' }
        } else { Set-Status $lblOmiCompile 'Not compiled' 'warn' }
    } else {
        Set-Status $lblOmiExport 'Not exported or validated' 'warn'
        Set-Status $lblOmiCompile 'Awaiting desktop encoder qualification' 'warn'
    }
    # Restore the status of an existing v2 desktop export without changing any
    # device/production qualification flag. Validate saved graph file hashes.
    if ($script:OmiRunFolder -and (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) {
        $tdtRuns = @(Get-ChildItem -LiteralPath $script:OmiRunFolder -Directory -Filter 'omi-tdt-desktop-*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending)
        foreach ($tdtRun in $tdtRuns) {
            $tdtReceipt = Join-Path $tdtRun.FullName 'omi-tdt-desktop-validation.json'
            if (-not (Test-Path -LiteralPath $tdtReceipt -PathType Leaf)) { continue }
            try {
                $v2 = Get-Content -LiteralPath $tdtReceipt -Raw | ConvertFrom-Json
                if ([string]$v2.status -ne 'qualified_desktop_omi_v2_tdt_neural_graphs_only' -or [bool]$v2.checks.publishable) { continue }
                $predFile = Join-Path $tdtRun.FullName 'omi-tdt-predictor-v2.onnx'
                $jointFile = Join-Path $tdtRun.FullName 'omi-tdt-joint-v2.onnx'
                $embedFile = Join-Path $tdtRun.FullName 'omi-tdt-token-embedding-v2.npy'
                if (-not ((Test-Path -LiteralPath $predFile -PathType Leaf) -and (Test-Path -LiteralPath $jointFile -PathType Leaf) -and (Test-Path -LiteralPath $embedFile -PathType Leaf))) { continue }
                if ((Get-FileHash -LiteralPath $predFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$v2.graphs.predictor.sha256) { continue }
                if ((Get-FileHash -LiteralPath $jointFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$v2.graphs.joint.sha256) { continue }
                if ((Get-FileHash -LiteralPath $embedFile -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$v2.token_embedding.sha256) { continue }
                Set-Status $lblOmiTdtExport 'Desktop decoder validated; S26 compilation pending' 'good'
                break
            } catch { continue }
        }
    }
    try {
        $foundTdt = Get-OmiLatestTdtDesktopFolder
        $btnOmiTdtCompile.Enabled = $true
        if ($script:OmiRunFolder -and (Test-Path -LiteralPath (Join-Path $script:OmiRunFolder 'omi-encoder-qualification.json') -PathType Leaf)) {
            $btnOmiTranscript.Enabled = $true
            Set-Status $lblOmiTranscript 'Ready: original v2 desktop candidate (12s only)' 'good'
        }
        Set-Status $lblOmiTdtCompile 'Desktop graph hashes verified; compile or reuse' 'good'
        $cloudReceipt = Join-Path $foundTdt 'omi-tdt-cloud-compile.json'
        if (Test-Path -LiteralPath $cloudReceipt -PathType Leaf) {
            $compiled = Get-Content -LiteralPath $cloudReceipt -Raw | ConvertFrom-Json
            if ([string]$compiled.status -eq 'omi_v2_tdt_contexts_compiled_unvalidated') {
                $btnOmiTdtDevice.Enabled = $true
                $btnOmiTdtRecover.Enabled = $true
                if ($script:OmiRunFolder -and (Test-Path -LiteralPath (Join-Path $script:OmiRunFolder 'omi-encoder-cloud-compile.json') -PathType Leaf)) {
                    $btnOmiS26Transcript.Enabled = $true
                    $btnOmiMultiS26.Enabled = $true
                    Set-Status $lblOmiMultiS26 'Ready: 1-3 windows; shared S26 context staging' 'good'
                    Set-Status $lblOmiS26Transcript 'Three reusable contexts detected; verify and run S26' 'good'
                }
                Set-Status $lblOmiTdtDevice 'Context receipt found; full checksum check on click' 'warn'
                Set-Status $lblOmiTdtCompile 'QNN contexts present; checksum verification on click' 'good'
            }
        }
    } catch { }
    Set-Status $lblOmiPublish 'LOCKED: complete decoder and device proof required' 'bad'
}

$btnOmiSetup.Add_Click({
    Invoke-UiAction 'Preparing Windows-native Omi PyTorch + Transformers environment (no WSL)...' {
        $python = Ensure-OmiNativeEnvironmentFromUi
        Set-Status $lblOmiSetup 'Windows PyTorch/Transformers + PyYAML ready' 'good'
        Append-Log ('Omi export uses native Windows Python: ' + $python)
    }
})

$btnOmiSourcePage.Add_Click({ try { Open-Url 'https://huggingface.co/omi-health/omi-med-stt-v1' } catch { Show-Error $_ } })
$btnOmiDownload.Add_Click({
    Invoke-UiAction 'Downloading verified Omi canonical NeMo checkpoint...' {
        $python = Ensure-HuggingFaceUploaderPython
        New-Item -ItemType Directory -Path $script:OmiWorkspace -Force | Out-Null
        $report = Join-Path $script:OmiWorkspace 'omi-download.json'
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:OmiToolsPath,'download','--output-dir',$script:OmiModelDir,'--report',$report) -TimeoutSeconds 21600
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $data = Get-Content -LiteralPath $report -Raw | ConvertFrom-Json
        if ([string]$data.status -ne 'verified_original_nemo_checkpoint') { throw 'Checkpoint did not pass published SHA-256 validation.' }
        $txtSourceModel.Text = [string]$data.path
        Set-Status $lblOmiDownload 'Original Omi checkpoint VERIFIED' 'good'
        Append-Log ('Verified Omi source: ' + [string]$data.path)
        Save-Settings
    }
})
$btnOmiAudio.Add_Click({ Choose-File -TextBox $txtOmiAudio -Title 'Select non-sensitive reference speech for Omi encoder validation' -Filter 'Audio files (*.wav;*.ogg)|*.wav;*.ogg|All files (*.*)|*.*' })
$btnOmiExport.Add_Click({
    Invoke-UiAction 'Converting original Omi on native Windows and validating short-input medical encoder...' {
        $checkpoint = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        if (-not (Test-Path -LiteralPath $checkpoint -PathType Leaf)) { throw 'First download the original Omi model with Step 2.' }
        if ([string]::IsNullOrWhiteSpace($txtOmiAudio.Text) -or -not (Test-Path -LiteralPath $txtOmiAudio.Text -PathType Leaf)) { throw 'Select a non-sensitive speech .wav or .ogg file first.' }
        $python = Ensure-OmiNativeEnvironmentFromUi
        if (-not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
            $ff = Install-FfmpegFromUi
            if (-not (Test-Path -LiteralPath $ff -PathType Leaf)) { throw 'FFmpeg was not installed.' }
        }
        New-Item -ItemType Directory -Path $script:OmiWorkspace -Force | Out-Null
        $run = Join-Path $script:OmiWorkspace ('run-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $cache = Join-Path $script:OmiModelDir 'native-extracted'
        $args = @($script:OmiNativePath,'export-validate','--nemo',$checkpoint,'--audio',$txtOmiAudio.Text,'--output-dir',$run,'--cache-dir',$cache,'--ffmpeg',$txtFfmpeg.Text,'--seconds','12')
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 21600
        if ($r.ExitCode -ne 0) { throw ('Omi Windows exporter failed. ' + $r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $gatePy = Get-LocalBuilderPython
        $gate = Invoke-ProcessCapture -FilePath $gatePy -Arguments @($script:OmiGatePath,'check-desktop','--folder',$run) -TimeoutSeconds 300
        if ($gate.ExitCode -ne 0) { throw ('Omi ONNX parity/adapter gate failed: ' + $gate.StdOut + $gate.StdErr) }
        $script:OmiRunFolder = $run
        Set-Status $lblOmiExport 'PASS: native HF encoder vs ONNX (short audio)' 'good'
        Append-Log ('Omi short-encoder candidate qualified: ' + $run)
        Append-Log 'NOT YET proven equivalent to original NeMo. Long-audio local attention and S26 HTP proof are pending.'
        Save-Settings
        Update-OmiState
    }
})
$btnOmiCompile.Add_Click({
    Invoke-UiAction 'Qualcomm AI Hub experimental Omi encoder compilation...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'First run Omi ONNX export and medical adapter validation.' }
        $python = Get-LocalBuilderPython
        $gate = Invoke-ProcessCapture -FilePath $python -Arguments @($script:OmiGatePath,'check-desktop','--folder',$script:OmiRunFolder) -TimeoutSeconds 300
        if ($gate.ExitCode -ne 0) { throw ('Cannot compile an unqualified Omi ONNX graph: ' + $gate.StdOut) }
        $qualified = $gate.StdOut | ConvertFrom-Json
        $hubPython = Ensure-AiHubEnvironmentFromUi
        $out = Join-Path $script:OmiRunFolder 'compiled\omi-encoder-sm8850-v81-fp16.bin'
        $r = Invoke-AiHubCloudCompile -PythonPath $hubPython -BridgePath $script:BridgePath -OnnxPath ([string]$qualified.graph) -OutputPath $out -Precision 'float16' -QairtVersion 'default'
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $record = Invoke-ProcessCapture -FilePath $python -Arguments @($script:OmiGatePath,'record-compile','--folder',$script:OmiRunFolder,'--context',$out) -TimeoutSeconds 600
        if ($record.ExitCode -ne 0) { throw ('Compiled artifact was not verified: ' + $record.StdOut) }
        Set-Status $lblOmiCompile 'Encoder compiled; S26 HTP test ready' 'good'
        Update-OmiState
        Append-Log ('Experimental Omi QNN context: ' + $out)
        Append-Log 'NOT a complete NPU model; do not install/publish until Omi decoder and HTP validation are implemented.'
    }
})
# Analyze the existing raw device output, entirely on Windows. The results can
# guide a later encoder fix but never change the validation or publish gates.
function Invoke-OmiSavedNumericalAnalysis {
    if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Open an Omi model workspace first.' }
    $deviceRoot = Join-Path $script:OmiRunFolder 'device'
    if (-not (Test-Path -LiteralPath $deviceRoot -PathType Container)) { throw 'No saved Omi device test. Complete Step 5 first.' }
    $comparisons = @(Get-ChildItem -LiteralPath $deviceRoot -Filter 'comparison.json' -File -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Directory.Name -like 'omi-encoder-htp-*' } | Sort-Object LastWriteTime -Descending)
    if ($comparisons.Count -eq 0) { throw 'No saved Omi HTP comparison.json. Complete Step 5 first.' }
    $testFolder = Split-Path -Parent $comparisons[0].FullName
    $ref = Join-Path $script:OmiRunFolder 'omi-htp-reference\omi-encoder.reference.json'
    if (-not (Test-Path -LiteralPath $ref -PathType Leaf)) { throw 'Pinned Omi encoder ONNX reference is missing.' }
    $py = Join-Path $Root 'tools\parakeet-export-env\Scripts\python.exe'
    if (-not (Test-Path -LiteralPath $py -PathType Leaf)) { throw 'Omi Windows Python not installed. Run Step 1 first.' }
    $args = @((Join-Path $Root 'python\omi_htp_diagnostics.py'),'analyze','--device-folder',$testFolder,'--reference',$ref)
    $call = Invoke-ProcessCapture -FilePath $py -Arguments $args -TimeoutSeconds 180
    if ($call.ExitCode -ne 0) { throw ('Omi saved-output diagnostics failed: ' + $call.StdErr + $call.StdOut) }
    $parsed = $call.StdOut | ConvertFrom-Json
    if ([string]$parsed.status -ne 'non_qualifying_numerical_diagnostics') { throw 'Unexpected Omi diagnostics report status.' }
    return $parsed
}

$btnOmiAnalyze.Add_Click({
    Invoke-UiAction 'Analyzing saved Omi encoder NPU output offline...' {
        $analysis = Invoke-OmiSavedNumericalAnalysis
        Set-Status $lblOmiAnalyze 'Diagnostic report saved on Windows (not a PASS)' 'warn'
        Append-Log ('Omi offline diagnostics: ' + [string]$analysis.report)
        $metrics = $analysis.valid_comparison
        $rounding = $analysis.reference_fp16_rounding_only
        [System.Windows.Forms.MessageBox]::Show(
            ('Saved S26 output analyzed without contacting the phone.' + "`r`n`r`n" +
             'HTP mean absolute error: ' + $metrics.mean_abs + "`r`n" +
             'HTP RMSE: ' + $metrics.rmse + "`r`n" +
             'FP16 reference rounding-only mean error: ' + $rounding.mean_abs + "`r`n`r`n" +
             'Detailed frame/channel/shift diagnostics saved to:' + "`r`n" + [string]$analysis.report + "`r`n`r`n" +
             'This analysis does not change the existing HTP FAIL or unlock publishing.'),
            'Omi offline numerical diagnostics',
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnOmiSecondBrowse.Add_Click({ Choose-File -TextBox $txtOmiSecondAudio -Title 'Select a DIFFERENT non-sensitive Omi test recording' -Filter 'Audio files (*.wav;*.ogg)|*.wav;*.ogg|All files (*.*)|*.*' })
$btnOmiDecoderProbe.Add_Click({
    Invoke-UiAction 'Inspecting original Omi v2 TDT decoder and joint tensor contracts on Windows...' {
        $nativePy = Ensure-OmiNativeEnvironmentFromUi
        $weightsDir = Join-Path $script:OmiModelDir 'native-extracted'
        if (-not (Test-Path -LiteralPath (Join-Path $weightsDir 'model_weights.ckpt') -PathType Leaf)) { throw 'Export Omi encoder in Step 3 first to extract canonical weights.' }
        $reportFolder = if ($script:OmiRunFolder) { $script:OmiRunFolder } else { $script:OmiWorkspace }
        New-Item -ItemType Directory -Path $reportFolder -Force | Out-Null
        $dest = Join-Path $reportFolder 'omi-tdt-decoder-contract-probe.json'
        $args = @((Join-Path $Root 'python\omi_tdt_contract.py'),'probe','--cache-dir',$weightsDir,'--output',$dest)
        $r = Invoke-ProcessCapture -FilePath $nativePy -Arguments $args -TimeoutSeconds 1800
        if ($r.ExitCode -ne 0) { throw ('Omi TDT inspection failed: ' + $r.StdErr + $r.StdOut) }
        $data = $r.StdOut | ConvertFrom-Json
        if ([string]$data.status -ne 'tdt_contract_metadata_only_not_exported') { throw 'Unexpected decoder inspection status.' }
        Set-Status $lblOmiDecoderProbe 'Decoder metadata saved; export still pending' 'warn'
        Append-Log ('Omi decoder metadata: ' + [string]$data.report)
        [System.Windows.Forms.MessageBox]::Show('Original Omi decoder/joint shapes and config saved. This is metadata only; decoder ONNX and S26 compilation are not yet implemented.','Omi decoder probe','OK','Information') | Out-Null
    }
})
$btnOmiTdtExport.Add_Click({
    Invoke-UiAction 'Exporting original Omi v2 predictor + joint ONNX and validating two-step recurrent parity...' {
        $nativePy = Ensure-OmiNativeEnvironmentFromUi
        $weightsDir = Join-Path $script:OmiModelDir 'native-extracted'
        if (-not (Test-Path -LiteralPath (Join-Path $weightsDir 'model_weights.ckpt') -PathType Leaf)) { throw 'Omi checkpoint must be extracted by Step 3 or Step 5C first.' }
        $base = if ($script:OmiRunFolder -and (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) { [string]$script:OmiRunFolder } else { [string]$script:OmiWorkspace }
        $folder = Join-Path $base ('omi-tdt-desktop-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $args = @($script:OmiTdtExportPath,'export-validate','--cache-dir',$weightsDir,'--output-dir',$folder)
        $r = Invoke-ProcessCapture -FilePath $nativePy -Arguments $args -TimeoutSeconds 7200
        if ($r.ExitCode -ne 0) { throw ('Omi v2 decoder desktop export failed: ' + $r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $receipt = Join-Path $folder 'omi-tdt-desktop-validation.json'
        if (-not (Test-Path -LiteralPath $receipt -PathType Leaf)) { throw 'Omi v2 export did not produce a validated ONNX receipt.' }
        $proof = Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ([string]$proof.status -ne 'qualified_desktop_omi_v2_tdt_neural_graphs_only' -or [bool]$proof.checks.publishable) { throw 'Unexpected Omi v2 TDT qualification status; refusing to accept export.' }
        foreach ($name in @('omi-tdt-predictor-v2.onnx','omi-tdt-joint-v2.onnx','omi-tdt-token-embedding-v2.npy')) {
            if (-not (Test-Path -LiteralPath (Join-Path $folder $name) -PathType Leaf)) { throw ('Omi decoder artifact missing: ' + $name) }
        }
        Set-Status $lblOmiTdtExport 'Desktop predictor + joint ONNX PASS; HTP pending' 'good'
        $btnOmiTdtCompile.Enabled = $true
        Set-Status $lblOmiTdtCompile 'Ready to compile original Omi v2 decoder on AI Hub' 'good'
        Append-Log ('Original Omi v2 predictor+joint desktop validation receipt: ' + $receipt)
        Append-Log 'Original NeMo decision parity, HTP execution, controller, tokenizer and full transcripts are NOT validated. HF gate remains locked.'
        [System.Windows.Forms.MessageBox]::Show(('Original Omi v2 neural decoder graphs qualified against desktop PyTorch and ONNX Runtime.' + "`r`n`r`n" + 'Predictor: omi-tdt-predictor-v2.onnx' + "`r`n" + 'Joint: omi-tdt-joint-v2.onnx' + "`r`n" + 'Embedding: omi-tdt-token-embedding-v2.npy' + "`r`n`r`n" + 'Results: ' + $receipt + "`r`n`r`n" + 'Not yet compiled or tested on S26; not Android-ready.'),'Omi v2 TDT desktop export',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})
$btnOmiTdtCompile.Add_Click({
    Invoke-UiAction 'Compiling original Omi v2 TDT predictor and joint contexts on Qualcomm AI Hub...' {
        $folder = Get-OmiLatestTdtDesktopFolder
        $py = Ensure-OmiNativeEnvironmentFromUi
        $prep = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiTdtHtpPath,'prepare','--folder',$folder) -TimeoutSeconds 1200
        if ($prep.ExitCode -ne 0) { throw ('Omi v2 decoder reference preparation failed: ' + $prep.StdErr + $prep.StdOut) }
        $refData = $prep.StdOut | ConvertFrom-Json
        if ([string]$refData.status -ne 'omi_v2_tdt_htp_references_prepared_uncompiled') { throw 'Unexpected Omi decoder reference preparation result.' }
        $compileReceipt = Join-Path $folder 'omi-tdt-cloud-compile.json'
        if (Test-Path -LiteralPath $compileReceipt -PathType Leaf) {
            $existing = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiTdtHtpPath,'check-compile','--folder',$folder) -TimeoutSeconds 600
            if ($existing.ExitCode -ne 0) { throw ('Compiled Omi decoder artifacts are present but failed checksum validation. They will NOT be overwritten: ' + $existing.StdErr) }
            $btnOmiTdtDevice.Enabled = $true
            $btnOmiTdtRecover.Enabled = $true
            Set-Status $lblOmiTdtCompile 'Two QNN contexts reused; no cloud compilation' 'good'
            Set-Status $lblOmiTdtDevice 'Ready for device verification' 'good'
            Append-Log ('Reusing both compiled Omi v2 QNN contexts from ' + $folder)
            return
        }
        $hubPy = Ensure-AiHubEnvironmentFromUi
        foreach ($role in @('predictor','joint')) {
            $stem = 'omi-tdt-' + $role + '-v2'
            $onnx = Join-Path $folder ($stem + '.onnx')
            $reference = Join-Path (Join-Path $folder 'omi-tdt-htp-reference') ($stem + '.reference.json')
            $out = Join-Path (Join-Path $folder 'compiled') ($stem + '-sm8850-v81-fp16.bin')
            if (Test-Path -LiteralPath $out -PathType Leaf) {
                $meta = $out + '.metadata.json'
                if (-not (Test-Path -LiteralPath $meta -PathType Leaf)) { throw ('Existing ' + $role + ' .bin has no provenance sidecar. Refusing overwrite; inspect ' + $out) }
                $existingMeta = Get-Content -LiteralPath $meta -Raw | ConvertFrom-Json
                if ([string]$existingMeta.sourceOnnx -ne $onnx -or [string]$existingMeta.referencePath -ne $reference) { throw ('Existing ' + $role + ' context belongs to a different source/reference.') }
                Append-Log ('Keeping previously compiled Omi v2 ' + $role + ' context: ' + $out)
            } else {
                Append-Log ('Compiling original Omi v2 ' + $role + ' on Qualcomm AI Hub (FP16): ' + $onnx)
                $r = Invoke-AiHubCloudCompile -PythonPath $hubPy -BridgePath $script:BridgePath -OnnxPath $onnx -OutputPath $out -Precision 'float16' -QairtVersion 'default'
                if ($r.ExitCode -ne 0) { throw ('Omi v2 ' + $role + ' Qualcomm compilation failed: ' + $r.StdErr + $r.StdOut) }
                [void](Write-QnnCompiledArtifactMetadata -ContextPath $out -SourceOnnx $onnx -ReferencePath $reference -Target 'Samsung Galaxy S26 (Family)' -Precision 'float16' -QairtVersion 'default')
            }
        }
        $record = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiTdtHtpPath,'record-compile','--folder',$folder) -TimeoutSeconds 600
        if ($record.ExitCode -ne 0) { throw ('Decoder cloud contexts not verified: ' + $record.StdErr + $record.StdOut) }
        $btnOmiTdtDevice.Enabled = $true
        $btnOmiTdtRecover.Enabled = $true
        Set-Status $lblOmiTdtCompile 'Both original v2 decoder contexts compiled' 'good'
        Set-Status $lblOmiTdtDevice 'Ready: S26 HTP test pending' 'good'
        Append-Log ('Omi v2 decoder QNN compile receipt: ' + $compileReceipt)
        Append-Log 'NeMo parity, canonical duration behavior and full transcription remain unverified. No HF publication.'
    }
})

# Step 5F is usable directly after reopening Builder, even if the global
# Deploy & Test device-refresh action was not clicked in this session.
# Never pass an empty or disconnected serial into Get-DeviceInfo. With more
# than one authorized phone, do not silently choose which receives the model.
function Resolve-OmiTdtS26Device {
    param([Parameter(Mandatory=$true)][string]$AdbPath)
    if ([string]::IsNullOrWhiteSpace($AdbPath) -or -not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) {
        throw 'ADB executable not found. Open Setup or Deploy & Test and select adb.exe.'
    }
    $authorized = @(Get-AdbDevices -AdbPath $AdbPath | Where-Object {
        $_.State -eq 'device' -and -not [string]::IsNullOrWhiteSpace([string]$_.Serial)
    })
    if ($authorized.Count -eq 0) {
        $script:DeviceSerial = ''
        throw 'No authorized Galaxy S26 connected. Connect USB, enable USB debugging, and accept the authorization prompt.'
    }
    if ($authorized.Count -gt 1) {
        throw 'Multiple authorized ADB devices detected. Disconnect other devices before testing Omi on the S26.'
    }
    $connectedSerial = [string]$authorized[0].Serial
    if ($connectedSerial -ne [string]$script:DeviceSerial) {
        Append-Log ('Step 5F auto-detected connected ADB device: ' + $connectedSerial)
        Refresh-Device
    }
    if ([string]::IsNullOrWhiteSpace([string]$script:DeviceSerial)) {
        throw 'No verified Galaxy S26 serial after ADB refresh. Reconnect the phone and retry Step 5F.'
    }
    # Verify the freshly enumerated serial instead of trusting restored GUI state.
    if ([string]$script:DeviceSerial -ne $connectedSerial) {
        throw 'ADB device changed during refresh. Reconnect the Galaxy S26 and retry Step 5F.'
    }
    $device = Get-DeviceInfo -AdbPath $AdbPath -Serial $connectedSerial
    if (-not $device.IsTargetDevice) {
        throw ('Refusing Omi decoder test: connected device is ' + [string]$device.Model + ' (expected Samsung SM-S942U).')
    }
    return $device
}


# Dedicated Omi manual recovery after an aborted process or lost ADB session.
# Does not require original QNN binaries, NeMo, any SDK or Python.
$btnOmiClearDeviceTests.Add_Click({
    Invoke-UiAction 'Clearing Builder-owned temporary files from Galaxy S26...' {
        $device = Resolve-OmiTdtS26Device -AdbPath ([string]$txtAdb.Text)
        $folders = @(Get-BuilderDeviceTestStaging -AdbPath $txtAdb.Text -Serial $script:DeviceSerial)
        if ($folders.Count -eq 0) {
            Set-Status $lblOmiClearDeviceTests 'No Builder test files remain on the S26' 'good'
            Set-Status $lblPhoneStorage 'Phone test storage clean' 'good'
            return
        }
        $confirm = [System.Windows.Forms.MessageBox]::Show(
            ("Remove $($folders.Count) Builder-owned TEMPORARY test folder(s) on $($device.Model)?`r`n`r`n" +
            ($folders -join "`r`n") +
            "`r`n`r`nThis will NOT remove application data, recordings, or saved Windows reports."),
            'Confirm S26 Builder test cleanup',
            [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        $result = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not [bool]$result.VerifiedClean) { throw 'Phone cleanup did not verify an empty Builder staging area.' }
        $summary = ('Cleared {0} temporary folder(s); about {1:N1} MiB reclaimed. Verified empty.' -f $result.RemovedCount,$result.FreedMiB)
        Set-Status $lblOmiClearDeviceTests $summary 'good'
        Set-Status $lblPhoneStorage 'Builder test files removed from S26' 'good'
        $txtDeployResult.Text = $summary
        Append-Log ('Manual S26 cleanup: ' + $summary)
    }
})

# This is a separate Qwen-tab entry point to the SAME shared, allowlisted
# cleanup routine used for Omi; it does not delete user files or whole tmp.
$btnQwenStageFClear.Add_Click({
    Invoke-UiAction 'Manually clearing Builder-owned temporary S26 test files...' {
        $device = Resolve-OmiTdtS26Device -AdbPath ([string]$txtAdb.Text)
        $folders = @(Get-BuilderDeviceTestStaging -AdbPath $txtAdb.Text -Serial $script:DeviceSerial)
        if ($folders.Count -gt 0) {
            $confirm = [System.Windows.Forms.MessageBox]::Show(
                ("Clear $($folders.Count) Builder TEMPORARY test folder(s) on $($device.Model)?`r`n`r`n" +
                ($folders -join "`r`n") +
                "`r`n`r`nNo recordings, Android application data or Windows files will be deleted."),
                'Confirm S26 Builder test cleanup',
                [System.Windows.Forms.MessageBoxButtons]::YesNo,
                [System.Windows.Forms.MessageBoxIcon]::Warning)
            if ($confirm -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        }
        # Run the verified post-delete scan even when the first scan was empty.
        $result = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not [bool]$result.VerifiedClean) { throw 'Manual cleanup was not verified; reconnect the S26 and retry.' }
        $summary = ('S26 staging empty; removed {0} folder(s), reclaimed {1:N1} MiB.' -f $result.RemovedCount,$result.FreedMiB)
        Set-Status $lblQwenStageFClear $summary 'good'
        Set-Status $lblPhoneStorage 'Phone Builder test storage verified empty' 'good'
        Append-Log ('[Qwen manual S26 cleanup] ' + $summary)
    }
})

# This receipt describes only predictor/joint neural execution. It MUST NOT
# imply canonical NeMo equivalence, an accurate encoder or a complete transcript.
function Write-OmiTdtS26NeuralReport {
    param(
        [Parameter(Mandatory=$true)][string]$RunRoot,
        [Parameter(Mandatory=$true)][System.Collections.IDictionary]$Results,
        [Parameter(Mandatory=$true)][string]$CleanupText,
        [switch]$RecoveredFromSavedOutputs
    )
    $bothPassed = ($Results.Contains('predictor') -and $Results.Contains('joint') -and
        [bool]$Results['predictor'].pass -and [bool]$Results['joint'].pass)
    $reportPath = Join-Path $RunRoot 'omi-tdt-s26-htp-validation.json'
    $report = [ordered]@{
        schema = 1
        source = 'omi-health/omi-med-stt-v1'
        status = $(if ($bothPassed) { 'omi_v2_tdt_neural_s26_pass_not_full_model' } else { 'omi_v2_tdt_neural_s26_not_qualified' })
        target = 'Samsung Galaxy S26 SM8850 Hexagon V81'
        neural_graphs = $Results
        cleanup = $CleanupText
        recovered_from_saved_outputs = [bool]$RecoveredFromSavedOutputs
        canonical_nemo_parity = $false
        blank_duration_semantics_proven = $false
        full_transcript_pass = $false
        encoder_numerical_qualification = $false
        publishable = $false
    }
    $tempPath = $reportPath + '.tmp'
    try {
        $report | ConvertTo-Json -Depth 9 | Set-Content -LiteralPath $tempPath -Encoding UTF8
        Move-Item -LiteralPath $tempPath -Destination $reportPath -Force
    } finally {
        if (Test-Path -LiteralPath $tempPath -PathType Leaf) { Remove-Item -LiteralPath $tempPath -Force -ErrorAction SilentlyContinue }
    }
    return $reportPath
}

# Recover a finished S26 run when the Windows GUI failed AFTER qnn-net-run
# and the numerical comparisons succeeded. No phone connection is required.
# Fail closed on mismatched context binaries, reference manifests, backends,
# incomplete raw outputs, or a difference under the pinned thresholds.
function Recover-OmiTdtS26SavedProof {
    param([Parameter(Mandatory=$true)][string]$DesktopFolder)
    $py = Join-Path $Root 'tools\python311\python.exe'
    if (-not (Test-Path -LiteralPath $py -PathType Leaf)) {
        $py = Join-Path $Root 'tools\parakeet-export-env\Scripts\python.exe'
    }
    if (-not (Test-Path -LiteralPath $py -PathType Leaf)) { throw 'Project Python missing; open Omi Setup first.' }
    $checked = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiTdtHtpPath,'check-compile','--folder',$DesktopFolder) -TimeoutSeconds 600
    if ($checked.ExitCode -ne 0) { throw ('Omi context checksum/provenance validation failed: ' + $checked.StdErr + $checked.StdOut) }
    $ctx = $checked.StdOut | ConvertFrom-Json
    if ([string]$ctx.status -ne 'omi_v2_tdt_contexts_compiled_unvalidated' -or [bool]$ctx.publishable) { throw 'Unexpected compiled Omi context qualification.' }
    $cloud = Get-Content -LiteralPath (Join-Path $DesktopFolder 'omi-tdt-cloud-compile.json') -Raw | ConvertFrom-Json
    $deviceRoot = Join-Path $DesktopFolder 'device'
    if (-not (Test-Path -LiteralPath $deviceRoot -PathType Container)) { throw 'No saved Step 5F device results found. Run Step 5F first.' }
    $runs = @(Get-ChildItem -LiteralPath $deviceRoot -Directory -Filter 'omi-tdt-htp-*' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
    if ($runs.Count -eq 0) { throw 'No saved Step 5F device results found. Run Step 5F first.' }
    $failure = ''
    foreach ($run in $runs) {
        try {
            $results = [ordered]@{}
            foreach ($role in @('predictor','joint')) {
                $dir = Join-Path $run.FullName $role
                $oldPath = Join-Path $dir 'comparison.json'
                if (-not (Test-Path -LiteralPath $oldPath -PathType Leaf)) { throw ('Missing ' + $role + ' original device comparison.') }
                $old = Get-Content -LiteralPath $oldPath -Raw | ConvertFrom-Json
                $kindExpected = 'omi_tdt_' + $role + '_v2'
                if ([int]$old.schema -ne 1 -or [string]$old.kind -ne $kindExpected -or
                    [string]$old.requested_backend -ne 'libQnnHtp.so' -or
                    [bool]$old.cpu_backend_requested -or -not [bool]$old.pass) {
                    throw ('Original ' + $role + ' comparison does not prove HTP PASS.')
                }
                $contextCopy = Join-Path $dir 'context.bin'
                $refCopy = Join-Path $dir 'proof.reference.json'
                $expectedHash = [string]$cloud.contexts.$role.sha256
                $referenceHash = [string]$cloud.contexts.$role.reference_sha256
                if (-not (Test-Path -LiteralPath $contextCopy -PathType Leaf) -or
                    (Get-FileHash -LiteralPath $contextCopy -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
                    throw ('Staged ' + $role + ' context hash differs from verified cloud binary.')
                }
                if (-not (Test-Path -LiteralPath $refCopy -PathType Leaf) -or
                    (Get-FileHash -LiteralPath $refCopy -Algorithm SHA256).Hash.ToLowerInvariant() -ne $referenceHash) {
                    throw ('Staged ' + $role + ' reference hash differs from verified desktop reference.')
                }
                # Never overwrite the original qnn-net-run comparison.json: it is
                # direct on-device evidence. Copy only the small pulled tensor
                # outputs into a separate offline re-comparison folder.
                $auditDir = Join-Path (Join-Path $run.FullName 'offline-recompare') $role
                if (Test-Path -LiteralPath $auditDir) { Remove-Item -LiteralPath $auditDir -Recurse -Force }
                New-Item -ItemType Directory -Path $auditDir -Force | Out-Null
                $pulledDir = Join-Path $dir 'qnn_out_device'
                if (-not (Test-Path -LiteralPath $pulledDir -PathType Container)) { throw ('Saved ' + $role + ' device output tensors are missing.') }
                Copy-Item -LiteralPath $pulledDir -Destination (Join-Path $auditDir 'qnn_out_device') -Recurse -Force
                $r = Compare-QnnSavedDeviceOutput -HostOutputFolder $auditDir -ReferenceJson ([string]$ctx.references.$role)
                if (-not [bool]$r.Pass -or [string]$r.Kind -ne $kindExpected) { throw ('Saved ' + $role + ' output failed fresh numerical comparison.') }
                $results[$role] = [ordered]@{
                    pass = [bool]$r.Pass; kind = [string]$r.Kind
                    max_abs = [double]$r.MaxAbsError; mean_abs = [double]$r.MeanAbsError
                    rmse = [double]$r.Rmse; cosine = [double]$r.Cosine
                    comparison = [string]$r.ComparisonReport; original_device_comparison = $oldPath; diagnostics_zip = [string]$r.ComparisonBundle
                }
                Append-Log ('Recovered saved original Omi ' + $role + ' HTP PASS by re-comparing Windows tensors: ' + $oldPath)
            }
            $note = 'Offline report recovery only; no ADB activity or phone cleanup performed in this action. Consult the original Step 5F session log for its cleanup result.'
            $receipt = Write-OmiTdtS26NeuralReport -RunRoot $run.FullName -Results $results -CleanupText $note -RecoveredFromSavedOutputs
            return $receipt
        } catch {
            $failure = $_.Exception.Message
            Append-Log ('Could not recover ' + $run.FullName + ': ' + $failure)
        }
    }
    throw ('No saved Step 5F run was eligible for safe offline recovery. ' + $failure + ' You can run Step 5F again without recompiling.')
}

$btnOmiTdtRecover.Add_Click({
    Invoke-UiAction 'Rechecking saved original Omi v2 decoder S26 tensors (OFFLINE; no ADB)...' {
        $folder = Get-OmiLatestTdtDesktopFolder
        $reportPath = Recover-OmiTdtS26SavedProof -DesktopFolder $folder
        $script:LastDeployOutputFolder = Split-Path -Parent $reportPath
        Set-Status $lblOmiTdtDevice 'S26 predictor + joint HTP PASS recovered (neural only)' 'good'
        $txtDeployResult.Text = 'Omi v2 decoder S26 neural HTP PASS (recovered from previously saved device outputs)' + "`r`n" + 'Report: ' + $reportPath + "`r`n" + 'No new phone run, cleanup or Qualcomm compilation. Encoder accuracy, canonical NeMo and full transcript remain unqualified.'
        Append-Log ('Omi v2 decoder S26 saved proof revalidated: ' + $reportPath)
    }
})

$btnOmiTdtDevice.Add_Click({
    Invoke-UiAction 'Testing original Omi v2 TDT predictor plus joint on Samsung S26 Hexagon V81...' {
        $folder = Get-OmiLatestTdtDesktopFolder
        $py = Ensure-OmiNativeEnvironmentFromUi
        $checked = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiTdtHtpPath,'check-compile','--folder',$folder) -TimeoutSeconds 600
        if ($checked.ExitCode -ne 0) { throw ('Omi decoder QNN checksum verification failed: ' + $checked.StdErr + $checked.StdOut) }
        $ctx = $checked.StdOut | ConvertFrom-Json
        if ([string]$ctx.status -ne 'omi_v2_tdt_contexts_compiled_unvalidated') { throw 'Unexpected Omi decoder compilation receipt.' }
        $device = Resolve-OmiTdtS26Device -AdbPath ([string]$txtAdb.Text)
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('QNN Android runtime incomplete: ' + ($runtime.Missing -join ', ')) }
        $runRoot = Join-Path $folder ('device\omi-tdt-htp-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $runRoot -Force | Out-Null
        $results = [ordered]@{}
        $cleanupText = 'Automatic cleanup pending.'
        try {
            foreach ($role in @('predictor','joint')) {
                $dest = Join-Path $runRoot $role
                Append-Log ('S26 Omi v2 ' + $role + ' HTP test: ' + $dest)
                $r = Invoke-QnnProofOnDevice -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -ContextPath ([string]$ctx.contexts.$role) -ReferenceJson ([string]$ctx.references.$role) -RuntimeRoot $txtRuntimeRoot.Text -HostOutputFolder $dest
                $results[$role] = [ordered]@{ pass = [bool]$r.Pass; kind=[string]$r.Kind; max_abs=[double]$r.MaxAbsError; mean_abs=[double]$r.MeanAbsError; rmse=[double]$r.Rmse; cosine=[double]$r.Cosine; comparison=[string]$r.ComparisonReport; diagnostics_zip=[string]$r.ComparisonBundle }
                Append-Log ('Omi v2 ' + $role + ' HTP ' + $(if ($r.Pass) { 'PASS' } else { 'FAIL' }) + ': ' + [string]$r.ComparisonReport)
                if (-not $r.Pass) { break }
            }
        } finally {
            if ($script:DeviceSerial) {
                try {
                    $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                    $cleanupText = if ($clean.WasPresent) { ('Removed ' + [Math]::Round($clean.FreedMiB,1) + ' MiB from S26') } else { 'Verified phone test folder empty' }
                    Set-Status $lblPhoneStorage 'Phone test folder clean' 'good'
                } catch {
                    $cleanupText = 'Automatic cleanup FAILED: ' + $_.Exception.Message
                    Append-Log $cleanupText
                    Set-Status $lblPhoneStorage 'Use Clean phone files on Deploy & Test' 'bad'
                }
            }
        }
        if ($cleanupText -match '(?i)fail|pending') { throw ('Phone test files not confirmed removed: ' + $cleanupText + '. Use Clear previous S26 test files after reconnecting ADB.') }
        $bothPassed = ($results.Contains('predictor') -and $results.Contains('joint') -and [bool]$results['predictor'].pass -and [bool]$results['joint'].pass)
        $reportPath = Write-OmiTdtS26NeuralReport -RunRoot $runRoot -Results $results -CleanupText $cleanupText
        $script:LastDeployOutputFolder = $runRoot
        $txtDeployResult.Text = 'Omi v2 TDT original decoder S26 test: ' + $(if ($bothPassed) { 'NEURAL HTP PASS' } else { 'NUMERICAL FAIL' }) + "`r`n" + 'Report: ' + $reportPath + "`r`n" + 'Cleanup: ' + $cleanupText + "`r`n" + 'The encoder remains numerically unqualified; no NeMo parity or complete transcript.'
        if ($bothPassed) { Set-Status $lblOmiTdtDevice 'Original v2 predictor + joint HTP PASS (neural only)' 'good' }
        else { Set-Status $lblOmiTdtDevice 'Decoder HTP numerical accuracy FAIL; see report' 'bad' }
        Append-Log ('Omi v2 decoder S26 proof: ' + $reportPath)
    }
})

$btnOmiTranscriptBrowse.Add_Click({ Choose-File -TextBox $txtOmiTranscriptAudio -Title 'Select non-sensitive Omi audio (first 12 seconds only)' -Filter 'Audio files (*.wav;*.ogg)|*.wav;*.ogg|All files (*.*)|*.*' })
$btnOmiTranscript.Add_Click({
    Invoke-UiAction 'Running original Omi v2 12-second desktop ONNX TDT greedy candidate transcription...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Complete Omi encoder Step 3 first.' }
        $decoder = Get-OmiLatestTdtDesktopFolder
        $audio = [string]$txtOmiTranscriptAudio.Text
        if ([string]::IsNullOrWhiteSpace($audio)) { $audio = [string]$txtOmiAudio.Text }
        if ([string]::IsNullOrWhiteSpace($audio) -or -not (Test-Path -LiteralPath $audio -PathType Leaf)) {
            throw 'Choose a non-sensitive .wav or .ogg recording. You can also use your Step 3 reference audio.'
        }
        $nemo = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        if (-not (Test-Path -LiteralPath $nemo -PathType Leaf)) { throw 'Original Omi .nemo file missing; restore Step 2 verified download.' }
        $python = Ensure-OmiTdtTranscriptEnvironmentFromUi
        if (-not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
            $ff = Install-FfmpegFromUi
            if (-not (Test-Path -LiteralPath $ff -PathType Leaf)) { throw 'FFmpeg is required.' }
        }
        $outDir = Join-Path $script:OmiRunFolder ('omi-tdt-transcript-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $args = @($script:OmiTdtTranscribePath,'transcribe','--encoder-folder',$script:OmiRunFolder,
            '--decoder-folder',$decoder,'--cache-dir',(Join-Path $script:OmiModelDir 'native-extracted'),
            '--nemo',$nemo,'--audio',$audio,'--ffmpeg',$txtFfmpeg.Text,'--output-dir',$outDir)
        $run = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 21600
        if ($run.ExitCode -ne 0) { throw ('Omi desktop TDT transcript failed: ' + $run.StdErr + [Environment]::NewLine + $run.StdOut) }
        $reportPath = Join-Path $outDir 'omi-tdt-desktop-candidate-transcript.json'
        if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) { throw 'Omi desktop transcript report was not created.' }
        $proof = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if ([bool]$proof.checks.publishable -or [bool]$proof.checks.full_transcript_pass -or [bool]$proof.checks.canonical_nemo_transcript_parity_tested) {
            throw 'Omi candidate report unexpectedly claims full model qualification.'
        }
        if ([string]$proof.status -notin @('candidate_desktop_omi_v2_tdt_transcript_not_nemo_verified','desktop_omi_v2_tdt_empty_candidate_not_nemo_verified')) {
            throw 'Unrecognized Omi candidate transcript status.'
        }
        $script:LastDeployOutputFolder = $outDir
        $prefix = if ([bool]$proof.source_audio_truncated_to_first_12_seconds) { 'First 12 seconds ONLY; source audio was longer.' } else { 'Up to first 12 seconds only.' }
        $txtDeployResult.Text = "OMI ORIGINAL V2 - DESKTOP CANDIDATE TRANSCRIPT`r`n$prefix`r`n`r`n" + [string]$proof.transcript + "`r`n`r`nToken count: " + [string]$proof.greedy_controller.token_ids.Count + "`r`nReport: " + $reportPath + "`r`n`r`nNOT NeMo transcript parity. NOT S26 end-to-end. Encoder HTP numerical gate remains failed; publication locked."
        if ([string]::IsNullOrWhiteSpace([string]$proof.transcript)) {
            Set-Status $lblOmiTranscript 'Controller ran; empty candidate - inspect report' 'warn'
        } else {
            Set-Status $lblOmiTranscript '12s desktop candidate saved (not production qualified)' 'good'
        }
        Append-Log ('Omi original v2 desktop candidate saved locally: ' + $reportPath + '; source truncated: ' + [string]$proof.source_audio_truncated_to_first_12_seconds)
        [System.Windows.Forms.MessageBox]::Show(('Desktop Omi v2 candidate transcript saved.' + "`r`n`r`n" + $prefix + "`r`n`r`n" + [string]$proof.transcript + "`r`n`r`n" + 'Report: ' + $reportPath + "`r`n`r`n" + 'Unverified against canonical NeMo; NOT clinical or S26 full-model proof.'),'Omi v2 desktop candidate',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})


$btnOmiS26Transcript.Add_Click({
    Invoke-UiAction 'Running experimental Omi v2 12-second S26 HTP end-to-end transcript...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Complete Omi encoder Steps 3 and 4 first.' }
        $decoder = Get-OmiLatestTdtDesktopFolder
        $audio = [string]$txtOmiTranscriptAudio.Text
        if ([string]::IsNullOrWhiteSpace($audio)) { $audio = [string]$txtOmiAudio.Text }
        if ([string]::IsNullOrWhiteSpace($audio) -or -not (Test-Path -LiteralPath $audio -PathType Leaf)) {
            throw 'Select non-sensitive .wav or .ogg audio in Step 5G (same clip recommended for comparison).'
        }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('S26 QNN runtime tools missing: ' + ($runtime.Missing -join ', ')) }
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Connect an authorized Galaxy S26 with USB debugging enabled, then refresh the device selection.' }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'The selected ADB device is not the supported Galaxy S26.' }
        $python = Ensure-OmiTdtTranscriptEnvironmentFromUi
        if (-not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
            $ff = Install-FfmpegFromUi
            if (-not (Test-Path -LiteralPath $ff -PathType Leaf)) { throw 'FFmpeg is required for this transcript test.' }
        }
        $nemo = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        if (-not (Test-Path -LiteralPath $nemo -PathType Leaf)) { throw 'Original Omi checkpoint not found; preserve Step 2 download.' }
        $outDir = Join-Path $script:OmiRunFolder ('omi-tdt-s26-transcript-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
        $bundle = [ordered]@{}
        foreach ($entry in $runtime.Files.GetEnumerator()) { $bundle[[string]$entry.Key] = [string]$entry.Value }
        $runtimeManifest = Join-Path $outDir 'qnn-runtime-local-paths.json'
        $json = $bundle | ConvertTo-Json -Depth 4
        $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
        [System.IO.File]::WriteAllText($runtimeManifest,$json,$utf8NoBom)
        $args = @($script:OmiTdtS26TranscribePath,'transcribe','--encoder-folder',$script:OmiRunFolder,
            '--decoder-folder',$decoder,'--cache-dir',(Join-Path $script:OmiModelDir 'native-extracted'),
            '--nemo',$nemo,'--audio',$audio,'--ffmpeg',$txtFfmpeg.Text,
            '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$runtimeManifest,
            '--output-dir',$outDir,'--auto-clean','true')
        $run = $null
        $deviceCleanupSucceeded = $false
        try {
            $run = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 7200
        } finally {
            # The Python runner cleans its own staging directory. After any
            # exception/timeout, independently remove ALL allowlisted orphaned
            # Omi test sessions and verify the device is clean.
            try {
                $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                $deviceCleanupSucceeded = [bool]$clean.VerifiedClean
                Append-Log ('S26 transcript staging cleanup verified: ' + $clean.RemovedCount + ' leftover folders removed')
                Set-Status $lblPhoneStorage 'S26 Builder test staging cleared' 'good'
            } catch {
                Append-Log ('S26 transcript cleanup FAILED - run Clear previous S26 test files: ' + $_.Exception.Message)
                Set-Status $lblPhoneStorage 'S26 cleanup failed; use Clear previous S26 test files' 'bad'
                $script:OmiDeviceCleanupFailed = $true
            }
        }
        if (-not $deviceCleanupSucceeded) { throw 'S26 test cleanup could not be verified; reconnect phone and click Clear previous S26 test files. No qualification result will be accepted.' }
        if ($run.ExitCode -ne 0) { throw ('S26 Omi TDT transcript failed (no qualified result): ' + $run.StdErr + [Environment]::NewLine + $run.StdOut) }
        $reportPath = Join-Path $outDir 'omi-tdt-s26-candidate-transcript.json'
        if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) { throw 'No final S26 transcript report was saved.' }
        $proof = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if ([string]$proof.status -ne 'candidate_omi_v2_12s_s26_htp_transcript_not_model_qualified' -or
            -not [bool]$proof.on_device_execution -or [bool]$proof.checks.publishable -or
            [bool]$proof.checks.full_transcript_pass -or [bool]$proof.checks.canonical_nemo_transcript_parity_tested) {
            throw 'S26 candidate report has an unexpected qualification status.'
        }
        $script:LastDeployOutputFolder = $outDir
        Set-OmiStep5JSourceFromCompletedRun -Path $reportPath
        $txtDeployResult.Text = "OMI ORIGINAL V2 - S26 HTP TRANSCRIPT (12s ONLY)`r`n`r`nS26:`r`n" + [string]$proof.s26_candidate.transcript + "`r`n`r`nDesktop:`r`n" + [string]$proof.desktop_baseline.transcript + "`r`n`r`nEncoder FP16 gate: " + [string]$proof.encoder_htp_comparison.original_provisional_gate_pass + "`r`nToken/duration mismatches: " + [string]$proof.joint_htp_decisions.mismatch_count + "`r`nReport: " + $reportPath + "`r`nCleanup: " + [string]$proof.cleanup + "`r`n`r`nNOT qualified for production or canonical NeMo parity."
        Set-Status $lblOmiS26Transcript 'S26 candidate saved; inspect encoder and transcript differences' 'warn'
        Append-Log ('Omi S26 end-to-end candidate saved: ' + $reportPath)
        [System.Windows.Forms.MessageBox]::Show(('S26 Omi v2 12-second candidate saved.' + "`r`n`r`n" + [string]$proof.s26_candidate.transcript + "`r`n`r`nDesktop comparison: " + [string]$proof.desktop_baseline.transcript + "`r`n`r`nReport: " + $reportPath + "`r`n`r`nNeMo parity and encoder numerical qualification remain pending."),'Omi S26 candidate transcript',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})


$btnOmiMultiS26.Add_Click({
    Invoke-UiAction 'Testing consecutive Omi v2 windows on S26 with one shared QNN staging...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Complete Omi encoder Steps 3 and 4 first.' }
        $decoder = Get-OmiLatestTdtDesktopFolder
        $audio = [string]$txtOmiTranscriptAudio.Text
        if ([string]::IsNullOrWhiteSpace($audio)) { $audio = [string]$txtOmiAudio.Text }
        if ([string]::IsNullOrWhiteSpace($audio) -or -not (Test-Path -LiteralPath $audio -PathType Leaf)) { throw 'Choose the matching non-sensitive .wav/.ogg audio above.' }
        $start = 0
        $count = 0
        if (-not [int]::TryParse(([string]$txtOmiMultiStart.Text).Trim(),[ref]$start) -or $start -lt 0 -or $start -gt 7200) {
            throw 'Start seconds must be an integer from 0 to 7200.'
        }
        if (-not [int]::TryParse(([string]$txtOmiMultiCount.Text).Trim(),[ref]$count) -or $count -lt 1 -or $count -gt 3) {
            throw 'Select 1, 2, or 3 consecutive diagnostic windows.'
        }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('S26 QNN runtime tools missing: ' + ($runtime.Missing -join ', ')) }
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Connect an authorized Galaxy S26 and refresh the selected device.' }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'Refusing to run Omi on an unexpected device.' }
        $python = Ensure-OmiTdtTranscriptEnvironmentFromUi
        if (-not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
            $ff = Install-FfmpegFromUi
            if (-not (Test-Path -LiteralPath $ff -PathType Leaf)) { throw 'FFmpeg is required for the multi-window test.' }
        }
        $nemo = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        if (-not (Test-Path -LiteralPath $nemo -PathType Leaf)) { throw 'Original Omi checkpoint missing; keep the Step 2 model files.' }
        $outDir = Join-Path $script:OmiRunFolder ('omi-tdt-multiwindow-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
        $bundle = [ordered]@{}
        foreach ($entry in $runtime.Files.GetEnumerator()) { $bundle[[string]$entry.Key] = [string]$entry.Value }
        $runtimeManifest = Join-Path $outDir 'qnn-runtime-local-paths.json'
        $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
        [System.IO.File]::WriteAllText($runtimeManifest,($bundle | ConvertTo-Json -Depth 4),$utf8NoBom)
        $args = @($script:OmiTdtS26TranscribePath,'multiwindow','--encoder-folder',$script:OmiRunFolder,
            '--decoder-folder',$decoder,'--cache-dir',(Join-Path $script:OmiModelDir 'native-extracted'),
            '--nemo',$nemo,'--audio',$audio,'--ffmpeg',$txtFfmpeg.Text,
            '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$runtimeManifest,
            '--output-dir',$outDir,'--start-seconds',([string]$start),'--window-count',([string]$count),
            '--auto-clean','true')
        $run = $null
        $deviceCleanupSucceeded = $false
        try {
            $run = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 7200
        } finally {
            # The Python runner cleans its own staging directory. After any
            # exception/timeout, independently remove ALL allowlisted orphaned
            # Omi test sessions and verify the device is clean.
            try {
                $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                $deviceCleanupSucceeded = [bool]$clean.VerifiedClean
                Append-Log ('Multi-window staging cleanup verified: ' + $clean.RemovedCount + ' leftover folders removed')
                Set-Status $lblPhoneStorage 'S26 Builder test staging cleared' 'good'
            } catch {
                Append-Log ('Multi-window cleanup FAILED - run Clear previous S26 test files: ' + $_.Exception.Message)
                Set-Status $lblPhoneStorage 'S26 cleanup failed; use Clear previous S26 test files' 'bad'
                $script:OmiDeviceCleanupFailed = $true
            }
        }
        if (-not $deviceCleanupSucceeded) { throw 'S26 test cleanup could not be verified; reconnect phone and click Clear previous S26 test files. No qualification result will be accepted.' }
        if ($run.ExitCode -ne 0) { throw ('Omi multi-window experiment incomplete: ' + $run.StdErr + [Environment]::NewLine + $run.StdOut) }
        $reportPath = Join-Path $outDir 'omi-tdt-s26-multiwindow-diagnostic.json'
        if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) { throw 'No S26 multi-window receipt was saved.' }
        $proof = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if ([string]$proof.status -ne 'omi_v2_s26_multiwindow_candidate_not_long_form_qualified' -or
            -not [bool]$proof.all_windows_completed -or -not [bool]$proof.on_device_execution -or
            [bool]$proof.publishable -or [bool]$proof.full_transcript_pass -or [bool]$proof.native_android_persistent_qnn) {
            throw 'Multi-window receipt failed status verification.'
        }
        $lines = New-Object 'System.Collections.Generic.List[string]'
        foreach ($item in @($proof.windows)) {
            $lines.Add(('Window {0}, {1}s: S26: {2}' -f $item.window_index,$item.start_seconds,$item.s26_transcript))
            $lines.Add(('                  Desktop: {0} | Equal: {1} | Encoder gate: {2}' -f $item.desktop_transcript,$item.text_equal,$item.encoder_gate_pass))
        }
        $script:LastDeployOutputFolder = $outDir
        Set-OmiStep5JSourceFromCompletedRun -Path $reportPath
        $txtDeployResult.Text = "OMI V2 S26 - MULTI-WINDOW EXPERIMENT (NOT CONTINUOUS LONG-FORM)`r`n`r`n" + ($lines -join "`r`n") + "`r`n`r`nReport: " + $reportPath + "`r`nCleanup: " + [string]$proof.cleanup + "`r`n`r`nContexts staged once, but qnn-net-run still relaunched per graph. NeMo parity and clinical qualification NOT established."
        Set-Status $lblOmiMultiS26 'Multi-window candidate saved; model NOT long-form qualified' 'warn'
        Append-Log ('Omi S26 multi-window diagnostic: ' + $reportPath)
        [System.Windows.Forms.MessageBox]::Show(('S26 multi-window diagnostic complete. Windows: ' + [string]$proof.windows.Count + "`r`nMatching text all windows: " + [string]$proof.same_text_all_windows + "`r`n`r`nReport: " + $reportPath + "`r`n`r`nThis is not native Android or validated long-form transcription."),'Omi S26 multi-window diagnostic',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})


$btnOmiCompareS26Browse.Add_Click({ Choose-File -TextBox $txtOmiCompareS26Json -Title 'Select saved Omi S26 candidate JSON (Step 5H/5I)' -Filter 'JSON reports (*.json)|*.json|All files (*.*)|*.*' })
$btnOmiGpuTextBrowse.Add_Click({ Choose-File -TextBox $txtOmiGpuText -Title 'Select original Omi WhisperApp GPU transcription TXT' -Filter 'Text transcripts (*.txt)|*.txt|All files (*.*)|*.*' })
$btnOmiGpuReferenceReview.Add_Click({
    Invoke-UiAction 'Checking whether the S26 recording opening matches the original WhisperApp GPU reference...' {
        if (-not (Test-OmiStep5JSourceReport -Path ([string]$txtOmiCompareS26Json.Text))) { Restore-OmiStep5JSource }
        $sourceJson = [string]$txtOmiCompareS26Json.Text
        $reference = [string]$txtOmiGpuText.Text
        if ([string]::IsNullOrWhiteSpace($sourceJson) -or -not (Test-Path -LiteralPath $sourceJson -PathType Leaf)) { throw 'Choose a completed Step 5H/5I S26 candidate JSON.' }
        if ([string]::IsNullOrWhiteSpace($reference) -or -not (Test-Path -LiteralPath $reference -PathType Leaf)) { throw 'Choose the unmodified WhisperApp GPU reference TXT file.' }
        $outDir = Join-Path ([System.IO.Path]::GetDirectoryName($sourceJson)) ('gpu-reference-review-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        $python = Ensure-OmiTdtTranscriptEnvironmentFromUi
        $args = @($script:OmiTdtReferenceReviewPath,'--s26-report',$sourceJson,'--gpu-text',$reference,'--output-dir',$outDir)
        $result = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 120
        if ($result.ExitCode -ne 0) { throw ('GPU reference review failed: ' + $result.StdErr + [Environment]::NewLine + $result.StdOut) }
        $reportPath = Join-Path $outDir 'omi-tdt-s26-gpu-reference-review.json'
        $proof = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if ([string]$proof.status -ne 'gpu_reference_text_review_only_not_clinical_accuracy' -or
            [bool]$proof.same_source_audio_proven -or [bool]$proof.word_error_rate_measured -or [bool]$proof.publishable) {
            throw 'The GPU-text review returned an unexpected qualification status.'
        }
        $opening = [string]$proof.windows[0].opening_check.check
        $consistent = [string]$proof.windows[0].opening_check.consistent
        $similarity = [string]$proof.windows[0].opening_check.word_sequence_similarity
        $txtDeployResult.Text = "OMI GPU REFERENCE - OPENING SANITY CHECK ONLY`r`n`r`nCheck: " + $opening + "`r`nLikely consistent: " + $consistent + "`r`nOpening word similarity: " + $similarity + "`r`n`r`nGPU text source: " + $reference + "`r`nS26 report: " + $sourceJson + "`r`nResult: " + $reportPath + "`r`n`r`nNo aligned WER or audio identity is established."
        $script:LastDeployOutputFolder = $outDir
        Set-Status $lblOmiGpuReview 'Offline opening checked; see report for possible audio mismatch' 'warn'
        Append-Log ('Omi GPU opening review: ' + $reportPath)
        [System.Windows.Forms.MessageBox]::Show(('GPU reference opening check: ' + $opening + "`r`nLikely consistent: " + $consistent + "`r`n`r`nReport: " + $reportPath + "`r`n`r`nThis is NOT a certified WER comparison."),'GPU reference sanity check',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnOmiSecond.Add_Click({
    Invoke-UiAction 'Comparing a SECOND Omi recording on the SAME compiled S26 encoder (diagnostic only)...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Select an Omi compiled run first.' }
        $folder = [string]$script:OmiRunFolder
        $context = Join-Path $folder 'compiled\omi-encoder-sm8850-v81-fp16.bin'
        if (-not (Test-Path -LiteralPath $context -PathType Leaf)) { throw 'Compiled Omi context missing. Keep your existing compiled model workspace.' }
        $secondaryAudio = [string]$txtOmiSecondAudio.Text
        if ([string]::IsNullOrWhiteSpace($secondaryAudio) -or -not (Test-Path -LiteralPath $secondaryAudio -PathType Leaf)) { throw 'Choose a second non-sensitive .wav/.ogg recording first.' }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('S26 runtime bundle incomplete: ' + ($runtime.Missing -join ', ')) }
        if (-not $script:DeviceSerial) { Refresh-Device }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'Refusing to test Omi on an unexpected device.' }
        $python = Ensure-OmiNativeEnvironmentFromUi
        $args = @((Join-Path $Root 'python\omi_htp_reference.py'),'prepare','--secondary-audio',
            '--folder',$folder,'--audio',$secondaryAudio,'--cache-dir',(Join-Path $script:OmiModelDir 'native-extracted'),'--ffmpeg',$txtFfmpeg.Text)
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 1800
        if ($r.ExitCode -ne 0) { throw ('Omi secondary ONNX reference failed: ' + $r.StdErr + $r.StdOut) }
        $prepared = $r.StdOut | ConvertFrom-Json
        if ([string]$prepared.status -ne 'prepared_omi_secondary_reference') { throw 'Unexpected secondary reference status.' }
        $output = Join-Path $folder ('device\omi-secondary-htp-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        $result = $null
        $cleanupText = 'Automatic cleanup pending.'
        try {
            $result = Invoke-QnnProofOnDevice -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -ContextPath $context -ReferenceJson ([string]$prepared.reference) -RuntimeRoot $txtRuntimeRoot.Text -HostOutputFolder $output
        } finally {
            if ($script:DeviceSerial) {
                try {
                    $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                    $cleanupText = if ($clean.WasPresent) { ('Auto-clean removed {0:N1} MiB.' -f $clean.FreedMiB) } else { 'Phone staging empty.' }
                    Set-Status $lblPhoneStorage 'Phone test folder clean (0 MiB staged).' 'good'
                } catch {
                    $cleanupText = 'Auto-clean FAILED: ' + $_.Exception.Message
                    Set-Status $lblPhoneStorage 'Use Clear previous S26 test files' 'bad'
                    Append-Log $cleanupText
                }
            }
        }
        if ($cleanupText -match '(?i)fail|pending') { throw ('Phone test files not confirmed removed: ' + $cleanupText + '. Use Clear previous S26 test files after reconnecting ADB.') }
        $script:LastDeployOutputFolder = $output
        $diagArgs = @((Join-Path $Root 'python\omi_htp_diagnostics.py'),'analyze','--device-folder',$output,'--reference',([string]$prepared.reference))
        $analysis = Invoke-ProcessCapture -FilePath $python -Arguments $diagArgs -TimeoutSeconds 600
        if ($analysis.ExitCode -ne 0) { throw ('Device ran, but numerical analysis failed: ' + $analysis.StdErr + $analysis.StdOut) }
        $detail = $analysis.StdOut | ConvertFrom-Json
        Set-Status $lblOmiSecond ('S26 ran; numeric ' + $(if ($result.Pass) { 'PASS' } else { 'FAIL' }) + ' (diagnostic only)') 'warn'
        $txtDeployResult.Text = "Omi SECOND-SAMPLE diagnostic only`r`nDevice: $($device.Model)`r`nExisting context: $context`r`nCosine: $($result.Cosine)`r`nMAE: $($result.MeanAbsError)`r`nRMSE: $($result.Rmse)`r`nMax error: $($result.MaxAbsError)`r`nNumerical gate: $($result.Pass)`r`nReport: $($detail.report)`r`nComparison: $($result.ComparisonReport)`r`nCleanup: $cleanupText`r`nNeither the original reference nor publication status was changed."
        Append-Log ('Omi SECOND-SAMPLE HTP comparison: ' + [string]$result.ComparisonReport + '; detailed report: ' + [string]$detail.report)
        Update-OmiState
        Set-Status $lblOmiSecond 'Second recording tested; inspect diagnostic report' 'warn'
    }
})

$btnOmiHtpTest.Add_Click({
    Invoke-UiAction 'Omi compiled encoder: S26 Hexagon HTP device validation...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Open a previously exported and compiled Omi run first.' }
        $runFolder = [string]$script:OmiRunFolder
        $modelContext = Join-Path $runFolder 'compiled\omi-encoder-sm8850-v81-fp16.bin'
        $compileRecord = Join-Path $runFolder 'omi-encoder-cloud-compile.json'
        if (-not (Test-Path -LiteralPath $modelContext -PathType Leaf) -or -not (Test-Path -LiteralPath $compileRecord -PathType Leaf)) {
            throw 'No verified Omi compiler output in this run. Use Step 4 first.'
        }
        # v0.3.25 saved the audio SHA-256 but did not save its source path.
        # Recover a verified match from the qualified export and Builder logs,
        # without re-exporting or recompiling the 1.2 GB Omi context.
        $selectedOmiAudio = [string]$txtOmiAudio.Text
        if ([string]::IsNullOrWhiteSpace($selectedOmiAudio) -or -not (Test-Path -LiteralPath $selectedOmiAudio -PathType Leaf)) {
            $locatorPython = Get-LocalBuilderPython
            $locatorArgs = @((Join-Path $Root 'python\omi_audio_resolver.py'),'find',
                '--folder',$runFolder,'--builder-root',$Root)
            if (-not [string]::IsNullOrWhiteSpace($selectedOmiAudio)) { $locatorArgs += @('--preferred',$selectedOmiAudio) }
            if (-not [string]::IsNullOrWhiteSpace($txtAudioFolder.Text)) { $locatorArgs += @('--audio-folder',$txtAudioFolder.Text) }
            $lookup = Invoke-ProcessCapture -FilePath $locatorPython -Arguments $locatorArgs -TimeoutSeconds 120
            if ($lookup.ExitCode -ne 0) { throw ('Could not restore Omi reference audio: ' + $lookup.StdOut + $lookup.StdErr) }
            $located = $lookup.StdOut | ConvertFrom-Json
            if ([string]$located.status -eq 'exact_qualified_audio_found') {
                $txtOmiAudio.Text = [string]$located.audio
                $selectedOmiAudio = [string]$located.audio
                Append-Log ('Restored SHA-256 verified Omi reference audio from ' + [string]$located.source + '.')
                Save-Settings
            } else {
                Set-Status $lblOmiHtpTest 'Choose original Omi test audio (no NPU work started)' 'warn'
                [void][System.Windows.Forms.MessageBox]::Show(
                    'The Builder could not automatically find the original audio used for this compiled Omi encoder.' + "`r`n`r`n" +
                    'Choose the same .ogg or .wav file now. Its SHA-256 will be checked before any device test.' + "`r`n`r`n" +
                    'You do NOT need to export the model or compile it again.',
                    'Locate original Omi reference audio',
                    [System.Windows.Forms.MessageBoxButtons]::OK,
                    [System.Windows.Forms.MessageBoxIcon]::Information)
                Choose-File -TextBox $txtOmiAudio -Title 'Select ORIGINAL Omi qualification audio (hash verified)' -Filter 'Audio files (*.wav;*.ogg)|*.wav;*.ogg|All files (*.*)|*.*'
                $selectedOmiAudio = [string]$txtOmiAudio.Text
                if ([string]::IsNullOrWhiteSpace($selectedOmiAudio) -or -not (Test-Path -LiteralPath $selectedOmiAudio -PathType Leaf)) {
                    Append-Log 'Omi device test canceled: original reference audio was not selected. No device files were staged.'
                    return
                }
                Save-Settings
            }
        }
        if ([string]::IsNullOrWhiteSpace($selectedOmiAudio) -or -not (Test-Path -LiteralPath $selectedOmiAudio -PathType Leaf)) {
            throw 'Select the original Omi reference audio file first. The Builder will verify its SHA-256.'
        }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) {
            throw ('S26 HTP runtime bundle incomplete. Set the Runtime folder under Deploy & Test first. Missing: ' + ($runtime.Missing -join ', '))
        }
        if (-not $script:DeviceSerial) { Refresh-Device }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'S26 device verification failed; refusing Omi HTP test on an unexpected phone.' }
        $omiPython = Ensure-OmiNativeEnvironmentFromUi
        $refArgs = @((Join-Path $Root 'python\omi_htp_reference.py'),'prepare',
            '--folder',$runFolder,'--audio',$selectedOmiAudio,
            '--cache-dir',(Join-Path $script:OmiModelDir 'native-extracted'),
            '--ffmpeg',$txtFfmpeg.Text)
        $prepared = Invoke-ProcessCapture -FilePath $omiPython -Arguments $refArgs -TimeoutSeconds 1800
        if ($prepared.ExitCode -ne 0) { throw ('Omi pinned reference preparation failed: ' + $prepared.StdErr + $prepared.StdOut) }
        $pinned = $prepared.StdOut | ConvertFrom-Json
        if ([string]$pinned.status -ne 'prepared_omi_htp_reference') { throw 'Omi reference tool returned an unexpected status.' }
        $hostOutput = Join-Path $runFolder ('device\omi-encoder-htp-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        $result = $null
        $cleanupText = 'Automatic cleanup pending.'
        try {
            $result = Invoke-QnnProofOnDevice -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -ContextPath $modelContext -ReferenceJson ([string]$pinned.reference) -RuntimeRoot $txtRuntimeRoot.Text -HostOutputFolder $hostOutput
        } finally {
            if ($script:DeviceSerial) {
                try {
                    $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                    $cleanupText = if ($clean.WasPresent) { ('Auto-clean removed approximately {0:N1} MiB from phone.' -f $clean.FreedMiB) } else { 'Auto-clean verified empty test folder.' }
                    Set-Status $lblPhoneStorage 'Phone test folder clean (0 MiB staged).' 'good'
                } catch {
                    $cleanupText = 'Auto-clean failed: ' + $_.Exception.Message
                    Set-Status $lblPhoneStorage 'Use Clear previous S26 test files' 'bad'
                    Append-Log $cleanupText
                }
            }
        }
        if ($cleanupText -match '(?i)fail|pending') { throw ('Phone test files not confirmed removed: ' + $cleanupText + '. Use Clear previous S26 test files after reconnecting ADB.') }
        $script:LastDeployOutputFolder = $hostOutput
        $txtDeployResult.Text = "Omi Med encoder-only HTP test`r`nDevice: $($device.Model)`r`nQNN context: $modelContext`r`nFP16 result: $(if ($result.Pass) { 'PASS' } else { 'FAIL' })`r`nCosine: $($result.Cosine)`r`nMean absolute: $($result.MeanAbsError)`r`nRMSE: $($result.Rmse)`r`nMax absolute: $($result.MaxAbsError)`r`nPadded max: $($result.PaddedMaxAbs)`r`nCleanup: $cleanupText`r`nComparison report: $($result.ComparisonReport)`r`nDiagnostic bundle: $($result.ComparisonBundle)`r`nNot yet compared to canonical NeMo. Not a complete Omi ASR model."
        Append-Log ("Omi encoder HTP run: numeric={0}; cosine={1}; mean_abs={2}; rmse={3}; max_abs={4}; padded_max_abs={5}; report={6}" -f $result.Pass,$result.Cosine,$result.MeanAbsError,$result.Rmse,$result.MaxAbsError,$result.PaddedMaxAbs,$result.ComparisonReport)
        if (-not $result.Pass) {
            Set-Status $lblOmiHtpTest 'S26 executed; FP16 accuracy gate FAILED' 'bad'
            $btnOmiAnalyze.Enabled = $true
            try {
                $analysis = Invoke-OmiSavedNumericalAnalysis
                Set-Status $lblOmiAnalyze 'Offline diagnostic saved; inspect report' 'warn'
                Append-Log ('Omi HTP numerical FAIL diagnostics: ' + [string]$analysis.report)
            } catch {
                Append-Log ('Omi offline analysis was not available: ' + $_.Exception.Message)
                Set-Status $lblOmiAnalyze 'Click Analyze saved S26 output (offline)' 'warn'
            }
            [System.Windows.Forms.MessageBox]::Show(
                ('The Omi encoder executed on the S26 HTP, but numeric FP16 comparison did not meet the provisional thresholds.' + "`r`n`r`n" +
                 'Cosine: ' + $result.Cosine + "`r`n" +
                 'Mean abs: ' + $result.MeanAbsError + "`r`n" +
                 'RMSE: ' + $result.Rmse + "`r`n" +
                 'Max abs: ' + $result.MaxAbsError + "`r`n`r`n" +
                 'Outputs were saved on Windows and phone cleanup was attempted.' + "`r`n" +
                 'Use the offline analysis report before recompiling.' + "`r`n" +
                 'This context is NOT validated and publication remains locked.'),
                'Omi S26 HTP executed - numerical FAIL',
                [System.Windows.Forms.MessageBoxButtons]::OK,
                [System.Windows.Forms.MessageBoxIcon]::Warning) | Out-Null
            return
        }
        $gatePython = Get-LocalBuilderPython
        $receipt = Invoke-ProcessCapture -FilePath $gatePython -Arguments @($script:OmiGatePath,'record-htp','--folder',$runFolder,'--comparison',$result.ComparisonReport,'--serial',$script:DeviceSerial) -TimeoutSeconds 300
        if ($receipt.ExitCode -ne 0) { throw ('HTP PASS could not be recorded: ' + $receipt.StdErr + $receipt.StdOut) }
        Set-Status $lblOmiHtpTest 'S26 encoder HTP PASS (development harness)' 'good'
        Append-Log ('Omi S26 HTP proof saved: ' + (Join-Path $runFolder 'omi-encoder-htp-result.json'))
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Omi Med encoder passed the S26 HTP numerical comparison.`r`n`r`nNo additional Qualcomm compilation was needed.`r`n`r`nOnly the 12-second encoder is validated. The decoder, canonical NeMo parity, and long-audio support are still pending.",'Omi encoder HTP PASS',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})
$btnOmiPublish.Add_Click({
    Invoke-UiAction 'Checking Omi full-model publication requirements...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder)) { throw 'Cannot publish without validated Omi model outputs.' }
        $py = Get-LocalBuilderPython
        $gate = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiGatePath,'check-publish','--folder',$script:OmiRunFolder) -TimeoutSeconds 600
        if ($gate.ExitCode -ne 0) { throw ($gate.StdOut + [Environment]::NewLine + $gate.StdErr) }
        # A future full-graph Omi packager can unlock this gate after device
        # evidence has been verified. Keep the Omi folder separate from v3.
        $folder = Join-Path $script:OmiRunFolder 'deployment'
        if (-not (Test-Path -LiteralPath $folder -PathType Container)) { throw 'Qualified Omi deployment folder has not been generated.' }
        if ([string]::IsNullOrWhiteSpace($txtOmiRepo.Text) -or $txtOmiRepo.Text -notmatch '^[^/\s]+/[^/\s]+$') { throw 'Enter Hugging Face namespace/name.' }
        if ([string]::IsNullOrWhiteSpace($txtOmiToken.Text)) { throw 'Enter a Hugging Face write token.' }
        Register-SensitiveOutputValue $txtOmiToken.Text
        $hfPy = Ensure-HuggingFaceUploaderPython
        $audit = Invoke-ProcessCapture -FilePath $hfPy -Arguments @($script:HfDeployPath,'audit','--folder',$folder) -TimeoutSeconds 7200
        if ($audit.ExitCode -ne 0) { throw ('Omi deployment audit failed: ' + $audit.StdOut + $audit.StdErr) }
        $display = 'Hugging Face Omi upload [write token through protected process environment]'
        $upload = Invoke-ProcessCapture -FilePath $hfPy -Arguments @($script:HfDeployPath,'upload','--folder',$folder,'--repo-id',$txtOmiRepo.Text,'--revision','main','--visibility','private') -Environment @{ WR_HF_TOKEN=$txtOmiToken.Text } -TimeoutSeconds 21600 -DisplayCommand $display
        if ($upload.ExitCode -ne 0) { throw ($upload.StdOut + [Environment]::NewLine + $upload.StdErr) }
        $remote = Invoke-ProcessCapture -FilePath $hfPy -Arguments @($script:HfDeployPath,'verify-remote','--folder',$folder,'--repo-id',$txtOmiRepo.Text,'--revision','main') -Environment @{ WR_HF_TOKEN=$txtOmiToken.Text } -TimeoutSeconds 7200 -DisplayCommand 'Verify Omi Hugging Face remote [token hidden]'
        if ($remote.ExitCode -ne 0) { throw ($remote.StdOut + [Environment]::NewLine + $remote.StdErr) }
        Set-Status $lblOmiPublish 'Hugging Face remote verified' 'good'
        Save-Settings
    }
})
$btnOmiHfExperimental.Add_Click({
    Invoke-UiAction 'Deploying EXPERIMENTAL original Omi v2 S26 QNN model assets to Hugging Face...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder) -or -not (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) {
            throw 'Original Omi encoder workspace missing; restore the saved Step 3/4 run before deploying.'
        }
        $decoder = Get-OmiLatestTdtDesktopFolder
        $repoInput = ([string]$txtOmiRepo.Text).Trim()
        $repoSuggestedFromUsername = $false
        # The field is shared with Step 6. Step 7 alone accepts the user's
        # Hugging Face namespace and suggests a fixed, model-specific repo.
        # Never publish silently: the full destination is shown in a Yes/No
        # confirmation before any packaging or network upload starts.
        if ($repoInput -match '^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$' -and -not $repoInput.Contains('..') -and -not $repoInput.Contains('.git')) {
            $repoId = $repoInput + '/omi-med-stt-v1-snapdragon-sm8850-v81'
            $repoSuggestedFromUsername = $true
            $txtOmiRepo.Text = $repoId
            Append-Log ('[Omi HF] Username-only entry expanded for Step 7; proposed repository: ' + $repoId)
        } else {
            $repoId = $repoInput
        }
        if ($repoId -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]{0,95}/[A-Za-z0-9][A-Za-z0-9._-]{0,95}$' -or $repoId.Contains('..') -or $repoId.Contains('.git')) {
            throw 'Enter username/repository in Step 6 (e.g. drinnird/omi-med-stt-v1-snapdragon-sm8850-v81), or enter only your username and let Step 7 propose the Omi S26 name.'
        }
        $visibility = if ($chkOmiHfPublic.Checked) { 'public' } else { 'private' }
        $suggestionNotice = if ($repoSuggestedFromUsername) { "You entered only a username. The Builder suggested this model repository. Select No to edit it.`r`n`r`n" } else { '' }
        $prompt = "Publish an EXPERIMENTAL original Omi v2 QNN model package?`r`n`r`n$suggestionNotice" + "Repository: $repoId`r`nVisibility: $visibility`r`nDevice: SM-S942U / SM8850 / Hexagon V81`r`n`r`nThe S26 neural graphs ran, but the encoder FP16 numerical gate FAILED, canonical NeMo parity is unverified, and the Android runtime is NOT bundled.`r`n`r`nOnly model binaries, tokenizer, manifest, model card and license are uploaded. No recordings or local diagnostic logs.`r`n`r`nContinue?"
        $answer = [System.Windows.Forms.MessageBox]::Show($prompt,'Confirm experimental Omi S26 model deployment',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) {
            Append-Log 'Omi experimental Hugging Face deployment cancelled by user.'
            return
        }
        $replaceExisting = [bool]$chkOmiHfReplace.Checked
        if ($replaceExisting) {
            if (-not $repoId.EndsWith('/omi-med-stt-v1-snapdragon-sm8850-v81', [System.StringComparison]::Ordinal)) {
                throw 'Replacing remote contents is allowed only for the dedicated namespace/omi-med-stt-v1-snapdragon-sm8850-v81 model repository.'
            }
            $deletePrompt = "DELETE/REPLACE EVERY EXISTING MODEL REPOSITORY FILE?`r`n`r`nRepository: $repoId`r`n`r`nThe Builder will remove prior remote files when uploading the verified Omi package. Existing content may remain in Hugging Face commit history, but the latest revision will be replaced. This does NOT delete the repository itself and does NOT touch local files.`r`n`r`nThis cannot be undone from the Builder. Continue with replacement?"
            $deleteAnswer = [System.Windows.Forms.MessageBox]::Show($deletePrompt,'CONFIRM REMOTE FILE REPLACEMENT',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
            if ($deleteAnswer -ne [System.Windows.Forms.DialogResult]::Yes) {
                Append-Log 'Omi Hugging Face repository replacement cancelled by user.'
                return
            }
            Append-Log ('[Omi HF] Explicit remote file replacement confirmed for: ' + $repoId)
        }
        $nemo = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        if (-not (Test-Path -LiteralPath $nemo -PathType Leaf)) { throw 'The original Omi model download is missing; Step 2 checkpoint is required to package its tokenizer.' }
        $folder = Join-Path $script:OmiRunFolder 'experimental-hf-sm8850-v81'
        $py = Get-LocalBuilderPython
        Append-Log '[Omi HF] Hash-verifying original three compiled QNN contexts and staging model-only files...'
        $prepared = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiExperimentalHfPath,'prepare','--encoder-folder',$script:OmiRunFolder,'--decoder-folder',$decoder,'--nemo',$nemo,'--output-dir',$folder) -TimeoutSeconds 7200
        if ($prepared.ExitCode -ne 0) { throw ('Omi experimental package verification failed; nothing uploaded: ' + $prepared.StdErr + [Environment]::NewLine + $prepared.StdOut) }
        $audit = Invoke-ProcessCapture -FilePath $py -Arguments @($script:OmiExperimentalHfPath,'audit','--folder',$folder) -TimeoutSeconds 7200
        if ($audit.ExitCode -ne 0) { throw ('Experimental Omi package integrity audit failed: ' + $audit.StdErr + [Environment]::NewLine + $audit.StdOut) }
        # Reuse whichever token the user already entered. The HF package can also
        # use the existing standard Hugging Face login cache if both boxes are empty.
        $hfToken = ([string]$txtHfToken.Text).Trim()
        if (-not $hfToken) { $hfToken = ([string]$txtOmiToken.Text).Trim() }
        $privateEnvironment = @{}
        if ($hfToken) {
            Register-SensitiveOutputValue $hfToken
            $privateEnvironment['WR_HF_TOKEN'] = $hfToken
        }
        $hfPy = Ensure-HuggingFaceUploaderPython
        Append-Log ('[Omi HF] Uploading verified EXPERIMENTAL original Omi v2 S26 assets to ' + $repoId + ' (' + $visibility + '). This may upload ~1.24 GB.')
        $deployArgs = @($script:OmiExperimentalHfPath,'deploy','--folder',$folder,'--repo-id',$repoId,'--visibility',$visibility)
        if ($replaceExisting) { $deployArgs += '--replace-existing' }
        $uploaded = Invoke-ProcessCapture -FilePath $hfPy -Arguments $deployArgs -Environment $privateEnvironment -TimeoutSeconds 21600 -DisplayCommand ('Omi S26 Hugging Face experimental upload [credentials hidden]')
        if ($uploaded.ExitCode -ne 0) { throw ('Omi Hugging Face upload/remote verification did not complete: ' + $uploaded.StdErr + [Environment]::NewLine + $uploaded.StdOut) }
        $receipt = Join-Path $script:OmiRunFolder 'omi-hf-experimental-deployment.json'
        if (-not (Test-Path -LiteralPath $receipt -PathType Leaf)) { throw 'Hugging Face upload returned without a final verified deployment receipt.' }
        $proof = Get-Content -LiteralPath $receipt -Raw | ConvertFrom-Json
        if ([string]$proof.status -ne 'experimental_omi_model_uploaded_remote_files_checked' -or [string]$proof.repo_id -ne $repoId -or [bool]$proof.production_qualified -or [bool]$proof.publishable) {
            throw 'The experimental Hugging Face deployment receipt did not validate.'
        }
        $weak = @($proof.verification.PSObject.Properties | Where-Object { [string]$_.Value -eq 'size_only_no_content_hash_from_hub' })
        if ($weak.Count -gt 0) {
            Set-Status $lblOmiHfExperimental 'Uploaded; remote large file size checked (SHA readback unavailable)' 'warn'
        } else {
            Set-Status $lblOmiHfExperimental 'Experimental Omi model uploaded and remotely checked' 'good'
        }
        Append-Log ('[Omi HF] Experimental model repository: ' + [string]$proof.repo_url)
        Append-Log ('[Omi HF] Immutable commit: ' + [string]$proof.commit_sha)
        Append-Log ('[Omi HF] Receipt: ' + $receipt)
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show(("Experimental Omi v2 S26 model published.`r`n`r`n" + [string]$proof.repo_url + "`r`n`r`nCommit: " + [string]$proof.commit_sha + "`r`n`r`nNot a production-qualified or clinical ASR package."),'Experimental Omi Hugging Face deployment',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnOmiQnnUtilityBrowse.Add_Click({
    Choose-Exe -TextBox $txtOmiQnnContextUtility -Title 'Select official Qualcomm qnn-context-binary-utility.exe (Windows x64)'
})

$btnOmiQnnExportInfo.Add_Click({
    Invoke-UiAction 'Exporting actual Omi SM8850/V81 QNN graph metadata...' {
        if ([string]::IsNullOrWhiteSpace($script:OmiRunFolder) -or -not (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) {
            throw 'The verified Omi encoder workspace is missing. Restart the Builder with the original workspace preserved.'
        }
        $decoder = Get-OmiLatestTdtDesktopFolder
        $utility = ([string]$txtOmiQnnContextUtility.Text).Trim()
        if (-not $utility) {
            foreach ($sdkRoot in @([Environment]::GetEnvironmentVariable('QNN_SDK_ROOT'),[Environment]::GetEnvironmentVariable('QAIRT_SDK_ROOT'))) {
                if ($sdkRoot) {
                    $candidate = Join-Path $sdkRoot 'bin\x86_64-windows-msvc\qnn-context-binary-utility.exe'
                    if (Test-Path -LiteralPath $candidate -PathType Leaf) { $utility = $candidate; break }
                }
            }
        }
        if (-not $utility -or -not (Test-Path -LiteralPath $utility -PathType Leaf)) {
            throw 'Step 8 needs the official Qualcomm QAIRT Windows x64 qnn-context-binary-utility.exe. The original Builder used cloud compilation and does not install this local SDK tool. Download/install the official Windows QAIRT SDK and select its bin\x86_64-windows-msvc\qnn-context-binary-utility.exe using Browse. No model recompilation is needed.'
        }
        if ([IO.Path]::GetFileName($utility) -ne 'qnn-context-binary-utility.exe') {
            throw 'Please select the official Qualcomm qnn-context-binary-utility.exe, not qnn-net-run or a different executable.'
        }
        $outDir = Join-Path $script:OmiRunFolder 'omi-qnn-context-info'
        $python = Get-LocalBuilderPython
        Append-Log '[Omi QNN] Extracting true context graph names, shapes and data types with Qualcomm utility. May take several minutes on the 1.2 GB encoder.'
        $args = @($script:OmiQnnContextInfoPath,'--encoder-folder',$script:OmiRunFolder,'--decoder-folder',$decoder,'--output-dir',$outDir,'--utility',$utility)
        $res = Invoke-ProcessCapture -FilePath $python -Arguments $k2Argv -TimeoutSeconds 3600 -DisplayCommand 'Extract Omi QNN context graph metadata [official host utility]'
        if ($res.ExitCode -ne 0) {
            Set-Status $lblOmiQnnExportInfo 'Metadata extraction did not complete' 'bad'
            throw ('No authentic three-graph metadata set was produced: ' + $res.StdErr + [Environment]::NewLine + $res.StdOut)
        }
        foreach ($name in @('encoder-info.json','predictor-info.json','joint-info.json','qnn-sdk-and-compile-provenance.json')) {
            if (-not (Test-Path -LiteralPath (Join-Path $outDir $name) -PathType Leaf)) { throw ('Missing required QNN metadata output: ' + $name) }
        }
        Set-Status $lblOmiQnnExportInfo 'Actual 3-graph metadata extracted' 'good'
        Append-Log ('[Omi QNN] QNN metadata written to ' + $outDir)
        $txtDeployResult.Text = 'Actual QNN context metadata exported:`r`n' + $outDir + "`r`nIncludes encoder-info.json, predictor-info.json, joint-info.json, SDK/compile provenance.`r`nNo model rebuild or HF upload."
        [System.Windows.Forms.MessageBox]::Show(('Four QNN metadata JSON files are ready for your Android developer:`r`n`r`n' + $outDir),'Omi QNN compatibility metadata exported',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        Open-Folder $outDir
    }
})

$btnOmiAndroidOrtProbe.Add_Click({
    try {
        $bundle = Join-Path $Root 'tools\Omi-v2-ORT-QNN-Debug-Probe-Source-Only.zip'
        $fullSource = Join-Path $Root 'tools\WhisperRecorder-v6.5.14-Omi-Joint-ORT-QNN-Debug-Source.zip'
        foreach ($source in @($bundle,$fullSource)) {
            if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
                throw ('SDK-free debug probe source not found in Builder: ' + $source)
            }
        }
        $dest = Join-Path $script:OmiWorkspace 'android-ort-qnn-joint-probe'
        New-Item -ItemType Directory -Path $dest -Force | Out-Null
        Copy-Item -LiteralPath $bundle -Destination (Join-Path $dest 'Omi-v2-ORT-QNN-Debug-Probe-Source-Only.zip') -Force
        Copy-Item -LiteralPath $fullSource -Destination (Join-Path $dest 'WhisperRecorder-v6.5.14-Omi-Joint-ORT-QNN-Debug-Source.zip') -Force
        $notes = @'
WhisperRecorder / original Omi v2 joint.bin ORT-QNN compatibility probe

NO Qualcomm QAIRT SDK / desktop QNN context utility required for this test.

1. Choose the changed-files-only ZIP to merge just TWO debug-only files into
   your existing WhisperRecorder Android source; OR use the full Android source
   ZIP based on the uploaded v6.5.14 project. Keep working Parakeet files intact.
2. Build an Android DEBUG APK using your existing Android Studio or GitHub
   Actions/Gradle environment (NOT by this Windows NPU Builder). The debug APK
   uses the QNN runtime Maven dependencies already declared in that project.
3. Install DEBUG APK on Galaxy S26; open extra launcher icon 'Omi QNN Joint Probe'.
4. Download original joint.bin (3,556,368 bytes) from your Hugging Face Omi repo
   onto S26. Tap 'Choose joint.bin', select the file, then share the JSON report.
5. Send the report to the other chat to determine whether ORT-QNN works.

The test uses ORIGINAL QNN compiled joint.bin, a provisional ONNX source graph
contract and an ONNX EPContext wrapper, with CPU fallback disabled. A successful
session plus inference tests REAL on-device ORT-QNN loading. Graph names and
QNN data types are NOT verified by the Qualcomm context-binary utility.

No standalone production Android runtime is modified or compiled by Builder.
No decoder, encoder, or NeMo accuracy gate is changed; this is experimental.
'@
        Set-Content -LiteralPath (Join-Path $dest 'START-HERE.txt') -Value $notes -Encoding UTF8
        Set-Status $lblOmiAndroidOrtProbe 'SDK-free debug Android probe exported' 'good'
        Append-Log ('[Omi ORT] SDK-free Android probe source available: ' + $dest)
        [System.Windows.Forms.MessageBox]::Show(("SDK-free Android ORT-QNN joint probe source exported.\r\n\r\n" + $dest + "\r\n\r\nBuild DEBUG APK using your Android workflow; select original joint.bin on S26. Qualcomm SDK is NOT needed for the loading test; official QNN metadata is NOT extracted."),'Omi S26 ORT-QNN debug probe',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        Open-Folder $dest
    } catch { Set-Status $lblOmiAndroidOrtProbe 'Probe export failed' 'bad'; Show-Error $_ }
})

# Navigate directly to the latest COMPLETED Step 10 JSON/TXT evidence.
# A completed report is required: never open a failed/incomplete device-run folder.
# Works after app restart even if the last result path was not saved in user-settings.json.
function Get-LatestOmiStep10ResultsFolder {
    $reportName = 'omi-fp32-onnx-vs-s26-npu-parity.json'
    $saved = [string]$script:LastOmiParityOutputFolder
    if ($saved -and (Test-Path -LiteralPath (Join-Path $saved $reportName) -PathType Leaf)) {
        return $saved
    }
    $runs = New-Object 'System.Collections.Generic.List[string]'
    if ($script:OmiRunFolder -and (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) {
        [void]$runs.Add($script:OmiRunFolder)
    }
    if (Test-Path -LiteralPath $script:OmiWorkspace -PathType Container) {
        foreach ($entry in @(Get-ChildItem -LiteralPath $script:OmiWorkspace -Directory -Filter 'run-*' -ErrorAction SilentlyContinue)) {
            if (-not $runs.Contains($entry.FullName)) { [void]$runs.Add($entry.FullName) }
        }
    }
    $latest = $null
    foreach ($run in $runs) {
        foreach ($candidate in @(Get-ChildItem -LiteralPath $run -Directory -Filter 'omi-fp32-s26-parity-*' -ErrorAction SilentlyContinue)) {
            foreach ($folder in @($candidate.FullName,(Join-Path $candidate.FullName 'parity-report'))) {
                $report = Join-Path $folder $reportName
                if (-not (Test-Path -LiteralPath $report -PathType Leaf)) { continue }
                $info = Get-Item -LiteralPath $report -ErrorAction SilentlyContinue
                if ($null -ne $info -and ($null -eq $latest -or $info.LastWriteTimeUtc -gt $latest.LastWriteTimeUtc)) {
                    $latest = $info
                }
            }
        }
    }
    if ($null -eq $latest) { return $null }
    $script:LastOmiParityOutputFolder = [string]$latest.DirectoryName
    return [string]$latest.DirectoryName
}

$btnOmiParityOpenResults.Add_Click({
    try {
        $folder = Get-LatestOmiStep10ResultsFolder
        if (-not $folder) {
            Set-Status $lblOmiParityResults 'No completed Step 10 report found' 'warn'
            [System.Windows.Forms.MessageBox]::Show(
                'No completed Step 10 result was found in your Omi workspace. Run Step 10 or analyze an existing Step 5I report first.',
                'Step 10 results folder',[System.Windows.Forms.MessageBoxButtons]::OK,
                [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
            return
        }
        Open-Folder $folder
        Set-Status $lblOmiParityResults 'Opened latest Step 10 report folder' 'good'
    } catch {
        Set-Status $lblOmiParityResults 'Could not open results folder' 'bad'
        Show-Error $_
    }
})

# Step 10 runs without any model export or compilation. In either mode,
# the existing Step 6 production publishing gate remains untouched.
function Invoke-OmiFp32ParityReport {
    param([string]$S26Report,[string]$Destination,[string]$PythonExe)
    if (-not (Test-Path -LiteralPath $S26Report -PathType Leaf)) { throw 'Select a completed Step 5H/5I report. The source receipt was not found.' }
    $args = @($script:OmiFp32S26ParityPath,'--s26-report',$S26Report,'--output-dir',$Destination)
    $result = Invoke-ProcessCapture -FilePath $PythonExe -Arguments $args -TimeoutSeconds 180 -DisplayCommand 'Analyze ONNX FP32 vs real S26 FP16 QNN evidence'
    if ($result.ExitCode -ne 0) { throw ('FP32/S26 parity analysis failed: ' + $result.StdErr + [Environment]::NewLine + $result.StdOut) }
    $path = Join-Path $Destination 'omi-fp32-onnx-vs-s26-npu-parity.json'
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'No completed FP32/S26 parity report was saved.' }
    $proof = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    if ([string]$proof.status -notin @('experimental_normalized_word_parity_observed','experimental_encoder_isolated_and_full_word_parity_observed','experimental_transcript_mismatch_review_required') -or
        [bool]$proof.production_qualified -or [bool]$proof.publishable -or [bool]$proof.original_nemo_gpu_parity_proven -or
        [int]$proof.window_count -lt 1 -or [int]$proof.window_count -gt 3) {
        throw 'Unexpected parity status; no output will be treated as qualification.'
    }
    $lines = New-Object 'System.Collections.Generic.List[string]'
    foreach ($item in @($proof.windows)) {
        $lines.Add(('Window {0} / start {1}s: normalized words same = {2}' -f $item.index,$item.start_seconds,$item.same_normalized_words))
        $lines.Add(('  FP32 ONNX: ' + $item.cpu_fp32_onnx_transcript))
        $lines.Add(('  S26 QNN:   ' + $item.s26_fp16_qnn_transcript))
        if ([bool]$item.encoder_isolation_performed) {
            $lines.Add(('  S26 encoder + same CPU ONNX decoder: ' + $item.s26_encoder_fp16_plus_same_cpu_fp32_decoder_transcript))
        } else { $lines.Add('  Encoder-only same-decoder test unavailable in old Step 5I report.') }
    }
    $txtDeployResult.Text = "OMI V2 EXPERIMENTAL PARITY (NOT NEMO GPU)`r`n`r`n" + ($lines -join "`r`n") +
        "`r`n`r`nAll normalized words same: " + [string]$proof.all_windows_normalized_word_match +
        "`r`nEncoder isolated test completed: " + [string]$proof.encoder_isolation_performed_all_windows +
        "`r`nEncoder isolated words same: " + [string]$proof.encoder_isolated_all_windows_normalized_word_match +
        "`r`nFP16 numerical gate (all): " + [string]$proof.encoder_provisional_numerical_gate_all_pass +
        "`r`n`r`nReport: " + $path +
        "`r`n`r`nNo production, NeMo, clinical or continuous audio qualification."
    $script:LastDeployOutputFolder = $Destination
    $script:LastOmiParityOutputFolder = $Destination
    Save-Settings
    Set-Status $lblOmiParityResults 'Latest Step 10 report folder saved' 'good'
    Set-Status $lblOmiParity $(if ([bool]$proof.experimental_parity_conditions_met) { 'Experimental matched words; not production-qualified' } else { 'Transcript differences; review report' }) 'warn'
    Append-Log ('[Omi Step 10] Word parity: ' + $path)
    [System.Windows.Forms.MessageBox]::Show(("Omi v2 FP32 ONNX / S26 NPU comparison saved.`r`n`r`nNormalized words match: " +
        [string]$proof.all_windows_normalized_word_match + "`r`nWindows: " + [string]$proof.window_count +
        "`r`n`r`n" + $path + "`r`n`r`nNumerical FP16 gate remains unchanged. Not canonical NeMo or clinical parity."),
        'Omi experimental transcription parity',[System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    Open-Folder $Destination
}

$btnOmiParitySaved.Add_Click({
    Invoke-UiAction 'Reviewing existing Omi FP32 ONNX CPU vs S26 NPU word parity (offline)...' {
        $source = ([string]$txtOmiCompareS26Json.Text).Trim()
        if (-not (Test-OmiStep5JSourceReport -Path $source)) {
            Restore-OmiStep5JSource
            $source = ([string]$txtOmiCompareS26Json.Text).Trim()
        }
        if (-not (Test-OmiStep5JSourceReport -Path $source)) { throw 'No completed Step 5H/5I report. Run Step 5I first, or browse for its JSON in Step 5J.' }
        if (-not $script:OmiRunFolder) { throw 'The Omi workspace is not selected.' }
        $out = Join-Path $script:OmiRunFolder ('omi-fp32-s26-parity-review-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        Invoke-OmiFp32ParityReport -S26Report $source -Destination $out -PythonExe (Get-LocalBuilderPython)
    }
})

$btnOmiParityFresh.Add_Click({
    Invoke-UiAction 'Running fresh 1-3 window Omi FP32 ONNX vs S26 FP16 QNN transcription comparison...' {
        if (-not $script:OmiRunFolder -or -not (Test-Path -LiteralPath $script:OmiRunFolder -PathType Container)) { throw 'Complete Omi Steps 3-5F, preserving the original workspace.' }
        $decoder = Get-OmiLatestTdtDesktopFolder
        $audio = ([string]$txtOmiTranscriptAudio.Text).Trim()
        if (-not $audio) { $audio = ([string]$txtOmiAudio.Text).Trim() }
        if (-not (Test-Path -LiteralPath $audio -PathType Leaf) -or [IO.Path]::GetExtension($audio).ToLowerInvariant() -notin @('.wav','.ogg')) {
            throw 'Select your non-sensitive matching .wav/.ogg recording in Step 5G.'
        }
        $start = 0
        $count = 0
        if (-not [int]::TryParse(([string]$txtOmiMultiStart.Text).Trim(),[ref]$start) -or $start -lt 0 -or $start -gt 7200) { throw 'Step 5I start time must be an integer from 0 to 7200.' }
        if (-not [int]::TryParse(([string]$txtOmiMultiCount.Text).Trim(),[ref]$count) -or $count -lt 1 -or $count -gt 3) { throw 'Choose 1-3 12-second windows in Step 5I.' }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('S26 QNN runtime tools missing: ' + ($runtime.Missing -join ', ')) }
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'Connect your authorized Galaxy S26 and refresh the selected device.' }
        $device = Get-DeviceInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        if (-not $device.IsTargetDevice) { throw 'Selected ADB device is not the validated S26 target.' }
        $python = Ensure-OmiTdtTranscriptEnvironmentFromUi
        if (-not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
            $ff = Install-FfmpegFromUi
            if (-not (Test-Path -LiteralPath $ff -PathType Leaf)) { throw 'FFmpeg required for the selected audio.' }
        }
        $nemo = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        if (-not (Test-Path -LiteralPath $nemo -PathType Leaf)) { throw 'Original Omi .nemo checkpoint not found; preserve Step 2 model files.' }
        $out = Join-Path $script:OmiRunFolder ('omi-fp32-s26-parity-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        $deviceRun = Join-Path $out 'device-run'
        New-Item -Path $deviceRun -ItemType Directory -Force | Out-Null
        $bundle = [ordered]@{}
        foreach ($entry in $runtime.Files.GetEnumerator()) { $bundle[[string]$entry.Key] = [string]$entry.Value }
        $runtimeManifest = Join-Path $deviceRun 'qnn-runtime-local-paths.json'
        $utf8NoBom = New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false
        [System.IO.File]::WriteAllText($runtimeManifest,($bundle | ConvertTo-Json -Depth 4),$utf8NoBom)
        Append-Log ('[Omi Step 10] Running exact existing Step 5I engine; 12s windows=' + $count + '. This can take many minutes.')
        $args = @($script:OmiTdtS26TranscribePath,'multiwindow','--encoder-folder',$script:OmiRunFolder,
            '--decoder-folder',$decoder,'--cache-dir',(Join-Path $script:OmiModelDir 'native-extracted'),
            '--nemo',$nemo,'--audio',$audio,'--ffmpeg',$txtFfmpeg.Text,
            '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--runtime-manifest',$runtimeManifest,
            '--output-dir',$deviceRun,'--start-seconds',([string]$start),'--window-count',([string]$count),
            '--auto-clean','true')
        $deviceCleanupSucceeded = $false
        try {
            $run = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 7200 -DisplayCommand 'Omi Step 10 real S26 1-3 window QNN + desktop ONNX'
        } finally {
            # The Python runner cleans its own staging directory. After any
            # exception/timeout, independently remove ALL allowlisted orphaned
            # Omi test sessions and verify the device is clean.
            try {
                $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                $deviceCleanupSucceeded = [bool]$clean.VerifiedClean
                Append-Log ('Step 10 device staging cleanup verified: ' + $clean.RemovedCount + ' leftover folders removed')
                Set-Status $lblPhoneStorage 'S26 Builder test staging cleared' 'good'
            } catch {
                Append-Log ('Step 10 device cleanup FAILED - run Clear previous S26 test files: ' + $_.Exception.Message)
                Set-Status $lblPhoneStorage 'S26 cleanup failed; use Clear previous S26 test files' 'bad'
                $script:OmiDeviceCleanupFailed = $true
            }
        }
        if (-not $deviceCleanupSucceeded) { throw 'S26 test cleanup could not be verified; reconnect phone and click Clear previous S26 test files. No qualification result will be accepted.' }
        if ($run.ExitCode -ne 0) { throw ('Fresh S26 comparison failed or was interrupted; no parity qualification: ' + $run.StdErr + [Environment]::NewLine + $run.StdOut) }
        $receipt = Join-Path $deviceRun 'omi-tdt-s26-multiwindow-diagnostic.json'
        if (-not (Test-OmiStep5JSourceReport -Path $receipt)) { throw 'S26 multi-window evidence is incomplete. Parity report will not be generated.' }
        Set-OmiStep5JSourceFromCompletedRun -Path $receipt
        Invoke-OmiFp32ParityReport -S26Report $receipt -Destination (Join-Path $out 'parity-report') -PythonExe $python
    }
})

$btnOmiFolder.Add_Click({
    try { New-Item -ItemType Directory -Path $script:OmiWorkspace -Force | Out-Null; Open-Folder $script:OmiWorkspace } catch { Show-Error $_ }
})

# -------------------- Deploy / Test --------------------
$tabDeploy = New-Object System.Windows.Forms.TabPage
$tabDeploy.Text = 'Deploy & Test'
$tabDeploy.AutoScroll = $true
[void]$tabs.TabPages.Add($tabDeploy)

$tabDeploy.Controls.Add((New-Label 'Galaxy S26 HTP deployment + validation' 25 14 360 28 -Bold))
$lblDeployIntro = New-Label 'This page reuses downloaded SM8850/V81 QNN context binaries. It supports both the tiny HTP proof and the Parakeet FastConformer encoder, stages the matching reference tensors automatically, runs qnn-net-run, and compares device outputs with the Windows reference.' 25 43 865 56
$tabDeploy.Controls.Add($lblDeployIntro)

$tabDeploy.Controls.Add((New-Label 'STEP 1 - Select a compiled QNN context' 25 105 380 24 -Bold))
$cmbDeployArtifact = New-Object System.Windows.Forms.ComboBox
$cmbDeployArtifact.DropDownStyle = 'DropDownList'
$cmbDeployArtifact.DisplayMember = 'DisplayName'
$cmbDeployArtifact.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,135)
$cmbDeployArtifact.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(570,25)
$tabDeploy.Controls.Add($cmbDeployArtifact)
$btnDeployRefreshArtifacts = New-Button 'Rescan workspaces' 610 132 150 32
$tabDeploy.Controls.Add($btnDeployRefreshArtifacts)
$btnDeployBrowseArtifact = New-Button 'Browse .bin...' 770 132 120 32
$tabDeploy.Controls.Add($btnDeployBrowseArtifact)
$lblDeployArtifact = New-StatusLabel 25 170 865
$tabDeploy.Controls.Add($lblDeployArtifact)

$tabDeploy.Controls.Add((New-Label 'STEP 2 - Verify the connected phone' 25 205 360 24 -Bold))
$btnDeployRefreshPhone = New-Button 'Refresh phone' 25 235 135 32
$tabDeploy.Controls.Add($btnDeployRefreshPhone)
$lblDeployDevice = New-StatusLabel 175 240 715
$tabDeploy.Controls.Add($lblDeployDevice)

$tabDeploy.Controls.Add((New-Label 'STEP 3 - Android QNN runtime runner' 25 282 360 24 -Bold))
$txtRuntimeRoot = New-TextBox 25 312 560 25
$txtRuntimeRoot.Text = Join-Path $Root 'tools\qnn-android-runtime'
$tabDeploy.Controls.Add($txtRuntimeRoot)
$btnRuntimeBrowse = New-Button 'Browse...' 595 309 90 31
$tabDeploy.Controls.Add($btnRuntimeBrowse)
$btnRuntimeCheck = New-Button 'Check runtime' 695 309 105 31
$tabDeploy.Controls.Add($btnRuntimeCheck)
$btnRuntimeDocs = New-Button 'Runtime docs' 810 309 80 31
$tabDeploy.Controls.Add($btnRuntimeDocs)
$lblRuntimeStatus = New-StatusLabel 25 347 865
$tabDeploy.Controls.Add($lblRuntimeStatus)
$lblRuntimeGuide = New-Label 'For device validation, the Builder reuses your existing Android qnn-net-run plus HTP V81 runtime files. It does not copy arbitrary Samsung /vendor libraries. The same runtime bundle is used for the tiny proof and the Parakeet encoder test.' 25 374 865 52
$lblRuntimeGuide.ForeColor = [System.Drawing.Color]::DimGray
$tabDeploy.Controls.Add($lblRuntimeGuide)

$tabDeploy.Controls.Add((New-Label 'STEP 4 - Deploy and execute' 25 435 360 24 -Bold))
$btnStageProof = New-Button 'Verify staging' 25 465 145 34
$tabDeploy.Controls.Add($btnStageProof)
$btnRunProof = New-Button 'Run HTP test' 185 465 145 34
$tabDeploy.Controls.Add($btnRunProof)
$btnRecompareProof = New-Button 'Recompare latest' 345 465 145 34
$tabDeploy.Controls.Add($btnRecompareProof)
$btnOpenDeployOutput = New-Button 'Open test output' 505 465 145 34
$tabDeploy.Controls.Add($btnOpenDeployOutput)
$btnCleanPhone = New-Button 'Clean phone files' 665 465 145 34
$tabDeploy.Controls.Add($btnCleanPhone)

$chkAutoCleanPhone = New-Object System.Windows.Forms.CheckBox
$chkAutoCleanPhone.Text = 'Automatic phone cleanup after every test (required)'
$chkAutoCleanPhone.Checked = $true
$chkAutoCleanPhone.Enabled = $false
$chkAutoCleanPhone.AutoSize = $true
$chkAutoCleanPhone.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,510)
$tabDeploy.Controls.Add($chkAutoCleanPhone)
$lblPhoneStorage = New-StatusLabel 350 507 540
$tabDeploy.Controls.Add($lblPhoneStorage)
Set-Status $lblPhoneStorage 'All Builder phone test files are removed after each test. Interrupted runs: use Clear previous S26 test files.' 'good'

$lblDeployRun = New-StatusLabel 25 542 865
$tabDeploy.Controls.Add($lblDeployRun)

$txtDeployResult = New-Object System.Windows.Forms.TextBox
$txtDeployResult.Multiline = $true
$txtDeployResult.ReadOnly = $true
$txtDeployResult.ScrollBars = 'Vertical'
$txtDeployResult.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$txtDeployResult.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,575)
$txtDeployResult.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,125)
$txtDeployResult.Anchor = 'Top,Left,Right'
$tabDeploy.Controls.Add($txtDeployResult)

$tabDeploy.Controls.Add((New-Label 'STEP 5 - Separate-context runtime baseline' 25 720 430 24 -Bold))
$btnRuntimeBaseline = New-Button 'Benchmark 4 contexts' 25 750 180 34
$tabDeploy.Controls.Add($btnRuntimeBaseline)
$lblRuntimeBaseline = New-StatusLabel 220 756 670
$tabDeploy.Controls.Add($lblRuntimeBaseline)
Set-Status $lblRuntimeBaseline 'Requires all four HTP PASS contexts + controller PASS. No AI Hub recompilation.' 'warn'
$lblRuntimeBaselineGuide = New-Label 'Development baseline only: repeated qnn-net-run inputs measure a conservative burst-amortized warm upper bound and estimate fixed context/process overhead. Results include ADB/process/raw-file overhead and do not replace the in-process Android runtime bake-off.' 25 790 865 46
$lblRuntimeBaselineGuide.ForeColor = [System.Drawing.Color]::DimGray
$tabDeploy.Controls.Add($lblRuntimeBaselineGuide)
$txtRuntimeBaselineResult = New-Object System.Windows.Forms.TextBox
$txtRuntimeBaselineResult.Multiline = $true
$txtRuntimeBaselineResult.ReadOnly = $true
$txtRuntimeBaselineResult.ScrollBars = 'Vertical'
$txtRuntimeBaselineResult.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$txtRuntimeBaselineResult.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,842)
$txtRuntimeBaselineResult.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,170)
$txtRuntimeBaselineResult.Anchor = 'Top,Left,Right'
$tabDeploy.Controls.Add($txtRuntimeBaselineResult)

$tabDeploy.Controls.Add((New-Label 'STEP 6 - Linked decoder context candidate' 25 1035 430 24 -Bold))
$btnBuildLinkedDecoder = New-Button 'Build linked decoder' 25 1065 180 34
$tabDeploy.Controls.Add($btnBuildLinkedDecoder)
$btnValidateLinkedDecoder = New-Button 'Validate + benchmark' 220 1065 180 34
$tabDeploy.Controls.Add($btnValidateLinkedDecoder)
$lblLinkedDecoder = New-StatusLabel 415 1071 475
$tabDeploy.Controls.Add($lblLinkedDecoder)
Set-Status $lblLinkedDecoder 'Requires HTP PASS projector + predictor + joint. Encoder remains a separate context.' 'warn'
$lblLinkedDecoderGuide = New-Label 'AI Hub links three named QNN graphs (wr_projector / wr_predictor / wr_joint) into one decoder context. With the existing minimal six-file phone runtime, validation executes all three graphs together using three comma-separated qnn-net-run input-list descriptors, then checks each graph against its existing numeric reference. This does not fuse predictor state updates into joint calls.' 25 1105 865 50
$lblLinkedDecoderGuide.ForeColor = [System.Drawing.Color]::DimGray
$tabDeploy.Controls.Add($lblLinkedDecoderGuide)
$txtLinkedDecoderResult = New-Object System.Windows.Forms.TextBox
$txtLinkedDecoderResult.Multiline = $true
$txtLinkedDecoderResult.ReadOnly = $true
$txtLinkedDecoderResult.ScrollBars = 'Vertical'
$txtLinkedDecoderResult.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$txtLinkedDecoderResult.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,1165)
$txtLinkedDecoderResult.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,205)
$txtLinkedDecoderResult.Anchor = 'Top,Left,Right'
$tabDeploy.Controls.Add($txtLinkedDecoderResult)

$script:LastDeployOutputFolder = ''

function Get-SelectedDeployArtifact {
    if ($cmbDeployArtifact.SelectedIndex -lt 0) { return $null }
    return $cmbDeployArtifact.SelectedItem
}

function Test-RuntimeBaselineArtifactHasPass {
    param(
        [Parameter(Mandatory=$true)]$Artifact,
        [Parameter(Mandatory=$true)][string]$ExpectedKind
    )
    if ($null -eq $Artifact) { return $false }
    if ([string]::IsNullOrWhiteSpace([string]$Artifact.Workspace)) { return $false }
    $deviceRoot = Join-Path ([string]$Artifact.Workspace) 'device'
    if (-not (Test-Path -LiteralPath $deviceRoot -PathType Container)) { return $false }
    foreach ($comparisonFile in @(Get-ChildItem -LiteralPath $deviceRoot -Filter 'comparison.json' -File -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
        try {
            $comparison = Get-Content -LiteralPath $comparisonFile.FullName -Raw | ConvertFrom-Json
            $passProp = $comparison.PSObject.Properties['pass']
            $kindProp = $comparison.PSObject.Properties['kind']
            $backendProp = $comparison.PSObject.Properties['requested_backend']
            $cpuProp = $comparison.PSObject.Properties['cpu_backend_requested']
            if ($null -eq $passProp -or -not [bool]$passProp.Value) { continue }
            if ($null -eq $kindProp -or [string]$kindProp.Value -ne $ExpectedKind) { continue }
            if ($null -ne $backendProp -and [string]$backendProp.Value -notmatch 'libQnnHtp\.so') { continue }
            if ($null -ne $cpuProp -and [bool]$cpuProp.Value) { continue }
            return $true
        } catch {}
    }
    return $false
}

function Get-RuntimeBaselineArtifact {
    param(
        # An empty workspace is normal on first launch. Do not reject @() at
        # parameter binding time when Deploy & Test scans for saved contexts.
        [object[]]$Artifacts = @(),
        [Parameter(Mandatory=$true)][string]$Kind
    )
    if ($null -eq $Artifacts -or $Artifacts.Count -eq 0) { return $null }
    $candidates = @($Artifacts | Where-Object {
        $null -ne $_ -and
        [string]$_.Kind -eq $Kind -and
        $_.ReferencePath -and
        (Test-Path -LiteralPath $_.ReferencePath -PathType Leaf) -and
        (Test-Path -LiteralPath $_.Path -PathType Leaf)
    })
    foreach ($candidate in $candidates) {
        if (Test-RuntimeBaselineArtifactHasPass -Artifact $candidate -ExpectedKind $Kind) { return $candidate }
    }
    return $null
}


function Get-LinkedDecoderArtifact {
    $all = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
    return ($all | Where-Object {
        [string]$_.Kind -eq 'parakeet_tdt_decoder_linked' -and
        (Test-Path -LiteralPath $_.Path -PathType Leaf) -and
        (Test-Path -LiteralPath ($_.Path + '.aihub.json') -PathType Leaf)
    } | Sort-Object LastWriteTime -Descending | Select-Object -First 1)
}

function Get-ValidatedLinkedDecoderArtifactForPackage {
    param([Parameter(Mandatory=$true)][string]$PackageFolder)
    $validationPath = Join-Path $PackageFolder 'metadata\validation.json'
    if (-not (Test-Path -LiteralPath $validationPath -PathType Leaf)) { return $null }
    try {
        $validation = Get-Content -LiteralPath $validationPath -Raw | ConvertFrom-Json
        $runtimeProp = $validation.PSObject.Properties['runtime_linked_decoder_candidate']
        if ($null -eq $runtimeProp -or $null -eq $runtimeProp.Value) { return $null }
        $linkedProp = $runtimeProp.Value.PSObject.Properties['linked_context']
        if ($null -eq $linkedProp -or $null -eq $linkedProp.Value) { return $null }
        $shaProp = $linkedProp.Value.PSObject.Properties['sha256']
        if ($null -eq $shaProp -or [string]::IsNullOrWhiteSpace([string]$shaProp.Value)) { return $null }
        $expectedSha = ([string]$shaProp.Value).ToLowerInvariant()
        $all = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object {
            [string]$_.Kind -eq 'parakeet_tdt_decoder_linked' -and
            (Test-Path -LiteralPath $_.Path -PathType Leaf)
        } | Sort-Object LastWriteTime -Descending)
        foreach ($candidate in $all) {
            $actualSha = (Get-FileHash -Algorithm SHA256 -LiteralPath $candidate.Path).Hash.ToLowerInvariant()
            if ($actualSha -eq $expectedSha) { return $candidate }
        }
    } catch {
        Append-Log ('Could not bind the package to an exact linked-decoder artifact: ' + $_.Exception.Message)
    }
    return $null
}

function Populate-PackageArtifactsFromValidatedWorkspaces {
    param([switch]$Quiet)

    # Package fields are UI state, not source of truth. In-place upgrades or a
    # session that only visits Deploy & Test can legitimately leave them blank.
    # Recover the newest device-PASS individual context for each logical role.
    $all = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
    $roles = @(
        [pscustomobject]@{ Kind='parakeet_encoder'; Label='encoder'; Box=$txtCompiled },
        [pscustomobject]@{ Kind='parakeet_encoder_projector'; Label='projector'; Box=$txtProjectorCompiled },
        [pscustomobject]@{ Kind='parakeet_tdt_predictor'; Label='predictor'; Box=$txtPredictorCompiled },
        [pscustomobject]@{ Kind='parakeet_tdt_joint'; Label='joint'; Box=$txtJointCompiled }
    )
    $resolved = 0
    foreach ($role in $roles) {
        $current = [string]$role.Box.Text
        if (-not [string]::IsNullOrWhiteSpace($current) -and (Test-Path -LiteralPath $current -PathType Leaf)) {
            $resolved++
            continue
        }
        $candidate = Get-RuntimeBaselineArtifact -Artifacts $all -Kind ([string]$role.Kind)
        if ($null -ne $candidate) {
            $role.Box.Text = [string]$candidate.Path
            $resolved++
            if (-not $Quiet) { Append-Log ('Recovered package ' + [string]$role.Label + ' artifact from HTP PASS workspace: ' + [string]$candidate.Path) }
        }
    }
    return $resolved
}

function Test-FullNpuPackageFolder {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Container)) { return $false }
    $manifestPath = Join-Path $Path 'manifest.json'
    $checksumsPath = Join-Path $Path 'checksums.sha256'
    $validationPath = Join-Path $Path 'metadata\validation.json'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $checksumsPath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $validationPath -PathType Leaf)) { return $false }
    try {
        $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
        $schemaProp = $manifest.PSObject.Properties['schema']
        $versionProp = $manifest.PSObject.Properties['schema_version']
        $statusProp = $manifest.PSObject.Properties['package_status']
        $pendingProp = $manifest.PSObject.Properties['pending_graphs']
        if ($null -eq $schemaProp -or [string]$schemaProp.Value -ne 'whisperrecorder.android-npu-package') { return $false }
        if ($null -eq $versionProp -or [string]$versionProp.Value -ne '1-draft') { return $false }
        if ($null -eq $statusProp -or [string]$statusProp.Value -ne 'controller_validated_runtime_pending') { return $false }
        if ($null -ne $pendingProp -and $null -ne $pendingProp.Value -and @($pendingProp.Value).Count -ne 0) { return $false }

        $entries = @()
        $artifactsProp = $manifest.PSObject.Properties['artifacts']
        if ($null -ne $artifactsProp -and $null -ne $artifactsProp.Value) { $entries += @($artifactsProp.Value) }
        $assetsProp = $manifest.PSObject.Properties['assets']
        if ($null -ne $assetsProp -and $null -ne $assetsProp.Value) { $entries += @($assetsProp.Value) }
        if ($entries.Count -eq 0) { return $false }

        $artifactIds = @{}
        foreach ($entry in $entries) {
            if ($null -eq $entry) { return $false }
            $pathProp = $entry.PSObject.Properties['path']
            if ($null -eq $pathProp -or [string]::IsNullOrWhiteSpace([string]$pathProp.Value)) { return $false }
            $rel = ([string]$pathProp.Value).Replace('/', '\')
            $filePath = Join-Path $Path $rel
            if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) { return $false }
            $sizeProp = $entry.PSObject.Properties['size_bytes']
            if ($null -ne $sizeProp -and $null -ne $sizeProp.Value) {
                $actualSize = (Get-Item -LiteralPath $filePath).Length
                if ([int64]$sizeProp.Value -ne [int64]$actualSize) { return $false }
            }
            $idProp = $entry.PSObject.Properties['id']
            if ($null -ne $idProp -and -not [string]::IsNullOrWhiteSpace([string]$idProp.Value)) { $artifactIds[[string]$idProp.Value] = $true }
        }
        foreach ($requiredId in @('encoder_context','encoder_projector_context','predictor_context','predictor_embedding_table','joint_context')) {
            if (-not $artifactIds.ContainsKey($requiredId)) { return $false }
        }

        # A stripped support package intentionally keeps checksum entries for the
        # omitted large payloads. Require every checksum-listed file to exist so
        # Hugging Face auto-recovery can never select a stripped package.
        foreach ($line in @(Get-Content -LiteralPath $checksumsPath -ErrorAction Stop)) {
            if ([string]::IsNullOrWhiteSpace([string]$line)) { continue }
            if ([string]$line -notmatch '^[0-9a-fA-F]{64}  (.+)$') { return $false }
            $checksumRel = ([string]$Matches[1]).Replace('/', '\')
            if (-not (Test-Path -LiteralPath (Join-Path $Path $checksumRel) -PathType Leaf)) { return $false }
        }
        return $true
    } catch {
        return $false
    }
}

function Get-LatestFullNpuPackageFolder {
    param([string]$PreferredPath)

    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($PreferredPath)) {
        $candidates += [pscustomobject]@{ Path=$PreferredPath; LastWriteTime=[datetime]::MaxValue }
    }
    if ($null -ne $txtPackageFolder -and -not [string]::IsNullOrWhiteSpace([string]$txtPackageFolder.Text)) {
        $candidates += [pscustomobject]@{ Path=[string]$txtPackageFolder.Text; LastWriteTime=([datetime]::MaxValue).AddTicks(-1) }
    }

    $workspaceRoot = Join-Path $Root 'workspace'
    if (Test-Path -LiteralPath $workspaceRoot -PathType Container) {
        foreach ($manifestFile in @(Get-ChildItem -LiteralPath $workspaceRoot -Filter 'manifest.json' -File -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            $folder = $manifestFile.Directory.FullName
            $candidates += [pscustomobject]@{ Path=$folder; LastWriteTime=$manifestFile.LastWriteTime }
        }
    }

    $seen = @{}
    foreach ($candidate in @($candidates | Sort-Object LastWriteTime -Descending)) {
        $candidatePath = [string]$candidate.Path
        if ([string]::IsNullOrWhiteSpace($candidatePath)) { continue }
        $key = $candidatePath.ToLowerInvariant()
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        if (Test-FullNpuPackageFolder -Path $candidatePath) { return $candidatePath }
    }
    return ''
}

function Populate-HfPackageFolderFromValidatedPackage {
    param([switch]$Quiet)
    $current = [string]$txtHfPackageFolder.Text
    if (Test-FullNpuPackageFolder -Path $current) { return $current }
    $recovered = Get-LatestFullNpuPackageFolder -PreferredPath $current
    if (-not [string]::IsNullOrWhiteSpace($recovered)) {
        $txtHfPackageFolder.Text = $recovered
        if (-not $Quiet) { Append-Log ('Recovered complete unstripped v1-draft package for Hugging Face deployment: ' + $recovered) }
        return $recovered
    }
    return ''
}

function Update-DeployWorkflowState {
    $artifact = Get-SelectedDeployArtifact
    if ($null -ne $artifact -and (Test-Path -LiteralPath $artifact.Path -PathType Leaf)) {
        $hash = ''
        try { $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $artifact.Path).Hash.ToLowerInvariant() } catch {}
        $refText = if ($artifact.ReferencePath -and (Test-Path -LiteralPath $artifact.ReferencePath -PathType Leaf)) { 'reference found' } else { 'reference missing' }
        $kindText = if ($artifact.Kind -eq 'parakeet_tdt_decoder_linked') { 'Parakeet linked decoder context' } elseif ($artifact.Kind -eq 'parakeet_tdt_joint') { 'Parakeet TDT joint' } elseif ($artifact.Kind -eq 'parakeet_tdt_predictor') { 'Parakeet TDT predictor' } elseif ($artifact.Kind -eq 'parakeet_encoder_projector') { 'Parakeet encoder projector' } elseif ($artifact.Kind -eq 'parakeet_encoder') { 'Parakeet encoder' } elseif ($artifact.Kind -eq 'htp_proof') { 'HTP proof' } else { 'QNN context' }
        Set-Status $lblDeployArtifact ($kindText + ' | ' + (Split-Path -Leaf $artifact.Path) + ' | ' + $refText + $(if ($hash) { ' | SHA-256 ' + $hash.Substring(0,12) + '...' } else { '' })) $(if ($artifact.ReferencePath) { 'good' } else { 'warn' })
    } else {
        Set-Status $lblDeployArtifact 'No reusable context found. Build one on Model + AI Hub, or Browse .bin.' 'warn'
    }

    if ($script:DeviceSerial) {
        Set-Status $lblDeployDevice ('Connected: ' + $script:DeviceSerial + ' (expected SM-S942U)') 'good'
    } else {
        Set-Status $lblDeployDevice 'Phone not verified yet. Click Refresh phone.' 'warn'
    }

    $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
    if ($runtime.IsComplete) {
        Set-Status $lblRuntimeStatus 'Android QNN HTP runtime bundle ready (qnn-net-run + V81 runtime files found).' 'good'
    } else {
        Set-Status $lblRuntimeStatus ('Runtime runner not complete. Missing: ' + ($runtime.Missing -join ', ')) 'warn'
    }

    $artifactReady = ($null -ne $artifact) -and (Test-Path -LiteralPath $artifact.Path -PathType Leaf) -and $artifact.ReferencePath -and (Test-Path -LiteralPath $artifact.ReferencePath -PathType Leaf)
    $btnStageProof.Enabled = ($artifactReady -and -not [string]::IsNullOrWhiteSpace($script:DeviceSerial))
    $btnRunProof.Enabled = ($btnStageProof.Enabled -and $runtime.IsComplete)
    $btnRecompareProof.Enabled = $artifactReady
    $btnCleanPhone.Enabled = -not [string]::IsNullOrWhiteSpace($script:DeviceSerial)
    $allForBaseline = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
    $baselineKinds = @('parakeet_encoder','parakeet_encoder_projector','parakeet_tdt_predictor','parakeet_tdt_joint')
    $baselineReady = $true
    foreach ($bk in $baselineKinds) {
        $candidate = Get-RuntimeBaselineArtifact -Artifacts $allForBaseline -Kind $bk
        if ($null -eq $candidate) { $baselineReady = $false; break }
    }
    $btnRuntimeBaseline.Enabled = ($baselineReady -and -not [string]::IsNullOrWhiteSpace($script:DeviceSerial) -and $runtime.IsComplete)
    if ($btnRuntimeBaseline.Enabled) { Set-Status $lblRuntimeBaseline 'Ready to benchmark the four validated separate contexts.' 'good' }
    elseif (-not $baselineReady) { Set-Status $lblRuntimeBaseline 'Waiting for encoder + projector + predictor + joint contexts/references.' 'warn' }
    elseif ([string]::IsNullOrWhiteSpace($script:DeviceSerial)) { Set-Status $lblRuntimeBaseline 'Refresh the Galaxy S26 before benchmarking.' 'warn' }
    else { Set-Status $lblRuntimeBaseline 'Android QNN runtime bundle is incomplete.' 'warn' }
    $decoderReady = $true
    foreach ($dk in @('parakeet_encoder_projector','parakeet_tdt_predictor','parakeet_tdt_joint')) {
        $dc = Get-RuntimeBaselineArtifact -Artifacts $allForBaseline -Kind $dk
        if ($null -eq $dc -or [string]::IsNullOrWhiteSpace([string]$dc.SourceOnnx) -or -not (Test-Path -LiteralPath $dc.SourceOnnx)) { $decoderReady = $false; break }
    }
    $linkedArtifact = Get-LinkedDecoderArtifact
    $btnBuildLinkedDecoder.Enabled = $decoderReady
    $btnValidateLinkedDecoder.Enabled = ($null -ne $linkedArtifact -and -not [string]::IsNullOrWhiteSpace($script:DeviceSerial) -and $runtime.IsComplete)
    if ($btnValidateLinkedDecoder.Enabled) {
        Set-Status $lblLinkedDecoder ('Linked decoder ready: ' + (Split-Path -Leaf $linkedArtifact.Path)) 'good'
    } elseif ($null -ne $linkedArtifact) {
        Set-Status $lblLinkedDecoder 'Linked decoder exists; refresh phone / complete QNN runtime bundle to validate.' 'warn'
    } elseif ($decoderReady) {
        Set-Status $lblLinkedDecoder 'Ready to build one linked projector+predictor+joint context through AI Hub.' 'good'
    } else {
        Set-Status $lblLinkedDecoder 'Waiting for HTP PASS projector + predictor + joint contexts with source ONNX graphs.' 'warn'
    }
    if ($null -ne $artifact -and $artifact.Kind -eq 'omi_med_encoder') { $btnRunProof.Text = 'Run Omi encoder HTP test' }
    elseif ($null -ne $artifact -and $artifact.Kind -eq 'parakeet_tdt_joint') { $btnRunProof.Text = 'Run joint HTP test' }
    elseif ($null -ne $artifact -and $artifact.Kind -eq 'parakeet_tdt_predictor') { $btnRunProof.Text = 'Run predictor HTP test' }
    elseif ($null -ne $artifact -and $artifact.Kind -eq 'parakeet_encoder_projector') { $btnRunProof.Text = 'Run projector HTP test' }
    elseif ($null -ne $artifact -and $artifact.Kind -eq 'parakeet_encoder') { $btnRunProof.Text = 'Run Parakeet HTP test' }
    elseif ($null -ne $artifact -and $artifact.Kind -eq 'htp_proof') { $btnRunProof.Text = 'Run HTP proof' }
    else { $btnRunProof.Text = 'Run HTP test' }
    if ($btnRunProof.Enabled) { Set-Status $lblDeployRun ('Ready: ' + $btnRunProof.Text) 'good' } elseif ($btnStageProof.Enabled) { Set-Status $lblDeployRun 'Ready to stage; QNN runtime runner still required for execution' 'warn' } else { Set-Status $lblDeployRun 'Waiting for compiled context + reference + phone' 'warn' }
}

function Refresh-DeployArtifacts {
    param([string]$PreferredPath = '')

    # Preserve the user's current choice during ordinary rescans, but when a new
    # context has just been compiled the caller can explicitly select that exact
    # artifact. This prevents an older proof context from remaining active after
    # a successful Parakeet build.
    $selectedPath = ''
    if (-not [string]::IsNullOrWhiteSpace($PreferredPath)) {
        $selectedPath = $PreferredPath
    } else {
        $old = Get-SelectedDeployArtifact
        if ($null -ne $old) { $selectedPath = [string]$old.Path }
    }
    $cmbDeployArtifact.Items.Clear()
    $items = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
    foreach ($item in $items) { [void]$cmbDeployArtifact.Items.Add($item) }
    if ($cmbDeployArtifact.Items.Count -gt 0) {
        $index = 0
        if ($selectedPath) {
            for ($i=0; $i -lt $cmbDeployArtifact.Items.Count; $i++) {
                if ([string]$cmbDeployArtifact.Items[$i].Path -ieq $selectedPath) { $index = $i; break }
            }
        }
        $cmbDeployArtifact.SelectedIndex = $index
        $selected = Get-SelectedDeployArtifact
        if ($null -ne $selected) {
            if ([string]$selected.Kind -eq 'parakeet_tdt_joint') {
                $txtJointCompiled.Text = [string]$selected.Path
            } elseif ([string]$selected.Kind -eq 'parakeet_tdt_predictor') {
                $txtPredictorCompiled.Text = [string]$selected.Path
            } elseif ([string]$selected.Kind -eq 'parakeet_encoder_projector') {
                $txtProjectorCompiled.Text = [string]$selected.Path
            } elseif ([string]$selected.Kind -eq 'parakeet_encoder') {
                $txtCompiled.Text = [string]$selected.Path
            }
            $script:LastCompiledArtifact = [string]$selected.Path
        }
        Append-Log ('Deploy scan found ' + $cmbDeployArtifact.Items.Count + ' compiled QNN context file(s) across Builder workspaces. Newest is selected by default.')
    } else {
        Append-Log 'Deploy scan found no compiled QNN context files under any Builder workspace. Deploy & Test remains available; build a context or browse to an existing .bin.'
    }
    Update-DeployWorkflowState
}

# -------------------- Package --------------------
$tabPackage = New-Object System.Windows.Forms.TabPage
$tabPackage.Text = 'Package'
$tabPackage.AutoScroll = $true
[void]$tabs.TabPages.Add($tabPackage)

$tabPackage.Controls.Add((New-Label 'Encoder artifact' 25 25 140 24 -Bold))
$txtCompiled = New-TextBox 175 22 600
$tabPackage.Controls.Add($txtCompiled)
$btnCompiled = New-Button 'Browse...' 790 20 100
$tabPackage.Controls.Add($btnCompiled)
$tabPackage.Controls.Add((New-Label 'Projector artifact' 25 65 140 24 -Bold))
$txtProjectorCompiled = New-TextBox 175 62 600
$tabPackage.Controls.Add($txtProjectorCompiled)
$btnProjectorCompiled = New-Button 'Browse...' 790 60 100
$tabPackage.Controls.Add($btnProjectorCompiled)
$tabPackage.Controls.Add((New-Label 'Predictor artifact' 25 105 140 24 -Bold))
$txtPredictorCompiled = New-TextBox 175 102 600
$tabPackage.Controls.Add($txtPredictorCompiled)
$btnPredictorCompiled = New-Button 'Browse...' 790 100 100
$tabPackage.Controls.Add($btnPredictorCompiled)
$tabPackage.Controls.Add((New-Label 'Joint artifact' 25 145 140 24 -Bold))
$txtJointCompiled = New-TextBox 175 142 600
$tabPackage.Controls.Add($txtJointCompiled)
$btnJointCompiled = New-Button 'Browse...' 790 140 100
$tabPackage.Controls.Add($btnJointCompiled)
$tabPackage.Controls.Add((New-Label 'Model ID' 25 187 140 24 -Bold))
$txtModelId = New-TextBox 175 184 300
$txtModelId.Text = 'parakeet-tdt-0.6b-v3-qnn-s26'
$tabPackage.Controls.Add($txtModelId)
$tabPackage.Controls.Add((New-Label 'Display name' 25 229 140 24 -Bold))
$txtModelName = New-TextBox 175 226 600
$txtModelName.Text = 'NVIDIA Parakeet TDT 0.6B v3 - Galaxy S26 NPU'
$tabPackage.Controls.Add($txtModelName)
$tabPackage.Controls.Add((New-Label 'Package folder' 25 271 140 24 -Bold))
$txtPackageFolder = New-TextBox 175 268 600
$txtPackageFolder.Text = Join-Path $script:CurrentWorkspace 'package\parakeet-tdt-0.6b-v3-qnn-s26'
$tabPackage.Controls.Add($txtPackageFolder)
$btnPackageFolder = New-Button 'Browse...' 790 266 100
$tabPackage.Controls.Add($btnPackageFolder)

$btnProbeContract = New-Button 'Probe TDT contract' 175 312 165
$tabPackage.Controls.Add($btnProbeContract)
$btnBuildPackage = New-Button 'Build v1-draft package' 350 312 175
$tabPackage.Controls.Add($btnBuildPackage)
$btnZipPackage = New-Button 'Create deterministic ZIP' 535 312 175
$tabPackage.Controls.Add($btnZipPackage)
$btnOpenPackage = New-Button 'Open package' 720 312 130
$tabPackage.Controls.Add($btnOpenPackage)

$txtPackageResult = New-Object System.Windows.Forms.TextBox
$txtPackageResult.Multiline = $true
$txtPackageResult.ReadOnly = $true
$txtPackageResult.ScrollBars = 'Vertical'
$txtPackageResult.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$txtPackageResult.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,365)
$txtPackageResult.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(865,185)
$txtPackageResult.Anchor = 'Top,Left,Right,Bottom'
$tabPackage.Controls.Add($txtPackageResult)
$lblPackageNote = New-Label 'v0.3.21 preserves all validated graph/controller/runtime evidence, automatically recovers the newest complete unstripped v1-draft package, and safely ignores the package ZIP sidecar during Hugging Face deployment. No AI Hub or phone rerun is required. Production status remains runtime-pending.' 25 565 850 72
$tabPackage.Controls.Add($lblPackageNote)

# -------------------- Hugging Face --------------------
$tabHf = New-Object System.Windows.Forms.TabPage
$tabHf.Text = 'Hugging Face'
$tabHf.AutoScroll = $true
$tabHf.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,830)
[void]$tabs.TabPages.Add($tabHf)

$tabHf.Controls.Add((New-Label 'STEP 1 - Create canonical deployment folder' 25 18 430 28 -Bold))
$tabHf.Controls.Add((New-Label 'Source package' 25 58 130 24 -Bold))
$txtHfPackageFolder = New-TextBox 160 55 610
$txtHfPackageFolder.Text = $txtPackageFolder.Text
$tabHf.Controls.Add($txtHfPackageFolder)
$btnHfPackageBrowse = New-Button 'Browse...' 785 53 100
$tabHf.Controls.Add($btnHfPackageBrowse)

$tabHf.Controls.Add((New-Label 'Deployment folder' 25 98 130 24 -Bold))
$txtHfDeploymentFolder = New-TextBox 160 95 610
$txtHfDeploymentFolder.Text = Join-Path $Root 'deployment\parakeet-tdt-0.6b-v3-sm8850-v81-fp16'
$tabHf.Controls.Add($txtHfDeploymentFolder)
$btnHfDeploymentBrowse = New-Button 'Browse...' 785 93 100
$tabHf.Controls.Add($btnHfDeploymentBrowse)

$btnHfBuild = New-Button '1. Create deployment' 160 137 175 34
$tabHf.Controls.Add($btnHfBuild)
$btnHfAudit = New-Button '2. Audit deployment' 345 137 165 34
$tabHf.Controls.Add($btnHfAudit)
$btnHfOpenFolder = New-Button 'Open folder' 520 137 125 34
$tabHf.Controls.Add($btnHfOpenFolder)
$lblHfLocal = New-StatusLabel 660 143 225
$lblHfLocal.Text = 'Build the deployment folder first.'
$tabHf.Controls.Add($lblHfLocal)

$lblHfLayout = New-Label 'Builder creates README.md + CC BY 4.0 LICENSE/NOTICE + manifest/checksums + model/config/metadata + the exact validated linked decoder under experimental/. The deployment is re-hashed after copying. Qualcomm runtime binaries/tools are rejected.' 25 185 850 58
$lblHfLayout.ForeColor = [System.Drawing.Color]::DimGray
$tabHf.Controls.Add($lblHfLayout)

$tabHf.Controls.Add((New-Label 'STEP 2 - Upload / update Hugging Face repository' 25 255 500 28 -Bold))
$tabHf.Controls.Add((New-Label 'Repository' 25 298 130 24 -Bold))
$txtHfRepoId = New-TextBox 160 295 470
$tabHf.Controls.Add($txtHfRepoId)
$lblHfRepoHint = New-Label 'namespace/parakeet-tdt-0.6b-v3-sm8850-v81-fp16' 640 299 245 38
$lblHfRepoHint.ForeColor = [System.Drawing.Color]::DimGray
$tabHf.Controls.Add($lblHfRepoHint)

$tabHf.Controls.Add((New-Label 'Revision / branch' 25 338 130 24 -Bold))
$txtHfRevision = New-TextBox 160 335 190
$txtHfRevision.Text = 'main'
$tabHf.Controls.Add($txtHfRevision)
$tabHf.Controls.Add((New-Label 'Visibility' 375 338 75 24 -Bold))
$cmbHfVisibility = New-Object System.Windows.Forms.ComboBox
$cmbHfVisibility.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(455,335)
$cmbHfVisibility.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(140,25)
$cmbHfVisibility.DropDownStyle = 'DropDownList'
[void]$cmbHfVisibility.Items.Add('private')
[void]$cmbHfVisibility.Items.Add('public')
$cmbHfVisibility.SelectedItem = 'private'
$tabHf.Controls.Add($cmbHfVisibility)
$lblHfVisibility = New-Label 'Private is recommended until the Android runtime bake-off and redistribution review are complete.' 610 336 275 42
$lblHfVisibility.ForeColor = [System.Drawing.Color]::DimGray
$tabHf.Controls.Add($lblHfVisibility)

$tabHf.Controls.Add((New-Label 'Write token' 25 383 130 24 -Bold))
$txtHfToken = New-TextBox 160 380 470 25 $true $false
$tabHf.Controls.Add($txtHfToken)
$btnHfTokenPage = New-Button 'Get token' 640 377 105 30
$tabHf.Controls.Add($btnHfTokenPage)
$lblHfTokenPrivacy = New-Label 'Token is passed only through the uploader process environment. It is never saved in user-settings.json, session logs, or support ZIPs.' 160 414 720 42
$lblHfTokenPrivacy.ForeColor = [System.Drawing.Color]::DimGray
$tabHf.Controls.Add($lblHfTokenPrivacy)

$btnHfUpload = New-Button '3. Upload / update' 160 462 170 36
$tabHf.Controls.Add($btnHfUpload)
$btnHfVerifyRemote = New-Button '4. Verify remote' 340 462 150 36
$tabHf.Controls.Add($btnHfVerifyRemote)
$btnHfOpenRepo = New-Button 'Open repository' 500 462 145 36
$tabHf.Controls.Add($btnHfOpenRepo)
$lblHfRemote = New-StatusLabel 660 470 225
$lblHfRemote.Text = 'Not uploaded / verified.'
$tabHf.Controls.Add($lblHfRemote)

$txtHfResult = New-Object System.Windows.Forms.TextBox
$txtHfResult.Multiline = $true
$txtHfResult.ReadOnly = $true
$txtHfResult.ScrollBars = 'Vertical'
$txtHfResult.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$txtHfResult.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(25,520)
$txtHfResult.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(860,220)
$tabHf.Controls.Add($txtHfResult)

$lblHfFooter = New-Label 'Upload creates the repository if it does not exist. Existing repository visibility is not intentionally changed by the Builder. Uploads always start from a passing local deployment audit; Verify remote checks every expected path/size and available large-object hashes without re-downloading the 1+ GB encoder.' 25 750 850 54
$lblHfFooter.ForeColor = [System.Drawing.Color]::DimGray
$tabHf.Controls.Add($lblHfFooter)

# -------------------- Phone Benchmarks (official llama.cpp, no APK build) --------------------
$tabPhoneBench = New-Object System.Windows.Forms.TabPage
$tabPhoneBench.Text = 'Phone Benchmarks'
$tabPhoneBench.AutoScroll = $true
$tabPhoneBench.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,1070)
[void]$tabs.TabPages.Add($tabPhoneBench)
$tabPhoneBench.Controls.Add((New-Label 'Qwen3-ASR / llama.cpp — run official Android ARM64 CPU binaries on the S26' 24 18 860 31 -Bold))
$tabPhoneBench.Controls.Add((New-Label 'No WhisperRecorder APK build, QNN SDK, Termux or model replacement. First run downloads the pinned official release and Q8_0 GGUF (~1 GB), then caches them for repeat tests. CPU only in v0.4.52; persistent localhost HTTP server.' 24 55 852 62))
$tabPhoneBench.Controls.Add((New-Label 'Official pinned llama.cpp release' 24 131 260 23 -Bold))
$txtPhoneRelease = New-TextBox 294 127 120 25 $false $true
$txtPhoneRelease.Text = 'b11430'
$tabPhoneBench.Controls.Add($txtPhoneRelease)
$tabPhoneBench.Controls.Add((New-Label 'ggml-org/Qwen3-ASR-0.6B-GGUF : Q8_0 (805 MB + 214 MB mmproj)' 430 130 455 38))
$tabPhoneBench.Controls.Add((New-Label 'Reference recording (WAV, M4A, MP3, or OGG)' 24 185 400 24 -Bold))
$txtPhoneAudio = New-TextBox 24 219 690 26
$tabPhoneBench.Controls.Add($txtPhoneAudio)
$btnPhoneAudio = New-Button 'Browse audio...' 729 217 145 30
$tabPhoneBench.Controls.Add($btnPhoneAudio)
$tabPhoneBench.Controls.Add((New-Label 'First benchmark length' 24 275 200 24 -Bold))
$cmbPhoneSeconds = New-Object System.Windows.Forms.ComboBox
$cmbPhoneSeconds.DropDownStyle = 'DropDownList'
$cmbPhoneSeconds.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(240,272)
$cmbPhoneSeconds.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(205,26)
[void]$cmbPhoneSeconds.Items.Add('12 seconds (recommended)')
[void]$cmbPhoneSeconds.Items.Add('Full recording (single model, auto 12s segments)')
$cmbPhoneSeconds.SelectedIndex = 0
$tabPhoneBench.Controls.Add($cmbPhoneSeconds)
$tabPhoneBench.Controls.Add((New-Label 'CPU decoder threads' 484 275 183 24 -Bold))
$cmbPhoneThreads = New-Object System.Windows.Forms.ComboBox
$cmbPhoneThreads.DropDownStyle = 'DropDownList'
$cmbPhoneThreads.Location = New-Object -TypeName System.Drawing.Point -ArgumentList @(675,272)
$cmbPhoneThreads.Size = New-Object -TypeName System.Drawing.Size -ArgumentList @(100,25)
[void]$cmbPhoneThreads.Items.Add('4')
[void]$cmbPhoneThreads.Items.Add('2')
$cmbPhoneThreads.SelectedIndex = 0
$tabPhoneBench.Controls.Add($cmbPhoneThreads)
$btnPhoneConnect = New-Button '1. Connect phone' 24 337 190 38
$tabPhoneBench.Controls.Add($btnPhoneConnect)
$btnPhonePrepare = New-Button '2. Prepare official files' 226 337 216 38
$tabPhoneBench.Controls.Add($btnPhonePrepare)
$btnPhoneRun = New-Button '3. Run phone ASR test' 455 337 228 38
$tabPhoneBench.Controls.Add($btnPhoneRun)
$btnPhoneResults = New-Button 'Open results' 696 337 180 38
$tabPhoneBench.Controls.Add($btnPhoneResults)
$lblPhoneStatus = New-Label 'Ready. One local llama-server, independent audio requests, one model load; 12s chunks avoid context exhaustion.' 25 394 850 45
$tabPhoneBench.Controls.Add($lblPhoneStatus)
$tabPhoneBench.Controls.Add((New-Label 'Privacy + cleanup: each run removes ALL benchmark files from phone (Q8_0 GGUF, server/libs, run files) after success or failure, with verification in ZIP. Windows downloads stay cached, allowing future phone transfers without re-downloading.' 25 452 850 67))
$tabPhoneBench.Controls.Add((New-Label 'Recovery cleanup (only if an interrupted run left files)' 25 547 440 28 -Bold))
$btnPhoneCleanup = New-Button 'Retry cleanup of Builder-owned phone files' 25 585 385 34
$tabPhoneBench.Controls.Add($btnPhoneCleanup)
$tabPhoneBench.Controls.Add((New-Label 'Automatic cleanup runs after EVERY benchmark. This recovery button only removes /data/local/tmp/wr-llama-bench-v1 if ADB was interrupted. Never touches WhisperRecorder models, recordings or other ADB staging.' 25 633 850 60))

# -------------------- Qwen NPU + GGUF decoder readiness (read-only) --------------------
$tabPhoneBench.Controls.Add((New-Label 'Qwen hybrid readiness - compare diagnostics before any APK integration' 25 726 850 28 -Bold))
$tabPhoneBench.Controls.Add((New-Label 'Select an existing hybrid ZIP once. A FULL phone test then triggers this audit automatically after verified cleanup. Manual audit also works offline. Does NOT execute the NPU -> GGUF decoder bridge. No extra downloads.' 25 756 850 50))
$tabPhoneBench.Controls.Add((New-Label 'Standalone llama.cpp FULL benchmark ZIP (auto-selects newest; incomplete blocked)' 25 818 730 23 -Bold))
$txtQwenBridgeBenchmark = New-TextBox 25 844 695 26
$tabPhoneBench.Controls.Add($txtQwenBridgeBenchmark)
$btnQwenBridgeBenchmark = New-Button 'Browse...' 731 842 140 29
$tabPhoneBench.Controls.Add($btnQwenBridgeBenchmark)
$tabPhoneBench.Controls.Add((New-Label 'Existing Qwen NPU + ONNX hybrid diagnostic ZIP (select once)' 25 882 725 23 -Bold))
$txtQwenBridgeHybrid = New-TextBox 25 909 695 26
$tabPhoneBench.Controls.Add($txtQwenBridgeHybrid)
$btnQwenBridgeHybrid = New-Button 'Browse...' 731 907 140 29
$tabPhoneBench.Controls.Add($btnQwenBridgeHybrid)
$btnQwenBridgeAudit = New-Button 'Audit benchmarks + bridge readiness (offline)' 25 955 427 38
$tabPhoneBench.Controls.Add($btnQwenBridgeAudit)
$tabPhoneBench.Controls.Add((New-Label 'The resulting diagnostic ZIP has metrics and gating reasons only. No audio, transcripts, model weights or encoder-feature values. NPU-to-GGUF position/parity remains a separate required implementation gate.' 25 1000 850 52))


# -------------------- Qwen NPU -> GGUF mixed-embedding hardware test --------------------
# This is a genuine short QNN graph -> native GGUF decoder *execution* when all
# artifacts are supplied. It is NOT yet positional/token/transcription parity.
$script:QwenBridgePhonePath = Join-Path $Root 'python\qwen_native_bridge_phone.py'
$script:QwenBridgeDiscoveryPath = Join-Path $Root 'python\qwen_bridge_autodiscover.py'
$script:QwenBridgePhoneResults = Join-Path $Root 'workspace\qwen-native-bridge\results'
$script:QwenBridgeReviewPath = Join-Path $Root 'python\qwen_bridge_ground_truth.py'
$script:QwenBridgeRepeatPath = Join-Path $Root 'python\qwen_bridge_repeat_guard.py'
$script:QwenBridgeReviewDir = Join-Path $Root 'workspace\qwen-bridge-reference-review'
$script:QwenBridgeBatchPath = Join-Path $Root 'python\qwen_bridge_batch_qualification.py'
$script:QwenBridgeReferencePath = Join-Path $Root 'python\qwen_bridge_reference.py'
$script:QwenBridgeAccuracyPath = Join-Path $Root 'python\qwen_bridge_accuracy.py'
$script:QwenBridgePrefs = Join-Path $Root 'workspace\qwen-native-bridge\choices.json'
# Under Set-StrictMode -Version Latest, script-scope flags must exist before
# SelectedIndexChanged reads them. The first visit should auto-find once.
$script:QwenBridgeAutofindTried = $false
$tabQwenBridge = New-Object System.Windows.Forms.TabPage
$tabQwenBridge.Text = 'Qwen NPU Bridge'
$tabQwenBridge.AutoScroll = $true
$tabQwenBridge.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,1770)
[void]$tabs.TabPages.Add($tabQwenBridge)
$tabQwenBridge.Controls.Add((New-Label 'Qwen3-ASR 0.6B: Real NPU encoder -> native GGUF decoder probe' 24 18 865 30 -Bold))
$tabQwenBridge.Controls.Add((New-Label 'ONE 12-second experiment using the previously compiled QNN HTP encoder and cached Q8 GGUF. The Android-native probe is built once with the included GitHub Actions workflow (no local NDK install). Execution does NOT prove M-RoPE or transcription parity.' 24 55 851 75))
$tabQwenBridge.Controls.Add((New-Label 'Existing S26 encoder QNN context (.bin, Stage F3/Stage I)' 24 144 800 25 -Bold))
$txtBridgeQnnContext = New-TextBox 24 175 680 26
$tabQwenBridge.Controls.Add($txtBridgeQnnContext)
$btnBridgeQnnContext = New-Button 'Browse...' 716 174 148 29
$tabQwenBridge.Controls.Add($btnBridgeQnnContext)
$tabQwenBridge.Controls.Add((New-Label 'Existing validated Android QNN runtime paths manifest (JSON)' 24 216 800 25 -Bold))
$txtBridgeQnnRuntime = New-TextBox 24 247 680 26
$tabQwenBridge.Controls.Add($txtBridgeQnnRuntime)
$btnBridgeQnnRuntime = New-Button 'Browse...' 716 246 148 29
$tabQwenBridge.Controls.Add($btnBridgeQnnRuntime)

$tabQwenBridge.Controls.Add((New-Label 'One-time GitHub Actions Android ARM64 bridge-probe ZIP' 24 288 800 25 -Bold))
$txtBridgeProbe = New-TextBox 24 319 680 26
$tabQwenBridge.Controls.Add($txtBridgeProbe)
$btnBridgeProbe = New-Button 'Browse...' 716 318 148 29
$tabQwenBridge.Controls.Add($btnBridgeProbe)
$tabQwenBridge.Controls.Add((New-Label 'Audio preprocessing uses existing FFmpeg + NumPy automatically. No preprocessing file to select. Same audio and ADB as Phone Benchmarks; cached GGUF is reused.' 24 365 852 48))
$btnBridgeAutoFind = New-Button 'Find existing encoder + runtime + probe files' 24 424 540 39
$tabQwenBridge.Controls.Add($btnBridgeAutoFind)
$btnBridgeCpuProbe = New-Button 'Find CPU-tuned probe ZIP' 573 424 293 39
$tabQwenBridge.Controls.Add($btnBridgeCpuProbe)
$tabQwenBridge.Controls.Add((New-Label 'Searches Builder workspaces and existing runtime tools, plus Downloads for an optional probe ZIP. No phone or drive-wide scan.' 24 466 852 41))
$btnBridgeTest = New-Button 'Run 12s QNN -> GGUF decoder test' 24 524 405 40
$tabQwenBridge.Controls.Add($btnBridgeTest)
$btnBridgeRecover = New-Button 'Recover exact owned phone staging' 444 524 425 40
$tabQwenBridge.Controls.Add($btnBridgeRecover)
$btnBridgeResults = New-Button 'Open bridge diagnostic ZIP folder' 24 582 410 36
$tabQwenBridge.Controls.Add($btnBridgeResults)
$tabQwenBridge.Controls.Add((New-Label 'ONE-CLICK short correctness evidence: first prepare/check a reference transcript locally. Runs QNN + native/ONNX tokens and WER/CER, with verified cleanup. Do not repeat the same clip without new ground truth; long tests remain gated.' 24 633 851 69))
$btnBridgeInstrument = New-Button 'RUN instrumented parity + ALL qualification gates' 24 708 710 43
$btnBridgeInstrument.Name = 'QwenBridgeRunInstrumentedParity'
$tabQwenBridge.Controls.Add($btnBridgeInstrument)
$btnBridgeBatch = New-Button 'Offline audit existing evidence (no phone test)' 24 767 555 37
$tabQwenBridge.Controls.Add($btnBridgeBatch)
$tabQwenBridge.Controls.Add((New-Label 'Original existing Qwen3-ASR 0.6B ONNX decoder export folder (no re-export)' 24 828 851 28 -Bold))
$txtBridgeOnnxExport = New-TextBox 24 861 675 27
$tabQwenBridge.Controls.Add($txtBridgeOnnxExport)
$btnBridgeOnnxExport = New-Button 'Browse folder...' 712 861 158 31
$tabQwenBridge.Controls.Add($btnBridgeOnnxExport)
$tabQwenBridge.Controls.Add((New-Label 'Required for NEW accuracy runs: manually verified first-12s transcript (.txt; local)' 24 898 851 24 -Bold))
$txtBridgeGroundTruth = New-TextBox 24 919 675 26
$tabQwenBridge.Controls.Add($txtBridgeGroundTruth)
$btnBridgeGroundTruth = New-Button 'Browse text...' 712 919 158 29
$tabQwenBridge.Controls.Add($btnBridgeGroundTruth)
$btnBridgePrepareTruth = New-Button 'Prepare 12s listening clip + text file (local only)' 24 956 640 32
$tabQwenBridge.Controls.Add($btnBridgePrepareTruth)
$btnBridgeListenLouder = New-Button 'Listen louder (private)' 682 956 189 32
$tabQwenBridge.Controls.Add($btnBridgeListenLouder)
$tabQwenBridge.Controls.Add((New-Label 'Optional historical parity and performance evidence JSON for offline-only audit' 24 1007 851 25 -Bold))
$txtBridgeParityEvidence = New-TextBox 24 1038 678 26
$tabQwenBridge.Controls.Add($txtBridgeParityEvidence)
$btnBridgeParityEvidence = New-Button 'Browse...' 716 1038 148 29
$tabQwenBridge.Controls.Add($btnBridgeParityEvidence)
$chkBridgePrivateReview = New-Object System.Windows.Forms.CheckBox
$chkBridgePrivateReview.Text = 'Save private text on Windows for the transcription viewer (never ZIP)'
$chkBridgePrivateReview.SetBounds(24, 1064, 770, 32)
$chkBridgePrivateReview.Checked = $false
$chkBridgePrivateReview.Name = 'QwenBridgeSavePrivateText'
$tabQwenBridge.Controls.Add($chkBridgePrivateReview)
$lblBridgeStatus = New-Label 'The instrumented workflow needs reviewed ground truth. Prepare the local 12s audio and transcript, then use existing ONNX, NPU and probe files.' 24 1120 851 70
$tabQwenBridge.Controls.Add($lblBridgeStatus)
$tabQwenBridge.Controls.Add((New-Label 'Native trace values never enter the diagnostic ZIP. ONNX runs locally AFTER phone files have been removed. M-RoPE and independent transcript review remain required, and full-recording measurements remain gated. No WhisperRecorder APK build.' 24 1220 851 100))

# v0.4.70: show the SAME selected audio on the Qwen tab; no second input source.
# Move existing lower controls together to make room without overlapping labels.
foreach ($control in @($tabQwenBridge.Controls)) {
    if ($control.Top -ge 144) { $control.Top += 76 }
    if ($control.Top -ge 1190) { $control.Top += 104 }
}
$tabQwenBridge.Controls.Add((New-Label 'Current 12s reference recording (shared with Phone Benchmarks)' 24 143 840 24 -Bold))
$txtBridgeAudio = New-TextBox 24 174 677 26 $false $true
$txtBridgeAudio.Text = [string]$txtPhoneAudio.Text
$tabQwenBridge.Controls.Add($txtBridgeAudio)
$btnBridgeAudio = New-Button 'Browse audio...' 714 173 154 31
$tabQwenBridge.Controls.Add($btnBridgeAudio)
$script:QwenBridgeAudioSyncReady = $false
$txtPhoneAudio.Add_TextChanged({
    $txtBridgeAudio.Text = [string]$txtPhoneAudio.Text
    # Startup settings restore audio AFTER bridge transcript preferences. Preserve
    # the saved transcript until startup is complete; only user changes clear it.
    if ($script:QwenBridgeAudioSyncReady) { $txtBridgeGroundTruth.Text = '' }
})
$btnBridgeAudio.Add_Click({
    $previousAudio = [string]$txtPhoneAudio.Text
    Choose-File $txtPhoneAudio 'Select Qwen NPU Bridge 12s reference audio' 'Audio (*.wav;*.m4a;*.mp3;*.ogg)|*.wav;*.m4a;*.mp3;*.ogg|All files (*.*)|*.*'
    if ($previousAudio -ne [string]$txtPhoneAudio.Text) {
        $txtBridgeGroundTruth.Text = ''
        Save-QwenBridgeChoices
    }
})
$btnBridgeViewText = New-Button 'View transcript from LAST instrumented test' 24 1195 565 37
$tabQwenBridge.Controls.Add($btnBridgeViewText)
$btnBridgeOpenReviewFolder = New-Button 'Open transcript folder' 602 1195 267 37
$tabQwenBridge.Controls.Add($btnBridgeOpenReviewFolder)
$btnBridgeOpenReviewFolder.Add_Click({
    try {
        if (-not (Test-Path -LiteralPath $script:QwenBridgeReviewDir -PathType Container)) {
            throw 'No private review folder yet. Prepare a local listening clip first.'
        }
        Open-Folder $script:QwenBridgeReviewDir
    } catch { Show-Error $_ }
})
$tabQwenBridge.Controls.Add((New-Label 'Shows only a linked completed run. Use Open transcript folder for older unlinked files.' 24 1240 850 35))
$chkBridgeAllowRepeat = New-Object System.Windows.Forms.CheckBox
$chkBridgeAllowRepeat.Text = 'Allow deliberate repeat of an already scored 12s clip (normally leave unchecked)'
$chkBridgeAllowRepeat.SetBounds(24, 1522, 846, 34)
$chkBridgeAllowRepeat.Checked = $false
$tabQwenBridge.Controls.Add($chkBridgeAllowRepeat)
# v0.4.76 isolated experimental gain A/B preflight, entirely local until the user
# explicitly runs the already existing instrumented phone qualification.
$btnBridgeGainProbe = New-Button 'Prepare +15 dB MODEL INPUT test (experimental; local only)' 24 1580 725 39
$btnBridgeGainProbe.Name = 'QwenBridgePrepareGain'
$tabQwenBridge.Controls.Add($btnBridgeGainProbe)
$tabQwenBridge.Controls.Add((New-Label 'Unlike Listen louder, this changes what the NPU will hear in the NEXT test. It does not update the original recording or production app. Listen and verify reference text before intentionally running one comparison.' 24 1625 851 74))
$btnBridgeGainProbe.Add_Click({
    Append-Log 'QWEN_UI_ACTION=PREPARE_GAIN_CLICKED; no_phone_work_until_instrumented_run'
    # A prepared gainprobe WAV is automatically selected after a successful
    # local preparation. Re-clicking Prepare must not create an error ZIP or
    # double-amplify the audio. Keep Python's independent safety guard too.
    $chosenAudio = [string]$txtPhoneAudio.Text
    if ($chosenAudio) {
        $chosenName = [System.IO.Path]::GetFileName($chosenAudio)
        $chosenDir = [System.IO.Path]::GetDirectoryName($chosenAudio)
        if ([string]::Equals($chosenDir, [string]$script:QwenBridgeReviewDir,
                [System.StringComparison]::OrdinalIgnoreCase) -and
            $chosenName -cmatch '^(?:gainprobe|boost)-[a-f0-9]{20}\.wav$') {
            $hint = 'The +15 dB audio is already selected. Do not amplify it again. To read the words, use View transcript from LAST instrumented test (if private text was saved), or Open transcript folder for older runs. To prepare a NEW gain test, choose the original OGG with Browse audio.'
            $lblBridgeStatus.Text = $hint
            Append-Log 'Gain model-input test already selected. Existing local file preserved; no phone activity or repeated amplification.'
            [void][System.Windows.Forms.MessageBox]::Show($hint, 'Already amplified - no action needed',
                [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Information)
            return
        }
    }
    Invoke-UiAction 'Preparing a private louder model-input A/B clip; no phone activity...' {
        if (-not $txtPhoneAudio.Text -or -not (Test-Path -LiteralPath $txtPhoneAudio.Text -PathType Leaf)) {
            throw 'Choose and prepare the original recording first. No phone files staged.'
        }
        if (-not $txtBridgeGroundTruth.Text -or -not (Test-Path -LiteralPath $txtBridgeGroundTruth.Text -PathType Leaf)) {
            throw 'Prepare and save the manually verified original first-12s transcript first. No phone files staged.'
        }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        $py = Get-LocalBuilderPython
        $args = @('-u', $script:QwenBridgeReviewPath, 'gain-probe',
            '--audio', [string]$txtPhoneAudio.Text, '--ffmpeg', [string]$txtFfmpeg.Text,
            '--review-dir', $script:QwenBridgeReviewDir,
            '--ground-truth', [string]$txtBridgeGroundTruth.Text)
        $r = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 180
        $old = [regex]::Match($r.StdOut, '(?m)^GAIN_PROBE_SOURCE_KEY=([a-f0-9]{20})\r?$')
        $new = [regex]::Match($r.StdOut, '(?m)^GAIN_PROBE_NEW_KEY=([a-f0-9]{20})\r?$')
        $ok = [regex]::IsMatch($r.StdOut, '(?m)^GAIN_PROBE_STATUS=prepared_local_only_verify_text_before_phone\r?$')
        if ($r.ExitCode -ne 0 -or -not $ok -or -not $old.Success -or -not $new.Success) {
            throw 'Gain comparison preparation blocked locally; check GAIN_PROBE error category in the Builder log. No phone staging occurred.'
        }
        $louderAudio = Join-Path $script:QwenBridgeReviewDir ('gainprobe-' + $old.Groups[1].Value + '.wav')
        $newText = Join-Path $script:QwenBridgeReviewDir ('clip-' + $new.Groups[1].Value + '.txt')
        if (-not (Test-Path -LiteralPath $louderAudio -PathType Leaf) -or -not (Test-Path -LiteralPath $newText -PathType Leaf)) {
            throw 'Prepared files missing. No phone staging occurred.'
        }
        $txtPhoneAudio.Text = $louderAudio
        $txtBridgeGroundTruth.Text = $newText
        Save-QwenBridgeChoices
        $lblBridgeStatus.Text = 'Experimental louder MODEL INPUT selected. Listen, inspect the copied ground truth and save any corrections. Then deliberately run one instrumented comparison. To go back, browse your original audio.'
        try { Start-Process -FilePath 'notepad.exe' -ArgumentList ('"{0}"' -f $newText) | Out-Null }
        catch { Append-Log 'Open the selected local reference text manually if Notepad did not start.' }
        try { Start-Process -FilePath $louderAudio | Out-Null }
        catch { Append-Log 'Open the selected louder WAV manually to listen.' }
    }
})

# v0.4.82: bounded one-click experimental full-recording evidence.
# The native artifact is NEW (GitHub one-time build), and prior accuracy is
# accepted ONLY for benchmarking; NOT for production or M-RoPE qualification.
$script:QwenBridgeFullPath = Join-Path $Root 'python\qwen_bridge_full_benchmark.py'
$tabQwenBridge.AutoScrollMinSize = New-Object -TypeName System.Drawing.Size -ArgumentList @(900,2530)
$tabQwenBridge.Controls.Add((New-Label 'Q8 NATIVE DECODER PERFORMANCE (single first run by default; optional thread sweep)' 24 1740 851 30 -Bold))
$tabQwenBridge.Controls.Add((New-Label 'One controlled phone session: two-segment canary, encode all segments once, then ONE Q8 decoder run (4/4 threads). CPU seconds, time, hashes, memory and cleanup are measured. Optional 4/3/2/4 thread sweep remains available. Optimized ARM dotprod probes are experimental build candidates, not proven faster.' 24 1780 850 120))
$chkBridgeProvisionalFull = New-Object System.Windows.Forms.CheckBox
$chkBridgeProvisionalFull.Text = 'I accept current short-clip accuracy provisionally for EXPERIMENTAL performance measurement'
$chkBridgeProvisionalFull.SetBounds(24,1906,850,34)
$chkBridgeProvisionalFull.Checked = $false
$chkBridgeProvisionalFull.Name = 'QwenBridgeProvisionalPerformanceConsent'
$tabQwenBridge.Controls.Add($chkBridgeProvisionalFull)
$btnBridgeFull = New-Button 'RUN gated Q8 first-run decoder benchmark (first 240s max)' 24 1948 744 43
$btnBridgeFull.Name = 'QwenBridgeRunPersistentBenchmark'
$tabQwenBridge.Controls.Add($btnBridgeFull)
# v0.4.92: opt-in matched prefill + generation CPU experiment.
$chkBridgeMatchedThreads = New-Object System.Windows.Forms.CheckBox
$chkBridgeMatchedThreads.Text = 'NEW: match prefill and generation threads (4/4, 3/3, 2/2, 4/4; requires v0.4.92 ARM64 probe)'
$chkBridgeMatchedThreads.SetBounds(24,2000,855,35)
$chkBridgeMatchedThreads.Checked = $false
$chkBridgeMatchedThreads.Name = 'QwenBridgeMatchedThreads'
$tabQwenBridge.Controls.Add($chkBridgeMatchedThreads)
# v0.4.93: optional between-profile temperature proxy control; no new ARM64 ELF.
$chkBridgeTemperatureMatch = New-Object System.Windows.Forms.CheckBox
$chkBridgeTemperatureMatch.Text = 'NEW: compare profiles after cooling near first trial temperature (up to 4 min between trials)'
$chkBridgeTemperatureMatch.SetBounds(24,2037,855,35)
$chkBridgeTemperatureMatch.Checked = $false
$chkBridgeTemperatureMatch.Name = 'QwenBridgeTemperatureMatch'
$tabQwenBridge.Controls.Add($chkBridgeTemperatureMatch)
# v0.4.94: experimental local GGUF Q8/Q4/Q5 controlled comparison.
$tabQwenBridge.Controls.Add((New-Label 'NEW: LOWER-BIT GGUF DECODER COMPARISON (NO NEW ARM64 BUILD)' 24 2090 851 32 -Bold))
$chkBridgeQuantCompare = New-Object System.Windows.Forms.CheckBox
$chkBridgeQuantCompare.Text = 'Compare Q8_0 baseline with Q4_K_M (and optional Q5_K_M); same 4/4 threads, one NPU pass'
$chkBridgeQuantCompare.SetBounds(24,2127,850,36)
$chkBridgeQuantCompare.Checked = $false
$chkBridgeQuantCompare.Name = 'QwenBridgeQuantCompare'
$tabQwenBridge.Controls.Add($chkBridgeQuantCompare)
$tabQwenBridge.Controls.Add((New-Label 'Q4_K_M local GGUF (required for quant comparison)' 24 2170 850 25))
$txtBridgeQuantQ4 = New-TextBox 24 2200 678 27
$tabQwenBridge.Controls.Add($txtBridgeQuantQ4)
$btnBridgeQuantQ4 = New-Button 'Browse Q4...' 716 2199 151 31
$tabQwenBridge.Controls.Add($btnBridgeQuantQ4)
$tabQwenBridge.Controls.Add((New-Label 'Q5_K_M local GGUF (optional, adds a third decoder run)' 24 2242 850 25))
$txtBridgeQuantQ5 = New-TextBox 24 2271 678 27
$tabQwenBridge.Controls.Add($txtBridgeQuantQ5)
$btnBridgeQuantQ5 = New-Button 'Browse Q5...' 716 2270 151 31
$tabQwenBridge.Controls.Add($btnBridgeQuantQ5)
$btnBridgeQuantDownload = New-Button 'Download and cache Q4/Q5 models (optional; ~1 GB, Windows only)' 24 2318 840 37
$tabQwenBridge.Controls.Add($btnBridgeQuantDownload)
$tabQwenBridge.Controls.Add((New-Label 'Third-party Qwen3-ASR GGUF quantizations are experimental; their source-weight equivalence and transcript accuracy are NOT established by downloading. The phone benchmark stages only test-owned files and keeps model caches on Windows.' 24 2365 840 90))
# v0.4.96: one decoder pass as the default; opt out to explicitly run
# the older 4/3/2/4 generation-only experiment.
$chkBridgeFastQ8 = New-Object System.Windows.Forms.CheckBox
$chkBridgeFastQ8.Text = 'RECOMMENDED: one Q8 decoder run (4/4 threads), no repeated warming trials'
$chkBridgeFastQ8.SetBounds(24,2465,850,35)
$chkBridgeFastQ8.Checked = $true
$chkBridgeFastQ8.Name = 'QwenBridgeFirstRunQ8'
$tabQwenBridge.Controls.Add($chkBridgeFastQ8)
$chkBridgeFastQ8.Add_CheckedChanged({
    if (-not $chkBridgeQuantCompare.Checked -and -not $chkBridgeMatchedThreads.Checked) {
        $btnBridgeFull.Text = if ($chkBridgeFastQ8.Checked) {
            'RUN gated Q8 first-run decoder benchmark (first 240s max)'
        } else {
            'RUN gated CPU thread/thermal sweep (first 240s max)'
        }
    }
})
$chkBridgeQuantCompare.Add_CheckedChanged({
    if ($chkBridgeQuantCompare.Checked) {
        $chkBridgeMatchedThreads.Checked = $false
        $chkBridgeTemperatureMatch.Checked = $false
        $btnBridgeFull.Text = 'RUN gated Q8 / Q4 / Q5 comparison (first 240s max)'
    } else {
        $btnBridgeFull.Text = if ($chkBridgeFastQ8.Checked) { 'RUN gated Q8 first-run decoder benchmark (first 240s max)' } else { 'RUN gated CPU thread/thermal sweep (first 240s max)' }
    }
})
$btnBridgeQuantQ4.Add_Click({ Choose-File $txtBridgeQuantQ4 'Select local Qwen3-ASR Q4_K_M GGUF' 'GGUF (*.gguf)|*.gguf'; Save-QwenBridgeChoices })
$btnBridgeQuantQ5.Add_Click({ Choose-File $txtBridgeQuantQ5 'Select local Qwen3-ASR Q5_K_M GGUF' 'GGUF (*.gguf)|*.gguf'; Save-QwenBridgeChoices })
$btnBridgeQuantDownload.Add_Click({
    Invoke-UiAction 'Downloading optional Qwen GGUF quantized models into Windows cache...' {
        $py = Get-LocalBuilderPython
        $modelDir = Join-Path $script:LlamaPhoneBenchCache 'models'
        New-Item -ItemType Directory -Force -Path $modelDir | Out-Null
        $helper = Join-Path $Root 'python\qwen_bridge_quant.py'
        foreach ($q in @('Q4_K_M','Q5_K_M')) {
            $r = Invoke-ProcessCapture -FilePath $py -Arguments @('-u',$helper,'download','--quant',$q,'--cache-models',$modelDir) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 1800
            if ($r.ExitCode -ne 0 -or -not [regex]::IsMatch($r.StdOut,'(?m)^QUANT_DOWNLOAD_STATUS=verified_local_gguf\r?$')) {
                $reason = [regex]::Match($r.StdOut,'(?m)^QUANT_DOWNLOAD_ERROR=([A-Z0-9_]{5,100})\r?$')
                $category = if ($reason.Success) {$reason.Groups[1].Value} else {'QUANT_DOWNLOAD_INCOMPLETE'}
                throw ('Quant model download blocked: '+$category+'. Existing cached models are unchanged.')
            }
        }
        $txtBridgeQuantQ4.Text = Join-Path $modelDir 'Qwen3-ASR-0.6B.Q4_K_M.gguf'
        $txtBridgeQuantQ5.Text = Join-Path $modelDir 'Qwen3-ASR-0.6B.Q5_K_M.gguf'
        Save-QwenBridgeChoices
        $lblBridgeStatus.Text = 'Q4/Q5 models cached and metadata-checked locally. Check quant comparison and RUN gated benchmark when ready. This does not qualify model accuracy.'
    }
})
$btnBridgeFull.Add_Click({
    Append-Log 'QWEN_UI_ACTION=PERSISTENT_MULTI_BENCH_CLICKED'
    Invoke-UiAction 'Experimental persistent multi-segment bridge benchmark...' {
        if (-not $chkBridgeProvisionalFull.Checked) {
            throw 'Explicit provisional-accuracy benchmark consent is required. No phone files staged.'
        }
        if (-not (Test-Path -LiteralPath $txtPhoneAudio.Text -PathType Leaf)) {
            throw 'Select the approximately 3-minute lecture audio first. No phone files staged.'
        }
        if (-not (Test-Path -LiteralPath $txtBridgeQnnContext.Text -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txtBridgeQnnRuntime.Text -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txtBridgeProbe.Text -PathType Leaf)) {
            Find-QwenBridgeExistingFiles
        }
        # Upgrade stale saved v0.4.82 probe selections using an exact local
        # capability check. Stop here if no compatible v0.4.88+ artifact exists.
        if (-not (Resolve-QwenBridgeCpuTuneProbe)) {
            throw ('CPU-tuned ARM64 probe preflight blocked: ' + $lblBridgeStatus.Text)
        }
        # Matched experiment uses a NEW native protocol and is separately opted in.
        $useMatched = [bool]$chkBridgeMatchedThreads.Checked
        $useQuant = [bool]$chkBridgeQuantCompare.Checked
        $useFastQ8 = [bool]$chkBridgeFastQ8.Checked -and -not $useQuant -and -not $useMatched
        if ($useFastQ8 -and [bool]$chkBridgeTemperatureMatch.Checked) {
            throw 'Uncheck cooling/temperature-match for single-run Q8 mode. No phone files staged.'
        }
        if ($useQuant -and ($useMatched -or [bool]$chkBridgeTemperatureMatch.Checked)) {
            throw 'Uncheck matched-thread and temperature-match options when comparing quantized GGUF models. No phone files staged.'
        }
        if ($useQuant -and $txtBridgeQuantQ5.Text -and -not (Test-Path -LiteralPath $txtBridgeQuantQ5.Text -PathType Leaf)) {
            throw 'Selected optional Q5 GGUF does not exist. Clear its field or select a valid Q5 model. No phone files staged.'
        }
        if ($useQuant -and (-not (Test-Path -LiteralPath $txtBridgeQuantQ4.Text -PathType Leaf))) {
            throw 'Choose or download a Q4_K_M decoder GGUF before running the quant comparison. No phone files staged.'
        }
        # Phone use is opt-in. Avoid running a 72-minute recording by surprise;
        # benchmark ONLY the first 240s even if the source is longer.
        $description = if ($useQuant) {
            'Run ONE Q8 vs Q4_K_M (plus optional Q5_K_M) model-comparison session? QNN encodes the first 240 seconds only once. A 2-segment Q8 canary then independently loads each decoder model with 4/4 CPU threads. Measure decoder time, load cost, memory, token/EOS changes, thermal snapshots and verified cleanup. Quantized transcripts and source-weight equivalence are NOT independently qualified; the Q8-first order is thermally confounded.'
        } elseif ($useMatched) {
            'Run ONE matched-thread experiment: after the two-segment canary, QNN encodes all segments once and FOUR native profiles (decode/prefill = 4/4,3/3,2/2,4/4) replay identical features. Requires NEW v0.4.92 GitHub ARM64 native probe. Temp and workload order confound energy; no production change or watt estimate.'
        } elseif ($useFastQ8) {
            'Run ONE Q8 decoder candidate using the first 240 seconds at most? A 2-segment canary runs once; encode segments on QNN once; then execute ONE full decoder run using 4 prefill and 4 generation threads. Record native decoding wall time, CPU seconds, token hashes, peak RSS and verified cleanup. No thermal-repeat tests, no APK, no production change. The selected ARM64 executable may be an experimental dotprod candidate; no speedup or transcription qualification is claimed.'
        } else {
            'Run ONE controlled session (up to first 240 seconds)? A two-segment canary runs first. If successful, QNN encodes all segments once and FOUR thread profiles (4/3/2/4) reuse identical features. Each profile loads GGUF once and runs persistently. Collect CPU seconds, decode time, token hashes, RSS and battery/thermal proxies. Order and warming confound power comparisons; no calibrated energy or automatic production change. This is not a production speedup, full WER, M-RoPE, PSS, or calibrated energy test.'
        }
        if ([bool]$chkBridgeTemperatureMatch.Checked) {
            $description += "`r`n`r`nTemperature-matched option: pause up to 4 minutes before each later profile to approach the initial BATTERY temperature (not CPU/SoC temperature). If it cannot cool, stop remaining trials and verify cleanup. This can add up to 12 minutes."
        }
        $choice = [System.Windows.Forms.MessageBox]::Show(
          $description,
          'Confirm gated Qwen performance experiment',
          [System.Windows.Forms.MessageBoxButtons]::YesNo,
          [System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        if (-not $txtAdb.Text) { $txtAdb.Text = Install-AdbFromUi }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        $py = Get-QwenBridgePython
        Refresh-Device
        New-Item -ItemType Directory -Force -Path $script:QwenBridgePhoneResults | Out-Null
        $params = @('-u',$script:QwenBridgeFullPath,
            '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,
            '--ffmpeg',$txtFfmpeg.Text,'--audio',$txtPhoneAudio.Text,
            '--qnn-context',$txtBridgeQnnContext.Text,
            '--runtime-manifest',$txtBridgeQnnRuntime.Text,
            '--cache',$script:LlamaPhoneBenchCache,
            '--probe-archive',$txtBridgeProbe.Text,
            '--results',$script:QwenBridgePhoneResults,
            '--max-seconds','240','--threads','4','--steps','96','--full-trials',$(if ($useFastQ8) {'1'} else {'3'}),
            '--ack-provisional-accuracy')
        if ($useQuant) {
            $params += @('--quant-compare','--quant-q4-model',[string]$txtBridgeQuantQ4.Text)
            if ($txtBridgeQuantQ5.Text) {
                $params += @('--quant-q5-model',[string]$txtBridgeQuantQ5.Text)
            }
        } elseif ($useMatched) { $params += '--dual-thread-sweep' } elseif ($useFastQ8) { $params += '--q8-fast-baseline' } else { $params += '--cpu-thread-sweep' }
        if ([bool]$chkBridgeTemperatureMatch.Checked -and -not $useQuant) { $params += '--temperature-match' }
        $run = Invoke-ProcessCapture -FilePath $py -Arguments $params -Environment @{
            'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'PYTHONUNBUFFERED'='1'
        } -TimeoutSeconds 3600 -DisplayCommand 'Qwen bounded experimental full-recording benchmark'
        $receipts = [regex]::Matches($run.StdOut,'(?m)^DIAGNOSTIC_ZIP=(.+)\r?$')
        if ($receipts.Count -ne 1) { throw 'Full benchmark emitted no unique receipt; see error log. Do not rerun after an unverified cleanup.' }
        $receipt = $receipts[0].Groups[1].Value.Trim()
        if (-not (Test-Path -LiteralPath $receipt -PathType Leaf)) {
            throw 'Full benchmark receipt not saved. Do not blindly rerun.'
        }
        Append-Log ('Qwen experimental full-run sanitized diagnostic ZIP: '+$receipt)
        Save-QwenBridgeChoices
        # Explicitly surface the bounded, privacy-safe root-cause metadata.
        # No raw device stderr, transcript, model path or full manifest is logged.
        $stageMatch = [regex]::Match($run.StdOut, '(?m)^MULTI_BENCH_ERROR_STAGE=([A-Za-z0-9_]{1,80})\r?$')
        $codeMatch = [regex]::Match($run.StdOut, '(?m)^MULTI_BENCH_ERROR_CODE=([A-Za-z0-9_]{1,100})\r?$')
        $cleanMatch = [regex]::Match($run.StdOut, '(?m)^MULTI_BENCH_CLEANUP=(verified|not_staged|UNVERIFIED_REQUIRES_RECOVERY)\r?$')
        if ($run.ExitCode -ne 0) {
            $stageText = if ($stageMatch.Success) { $stageMatch.Groups[1].Value } else { 'see_benchmark_manifest' }
            $codeText = if ($codeMatch.Success) { $codeMatch.Groups[1].Value } else { 'see_benchmark_manifest' }
            $cleanupText = if ($cleanMatch.Success) { $cleanMatch.Groups[1].Value } else { 'unreported' }
            Append-Log ('MULTI_BENCH_DIAG stage='+$stageText+' code='+$codeText+' cleanup='+$cleanupText)
            try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $receipt) | Out-Null }
            catch { Append-Log 'Open the full benchmark diagnostic ZIP manually from the results folder.' }
            throw ('Gated benchmark stopped: '+$stageText+' / '+$codeText+'; phone cleanup='+$cleanupText+'. Upload the MultiSegment Benchmark ZIP, not just the generic Builder error ZIP. Do not rerun until cleanup is verified.')
        }
        $lblBridgeStatus.Text = 'Experimental multi-segment benchmark completed. Compare the model profiles and checks in the ZIP; full WER/PSS/joules/M-RoPE remain unqualified.'
        try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $receipt) | Out-Null }
        catch { Open-Folder $script:QwenBridgePhoneResults }
    }
})

$btnBridgeViewText.Add_Click({
    try {
        if (-not (Test-Path -LiteralPath $script:QwenBridgeReviewDir -PathType Container)) {
            throw 'No local transcript folder yet. Enable private saving for an instrumented test.'
        }
        # A random private filename has no inherent association with the latest
        # bridge run. Never show an older transcript as if it were the latest.
        $indexPath = Join-Path $script:QwenBridgeReviewDir 'latest-private-asr-run.json'
        if (-not (Test-Path -LiteralPath $indexPath -PathType Leaf)) {
            Open-Folder $script:QwenBridgeReviewDir
            throw 'Older Builder runs lack a verified run-to-transcript link. Review folder opened; sort private-asr files by Date modified. This button cannot identify the newest completed test from legacy files.'
        }
        $indexFile = Get-Item -LiteralPath $indexPath -ErrorAction Stop
        if (($indexFile.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -or $indexFile.Length -gt 4096) {
            throw 'Local transcript index is invalid; no file was opened.'
        }
        $index = Get-Content -LiteralPath $indexPath -Raw -ErrorAction Stop | ConvertFrom-Json -ErrorAction Stop
        if ($index.kind -ne 'qwen_private_review_latest_run_v1' -or
            [string]$index.bridge_receipt -notmatch '^WhisperRecorder-Qwen-NPU-GGUF-Bridge-[0-9]{8}-[0-9]{6}-[a-f0-9]{6}\.zip$') {
            throw 'Local transcript index is invalid; no file was opened.'
        }
        if ($index.completed -ne $true) {
            throw 'The last instrumented phone run did not complete successfully. An older transcript will not be shown.'
        }
        if ($index.saved -ne $true) {
            throw 'The last completed phone run did not save private text. Check Save private text on Windows BEFORE the next intentional run. Old transcripts remain available in Open transcript folder.'
        }
        $reviewName = [string]$index.review_file
        if ($reviewName -cnotmatch '^private-asr-[a-f0-9]{16}\.txt$') {
            throw 'Local transcript index has an invalid filename; no file was opened.'
        }
        $reviewFile = Join-Path $script:QwenBridgeReviewDir $reviewName
        $entry = Get-Item -LiteralPath $reviewFile -ErrorAction Stop
        if (($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -or
            $entry.Length -lt 1 -or $entry.Length -gt 131072) {
            throw 'Linked private transcription is unavailable or invalid.'
        }
        # This local-only numeric re-score reads an already-saved private file.
        # Never print words, save derived transcripts to ZIPs, or stage phone files.
        try {
            $py = Join-Path $Root 'tools\python311\python.exe'
            if (-not (Test-Path -LiteralPath $py -PathType Leaf)) { $py = [string]$txtPython.Text }
            if (-not $py -or -not (Test-Path -LiteralPath $py -PathType Leaf)) { throw 'LOCAL_RESCORE_PYTHON_UNAVAILABLE' }
            $scored = Invoke-ProcessCapture -FilePath $py -Arguments @('-u', $script:QwenBridgeAccuracyPath,
                'rescore', '--review-file', [string]$entry.FullName) -TimeoutSeconds 30 -DisplayCommand 'Local-only Qwen transcript numeric review'
            $metric = [regex]::Match([string]$scored.StdOut, '(?m)^PRIVATE_NUMBER_WER=([0-9]+(?:\.[0-9]+)?)\r?$')
            $ok = [regex]::IsMatch([string]$scored.StdOut, '(?m)^PRIVATE_RESCORE_STATUS=ok\r?$')
            if ($scored.ExitCode -eq 0 -and $ok -and $metric.Success) {
                $w = [double]::Parse($metric.Groups[1].Value, [System.Globalization.CultureInfo]::InvariantCulture)
                if ($w -ge 0 -and $w -le 10) {
                    [void][System.Windows.Forms.MessageBox]::Show(
                        ('Saved transcript number-equivalent WER: {0:N2}%. This is a supplemental local score; the original WER remains unchanged. No phone test was run.' -f ($w * 100)),
                        'Locally re-scored transcript', [System.Windows.Forms.MessageBoxButtons]::OK,
                        [System.Windows.Forms.MessageBoxIcon]::Information)
                }
            }
        } catch { Append-Log 'Optional local numeric re-score unavailable; the original private transcript remains unchanged.' }
        Start-Process -FilePath 'notepad.exe' -ArgumentList ('"{0}"' -f $entry.FullName) | Out-Null
    } catch { Show-Error $_ }
})

function Save-QwenBridgeChoices {
    try {
        $dir = Split-Path -Parent $script:QwenBridgePrefs
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        @{qnn_context=[string]$txtBridgeQnnContext.Text;
          runtime_manifest=[string]$txtBridgeQnnRuntime.Text;
          probe_archive=[string]$txtBridgeProbe.Text;
          parity_evidence=[string]$txtBridgeParityEvidence.Text;
          onnx_export=[string]$txtBridgeOnnxExport.Text;
          ground_truth=[string]$txtBridgeGroundTruth.Text;
          quant_q4=[string]$txtBridgeQuantQ4.Text; quant_q5=[string]$txtBridgeQuantQ5.Text} | ConvertTo-Json |
              Set-Content -LiteralPath $script:QwenBridgePrefs -Encoding UTF8
    } catch { Append-Log ('Bridge field save skipped: ' + $_.Exception.GetType().Name) }
}
if (Test-Path -LiteralPath $script:QwenBridgePrefs -PathType Leaf) {
    try {
        $values = Get-Content -LiteralPath $script:QwenBridgePrefs -Raw | ConvertFrom-Json
        $txtBridgeQnnContext.Text = [string]$values.qnn_context
        $txtBridgeQnnRuntime.Text = [string]$values.runtime_manifest
        $txtBridgeProbe.Text = [string]$values.probe_archive
        $txtBridgeParityEvidence.Text = [string]$values.parity_evidence
        # Older choices.json files predate this optional field. StrictMode
        # prohibits reading a missing PSCustomObject property directly.
        $savedOnnxProperty = $values.PSObject.Properties['onnx_export']
        if ($null -ne $savedOnnxProperty) {
            $txtBridgeOnnxExport.Text = [string]$savedOnnxProperty.Value
        }
        $savedTruth = $values.PSObject.Properties['ground_truth']
        if ($null -ne $savedTruth) { $txtBridgeGroundTruth.Text = [string]$savedTruth.Value }
        $savedQ4 = $values.PSObject.Properties['quant_q4']
        if ($null -ne $savedQ4) { $txtBridgeQuantQ4.Text = [string]$savedQ4.Value }
        $savedQ5 = $values.PSObject.Properties['quant_q5']
        if ($null -ne $savedQ5) { $txtBridgeQuantQ5.Text = [string]$savedQ5.Value }
    } catch { Append-Log 'Previous bridge file choices are unreadable; browse again.' }
}
# The original bridge used Get-LocalBuilderPython, which prioritizes the bare
# portable CPython runtime. That interpreter may lack NumPy even when the
# Builder's already-installed qai-hub environment has it. Qualify the exact
# runner imports before attempting ANY QNN/ADB staging. No auto-installs.
function Get-QwenBridgePython {
    param([switch]$RequireOnnx)
    $candidates = @(
        (Join-Path $Root 'tools\qai-hub-env\Scripts\python.exe'),
        ([string]$txtPython.Text),
        (Join-Path $Root 'tools\qwen3-asr-env\Scripts\python.exe'),
        (Join-Path $Root 'tools\qwen-hybrid-env\Scripts\python.exe'),
        (Join-Path $Root 'tools\parakeet-export-env\Scripts\python.exe'),
        (Join-Path $Root 'tools\python311\python.exe')
    )
    $seen = @{}
    $missing = @()
    foreach ($candidate in $candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate) -or $seen.ContainsKey($candidate)) { continue }
        $seen[$candidate] = $true
        if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) { continue }
        $dependencyCommand = if ($RequireOnnx) { 'deps-parity' } else { 'deps' }
        $check = Invoke-ProcessCapture -FilePath $candidate -Arguments @('-u', $script:QwenBridgePhonePath, $dependencyCommand) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
        if ($check.ExitCode -eq 0 -and $check.StdOut -match 'BRIDGE_DEPENDENCIES=ready') {
            Append-Log ('Qwen bridge: reusing dependency-qualified Builder Python at ' + $candidate)
            return $candidate
        }
        $reason = [regex]::Match($check.StdOut, 'BRIDGE_DEPENDENCIES=(missing:[A-Za-z0-9_.]+|invalid:[A-Za-z0-9_]+)')
        if ($reason.Success) { $missing += $reason.Groups[1].Value }
    }
    $detail = if ($missing.Count -gt 0) { ($missing | Select-Object -Unique) -join ', ' } else { 'no compatible installed Python environment' }
    throw ('Qwen bridge cannot start: required local Python modules are unavailable (' + $detail + '). No phone files were staged. Reuse or repair your existing Qwen ONNX Builder environment. Do not rebuild the APK.')
}

# This is a local, read-only lookup. It never exports models, downloads weights,
# calls ADB or stages a phone file. Reject partial exports at the GUI boundary.
function Test-QwenBridgeOnnxFolder {
    param([string]$Folder)
    if ([string]::IsNullOrWhiteSpace($Folder) -or
        -not (Test-Path -LiteralPath $Folder -PathType Container)) { return $false }
    foreach ($part in @('decoder_init.onnx', 'decoder_init.onnx.data',
            'decoder_step.onnx', 'decoder_step.onnx.data', 'embed_tokens.bin',
            'tokenizer.json', 'config.json')) {
        if (-not (Test-Path -LiteralPath (Join-Path $Folder $part) -PathType Leaf)) { return $false }
    }
    return $true
}
function Resolve-QwenBridgeOnnxExport {
    param([switch]$AllowMissing)
    # The user may already have selected Stage C's original ONNX export.
    # Prefer the 0.6B canonical location over the currently selected 1.7B model.
    $canonical = Join-Path $script:QwenAsrWorkspace '0.6b\onnx-fp32'
    $candidates = @([string]$txtBridgeOnnxExport.Text,
                    [string]$canonical)
    foreach ($candidate in $candidates) {
        if (Test-QwenBridgeOnnxFolder -Folder $candidate) {
            $txtBridgeOnnxExport.Text = $candidate
            Save-QwenBridgeChoices
            return $true
        }
    }
    # Do not run a drive-wide search or silently rebuild multi-GB exports.
    $py = Get-LocalBuilderPython
    $args = @('-u', $script:QwenBridgeReferencePath, '--root', $Root)
    $downloads = Join-Path $env:USERPROFILE 'Downloads'
    if (-not [string]::IsNullOrWhiteSpace($downloads)) { $args += @('--downloads', $downloads) }
    $lookup = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1'} -TimeoutSeconds 60
    if ($lookup.ExitCode -eq 0) {
        $match = [regex]::Match($lookup.StdOut, '(?m)^ONNX_EXPORT=(.+)$')
        if ($match.Success) {
            $found = $match.Groups[1].Value.Trim()
            if ($found -ne 'NOT_FOUND' -and (Test-QwenBridgeOnnxFolder -Folder $found)) {
                $txtBridgeOnnxExport.Text = $found
                Save-QwenBridgeChoices
                Append-Log 'Found complete local Qwen3-ASR ONNX export; no re-export needed.'
                return $true
            }
        }
    }
    if ($AllowMissing) {
        $lblBridgeStatus.Text = 'Original 0.6B ONNX decoder export not found. Browse to completed Stage C onnx-fp32 folder (7 files) before instrumented parity. No phone staging.'
        return $false
    }
    $missing = @()
    $selected = ([string]$txtBridgeOnnxExport.Text).Trim()
    if (-not [string]::IsNullOrWhiteSpace($selected) -and
        (Test-Path -LiteralPath $selected -PathType Container)) {
        foreach ($file in @('decoder_init.onnx', 'decoder_init.onnx.data',
                'decoder_step.onnx', 'decoder_step.onnx.data', 'embed_tokens.bin',
                'tokenizer.json', 'config.json')) {
            if (-not (Test-Path -LiteralPath (Join-Path $selected $file) -PathType Leaf)) {
                $missing += $file
            }
        }
    }
    if ($missing.Count -gt 0) {
        throw ('Selected ONNX folder is incomplete: missing ' + ($missing -join ', ') + '. Browse to the original completed 0.6B Stage C onnx-fp32 folder. No phone files were staged.')
    }
    throw 'Original 0.6B ONNX decoder export not found in saved selection, Stage C or bounded Builder workspace search. Browse to a completed Stage C onnx-fp32 folder; do not re-export automatically. No phone files were staged.'
}

function Find-QwenBridgeExistingFiles {
    $py = Get-LocalBuilderPython
    $downloads = Join-Path $env:USERPROFILE 'Downloads'
    # Blank Browse fields are expected on first launch.  Passing an empty
    # PowerShell argument to Invoke-ProcessCapture drops the argument value,
    # leaving Python argparse with a dangling --context/--runtime/--probe.
    # Omit absent optional flags entirely; Python supplies empty defaults.
    $args = @('-u', $script:QwenBridgeDiscoveryPath, 'discover', '--root', $Root)
    if (-not [string]::IsNullOrWhiteSpace($txtBridgeQnnContext.Text)) {
        $args += @('--context', $txtBridgeQnnContext.Text)
    }
    if (-not [string]::IsNullOrWhiteSpace($txtBridgeQnnRuntime.Text)) {
        $args += @('--runtime', $txtBridgeQnnRuntime.Text)
    }
    if (-not [string]::IsNullOrWhiteSpace($txtRuntimeRoot.Text)) {
        $args += @('--runtime-root', $txtRuntimeRoot.Text)
    }
    if (-not [string]::IsNullOrWhiteSpace($txtBridgeProbe.Text)) {
        $args += @('--probe', $txtBridgeProbe.Text)
    }
    if (-not [string]::IsNullOrWhiteSpace($downloads)) {
        $args += @('--downloads', $downloads)
    }
    $result = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 90
    if ($result.ExitCode -ne 0) { throw 'Existing-file discovery failed; see Builder log or browse manually.' }
    $found = $result.StdOut.Trim() | ConvertFrom-Json
    if ([string]$found.status -eq 'error') { throw 'Could not inspect existing bridge files.' }
    $txtBridgeQnnContext.Text = [string]$found.qnn_context
    $txtBridgeQnnRuntime.Text = [string]$found.runtime_manifest
    $txtBridgeProbe.Text = [string]$found.probe_archive
    # ONNX availability is independent of encoder/runtime/probe selection.
    # Resolve on every explicit Find operation even when those three exist.
    $onnxReady = Resolve-QwenBridgeOnnxExport -AllowMissing
    Save-QwenBridgeChoices
    $missing = @()
    if (-not $found.context_found) { $missing += 'encoder context' }
    if (-not $found.runtime_found) { $missing += 'Android QNN runtime' }
    if (-not $found.probe_found) { $missing += 'GitHub probe artifact' }
    if ($missing.Count -eq 0) {
        if ($onnxReady) {
            $lblBridgeStatus.Text = 'Bridge files and the local Stage C ONNX export found. Instrumented parity may proceed after remaining preflight checks.'
        } else {
            $lblBridgeStatus.Text = 'QNN/runtime/probe files located, but 0.6B ONNX export is still missing. Browse to completed Stage C onnx-fp32 before pressing instrumented parity; no phone work performed.'
        }
    } else {
        $lblBridgeStatus.Text = 'Found available files. Still missing: ' + ($missing -join ', ') + '. Use Browse only for missing files; no new model download is required.'
    }
}
function Resolve-QwenBridgeCpuTuneProbe {
    # Probe capability is verified locally and independently by the benchmark
    # runner before any phone staging. Old 0.4.82 probes are not suitable.
    $py = Get-LocalBuilderPython
    $args = @('-u', $script:QwenBridgeDiscoveryPath, 'find-probe',
        '--root', $Root, '--require-cpu-tune')
    $current = ([string]$txtBridgeProbe.Text).Trim()
    if (-not [string]::IsNullOrWhiteSpace($current)) { $args += @('--probe', $current) }
    $downloads = Join-Path $env:USERPROFILE 'Downloads'
    if (-not [string]::IsNullOrWhiteSpace($downloads)) {
        $args += @('--downloads', $downloads)
    }
    $result = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{
        'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'
    } -TimeoutSeconds 90
    if ($result.ExitCode -ne 0) {
        throw 'CPU-tune artifact discovery failed locally. Use Browse to select the GitHub-built v0.4.88+ native probe ZIP. No phone files staged.'
    }
    $found = $result.StdOut.Trim() | ConvertFrom-Json
    $probeState = [string]$found.selected_probe_status
    if ($probeState -notin @('not_selected','selected_file_missing','invalid_probe_archive_or_elf','checksum_mismatch','legacy_no_cpu_tune_protocol','compatible_cpu_tuned')) {
        $probeState = 'unknown'
    }
    $nChecked = [int]$found.candidate_count
    Append-Log ('CPU_PROBE_PREFLIGHT selected_status='+$probeState+' candidate_count='+$nChecked+' compatible='+[bool]$found.cpu_tune_probe_found)
    if (-not $found.cpu_tune_probe_found) {
        $hint = switch ($probeState) {
            'legacy_no_cpu_tune_protocol' { 'Selected native artifact is an OLD bridge probe without v0.4.88 CPU thread tuning.' }
            'checksum_mismatch' { 'Selected native ZIP has a SHA-256 receipt mismatch; do not run it.' }
            'invalid_probe_archive_or_elf' { 'Selected native ZIP is not a valid GitHub ARM64 probe artifact.' }
            'selected_file_missing' { 'Previously selected native probe is missing.' }
            default { 'No compatible CPU-tuned ARM64 probe was found locally.' }
        }
        $lblBridgeStatus.Text = $hint + ' Download the artifact from the updated native-only GitHub Actions workflow (not APK), then select the ZIP with Browse. No phone staging.'
        return $false
    }
    if ([string]$txtBridgeProbe.Text -ne [string]$found.probe_archive) {
        $txtBridgeProbe.Text = [string]$found.probe_archive
        Save-QwenBridgeChoices
        Append-Log 'CPU-tuned ARM64 probe located and selected using exact protocol marker and verified ZIP hash if provided. No phone staging.'
    }
    $lblBridgeStatus.Text = 'CPU-tuned ARM64 artifact found locally. Native identity is checked again when the phone test starts.'
    return $true
}
$btnBridgeCpuProbe.Add_Click({
    try {
        if (-not (Resolve-QwenBridgeCpuTuneProbe)) {
            [System.Windows.Forms.MessageBox]::Show(
                'No compatible v0.4.88+ CPU-tuned probe was found. Check the status text for whether the selected ZIP is old, invalid or missing. Download the NEW native-only qwen-android-arm64-probe artifact from GitHub Actions, then select it using the Browse button by the probe field. No phone files were staged.',
                'CPU-tuned native probe not found',
                [System.Windows.Forms.MessageBoxButtons]::OK,
                [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        }
    } catch { Show-Error $_ }
})

$btnBridgeAutoFind.Add_Click({
    try { Find-QwenBridgeExistingFiles }
    catch { Show-Error $_ }
})
$btnBridgeQnnContext.Add_Click({ Choose-File $txtBridgeQnnContext 'Select original compiled Qwen S26 QNN encoder context' 'Binary (*.bin)|*.bin|All files (*.*)|*.*'; Save-QwenBridgeChoices })
$btnBridgeQnnRuntime.Add_Click({ Choose-File $txtBridgeQnnRuntime 'Select your existing Android QNN runtime paths JSON' 'JSON (*.json)|*.json'; Save-QwenBridgeChoices })
$btnBridgeProbe.Add_Click({ Choose-File $txtBridgeProbe 'Select GitHub ARM64 bridge probe artifact ZIP' 'ZIP (*.zip)|*.zip|All files (*.*)|*.*'; Save-QwenBridgeChoices })
$btnBridgeResults.Add_Click({
    New-Item -ItemType Directory -Force -Path $script:QwenBridgePhoneResults | Out-Null
    Open-Folder $script:QwenBridgePhoneResults
})
$btnBridgeGroundTruth.Add_Click({ Choose-File $txtBridgeGroundTruth 'Select manually checked transcript matching FIRST 12s ONLY' 'Text files (*.txt)|*.txt'; Save-QwenBridgeChoices })
$btnBridgePrepareTruth.Add_Click({
    Invoke-UiAction 'Preparing a private 12-second listening clip; no phone test...' {
        if ([string]::IsNullOrWhiteSpace($txtPhoneAudio.Text) -or
            -not (Test-Path -LiteralPath $txtPhoneAudio.Text -PathType Leaf)) {
            throw 'Choose a recording on the Qwen NPU Bridge tab first. No phone files staged.'
        }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        $py = Get-LocalBuilderPython
        $args = @('-u', $script:QwenBridgeReviewPath, 'prepare',
                  '--audio', $txtPhoneAudio.Text, '--ffmpeg', $txtFfmpeg.Text,
                  '--review-dir', $script:QwenBridgeReviewDir)
        $result = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        # Windows subprocess output can contain CRLF. In .NET regex multiline
        # mode, `$` before `\n` does not also consume the preceding `\r`.
        # Require the successful output contract, not just a zero exit status.
        $match = [regex]::Match($result.StdOut, '(?m)^REVIEW_CLIP_KEY=([a-f0-9]{20})\r?$')
        $prepared = [regex]::IsMatch($result.StdOut, '(?m)^REVIEW_STATUS=prepared_local_only\r?$')
        if ($result.ExitCode -ne 0 -or -not $match.Success -or -not $prepared) {
            if ([regex]::IsMatch($result.StdOut, '(?m)^REVIEW_ERROR=REVIEW_BUILDER_MODULE_MISMATCH\r?$')) {
                throw 'Builder Python modules are from different versions. Close Builder, extract the FULL v0.4.96 Windows ZIP over your existing Builder folder, replacing program files while preserving workspace/tools/config. No phone files staged.'
            }
            throw 'Local review preparation failed before phone staging; see REVIEW_ERROR in the local Builder log. No phone files staged.'
        }
        $key = $match.Groups[1].Value
        $wav = Join-Path $script:QwenBridgeReviewDir ('clip-' + $key + '.wav')
        $txt = Join-Path $script:QwenBridgeReviewDir ('clip-' + $key + '.txt')
        if (-not (Test-Path -LiteralPath $wav -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txt -PathType Leaf)) {
            throw 'Local reference files missing after preparation; no phone files staged.'
        }
        $txtBridgeGroundTruth.Text = $txt
        Save-QwenBridgeChoices
        $lblBridgeStatus.Text = 'Exact model-input clip ready (0:00-0:12). Listen to the WAV, check the local -window.txt if needed, then save verified words. No phone files staged.'
        try { Start-Process -FilePath 'notepad.exe' -ArgumentList ('"{0}"' -f $txt) | Out-Null }
        catch { Append-Log 'Could not open Notepad automatically. Use the selected transcript path in the field.' }
        $windowAudit = Join-Path $script:QwenBridgeReviewDir ('window-' + $key + '.txt')
        if (Test-Path -LiteralPath $windowAudit -PathType Leaf) {
            Append-Log 'First-12s local audio window proof saved next to listening WAV (contains no transcript).'
        }
        try { Start-Process -FilePath $wav | Out-Null }
        catch { Append-Log 'Could not open WAV automatically. Open the local review WAV using Explorer.' }
    }
})

# v0.4.72: playback-only volume amplification from exactly the PCM used by QNN.
# Audio sent to the model is never amplified by this button.
$btnBridgeListenLouder.Add_Click({
    try {
        $truthPath = ([string]$txtBridgeGroundTruth.Text).Trim()
        if (-not $truthPath -or -not (Test-Path -LiteralPath $truthPath -PathType Leaf)) {
            throw 'First click Prepare 12s listening clip + text file, then listen louder. No phone run is required.'
        }
        $truthFile = Get-Item -LiteralPath $truthPath -ErrorAction Stop
        if ($truthFile.Directory.FullName -ne [IO.Path]::GetFullPath($script:QwenBridgeReviewDir)) {
            throw 'Selected transcript is not inside the local Qwen review directory. Prepare the current audio first.'
        }
        if ($truthFile.Name -notmatch '^clip-([a-f0-9]{20})\.txt$') {
            throw 'Selected transcript is not a Builder-bound review clip. Prepare the current audio first.'
        }
        $key = $Matches[1]
        $boost = Join-Path $script:QwenBridgeReviewDir ('boost-' + $key + '.wav')
        $item = Get-Item -LiteralPath $boost -ErrorAction SilentlyContinue
        if ($null -eq $item -or $item.Attributes -band [System.IO.FileAttributes]::ReparsePoint -or
            $item.Length -lt 1000 -or $item.Length -gt 1000000) {
            throw 'No boosted listening WAV found. Prepare the currently selected recording again (local only).'
        }
        Start-Process -FilePath $boost | Out-Null
        Append-Log 'Opened private listening-only amplified WAV. Model PCM remains unchanged.'
    } catch { Show-Error $_ }
})

$btnBridgeParityEvidence.Add_Click({ Choose-File $txtBridgeParityEvidence 'Select optional privately held ONNX/native same-feature parity receipts' 'JSON (*.json)|*.json'; Save-QwenBridgeChoices })
$btnBridgeOnnxExport.Add_Click({
    $folder = New-Object System.Windows.Forms.FolderBrowserDialog
    $folder.Description = 'Select existing Qwen3-ASR-0.6B ONNX export with decoder_init.onnx, decoder_step.onnx, embed_tokens.bin'
    if ($folder.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        $txtBridgeOnnxExport.Text = $folder.SelectedPath
        Save-QwenBridgeChoices
    }
})
$btnBridgeInstrument.Add_Click({
    Append-Log ('QWEN_UI_ACTION=RUN_INSTRUMENTED_CLICKED; private_text_requested=' + [string][bool]$chkBridgePrivateReview.Checked)
    Invoke-UiAction 'Running NPU + native GGUF + reference ONNX private parity, then ONE gated ZIP...' {
        # Preflight EVERY prerequisite before staging anything on the phone.
        if (-not (Test-Path -LiteralPath $txtBridgeQnnContext.Text -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txtBridgeQnnRuntime.Text -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txtBridgeProbe.Text -PathType Leaf)) { Find-QwenBridgeExistingFiles }
        # Independent ONNX preflight even when all three bridge artifacts exist.
        [void](Resolve-QwenBridgeOnnxExport)
        if ([string]::IsNullOrWhiteSpace($txtPhoneAudio.Text) -or
            -not (Test-Path -LiteralPath $txtPhoneAudio.Text -PathType Leaf)) {
            throw 'Choose a recording on the Qwen NPU Bridge tab first. No phone files were staged.'
        }
        # Parity has already been demonstrated on the current fixture. Require
        # new checked transcript evidence to prevent another no-progress phone run.
        if ([string]::IsNullOrWhiteSpace($txtBridgeGroundTruth.Text)) {
            throw 'No ground-truth text selected. Use Prepare 12s listening clip + text file, type and save checked words, then run instrumented parity. No phone files were staged.'
        }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        $reviewPy = Get-LocalBuilderPython
        $reviewCheck = Invoke-ProcessCapture -FilePath $reviewPy -Arguments @('-u', $script:QwenBridgeReviewPath, 'verify-bound',
                '--audio', [string]$txtPhoneAudio.Text, '--ffmpeg', [string]$txtFfmpeg.Text,
                '--review-dir', $script:QwenBridgeReviewDir,
                '--ground-truth', [string]$txtBridgeGroundTruth.Text) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 60
        if ($reviewCheck.ExitCode -ne 0 -or $reviewCheck.StdOut -notmatch 'REVIEW_STATUS=reference_bound_to_current_audio_unverified_words') {
            if ([regex]::IsMatch($reviewCheck.StdOut, '(?m)^REVIEW_ERROR=REVIEW_BUILDER_MODULE_MISMATCH\r?$')) {
                throw 'Builder Python modules are from different versions. Close Builder, extract the FULL v0.4.96 Windows ZIP over your existing Builder folder, replacing program files while preserving workspace/tools/config. No phone files staged.'
            }
            if ([regex]::IsMatch($reviewCheck.StdOut, '(?m)^REVIEW_ERROR=REFERENCE_AUDIO_MISMATCH_REPREPARE\r?$')) {
                throw 'Reference transcript belongs to different audio. Prepare the first 12 seconds of the selected recording, review and save the words, then retry. No phone files staged.'
            }
            throw 'Reference review preflight failed. Check the REVIEW_ERROR code in local Builder logs; no phone files staged.'
        }
        # v0.4.75: no-progress repeat protection is local and BEFORE ADB.
        # Do not confuse a changed label with new inference evidence; if the
        # exact PCM was already scored, view its private local transcript first.
        # Explicit override remains available for controlled repeatability.
        if (-not $chkBridgeAllowRepeat.Checked) {
            $localPy = Get-LocalBuilderPython
            $guard = Invoke-ProcessCapture -FilePath $localPy -Arguments @('-u', $script:QwenBridgeRepeatPath,
                '--audio', [string]$txtPhoneAudio.Text, '--ffmpeg', [string]$txtFfmpeg.Text,
                '--results', $script:QwenBridgePhoneResults) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 180
            if ($guard.ExitCode -ne 0 -or -not [regex]::IsMatch($guard.StdOut, '(?m)^REPEAT_STATUS=(already_scored|new_or_unscored)\r?$')) {
                throw 'Local duplicate-audio check failed before ADB. Check FFmpeg and Builder modules; no phone files staged.'
            }
            if ([regex]::IsMatch($guard.StdOut, '(?m)^REPEAT_STATUS=already_scored\r?$')) {
                if ($chkBridgePrivateReview.Checked) {
                    # The privacy-safe ZIP never stores decoded words. If the
                    # previous run omitted the opt-in private file, the user
                    # must consent to one repeated real phone test to get it.
                    if ([bool]$chkBridgeTemperatureMatch.Checked) {
            $description += "`r`n`r`nTemperature-matched option: pause up to 4 minutes before each later profile to approach the initial BATTERY temperature (not CPU/SoC temperature). If it cannot cool, stop remaining trials and verify cleanup. This can add up to 12 minutes."
        }
        $choice = [System.Windows.Forms.MessageBox]::Show(
                        'This exact audio was scored already. Diagnostic ZIPs cannot reconstruct the transcript. Repeat ONE phone test now to save private words on this Windows computer? Existing audio and phone app data will remain untouched; the test uses only its own staged files.',
                        'Confirm repeat for private transcription',
                        [System.Windows.Forms.MessageBoxButtons]::YesNo,
                        [System.Windows.Forms.MessageBoxIcon]::Question)
                    if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) {
                        Append-Log 'QWEN_REPEAT_STATUS=user_declined_private_capture; phone_modified=false'
                        $lblBridgeStatus.Text = 'Previously scored. Private transcript recapture canceled; no phone staging.'
                        return
                    }
                    Append-Log 'QWEN_REPEAT_STATUS=explicit_private_capture_confirmed; continuing instrumented workflow'
                } else {
                    throw 'These exact first 12 seconds already have a successful token/text comparison. To save the missing private transcript, enable Save private text and confirm ONE repeat when prompted; or select a different clip. No phone files staged.'
                }
            }
        }
        if (-not $txtAdb.Text) { $txtAdb.Text = Install-AdbFromUi }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        $py = Get-QwenBridgePython -RequireOnnx
        Refresh-Device
        New-Item -ItemType Directory -Force -Path $script:QwenBridgePhoneResults | Out-Null
        $runArgs = @('-u',$script:QwenBridgePhonePath,'run',
            '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,
            '--ffmpeg',$txtFfmpeg.Text,'--audio',$txtPhoneAudio.Text,
            '--qnn-context',$txtBridgeQnnContext.Text,
            '--runtime-manifest',$txtBridgeQnnRuntime.Text,
            '--cache',$script:LlamaPhoneBenchCache,
            '--probe-archive',$txtBridgeProbe.Text,
            '--results',$script:QwenBridgePhoneResults,'--threads','4','--steps','48',
            '--onnx-export',$txtBridgeOnnxExport.Text)
        if (-not [string]::IsNullOrWhiteSpace($txtBridgeGroundTruth.Text)) {
            if (-not (Test-Path -LiteralPath $txtBridgeGroundTruth.Text -PathType Leaf)) {
                throw 'Selected first-12s ground-truth text file missing; no phone work performed.'
            }
            $runArgs += @('--ground-truth', [string]$txtBridgeGroundTruth.Text)
        }
        if ($chkBridgePrivateReview.Checked) {
            $runArgs += @('--private-review-dir', $script:QwenBridgeReviewDir)
        }
        $run = Invoke-ProcessCapture -FilePath $py -Arguments $runArgs -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'PYTHONUNBUFFERED'='1'} -TimeoutSeconds 3600
        $foundZip = [regex]::Matches($run.StdOut,'(?m)^DIAGNOSTIC_ZIP=(.+)$')
        if ($foundZip.Count -ne 1) { throw 'Instrumented run did not emit one phone receipt. Review Builder logs.' }
        if ($chkBridgePrivateReview.Checked) {
            $localReview = [regex]::Matches($run.StdOut,'(?m)^LOCAL_REVIEW_FILE=(.+)\r?$')
            if ($localReview.Count -eq 1) {
                $privateTxt = $localReview[0].Groups[1].Value.Trim()
                if (Test-Path -LiteralPath $privateTxt -PathType Leaf) {
                    try { Start-Process -FilePath 'notepad.exe' -ArgumentList ('"{0}"' -f $privateTxt) | Out-Null }
                    catch { Append-Log 'Private comparison file was saved locally; open it from the review folder.' }
                }
            }
        }
        $bridgeZip = $foundZip[0].Groups[1].Value.Trim()
        if (-not (Test-Path -LiteralPath $bridgeZip -PathType Leaf)) { throw 'Instrumented bridge receipt missing.' }
        # Local-only link from the most recent actual run to its opt-in text.
        # Always update it (even when private saving was off) to prevent the
        # viewer from quietly displaying words from an earlier test.
        $reviewName = ''
        if ($chkBridgePrivateReview.Checked) {
            $localReview = [regex]::Matches($run.StdOut,'(?m)^LOCAL_REVIEW_FILE=(.+)\r?$')
            if ($localReview.Count -eq 1) {
                $candidate = $localReview[0].Groups[1].Value.Trim()
                $candidateName = [IO.Path]::GetFileName($candidate)
                if ($candidateName -cmatch '^private-asr-[a-f0-9]{16}\.txt$' -and
                    [IO.Path]::GetFullPath([IO.Path]::GetDirectoryName($candidate)) -eq
                    [IO.Path]::GetFullPath($script:QwenBridgeReviewDir) -and
                    (Test-Path -LiteralPath $candidate -PathType Leaf)) {
                    $reviewName = $candidateName
                }
            }
        }
        $receiptName = [IO.Path]::GetFileName($bridgeZip)
        if ($receiptName -notmatch '^WhisperRecorder-Qwen-NPU-GGUF-Bridge-[0-9]{8}-[0-9]{6}-[a-f0-9]{6}\.zip$') {
            throw 'Instrumented bridge report name was unexpected; private review link not updated.'
        }
        New-Item -ItemType Directory -Force -Path $script:QwenBridgeReviewDir | Out-Null
        $reviewIndex = Join-Path $script:QwenBridgeReviewDir 'latest-private-asr-run.json'
        if ((Test-Path -LiteralPath $reviewIndex) -and
            ((Get-Item -LiteralPath $reviewIndex).Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
            throw 'Private review index is a linked file; refusing to replace it.'
        }
        @{kind='qwen_private_review_latest_run_v1'; bridge_receipt=$receiptName;
          completed=($run.ExitCode -eq 0); saved=($reviewName.Length -gt 0);
          review_file=$reviewName} | ConvertTo-Json -Compress |
            Set-Content -LiteralPath $reviewIndex -Encoding UTF8
        $auditArgs = @('-u',$script:QwenBridgeBatchPath,'--results',$script:QwenBridgePhoneResults,
                       '--bridge-zip',$bridgeZip)
        $audit = Invoke-ProcessCapture -FilePath $py -Arguments $auditArgs -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        $auditZip = [regex]::Matches($audit.StdOut,'(?m)^DIAGNOSTIC_ZIP=(.+)$')
        if ($audit.ExitCode -ne 0 -or $auditZip.Count -ne 1) { throw 'Parity batch report generation failed; original phone receipt preserved.' }
        $out = $auditZip[0].Groups[1].Value.Trim()
        Append-Log ('ONE consolidated qualification ZIP: ' + $out)
        $lblBridgeStatus.Text = 'Token/text comparison measured. Check WER and review transcript mismatches; M-RoPE and multi-clip accuracy remain gated.'
        Save-QwenBridgeChoices
        try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $out) | Out-Null }
        catch { Open-Folder $script:QwenBridgePhoneResults }
        if ($run.ExitCode -ne 0) { throw 'Instrumented phone execution failed or cleanup was unverified; see the consolidated ZIP.' }
    }
})
$btnBridgeBatch.Add_Click({
    Invoke-UiAction 'Running complete Qwen bridge gate audit (one offline ZIP, no phone writes)...' {
        New-Item -ItemType Directory -Force -Path $script:QwenBridgePhoneResults | Out-Null
        # This audit uses stdlib only. It never uses ADB, downloads, or recompiles.
        $py = Get-LocalBuilderPython
        $args = @('-u', $script:QwenBridgeBatchPath, '--results', $script:QwenBridgePhoneResults)
        if (-not [string]::IsNullOrWhiteSpace($txtBridgeParityEvidence.Text)) {
            if (-not (Test-Path -LiteralPath $txtBridgeParityEvidence.Text -PathType Leaf)) {
                throw 'Selected parity evidence file is missing; clear the optional field or browse again.'
            }
            $args += @('--parity-evidence', $txtBridgeParityEvidence.Text)
        }
        # Both historical comparison ZIPs are OPTIONAL for the offline batch.
        # PowerShell 5.1 rejects Test-Path -LiteralPath '' before running Python.
        # Check for blank fields first (short-circuit -and), then for actual files.
        $historicalStandalone = [string]$txtQwenBridgeBenchmark.Text
        $historicalHybrid = [string]$txtQwenBridgeHybrid.Text
        if (-not [string]::IsNullOrWhiteSpace($historicalStandalone) -and
            -not [string]::IsNullOrWhiteSpace($historicalHybrid) -and
            (Test-Path -LiteralPath $historicalStandalone -PathType Leaf) -and
            (Test-Path -LiteralPath $historicalHybrid -PathType Leaf)) {
            $args += @('--standalone-zip', $historicalStandalone,
                        '--hybrid-zip', $historicalHybrid)
        }
        $result = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($result.ExitCode -ne 0) { throw 'Batch qualifier failed before emitting a report. Check Builder log.' }
        $matched = [regex]::Matches($result.StdOut, '(?m)^DIAGNOSTIC_ZIP=(.+)$')
        if ($matched.Count -ne 1) { throw 'Batch qualifier did not return one diagnostic ZIP.' }
        $outZip = $matched[0].Groups[1].Value.Trim()
        if (-not (Test-Path -LiteralPath $outZip -PathType Leaf)) { throw 'Batch qualification ZIP was not saved.' }
        Append-Log ('Qwen qualification ZIP: ' + $outZip)
        $verdict = [regex]::Match($result.StdOut, '(?m)^BATCH_VERDICT=(\S+)')
        $lblBridgeStatus.Text = 'Batch report: ' + $verdict.Groups[1].Value + '. View the ZIP gate table. No phone files touched.'
        Save-QwenBridgeChoices
        try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $outZip) | Out-Null }
        catch { Open-Folder $script:QwenBridgePhoneResults }
    }
})
$btnBridgeTest.Add_Click({
    Invoke-UiAction 'Executing REAL QNN encoder + native GGUF mixed-batch decoder (12s only)...' {
        if (-not (Test-Path -LiteralPath $txtBridgeQnnContext.Text -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txtBridgeQnnRuntime.Text -PathType Leaf) -or
            -not (Test-Path -LiteralPath $txtBridgeProbe.Text -PathType Leaf)) {
            Find-QwenBridgeExistingFiles
        }
        if (-not (Test-Path -LiteralPath $txtPhoneAudio.Text -PathType Leaf)) { throw 'Choose audio on the Qwen NPU Bridge tab first.' }
        if (-not $txtAdb.Text) { $txtAdb.Text = Install-AdbFromUi }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        Refresh-Device
        New-Item -ItemType Directory -Force -Path $script:QwenBridgePhoneResults | Out-Null
        $py = Get-QwenBridgePython
        $args = @('-u',$script:QwenBridgePhonePath,'run',
                  '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,
                  '--ffmpeg',$txtFfmpeg.Text,'--audio',$txtPhoneAudio.Text,
                  '--qnn-context',$txtBridgeQnnContext.Text,
                  '--runtime-manifest',$txtBridgeQnnRuntime.Text,
                  '--cache',$script:LlamaPhoneBenchCache,
                  '--probe-archive',$txtBridgeProbe.Text,
                  '--results',$script:QwenBridgePhoneResults,'--threads','4','--steps','48')
        $r = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'PYTHONUNBUFFERED'='1'} -TimeoutSeconds 3000
        $m = [regex]::Matches($r.StdOut,'(?m)DIAGNOSTIC_ZIP=(.+)$')
        if ($m.Count -gt 0) {
            $zip = $m[$m.Count-1].Groups[1].Value.Trim()
            Append-Log ('Real QNN-to-GGUF probe report: ' + $zip)
            if (Test-Path -LiteralPath $zip -PathType Leaf) {
                try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $zip) | Out-Null }
                catch { Open-Folder $script:QwenBridgePhoneResults }
            }
        }
        if ($r.ExitCode -ne 0) {
            $lblBridgeStatus.Text = 'Bridge experiment blocked or failed. Review ZIP; verify phone cleanup before retrying.'
            throw 'QNN-to-GGUF bridge probe did not pass; open automatic ZIP. No APK change was attempted.'
        }
        $lblBridgeStatus.Text = 'NPU features reached native GGUF decoder. Positional and full transcript parity remain UNQUALIFIED.'
        Save-QwenBridgeChoices
    }
})
$btnBridgeRecover.Add_Click({
    $choice = [System.Windows.Forms.MessageBox]::Show('Retry cleanup only of the EXACT QNN bridge directory recorded by this Builder for this phone? No other files will be removed.','Qwen bridge targeted recovery',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'Verifying and cleaning recorded QNN bridge staging...' {
        Refresh-Device
        $py = Get-LocalBuilderPython
        $r = Invoke-ProcessCapture -FilePath $py -Arguments @('-u',$script:QwenBridgePhonePath,'recover','--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--results',$script:QwenBridgePhoneResults) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 180
        if ($r.ExitCode -ne 0) { throw 'Qwen bridge targeted cleanup is not verified. Preserve receipt for recovery.' }
        $lblBridgeStatus.Text = 'Exact QNN bridge phone staging removed and independently verified.'
    }
})

# -------------------- Logs --------------------
$tabLogs = New-Object System.Windows.Forms.TabPage
$tabLogs.Text = 'Logs'
[void]$tabs.TabPages.Add($tabLogs)
$logToolbar = New-Object System.Windows.Forms.Panel
$logToolbar.Dock = 'Top'
$logToolbar.Height = 42
$tabLogs.Controls.Add($logToolbar)
$btnClearLog = New-Button 'Clear' 8 6 80 28
$logToolbar.Controls.Add($btnClearLog)
$btnOpenWorkspaceFromLog = New-Button 'Open workspace' 100 6 130 28
$logToolbar.Controls.Add($btnOpenWorkspaceFromLog)
$btnCreateSupport = New-Button 'Create support ZIP' 242 6 150 28
$logToolbar.Controls.Add($btnCreateSupport)
$btnOpenLogFolder = New-Button 'Open log folder' 404 6 130 28
$logToolbar.Controls.Add($btnOpenLogFolder)
$btnDeleteSupportZips = New-Button 'Delete diagnostic ZIPs' 546 6 177 28
$logToolbar.Controls.Add($btnDeleteSupportZips)
$txtLog = New-Object System.Windows.Forms.RichTextBox
$txtLog.Dock = 'Fill'
$txtLog.ReadOnly = $true
$txtLog.Font = New-Object -TypeName System.Drawing.Font -ArgumentList @('Consolas',[single]9)
$tabLogs.Controls.Add($txtLog)
$logToolbar.BringToFront()


# -------------------- Persistent navigation --------------------
$script:NavSyncing = $false
foreach ($page in $tabs.TabPages) { [void]$cmbNavPage.Items.Add($page.Text) }

function Update-WorkflowNavigation {
    if ($tabs.SelectedIndex -lt 0) { return }
    $script:NavSyncing = $true
    try {
        $cmbNavPage.SelectedIndex = $tabs.SelectedIndex
        $lblNavCurrent.Text = 'Current: ' + $tabs.SelectedTab.Text
        $btnNavPrev.Enabled = ($tabs.SelectedIndex -gt 0)
        $btnNavNext.Enabled = ($tabs.SelectedIndex -lt ($tabs.TabPages.Count - 1))
    } finally {
        $script:NavSyncing = $false
    }
}

$tabs.Add_SelectedIndexChanged({
    Update-WorkflowNavigation
    if ($tabs.SelectedTab -eq $tabDeploy) { Refresh-DeployArtifacts }
    if ($tabs.SelectedTab -eq $tabCloud) { Update-CloudWorkflowState }
    if ($tabs.SelectedTab -eq $tabQwenBridge -and -not $script:QwenBridgeAutofindTried) {
        $script:QwenBridgeAutofindTried = $true
        try { Find-QwenBridgeExistingFiles } catch { Append-Log ('Bridge auto-find unavailable: ' + $_.Exception.GetType().Name) }
    }
})
$btnNavHome.Add_Click({ $tabs.SelectedTab = $tabDashboard })
$btnNavPrev.Add_Click({
    if ($tabs.SelectedIndex -gt 0) { $tabs.SelectedIndex = $tabs.SelectedIndex - 1 }
})
$btnNavNext.Add_Click({
    if ($tabs.SelectedIndex -lt ($tabs.TabPages.Count - 1)) { $tabs.SelectedIndex = $tabs.SelectedIndex + 1 }
})
$cmbNavPage.Add_SelectedIndexChanged({
    if (-not $script:NavSyncing -and $cmbNavPage.SelectedIndex -ge 0 -and $cmbNavPage.SelectedIndex -lt $tabs.TabPages.Count) {
        $tabs.SelectedIndex = $cmbNavPage.SelectedIndex
    }
})
Update-WorkflowNavigation

$txtSetupToken.Add_TextChanged({
    if ($txtDashToken.Text -ne $txtSetupToken.Text) { $txtDashToken.Text = $txtSetupToken.Text }
    if ($txtToken.Text -ne $txtSetupToken.Text) { $txtToken.Text = $txtSetupToken.Text }
})
$txtDashToken.Add_TextChanged({
    if ($txtSetupToken.Text -ne $txtDashToken.Text) { $txtSetupToken.Text = $txtDashToken.Text }
    if ($txtToken.Text -ne $txtDashToken.Text) { $txtToken.Text = $txtDashToken.Text }
})
$txtToken.Add_TextChanged({
    if ($txtSetupToken.Text -ne $txtToken.Text) { $txtSetupToken.Text = $txtToken.Text }
    if ($txtDashToken.Text -ne $txtToken.Text) { $txtDashToken.Text = $txtToken.Text }
})

# -------------------- Phone Benchmarks events --------------------
$btnPhoneAudio.Add_Click({ Choose-File $txtPhoneAudio 'Select reference audio for llama.cpp test' 'Audio (*.wav;*.m4a;*.mp3;*.ogg)|*.wav;*.m4a;*.mp3;*.ogg|All files (*.*)|*.*' })
$btnPhoneConnect.Add_Click({
    Invoke-UiAction 'Connecting Galaxy S26 for phone benchmark...' {
        if (-not $txtAdb.Text) { $txtAdb.Text = Install-AdbFromUi }
        Refresh-Device
        $py = Get-LocalBuilderPython
        $r = Invoke-ProcessCapture -FilePath $py -Arguments @('-u',$script:LlamaPhoneBenchPath,'probe','--adb',$txtAdb.Text,'--serial',$script:DeviceSerial) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 60
        if ($r.ExitCode -ne 0) { throw ('Phone probe failed: ' + $r.StdErr + $r.StdOut) }
        $lblPhoneStatus.Text = 'Connected: ' + $script:DeviceSerial + '. Select recording and Run phone ASR test.'
    }
})
$btnPhonePrepare.Add_Click({
    Invoke-UiAction 'Downloading/verifying pinned official llama.cpp CPU and Q8_0 GGUF (once)...' {
        $py = Get-LocalBuilderPython
        $r = Invoke-ProcessCapture -FilePath $py -Arguments @('-u',$script:LlamaPhoneBenchPath,'prepare','--release',$txtPhoneRelease.Text,'--cache',$script:LlamaPhoneBenchCache) -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'PYTHONUNBUFFERED'='1'} -TimeoutSeconds 7200
        if ($r.ExitCode -ne 0) { throw ('Official files preflight failed: ' + $r.StdErr + $r.StdOut) }
        $lblPhoneStatus.Text = 'Official release and GGUF prepared. Press Run phone ASR test.'
    }
})
$btnPhoneRun.Add_Click({
    Invoke-UiAction 'Running native Qwen ASR on Galaxy S26 (automatic diagnostics)...' {
        if (-not (Test-Path -LiteralPath $txtPhoneAudio.Text -PathType Leaf)) { throw 'Choose a reference audio file first.' }
        if (-not $txtAdb.Text) { $txtAdb.Text = Install-AdbFromUi }
        if (-not $txtFfmpeg.Text) { $txtFfmpeg.Text = Install-FfmpegFromUi }
        Refresh-Device
        $seconds = if ($cmbPhoneSeconds.SelectedIndex -eq 0) { '12' } else { '0' }
        $timeout = if ($seconds -eq '12') { 900 } else { 3600 }
        $py = Get-LocalBuilderPython
        $args = @('-u',$script:LlamaPhoneBenchPath,'run','--release',$txtPhoneRelease.Text,
                  '--cache',$script:LlamaPhoneBenchCache,'--results',$script:LlamaPhoneBenchResults,
                  '--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,
                  '--ffmpeg',$txtFfmpeg.Text,'--audio',$txtPhoneAudio.Text,
                  '--seconds',$seconds,'--threads',[string]$cmbPhoneThreads.SelectedItem,
                  '--timeout',[string]$timeout)
        $r = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8';'PYTHONUNBUFFERED'='1'} -TimeoutSeconds ($timeout + 2400)
        $phoneRunZip = ''
        $m = [regex]::Matches($r.StdOut,'(?m)DIAGNOSTIC_ZIP=(.+)$')
        if ($m.Count -gt 0) {
            $zip = $m[$m.Count - 1].Groups[1].Value.Trim()
            $phoneRunZip = $zip
            if (Test-Path -LiteralPath $zip -PathType Leaf) {
                Append-Log ('Phone benchmark ZIP: ' + $zip)
                try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $zip) | Out-Null }
                catch { Open-Folder $script:LlamaPhoneBenchResults }
            }
        }
        if ($r.ExitCode -ne 0) {
            $lblPhoneStatus.Text = 'Native phone test failed; diagnostic ZIP saved. See console and results.'
            throw 'Phone ASR test failed. Do not repeat full-length tests before reviewing the diagnostic ZIP.'
        }
        $lblPhoneStatus.Text = 'Phone ASR test finished; verified phone cleanup, closed ADB forwarding and transcripts are recorded in ZIP.'
        # Optional same-click local comparison: no second ADB request, no model transfers.
        if ($seconds -eq '0' -and $phoneRunZip -and
            (Test-Path -LiteralPath $phoneRunZip -PathType Leaf) -and
            (Test-Path -LiteralPath $txtQwenBridgeHybrid.Text -PathType Leaf)) {
            $auditArgs = @('-u',$script:QwenHybridQualifierPath,'audit','--benchmark',$phoneRunZip,
                           '--hybrid',$txtQwenBridgeHybrid.Text,'--results',$script:LlamaPhoneBenchResults)
            $gguf = Join-Path $script:LlamaPhoneBenchCache 'models\Qwen3-ASR-0.6B-Q8_0.gguf'
            if (Test-Path -LiteralPath $gguf -PathType Leaf) { $auditArgs += @('--gguf',$gguf) }
            $audit = Invoke-ProcessCapture -FilePath $py -Arguments $auditArgs -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
            $a = [regex]::Matches($audit.StdOut,'(?m)DIAGNOSTIC_ZIP=(.+)$')
            if ($a.Count -gt 0) {
                $auditZip = $a[$a.Count-1].Groups[1].Value.Trim()
                Append-Log ('Qwen readiness report: ' + $auditZip)
                if (Test-Path -LiteralPath $auditZip -PathType Leaf) {
                    try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $auditZip) | Out-Null }
                    catch { Open-Folder $script:LlamaPhoneBenchResults }
                }
            }
            if ($audit.ExitCode -ne 0) {
                Append-Log 'Readiness comparison incomplete or blocked; source benchmark ZIP remains saved. No hybrid execution was attempted.'
            } else {
                $lblPhoneStatus.Text = 'Full benchmark and offline Qwen hybrid readiness report saved. Native bridge remains unqualified.'
            }
        }
        Save-Settings
    }
})
$btnPhoneResults.Add_Click({
    try {
        New-Item -ItemType Directory -Path $script:LlamaPhoneBenchResults -Force | Out-Null
        Open-Folder $script:LlamaPhoneBenchResults
    } catch { Show-Error $_ }
})
$btnPhoneCleanup.Add_Click({
    $choice = [System.Windows.Forms.MessageBox]::Show('Retry deleting only /data/local/tmp/wr-llama-bench-v1 after an interrupted benchmark? Q8_0 GGUF and binaries should already be removed after every completed test.','Phone benchmark cache cleanup',[System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
    if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
    Invoke-UiAction 'Removing scoped benchmark cache from phone...' {
        Refresh-Device
        $py = Get-LocalBuilderPython
        $r = Invoke-ProcessCapture -FilePath $py -Arguments @('-u',$script:LlamaPhoneBenchPath,'cleanup','--adb',$txtAdb.Text,'--serial',$script:DeviceSerial,'--confirm-owned-only') -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        if ($r.ExitCode -ne 0) { throw ('Benchmark cache cleanup verification failed: ' + $r.StdErr + $r.StdOut) }
        $lblPhoneStatus.Text = 'Only benchmark-owned phone cache removed; app and model files preserved.'
    }
})

# Read-only qualification of actual native and existing hybrid reports.
$btnQwenBridgeBenchmark.Add_Click({ Choose-File $txtQwenBridgeBenchmark 'Select completed llama.cpp phone benchmark ZIP' 'Diagnostic ZIP (*.zip)|*.zip' })
$btnQwenBridgeHybrid.Add_Click({ Choose-File $txtQwenBridgeHybrid 'Select completed Qwen NPU hybrid diagnostic ZIP' 'Diagnostic ZIP (*.zip)|*.zip' })
$btnQwenBridgeAudit.Add_Click({
    Invoke-UiAction 'Checking Qwen decoder evidence and creating a privacy-safe diagnostic ZIP...' {
        $bench = [string]$txtQwenBridgeBenchmark.Text
        if ([string]::IsNullOrWhiteSpace($bench)) {
            $recent = @(Get-ChildItem -LiteralPath $script:LlamaPhoneBenchResults -Filter 'WhisperRecorder-PhoneBenchmark-*.zip' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
            if ($recent.Count -gt 0) { $bench = [string]$recent[0].FullName; $txtQwenBridgeBenchmark.Text = $bench }
        }
        if (-not (Test-Path -LiteralPath $bench -PathType Leaf)) { throw 'Run the full llama.cpp phone test first, or choose its diagnostic ZIP.' }
        if (-not (Test-Path -LiteralPath $txtQwenBridgeHybrid.Text -PathType Leaf)) { throw 'Choose an existing completed Qwen NPU hybrid diagnostic ZIP.' }
        $modelCandidate = Join-Path $script:LlamaPhoneBenchCache 'models\Qwen3-ASR-0.6B-Q8_0.gguf'
        $py = Get-LocalBuilderPython
        $args = @('-u',$script:QwenHybridQualifierPath,'audit','--benchmark',$bench,'--hybrid',$txtQwenBridgeHybrid.Text,'--results',$script:LlamaPhoneBenchResults)
        if (Test-Path -LiteralPath $modelCandidate -PathType Leaf) { $args += @('--gguf',$modelCandidate) }
        $r = Invoke-ProcessCapture -FilePath $py -Arguments $args -Environment @{'PYTHONUTF8'='1';'PYTHONIOENCODING'='utf-8'} -TimeoutSeconds 120
        $m = [regex]::Matches($r.StdOut,'(?m)DIAGNOSTIC_ZIP=(.+)$')
        if ($m.Count -gt 0) {
            $zip = $m[$m.Count-1].Groups[1].Value.Trim()
            Append-Log ('Qwen bridge readiness ZIP: ' + $zip)
            if (Test-Path -LiteralPath $zip -PathType Leaf) {
                try { Start-Process -FilePath 'explorer.exe' -ArgumentList ('/select,"{0}"' -f $zip) | Out-Null }
                catch { Open-Folder $script:LlamaPhoneBenchResults }
            }
        }
        if ($r.ExitCode -ne 0) { throw 'Qualification is blocked or incomplete. Open the generated ZIP for the exact reason; do not build an APK.' }
        $lblPhoneStatus.Text = 'Reports compared. Native QNN-to-llama.cpp decoding and M-RoPE parity are NOT YET verified.'
    }
})

# -------------------- Events --------------------
$btnPython.Add_Click({ Choose-Exe $txtPython 'Select python.exe'; Update-ToolStatuses })
$btnAdb.Add_Click({ Choose-Exe $txtAdb 'Select adb.exe'; Update-ToolStatuses })
$btnFfmpeg.Add_Click({ Choose-Exe $txtFfmpeg 'Select ffmpeg.exe'; Update-ToolStatuses })
$btnAudioFolder.Add_Click({
    $before = [string]$txtAudioFolder.Text
    Choose-Folder $txtAudioFolder 'Select folder containing .ogg recordings'
    if (-not [string]::IsNullOrWhiteSpace($txtAudioFolder.Text) -and $txtAudioFolder.Text -ne $before) {
        Invoke-UiAction 'Scanning selected .ogg folder...' {
            Scan-Audio
            Save-Settings
        }
    }
})
$btnOnnx.Add_Click({
    Choose-File $txtOnnx 'Select exported ONNX graph' 'ONNX model (*.onnx)|*.onnx|All files (*.*)|*.*'
    if ($txtOnnx.Text -and (Test-Path -LiteralPath $txtOnnx.Text -PathType Leaf)) {
        if ([string]::IsNullOrWhiteSpace($txtSourceModel.Text)) { $txtSourceModel.Text = $txtOnnx.Text }
        Append-Log ('Selected ONNX compile input: ' + $txtOnnx.Text)
    }
    Update-CloudWorkflowState
    Save-Settings
})
$btnCompiled.Add_Click({ Choose-File $txtCompiled 'Select compiled encoder QNN context artifact' 'Binary/context files (*.bin;*.serialized)|*.bin;*.serialized|All files (*.*)|*.*' })
$btnProjectorCompiled.Add_Click({ Choose-File $txtProjectorCompiled 'Select compiled encoder-projector QNN context artifact' 'Binary/context files (*.bin;*.serialized)|*.bin;*.serialized|All files (*.*)|*.*' })
$btnPredictorCompiled.Add_Click({ Choose-File $txtPredictorCompiled 'Select compiled TDT predictor QNN context artifact' 'Binary/context files (*.bin;*.serialized)|*.bin;*.serialized|All files (*.*)|*.*' })
$btnJointCompiled.Add_Click({ Choose-File $txtJointCompiled 'Select compiled TDT joint QNN context artifact' 'Binary/context files (*.bin;*.serialized)|*.bin;*.serialized|All files (*.*)|*.*' })
$btnPackageFolder.Add_Click({ Choose-Folder $txtPackageFolder 'Select package output folder' })
$btnHfPackageBrowse.Add_Click({ Choose-Folder $txtHfPackageFolder 'Select full WhisperRecorder NPU package folder' })
$btnHfDeploymentBrowse.Add_Click({ Choose-Folder $txtHfDeploymentFolder 'Select Hugging Face deployment output folder' })
$btnHfTokenPage.Add_Click({ try { Open-Url 'https://huggingface.co/settings/tokens' } catch { Show-Error $_ } })


$btnInstallPython.Add_Click({
    Invoke-UiAction 'Installing Builder Python 3.11...' {
        [void](Install-PythonFromUi)
        Update-ToolStatuses
        Save-Settings
    }
})

$btnInstallAdb.Add_Click({
    Invoke-UiAction 'Installing Android Platform-Tools...' {
        [void](Install-AdbFromUi)
        Update-ToolStatuses
        Save-Settings
    }
})

$btnInstallFfmpeg.Add_Click({
    Invoke-UiAction 'Installing FFmpeg...' {
        [void](Install-FfmpegFromUi)
        Update-ToolStatuses
        Save-Settings
    }
})

$btnInstallHub.Add_Click({
    Invoke-UiAction 'Installing Qualcomm AI Hub client...' {
        [void](Install-AiHubFromUi)
        Update-ToolStatuses
        Set-Status $lblSetupHub 'Installed' 'good'
        Save-Settings
    }
})

$btnInstallAll.Add_Click({
    Invoke-UiAction 'Installing / repairing prerequisites...' { Install-AllPrerequisites }
})

function Invoke-AiHubAuthenticationUi {
    Invoke-UiAction 'Verifying / configuring Qualcomm AI Hub authentication...' {
        $hubPython = Ensure-AiHubEnvironmentFromUi
        $tokenValue = $txtSetupToken.Text
        if ([string]::IsNullOrWhiteSpace($tokenValue)) { $tokenValue = $txtDashToken.Text }

        # If the Builder was authenticated previously, a token does not need to be
        # pasted again. Verify the persisted Qualcomm profile first.
        if ([string]::IsNullOrWhiteSpace($tokenValue)) {
            Append-Log 'No new token entered. Checking existing Qualcomm AI Hub authentication...'
            $verifyExisting = Test-AiHubAuthentication -PythonPath $hubPython
            if ($verifyExisting.ExitCode -ne 0) {
                throw 'No working saved AI Hub authentication was found. Click Get API token, paste the token on Dashboard or Setup, then click Configure / Verify.'
            }
            Append-Log 'Existing Qualcomm AI Hub authentication is valid. No token reconfiguration was needed.'
        } else {
            Register-SensitiveOutputValue $tokenValue
            $r = Configure-AiHubToken -PythonPath $hubPython -ApiToken $tokenValue
            if ($r.ExitCode -ne 0) {
                Append-Log ('qai-hub configure exited with code ' + $r.ExitCode + '. Checking whether an existing saved profile is already valid...')
                $verifyExisting = Test-AiHubAuthentication -PythonPath $hubPython
                if ($verifyExisting.ExitCode -ne 0) {
                    $detail = (($r.StdErr + [Environment]::NewLine + $r.StdOut).Trim())
                    if ([string]::IsNullOrWhiteSpace($detail)) { $detail = 'qai-hub configure exited with code ' + $r.ExitCode + ' and produced no diagnostic output.' }
                    throw ($detail + [Environment]::NewLine + 'Existing saved authentication also failed verification.')
                }
                Append-Log 'The new configure command did not complete, but an existing saved Qualcomm AI Hub profile is authenticated and usable.'
            } else {
                Append-Log 'AI Hub token saved. Verifying authentication with qai-hub list-devices...'
                $verifyNew = Test-AiHubAuthentication -PythonPath $hubPython
                if ($verifyNew.ExitCode -ne 0) {
                    throw ('The token was saved, but Qualcomm AI Hub rejected or could not verify it.' + [Environment]::NewLine + $verifyNew.StdErr + [Environment]::NewLine + $verifyNew.StdOut)
                }
                Append-Log 'AI Hub token configured and authentication verified successfully. The token value was not logged.'
            }
        }

        $txtSetupToken.Clear()
        $txtDashToken.Clear()
        $txtToken.Clear()
        Set-Status $stHub 'AI Hub authenticated + verified' 'good'
        Set-Status $lblSetupHub 'Authenticated + verified' 'good'
        Set-Status $lblDashHubAuth 'Authenticated + verified' 'good'
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Qualcomm AI Hub authentication is verified.`r`n`r`nYou can now connect/refresh the Galaxy S26, open Model + AI Hub for a new build, or open Deploy & Test to reuse an existing compiled context.",'AI Hub ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
}

$btnConfigureToken.Add_Click({ Invoke-AiHubAuthenticationUi })
$btnDashConfigureToken.Add_Click({ Invoke-AiHubAuthenticationUi })
$btnDashGetToken.Add_Click({ try { Open-Url 'https://aihub.qualcomm.com/get-started' } catch { Show-Error $_ } })

$btnOpenAiHub.Add_Click({ try { Open-Url 'https://aihub.qualcomm.com/get-started' } catch { Show-Error $_ } })
$btnInstallSamsungDriver.Add_Click({
    Invoke-UiAction 'Installing Samsung USB driver...' {
        Append-Log 'Downloading Samsung Android USB Driver from Samsung Developer...'
        $r = Install-SamsungUsbDriver -BuilderRoot $Root
        Append-Log ('Samsung USB driver installer completed. SHA-256: ' + $r.Sha256)
        Append-Log ('Verified signer: ' + $r.Signer)
        [System.Windows.Forms.MessageBox]::Show("Samsung USB-driver setup completed.`r`n`r`nReconnect the phone if necessary, enable USB debugging, accept the RSA prompt, then click Refresh phone.",'Samsung USB driver',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})
$btnOpenSamsungDriver.Add_Click({ try { Open-Url 'https://developer.samsung.com/android-usb-driver' } catch { Show-Error $_ } })

$detectAction = {
    Invoke-UiAction 'Detecting local tools...' {
        Auto-DetectTools
        Save-Settings
    }
}
$btnDetect.Add_Click($detectAction)
$btnDashDetect.Add_Click($detectAction)

$btnSaveSettings.Add_Click({ Invoke-UiAction 'Saving settings...' { Save-Settings } })

$btnTestHub.Add_Click({
    Invoke-UiAction 'Testing / repairing qai_hub Python client...' {
        $hubPython = Ensure-AiHubEnvironmentFromUi
        $r = Test-PythonAiHubClient -PythonPath $hubPython
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        Set-Status $stHub 'qai_hub available' 'good'
        Set-Status $lblSetupHub 'Installed' 'good'
        Save-Settings
        Append-Log $r.StdOut
    }
})

$deviceAction = {
    Invoke-UiAction 'Refreshing ADB device...' {
        Refresh-Device
        Update-DeployWorkflowState
        if ($tabs.SelectedTab -eq $tabDeploy -and $script:DeviceSerial) {
            try {
                $storage = Get-QnnDeviceTestStorageInfo -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                if ($storage.Exists) { Set-Status $lblPhoneStorage (('{0:N1} MiB currently staged on phone.' -f $storage.MiB)) 'warn' }
                else { Set-Status $lblPhoneStorage 'Phone test folder clean (0 MiB staged).' 'good' }
            } catch {
                Set-Status $lblPhoneStorage 'Phone test storage could not be measured.' 'warn'
            }
        }
    }
}
$btnRefreshDevice.Add_Click($deviceAction)
$btnDashDevice.Add_Click($deviceAction)
$btnSetupRefreshPhone.Add_Click($deviceAction)
$btnDeployRefreshPhone.Add_Click($deviceAction)

$btnDiag.Add_Click({
    Invoke-UiAction 'Collecting S26 diagnostics...' {
        if (-not $script:DeviceSerial) { Refresh-Device }
        $out = Join-Path $script:CurrentWorkspace ('device\SM-S942U-NPU-diagnostics-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.zip')
        $p = Export-NpuDiagnostics -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -OutputZip $out
        Append-Log ("Diagnostics: " + $p)
        [System.Windows.Forms.MessageBox]::Show("Diagnostics created:`n$p",'NPU Builder') | Out-Null
    }
})

$btnAdbShell.Add_Click({
    try {
        if (-not $script:DeviceSerial) { Refresh-Device }
        $cmd = '"{0}" -s {1} shell' -f $txtAdb.Text,$script:DeviceSerial
        Start-Process -FilePath $env:ComSpec -ArgumentList @('/k',$cmd) | Out-Null
    } catch { Show-Error $_ }
})

$audioScanAction = {
    Invoke-UiAction 'Scanning .ogg recordings...' {
        Scan-Audio
        Save-Settings
    }
}
$btnScanAudio.Add_Click($audioScanAction)
$btnDashScan.Add_Click($audioScanAction)

$btnPrepareAudio.Add_Click({
    Invoke-UiAction 'Preparing 16 kHz calibration PCM...' {
        if (-not (Ensure-AudioFolderSelection)) { return }
        if ([string]::IsNullOrWhiteSpace($txtFfmpeg.Text) -or -not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
            Set-Status $stAudio 'FFmpeg is required for PCM preparation' 'warn'
            [System.Windows.Forms.MessageBox]::Show("FFmpeg is required to prepare calibration PCM.`r`n`r`nOpen Setup and run Install / repair everything, then try again.",'FFmpeg required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
            return
        }
        $out = Join-Path $script:CurrentWorkspace 'audio\calibration'
        $script:CalibrationManifest = Convert-OggFolderToCalibrationPcm -Folder $txtAudioFolder.Text -FfmpegPath $txtFfmpeg.Text -OutputFolder $out
        Append-Log ("Calibration manifest: " + $script:CalibrationManifest)
        Set-Status $stAudio 'PCM calibration prepared' 'good'
        [System.Windows.Forms.MessageBox]::Show("Calibration data prepared:`n$script:CalibrationManifest",'NPU Builder') | Out-Null
    }
})

$btnOpenAudioOut.Add_Click({
    try { Open-Folder (Join-Path $script:CurrentWorkspace 'audio') } catch { Show-Error $_ }
})

$cmbRecipe.Add_SelectedIndexChanged({
    $choice = [string]$cmbRecipe.SelectedItem
    if ($choice -eq 'HTP Proof-of-Life (recommended)') {
        $proof = Join-Path $script:CurrentWorkspace 'source\proof\whisperrecorder-htp-proof.onnx'
        $txtSourceModel.Text = if (Test-Path -LiteralPath $proof) { $proof } else { '' }
        $txtOnnx.Text = if (Test-Path -LiteralPath $proof) { $proof } else { '' }
    } elseif ($choice -eq 'Omi Med STT v1 (Parakeet v2)') {
        $checkpoint = Join-Path $script:OmiModelDir 'omimedstt-v1.nemo'
        $txtSourceModel.Text = if (Test-Path -LiteralPath $checkpoint) { $checkpoint } else { '' }
        $txtOnnx.Clear()
        $tabs.SelectedTab = $tabOmi
    } elseif ($choice -eq 'NVIDIA Parakeet TDT 0.6B v3') {
        $cache = Join-Path $Root 'models\source\parakeet-tdt-0.6b-v3-transformers'
        $txtSourceModel.Text = if (Test-Path -LiteralPath (Join-Path $cache 'model.safetensors')) { $cache } else { '' }
        $encoder = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-encoder-40s-mask-v2.onnx'
        $projector = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-encoder-projector-500f.onnx'
        $predictor = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-tdt-predictor-step.onnx'
        $currentCompileTarget = [string]$txtOnnx.Text
        $currentCompileTargetValid = (-not [string]::IsNullOrWhiteSpace($currentCompileTarget)) -and (Test-Path -LiteralPath $currentCompileTarget) -and ($currentCompileTarget -match '(?:parakeet-encoder-(?:40s-mask-v2|projector-500f)|parakeet-tdt-predictor-step)\.onnx$')
        if (-not $currentCompileTargetValid) {
            if (Test-Path -LiteralPath $predictor -PathType Leaf) { $txtOnnx.Text = $predictor }
            elseif (Test-Path -LiteralPath $projector -PathType Leaf) { $txtOnnx.Text = $projector }
            elseif (Test-Path -LiteralPath $encoder -PathType Container) { $txtOnnx.Text = $encoder }
            else { $txtOnnx.Text = '' }
        }
        if ([string]::IsNullOrWhiteSpace($txtReferenceAudio.Text) -and $txtAudioFolder.Text -and (Test-Path -LiteralPath $txtAudioFolder.Text)) {
            $ogg = Get-ChildItem -LiteralPath $txtAudioFolder.Text -Filter '*.ogg' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($ogg) { $txtReferenceAudio.Text = $ogg.FullName }
        }
    } else {
        $txtSourceModel.Clear()
        $txtOnnx.Clear()
    }
    Update-CloudWorkflowState
    Save-Settings
})

$btnPrepareModel.Add_Click({
    $choice = [string]$cmbRecipe.SelectedItem
    if ($choice -eq 'Omi Med STT v1 (Parakeet v2)') {
        $tabs.SelectedTab = $tabOmi
        return
    }
    if ($choice -eq 'HTP Proof-of-Life (recommended)') {
        Invoke-UiAction 'Creating deterministic HTP proof ONNX...' {
            $hubPython = Ensure-ModelToolsFromUi
            $out = Join-Path $script:CurrentWorkspace 'source\proof\whisperrecorder-htp-proof.onnx'
            New-Item -ItemType Directory -Path (Split-Path -Parent $out) -Force | Out-Null
            $r = Invoke-ProcessCapture -FilePath $hubPython -Arguments @($script:ModelToolsPath,'create-proof','--output',$out) -TimeoutSeconds 300
            $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
            if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
            $txtSourceModel.Text = $out
            $txtOnnx.Text = $out
            Append-Log ('Proof ONNX ready: ' + $out)
            Update-CloudWorkflowState
            Save-Settings
        }
    } elseif ($choice -eq 'NVIDIA Parakeet TDT 0.6B v3') {
        Invoke-UiAction 'Downloading NVIDIA Parakeet Transformers checkpoint...' {
            $python = Ensure-AiHubEnvironmentFromUi
            $outDir = Join-Path $Root 'models\source\parakeet-tdt-0.6b-v3-transformers'
            New-Item -ItemType Directory -Path $outDir -Force | Out-Null
            $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:ModelToolsPath,'download-parakeet-transformers','--output-dir',$outDir) -TimeoutSeconds 21600
            $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
            if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
            $txtSourceModel.Text = $outDir
            Append-Log ('Official Parakeet Transformers checkpoint ready: ' + $outDir)
            Update-CloudWorkflowState
            Save-Settings
            [System.Windows.Forms.MessageBox]::Show("Official NVIDIA Parakeet model downloaded and verified.`r`n`r`nNext: choose a real .ogg lecture as the encoder reference, then click 2. Prepare encoder. The Builder will install its project-local export environment, create the fixed 40-second FastConformer host-mask-v2 ONNX package, and validate it on Windows.",'Parakeet model ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        }
    }
})

$btnReferenceAudio.Add_Click({
    try {
        $d = New-Object System.Windows.Forms.OpenFileDialog
        $d.Title = 'Choose one representative .ogg lecture for Parakeet encoder validation'
        $d.Filter = 'OGG recordings (*.ogg)|*.ogg|All files (*.*)|*.*'
        if ($txtAudioFolder.Text -and (Test-Path -LiteralPath $txtAudioFolder.Text)) { $d.InitialDirectory = $txtAudioFolder.Text }
        if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
            $txtReferenceAudio.Text = $d.FileName
            Append-Log ('Parakeet reference audio: ' + $d.FileName)
            Update-CloudWorkflowState
        }
    } catch { Show-Error $_ }
})

$btnExportParakeet.Add_Click({
    if ([string]$cmbRecipe.SelectedItem -ne 'NVIDIA Parakeet TDT 0.6B v3') { return }
    $modelDir = $txtSourceModel.Text
    if ([string]::IsNullOrWhiteSpace($modelDir) -or -not (Test-Path -LiteralPath (Join-Path $modelDir 'model.safetensors') -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Download the official Parakeet model first (Step 1).','Parakeet model required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        $btnPrepareModel.Focus(); return
    }
    if ([string]::IsNullOrWhiteSpace($txtReferenceAudio.Text) -or -not (Test-Path -LiteralPath $txtReferenceAudio.Text -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Choose one representative .ogg lecture. The Builder uses its first 40 seconds to make real encoder inputs and a high-precision reference output.','Reference .ogg required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        $btnReferenceAudio.PerformClick(); return
    }
    if ([string]::IsNullOrWhiteSpace($txtFfmpeg.Text) -or -not (Test-Path -LiteralPath $txtFfmpeg.Text -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('FFmpeg is required to decode the reference .ogg. Run Setup -> Install / repair everything first.','FFmpeg required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    Invoke-UiAction 'Preparing and validating Parakeet FastConformer encoder...' {
        $python = Ensure-ParakeetExportEnvironmentFromUi
        $sourceDir = Join-Path $script:CurrentWorkspace 'source\parakeet'
        New-Item -ItemType Directory -Path $sourceDir -Force | Out-Null
        $package = Join-Path $sourceDir 'parakeet-encoder-40s-mask-v2.onnx'
        $reference = Join-Path $sourceDir 'parakeet-encoder-40s-mask-v2.reference.json'
        $args = @($script:ParakeetExportPath,'prepare','--model-dir',$modelDir,'--reference-audio',$txtReferenceAudio.Text,'--ffmpeg',$txtFfmpeg.Text,'--output-package',$package,'--reference-json',$reference,'--chunk-seconds','40')
        $env = @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'TOKENIZERS_PARALLELISM'='false' }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment $env -TimeoutSeconds 21600
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtOnnx.Text = $package
        Append-Log ('Parakeet fixed 40-second encoder ONNX package ready: ' + $package)
        Append-Log ('Parakeet encoder reference: ' + $reference)
        Update-CloudWorkflowState
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Parakeet FastConformer host-mask-v2 export and desktop validation passed.`r`n`r`nCompile input:`r`n$package`r`n`r`nNext: click Probe AI Hub, then Build QNN context. The resulting .bin can be selected automatically in Deploy & Test for an HTP encoder comparison.",'Parakeet encoder ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnPrepareProjector.Add_Click({
    if ([string]$cmbRecipe.SelectedItem -ne 'NVIDIA Parakeet TDT 0.6B v3') { return }
    $modelDir = $txtSourceModel.Text
    if ([string]::IsNullOrWhiteSpace($modelDir) -or -not (Test-Path -LiteralPath (Join-Path $modelDir 'model.safetensors') -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Download/select the official Parakeet checkpoint first.','Parakeet model required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    $encoderReference = Join-Path $script:CurrentWorkspace 'source\parakeet\parakeet-encoder-40s-mask-v2.reference.json'
    if (-not (Test-Path -LiteralPath $encoderReference -PathType Leaf)) {
        $latest = Get-ChildItem -LiteralPath $script:WorkspaceRoot -Filter 'parakeet-encoder-40s-mask-v2.reference.json' -File -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($latest) { $encoderReference = $latest.FullName }
    }
    if (-not (Test-Path -LiteralPath $encoderReference -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Prepare the host-mask-v2 encoder first. The projector uses the validated 500-frame encoder reference output as its real input tensor.','Encoder reference required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    Invoke-UiAction 'Preparing and validating Parakeet encoder projector...' {
        $python = Ensure-ParakeetExportEnvironmentFromUi
        $sourceDir = Join-Path $script:CurrentWorkspace 'source\parakeet'
        New-Item -ItemType Directory -Path $sourceDir -Force | Out-Null
        $onnx = Join-Path $sourceDir 'parakeet-encoder-projector-500f.onnx'
        $reference = Join-Path $sourceDir 'parakeet-encoder-projector-500f.reference.json'
        $args = @($script:ParakeetProjectorPath,'prepare','--model-dir',$modelDir,'--encoder-reference',$encoderReference,'--output-onnx',$onnx,'--reference-json',$reference)
        $env = @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'TOKENIZERS_PARALLELISM'='false' }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment $env -TimeoutSeconds 1800
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtOnnx.Text = $onnx
        Append-Log ('Parakeet encoder projector ONNX ready: ' + $onnx)
        Append-Log ('Parakeet encoder projector reference: ' + $reference)
        Update-CloudWorkflowState
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Encoder projector export and desktop ONNX validation passed.`r`n`r`nInput: [1,1024,500] valid encoder frames`r`nOutput: [1,640,500]`r`n`r`nNext: Probe AI Hub if needed, then Build QNN context. Deploy & Test will use the projector reference automatically.",'Parakeet projector ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnPreparePredictor.Add_Click({
    if ([string]$cmbRecipe.SelectedItem -ne 'NVIDIA Parakeet TDT 0.6B v3') { return }
    $modelDir = $txtSourceModel.Text
    if ([string]::IsNullOrWhiteSpace($modelDir) -or -not (Test-Path -LiteralPath (Join-Path $modelDir 'model.safetensors') -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Download/select the official Parakeet checkpoint first.','Parakeet model required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    $contract = Join-Path $modelDir 'whisperrecorder-tdt-contract.json'
    if (-not (Test-Path -LiteralPath $contract -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Go to Package and click Probe TDT contract first. Predictor export refuses to guess decoder/state shapes.','Verified TDT contract required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }

    # Enforce the roadmap boundary: do not advance to predictor until a saved
    # passing projector HTP comparison exists. This is Windows-only metadata;
    # no phone action is performed here.
    $projectorPass = $false
    foreach ($ctx in @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { [string]$_.Kind -eq 'parakeet_encoder_projector' })) {
        $deviceRoot = Join-Path $ctx.Workspace 'device'
        if (-not (Test-Path -LiteralPath $deviceRoot -PathType Container)) { continue }
        $comparisons = @(Get-ChildItem -LiteralPath $deviceRoot -Filter 'comparison.json' -File -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)
        foreach ($comparisonFile in $comparisons) {
            try {
                $comparison = Get-Content -LiteralPath $comparisonFile.FullName -Raw | ConvertFrom-Json
                $passProp = $comparison.PSObject.Properties['pass']
                $kindProp = $comparison.PSObject.Properties['kind']
                if ($null -ne $passProp -and [bool]$passProp.Value -and $null -ne $kindProp -and [string]$kindProp.Value -eq 'parakeet_encoder_projector') {
                    $projectorPass = $true
                    break
                }
            } catch {}
        }
        if ($projectorPass) { break }
    }
    if (-not $projectorPass) {
        [System.Windows.Forms.MessageBox]::Show('The Builder cannot find a passing saved encoder-projector HTP comparison. Run/Recompare the projector in Deploy & Test until it reports HTP PASS, then prepare the predictor.','Projector HTP PASS required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }

    Invoke-UiAction 'Preparing and validating Parakeet TDT predictor core...' {
        $python = Ensure-ParakeetExportEnvironmentFromUi
        $sourceDir = Join-Path $script:CurrentWorkspace 'source\parakeet'
        New-Item -ItemType Directory -Path $sourceDir -Force | Out-Null
        $onnx = Join-Path $sourceDir 'parakeet-tdt-predictor-step.onnx'
        $reference = Join-Path $sourceDir 'parakeet-tdt-predictor-step.reference.json'
        $args = @($script:ParakeetPredictorPath,'prepare','--model-dir',$modelDir,'--contract-json',$contract,'--output-onnx',$onnx,'--reference-json',$reference)
        $env = @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'TOKENIZERS_PARALLELISM'='false' }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment $env -TimeoutSeconds 1800
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtOnnx.Text = $onnx
        Append-Log ('Parakeet TDT predictor core ONNX ready: ' + $onnx)
        Append-Log ('Parakeet TDT predictor reference: ' + $reference)
        Update-CloudWorkflowState
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("TDT predictor-core export and desktop validation passed.`r`n`r`nInputs:`r`n  token_embedding [1,1,640]`r`n  h0/c0 [1,640]`r`n  h1/c1 [1,640]`r`nOutputs: predictor + split per-layer recurrent state.`r`n`r`nEvery QNN state tensor is batch=1; this avoids qnn-net-run interpreting the two-layer axis as batch=2. The HTP reference uses a non-zero state from a previous token step. Host token lookup is intentionally outside QNN; the 2-layer LSTM + decoder projection are inside the predictor graph.`r`n`r`nNext: Probe AI Hub if needed, then Build QNN context and run the predictor HTP test.",'Parakeet predictor ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnPrepareJoint.Add_Click({
    if ([string]$cmbRecipe.SelectedItem -ne 'NVIDIA Parakeet TDT 0.6B v3') { return }
    $modelDir = $txtSourceModel.Text
    if ([string]::IsNullOrWhiteSpace($modelDir) -or -not (Test-Path -LiteralPath (Join-Path $modelDir 'model.safetensors') -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Download/select the official Parakeet checkpoint first.','Parakeet model required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    $contract = Join-Path $modelDir 'whisperrecorder-tdt-contract.json'
    if (-not (Test-Path -LiteralPath $contract -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Go to Package and click Probe TDT contract first. Joint export refuses to guess token/duration/head dimensions.','Verified TDT contract required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }

    $projectorReference = ''
    $predictorReference = ''
    $projectorPass = $false
    $predictorPass = $false
    foreach ($ctx in @(Get-QnnCompiledArtifacts -BuilderRoot $Root)) {
        $kind = [string]$ctx.Kind
        if ($kind -notin @('parakeet_encoder_projector','parakeet_tdt_predictor')) { continue }
        $deviceRoot = Join-Path $ctx.Workspace 'device'
        if (-not (Test-Path -LiteralPath $deviceRoot -PathType Container)) { continue }
        foreach ($comparisonFile in @(Get-ChildItem -LiteralPath $deviceRoot -Filter 'comparison.json' -File -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $comparison = Get-Content -LiteralPath $comparisonFile.FullName -Raw | ConvertFrom-Json
                $passProp = $comparison.PSObject.Properties['pass']
                $kindProp = $comparison.PSObject.Properties['kind']
                if ($null -eq $passProp -or -not [bool]$passProp.Value -or $null -eq $kindProp) { continue }
                if ([string]$kindProp.Value -eq 'parakeet_encoder_projector' -and $kind -eq 'parakeet_encoder_projector') {
                    $projectorPass = $true
                    if ($ctx.ReferencePath -and (Test-Path -LiteralPath $ctx.ReferencePath -PathType Leaf)) { $projectorReference = [string]$ctx.ReferencePath }
                }
                if ([string]$kindProp.Value -eq 'parakeet_tdt_predictor' -and $kind -eq 'parakeet_tdt_predictor') {
                    $predictorPass = $true
                    if ($ctx.ReferencePath -and (Test-Path -LiteralPath $ctx.ReferencePath -PathType Leaf)) { $predictorReference = [string]$ctx.ReferencePath }
                }
            } catch {}
            if (($kind -eq 'parakeet_encoder_projector' -and $projectorPass -and $projectorReference) -or ($kind -eq 'parakeet_tdt_predictor' -and $predictorPass -and $predictorReference)) { break }
        }
    }
    if (-not $projectorPass -or [string]::IsNullOrWhiteSpace($projectorReference)) {
        [System.Windows.Forms.MessageBox]::Show('The Builder cannot find a passing saved encoder-projector HTP comparison + reference. Keep/reuse the validated projector workspace and Recompare latest if needed.','Projector HTP PASS required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    if (-not $predictorPass -or [string]::IsNullOrWhiteSpace($predictorReference)) {
        [System.Windows.Forms.MessageBox]::Show('The Builder cannot find a passing saved predictor HTP comparison + reference. Run/Recompare the split-state predictor until it reports HTP PASS, then prepare the joint graph.','Predictor HTP PASS required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }

    Invoke-UiAction 'Preparing and validating Parakeet TDT joint...' {
        $python = Ensure-ParakeetExportEnvironmentFromUi
        $sourceDir = Join-Path $script:CurrentWorkspace 'source\parakeet'
        New-Item -ItemType Directory -Path $sourceDir -Force | Out-Null
        $onnx = Join-Path $sourceDir 'parakeet-tdt-joint-step.onnx'
        $reference = Join-Path $sourceDir 'parakeet-tdt-joint-step.reference.json'
        $args = @($script:ParakeetJointPath,'prepare','--model-dir',$modelDir,'--contract-json',$contract,'--projector-reference',$projectorReference,'--predictor-reference',$predictorReference,'--output-onnx',$onnx,'--reference-json',$reference)
        $env = @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'TOKENIZERS_PARALLELISM'='false' }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment $env -TimeoutSeconds 1800
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtOnnx.Text = $onnx
        Append-Log ('Parakeet TDT joint ONNX ready: ' + $onnx)
        Append-Log ('Parakeet TDT joint reference: ' + $reference)
        Update-CloudWorkflowState
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("TDT joint export and desktop validation passed.`r`n`r`nInputs:`r`n  encoder_projected_frame [1,1,640]`r`n  predictor_out [1,1,640]`r`nOutput:`r`n  tdt_logits [1,1,8198] = 8193 token/blank logits + 5 duration logits.`r`n`r`nThe graph executes ReLU(encoder + predictor) followed by the learned joint head. HTP validation requires both float parity and exact token + duration argmax.`r`n`r`nNext: Probe AI Hub if needed, Build QNN context, then run the joint HTP test.",'Parakeet joint ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnValidateController.Add_Click({
    if ([string]$cmbRecipe.SelectedItem -ne 'NVIDIA Parakeet TDT 0.6B v3') { return }
    $modelDir = $txtSourceModel.Text
    if ([string]::IsNullOrWhiteSpace($modelDir) -or -not (Test-Path -LiteralPath (Join-Path $modelDir 'model.safetensors') -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Download/select the official Parakeet checkpoint first.','Parakeet model required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }

    $requiredKinds = @('parakeet_encoder','parakeet_encoder_projector','parakeet_tdt_predictor','parakeet_tdt_joint')
    $passes = @{}
    $references = @{}
    foreach ($ctx in @(Get-QnnCompiledArtifacts -BuilderRoot $Root)) {
        $kind = [string]$ctx.Kind
        if ($kind -notin $requiredKinds) { continue }
        if ($passes.ContainsKey($kind) -and [bool]$passes[$kind]) { continue }
        $deviceRoot = Join-Path $ctx.Workspace 'device'
        if (-not (Test-Path -LiteralPath $deviceRoot -PathType Container)) { continue }
        foreach ($comparisonFile in @(Get-ChildItem -LiteralPath $deviceRoot -Filter 'comparison.json' -File -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending)) {
            try {
                $comparison = Get-Content -LiteralPath $comparisonFile.FullName -Raw | ConvertFrom-Json
                $passProp = $comparison.PSObject.Properties['pass']
                $kindProp = $comparison.PSObject.Properties['kind']
                if ($null -eq $passProp -or -not [bool]$passProp.Value -or $null -eq $kindProp -or [string]$kindProp.Value -ne $kind) { continue }
                $passes[$kind] = $true
                if ($ctx.ReferencePath -and (Test-Path -LiteralPath $ctx.ReferencePath -PathType Leaf)) { $references[$kind] = [string]$ctx.ReferencePath }
                break
            } catch {}
        }
    }
    foreach ($kind in $requiredKinds) {
        if (-not $passes.ContainsKey($kind) -or -not [bool]$passes[$kind]) {
            [System.Windows.Forms.MessageBox]::Show(('A passing saved HTP comparison was not found for ' + $kind + '. Reuse/Recompare the existing validated workspace; do not rebuild unless the saved proof is genuinely missing.'),'All four HTP proofs required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
            return
        }
    }
    foreach ($kind in @('parakeet_encoder_projector','parakeet_tdt_predictor','parakeet_tdt_joint')) {
        if (-not $references.ContainsKey($kind) -or [string]::IsNullOrWhiteSpace([string]$references[$kind])) {
            [System.Windows.Forms.MessageBox]::Show(('A saved reference manifest was not found for ' + $kind + '. Keep the validated workspace that produced its HTP proof.'),'Reference manifest required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
            return
        }
    }

    Invoke-UiAction 'Validating full Parakeet TDT controller against Transformers 5.9.0...' {
        $python = Ensure-ParakeetExportEnvironmentFromUi
        $contract = Join-Path $modelDir 'whisperrecorder-tdt-contract.json'
        # Refresh the contract with the pinned v0.3.14+ controller semantics before binding the proof.
        $contractRun = Invoke-ProcessCapture -FilePath $python -Arguments @($script:ParakeetContractPath,'--model-dir',$modelDir,'--output',$contract) -Environment @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8' } -TimeoutSeconds 300
        if ($contractRun.ExitCode -ne 0) { throw ($contractRun.StdErr + [Environment]::NewLine + $contractRun.StdOut) }

        $validationDir = Join-Path $script:CurrentWorkspace 'validation'
        New-Item -ItemType Directory -Path $validationDir -Force | Out-Null
        $proof = Join-Path $validationDir 'parakeet-tdt-controller.json'
        $args = @(
            $script:ParakeetControllerPath,
            '--model-dir',$modelDir,
            '--contract-json',$contract,
            '--projector-reference',[string]$references['parakeet_encoder_projector'],
            '--predictor-reference',[string]$references['parakeet_tdt_predictor'],
            '--joint-reference',[string]$references['parakeet_tdt_joint'],
            '--output-json',$proof
        )
        $env = @{ 'PYTHONUTF8'='1'; 'PYTHONIOENCODING'='utf-8'; 'TOKENIZERS_PARALLELISM'='false' }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -Environment $env -TimeoutSeconds 3600
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $proofJson = Get-Content -LiteralPath $proof -Raw | ConvertFrom-Json
        $passProp = $proofJson.PSObject.Properties['pass']
        if ($null -eq $passProp -or -not [bool]$passProp.Value) { throw 'Controller proof did not report PASS.' }
        Append-Log ('Parakeet TDT controller reference loop PASS: ' + $proof)
        Append-Log ('Controller decisions: steps=' + [string]($proofJson.summary.steps) + '; blanks=' + [string]($proofJson.summary.blank_emissions) + '; nonblank=' + [string]($proofJson.summary.nonblank_emissions) + '; predictor_invocations=' + [string]($proofJson.summary.predictor_invocations_including_start) + '; blank_reuses=' + [string]($proofJson.summary.blank_predictor_reuses))
        [System.Windows.Forms.MessageBox]::Show("Full TDT controller reference validation PASS.`r`n`r`nThe exported predictor + joint graphs exactly matched pinned Transformers 5.9.0 token decisions, duration decisions, frame advancement, decoder-start initialization, and blank-cache reuse across the complete 500-frame projector reference stream.`r`n`r`nNo AI Hub compilation and no phone execution were used for this step.`r`n`r`nNext: Package -> Build v1-draft package. Expected status: controller_validated_runtime_pending.",'Parakeet TDT controller PASS',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnOpenModelSource.Add_Click({
    try {
        $path = $txtSourceModel.Text
        if ($path -and (Test-Path -LiteralPath $path -PathType Container)) { Open-Folder $path } elseif ($path -and (Test-Path -LiteralPath $path -PathType Leaf)) { Open-Folder (Split-Path -Parent $path) }
        else { Open-Folder (Join-Path $Root 'models') }
    } catch { Show-Error $_ }
})

$btnOpenOfficialModel.Add_Click({
    try { if ([string]$cmbRecipe.SelectedItem -eq 'Omi Med STT v1 (Parakeet v2)') { Open-Url 'https://huggingface.co/omi-health/omi-med-stt-v1' } else { Open-Url 'https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3' } } catch { Show-Error $_ }
})

$cmbPrecision.Add_SelectedIndexChanged({ Update-CloudWorkflowState })

$btnProbeCloud.Add_Click({
    Invoke-UiAction 'Probing Qualcomm AI Hub...' {
        Register-SensitiveOutputValue $txtToken.Text
        $hubPython = Ensure-AiHubEnvironmentFromUi
        $r = Invoke-AiHubProbe -PythonPath $hubPython -BridgePath $script:BridgePath -ApiToken $txtToken.Text
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) {
            $script:CloudProbeOk = $false
            Set-Status $stHub 'AI Hub unavailable' 'bad'
            Update-CloudWorkflowState
            throw ($r.StdErr + [Environment]::NewLine + $r.StdOut)
        }
        if ($r.StdOut -notmatch 'Galaxy S26') {
            $script:CloudProbeOk = $false
            Update-CloudWorkflowState
            [System.Windows.Forms.MessageBox]::Show('AI Hub responded, but no Galaxy S26 target was found in the response. See the output box and session log.','S26 target not found',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Warning) | Out-Null
            return
        }
        $script:CloudProbeOk = $true
        Set-Status $stHub 'AI Hub + S26 target ready' 'good'
        Set-Status $lblDashHubAuth 'Authenticated + S26 target verified' 'good'
        Update-CloudWorkflowState
        Append-Log 'AI Hub probe succeeded and a Galaxy S26 target was found. Build QNN context will unlock once an ONNX graph is ready.'
    }
})

$btnCompileCloud.Add_Click({
    if (-not $script:CloudProbeOk) {
        [System.Windows.Forms.MessageBox]::Show("Verify the Qualcomm cloud target first.`r`n`r`nClick Probe AI Hub. The Builder will confirm that a Galaxy S26 target is available before compilation.",'Probe AI Hub first',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        $btnProbeCloud.Focus()
        return
    }
    if ([string]::IsNullOrWhiteSpace($txtOnnx.Text) -or -not (Test-Path -LiteralPath $txtOnnx.Text)) {
        [System.Windows.Forms.MessageBox]::Show("No compile-ready model is selected.`r`n`r`nFor Parakeet: click 1. Download model, choose a reference .ogg, then click 2. Prepare encoder. The Builder creates and validates the external-weight ONNX package automatically.",'Prepare the model first',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        $btnPrepareModel.Focus()
        return
    }
    if ($cmbPrecision.SelectedIndex -ne 0) {
        [System.Windows.Forms.MessageBox]::Show('INT8 is not enabled yet. Choose float16 for the proof-of-life and correctness baseline.','Use float16',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        $cmbPrecision.SelectedIndex = 0
        return
    }
    Invoke-UiAction 'Submitting AI Hub compile + link for QNN context...' {
        Register-SensitiveOutputValue $txtToken.Text
        $hubPython = Ensure-AiHubEnvironmentFromUi
        $stem = [IO.Path]::GetFileNameWithoutExtension($txtOnnx.Text)
        Append-Log ('Compile target selected: ' + $txtOnnx.Text)
        $out = Join-Path $script:CurrentWorkspace ("compiled\{0}-sm8850-v81-fp16.bin" -f $stem)
        $r = Invoke-AiHubCloudCompile -PythonPath $hubPython -BridgePath $script:BridgePath -OnnxPath $txtOnnx.Text -OutputPath $out -ApiToken $txtToken.Text -Precision 'float16' -QairtVersion $txtQairt.Text
        $txtCloudResult.Text = ($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim()
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $script:LastCompiledArtifact = $out
        if ($stem -match 'tdt-joint') { $txtJointCompiled.Text = $out } elseif ($stem -match 'tdt-predictor') { $txtPredictorCompiled.Text = $out } elseif ($stem -match 'encoder-projector') { $txtProjectorCompiled.Text = $out } else { $txtCompiled.Text = $out }
        $referenceCandidate = Join-Path (Split-Path -Parent $txtOnnx.Text) (([IO.Path]::GetFileNameWithoutExtension($txtOnnx.Text)) + '.reference.json')
        if (-not (Test-Path -LiteralPath $referenceCandidate -PathType Leaf)) { $referenceCandidate = '' }
        try {
            $metaPath = Write-QnnCompiledArtifactMetadata -ContextPath $out -SourceOnnx $txtOnnx.Text -ReferencePath $referenceCandidate -Target $txtTarget.Text -Precision 'float16' -QairtVersion $txtQairt.Text
            Append-Log ('Saved compiled-context metadata: ' + $metaPath)
        } catch {
            Append-Log ('Warning: could not write compiled-context metadata sidecar: ' + $_.Exception.Message)
        }
        Set-Status $lblCompileStatus 'Compiled' 'good'
        Append-Log ("Compiled artifact: " + $out)
        Save-Settings
        Refresh-DeployArtifacts -PreferredPath $out
        $tabs.SelectedTab = $tabDeploy
        $compiledRole = if ($stem -match 'tdt-joint') { 'Parakeet TDT joint' } elseif ($stem -match 'tdt-predictor') { 'Parakeet TDT predictor core' } elseif ($stem -match 'encoder-projector') { 'Parakeet encoder projector' } elseif ($stem -match 'parakeet-encoder') { 'Parakeet encoder' } else { 'QNN graph' }
        [System.Windows.Forms.MessageBox]::Show("$compiledRole QNN context build completed through AI Hub compile + link.`r`n`r`n$($out)`r`n`r`nThe Builder has opened Deploy & Test and selected the newest compiled context automatically.",'QNN context ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnOpenCompiled.Add_Click({
    try { Open-Folder (Join-Path $script:CurrentWorkspace 'compiled') } catch { Show-Error $_ }
})

$cmbDeployArtifact.Add_SelectedIndexChanged({ Update-DeployWorkflowState })
$btnDeployRefreshArtifacts.Add_Click({ Refresh-DeployArtifacts })
$btnDeployBrowseArtifact.Add_Click({
    try {
        $d = New-Object System.Windows.Forms.OpenFileDialog
        $d.Title = 'Select a compiled QNN context binary'
        $d.Filter = 'QNN context binary (*.bin)|*.bin|All files (*.*)|*.*'
        if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
            $f = Get-Item -LiteralPath $d.FileName
            $known = @(Get-QnnCompiledArtifacts -BuilderRoot $Root | Where-Object { $_.Path -ieq $f.FullName } | Select-Object -First 1)
            if ($known.Count -gt 0) {
                $item = $known[0]
            } else {
                $workspace = if ($f.Directory.Name -ieq 'compiled') { $f.Directory.Parent.FullName } else { $f.Directory.FullName }
                $reference = Get-ChildItem -LiteralPath (Join-Path $workspace 'source') -Filter '*.reference.json' -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
                $item = [pscustomobject]@{
                    Path=$f.FullName; Name=$f.Name; Bytes=$f.Length; MiB=[Math]::Round($f.Length/1MB,2); LastWriteTime=$f.LastWriteTime; Workspace=$workspace;
                    ReferencePath=$(if ($reference) { $reference.FullName } else { '' }); SourceOnnx='';
                    DisplayName=('{0}  |  {1:N2} MiB  |  manual selection' -f $f.Name,($f.Length/1MB))
                }
            }
            [void]$cmbDeployArtifact.Items.Insert(0,$item)
            $cmbDeployArtifact.SelectedIndex = 0
        }
    } catch { Show-Error $_ }
})
$btnRuntimeBrowse.Add_Click({ Choose-Folder $txtRuntimeRoot 'Select a folder containing the Android qnn-net-run and HTP V81 runtime files'; Update-DeployWorkflowState })
$btnRuntimeCheck.Add_Click({
    $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
    $txtDeployResult.Text = if ($runtime.IsComplete) { 'Android QNN runtime bundle is complete.' } else { 'Android QNN runtime bundle is incomplete.' + [Environment]::NewLine + 'Missing: ' + ($runtime.Missing -join ', ') }
    Update-DeployWorkflowState
})
$btnRuntimeDocs.Add_Click({ try { Open-Url 'https://docs.qualcomm.com/nav/home/qnn_tutorial_windows_host_linux_target.html?product=924033590759186372' } catch { Show-Error $_ } })
$btnStageProof.Add_Click({
    $artifact = Get-SelectedDeployArtifact
    if ($null -eq $artifact -or -not (Test-Path -LiteralPath $artifact.Path -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('No compiled QNN context is selected. Click Detect compiled files first.','Select context',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    if (-not $artifact.ReferencePath -or -not (Test-Path -LiteralPath $artifact.ReferencePath -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('This compiled context does not have an associated reference JSON. Build it through the guided HTP Proof or Parakeet encoder workflow, or select a context that has a metadata/reference association.','Reference missing',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    Invoke-UiAction 'Verifying stageable QNN context on Galaxy S26; cleaning immediately...' {
        if (-not $script:DeviceSerial) { Refresh-Device }
        $out = Join-Path $artifact.Workspace ('device\test-stage-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        $stage = $null
        $cleanupOk = $false
        try {
            $stage = Stage-QnnProofToDevice -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -ContextPath $artifact.Path -ReferenceJson $artifact.ReferencePath -HostOutputFolder $out
        } finally {
            try {
                $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                $cleanupOk = [bool]$clean.VerifiedClean
            } catch {
                Append-Log ('Stage-only cleanup failed: ' + $_.Exception.Message)
                Set-Status $lblPhoneStorage 'Stage cleanup failed; use Clear previous S26 test files' 'bad'
            }
        }
        if (-not $cleanupOk) { throw 'Staging ended, but temporary files could not be removed from the S26. Use the manual cleanup button when ADB reconnects.' }
        $script:LastDeployOutputFolder = $out
        Set-Status $lblPhoneStorage 'Staging check finished; S26 test folder cleared' 'good'
        $txtDeployResult.Text = "QNN test staging verified successfully.`r`nDevice: $($stage.Device.Model)`r`nContext: $($artifact.Path)`r`n`r`nTemporary files deleted immediately after this verification.`r`nRun HTP test stages its own files and cleans after inference."
        Set-Status $lblDeployRun 'Staging verified; storage cleared' 'good'
        Append-Log 'Stage-only verification complete; phone staging empty.'
    }
})
$btnRunProof.Add_Click({
    $artifact = Get-SelectedDeployArtifact
    $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
    if (-not $runtime.IsComplete) {
        [System.Windows.Forms.MessageBox]::Show("The compiled .bin is ready, but the Android execution runner is not complete yet.`r`n`r`nMissing:`r`n$($runtime.Missing -join "`r`n")`r`n`r`nThe Builder will not copy arbitrary Samsung /vendor libraries. Point Runtime folder at an official QAIRT Android runtime/tools bundle containing qnn-net-run and the V81 HTP files.",'Android QNN runtime required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    Invoke-UiAction 'Executing QNN context on Galaxy S26 HTP...' {
        if (-not $script:DeviceSerial) { Refresh-Device }
        $out = Join-Path $artifact.Workspace ('device\htp-test-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        $result = $null
        $cleanupText = 'Automatic cleanup pending.'
        try {
            $result = Invoke-QnnProofOnDevice -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -ContextPath $artifact.Path -ReferenceJson $artifact.ReferencePath -RuntimeRoot $txtRuntimeRoot.Text -HostOutputFolder $out
        } finally {
            if ($script:DeviceSerial) {
                try {
                    $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
                    $cleanupText = if ($clean.WasPresent) { ('Auto-clean removed approximately {0:N1} MiB from the phone.' -f $clean.FreedMiB) } else { 'Auto-clean confirmed that no Builder test files remain on the phone.' }
                    Set-Status $lblPhoneStorage 'Phone test folder clean (0 MiB staged).' 'good'
                    Append-Log $cleanupText
                } catch {
                    $cleanupText = 'AUTO-CLEAN FAILED: ' + $_.Exception.Message
                    Set-Status $lblPhoneStorage 'Auto-clean failed. Click Clean phone files.' 'bad'
                    Append-Log $cleanupText
                }
            }
        }
        if ($cleanupText -match '(?i)fail|pending') { throw ('Phone test files not confirmed removed: ' + $cleanupText + '. Use Clear previous S26 test files after reconnecting ADB.') }
        $script:LastDeployOutputFolder = $out
        $kindLabel = if ($result.Kind -eq 'omi_med_encoder') { 'Omi Med experimental encoder' } elseif ($result.Kind -eq 'parakeet_tdt_joint') { 'Parakeet TDT joint' } elseif ($result.Kind -eq 'parakeet_tdt_predictor') { 'Parakeet TDT predictor' } elseif ($result.Kind -eq 'parakeet_encoder') { 'Parakeet encoder' } elseif ($result.Kind -eq 'parakeet_encoder_projector') { 'Parakeet encoder projector' } else { 'HTP proof' }
        $txtDeployResult.Text = "Test: $kindLabel`r`nRequested backend: $($result.RequestedBackend)`r`nBackend evidence: $($result.CpuFallback)`r`nNumeric result: $(if ($result.Pass) { 'PASS' } else { 'FAIL' })`r`nMax absolute error: $($result.MaxAbsError)`r`nMean absolute error: $($result.MeanAbsError)`r`nRMSE: $($result.Rmse)`r`nCosine similarity: $($result.Cosine)`r`nPadded max abs: $($result.PaddedMaxAbs)`r`nPhone cleanup: $cleanupText`r`n`r`nThresholds:`r`n$($result.ThresholdSummary)`r`n`r`nComparison:`r`n$($result.ComparisonSummary)`r`n`r`nDiagnostic bundle:`r`n$($result.ComparisonBundle)`r`n`r`nqnn-net-run output:`r`n$($result.StdOut)"
        Append-Log ("HTP comparison: {0}; result={1}; max_abs={2}; mean_abs={3}; rmse={4}; cosine={5}`r`nThresholds:`r`n{6}`r`nComparison:`r`n{7}" -f $kindLabel,$(if ($result.Pass) { 'PASS' } else { 'FAIL' }),$result.MaxAbsError,$result.MeanAbsError,$result.Rmse,$result.Cosine,$result.ThresholdSummary,$result.ComparisonSummary)
        if ($result.Pass) {
            Set-Status $lblDeployRun ($kindLabel + ' HTP PASS') 'good'
            [System.Windows.Forms.MessageBox]::Show("$kindLabel passed on the connected Galaxy S26.`r`n`r`nDevice output matches the Windows reference within the configured FP16 tolerance.`r`nBackend: libQnnHtp.so (CPU backend was not requested).`r`n`r`nA self-contained comparison ZIP was created automatically for diagnostics.",($kindLabel + ' HTP PASS'),[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        } else {
            Set-Status $lblDeployRun ($kindLabel + ' ran; numeric comparison failed') 'bad'
        }
    }
})
$btnRecompareProof.Add_Click({
    $artifact = Get-SelectedDeployArtifact
    if ($null -eq $artifact -or -not $artifact.ReferencePath -or -not (Test-Path -LiteralPath $artifact.ReferencePath -PathType Leaf)) {
        [System.Windows.Forms.MessageBox]::Show('Select a compiled context with an associated reference first.','Reference required',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        return
    }
    Invoke-UiAction 'Recomparing latest saved HTP output...' {
        $deviceRoot = Join-Path $artifact.Workspace 'device'
        $latest = Get-ChildItem -LiteralPath $deviceRoot -Directory -Filter 'htp-test-*' -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'qnn_out_device') -PathType Container } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($null -eq $latest) { throw "No saved HTP device output was found under $deviceRoot. Run the HTP test once first." }

        $result = Compare-QnnSavedDeviceOutput -HostOutputFolder $latest.FullName -ReferenceJson $artifact.ReferencePath
        $script:LastDeployOutputFolder = $latest.FullName
        $kindLabel = if ([string]$result.Kind -eq 'parakeet_tdt_joint') { 'Parakeet TDT joint' } elseif ([string]$result.Kind -eq 'parakeet_tdt_predictor') { 'Parakeet TDT predictor' } elseif ([string]$result.Kind -eq 'parakeet_encoder') { 'Parakeet encoder' } elseif ([string]$result.Kind -eq 'parakeet_encoder_projector') { 'Parakeet encoder projector' } else { 'HTP proof' }
        $txtDeployResult.Text = "Saved-output comparison: $kindLabel`r`nNumeric result: $(if ($result.Pass) { 'PASS' } else { 'FAIL' })`r`nMax absolute error: $($result.MaxAbsError)`r`nMean absolute error: $($result.MeanAbsError)`r`nRMSE: $($result.Rmse)`r`nCosine similarity: $($result.Cosine)`r`nPadded max abs: $($result.PaddedMaxAbs)`r`n`r`nThresholds:`r`n$($result.ThresholdSummary)`r`n`r`nComparison:`r`n$($result.ComparisonSummary)`r`n`r`nDiagnostic bundle:`r`n$($result.ComparisonBundle)`r`n`r`nNo ADB push or device execution was repeated."
        Append-Log ("Saved HTP comparison: {0}; result={1}; max_abs={2}; mean_abs={3}; rmse={4}; cosine={5}`r`nThresholds:`r`n{6}`r`nComparison:`r`n{7}" -f $kindLabel,$(if ($result.Pass) { 'PASS' } else { 'FAIL' }),$result.MaxAbsError,$result.MeanAbsError,$result.Rmse,$result.Cosine,$result.ThresholdSummary,$result.ComparisonSummary)
        if ($result.Pass) {
            Set-Status $lblDeployRun ($kindLabel + ' HTP PASS') 'good'
            [System.Windows.Forms.MessageBox]::Show("$kindLabel saved device output passes the configured FP16 validation profile.`r`n`r`nThe phone was not rerun and the context was not pushed again.",($kindLabel + ' HTP PASS'),[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
        } else {
            Set-Status $lblDeployRun ($kindLabel + ' saved comparison FAIL') 'bad'
        }
    }
})

$btnRuntimeBaseline.Add_Click({
    Invoke-UiAction 'Benchmarking four validated QNN contexts on Galaxy S26...' {
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'No verified Galaxy S26 is connected.' }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Android QNN runtime bundle is incomplete. Missing: ' + ($runtime.Missing -join ', ')) }

        $all = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
        $encoder = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_encoder'
        $projector = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_encoder_projector'
        $predictor = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_tdt_predictor'
        $joint = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_tdt_joint'
        if ($null -eq $encoder -or $null -eq $projector -or $null -eq $predictor -or $null -eq $joint) {
            throw 'Runtime baseline requires an HTP PASS encoder, projector, predictor, and joint context with their reference manifests. Recompare the saved output for any graph that is missing PASS evidence; no model recompilation is required.'
        }

        $python = Join-Path $Root 'tools\python311\python.exe'
        if (-not (Test-Path -LiteralPath $python) -or -not (Test-CompatiblePython -PythonPath $python)) { $python = Install-PythonFromUi }
        if (-not (Test-Path -LiteralPath $script:RuntimeBenchmarkPath -PathType Leaf)) { throw 'runtime_benchmark.py is missing from this Builder release.' }
        $outDir = Join-Path $script:CurrentWorkspace 'runtime-benchmark'
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
        $reportPath = Join-Path $outDir 'qnn-separate-context-baseline.json'
        $args = @(
            $script:RuntimeBenchmarkPath,'baseline',
            '--builder-root',$Root,
            '--adb',$txtAdb.Text,
            '--serial',$script:DeviceSerial,
            '--runtime-root',$txtRuntimeRoot.Text,
            '--encoder-context',[string]$encoder.Path,
            '--encoder-reference',[string]$encoder.ReferencePath,
            '--projector-context',[string]$projector.Path,
            '--projector-reference',[string]$projector.ReferencePath,
            '--predictor-context',[string]$predictor.Path,
            '--predictor-reference',[string]$predictor.ReferencePath,
            '--joint-context',[string]$joint.Path,
            '--joint-reference',[string]$joint.ReferencePath,
            '--output-json',$reportPath
        )
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 10800
        if ($r.ExitCode -ne 0) { throw (($r.StdErr + [Environment]::NewLine + $r.StdOut).Trim()) }
        if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) { throw 'Runtime benchmark completed without creating its JSON report.' }
        $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if (-not [bool]$report.complete) { throw 'Runtime benchmark report did not mark the baseline complete.' }
        $summary = $report.summary
        $txtRuntimeBaselineResult.Text = "Separate-context qnn-net-run development baseline: COMPLETE`r`n`r`nEncoder single median host wall: $($summary.encoder_single_median_host_wall_ms) ms`r`nEncoder burst-amortized upper bound: $($summary.encoder_burst_amortized_upper_bound_ms) ms`r`nProjector warm amortized upper bound: $($summary.projector_warm_amortized_upper_bound_ms) ms`r`nPredictor warm amortized upper bound: $($summary.predictor_warm_amortized_upper_bound_ms) ms`r`nJoint warm amortized upper bound: $($summary.joint_warm_amortized_upper_bound_ms) ms`r`nPredictor + joint warm amortized upper bound: $($summary.predictor_plus_joint_warm_amortized_upper_bound_ms) ms`r`nSeparate context bytes total: $($summary.separate_context_bytes_total)`r`n`r`nReport:`r`n$reportPath`r`n`r`nImportant: these are qnn-net-run/ADB/raw-file development-harness upper bounds, not production JNI/ORT latency."
        Set-Status $lblRuntimeBaseline 'Separate-context development baseline COMPLETE' 'good'
        Append-Log ('Runtime baseline report: ' + $reportPath)
        Append-Log ("Runtime baseline summary: encoder_single_ms={0}; encoder_burst_upper_ms={1}; projector_warm_amortized_upper_ms={2}; predictor_warm_amortized_upper_ms={3}; joint_warm_amortized_upper_ms={4}; predictor_plus_joint_warm_amortized_upper_ms={5}" -f $summary.encoder_single_median_host_wall_ms,$summary.encoder_burst_amortized_upper_bound_ms,$summary.projector_warm_amortized_upper_bound_ms,$summary.predictor_warm_amortized_upper_bound_ms,$summary.joint_warm_amortized_upper_bound_ms,$summary.predictor_plus_joint_warm_amortized_upper_bound_ms)
        [System.Windows.Forms.MessageBox]::Show("Separate-context runtime baseline completed on the Galaxy S26.`r`n`r`nNo model was recompiled. The Builder exercised the already-validated encoder, projector, predictor, and joint contexts with repeated qnn-net-run inputs and auto-cleaned its phone staging folder.`r`n`r`nThis is a development-harness upper-bound baseline, not the production runtime bake-off.`r`n`r`nNext: Package -> Build v1-draft package to bind this baseline report, then upload the model-stripped package.",'Runtime baseline complete',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})


$btnBuildLinkedDecoder.Add_Click({
    Invoke-UiAction 'Compiling + linking the three decoder graphs into one QNN context...' {
        $all = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
        $projector = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_encoder_projector'
        $predictor = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_tdt_predictor'
        $joint = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_tdt_joint'
        if ($null -eq $projector -or $null -eq $predictor -or $null -eq $joint) {
            throw 'Linked decoder build requires HTP PASS projector, predictor and joint contexts. Recompare any missing graph first; do not recompile the individual graphs.'
        }
        foreach ($a in @($projector,$predictor,$joint)) {
            if ([string]::IsNullOrWhiteSpace([string]$a.SourceOnnx) -or -not (Test-Path -LiteralPath $a.SourceOnnx)) {
                throw ('Source ONNX for validated artifact is missing: ' + [string]$a.Path + '. Keep the source workspace used to compile each decoder graph.')
            }
        }
        $hubPython = Ensure-AiHubEnvironmentFromUi
        $verify = Test-PythonAiHubClient -PythonPath $hubPython
        if ($verify.ExitCode -ne 0) { throw (($verify.StdErr + [Environment]::NewLine + $verify.StdOut).Trim()) }
        $compiledDir = Join-Path $script:CurrentWorkspace 'compiled'
        New-Item -ItemType Directory -Path $compiledDir -Force | Out-Null
        $out = Join-Path $compiledDir 'parakeet-tdt-decoder-linked-sm8850-v81-fp16.bin'
        $r = Invoke-AiHubLinkedDecoderCompile -PythonPath $hubPython -BridgePath $script:BridgePath -ProjectorOnnx ([string]$projector.SourceOnnx) -PredictorOnnx ([string]$predictor.SourceOnnx) -JointOnnx ([string]$joint.SourceOnnx) -OutputPath $out -ApiToken $txtToken.Text -QairtVersion $txtQairt.Text
        if ($r.ExitCode -ne 0) { throw (($r.StdErr + [Environment]::NewLine + $r.StdOut).Trim()) }
        if (-not (Test-Path -LiteralPath $out -PathType Leaf)) { throw 'AI Hub linked decoder workflow completed without downloading the QNN context binary.' }
        $meta = [ordered]@{
            schema = 1
            kind = 'parakeet_tdt_decoder_linked'
            target = 'Samsung Galaxy S26 (Family)'
            precision = 'float16'
            graphSelectors = [ordered]@{ encoder_projector='wr_projector'; predictor='wr_predictor'; joint='wr_joint' }
            componentContexts = [ordered]@{
                encoder_projector = [ordered]@{ path=[string]$projector.Path; sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $projector.Path).Hash.ToLowerInvariant(); reference=[string]$projector.ReferencePath }
                predictor = [ordered]@{ path=[string]$predictor.Path; sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $predictor.Path).Hash.ToLowerInvariant(); reference=[string]$predictor.ReferencePath }
                joint = [ordered]@{ path=[string]$joint.Path; sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $joint.Path).Hash.ToLowerInvariant(); reference=[string]$joint.ReferencePath }
            }
            sourceOnnx = [ordered]@{ encoder_projector=[string]$projector.SourceOnnx; predictor=[string]$predictor.SourceOnnx; joint=[string]$joint.SourceOnnx }
        }
        $meta | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath ($out + '.metadata.json') -Encoding UTF8
        $sizeMiB = [Math]::Round(((Get-Item -LiteralPath $out).Length / 1MB),2)
        $txtLinkedDecoderResult.Text = "Linked decoder context: BUILT`r`n`r`nContext:`r`n$out`r`nSize: $sizeMiB MiB`r`nGraphs: wr_projector / wr_predictor / wr_joint`r`n`r`nNext: click Validate + benchmark. With the minimal phone runtime, that executes all three linked graphs together using three qnn-net-run input-list descriptors, compares each graph against its pinned reference, then measures an all-graphs development upper bound."
        Set-Status $lblLinkedDecoder 'Linked decoder built; validation pending.' 'good'
        Append-Log ('Linked decoder context: ' + $out)
        Append-Log (($r.StdOut + [Environment]::NewLine + $r.StdErr).Trim())
        Refresh-DeployArtifacts -PreferredPath $out
        Update-DeployWorkflowState
        [System.Windows.Forms.MessageBox]::Show("Linked projector + predictor + joint QNN context downloaded successfully.`r`n`r`nThe logical graphs remain separate; only the physical context is shared.`r`n`r`nNext: Validate + benchmark.",'Linked decoder ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnValidateLinkedDecoder.Add_Click({
    Invoke-UiAction 'Validating and benchmarking the linked decoder context on Galaxy S26...' {
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'No verified Galaxy S26 is connected.' }
        $runtime = Get-QnnAndroidRuntimeBundleInfo -RuntimeRoot $txtRuntimeRoot.Text
        if (-not $runtime.IsComplete) { throw ('Android QNN runtime bundle is incomplete. Missing: ' + ($runtime.Missing -join ', ')) }
        $linked = Get-LinkedDecoderArtifact
        if ($null -eq $linked) { throw 'No compatible linked decoder context was found. Click Build linked decoder first.' }
        $linkedContextSha = (Get-FileHash -Algorithm SHA256 -LiteralPath $linked.Path).Hash.ToLowerInvariant()
        $linkedContextBytes = [int64](Get-Item -LiteralPath $linked.Path).Length
        $all = @(Get-QnnCompiledArtifacts -BuilderRoot $Root)
        $projector = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_encoder_projector'
        $predictor = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_tdt_predictor'
        $joint = Get-RuntimeBaselineArtifact -Artifacts $all -Kind 'parakeet_tdt_joint'
        if ($null -eq $projector -or $null -eq $predictor -or $null -eq $joint) { throw 'Validated decoder references are no longer available.' }

        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $proofRoot = Join-Path (Join-Path $script:CurrentWorkspace 'device') ('linked-decoder-test-' + $stamp)
        New-Item -ItemType Directory -Path $proofRoot -Force | Out-Null
        $proofSpecs = @(
            [pscustomobject]@{ Role='encoder_projector'; Selector='wr_projector'; Artifact=$projector },
            [pscustomobject]@{ Role='predictor'; Selector='wr_predictor'; Artifact=$predictor },
            [pscustomobject]@{ Role='joint'; Selector='wr_joint'; Artifact=$joint }
        )
        $proofSummary = [ordered]@{}
        try {
            Append-Log 'Linked decoder correctness: executing wr_projector + wr_predictor + wr_joint together with three qnn-net-run input-list descriptors.'
            Append-Log 'Reason: the minimal six-file Android validation runtime does not include libQnnHtpNetRunExtensions.so, so qnn-net-run graph-selection context configs are not available in this harness.'
            $multiProof = Invoke-QnnLinkedDecoderProofOnDevice -AdbPath $txtAdb.Text -Serial $script:DeviceSerial -ContextPath $linked.Path -ProjectorReferenceJson $projector.ReferencePath -PredictorReferenceJson $predictor.ReferencePath -JointReferenceJson $joint.ReferencePath -RuntimeRoot $txtRuntimeRoot.Text -HostOutputFolder $proofRoot
            foreach ($spec in $proofSpecs) {
                $roleKey = [string]$spec.Role
                $comparisonProp = $multiProof.Comparisons.PSObject.Properties[$roleKey]
                if ($null -eq $comparisonProp -or $null -eq $comparisonProp.Value) { throw ("Linked decoder combined proof did not return comparison evidence for {0}." -f $spec.Role) }
                $res = $comparisonProp.Value
                $proofSummary[$roleKey] = [ordered]@{
                    pass = [bool]$res.Pass
                    graph_name = [string]$spec.Selector
                    execution_mode = 'all_three_graphs_multi_input_list'
                    reference_sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $spec.Artifact.ReferencePath).Hash.ToLowerInvariant()
                    comparison_sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $res.ComparisonReport).Hash.ToLowerInvariant()
                    max_abs = [double]$res.MaxAbsError
                    mean_abs = [double]$res.MeanAbsError
                    rmse = [double]$res.Rmse
                    cosine = [double]$res.Cosine
                    thresholds = [string]$res.ThresholdSummary
                    details = [string]$res.ComparisonSummary
                }
                Append-Log ("Linked decoder correctness {0}: PASS={1}; cosine={2:F9}; MAE={3:G6}; RMSE={4:G6}; max={5:G6}" -f $spec.Role,[bool]$res.Pass,[double]$res.Cosine,[double]$res.MeanAbsError,[double]$res.Rmse,[double]$res.MaxAbsError)
                if (-not $res.Pass) { throw ("Linked decoder graph {0} failed the existing HTP numeric/decision reference. Individual graph contexts remain the validated fallback." -f $spec.Role) }
            }
            if (-not [bool]$multiProof.Pass) { throw 'Linked decoder combined three-graph proof did not report PASS.' }
        } finally {
            try { [void](Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial) }
            catch {
                Append-Log ('Linked decoder test cleanup failed: ' + $_.Exception.Message)
                Set-Status $lblPhoneStorage 'Phone cleanup failed; use Clean phone files' 'bad'
            }
        }

        $python = Join-Path $Root 'tools\python311\python.exe'
        if (-not (Test-Path -LiteralPath $python) -or -not (Test-CompatiblePython -PythonPath $python)) { $python = Install-PythonFromUi }
        $outDir = Join-Path $script:CurrentWorkspace 'runtime-benchmark'
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
        $reportPath = Join-Path $outDir 'qnn-linked-decoder-candidate.json'
        $args = @(
            $script:RuntimeBenchmarkPath,'linked-decoder',
            '--builder-root',$Root,
            '--adb',$txtAdb.Text,
            '--serial',$script:DeviceSerial,
            '--runtime-root',$txtRuntimeRoot.Text,
            '--linked-context',[string]$linked.Path,
            '--projector-reference',[string]$projector.ReferencePath,
            '--predictor-reference',[string]$predictor.ReferencePath,
            '--joint-reference',[string]$joint.ReferencePath,
            '--output-json',$reportPath
        )
        $bench = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 10800
        if ($bench.ExitCode -ne 0) { throw (($bench.StdErr + [Environment]::NewLine + $bench.StdOut).Trim()) }
        if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) { throw 'Linked decoder benchmark completed without its JSON report.' }
        $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
        if (-not [bool]$report.complete) { throw 'Linked decoder benchmark did not mark the candidate complete.' }
        $reportSha = [string]$report.linked_context.sha256
        $reportBytes = [int64]$report.linked_context.size_bytes
        $currentLinkedSha = (Get-FileHash -Algorithm SHA256 -LiteralPath $linked.Path).Hash.ToLowerInvariant()
        if ($reportSha -ne $linkedContextSha -or $currentLinkedSha -ne $linkedContextSha -or $reportBytes -ne $linkedContextBytes) {
            throw 'Linked decoder context changed between correctness proof and benchmark; refusing to bind mixed evidence.'
        }
        $validationObject = [pscustomobject]@{ pass=$true; linked_context_sha256=$linkedContextSha; graphs=[pscustomobject]$proofSummary; proof_root=$proofRoot }
        $report | Add-Member -NotePropertyName 'device_validation' -NotePropertyValue $validationObject -Force
        $report | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $reportPath -Encoding UTF8

        $summary = $report.summary
        $linkedBytes = [int64]$summary.linked_context_bytes
        $separateDecoderBytes = [int64]$projector.Bytes + [int64]$predictor.Bytes + [int64]$joint.Bytes
        $sizeDelta = $linkedBytes - $separateDecoderBytes
        $sizePct = if ($separateDecoderBytes -gt 0) { [Math]::Round((100.0 * $linkedBytes / $separateDecoderBytes),2) } else { 0 }
        $allGraphsWarm = [double]$summary.all_graphs_warm_amortized_upper_bound_ms
        $txtLinkedDecoderResult.Text = "Linked decoder candidate: VALIDATED + BENCHMARKED`r`n`r`nAll three graph decisions/numerics: PASS`r`nExecution mode: all three linked graphs with three qnn-net-run input lists`r`nLinked context bytes: $linkedBytes`r`nSeparate decoder context bytes: $separateDecoderBytes`r`nLinked/separate size: $sizePct% (delta $sizeDelta bytes)`r`nAll-graphs warm amortized development upper bound: $allGraphsWarm ms`r`nPer-graph selected latency available in this harness: NO`r`n`r`nReport:`r`n$reportPath`r`n`r`nImportant: the minimal validation runtime does not include libQnnHtpNetRunExtensions.so. This proves the linked context can execute all three HTP graphs and preserve their numerics; persistent Android graph-handle switching remains the next runtime gate."
        Set-Status $lblLinkedDecoder 'Linked decoder candidate VALIDATED' 'good'
        Append-Log ('Linked decoder candidate report: ' + $reportPath)
        Append-Log (($bench.StdOut + [Environment]::NewLine + $bench.StdErr).Trim())
        [System.Windows.Forms.MessageBox]::Show("Linked decoder context passed projector, predictor and joint HTP comparisons in one three-graph qnn-net-run execution, and the all-graphs development benchmark completed.`r`n`r`nRebuild the v1-draft package to bind this candidate evidence. The package remains runtime-pending until the persistent Android direct-QNN vs ORT-QNN bake-off.",'Linked decoder validated',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnCleanPhone.Add_Click({
    Invoke-UiAction 'Cleaning WhisperRecorder NPU test files from phone...' {
        if (-not $script:DeviceSerial) { Refresh-Device }
        if (-not $script:DeviceSerial) { throw 'No verified Galaxy S26 is connected.' }
        $clean = Remove-AllBuilderDeviceTestFiles -AdbPath $txtAdb.Text -Serial $script:DeviceSerial
        $message = if ($clean.WasPresent) { ('Removed approximately {0:N1} MiB of Builder test files from the phone.' -f $clean.FreedMiB) } else { 'No Builder test files were present on the phone.' }
        Set-Status $lblPhoneStorage 'Phone test folder clean (0 MiB staged).' 'good'
        Set-Status $lblDeployRun 'Phone test files cleaned' 'good'
        $txtDeployResult.Text = $message + "`r`n`r`nDeleted only Builder-owned test folders under /data/local/tmp:`r`nwhisperrecorder-npu-proof`r`nwhisperrecorder-omi-v2-[12 hex chars]`r`n`r`nWhisperRecorder app data and normal phone files were not touched."
        Append-Log ('Phone cleanup: ' + $message)
    }
})

$btnOpenDeployOutput.Add_Click({
    try {
        if ($script:LastDeployOutputFolder -and (Test-Path -LiteralPath $script:LastDeployOutputFolder)) { Open-Folder $script:LastDeployOutputFolder }
        else { Open-Folder (Join-Path $script:CurrentWorkspace 'device') }
    } catch { Show-Error $_ }
})

$btnProbeContract.Add_Click({
    Invoke-UiAction 'Probing Parakeet TDT checkpoint contract...' {
        $modelDir = $txtSourceModel.Text
        if ([string]::IsNullOrWhiteSpace($modelDir) -or -not (Test-Path -LiteralPath (Join-Path $modelDir 'model.safetensors') -PathType Leaf)) {
            throw 'Download/select the official Parakeet Transformers checkpoint on Model + AI Hub first.'
        }
        $python = Join-Path $Root 'tools\python311\python.exe'
        if (-not (Test-Path -LiteralPath $python) -or -not (Test-CompatiblePython -PythonPath $python)) { $python = Install-PythonFromUi }
        $contract = Join-Path $modelDir 'whisperrecorder-tdt-contract.json'
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:ParakeetContractPath,'--model-dir',$modelDir,'--output',$contract) -TimeoutSeconds 300
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtPackageResult.Text = Get-Content -LiteralPath $contract -Raw
        Append-Log ('Verified TDT checkpoint contract: ' + $contract)
    }
})

$btnBuildPackage.Add_Click({
    Invoke-UiAction 'Building WhisperRecorder Android NPU Package v1-draft...' {
        [void](Populate-PackageArtifactsFromValidatedWorkspaces)
        $encoderArtifact = [string]$txtCompiled.Text
        if ([string]::IsNullOrWhiteSpace($encoderArtifact) -or -not (Test-Path -LiteralPath $encoderArtifact -PathType Leaf)) { throw 'No HTP-PASS Parakeet encoder context could be recovered. Keep or browse to the validated encoder workspace before packaging.' }
        if ([string]::IsNullOrWhiteSpace($txtPackageFolder.Text)) { throw 'Choose a package folder.' }
        $python = Join-Path $Root 'tools\python311\python.exe'
        if (-not (Test-Path -LiteralPath $python) -or -not (Test-CompatiblePython -PythonPath $python)) { $python = Install-PythonFromUi }
        $args = @($script:PackageBuilderPath,'build','--builder-root',$Root,'--compiled-artifact',$encoderArtifact,'--output-folder',$txtPackageFolder.Text,'--model-id',$txtModelId.Text,'--display-name',$txtModelName.Text)
        $contract = ''
        if ($txtSourceModel.Text -and (Test-Path -LiteralPath $txtSourceModel.Text -PathType Container)) {
            $candidate = Join-Path $txtSourceModel.Text 'whisperrecorder-tdt-contract.json'
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { $contract = $candidate }
        }
        if ($contract) { $args += @('--contract-json',$contract) }
        if (-not [string]::IsNullOrWhiteSpace($txtProjectorCompiled.Text)) {
            if (-not (Test-Path -LiteralPath $txtProjectorCompiled.Text -PathType Leaf)) { throw 'The selected projector artifact does not exist.' }
            $args += @('--projector-artifact',$txtProjectorCompiled.Text)
            $projectorMetaPath = $txtProjectorCompiled.Text + '.metadata.json'
            if (Test-Path -LiteralPath $projectorMetaPath -PathType Leaf) {
                try {
                    $projectorMeta = Get-Content -LiteralPath $projectorMetaPath -Raw | ConvertFrom-Json
                    if ($projectorMeta.referencePath -and (Test-Path -LiteralPath ([string]$projectorMeta.referencePath) -PathType Leaf)) { $args += @('--projector-reference-json',[string]$projectorMeta.referencePath) }
                } catch { Append-Log ('Warning: could not read projector metadata reference path: ' + $_.Exception.Message) }
            }
        }
        if (-not [string]::IsNullOrWhiteSpace($txtPredictorCompiled.Text)) {
            if (-not (Test-Path -LiteralPath $txtPredictorCompiled.Text -PathType Leaf)) { throw 'The selected predictor artifact does not exist.' }
            $args += @('--predictor-artifact',$txtPredictorCompiled.Text)
            $predictorMetaPath = $txtPredictorCompiled.Text + '.metadata.json'
            if (Test-Path -LiteralPath $predictorMetaPath -PathType Leaf) {
                try {
                    $predictorMeta = Get-Content -LiteralPath $predictorMetaPath -Raw | ConvertFrom-Json
                    if ($predictorMeta.referencePath -and (Test-Path -LiteralPath ([string]$predictorMeta.referencePath) -PathType Leaf)) { $args += @('--predictor-reference-json',[string]$predictorMeta.referencePath) }
                } catch { Append-Log ('Warning: could not read predictor metadata reference path: ' + $_.Exception.Message) }
            }
        }
        if (-not [string]::IsNullOrWhiteSpace($txtJointCompiled.Text)) {
            if (-not (Test-Path -LiteralPath $txtJointCompiled.Text -PathType Leaf)) { throw 'The selected joint artifact does not exist.' }
            $args += @('--joint-artifact',$txtJointCompiled.Text)
            $jointMetaPath = $txtJointCompiled.Text + '.metadata.json'
            if (Test-Path -LiteralPath $jointMetaPath -PathType Leaf) {
                try {
                    $jointMeta = Get-Content -LiteralPath $jointMetaPath -Raw | ConvertFrom-Json
                    if ($jointMeta.referencePath -and (Test-Path -LiteralPath ([string]$jointMeta.referencePath) -PathType Leaf)) { $args += @('--joint-reference-json',[string]$jointMeta.referencePath) }
                } catch { Append-Log ('Warning: could not read joint metadata reference path: ' + $_.Exception.Message) }
            }
        }
        $referenceCandidate = $encoderArtifact + '.metadata.json'
        if (Test-Path -LiteralPath $referenceCandidate -PathType Leaf) {
            try {
                $compiledMeta = Get-Content -LiteralPath $referenceCandidate -Raw | ConvertFrom-Json
                if ($compiledMeta.referencePath -and (Test-Path -LiteralPath ([string]$compiledMeta.referencePath) -PathType Leaf)) { $args += @('--reference-json',[string]$compiledMeta.referencePath) }
            } catch { Append-Log ('Warning: could not read compiled metadata reference path: ' + $_.Exception.Message) }
        }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 1800
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $manifest = Join-Path $txtPackageFolder.Text 'manifest.json'
        $txtPackageResult.Text = Get-Content -LiteralPath $manifest -Raw
        Append-Log ('Package v1-draft manifest: ' + $manifest)
        $txtHfPackageFolder.Text = $txtPackageFolder.Text
        if ([string]::IsNullOrWhiteSpace($txtHfDeploymentFolder.Text)) { $txtHfDeploymentFolder.Text = Join-Path $Root 'deployment\parakeet-tdt-0.6b-v3-sm8850-v81-fp16' }
        if ($txtJointCompiled.Text -and (Test-Path -LiteralPath $txtJointCompiled.Text -PathType Leaf)) {
            try {
                $builtManifest = Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
                if ([string]$builtManifest.package_status -eq 'controller_validated_runtime_pending') {
                    $baselineMeasured = $false
                    $runtimeProp = $builtManifest.PSObject.Properties['runtime']
                    if ($null -ne $runtimeProp -and $null -ne $runtimeProp.Value) {
                        $devProp = $runtimeProp.Value.PSObject.Properties['development_baseline']
                        if ($null -ne $devProp -and $null -ne $devProp.Value) {
                            $statusProp = $devProp.Value.PSObject.Properties['status']
                            if ($null -ne $statusProp -and [string]$statusProp.Value -eq 'measured') { $baselineMeasured = $true }
                        }
                    }
                    $linkedValidated = $false
                    if ($null -ne $runtimeProp -and $null -ne $runtimeProp.Value) {
                        $linkedProp = $runtimeProp.Value.PSObject.Properties['linked_decoder_candidate']
                        if ($null -ne $linkedProp -and $null -ne $linkedProp.Value) {
                            $linkedStatus = $linkedProp.Value.PSObject.Properties['status']
                            if ($null -ne $linkedStatus -and [string]$linkedStatus.Value -eq 'validated') { $linkedValidated = $true }
                        }
                    }
                    if ($baselineMeasured -and $linkedValidated) { Append-Log 'Package includes all four HTP graph proofs + controller PASS + separate-context baseline + validated linked decoder candidate. Persistent Android direct-QNN vs ORT-QNN bake-off remains pending.' }
                    elseif ($baselineMeasured) { Append-Log 'Package includes all four HTP graph proofs + controller PASS + separate-context runtime baseline. Linked-context/Android runtime bake-off remains pending.' }
                    else { Append-Log 'Package includes all four HTP-validated neural graphs + a bound full TDT controller PASS. Run Benchmark 4 contexts before the production runtime bake-off.' }
                }
                else { Append-Log 'Package includes all four HTP-validated neural graphs. Run Validate TDT controller to advance package status before runtime bake-off.' }
            } catch { Append-Log 'Package includes encoder + projector + predictor + HTP-validated TDT joint.' }
        } elseif ($txtPredictorCompiled.Text -and (Test-Path -LiteralPath $txtPredictorCompiled.Text -PathType Leaf)) { Append-Log 'Package includes encoder + projector + HTP-validated predictor core; joint remains pending.' } elseif ($txtProjectorCompiled.Text -and (Test-Path -LiteralPath $txtProjectorCompiled.Text -PathType Leaf)) { Append-Log 'Package includes encoder + projector; predictor and joint remain pending.' } else { Append-Log 'Package status remains encoder_only until the projector, predictor and joint HTP graphs are validated.' }
    }
})

$btnZipPackage.Add_Click({
    Invoke-UiAction 'Creating deterministic model package ZIP...' {
        if (-not (Test-Path -LiteralPath $txtPackageFolder.Text -PathType Container)) { throw 'Build the package folder first.' }
        $python = Join-Path $Root 'tools\python311\python.exe'
        if (-not (Test-Path -LiteralPath $python) -or -not (Test-CompatiblePython -PythonPath $python)) { $python = Install-PythonFromUi }
        $zip = Join-Path (Split-Path -Parent $txtPackageFolder.Text) ((Split-Path -Leaf $txtPackageFolder.Text) + '.zip')
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:PackageBuilderPath,'zip','--folder',$txtPackageFolder.Text,'--output',$zip) -TimeoutSeconds 3600
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        Append-Log ('Deterministic package ZIP: ' + $zip)
        [System.Windows.Forms.MessageBox]::Show("Package ZIP created and verified:`n$zip",'NPU Builder') | Out-Null
    }
})

$btnOpenPackage.Add_Click({
    try { Open-Folder $txtPackageFolder.Text } catch { Show-Error $_ }
})

$btnHfBuild.Add_Click({
    Invoke-UiAction 'Creating canonical Hugging Face deployment folder...' {
        $recoveredPackage = Populate-HfPackageFolderFromValidatedPackage
        if ([string]::IsNullOrWhiteSpace($recoveredPackage)) { throw 'No complete unstripped controller-validated v1-draft package was found. The Builder searched existing workspaces. Build the v1-draft package once on Package, then return here; do not rerun AI Hub or the phone.' }
        if ([string]::IsNullOrWhiteSpace($txtHfDeploymentFolder.Text)) { throw 'Choose the deployment output folder.' }
        $python = Get-LocalBuilderPython
        $args = @($script:HfDeployPath,'build','--package-folder',$txtHfPackageFolder.Text,'--output-folder',$txtHfDeploymentFolder.Text)
        $linked = Get-ValidatedLinkedDecoderArtifactForPackage -PackageFolder $txtHfPackageFolder.Text
        if ($null -ne $linked -and (Test-Path -LiteralPath $linked.Path -PathType Leaf)) {
            $args += @('--linked-artifact',[string]$linked.Path)
            Append-Log ('Hugging Face deployment recovered the exact linked decoder bound to package validation and will include it under experimental/: ' + [string]$linked.Path)
        }
        $r = Invoke-ProcessCapture -FilePath $python -Arguments $args -TimeoutSeconds 7200
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtHfResult.Text = $r.StdOut
        Set-Status $lblHfLocal 'Deployment READY / audit PASS' 'good'
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Canonical Hugging Face deployment folder created and audited:`r`n`r`n$($txtHfDeploymentFolder.Text)`r`n`r`nThe repository remains runtime-pending; experimental linked decoder is not production-selected.",'Hugging Face deployment ready',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnHfAudit.Add_Click({
    Invoke-UiAction 'Auditing Hugging Face deployment folder...' {
        if ([string]::IsNullOrWhiteSpace($txtHfDeploymentFolder.Text) -or -not (Test-Path -LiteralPath $txtHfDeploymentFolder.Text -PathType Container)) { throw 'Create/select the deployment folder first.' }
        $python = Get-LocalBuilderPython
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:HfDeployPath,'audit','--folder',$txtHfDeploymentFolder.Text) -TimeoutSeconds 7200
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtHfResult.Text = $r.StdOut
        Set-Status $lblHfLocal 'Deployment audit PASS' 'good'
    }
})

$btnHfOpenFolder.Add_Click({ try { Open-Folder $txtHfDeploymentFolder.Text } catch { Show-Error $_ } })

$btnHfUpload.Add_Click({
    Invoke-UiAction 'Uploading WhisperRecorder NPU deployment to Hugging Face...' {
        if ([string]::IsNullOrWhiteSpace($txtHfDeploymentFolder.Text) -or -not (Test-Path -LiteralPath $txtHfDeploymentFolder.Text -PathType Container)) { throw 'Create and audit the deployment folder first.' }
        if ([string]::IsNullOrWhiteSpace($txtHfRepoId.Text) -or $txtHfRepoId.Text -notmatch '^[^/\s]+/[^/\s]+$') { throw 'Enter the Hugging Face repository as namespace/name.' }
        if ([string]::IsNullOrWhiteSpace($txtHfToken.Text)) { throw 'Paste a Hugging Face write token. It is used only for this upload session and is not saved.' }
        Register-SensitiveOutputValue $txtHfToken.Text
        $python = Ensure-HuggingFaceUploaderPython
        $revision = if ([string]::IsNullOrWhiteSpace($txtHfRevision.Text)) { 'main' } else { [string]$txtHfRevision.Text }
        $visibility = if ($cmbHfVisibility.SelectedItem) { [string]$cmbHfVisibility.SelectedItem } else { 'private' }
        $display = ('"' + $python + '" "' + $script:HfDeployPath + '" upload --folder "' + $txtHfDeploymentFolder.Text + '" --repo-id "' + $txtHfRepoId.Text + '" --revision "' + $revision + '" --visibility ' + $visibility + ' [token via protected environment]')
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:HfDeployPath,'upload','--folder',$txtHfDeploymentFolder.Text,'--repo-id',$txtHfRepoId.Text,'--revision',$revision,'--visibility',$visibility) -Environment @{ WR_HF_TOKEN=$txtHfToken.Text } -TimeoutSeconds 21600 -DisplayCommand $display
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtHfResult.Text = $r.StdOut
        Set-Status $lblHfRemote 'Upload completed; verify remote next.' 'good'
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Upload completed.`r`n`r`nNext click Verify remote. This checks the committed repository against the audited local deployment without re-downloading the large encoder context.",'Hugging Face upload complete',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnHfVerifyRemote.Add_Click({
    Invoke-UiAction 'Verifying Hugging Face remote deployment...' {
        if ([string]::IsNullOrWhiteSpace($txtHfDeploymentFolder.Text) -or -not (Test-Path -LiteralPath $txtHfDeploymentFolder.Text -PathType Container)) { throw 'Create/select the local deployment folder first.' }
        if ([string]::IsNullOrWhiteSpace($txtHfRepoId.Text) -or $txtHfRepoId.Text -notmatch '^[^/\s]+/[^/\s]+$') { throw 'Enter the Hugging Face repository as namespace/name.' }
        if ([string]::IsNullOrWhiteSpace($txtHfToken.Text)) { throw 'Paste a Hugging Face token so the Builder can verify a private repository.' }
        Register-SensitiveOutputValue $txtHfToken.Text
        $python = Ensure-HuggingFaceUploaderPython
        $revision = if ([string]::IsNullOrWhiteSpace($txtHfRevision.Text)) { 'main' } else { [string]$txtHfRevision.Text }
        $display = ('"' + $python + '" "' + $script:HfDeployPath + '" verify-remote --folder "' + $txtHfDeploymentFolder.Text + '" --repo-id "' + $txtHfRepoId.Text + '" --revision "' + $revision + '" [token via protected environment]')
        $r = Invoke-ProcessCapture -FilePath $python -Arguments @($script:HfDeployPath,'verify-remote','--folder',$txtHfDeploymentFolder.Text,'--repo-id',$txtHfRepoId.Text,'--revision',$revision) -Environment @{ WR_HF_TOKEN=$txtHfToken.Text } -TimeoutSeconds 7200 -DisplayCommand $display
        if ($r.ExitCode -ne 0) { throw ($r.StdErr + [Environment]::NewLine + $r.StdOut) }
        $txtHfResult.Text = $r.StdOut
        Set-Status $lblHfRemote 'REMOTE VERIFIED - ready for Android URL' 'good'
        Save-Settings
        [System.Windows.Forms.MessageBox]::Show("Remote Hugging Face deployment verified.`r`n`r`nYou can now give the repository link/revision to the Android integration chat. The package is still production_ready=false until that runtime bake-off and whole-recording validation are complete.",'Hugging Face remote verified',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnHfOpenRepo.Add_Click({
    try { Open-Url (Get-HuggingFaceRepoUrl -RepoId $txtHfRepoId.Text) } catch { Show-Error $_ }
})


$btnUseExistingContext.Add_Click({
    Refresh-DeployArtifacts
    if ($cmbDeployArtifact.Items.Count -gt 0) {
        $wantPattern = if ([string]$cmbRecipe.SelectedItem -eq 'NVIDIA Parakeet TDT 0.6B v3' -and $txtOnnx.Text -match 'tdt-joint') { '^parakeet-tdt-joint-step-sm8850-v81-fp16\.bin$' } elseif ([string]$cmbRecipe.SelectedItem -eq 'NVIDIA Parakeet TDT 0.6B v3' -and $txtOnnx.Text -match 'tdt-predictor') { '^parakeet-tdt-predictor-step-sm8850-v81-fp16\.bin$' } elseif ([string]$cmbRecipe.SelectedItem -eq 'NVIDIA Parakeet TDT 0.6B v3' -and $txtOnnx.Text -match 'encoder-projector') { '^parakeet-encoder-projector-500f-sm8850-v81-fp16\.bin$' } elseif ([string]$cmbRecipe.SelectedItem -eq 'NVIDIA Parakeet TDT 0.6B v3') { '^parakeet-encoder-40s-mask-v2-sm8850-v81-fp16\.bin$' } else { '^whisperrecorder-htp-proof-sm8850-v81-fp16\.bin$' }
        for ($i=0; $i -lt $cmbDeployArtifact.Items.Count; $i++) {
            if ([string]$cmbDeployArtifact.Items[$i].Name -match $wantPattern) { $cmbDeployArtifact.SelectedIndex = $i; break }
        }
        $tabs.SelectedTab = $tabDeploy
        $cmbDeployArtifact.Focus()
        Update-DeployWorkflowState
        Append-Log 'Opened Deploy & Test using the matching existing compiled QNN context.'
    } else {
        [System.Windows.Forms.MessageBox]::Show('No compiled QNN context was found in the Builder workspaces. Build one first, or use Browse .bin on Deploy & Test if the context is stored elsewhere.','No reusable context found',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$btnDashWorkspace.Add_Click({ try { Open-Folder $script:CurrentWorkspace } catch { Show-Error $_ } })
$btnOpenWorkspaceFromLog.Add_Click({ try { Open-Folder $script:CurrentWorkspace } catch { Show-Error $_ } })
$btnDashLogs.Add_Click({ $tabs.SelectedTab = $tabLogs })
# Setup shortcuts for the next cloud step. These stay visible even if DPI scaling
# wraps or compresses the main tab strip.
$btnSetupOpenCloud.Add_Click({
    $tabs.SelectedTab = $tabCloud
    $btnProbeCloud.Focus()
    Append-Log 'Opened Model + AI Hub from Setup.'
})
$btnSetupProbeCloud.Add_Click({
    $tabs.SelectedTab = $tabCloud
    $btnProbeCloud.PerformClick()
})

$btnDashCloud.Add_Click({
    $tabs.SelectedTab = $tabCloud
    $btnProbeCloud.Focus()
    Append-Log 'Opened Model + AI Hub from Dashboard.'
})
$btnDashProbe.Add_Click({
    $tabs.SelectedTab = $tabCloud
    $btnProbeCloud.PerformClick()
})
$btnDashDeploy.Add_Click({
    Refresh-DeployArtifacts
    $tabs.SelectedTab = $tabDeploy
    Append-Log 'Opened Deploy & Test from Dashboard.'
})

$btnDashSettings.Add_Click({ $tabs.SelectedTab = $tabSetup })
$btnDashSupport.Add_Click({ Invoke-UiAction 'Creating support ZIP...' { Create-SupportBundle } })
$btnSetupSupport.Add_Click({ Invoke-UiAction 'Creating support ZIP...' { Create-SupportBundle } })
$btnCreateSupport.Add_Click({ Invoke-UiAction 'Creating support ZIP...' { Create-SupportBundle } })
$btnDeleteSupportZips.Add_Click({
    Invoke-UiAction 'Deleting only Builder-generated diagnostic ZIPs...' {
        $items=@(Get-ChildItem -LiteralPath $script:LogsRoot -File -Filter 'WhisperRecorder-NPU-Builder-*.zip' -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match '^WhisperRecorder-NPU-Builder-(support|error)-[0-9]{8}-[0-9]{6}(-[0-9]{3})?\.zip$'
        })
        if ($items.Count -eq 0) { Append-Log 'No Builder diagnostic ZIPs to delete.'; return }
        $choice=[System.Windows.Forms.MessageBox]::Show(('Delete ' + $items.Count + ' Builder diagnostic ZIP(s) from logs? This does NOT delete original logs, model files or workspaces.'), 'Delete diagnostic archives', [System.Windows.Forms.MessageBoxButtons]::YesNo,[System.Windows.Forms.MessageBoxIcon]::Warning)
        if ($choice -ne [System.Windows.Forms.DialogResult]::Yes) { return }
        foreach ($item in $items) { Remove-Item -LiteralPath $item.FullName -Force -ErrorAction Stop }
        Append-Log ('Deleted ' + $items.Count + ' diagnostic ZIP(s); preserved source/workspace and ordinary logs.')
    }
})
$btnOpenLogFolder.Add_Click({ try { Open-Folder $script:LogsRoot } catch { Show-Error $_ } })
$btnClearLog.Add_Click({ $txtLog.Clear() })
$btnConsoleClear.Add_Click({ $txtConsole.Clear() })
$btnConsoleCopy.Add_Click({ if ($txtConsole.TextLength -gt 0) { [System.Windows.Forms.Clipboard]::SetText($txtConsole.Text) } })
$btnConsoleLogs.Add_Click({ try { Open-Folder $script:LogsRoot } catch { Show-Error $_ } })

$form.Add_FormClosing({ [void](Populate-PackageArtifactsFromValidatedWorkspaces -Quiet); [void](Populate-HfPackageFolderFromValidatedPackage -Quiet); Save-Settings })

# -------------------- Startup --------------------
Apply-SavedSettings
$txtBridgeAudio.Text = [string]$txtPhoneAudio.Text
$script:QwenBridgeAudioSyncReady = $true
Restore-OmiStep5JSource
Update-OmiState
Prefer-ProjectLocalTools
if (-not $txtPython.Text -or -not $txtAdb.Text -or -not $txtFfmpeg.Text) { Auto-DetectTools } else { Update-ToolStatuses }
if ($stHub.Text -match 'installed|authenticated') {
    Set-Status $lblSetupHub 'Installed - verify authentication when needed' 'good'
    Set-Status $lblDashHubAuth 'Client installed. Click Configure / Verify, or paste a token on first use.' 'warn'
} else {
    Set-Status $lblSetupHub 'Not installed / not selected' 'warn'
    Set-Status $lblDashHubAuth 'AI Hub client setup required' 'warn'
}
if ($stPython.Text -match 'ready' -and $stAdb.Text -match 'ready' -and $stFfmpeg.Text -match 'ready') { Set-Status $lblSetupState 'Core tools ready' 'good' } else { Set-Status $lblSetupState 'Setup required' 'warn' }
Update-CloudWorkflowState
Refresh-DeployArtifacts
[void](Populate-PackageArtifactsFromValidatedWorkspaces -Quiet)
$startupHfPackage = Populate-HfPackageFolderFromValidatedPackage -Quiet
if (-not [string]::IsNullOrWhiteSpace($startupHfPackage)) { Append-Log ('Hugging Face source package ready: ' + $startupHfPackage) }
# Start on the actual Qwen NPU Bridge workflow, not the standalone CPU benchmark.
# Selection also invokes existing artifact auto-find once; it never stages files on the phone.
$tabs.SelectedTab = $tabQwenBridge
Update-WorkflowNavigation
Append-Log ("NPU Builder v{0} started." -f $script:AppVersion)
Append-Log ('Persistent session log: ' + $script:SessionLogPath)
Append-Log ("Workspace: " + $script:CurrentWorkspace)
Append-Log 'Live command console enabled: stdout/stderr from Builder-launched tools is mirrored below and persisted to the session log.'
Append-Log 'Persistent navigation enabled: Dashboard / Previous / Next / Go to remain visible above every workflow page.'
Append-Log 'Setup can install portable ADB/FFmpeg and an isolated qai_hub environment; the full QAIRT/QNN SDK is not required.'
Append-Log 'Target: Samsung SM-S942U / SM8850 / Hexagon V81.'
Append-Log 'Phone Benchmarks: pinned official Android llama.cpp CPU + Q8_0, no APK builds, auto ZIP and scoped cleanup.'
Append-Log 'Automatic device test cleanup is required. Omi Med NPU tab can remove Builder test files abandoned by interrupted sessions.'
Append-Log 'Hugging Face workflow enabled: canonical deployment folder + local audit + private-token upload + remote verification. HF tokens are never saved.'

# Give keyboard focus to the selected Qwen NPU Bridge tab once the form is visible.
# Keep navigation in sync without requiring the user to click through another page.
$form.Add_Shown({
    [void]$tabs.Focus()
})

[void]$form.ShowDialog()
