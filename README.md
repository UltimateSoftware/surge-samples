# Surge Sample Apps

This is a collection of [Surge](https://github.com/UltimateSoftware/surge) example apps to demonstrate some basic functionality using Surge for event sourcing. If you'd like to learn more about Surge including how it works under the hood, we recommend taking a look at the [Surge repository](https://github.com/UltimateSoftware/surge) or the [current docs on GitHub Pages](https://ultimatesoftware.github.io/surge/).

## Running The Examples

### Kafka Setup

The example apps depend on Kafka running. Read this [guide](https://kafka.apache.org/quickstart) to quickly get a kafka server running on your system.

Surge command applications require 2 main topics to run, a state topic for maintaining state snapshots and an events topic for downstream consumers to listen for events. The topics for our example application can be created using the following:

```
bin/kafka-topics.sh --create --topic bank-account-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic bank-account-store --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### Starting The App

Once the topics are created and Kafka is running, you can run the example apps locally using your favorite IDE or via sbt for the scala example or Maven for the java example.
For example:
```
sbt surge-scala-sample/run 
```

### Sending Requests

The application runs on port 8080 by default. Using curl you can interact with the running app through the API.

To create an account:
```
curl -X POST -H "content-type: application/json" \ 
-i http://localhost:8080/bank-accounts/create \ 
--data '{"accountNumber":"4317c6cf-438c-4f36-8391-7b6b36a0e2d9","accountOwner":"foobar","securityCode":"verysecret","initialBalance":100.0}'
```

To check the balance of an account:
```
curl -X GET http://localhost:8080/bank-accounts/4317c6cf-438c-4f36-8391-7b6b36a0e2d9
```

