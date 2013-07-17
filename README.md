pcells
======

This is the dCache administration GUI pcells.

Building
-------------------------

Building it can be done by going to the jobs directory and execute:

    ./makeModuleCellGui.sh

This will create a MacOS app in directory dist that can either be started by:

Starting
------------------------------------
The following commands are relative to the clone root directory.


    open -a dist/pcells-1.7.0.app

or, in case you want to debug things, you can run:

    java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -jar \
    dist/pcells-1.7.0.app/Contents/Resources/Java/org.pcells.jar

Of course the app version needs to be adapted accordingly in case you are starting
another version. Then use your IDE to remote debug on port 8000.
