package io.nais.devrapid

import com.google.protobuf.InvalidProtocolBufferException
import io.nais.devrapid.github.Message
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import no.nav.protos.deployment.DeploymentEvent
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class LeadtimeCalculator(val configuration: Configuration) {

    private val consumer = KafkaConsumer<String, ByteArray>(configuration.props)

    private val LOGGER = LoggerFactory.getLogger("devrapid-leadtime")


    suspend fun run() = coroutineScope {
        launch {

            consumer.subscribe(listOf(configuration.topic))
            val messages: MutableMap<String, Message.Push> = mutableMapOf()

            while (true) {
                val records = consumer.poll(Duration.ofSeconds(1))

                records.iterator().forEach {

                    val push = try{
                        Message.Push.parseFrom(it.value())
                    }catch (e: InvalidProtocolBufferException) {
                        null
                    }
                    messages.put(push.latestCommitSha, push)


                    val deploy = try{
                        DeploymentEvent.Event.parseFrom(it.value())
                    }catch (e: InvalidProtocolBufferException) {
                        null
                    }


                    if (messages.containsKey(deploy.gitCommitSha)) {
                        computeLeadTime(messages[deploy.gitCommitSha], deploy)
                    }



                }

            }
        }
    }
}



fun Pair<Message.Push, DeploymentEvent.Event>.isComplete(): Boolean {
    return this.first != null && this.second != null
}




