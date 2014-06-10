# What is this?
JobMine has an issue where their server did not configure their SSL certificates
correctly. Therefore login broke spring 2014. The solution is to use a custom
certicate manager; this is a compromise and really is not ideal. You must build
your own certificate. 

# Steps
1. In your browser, export the certificate (after visiting JobMine's website). Then
save the file here (jobmine.ccol.uwaterloo.ca.crt). 

2. Run build.sh (which will put the certificate into /res/raw)

3. Then you can build the apk.