#!/bin/bash

if [ "$1" != "" ]; then

    # work folder
    cp -r ../bundles ./target
    cd ./target/bundles

    # copy latest version into work folder
    cp ../devmodeler-*.jar ./devmodeler.jar
    
    chmod 0755 ./devmodeler.jar

    # pack for MacOS
    cp ./devmodeler.jar "./Databazoo Dev Modeler.app/Contents/Java"
    zip -9 -r "../DDM.v$1.mac.zip" "./Databazoo Dev Modeler.app"

    # pack for Windows
    ./launch4j ./launch4j.xml
    zip -9 -r "../DDM.v$1.win.zip" "./Databazoo Dev Modeler.exe" README.TXT themes plugins

    # pack for Linux
    zip -9 -r "../DDM.v$1.zip" devmodeler.png devmodeler.jar devmodeler.sh README.TXT themes plugins

fi
