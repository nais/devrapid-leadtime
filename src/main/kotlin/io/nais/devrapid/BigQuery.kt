package io.nais.devrapid

import com.google.cloud.bigquery.*
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class BigQuery {
    val table = "deploy_history"
    val dataset = "devrapid_leadtime"
    val project = "nais-analyse-prod-2dcc"


    private companion object {
        private val log = LoggerFactory.getLogger(BigQuery::class.java)
    }

    private val bigquery =
        BigQueryOptions.newBuilder()
            .setLocation("europe-north1")
            .setProjectId(project)
            .build().service

    fun write(deployHistoryRow: DeployHistoryRow): InsertAllResponse {
        val builder = InsertAllRequest.newBuilder(TableId.of(dataset, table))
        builder.addRow(toRow(deployHistoryRow))

        val response = bigquery.insertAll(builder.build())
        if (response.hasErrors()) response.insertErrors.entries.forEach { log.info("insertError: ${it.value}") }

        return response
    }

}

fun toRow(deployHistoryRow: DeployHistoryRow): Map<String, Any> {
    return mapOf(
        "deploySha" to deployHistoryRow.deploySha,
        "repo" to deployHistoryRow.repo,
        "language" to deployHistoryRow.language,
        "deployTime" to DateTimeFormatter.ISO_ZONED_DATE_TIME.format(deployHistoryRow.deployTime),
        "pushTime" to DateTimeFormatter.ISO_ZONED_DATE_TIME.format(deployHistoryRow.pushTime),
        "firstCommitOnBranch" to DateTimeFormatter.ISO_ZONED_DATE_TIME.format(deployHistoryRow.firstCommitOnBranch)
    )
}

data class DeployHistoryRow(
    val deploySha: String,
    val repo: String,
    val language: String,
    val deployTime: ZonedDateTime,
    val pushTime: ZonedDateTime,
    val firstCommitOnBranch: ZonedDateTime?
)