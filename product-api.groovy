@Grapes([
  @Grab('io.ratpack:ratpack-groovy:1.6.1'),
  @Grab("org.grails:grails-datastore-gorm-mongodb:6.1.1.RELEASE"),
])

import static ratpack.groovy.Groovy.ratpack

import ratpack.http.Status
import ratpack.jackson.Jackson
import static ratpack.jackson.Jackson.json

import grails.gorm.annotation.Entity
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.bson.types.ObjectId


def datastore = new MongoDatastore(Product)

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
            logHandler(request)
            Long count = Product.count()
            render json(count)
        }

        //~ --------------------------------------------------------------------------------------------

        get("products") {
            logHandler(request)
            // GORM supports paginiation; e.g. `products ? offset=2 & max=3`.
            Long offset = request.queryParams.offset?.toLong() ?: 0
            Long max = request.queryParams.max?.toLong() ?: 10
            def products = Product.list(offset:offset, max:max)
            render json( products?.collect{ product -> product.toMap() } )
        }

        //~ --------------------------------------------------------------------------------------------

        path("product") {
            byMethod {
                post {
                    logHandler(request)
                    context.parse(Jackson.fromJson(Map)).then { data ->
                        if (Product.countByName(data.name)) {
                            response.status(Status.UNPROCESSABLE_ENTITY) // 422
                            response.headers.add("X-Status-Reason", "Validation failed")
                            render "Product with that name already exits."
                        } else {
                            Product p = new Product()
                            p.id = new ObjectId()
                            p.name = data.name
                            p.save(flush:true)
                            println p.toMap()
                            render json ( p.toMap() )
                            //render "/product/${p.id}"
                        }
                    }
                }
            }
        }

        //~ --------------------------------------------------------------------------------------------

        path("product/:productId") {
            byMethod {
                delete {
                    logHandler(request)
                    def product = Product.get(new ObjectId(pathTokens.productId))
                    if (product) {
                        product.delete(flush:true)
                        response.status(Status.NO_CONTENT)
                        render json(product.toMap())
                    } else {
                        response.status(Status.NOT_FOUND)
                        render "${response.status.code}"
                    }
                }
                get {
                    logHandler(request)
                    def product = Product.get(pathTokens.productId)
                    if (product) {
                        render json(product.toMap())
                    } else {
                        response.status(Status.NOT_FOUND)
                        render "${response.status.code}"
                    }
                }
                patch {
                    logHandler(request)
                    response.status(Status.NOT_IMPLEMENTED)
                    render "TODO: PATCH product/:productId NOT IMPLEMENTED"
                }
                put {
                    logHandler(request)
                    response.status(Status.NOT_IMPLEMENTED)
                    render "TODO: PUT product/:productId NOT IMPLEMENTED"
                }
            }
        }

    }

}


//~ --------------------------------------------------------------------------------------------

void logHandler(def request) {
    println ">>> ----------------------------------------------------------------"
    println ">>> ${request.method} ${request.rawUri}"
    request.headers.names.each { header ->
        println ">>>    ${header}: ${request.headers.get(header)}"
    }
}





//~ --------------------------------------------------------------------------------------------

@Entity
class Product {
    ObjectId id
    String name

    static constraints = {
        name size: 5..42, blank: false, unique: true
    }

    def toMap() {
        [
            id: id?.toString(),
            name: name
        ]
    }
}

//~ --------------------------------------------------------------------------------------------
