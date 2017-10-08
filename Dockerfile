FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/ads-txt.jar /ads-txt/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/ads-txt/app.jar"]
