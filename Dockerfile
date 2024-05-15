FROM openjdk:21
WORKDIR /opt/TruthOrDareBot/
COPY build/libs/TruthOrDareBot-all.jar TruthOrDareBot.jar
CMD ["java", "-jar", "TruthOrDareBot.jar", "-env", "/env/todbot.env"]