FROM amazoncorretto:17-alpine3.17

COPY backend/target/scala-2.13/roof-mate.jar /var/www/roof-mate.jar
COPY frontend/target/scala-2.13/roof-mate-frontend-opt /var/www/public
COPY frontend/src/main/resources/index.html /var/www/public/index.html

CMD ["java", "-jar", "/var/www/roof-mate.jar"]