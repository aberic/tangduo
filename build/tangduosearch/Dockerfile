FROM docker.1panel.live/amd64/openjdk:21

# 1. 指定运行工作目录
WORKDIR /app

# 2. 暴露服务端口（与application.yml一致）
EXPOSE 19219
EXPOSE 19220

# 3. 从构建阶段复制search的JAR包到运行目录
# 注：路径对应克隆后项目的search模块target目录
COPY search.jar search.jar

# 4. 环境变量（可通过docker run动态覆盖）
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    TIME_ZONE=Asia/Shanghai \
    LOG_PATH=/var/service/log \
    DB_ROOT_PATH=/data

# 5. 健康检查（需search模块引入spring-boot-starter-actuator）
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:19220/actuator/health || exit 1

# 6. 启动命令
CMD java -jar search.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}

# docker build -t tangduosearch:"$1" .