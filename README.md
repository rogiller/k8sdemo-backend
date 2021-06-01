## Introduction
This repo is the source for a Kubernetes (k8s for now on) demo I did at work.  This README contains the full content of that demo.

The demo is a single k8s node running a main backend deployment and an integration pod.  The backend app hosts the Rest API that
queries a MySQL database running on the host VirtualBox for a customer record by ID.  The customer's address information
is then sent to the integration app.  The integration app hosts a Rest API that will fetch weather information from
https://openweathermap.org/ and return a subset of the weather data.

These projects can be checked out locally by calling:

git clone https://github.com/mattrr78/k8sdemo-backend

git clone https://github.com/mattrr78/k8sdemo-integration

The projects were then built locally with Gradle.  The Dockerfile, k8s YAML files, and jar file were then copied to 
VirtualBox using a shared folder.  We used VirtualBox (with Vagrant) to use the closest setup to a production environment.
MySQL runs on VirtualBox, so references to the host machine are really referencing the running VirtualBox instance.

## Setting up VirtualBox

Everything will be done as root in VirtualBox.  All work will be done inside the VirtualBox shell.

Install Docker:  https://docs.docker.com/engine/install/ubuntu/

Then run the following to install microk8s:  `snap install microk8s --classic`

With MySQL running, refer to `k8sdemo-backend/mysql/README.txt` to setup k8sdemo account, database, and customer table.

Create a shortcut to kubectl:

`echo "alias kubectl='microk8s.kubectl'" > ~/.bash_aliases`
`. ~/.bash_aliases`

Run the following to view enabled/disabled addons: `microk8s.status`

Enable the following microk8s addons

`microk8s.enable dns` To enable communication between pods and resolve internet DNS names.

`microk8s.enable host-access` To connect to the host MySQL database by using host's static IP address of 10.0.1.1.

`microk8s.enable metrics-server` To gather pod metrics that will be used by `kubectl top pod` and HorizontalPodAutoScaler.

Create the following directory structure and copy artifacts to your VirtualBox's shared folder and then copy from shared
folder to the below directory structure (do not make /k8sdemo the shared folder):

```
/
    k8sdemo/
        k8sdemo-backend/
            Dockerfile
            build/
                libs/
                    k8sdemo-backend-1.0.0.jar
            config/
            k8s/
    
        k8sdemo-integration/
            Dockerfile
            build/
                libs/
                    k8sdemo-integration-1.0.0.jar
            config/
            k8s/
```

## Docker
Change into backend directory and run:

`docker build . -t k8sdemo-backend:v1.0.0` To build the Docker image.

`docker save k8sdemo-backend:v1.0.0 > k8sdemo-backend_v1.0.0.tar`  To write out the image to a tar file.

`microk8s.ctr image import k8sdemo-backend_v1.0.0.tar` To import Docker image tar file into microk8s.

Then change into integration directory and run:

`docker build . -t k8sdemo-integration:v1.0.0`

`docker save k8sdemo-integration:v1.0.0 > k8sdemo-integration_v1.0.0.tar`

`microk8s.ctr image import k8sdemo-integration_v1.0.0.tar`

Then run:

`microk8s.ctr images ls | grep k8sdemo` To confirm that the Docker images have been loaded into microk8s 
(microk8s prepends `docker.io/library/` when importing Docker images locally).  If you make a mistake, run
`microk8s.ctr images rm docker.io/library/<IMAGE-NAME>:<IMAGE-VERSION>` and try again.

## application.properties setup

application.properties will exist outside k8s.  We will create a PersistentVolume and PersistentVolumeClaim so pods
can access the host's `config/` directory.

In `/k8sdemo/k8sdemo-backend/config` directory, create application.properties file and add:
```
spring.datasource.url=jdbc:mysql://10.0.1.1:3306/k8sdemo
k8sdemo.integration.base-url=http://k8sdemo-integration:9120/k8sdemo/integration
```

In `/k8sdemo/k8sdemo-integration/config directory`, create application.properties file and add:
```
k8sdemo.weather.api-key=<YOUR OPEN WEATHER API KEY>
```

## Start

