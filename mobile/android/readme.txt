To import into Eclipse (Java)
=============================

Preinstall and Setup Eclipse
----------------------------
1. Make sure that your Eclipse has Java
2. Download the Android SDK (http://developer.android.com/sdk/index.html)
3. Install the Android plugin for Eclipse
    - Help->Install New Software...->[https://dl-ssl.google.com/android/eclipse/]
    - Install the developer tools
4. Windows->SDK Manager->[Install anything you need]
5. Once installed and restart Eclipse, make sure you choose the sdk

Importing the project
---------------------
1. Clone the repository (you probably did this)
    - git clone https://jobmineplus@bitbucket.org/jobmineplus/jobmineplus.git
2. Go to the folder where the project is
    - [.../jobmineplus/mobile/android/]
    - Rename ".project.sample" to ".project"
        - On Windows, open ".project.sample" with notepad and save as with the name ".project"
3. Edit hidden strings
    - Copy "hidden_strings.xml.sample" and paste it in [.../jobmineplus/mobile/android/res/values/]
    - Change the name to "hidden_strings.xml"
    - Open the file and change the username and password field to credentials for Jobmine
4. Open Eclipse and import the project
    - File->Import->General->Exisiting Projects into Workspace->[Next]
    - Select the jobmineplus folder
    - Make sure that the project is checked (only 1), everything else below do not check
    - Click Finish
5. Right click the project->Android tools->Fix Project Properties (any error click "ok")
6. Setup the Java Build Path
    - Right click the project->Properties->Java Build Path->Source (tab)
    - Make sure JobminePlusMobile/gen and JobminePlusMobile/src are here, if not:
        - Click "Add Folder..." click "gen" and "src" and click OK
        - Note if you don't see "gen", you need to do step 5 again
    - Exit the dialog by clicking OK
7. You can clean (Project->Clean...) or/and you can try building

Any problems email me at jobmineplus@gmail.com