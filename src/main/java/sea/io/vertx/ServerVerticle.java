package sea.io.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import sea.whiskey.Whiskey;

import java.util.LinkedHashMap;
import java.util.Map;

public class ServerVerticle extends AbstractVerticle {

    private Map<Integer, Whiskey> products = new LinkedHashMap<>();

    private void createSomeData() {
        Whiskey bowmore = new Whiskey("Bowmore 15 Years Laimrig", "Scotland, Islay");
        products.put(bowmore.getId(), bowmore);
        Whiskey talisker = new Whiskey("Talisker 57° North", "Scotland, Island");
        products.put(talisker.getId(), talisker);
    }
    
    @Override
    public void start(Future<Void> future) throws Exception {
        createSomeData();

        // Create a router object.
        Router router = Router.router(vertx);

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });

        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.route("/api/*").handler(BodyHandler.create());
        router.post("/api/whiskies").handler(this::addOne);
        router.get("/api/whiskies").handler(this::getAll);
        router.delete("/api/whiskies/:id").handler(this::deleteOne);
        router.get("/api/whiskies/:id").handler(this::getOne);
        router.post("/api/whiskies/:id").handler(this::updateOne);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        });
    }

    public void getAll(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(products.values()));
    }

    public void addOne(RoutingContext routingContext) {
        final Whiskey whisky = Json.decodeValue(routingContext.getBodyAsString(),
                Whiskey.class);
        products.put(whisky.getId(), whisky);
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(whisky));
    }

    public void updateOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        final Whiskey modifiedWhiskey = Json.decodeValue(routingContext.getBodyAsString(),
                Whiskey.class);

        if (id != null) {
            Integer idAsInteger = Integer.valueOf(id);
            if (products.containsKey(idAsInteger)) {
                Whiskey whiskey = products.get(idAsInteger);
                whiskey.setName(modifiedWhiskey.getName());
                whiskey.setOrigin(modifiedWhiskey.getOrigin());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(whiskey));
            }
        }
        routingContext.response().setStatusCode(400).end();
    }

    public void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            products.remove(idAsInteger);
        }
        routingContext.response().setStatusCode(204).end();
    }

    public void getOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id != null) {
            Integer idAsInteger = Integer.valueOf(id);
            if (products.containsKey(idAsInteger)) {
                Whiskey whiskey = products.get(idAsInteger);
                routingContext.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(whiskey));
            }
        }
        routingContext.response().setStatusCode(400).end();
    }

}
