package io.nais.devrapid

import com.google.protobuf.Timestamp
import io.nais.devrapid.github.Message
import no.nav.protos.deployment.DeploymentEvent
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import com.google.protobuf.Any
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ProtoMessageTest{
    @Test
    fun `should parse`(){
        val pushMessageProto = pushMessageProto()
        val anyByteArray = Any.parseFrom(Any.pack(pushMessageProto).toByteArray())
        assertTrue(anyByteArray.`is`((Message.Push::class.java)))
        assertFalse(anyByteArray.`is`((DeploymentEvent.Event::class.java)))
        assertThat(anyByteArray.unpack(Message.Push::class.java)).isEqualTo(pushMessageProto)

        val deploymentProto = deploymentProto()
        val anyDeploymentByteArray =  Any.parseFrom(Any.pack(deploymentProto).toByteArray())
        assertTrue(anyDeploymentByteArray.`is`((DeploymentEvent.Event::class.java)))
        assertFalse(anyDeploymentByteArray.`is`((Message.Push::class.java)))
        assertThat(anyDeploymentByteArray.unpack(DeploymentEvent.Event::class.java)).isEqualTo(deploymentProto)

    }

    private fun deploymentProto(): DeploymentEvent.Event {
        return DeploymentEvent.Event.newBuilder()
            .setApplication("application")
            .setGitCommitSha("sha")
            .build()
    }

    private fun pushMessageProto(): Message.Push {
        val fixed = ZonedDateTime.of(2020,1,1,1,1,0,0, ZoneId.of("Europe/Oslo"))
        val timestamp = Timestamp.newBuilder().setSeconds(fixed.toEpochSecond())
        return Message.Push.newBuilder()
            .setLatestCommitSha("sha")
            .setLatestCommit(timestamp)
            .setWebHookRecieved(timestamp)
            .setRef("ref")
            .setMasterBranch("masterBranch")
            .setProgrammingLanguage("programmingLanguage")
            .setRepositoryName("repositoryName")
            .setPrivateRepo(false)
            .setOrganizationName("organizationName")
            .setFilesAdded(1)
            .setFilesDeleted(2)
            .setFilesModified(3)
            .addAllCommitMessages(listOf("commitMessages"))
            .setCoAuthors(1)
            .build()

    }

}

