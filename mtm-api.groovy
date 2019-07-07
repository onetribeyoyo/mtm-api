@Grapes([
  @Grab('io.ratpack:ratpack-groovy:1.6.1'),
])

import static ratpack.groovy.Groovy.ratpack

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
            render "TODO: GET product/count NOT IMPLEMENTED"
        }

        get("products") {
            render "TODO: GET products NOT IMPLEMENTED"
        }

        path("product/:productId") {
            byMethod {
                delete {
                    render "TODO: DELETE product/:productId NOT IMPLEMENTED"
                }
                get {
                    render "TODO: GET product/:productId NOT IMPLEMENTED"
                }
                patch {
                    render "TODO: PATCH product/:productId NOT IMPLEMENTED"
                }
                post {
                    render "TODO: POST product/:productId NOT IMPLEMENTED"
                }
                put {
                    render "TODO: PUT product/:productId NOT IMPLEMENTED"
                }
            }
        }

    }

}
