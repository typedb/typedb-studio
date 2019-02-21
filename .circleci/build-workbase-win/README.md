# Workbase CI on Windows

# Implementation

`.circleci/build-workbase-win/build-workspace-win.py` is the script performing CI tasks, such as:
* provisioning Windows Server instance with a custom image
* communicating with the instance via `ssh`:
  * cloning Workbase repo
  * building `grakn-core` distribution
  * starting `grakn-core` server
  * populating `grakn-core` with data
  * building Workbase for Windows
  * running unit/integration tests
  * running end-to-end tests
  
# Custom Windows Server image

A prebuilt image (`circleci-workbase-build`) is used to execute CI steps.
This is how it can be recreated:

`template_setup.ps1`
```
New-LocalUser -Name template -Password $(ConvertTo-SecureString "template_password" -AsPlainText -Force)
Add-LocalGroupMember -Group "Administrators" -Member "template"
reg add HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\system /v LocalAccountTokenFilterPolicy /t REG_DWORD /d 1 /f
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'
Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
```

```
gcloud compute instances create circleci-workbase-template-builder \
  --image-project  windows-cloud --image-family windows-2019 \
  --machine-type n1-standard-4 --zone=europe-west1-b --project=grakn-dev \
  --metadata-from-file sysprep-specialize-script-ps1=template_setup.ps1
```

Connect to the machine via SSH:
`ssh template@<instance_ip>`

Install needed dependencies:

```
choco install vcredist2015 git bazel jdk8 visualstudio2017buildtools visualstudio2017-workload-vctools --limit-output --yes --no-progress
c:\\tools\\msys64\\usr\\bin\\pacman.exe -S --noconfirm unzip
```

Prepare for image creation:
```
GCESysprep
```

Create the template image:
`gcloud compute images create circleci-workbase-build --source-disk circleci-workbase-template-builder`


Refer to [this guide](https://cloud.google.com/compute/docs/instances/windows/creating-windows-os-image) if needed.