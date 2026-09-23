CampusConnect
=============

Web application. Build with Maven.

    mvn clean install

Deploy campusconnect-web/target/campusconnect.war to Tomcat.

Database: see src/main/resources/db/schema.sql. Point the datasource at it
(jdbc.properties). Seed data is in seed.sql.

Docker (newer, not everyone uses it):

    docker-compose up

Notes
-----
- Customer is resolved per request. Set the customer code in the config before
  running locally or you will get the default.
- The nightly import expects the files in the usual place on the share.
  Ask someone on the team where that is now.
- Wiki page with the deployment runbook: http://wiki.internal/campusconnect/deploy
  (moved? ask infra)

TODO: document the appointment rules properly. -- ea

---
This repository is a synthetic codebase created as the starting point of a
software modernization exercise. It is not a real product, it has never served a
real user, and it contains no real data or credentials.
