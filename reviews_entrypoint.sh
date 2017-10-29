#!/bin/sh

java -cp /app/reviews-service.jar test.envoy.reviews.ReviewsServiceBootstrap -port=9100 -proxyPort=10100 &
/usr/local/bin/envoy -c /etc/reviews_envoy.json --service-cluster all-services --service-node some-node