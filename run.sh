#!/usr/bin/env bash

JAVA_OPTS="-Xmx1G"

# remote debug
JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:8000"

# flight recorder
# JAVA_OPTS="$JAVA_OPTS -XX:+FlightRecorder -XX:StartFlightRecording=duration=200s,filename=flight.jfr"

# activate HotswapAgent when using JBR
# https://github.com/JetBrains/JetBrainsRuntime/releases
# JAVA_OPTS="$JAVA_OPTS -XX:HotswapAgent=fatjar"

# use mvn for running application without building it
exec mvn spring-boot:run -Dspring-boot.run.jvmArguments="$JAVA_OPTS"
