#!/bin/sh

java -cp /app/books-service.jar test.envoy.books.BooksServiceBootstrap -port=9000 -proxyPort=10000 &
/usr/local/bin/envoy -c /etc/books_envoy.json --service-cluster all-services --service-node some-node