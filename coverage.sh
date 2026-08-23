#!/bin/bash
if [ -z "$JAVA_HOME" ] && [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "$ANDROID_HOME" ] && [ -d "/opt/homebrew/share/android-commandlinetools" ]; then
  export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
fi
if [ -n "$JAVA_HOME" ]; then
  export PATH=$JAVA_HOME/bin:$PATH
fi
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
  export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
fi

./gradlew testDebugUnitTest koverXmlReportDebug koverVerifyDebug "$@"
