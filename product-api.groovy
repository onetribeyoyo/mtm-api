@Grapes([
  @Grab('io.ratpack:ratpack-groovy:1.6.1'),
  @Grab('org.mongodb:mongodb-driver:3.12.2'), // TODO: upgrade to 4.0 ?
])

import static ratpack.groovy.Groovy.ratpack

import ratpack.http.Status
import ratpack.jackson.Jackson
import static ratpack.jackson.Jackson.json

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase

import static com.mongodb.client.model.Filters.*

import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId


//~ --------------------------------------------------------------------------------------------
//~ config

// TODO: externalize server and DB params
def mongo = new MongoService("localhost", 27017, "test")


//~ --------------------------------------------------------------------------------------------
//~ the api

int DEFAULT_PAGE_OFFSET = 0
int DEFAULT_PAGE_SIZE = 10

ratpack {

    handlers {

        //~ --------------------------------------------------------------------------------------------

        //
        // health check / heart beat
        //
        get("ping") {
            // TODO: as needed, add checks of additional runtime dependencies
            render "MTM API's are alive"
        }

        //~ --------------------------------------------------------------------------------------------

        get("product/count") {
            log(request)
            render json( new ProductCollection(mongo).count() )
        }

        //~ --------------------------------------------------------------------------------------------

        get("products") {
            log(request)

            int offset = request.queryParams.offset?.toInt() ?: DEFAULT_PAGE_OFFSET
            int pageSize = request.queryParams.max?.toInt() ?: DEFAULT_PAGE_SIZE

            def productData = new ProductCollection(mongo).findAll(offset, pageSize)
            render json( productData )
        }

        //~ --------------------------------------------------------------------------------------------

        path("product") {
            byMethod {
                post {
                    log(request)
                    context.parse(Jackson.fromJson(Map)).then { data ->

                        ProductCollection products = new ProductCollection(mongo)

                        if (products.countByName(data.name)) {
                            response.status(Status.UNPROCESSABLE_ENTITY) // 422
                            response.headers.add("X-Status-Reason", "Validation failed")
                            render "Product with that name already exits."

                        } else {
                            def doc = products.insert(data)
                            render doc.toJson()
                        }
                    }
                }
            }
        }

        //~ --------------------------------------------------------------------------------------------

        path("product/:productId") {
            byMethod {
                delete {
                    log(request)
                    if (new ProductCollection(mongo).deleteById(pathTokens.productId)) {
                        render "deleted ${pathTokens.productId}"
                    } else {
                        response.status(Status.NOT_FOUND)
                        render "${response.status.code}"
                    }
                }
                get {
                    log(request)
                    def product = new ProductCollection(mongo).find(pathTokens.productId)
                    if (product) {
                        render json( product )
                    } else {
                        response.status(Status.NOT_FOUND)
                        render "${response.status.code}"
                    }
                }
                patch {
                    log(request)
                    response.status(Status.NOT_IMPLEMENTED)
                    render "TODO: PATCH product/:productId NOT IMPLEMENTED"
                }
                put {
                    log(request)
                    response.status(Status.NOT_IMPLEMENTED)
                    render "TODO: PUT product/:productId NOT IMPLEMENTED"
                }
            }
        }

    }

}

void log(def request) {
    println ">>> ----------------------------------------------------------------"
    println ">>> ${request.method} ${request.rawUri}"
    request.headers.names.each { header ->
        println ">>>    ${header}: ${request.headers.get(header)}"
    }
}


//~ --------------------------------------------------------------------------------------------
//~ mongo utilities

class MongoService {
    String host
    int port
    String databaseName

    MongoClient client
    MongoDatabase database

    //MongoService(String databaseName = "test", String host = "localhost", int port = 27017) {
    MongoService(String host, int port, String databaseName) {
        this.host = host
        this.port = port
        this.databaseName = databaseName

        client = new MongoClient(host, port)
        database = client.getDatabase(databaseName)
    }

    MongoCollection collection(collectionName) {
        database.getCollection(collectionName)
    }

}

abstract class MongoEntity {

    MongoService mongo

    MongoEntity(MongoService mongo) {
        this.mongo = mongo
    }

    abstract MongoCollection collection()

    //~ generic methods ------------------------------------------------------------------------

    Long count() {
        collection().countDocuments()
    }

    Long countBy(String field, def value) {
        Bson filter = eq(field, value)
        collection().countDocuments(filter)
    }

    def findAll(int offset, int pageSize) {
        def objs
        if (offset) {
            // WARNING: for large collections, this skip() method may be expensive; it requires the server
            // to walk from the start of the collection/cursor before beginning to return results.
            objs = collection().find().skip(offset).limit(pageSize)
        } else {
            objs = collection().find().limit(pageSize)
        }

        objs.collect { obj ->
            // TODO: there has got to be a better way to get this ready for response as json!
            toMap(obj)
        }
    }

    def findBy(String field, def value) {
        Bson filter = eq(field, value)
        def obj = collection().find(filter).first()
        if (obj) {
            toMap(obj)
        } else {
            null
        }
    }

    // TODO: findAllBy(String field, def value) & optional offset+pageSize

    //~ ID methods -----------------------------------------------------------------------------

    boolean deleteById(String id) {
        try {
            ObjectId objId = new ObjectId(id)
            def result = collection().deleteOne(new Document("_id", objId))
            return (result.deletedCount as boolean)
        }
        catch (IllegalArgumentException ex) { // illegal id value
            return false
        }
    }

    def findById(String id) {
        try {
            Bson filter = eq("_id", new ObjectId(id))
            def obj = collection().find(filter).first()
            if (obj) {
                toMap(obj)
            } else {
                null
            }
        }
        catch (IllegalArgumentException ex) { // illegal id value
            return null
        }
    }

    //~ entity field methods -------------------------------------------------------------------

    abstract def insert(Map data)
    abstract Map toMap(def data)
}


//~ --------------------------------------------------------------------------------------------
//~ entities

class ProductCollection extends MongoEntity {

    ProductCollection(MongoService mongo) {
        super(mongo)
    }

    MongoCollection collection() {
        mongo.collection("product")
    }

    //~ entity field methods -------------------------------------------------------------------

    Long countByName(def value) {
        countBy("name", value)
    }

    def findByName(def value) {
        findBy("name", value)
    }

    def insert(Map data) {
        Document doc = new Document()
            .append("_id", ObjectId.get())
            .append("name", data.name)
        collection().insertOne(doc)
        return doc
    }

    Map toMap(def data) {
        [
            "_id":  data._id.toString(),
            "name": data.name
        ]
    }

    // TODO: validation?

}

//~ --------------------------------------------------------------------------------------------
