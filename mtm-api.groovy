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

ratpack {

    handlers {

        // --------------------------------------------------------------------------------------------

        //
        // health check / heart beat
        //
        get("ping") {
            // TODO: as needed, add checks of additional runtime dependencies
            render "MTM API's are alive"
        }

        // --------------------------------------------------------------------------------------------

        //
        // product api
        //
        get("product/count") {
            Long count = Product.count()
            render json(count)
        }

        get("products") {
            // GORM supports paginiation; e.g. `products ? offset=2 & max=3`.
            Long offset = request.queryParams.offset?.toLong() ?: 0
            Long max = request.queryParams.max?.toLong() ?: 10
            def products = Product.list(offset:offset, max:max)
            render json( products?.collect{ product -> product.toMap() } )
        }

        path("product/:productId") {
            byMethod {
                delete {
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
                    def product = Product.get(pathTokens.productId)
                    if (product) {
                        render json(product.toMap())
                    } else {
                        response.status(Status.NOT_FOUND)
                        render "${response.status.code}"
                    }
                }
                patch {
                    response.status(Status.NOT_IMPLEMENTED)
                    render "TODO: PATCH product/:productId NOT IMPLEMENTED"
                }
                post {
                    context.parse(Jackson.fromJson(Map)).then { data ->
                        Product p = new Product(name:data.name)
                        if (!p.validate()) {
                            response.status(Status.UNPROCESSABLE_ENTITY)
                            render p.errors.allErrors.collect { it }
                        } else {
                            p.save(flush:true)
                            render "/product/${p.id}"
                        }
                    }
                }
                put {
                    response.status(Status.NOT_IMPLEMENTED)
                    render "TODO: PUT product/:productId NOT IMPLEMENTED"
                }
            }
        }

    }

}

// --------------------------------------------------------------------------------------------

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

// --------------------------------------------------------------------------------------------
