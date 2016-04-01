# Jobmine Plus (Mobile App)

A JobMine online and offline viewer. This was created along side the extension.
This app is only avaliable on Android.

## Features

- Android App that is able to view your applications, job search, interviews etc
- View job search and keep track of jobs shortlisted, applied and read
- Shortlist on job search
- View individual job descriptions
- View everything you saw already offline

## Mobile (Android)

[![playstore](https://cdn1.iconfinder.com/data/icons/app-stores-2/128/Google_Play_3.png)](https://play.google.com/store/apps/details?id=com.jobmineplus.mobilepro)

[Play Store (Free with Ads)](https://play.google.com/store/apps/details?id=com.jobmineplus.mobile)

Check out the source code [here](https://github.com/matthewn4444/Jobmine-Plus)

## Building the app

Unfortunately I did not update the project to Android Studio so you can either
try to migrate it or try to get Eclipse with Android ADT and build it. I was too
lazy to remove the ad-related code from the repository but you can do it yourself.

### Removing Ads

1. Right-click the LoginActivity (of the project) and remove the **google-play-services_lib** dependency
2. Open *Description.java* file and delete all instances related to **AdView**
3. Open *SimpleActivityBase.java* file and also remove all instances related to and around **AdView & AdRequest** from lines 173-178
4. Delete the file *InterstitialAdHelper.java*
5. Open *HomeActivity.java* file and delete all instances of **InterstitialAdHelper** and the function **attachAds**
6. Open *home.xml* file and delete **AdView** xml object at the bottom of the file
7. Do the same thing as above with *tabs_ads.xml*

### Configure for non-debug build

1. Open *SimpleActivityBase.java*, at the function **isDebug**, delete contents and return false
2. Build the project

### Building with Eclipse

1. Make sure you have Android ADT installed (https://dl-ssl.google.com/android/eclipse/)
2. Import the project into Eclipse
3. Run the Android SDK and download build tools and Android sdk below Marshmallow 6.0 (do anything under that)
3. Right click all projects and select the *Project build target* to the sdk version you downloaded above
4. Clean and build so do the trick

## Other notes

Unfortunately I graduated already from Waterloo and do not want to keep maintaining
this. I know that this app has become more buggy, however since I graduated, I have
minimum access to JobMine and therefore cannot fix any changes. Please understand
that once Waterloo changes the website for good, this app will break. You can try
to compile the app and run it on your phone. Please follow the building instructions
