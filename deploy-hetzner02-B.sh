#!/bin/bash
IMAGE=$1

if [ -z "$IMAGE" ]; then
    echo -e "[ERROR] Please pass a image name git as argument to deploy version"
    exit 1
fi

echo -e "\n===== [ACCEPT] Deploy example service to Marathon =====" &&
echo -e "\n===== [VERSION] $1 =====" &&
curl -i \
-H "Accept: application/json" \
-H "Content-Type:application/json" \
-H "Authorization: Basic YWRtaW46Y29tZW9ubGV0bWVpbg==" \
-X PUT --data '{
  "container": {
    "type": "DOCKER",
    "docker": {
      "forcePullImage": true,
      "image": "'"$IMAGE"'",
      "network": "BRIDGE",
      "parameters": [
        { "key": "log-driver", "value": "syslog" }
      ]
    },
    "portMappings": [
      {
        "containerPort": 9000,
        "hostPort": 0,
        "servicePort": 9000,
        "protocol": "tcp"
      },
      {
        "containerPort": 9101,
        "hostPort": 0,
        "servicePort": 9101,
        "protocol": "tcp",
        "labels" : {
          "prometheusPath" : "/metrics"
        }
      }
    ]
  },
  "id": "service-b",
  "instances": 4,
  "cpus": 0.2,
  "mem": 256,
  "env": {
    "LOG_DIR" : "/var/log"
  },
  "constraints": [["hostname", "UNIQUE"]],
  "upgradeStrategy": {
    "minimumHealthCapacity": 0.5,
    "maximumOverCapacity": 0
  },
  "healthChecks": [
    {
      "protocol": "HTTP",
      "path": "/health/check",
      "portIndex": 0,
      "gracePeriodSeconds": 30,
      "intervalSeconds": 10,
      "timeoutSeconds": 10,
      "maxConsecutiveFailures": 3
    }
  ]
}' "http://cluster.jacum.com:8080/v2/apps/service-b"

echo -e "\n===== DONE ====="
