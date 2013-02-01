=======
PURPOSE
=======
This tests locally for the applications, shortlist and interviews page.

==========
HOW TO RUN
==========
1. Make sure you install Node.js (google it)
2. Make sure you ran (use git-bash, terminal etc)
    > npm install supervisor -g
3. Double click the "run-supervisor.bat" on Windows 
   or on Mac/Linux use the following command in Terminal
   > supervisor app.js
3a. You can run "run.bat" or 
   > node app.js
   But if it crashes or change something then you have to close the 
   terminal/command prompt and run step 3 again
4. Go to your browser and go to "localhost:1111"
5. You can play around with the settings of what pages you would like to 
   display. Also make sure that the debug links in the Android application
   points to: 
   Applications:    "http://10.0.2.2:1111/applications/"
   Interviews:      "http://10.0.2.2:1111/interviews/"
   Short List:      "http://10.0.2.2:1111/shortlist/"
6. Make sure you compile the Android app in "debug mode" (check the manifest file)
7. Run the app in the emulator and you can control the pages from debug mode
   
   
