package com.aws.iot.evergreen.telemetry.impl;

import com.aws.iot.evergreen.telemetry.models.TelemetryMetricName;
import com.aws.iot.evergreen.telemetry.models.TelemetryNamespace;
import com.aws.iot.evergreen.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class MetricDataPointSerializer extends StdDeserializer {
    private static final long serialVersionUID = 0L;

    public MetricDataPointSerializer() {
        this(null);
    }

    protected MetricDataPointSerializer(Class vc) {
        super(vc);
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        JsonNode node =  jsonParser.getCodec().readTree(jsonParser);
        TelemetryNamespace tn = TelemetryNamespace.valueOf(node.get("M").get("NS").asText());
        TelemetryMetricName tm = TelemetryMetricName.valueOf(node.get("M").get("N").asText());
        TelemetryUnit tu = TelemetryUnit.valueOf(node.get("M").get("U").asText());
        Object v = node.get("V").asText();
        long ts = node.get("TS").asLong();
        Metric m = new Metric(tn,tm,tu);
        return new MetricDataPoint(m,v,ts);

    }
}
