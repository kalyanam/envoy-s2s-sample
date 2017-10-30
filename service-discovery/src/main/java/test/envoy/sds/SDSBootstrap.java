package test.envoy.sds;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
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
import java.util.Map;
import java.util.Set;

public class SDSBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(SDSBootstrap.class);

    @Option(name = "-port")
    private Integer port;

    private EndpointStore endpointStore = new EndpointStore();

    private void start() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            endpointStore.addEndpoint("sds-service", new Endpoint(ipAddress, port));
        } catch (Exception ex) {
            logger.error("Unable to bootstrap SDS: {}", ex);
            return ;
        }

        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*"));

        router.get("/ping").handler(this::handlePing);
        router.get("/v1/registration/:serviceName").handler(this::findEndpoints);
        router.post("/v1/registration/:serviceName").handler(this::saveEndpoint);

        router.get("/admin/v1/registration").handler(this::findAllEndpoints);

        router.get("/*").handler(rc -> {
            logger.info("Request is: {}", rc.request().path());
            rc.response().end();
        });

        vertx.createHttpServer().requestHandler(router::accept).listen(this.port);

        try {
            logger.info("Started sds on host:{}, port: {}", InetAddress.getLocalHost().getHostAddress(), this.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void findAllEndpoints(RoutingContext routingContext) {
        Map<?, ?> endpoints = this.endpointStore.getAllEndpoints();
        routingContext.response().end(Json.encodePrettily(endpoints));
    }

    private void handlePing(RoutingContext routingContext) {
        logger.info("Successful ping...");
        routingContext.response().end("SDS OK\n");
    }

    private void findEndpoints(RoutingContext routingContext) {
        String serviceName = routingContext.request().getParam("serviceName");
        logger.info("SDS Request is: {}, for: {}", routingContext.request().path(), serviceName);

        Set<Endpoint> endpoints = this.endpointStore.getEndpoints(serviceName);
        if(endpoints.size() == 0) {
            logger.info("Unable to find any endpoints for: {}", serviceName);
            routingContext.response().setStatusCode(404);
            return;
        }

        logger.info("Found endpoints: {}", endpoints);

        JsonArray hostsArray = new JsonArray();

        for(Endpoint endpoint: endpoints) {
            JsonObject host = new JsonObject();
            host.put("ip_address", endpoint.getIpAddress());
            host.put("port", endpoint.getPort());
            hostsArray.add(host);
        }

        JsonObject hosts = new JsonObject();
        hosts.put("hosts", hostsArray);

        logger.info("Response is: {}", hosts.encode());
        routingContext.response().end(hosts.encodePrettily());
    }

    private void saveEndpoint(RoutingContext routingContext) {
        JsonObject payload = routingContext.getBodyAsJson();
        String serviceName = payload.getString("service_name");
        String ipAddress = payload.getString("ip_address");
        Integer port = payload.getInteger("port");
        endpointStore.addEndpoint(serviceName, new Endpoint(ipAddress, port));
        routingContext.response().setStatusCode(201).end("Endpoint saved");
    }

    public static void main(String[] args) {
        SDSBootstrap bootstrap = new SDSBootstrap();
        CmdLineParser parser = new CmdLineParser(bootstrap);

        try {
            parser.parseArgument(args);
            bootstrap.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
