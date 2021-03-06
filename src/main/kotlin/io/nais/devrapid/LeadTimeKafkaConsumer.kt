package io.nais.devrapid

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class LeadTimeKafkaConsumer(val configuration: Configuration) {

    private val consumer = KafkaConsumer<String, ByteArray>(configuration.props)
    private val LOGGER = LoggerFactory.getLogger("devrapid-leadtime")
    private val bigquery = BigQuery()


    fun run() {
        LOGGER.info("Started consumer thread")
        consumer.subscribe(listOf(configuration.topic))
        val collector = EventCollector()
        while (true) {
            val records = consumer.poll(Duration.ofSeconds(1))
            records.iterator()
                .forEach { collector.collectOrComputeLeadTime(it.value())?.let { row -> bigquery.write(row) } }
        }
    }
}


