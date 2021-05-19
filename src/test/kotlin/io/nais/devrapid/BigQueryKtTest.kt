package io.nais.devrapid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class BigQueryKtTest {


    @Test
    fun `date string is without zone`() {

        val date = ZonedDateTime.parse("2021-05-19T09:09:01+02:00[Europe/Oslo]")
        val row = DeployHistoryRow(
            deploySha = "123",
            repo = "repo",
            language = "language",
            deployTime = date,
            pushTime = date,
            firstCommitOnBranch = date
        )
        assertThat(row.asMap()["deployTime"]).isEqualTo("2021-05-19T09:09:01")
    }

    @Test
    fun `handles null for firstcommit`() {

        val row = DeployHistoryRow(
            deploySha = "123",
            repo = "repo",
            language = "language",
            deployTime = ZonedDateTime.now(),
            pushTime = ZonedDateTime.now(),
            firstCommitOnBranch = null
        )
        assertThat(row.asMap()).doesNotContainKey("firstCommitOnBranch")
    }
}