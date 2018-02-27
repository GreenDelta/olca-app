
Name openLCA

# General Symbol Definitions
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION {version}
!define COMPANY "GreenDelta GmbH"
!define URL http://www.greendelta.com

# MultiUser Symbol Definitions
!define MULTIUSER_EXECUTIONLEVEL Admin
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_KEY "${{REGKEY}}"
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_VALUENAME MultiUserInstallMode
!define MULTIUSER_INSTALLMODE_DEFAULT_CURRENTUSER
!define MULTIUSER_MUI
!define MULTIUSER_INSTALLMODE_COMMANDLINE
!define MULTIUSER_INSTALLMODE_INSTDIR openLCA
!define MULTIUSER_INSTALLMODE_INSTDIR_REGISTRY_KEY "${{REGKEY}}"
!define MULTIUSER_INSTALLMODE_INSTDIR_REGISTRY_VALUE "Path"
!define MUI_WELCOMEFINISHPAGE_BITMAP "welcome.bmp"
!define MULTIUSER_INSTALLMODEPAGE_TEXT_CURRENTUSER "$(CurrentUserButtonLabel)"


# MUI Symbol Definitions
!define MUI_ICON "icon.ico"
!define MUI_LICENSEPAGE_CHECKBOX
!define MUI_STARTMENUPAGE_REGISTRY_ROOT SHCTX
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${{REGKEY}}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULTFOLDER openLCA
!define MUI_FINISHPAGE_RUN $INSTDIR\openLCA.exe
!define MUI_UNICON "orange-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_LANGDLL_REGISTRY_ROOT SHCTX
!define MUI_LANGDLL_REGISTRY_KEY ${{REGKEY}}
!define MUI_LANGDLL_REGISTRY_VALUENAME InstallerLanguage

# Included files
!include MultiUser.nsh
!include Sections.nsh
!include MUI2.nsh
!include LogicLib.nsh

# Reserved Files
!insertmacro MUI_RESERVEFILE_LANGDLL

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE openlca\OPENLCA_README.txt
!insertmacro MULTIUSER_PAGE_INSTALLMODE
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE German

# a macro for using the WriteToFile function (see below)
# NewLine indicates if a carriage return should be added at the end of the text
!macro WriteToFile NewLine File Text
  
  # push the text on the stack
  !if `${{NewLine}}` == true
    Push `${{Text}}$\r$\n`
  !else
    Push `${{Text}}`
  !endif
  
  # push the file on the stack
  Push `${{File}}`
  
  # call the function
  Call WriteToFile
!macroend
!define WriteToFile `!insertmacro WriteToFile false`
!define WriteLineToFile `!insertmacro WriteToFile true`

# function for writing text to a file
# the stack should look like [file, text] when calling this function
Function WriteToFile
  Exch $0 # put the file to register $0
  Exch	# change the top two stack values
  Exch $1 # put the text to register $1
 
  FileOpen $0 $0 a 	# open the file
  FileSeek $0 0 END # go to end
  FileWrite $0 $1 	# write to file
  FileClose $0		# close the file
 
Pop $1	# remove the text and the file from the stack
Pop $0
FunctionEnd

# Installer attributes
OutFile setup.exe
# ALL USERS: InstallDir "$PROGRAMFILES64\openLCA"
InstallDir "$LOCALAPPDATA\openLCA"
CRCCheck on
XPStyle on
ShowInstDetails hide
VIProductVersion {version}.0
VIAddVersionKey /LANG=${{LANG_ENGLISH}} ProductName openLCA
VIAddVersionKey /LANG=${{LANG_ENGLISH}} ProductVersion "${version}"
VIAddVersionKey /LANG=${{LANG_ENGLISH}} CompanyName "${{COMPANY}}"
VIAddVersionKey /LANG=${{LANG_ENGLISH}} CompanyWebsite "${{URL}}"
VIAddVersionKey /LANG=${{LANG_ENGLISH}} FileVersion "${version}"
VIAddVersionKey /LANG=${{LANG_ENGLISH}} FileDescription ""
VIAddVersionKey /LANG=${{LANG_ENGLISH}} LegalCopyright ""

ShowUninstDetails hide

