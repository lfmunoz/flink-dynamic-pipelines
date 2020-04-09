package eco.analytics.bridge.monitor


import eco.analytics.flink.data.eco.MonitorMessage
import java.util.ArrayList
import java.util.HashMap
import java.util.Random
import java.util.UUID
import java.util.stream.IntStream


/**
 * Generator for simple flat property data structure.
 *
 * @param nrOfUuids the number of UUIDs to use when generating [MonitorMessage] objects.
 */
class MonitorMessageDataGenerator (nrOfUuids: Int) {
    private val uuidList: MutableList<String>

    init {
        uuidList = ArrayList(nrOfUuids)
        // Fill up the UUID list
        var count = 0
        while (uuidList.size < nrOfUuids) {
            uuidList.add("UUID-GENERATED-$count")
            count += 1
        }
    }

    /**
     * Generates a [MonitorMessage] object with the given number of numeric and boolean properties.
     *
     * @param numericProperties the number of numeric properties that will be included in the object.
     * @param booleanProperties the number of boolean properties that will be included in the object.
     * @return a new instance of [MonitorMessage]
     */
    fun random(hasUUID: Boolean, numericProperties: Int, booleanProperties: Int): MonitorMessage {
        val r = Random()
        val uuid = uuidList[r.nextInt(uuidList.size)]
        val time = System.currentTimeMillis()

        // Build the element property values (note, only numeric values are provided)
        val values = HashMap<String, Double>()
        IntStream.rangeClosed(1, numericProperties)
                .forEach { i -> values["property.name.goes.here.numeric$i"] = r.nextDouble() * 100 + 50 }

        // Build the diagnostic results (boolean)
        val diagnosticResults = HashMap<String, Boolean>()
        IntStream.rangeClosed(1, booleanProperties)
                .forEach { i -> diagnosticResults["property.name.goes.here.boolean$i"] = r.nextBoolean() }

        return MonitorMessage(
                uuid =  if (hasUUID)  uuid  else null,
                id = "id-$uuid",
                informTime =  time,
                informInterval = 15,
                collectPickupTime = time,
                collectCompleteTime = time,
                diagnosticsNotComputed = false,
                subscriberId = "subscriberIDTest",
                values = values,
                diagnosticResults = diagnosticResults
        )
    }
}
