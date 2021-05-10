package io.nais.devrapid

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.util.*

private fun config() =
    systemProperties() overriding EnvironmentVariables overriding ConfigurationProperties.fromResource("defaults.properties")

fun setProps(): Properties {
    val props: Properties = Properties()

    props["bootstrap.servers"] = config()[Key("KAFKA_BROKERS", stringType)]
    props["security.protocol"] = "SSL"
    props["schema.registry.url"] = config()[Key("KAFKA_SCHEMA_REGISTRY", stringType)]
    props["basic.auth.credentials.source"] = "USER_INFO"
    props["ssl.truststore.location"] = config()[Key("KAFKA_TRUSTSTORE_PATH", stringType)]
    props["ssl.truststore.password"] = config()[Key("KAFKA_CREDSTORE_PASSWORD", stringType)]
    props["ssl.keystore.type"] = "PKCS12"
    props["ssl.keystore.location"] = config()[Key("KAFKA_KEYSTORE_PATH", stringType)]
    props["ssl.keystore.password"] = config()[Key("KAFKA_CREDSTORE_PASSWORD", stringType)]
    props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
    props["value.serializer"] = "org.apache.kafka.common.serialization.ByteArraySerializer"
    return props

}

data class Configuration(
    val topic: String = "aura.dev-rapid",
    val props: Properties = setProps()
)
