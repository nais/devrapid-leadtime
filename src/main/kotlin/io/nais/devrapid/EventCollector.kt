package io.nais.devrapid

import com.google.protobuf.Any
import io.nais.devrapid.github.Message
import io.prometheus.client.Gauge
import no.nav.protos.deployment.DeploymentEvent
import no.nav.protos.deployment.DeploymentEvent.RolloutStatus.complete
import org.slf4j.LoggerFactory

class EventCollector {

    private val memory = mutableMapOf<String, Message.Push>()
    private val LOGGER = LoggerFactory.getLogger("devrapid-leadtime")

    private val leadTimeGauge = Gauge.build()
        .name("deployment_leadtime")
        .labelNames("repo")
        .help("Lead time from Github push to completed deployment")
        .create()

    private val memorySize = Gauge.build()
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
                updateMemorySizeGauge()
            }
            any.`is`(DeploymentEvent.Event::class.java) -> {
                val deploy = any.unpack(DeploymentEvent.Event::class.java)
                val sha = deploy.gitCommitSha
                val push = memory[sha]
                if (push != null && deploy.rolloutStatus == complete) {
                    LOGGER.info("Received deploy message (app: ${deploy.application} sha: ${sha})")
                    computeLeadTime(push, deploy)
                    memory.remove(sha)
                    updateMemorySizeGauge()
                } else {
                    LOGGER.info("Received deploy message (app: ${deploy.application} sha: ${sha}) with incomplete status: ${deploy.rolloutStatus}")
                }
            }
        }
    }

    private fun computeLeadTime(push: Message.Push, deploy: DeploymentEvent.Event) {
        val leadTime = deploy.timestamp.seconds - push.webHookRecieved.seconds
        LOGGER.info("Calculated lead time for deploy with sha ${push.latestCommitSha} in repo ${push.repositoryName} is $leadTime seconds")
        leadTimeGauge.labels(push.repositoryName).set(leadTime.toDouble())
    }

    private fun updateMemorySizeGauge() = memorySize.set(memory.keys.size.toDouble())
    internal fun memorySize() = memory.size


}