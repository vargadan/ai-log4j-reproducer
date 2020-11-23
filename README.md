This is reproducer for an issue with the applicationinsights-agent-3.0.0 codeless agent and Log4J.
Namely, trace (log) events are not intercepted and sent to AI when the
`-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector`
system property is set.

To reproduce the issue:
- build with maven "mvn install"
To start the application with AI events sent:
- edit the APPINSIGHTS_INSTRUMENTATIONKEY env var in the infra/docker-compose.yaml file
- build and run with docker compose:
  - cd ./infra
  - docker-compose up --build
- call http://localhost:8080/hello
- in a couple of minutes search for traces with /hello in AI/Transaction Search of the corresponding AI project and find the corresponding traces of "called : /hello" with severity level information

Then you may set the `-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector` property in JAVA_OPTS env var in docker-compose.yaml and repeat the above steps.
However, you will not find the corresponding traces in AI/Transaction Search.
