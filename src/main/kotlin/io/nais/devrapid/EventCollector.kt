package io.nais.devrapid

import com.google.protobuf.Any
import io.nais.devrapid.github.Message
import io.prometheus.client.Gauge
import no.nav.protos.deployment.DeploymentEvent
import org.slf4j.LoggerFactory

class EventCollector {
    private val memory = mutableMapOf<String, Message.Push>()
    private val LOGGER = LoggerFactory.getLogger("devrapid-leadtime")
    private val leadTimeGauge = Gauge.build()
        .name("deployment_leadtime")
        .labelNames("repo")
        .help("Lead time from Github push to completed deployment")
        .create()

    private val messageMapSize = Gauge.build()
        .name("message_map_size")
        .help("Size of map that holds messages (deploy and push)")
        .create()

    internal fun collectOrComputeLeadTime(byteArray: ByteArray) {
        val any = Any.parseFrom(byteArray)
        when {
            any.`is`(Message.Push::class.java) -> {
                val push = any.unpack(Message.Push::class.java)

                LOGGER.info("Received push message (repo: ${push.repositoryName} sha: ${push.latestCommitSha})")
                memory[push.latestCommitSha] = push
                messageMapSize.set(memory.keys.size.toDouble())
            }
            any.`is`(DeploymentEvent.Event::class.java) -> {
                val deploy = any.unpack(DeploymentEvent.Event::class.java)
                val key = deploy.gitCommitSha
                if (deploy.rolloutStatus == DeploymentEvent.RolloutStatus.complete && memory.containsKey(key)) {
                    LOGGER.info("Received deploy message (app: ${deploy.application} sha: ${key})")
                    computeLeadTime(memory[key]!!, deploy)
                    memory.remove(key)
                    messageMapSize.set(memory.keys.size.toDouble())
                }else{
                    LOGGER.info("Received deploy message (app: ${deploy.application} sha: ${key}) with status ${deploy.rolloutStatus}")
                }
            }
            else -> {
            }
        }
    }

    private fun computeLeadTime(push: Message.Push, deploy: DeploymentEvent.Event) {
        val leadTime = deploy.timestamp.seconds - push.webHookRecieved.seconds.toDouble()
        LOGGER.info("Lead time for ${push.repositoryName} is $leadTime")
        leadTimeGauge.labels(push.repositoryName).set(leadTime)

    }

    fun memorySize() = memory.size
}