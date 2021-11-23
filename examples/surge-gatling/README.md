Surge Multilanguage Sample Application
======================================

How to Run
-----------

1. Install and start minikube. Note: you'll need Docker too.
    ```
    minikube start --driver=virtualbox --memory=8192 # this can also work without virtualbox
    ```
2. Install Strimzi. Strimzi is just one of the multiple ways of running a Kafka cluster on K8s.
   ```
   open https://strimzi.io/quickstarts/
   ```

3. Publish docker images.
   ```
   $ eval $(minikube docker-env)
   $ sbt
   sbt:surge> project surge-scala-sample
   sbt:surge-scala-sample> docker:publishLocal
   ``` 

4. Apply yaml files in the following order from k8s directory:
   ```
   $ kubectl create -f bank-account-events-topic.yaml -n kafka
   $ kubectl create -f bank-account-state-topic.yaml -n kafka
   $ kubectl create -f scala-sample.yaml -n kafka 
   ```

5. Call the service:
   ```
   $ minikube service --url scala-app -n kafka
   $ curl --location --request POST 'http://192.168.99.131:31036/bank-accounts/create' \
      --header 'Content-Type: application/json' \
      --data-raw '{
        "accountOwner": "Jane Doe",
        "securityCode": "12345678910",
        "initialBalance": 1000
   }'
   $ curl --location --request GET 'http://192.168.99.131:31036/bank-accounts/57bdf98a-08b3-4945-b1e7-b298192777e6'
   
   # replace 192.168.99.131:31036 with the IP you got from the minikube service --url command 
   ```
   
6. Run Gatling simulation
   ```
   first export the url you got from minikube service --url command to the following variable in terminal tab
    $ export SCALA_APP_URL="http://192.168.99.131:31036" # Keep the variable name as it is.
   In the same terminal tab 
   $ sbt
   $ sbt:surge-samples> project surge-gatling-sample
   $ sbt:surge-gatling-sample> Gatling / test 
   or you can run one simulation at a time from the following command
   $ sbt:surge-gatling-sample> Gatling / testOnly simulation.creditAccountSimulation
   $ sbt:surge-gatling-sample> Gatling / testOnly simulation.debitAccountSimulation 
   $ sbt:surge-gatling-sample> Gatling / testOnly simulation.createAccountSimulation  
   
   ```
  

Does it work ?
--------------

Make several calls to the service (i.e. create multiple bank accounts).

Watch the logs using:
```
kubectl get pods -n kafka
kubectl logs -f POD_ID -c scala-app -n kafka # open several terminals - one for each pod 
```

Look at the logs coming from the logger "com.ukg.surge.multilanguage.GenericAsyncAggregateCommandModel". You should see bank account entities evenly distributed across containers (i.e. each container owns a bunch of accounts) because of cluster sharding.
