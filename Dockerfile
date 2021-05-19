FROM adoptopenjdk/openjdk8:alpine
COPY build/libs/k8sdemo-backend-1.0.0.jar /
RUN mkdir config
CMD ["java", "-Xmx512m", "-XX:+ExitOnOutOfMemoryError", "-jar", "/k8sdemo-backend-1.0.0.jar"]