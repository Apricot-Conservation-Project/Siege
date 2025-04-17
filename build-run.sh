cd ~/Git/Siege-Environment/Siege
bash make_mod.sh
echo ""
cd ../mserv

JAVA_HOME=$(find /usr/lib/jvm -maxdepth 1 -type d -printf "%f\n" | grep 17)

if [ -n "$JAVA_HOME" ]; then
    JAVA="/usr/lib/jvm/$JAVA_HOME/bin/java"
    $JAVA -jar server-release.jar
else
    echo "Java 17 not set up. Please install jdk17-openjdk."
    exit 1
fi
