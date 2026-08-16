#!/bin/bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

if [ -d "app/build/kspCaches" ]; then
  ./gradlew --stop > /dev/null 2>&1
  rm -rf app/build
fi

./gradlew testDebugUnitTest "$@"