# Installer sections
Section "-openLCA {version}" SEC0000
    SetOutPath $INSTDIR
    SetOverwrite on
    File /r openlca\*
    ${{If}} $LANGUAGE = 1031
        File /r german\openLCA.ini
    ${{Else}}
        File /r english\openLCA.ini
    ${{EndIf}}
    WriteRegStr SHCTX "${{REGKEY}}\Components" "openLCA {version}" 1

    ${{If}} $MultiUser.InstallMode == "CurrentUser"
        ${{WriteToFile}} `$INSTDIR\singleuserinstall.mrk` `marker file`
    ${{EndIf}}

SectionEnd

Section -post SEC0001
    WriteRegStr SHCTX "${{REGKEY}}" Path $INSTDIR
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^Name).lnk" $INSTDIR\openLCA.exe
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\Uninstall $(^Name).lnk" $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${version}"
    WriteRegStr SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${{COMPANY}}"
    WriteRegStr SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${{URL}}"
    WriteRegStr SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 SHCTX "${{REGKEY}}\Components" "${{SECTION_NAME}}"
    StrCmp $R0 1 0 next${{UNSECTION_ID}}
    !insertmacro SelectSection "${{UNSECTION_ID}}"
    GoTo done${{UNSECTION_ID}}
next${{UNSECTION_ID}}:
    !insertmacro UnselectSection "${{UNSECTION_ID}}"
done${{UNSECTION_ID}}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o "-un.openLCA {version}" UNSEC0000
    Delete /REBOOTOK "$INSTDIR\openLCA.exe"
    Delete /REBOOTOK "$INSTDIR\.eclipseproduct"
    RmDir /r /REBOOTOK $INSTDIR
    DeleteRegValue SHCTX "${{REGKEY}}\Components" "openLCA {version}"
SectionEnd

Section -un.post UNSEC0001
    DeleteRegKey SHCTX "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\openLCA.lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue SHCTX "${{REGKEY}}" StartMenuGroup
    DeleteRegValue SHCTX "${{REGKEY}}" Path
    DeleteRegKey /IfEmpty SHCTX "${{REGKEY}}\Components"
    DeleteRegKey /IfEmpty SHCTX "${{REGKEY}}"
    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RmDir /REBOOTOK $INSTDIR
    Push $R0
    StrCpy $R0 $StartMenuGroup 1
    StrCmp $R0 ">" no_smgroup
no_smgroup:
    Pop $R0
SectionEnd

Function setInstallDir
	Push $0
	# replace InstallDirRegKey call with SHCTX-compatible code
	
	ReadRegStr $0 SHCTX "${{REGKEY}}\Path" ""
	${{If}} $0 != ""
		StrCpy $INSTDIR $0
	${{EndIf}}
	Pop $0
FunctionEnd

Function un.setInstallDir
	Push $0
	# replace InstallDirRegKey call with SHCTX-compatible code
	
	ReadRegStr $0 SHCTX "${{REGKEY}}\Path" ""
	${{If}} $0 != ""
		StrCpy $INSTDIR $0
	${{EndIf}}
	Pop $0
FunctionEnd

# Installer functions
Function .onInit
	Call setInstallDir
	
    InitPluginsDir
    !insertmacro MUI_LANGDLL_DISPLAY
    !insertmacro MULTIUSER_INIT
FunctionEnd

# Uninstaller functions
Function un.onInit
	Call un.setInstallDir
	
    SetAutoClose true
    !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuGroup
    !insertmacro MUI_UNGETLANGUAGE
    !insertmacro MULTIUSER_UNINIT
    !insertmacro SELECT_UNSECTION "openLCA {version}" ${{UNSEC0000}}
FunctionEnd

# Installer Language Strings
# TODO Update the Language Strings with the appropriate translations.

LangString ^UninstallLink ${{LANG_ENGLISH}} "Uninstall $(^Name)"
LangString ^UninstallLink ${{LANG_GERMAN}} "Uninstall $(^Name)"
LangString CurrentUserButtonLabel ${{LANG_ENGLISH}} "Current user (recommended)"
LangString CurrentUserButtonLabel ${{LANG_GERMAN}} "Aktueller Benutzer (empfohlen)"
