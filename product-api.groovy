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
import org.bson.types.ObjectId


//~ --------------------------------------------------------------------------------------------

// TODO: externalize server and DB params
def mongo = new MongoService("localhost", 27017, "test")

int DEFAULT_OFFSET = 0
int DEFAULT_PAGESIZE = 10

//~ --------------------------------------------------------------------------------------------

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

            int offset = request.queryParams.offset?.toInt() ?: DEFAULT_OFFSET
            int pageSize = request.queryParams.max?.toInt() ?: DEFAULT_PAGESIZE

            def productData = new ProductCollection(mongo).all(offset, pageSize)
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
                    if (new ProductCollection(mongo).delete(pathTokens.productId)) {
                        render "deleted ${pathTokens.productId}"
                    } else {
                        response.status(Status.NOT_FOUND)
                        render "${response.status.code}"
                    }
                }
                get {
                    log(request)
                    def product = new ProductCollection(mongo).get(pathTokens.productId)
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


//~ --------------------------------------------------------------------------------------------

void log(def request) {
    println ">>> ----------------------------------------------------------------"
    println ">>> ${request.method} ${request.rawUri}"
    request.headers.names.each { header ->
        println ">>>    ${header}: ${request.headers.get(header)}"
    }
}

//~ --------------------------------------------------------------------------------------------

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

//~ --------------------------------------------------------------------------------------------

class ProductCollection {

    MongoService mongo

    ProductCollection(MongoService mongo) {
        this.mongo = mongo
    }

    // TODO: validation?

    MongoCollection collection() {
        mongo.collection("product")
    }

    Long count() {
        collection().countDocuments()
    }

    Long countByName(def value) {
        countBy("name", value)
    }

    def countBy(String field, def value) {
        collection().countDocuments(eq(field, value))
    }

    def get(String id) {
        try {
            ObjectId objId = new ObjectId(id)
            def obj = collection().find(eq("_id", objId)).first()
            if (obj) {
                toMap(obj)
            } else {
                null
            }
        }
        catch (IllegalArgumentException ex) {
            return null
        }
    }

    boolean delete(String id) {
        println 1
        try {
            println 2
            ObjectId objId = new ObjectId(id)
            println "id --> ${id}"
            println "objId --> ${objId}"
            def result = collection().deleteMany(eq("_id", objectid))
            println "result.deletedCount --> ${result.deletedCount}"
            println 3
            return (result.deletedCount as boolean)
        }
        catch (IllegalArgumentException ex) {
            println 4
            return false
        }
    }

    def getByName(def value) {
        getBy("name", value)
    }

    def getBy(String field, def value) {
        def obj = collection().find(eq(field, value)).first()
        if (obj) {
            toMap(obj)
        } else {
            null
        }
    }

    def all(int offset, int pageSize) {
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
}

//~ --------------------------------------------------------------------------------------------
