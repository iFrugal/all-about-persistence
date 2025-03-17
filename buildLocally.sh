
# Set JAVA_HOME to Java 17 for this script
#export JAVA_HOME=$(/usr/libexec/java_home -v 17)
#export PATH=$JAVA_HOME/bin:$PATH


#mvn versions:set -DnewVersion=$1
#mvn versions:update-child-modules

#mvn clean deploy -B -U -T 10

mvn clean
mvn release:prepare #-Darguments="-Dmaven.javadoc.skip=true"
mvn release:perform -Darguments="-DnexusStagingDeployTimeoutMinutes=20"

git push --tags
git push origin master