#!/bin/sh
OTHER_CLASSES=..
jobs=`pwd`
OUR_HOME=${jobs}/..
jarName=org.pcells.jar
TARGET="-target 1.5 -source 1.5"
#
usePsu=true
required=${jobs}/requiredFromDmg.list
cd ${OUR_HOME}
export CLASSPATH
CLASSES=${OUR_HOME}/classes
EXTRAS=${OUR_HOME}/extras
CELLS=${OUR_HOME}/classes/cells190.jar
DCACHE=${OUR_HOME}/classes/dcache.jar
SSHD=${OUR_HOME}/classes/sshd-core-0.8.0.jar
MINACORE=${OUR_HOME}/classes/mina-core-2.0.4.jar
SLF4J=${OUR_HOME}/classes/slf4j-api-1.6.4.jar
BCPROV=${OUR_HOME}/classes/bcprov-jdk16-1.46_3.jar
TOMCAT=${OUR_HOME}/classes/tomcat-embed-core-7.0.26.jar
A_COMMONS=${OUR_HOME}/classes/commons-collections-3.2.1-1.0.0.jar
jzlib=${OUR_HOME}/classes/jzlib-1.1.1.jar
CLASSPATH=${CELLS}:${DCACHE}:${SSHD}:${MINACORE}:${SLF4J}:${BCPROV}::${TOMCAT}::${jzlib}:${A_COMMONS}:${OUR_HOME}
DIST=${OUR_HOME}/dist/pcells
VERSIONFILE=${OUR_HOME}/docs/help/version
#
problem (){
   echo "$2"
   exit $1
}
#   compile our stuff
#
#
THISVERSION=2.0
if [ -f $VERSIONFILE ] ; then
  THISVERSION=`cat ${VERSIONFILE}`
fi
#
echo "Setting version to $THISVERSION"
#
#cd ${OUR_HOME}/org/pcells/services/gui
#cp JMultiLogin.java JMultiLogin.java.save
#sed 's/OURGUIVERSION/$THISVERSION/' <JMultiLogin.java >JMultiLogin.java.copy
#cp JMultiLogin.java
#
printf "Deleting all *.class files under  org/... "
find org -name "*.class" -exec rm {} \;
#
echo "Compiling 'org'"
javac -version
#
# DETAILS=-Xlint:deprecation
#
javac -g:source,lines,vars $TARGET $DETAILS  `find org -name "*.java"`
[ $? -ne 0 ] && exit 4
#
jarName=cells190
printf "Recreating ${jarName}.jar ... "
#
echo ""
printf "Unpacking cells.jar ... "
###############################################
# Changing Directory here ....   ##############
###############################################
cd ${CLASSES}
jar xf ${jarName}.jar
[ $? -ne 0 ] && problem 3 "Failed"
echo " Done"
#
printf "Packing cells.jar ... "
cp ../org/pcells/services/connection/*.class org/pcells/services/connection/ 
jar cf ${jarName}.jar org/pcells/services/connection/*.class `find dmg/ -name *.class`
[ $? -ne 0 ] && problem 3 "Failed"
echo " Done"
rm -rf dmg org META-INF
###############################################
# Comming back ....   ##############
###############################################
cd -
#
#
echo "Preparing Distribution"
rm -rf ${DIST}
mkdir ${DIST}
cp ${CLASSES}/*.jar ${DIST}
cp ${jobs}/YCommander.plugins ${DIST}
cp ${jobs}/MonitoringPanel.plugins ${DIST}
cp ${jobs}/to-kde3-desktop.sh ${DIST}
cp ${jobs}/../images/eagle_icon.* ${DIST}
cp ${jobs}/../images/Storyboard-5.png ${DIST}
if [ -d "${EXTRAS}" ] ; then cp ${EXTRAS}/* ${DIST} ; fi
#
#
jarName=org.pcells
#
printf "reating JAR ${jarName} ... "
jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar  org/pcells/util/*.class \
          images/sheep*.png \
          images/cells-logo.jpg \
          docs 
[ $? -ne 0 ] && problem 3 "Failed" 
echo " Done"
#
jarName=pcells-gui-core
#
printf "Creating JAR ${jarName} ... "
jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar `find org/pcells/services -name "*.class"` docs/help
[ $? -ne 0 ] && problem 3 "Failed" 
echo " Done"
#
jarName=pcells-gui-dcache
#
printf "Creating JAR ${jarName} ... "
jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar org/dcache/gui/pluggins/*.class org/dcache/gui/pluggins/pools/*.class \
                                   org/dcache/gui/pluggins/costs/*.class org/dcache/gui/pluggins/monitoring/*.class
[ $? -ne 0 ] && problem 3 "Failed" 
echo " Done"
#
#
jarName=pcells-gui-flush
#
printf "Creating JAR ${jarName} ... "
jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar `find org/dcache/gui/pluggins/flush -name "*.class"` \
                                    org/dcache/gui/pluggins/pnfs/*.class docs/help
[ $? -ne 0 ] && problem 3 "Failed" 
echo " Done"
#
jarName=pcells-gui-drives
#
printf "Creating JAR ${jarName} ... "
jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar `find org/dcache/gui/pluggins/drives/ -name "*.class"` docs/help
[ $? -ne 0 ] && problem 3 "Failed" 
echo " Done"
#
jarName=pcells-gui-psu
#
if [ ! -z "$usePsu"  ] ; then
printf "Creating JAR ${jarName} ... "
jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar `find org/dcache/gui/pluggins/poolManager/ -name "*.class"`
[ $? -ne 0 ] && problem 3 "Failed" 
echo " Done"
#
else
#
echo ""
echo " Warning : ${jarName} has been deactivated : ${jarName}.DONTUSE"
echo ""
##
fi
#
#jarName=pcells-gui-x
#
#printf "Creating JAR ${jarName} ... "
#jar cmf ${jobs}/manifest.${jarName} ${DIST}/${jarName}.jar docs/help
#[ $? -ne 0 ] && problem 3 "Failed" 
#echo " Done"
#
#
#  prepare generic jar
#
cd ${DIST}/..
rm -rf *.app
rm -rf pcells.zip

zip -r pcells pcells
#
# prepare Darwin .app.
#
echo "Creating Darwin .... "
#
DARWINNAME=pcells-${THISVERSION}.app
#
rm -rf ${DARWINNAME}
cp -R ../org/pcells/app/pcells.app pcells.app
cp -R ../org/pcells/app/pcells.app ${DARWINNAME}
sed "s/THISVERSION/$THISVERSION/" <pcells.app/Contents/Info.plist >${DARWINNAME}/Contents/Info.plist
cp pcells/* ${DARWINNAME}/Contents/Resources/Java/

exit 0
