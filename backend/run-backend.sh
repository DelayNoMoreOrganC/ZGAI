#!/bin/bash
cd "D:\ZGAI\backend"
CLASSPATH="target/classes"
for jar in $(find ~/.m2/repository/org/springframework -name "*2.7.18*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/org/hibernate -name "*5.3*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/com/h2database -name "*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/io/jsonwebtoken -name "*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/com/fasterxml -name "*.jar" 2>/dev/null | head -20); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/org/apache -name "commons*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/org/slf4j -name "*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
for jar in $(find ~/.m2/repository/ch/qos -name "*.jar" 2>/dev/null); do
  CLASSPATH="$CLASSPATH:$jar"
done
java -cp "$CLASSPATH" com.lawfirm.LawfirmApplication --server.port=8080
