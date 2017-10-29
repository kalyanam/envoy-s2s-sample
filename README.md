# envoy-s2s-sample

This is a sample that showcases [envoy](https://www.envoyproxy.io/) setup that acts as a service proxy and interacts with a service discovery service (sds).
I'm using this to learn how envoy works.

Note: I'm still working on this project, so you will see things that are hardcoded or not finished completely.
## Getting Started

This project has three modules:

1) Books Service - a simple REST service leveraging vertx
2) Reviews Service - a simple REST service leveraging vertx
3) Service Discovery Service - a simple REST service leveraging vertx and acting as the sds provider for envoy

Requires docker for running the first two modules. The third module is run on the localhost.

To build the docker images:

```
docker build -t books-service -f Dockerfile_books .
docker build -t reviews-service -f Dockerfile_reviews .
```

To run the docker images:

```
docker run -d -p10000:10000 -p10001:100001 books-service
docker run -d -p10100:10100 -p10101:101001 books-service
```

This is the topology and flow diagram at a high level

![alt text](./s2s-diagram.png?raw=true "topology")
