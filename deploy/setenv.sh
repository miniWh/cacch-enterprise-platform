#!/bin/sh
# ============================================================
# Tomcat setenv.sh：JVM 参数与外置配置
# 放置路径：/opt/cacch/tomcat/bin/setenv.sh  （需 chmod +x）
# 按 2核/3.5Gi 内存调优，勿照搬 README 的 -Xmx2g
# ============================================================

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export CATALINA_PID=/opt/cacch/tomcat/tomcat.pid

export JAVA_OPTS="$JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=/opt/cacch/config/ \
  -DLOG_HOME=/opt/cacch/logs \
  -Xms512m \
  -Xmx1024m \
  -XX:MaxMetaspaceSize=256m \
  -Xss512k \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/opt/cacch/logs/ \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai"

# 关闭 Tomcat 启动时的 jar 扫描（依赖 jar 较多，扫描会显著拖慢启动）
export CATALINA_OPTS="$CATALINA_OPTS \
  -Dtomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar \
  -Dtomcat.util.scan.StandardJarScanFilter.jarsToScan=spring-web-*.jar,cacch-*.jar"
