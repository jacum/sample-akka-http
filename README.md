# akka-http-docker-seed
Sample akka-http seed project, ready for packaging with docker.

## To run this on your machine, you'll need:

- Git

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

## Creating Intellij Project

> When opening Intellij for the first time, check the `Scala Plugin` option so it gets downloaded too

- Open Intellij and create a new project
- Under Project location inform the location where you cloned this repo
- Select `1.8` under Project SDK
- Select `2.11.8` under Scala version (if unavailable, any other 2.11 version is fine too)
- Check `"Sources"` and `"Sources for SBT and plugins`" unless your connection is poor
- ![intellij](http://i.imgur.com/mOxeZdVg.png)

## Run configuration

Once the intellij project is created, you need to define how you will run it:

- Open `Run` -> `Edit Configurations...`
- Click the green cross on the top left corner
- Select `SBT Task` and create a `run` task, as per the following image:
- ![img](http://i.imgur.com/kOss71d.png)

## Application

You'll need to change some configuration in `src/main/resources/application.conf`, namely:

- your queue name i



## Docker packaging

This application uses [sbt-native-packager](https://github.com/sbt/sbt-native-packager) which provides a docker plugin to package sbt projects.

Just run 
