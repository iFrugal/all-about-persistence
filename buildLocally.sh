export M2_HOME="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3"/bin

# Set JAVA_HOME to Java 17 for this script
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH


$M2_HOME/mvn versions:set -DnewVersion=$1
$M2_HOME/mvn versions:update-child-modules

$M2_HOME/mvn  clean install -B -U -T 10
