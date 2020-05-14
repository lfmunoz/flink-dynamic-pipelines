##  Flink Dynamic Pipeline

[![Build Status](https://travis-ci.org/lfmunoz/flink-dynamic-pipelines.svg?branch=master)](https://travis-ci.org/lfmunoz/flink-dynamic-pipelines)

This application is an attempt to create dynamic Flink pipelines.

There is a Kafka stream which has various mapper functions, these are Kotlin Scripts that
get compiled and stored inside Flink as regular Java Objects.

There is another Flink stream which is the data. It gets read and for each record the
compiled scripts are evaluated against the record. The output is written to another
Kafka stream.

It is like having a chain of map functions that are created and updated in
real time and for each event going through the data Kafka stream the map functions
are evaluated and combined to form a new output.

These map functions are effectively Java classes that are compiled
and dynamically loaded then used like a regular object. It might be slow
to compile them and load them but once created they should run as any other java object.

## Project Structure

This is a multi-project gradle build

```
.
├── build.gradle.kts
├── docs/
│   ├── notes.txt
│   └── screen-capture.png
├── flink-jobs                 Flink Job
│   ├── build.gradle.kts
│   ├── Makefile
│   ├── README.md
│   └── src
├── flink-monitor              Backend (Vertx)
│   ├── build
│   ├── build.gradle.kts
│   └── src
├── gradle
│   └── wrapper
├── gradlew
├── LICENSE
├── Makefile                   Useful commands for building and executing
├── README.md
├── settings.gradle.kts
├── version.json
└── webapp                     Frontend (Vuejs)
    ├── babel.config.js
    ├── Makefile
    ├── node_modules
    ├── package.json
    ├── package-lock.json
    ├── public
    ├── README.md
    └── src

```


## Frontend Screenshot

![screenshot](docs/screen-capture.png)


### Technologies Used

* External Services
    * Kafka
    * Flink
* Backend
    * Vertx
    * Kotlin
    * Koin
* Frontend
    * Vuejs
    * Ace Editor

