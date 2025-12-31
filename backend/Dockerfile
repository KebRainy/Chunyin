# 该镜像需要依赖的基础镜像
FROM ejiyuan/openjdk:21-jdk-alpine
VOLUME /home
# 这个地方我填写的是后端jar包名称，例如你的jar包名字叫aa.jar  这个地方就填aa.jar aa.jar(填两遍)
ADD chunyin.jar chunyin.jar
# 后端项目的端口号
EXPOSE 8081
# 前面都一样，只需要把后面的换成你的jar名称
ENTRYPOINT ["java", "-jar", "/chunyin.jar"]