#### Integration
To run k8sdemo-integration, run:

```
kubectl apply -f /k8sdemo/k8sdemo-integration/k8s
```

We started a pod vs. starting a deployment because this pod will not be scaled.
After a few seconds, the k8s-integration app should be running inside k8s.  Run `kubectl logs k8sdemo-integration`
to verify.

Now run the following to shell into the pod:  `kubectl exec k8sdemo-integration -it -- /bin/sh`

While in the shell, try to run the following (ctrl+c to stop pinging servers):
```
ping 8.8.8.8
ping www.google.com
```

If there was a problem with pinging, exit the shell (ctrl+d), run `iptables -P FORWARD ACCEPT`, shell in again and ping.

If we were able to ping both 8.8.8.8 (Google's primary DNS server) and www.google.com, then we can proceed.  
Exit out of the shell (ctrl+d) and run:
`curl "http://localhost:31120/k8sdemo/integration/weather?city=Chicago&state=Illinois&country=US"`  If you get back
weather information, then k8sdemo-integration was a success!  31120 is the node port we're exposing from k8s to the 
host defined in the service YAML file (Port range must be 30000-32767).  

#### Backend
Now run 
```
kubectl apply -f /k8sdemo/k8sdemo-backend/k8s
```

This time we are doing a deployment of the backend because a deployment can manage pod replicas, and we want k8s to
spin up replicas of the backend.

Run `curl "http://localhost:31110/k8sdemo/backend/customer/weather/1"` and hopefully it works!

## Crash the Backend!

First, run `kubectl delete -f /k8sdemo/k8sdemo-backend/k8s/k8sdemo-backend-hpa.yml` to remove HorizontalPodAutoScaler.

`http://localhost:31110/k8sdemo/backend/customer/junk/X` is a Rest API that will create a 100 byte object in memory X
times.

Run `curl -X POST "http://localhost:31110/k8sdemo/backend/customer/junk/100000"` multiple times to crash the backend.
The pod will run out of memory and shutdown, but then k8s will restart it!

## Autoscale the Backend

Run `kubectl apply -f /k8sdemo/k8sdemo-backend/k8s/k8sdemo-backend-hpa.yml` to re-enable HorizontalPodAutoScaler.

Run `curl -X POST "http://localhost:31110/k8sdemo/backend/customer/junk/100000"` a few times, changing the last number 
so you get close to the edge of crashing the backend pod.  You may have to play around with this a few times to get it
right.  Use `kubectl top pod` to monitor pod memory usage.  Wait a minute between each time you run the curl command
because the autoscaler does not act on burst metrics and needs some time to pass before it starts a replica pod.  Also,
call `kubectl get hpa` to view current memory usage and target memory usage.  If current memory usage exceeds target
memory usage, do not run the curl command anymore and wait a couple of minutes.  Then run `kubectl get pods` to see a
2nd backend pod started up by the HorizontalPodAutoScaler!

If you want to get a 3rd pod started, shell into the pod name with less memory usage and run the following a few times:
`wget --post-data '' http://localhost:9110/k8sdemo/backend/customer/junk/1000000`

Wait a couple of minutes, and a 3rd pod should be invoked!

## Redis

In the `v1.1.0` branch of this repo, I have a demo that uses Redis.  To use this Redis version:

* Check out that branch out, build the jar, and copy the jar
* Docker-ize the new 1.1.0 jar using version 1.1.0 
* save out the Docker image
* import the Docker image tar file into microk8s.
* Add the following to backend's application.properties file:
  `k8sdemo.redis.url=redis://redis:6379`
* Change version in `/k8sdemo/k8sdemo-backend/k8s/k8sdemo-backend-deployment.yml` to 1.1.0
* Before applying the change, run `kubectl diff -f /k8sdemo/k8sdemo-backend/k8s/k8sdemo-backend-deployment.yml` to
  observe differences between deployed version and our changed version of the YAML file.
* Run `kubectl apply -f /k8sdemo/k8sdemo-backend/k8s/k8sdemo-backend-deployment.yml`

API calls after the first one will now run faster because weather and address info will be cached in Redis.