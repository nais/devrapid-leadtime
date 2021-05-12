package io.nais.devrapid

import no.nav.protos.deployment.DeploymentEvent.RolloutStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EventCollectorTest {


    @Test
    fun `should parse`() {
        val collector = EventCollector()
        collector.collectOrComputeLeadTime(anyPushMessageProto(sha = "1").toByteArray())
        assertThat(collector.memorySize()).isEqualTo(1)
        collector.collectOrComputeLeadTime(anyDeploymentProto(sha = "1", rolloutStatus = RolloutStatus.complete).toByteArray())
        assertThat(collector.memorySize()).isEqualTo(0)
    }
}