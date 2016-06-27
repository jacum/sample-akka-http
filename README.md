# akka-http-scala-docker-seed
Sample akka-http seed project, ready for packaging with docker.

## Operations

[![Run in Postman](https://run.pstmn.io/button.svg)](https://app.getpostman.com/run-collection/7d91311bacdf7872c884)

## Prerequisites

- Git

- Docker

  Download at [https://docs.docker.com/engine/installation/](https://docs.docker.com/engine/installation/)

- Java 8

  Upon running `java -version` you should see something like this:
 
  ```
  $ java -version
  java version "1.8.0_74"
  Java(TM) SE Runtime Environment (build 1.8.0_74-b02)
  Java HotSpot(TM) 64-Bit Server VM (build 25.74-b02, mixed mode)
  ```

- SBT (Scala Build Tool)
 
  Download at [http://www.scala-sbt.org/download.html](http://www.scala-sbt.org/download.html) and optionally add it to your `PATH` environment variable.

- AWS Configuration

  This application uses [AWS SQS](https://aws.amazon.com/sqs/) so it expects to find aws configuration and credentials in the usual locations (e.g. `~/.aws/credentials` and `~/.aws/config` on linux)

- Intellij IDEA Community Edition

  Download at [https://www.jetbrains.com/idea/](https://www.jetbrains.com/idea/)

- A REST API client

  We suggest Postman. [Chrome-plugin](https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop?hl=en)

## Installation & Running

#### Creating Intellij Project

> When opening Intellij for the first time, check the `Scala Plugin` option so it gets downloaded too

- Open Intellij and create a new project
- Under Project location inform the location where you cloned this repo
- Select `1.8` under Project SDK
- Select `2.11.8` under Scala version (if unavailable, any other 2.11 version is fine too)
- Check `"Sources"` and `"Sources for SBT and plugins`" unless your connection is poor
- ![intellij](http://i.imgur.com/mOxeZdVg.png)

#### Run configuration

Once the intellij project is created, you need to define how you will run it:

- Open `Run` -> `Edit Configurations...`
- Click the green cross on the top left corner
- Select `SBT Task` and create a `run` task, as per the following image:
- ![img](http://i.imgur.com/kOss71d.png)

#### Application configuration

You'll need to change some configuration in `src/main/resources/application.conf`, namely:

- your queue name in SQS
- the api key for using the stock quote API (use what's been given to you or get a new one at [https://www.quandl.com/users/login](https://www.quandl.com/users/login))
- the message that you want the worker to write to the aforementioned queue

### Docker packaging

This application uses [sbt-native-packager](https://github.com/sbt/sbt-native-packager) which provides a docker plugin to package sbt projects.

Just run `sbt docker:publishLocal` at your project root.

## Structure

- `src/main/resources`: configuration files 

- `src/main/scala/com/vtex/akkahttpseed/actors`: actor classes
 
- `src/main/scala/com/vtex/akkahttpseed/models`: case classes and objects
used for many reasons such as for request validation, response formats and
marshalling (converting classes and objects to/from serialized formats, such
as json).

- `src/main/scala/com/vtex/akkahttpseed/routes`: classes that define routes
(i.e. what paths and methods trigger which operations) and call whatever
resources (actor operations, web services, etc) they need to complete their
tasks.

- `src/main/scala/com/vtex/akkahttpseed/utils`: utils directory contains 
code that is generic enough so as to be used in other projects.

- `src/main/scala/com/vtex/akkahttpseed/AkkaHttpScalaDockerSeed.scala`: this
file can be thought of as a "main" method. Here the actor system is started, 
others actors are started too and all routes are merged.

- `src/test`: test classes
