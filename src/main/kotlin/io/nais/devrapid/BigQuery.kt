package io.nais.devrapid

import com.google.cloud.bigquery.*
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

class BigQuery {
    private val table = "deploy_history"
    private val dataset = "devrapid_leadtime"
    private val project = "nais-analyse-prod-2dcc"

    private companion object {
        private val log = LoggerFactory.getLogger(BigQuery::class.java)
    }

    private val bigquery =
        BigQueryOptions.newBuilder()
            .setLocation("europe-north1")
            .setProjectId(project)
            .build().service

    fun write(deployHistoryRow: DeployHistoryRow): InsertAllResponse {
        val response = bigquery.insertAll(
            InsertAllRequest.newBuilder(TableId.of(dataset, table))
                .addRow(deployHistoryRow.asMap())
                .build()
        )
        if (response.hasErrors()) response.insertErrors.entries.forEach { log.info("insertError: ${it.value}") }
        return response
    }

}


data class DeployHistoryRow(
    val deploySha: String,
    val repo: String,
    val language: String,
    val deployTime: ZonedDateTime,
    val pushTime: ZonedDateTime,
    val firstCommitOnBranch: ZonedDateTime?
) {
    fun asMap(): Map<String, String> {
        val map = mutableMapOf(
            "deploySha" to deploySha,
            "repo" to repo,
            "language" to language,
            "deployTime" to deployTime.asTimeStamp(),
            "pushTime" to pushTime.asTimeStamp(),
        )

        if (firstCommitOnBranch == null) {
            map["firstCommitOnBranch"] = ""
        } else {
            map["firstCommitOnBranch"] = firstCommitOnBranch.asTimeStamp()
        }
        return map.toMap()
    }
    private fun ZonedDateTime.asTimeStamp() = ISO_LOCAL_DATE_TIME.format(this)
}