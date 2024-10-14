set -e
echo Making jar
./gradlew clean jar
echo Clearing mods folder
rm ../mserv/config/mods/* || echo "Mods folder already empty"
echo Copying jar
cp ./build/libs/* ../mserv/config/mods/

