FROM amazoncorretto:17-alpine3.17
COPY ./target/scala-2.13/roof-mate.jar .
CMD ["java", "-jar", "roof-mate.jar"]