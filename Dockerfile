FROM adoptopenjdk/openjdk13:alpine
COPY build/libs/k8sdemo-backend-1.0.0.jar /
RUN mkdir config
CMD ["java", "-Xmx128m", "-XX:+ExitOnOutOfMemoryError", "-jar", "/k8sdemo-backend-1.0.0.jar"]