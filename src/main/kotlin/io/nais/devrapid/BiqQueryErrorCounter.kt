package io.nais.devrapid

import io.prometheus.client.Counter

class BiqQueryErrorCounter {

    companion object {
        private val errors = Counter.build()
            .name("biquery_errors")
            .help("Errors from BiqQuery")
            .register()
    }

    internal fun countError(){
        errors.inc()
    }
}