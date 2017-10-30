package test.envoy.reviews;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by mkalyan on 10/26/17.
 */
public class ReviewsServiceBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(ReviewsServiceBootstrap.class);

    @Option(name = "-port")
    private Integer port;

    @Option(name = "-proxyPort")
    private Integer proxyPort;

    private Vertx vertx;
    private HttpClient sdsClient;

    private void start() {
        vertx = Vertx.vertx();

        //Client to call out to sds service at port 9999
        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost("0.0.0.0")
                .setDefaultPort(9999);
        this.sdsClient = this.vertx.createHttpClient(options);

        this.startApiServer()
                .compose(v -> this.registerInSDS())
                .setHandler(res -> {
                    if(res.succeeded()) {
                        try {
                            logger.info("Started reviews service on host:{}, port: {}", InetAddress.getLocalHost().getHostAddress(), this.port);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    } else {
                        logger.error("Unable to start reviews-service. Shutting down: {}", res.cause());
                        System.exit(-1);
                    }
                });

        try {
            logger.info("Started reviews service on host:{}, port: {}", InetAddress.getLocalHost().getHostAddress(), this.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private Future<Void> startApiServer() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*"));

        router.get("/ping").handler(this::handlePing);
        router.get("/v1/reviews").handler(this::findAllReviews);

        Future<Void> future = Future.future();
        try {
            vertx.createHttpServer().requestHandler(router::accept).listen(this.port);
            future.complete();
        } catch (Exception ex) {
            logger.error("Unable to start the api server: {}", ex);
            future.fail(ex);
        }

        return future;
    }

    private Future<Void>  registerInSDS() {
        Future<Void> future = Future.future();
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            HttpClientRequest request = this.sdsClient.post("/v1/registration/reviews-service", rh -> {
                if(rh.statusCode() != 201) {
                    logger.info("Registration failed: {}", rh.statusCode());
                    future.fail("Unable to register in SDS");
                }
                rh.bodyHandler(buffer -> {
                    logger.info("Registration succeeded: {}, {}", rh.statusCode(), buffer);
                    future.complete();
                });
            });
            JsonObject payload = new JsonObject();
            payload.put("service_name", "reviews-service");
            payload.put("ip_address", ipAddress);
            payload.put("port", proxyPort);

            request.putHeader("content-length", payload.encode().length()+"");
            request.putHeader("content-type", "application/json");
            request.end(payload.encode());
            logger.info("Registration sent to SDS");
        } catch (Exception ex) {
            logger.info("Unable to register in SDS: {}", ex);
            future.fail(ex);
        }
        return future;
    }

    private void handlePing(RoutingContext routingContext) {
        logger.info("Successful ping...");
        routingContext.response().end("Reviews OK\n");
    }

    private void findAllReviews(RoutingContext routingContext) {
        logger.info("Finding all reviews...");
        JsonObject review = new JsonObject();
        review.put("id", "1").put("bookId", "1").put("rating", "5");
        JsonArray reviews = new JsonArray();
        reviews.add(review);
        routingContext.response().end(reviews.encodePrettily());
    }

    public static void main(String[] args) {
        ReviewsServiceBootstrap bootstrap = new ReviewsServiceBootstrap();
        CmdLineParser parser = new CmdLineParser(bootstrap);

        try {
            parser.parseArgument(args);
            bootstrap.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
