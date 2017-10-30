package test.envoy.books;

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
public class BooksServiceBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(BooksServiceBootstrap.class);

    @Option(name = "-port")
    private Integer port;
    @Option(name = "-proxyPort")
    private Integer proxyPort;

    private Vertx vertx;
    private HttpClient sdsClient;
    private HttpClient reviewsClient;

    private void start() {
        this.vertx = Vertx.vertx();

        //Client to call out to sds service at port 9999
        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost("0.0.0.0")
                .setDefaultPort(9999);
        this.sdsClient = this.vertx.createHttpClient(options);

        //Client to call out to reviews service at port 10010
        options = new HttpClientOptions()
                .setDefaultHost("0.0.0.0")
                .setDefaultPort(10010);
        this.reviewsClient = this.vertx.createHttpClient(options);

        this.startApiServer()
                .compose(v -> this.registerInSDS())
                .setHandler(res -> {
                    if(res.succeeded()) {
                        try {
                            logger.info("Started books service on host:{}, port: {}", InetAddress.getLocalHost().getHostAddress(), this.port);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    } else {
                        logger.error("Unable to start books-service. Shutting down: {}", res.cause());
                        System.exit(-1);
                    }
                });
    }

    private Future<Void> startApiServer() {
        Future<Void> future = Future.future();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*"));

        router.get("/ping").handler(this::handlePing);
        router.get("/v1/books").handler(this::findAllBooks);
        router.get("/v1/books/reviews").handler(this::findAllBooksWithReviews);

        try {
            vertx.createHttpServer().requestHandler(router::accept).listen(this.port);
            logger.info("Started the api server");
            future.complete();
        } catch (Exception ex) {
            future.fail(ex);
        }
        return future;
    }

    private Future<Void> registerInSDS() {
        Future<Void> future = Future.future();
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            HttpClientRequest request = this.sdsClient.post("/v1/registration/books-service", rh -> {
                logger.info("Registration request came back with status code: {}", rh.statusCode());
                if(rh.statusCode() != 201) {
                    logger.info("Registration failed: {}", rh.statusCode());
                    future.fail("Unable to register books-service");
                }
                rh.bodyHandler(buffer -> {
                    logger.info("Registration succeeded: {}, {}", rh.statusCode(), buffer);
                    future.complete();
                });
            });
            JsonObject payload = new JsonObject();
            payload.put("service_name", "books-service");
            payload.put("ip_address", ipAddress);
            payload.put("port", proxyPort);

            request.putHeader("content-length", payload.encode().length()+"");
            request.putHeader("content-type", "application/json");
            request.exceptionHandler(exception -> {
                logger.error("Unable to reach SDS server: {}", exception);
                future.fail(exception);
            });
            request.end(payload.encode());
            logger.info("Registration sent to SDS");
        } catch (Exception ex) {
            logger.info("Unable to register in SDS, shutting down: {}", ex);
            future.fail(ex);
        }
        return future;
    }

    private void handlePing(RoutingContext routingContext) {
        logger.info("Successful ping...");
        routingContext.response().end("Books OK\n");
    }

    private void findAllBooks(RoutingContext routingContext) {
        logger.info("Finding all books...");
        Boolean includeReviews = Boolean.valueOf(routingContext.request().getParam("includeReviews"));
        if(includeReviews) {
            this.findAllBooksWithReviews(routingContext);
        } else {
            JsonObject book = new JsonObject();
            book.put("id", "1").put("name", "Gita").put("author", "Vyasa");
            JsonArray books = new JsonArray();
            books.add(book);
            routingContext.response().end(books.encodePrettily());
        }
    }

    private void findAllBooksWithReviews(RoutingContext routingContext) {
        logger.info("Finding all books and their reviews...");

        this.reviewsClient.getNow("/v1/reviews", responseHandler -> {
            logger.info("Status code is: {}", responseHandler.statusCode());

            if(responseHandler.statusCode() != 200) {
                routingContext.fail(500);
                return ;
            }

            responseHandler.bodyHandler(buffer -> {
                logger.info("Response is: {}", buffer.toString());
                JsonObject book = new JsonObject();
                book.put("id", "1")
                        .put("name", "Gita")
                        .put("author", "Vyasa")
                        .put("reviews", new JsonArray(buffer.toString()));
                JsonArray books = new JsonArray();
                books.add(book);
                routingContext.response().end(books.encodePrettily());
            });
        });
    }

    public static void main(String[] args) {
        BooksServiceBootstrap booksServiceBootstrap = new BooksServiceBootstrap();
        CmdLineParser parser = new CmdLineParser(booksServiceBootstrap);

        try {
            parser.parseArgument(args);
            booksServiceBootstrap.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
