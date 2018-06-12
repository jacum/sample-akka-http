#!/bin/bash

echo -e "\n===== [ACCEPT] Deploy kafka scheduler to Marathon =====" &&
echo -e "\n===== [VERSION] $1 =====" &&
curl -i \
-H "Accept: application/json" \
-H "Content-Type:application/json" \
-H "Authorization: Basic YWRtaW46Y29tZW9ubGV0bWVpbg==" \
-X PUT --data '{
        "id": "/kafka-mesos-scheduler",
        "cmd": "./kafka-mesos.sh scheduler --master=zk://192.168.122.101:2181,192.168.122.102:2181,192.168.122.103:2181/mesos --zk=192.168.122.101:2181,192.168.122.102:2181,192.168.122.103:2181 --user=kafka --api=http://172.17.0.1:17000 --storage=zk:/kafka-mesos --debug=true",
        "cpus": 1.0,
        "mem": 512,
        "disk": 0,
        "instances": 1,
        "constraints": [
          [
            "hostname",
            "LIKE",
            "hz2-n01.cluster.jacum.com"
          ]
        ],
        "acceptedResourceRoles": [
          "*"
        ],
        "container": {
          "type": "DOCKER",
          "volumes": [],
          "docker": {
            "image": "jacum/mesos-kafka:1.1.0-1.1.0",
            "network": "BRIDGE",
            "portMappings": [
              {
                "containerPort": 17000,
                "hostPort": 0,
                "servicePort": 17000,
                "protocol": "tcp",
                "labels": {}
              }
            ],
            "privileged": false,
            "parameters": [],
            "forcePullImage": true
          }
        }
      }' "http://cluster.jacum.com:8080/v2/apps/kafka-mesos-scheduler"
