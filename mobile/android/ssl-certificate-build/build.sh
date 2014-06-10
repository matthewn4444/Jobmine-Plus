export CLASSPATH=bcprov-jdk16-145.jar
CERTSTORE=../res/raw/jobmine_certificate.bks
if [ -a $CERTSTORE ]; then
    rm $CERTSTORE || exit 1
fi
keytool \
      -import \
      -v \
      -noprompt \
      -trustcacerts \
      -alias 0 \
      -file jobmine.ccol.uwaterloo.ca.crt \
      -keystore $CERTSTORE \
      -storetype BKS \
      -provider org.bouncycastle.jce.provider.BouncyCastleProvider \
      -providerpath /usr/share/java/bcprov.jar \
      -storepass jobmineplus