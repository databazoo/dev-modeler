# Databazoo Dev Modeler

## About

Databazoo Dev Modeler is a database modeling and management tool. It's main
purpose is to allow access to data, provide a simple way to design and visualize
databases, and support replication process between servers.

It does not matter whether you are a database designer, analyst, programmer,
release manager or database maintenance technician - once you get in touch with
databases Databazoo Dev Modeler will support you in your activities.

## Requires

- JDK 11+
- Maven 3.5+
- Launch4J
- Internet connection (to maven repositories)

## Project setup

- Create a symlink to your ***launch4j*** binary into **./bundles/**
- Add ***db2jcc4.jar*** (available in **./bundles/**) to maven with `mvn install:install-file -Dfile=bundles/db2jcc4.jar -Dpackaging=jar -DgroupId=com.ibm.db2.jcc -DartifactId=db2jcc4 -Dversion=1.4.0`
- `mvn clean install`

## Tests

Test coverage is around 60%.

Run tests in ***devmodeler*** module with IntelliJ IDEA coverage runner.

## Licenses

- Community edition is licensed under GNU AGPL 3.0.
- Commercial edition is covered by a proprietary license agreement.
