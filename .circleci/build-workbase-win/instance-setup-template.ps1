# Enabling OpenSSH
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
Start-Service sshd
Set-Service -Name sshd -StartupType 'Automatic'
# Create a new admin user and allow him to use elevated priveleges
New-LocalUser -Name circleci -Password $(ConvertTo-SecureString "INSTANCE_PASSWORD" -AsPlainText -Force)
Add-LocalGroupMember -Group "Administrators" -Member "circleci"
# https://github.com/PowerShell/Win32-OpenSSH/issues/962
reg add HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\system /v LocalAccountTokenFilterPolicy /t REG_DWORD /d 1 /f

# Download and install Chocolatey, which is a package manager for Windows
Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
