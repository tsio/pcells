#!/bin/sh
OTHER_CLASSES=../../../../..
jobs=`pwd`
OUR_HOME=${jobs}/../../../../..
jarName=org.pcells.jar
#
required=${jobs}/requiredFromDmg.list
cd ${OUR_HOME}
export CLASSPATH
CLASSPATH=${OUR_HOME}
#
#   compile our stuff
#
echo "Compiling 'org'"
#
javac  `find org -name "*.java"`
[ $? -ne 0 ] && exit 4
#
echo "Creating  'org' jar"
jar cf ${jobs}/${jarName} `find org -name "*.class"`
[ $? -ne 0 ] && exit 4
echo "Basic ${jarName} created"
#
echo "Including external classes ..."
cd ${jobs}
cd ${OTHER_CLASSES}
#
while read class 
  do
     jar uf ${jobs}/${jarName} ${class}
     [ $? -ne 0 ] && exit 4
     echo "${class} added ..."
  done <${required}
#
cd ${jobs}/..
jar umf ${jobs}/manifest.cellgui ${jobs}/${jarName} images docs
exit 0
