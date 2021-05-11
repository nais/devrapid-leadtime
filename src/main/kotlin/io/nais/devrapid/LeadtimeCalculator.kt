package io.nais.devrapid

import com.google.protobuf.InvalidProtocolBufferException
import io.nais.devrapid.github.Message
import io.prometheus.client.Gauge
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import no.nav.protos.deployment.DeploymentEvent
import no.nav.protos.deployment.DeploymentEvent.RolloutStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class LeadtimeCalculator(val configuration: Configuration) {

    private val consumer = KafkaConsumer<String, ByteArray>(configuration.props)
    private val leadTimeGauge =
        Gauge.build()
            .name("deployment_leadtime")
            .labelNames("repo")
            .help("Lead time from Github push to completed deployment")
            .create()
    private val messageMapSize =
        Gauge.build()
            .name("message_map_size")
            .help("Size of map that holds messages (deploy and push)")
            .create()

    private val LOGGER = LoggerFactory.getLogger("devrapid-leadtime")


    fun run() {
        LOGGER.info("Started consumer thread")
        consumer.subscribe(listOf(configuration.topic))
        val messages: MutableMap<String, Message.Push> = mutableMapOf()

        while (true) {
            val records = consumer.poll(Duration.ofSeconds(1))
            records.iterator().forEach {
                val push = it.parsePushMessage()
                push?.let {
                    messages[push.latestCommitSha] = push
                    LOGGER.info("Received push message (repo: ${push.repositoryName} sha: ${push.latestCommitSha})")
                }
                if (push == null) {
                    val deploy = it.parseDeploymentEvent()
                    deploy?.let {
                        val key = deploy.gitCommitSha
                        LOGGER.info("Received deploy message (app: ${deploy.application} sha: ${deploy.gitCommitSha})")
                        if (deploy.rolloutStatus == RolloutStatus.complete && messages.containsKey(key)) {
                            computeLeadTime(messages[key]!!, deploy)
                            messages.remove(key)
                        }
                    }
                }
            }
            messageMapSize.set(messages.keys.size.toDouble())
        }
    }

    private fun computeLeadTime(push: Message.Push, deploy: DeploymentEvent.Event) {
        val leadTime = deploy.timestamp.seconds - push.webHookRecieved.seconds.toDouble()
        LOGGER.info("Lead time for ${push.repositoryName} is $leadTime")
        leadTimeGauge.labels(push.repositoryName).set(leadTime)


    }
}

private fun ConsumerRecord<String, ByteArray>.parsePushMessage(): Message.Push? {
    return try {
        Message.Push.parseFrom(this.value())
    } catch (e: InvalidProtocolBufferException) {
        null
    }
}

private fun ConsumerRecord<String, ByteArray>.parseDeploymentEvent(): DeploymentEvent.Event? {
    return try {
        DeploymentEvent.Event.parseFrom(this.value())
    } catch (e: InvalidProtocolBufferException) {
        null
    }
}

