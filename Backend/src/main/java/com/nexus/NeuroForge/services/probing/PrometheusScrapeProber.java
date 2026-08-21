// Backend/src/main/java/com/nexus/NeuroForge/services/probing/PrometheusScrapeProber.java
package com.nexus.NeuroForge.services.probing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.NeuroForge.models.MonitoringTarget;
import com.nexus.NeuroForge.models.interfaces.ProbeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Richer option — requires the target to expose /metrics and to have a
// matching scrape job configured in prometheus.yml. Falls back to "down" on
// any parsing/network failure rather than throwing, same contract as
// HttpPingProber, so callers never need to special-case which prober ran.
@Component
public class PrometheusScrapeProber implements Prober {

    @Autowired private RestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Value("${app.prometheus.url:http://prometheus:9090}")
    private String prometheusBaseUrl;

    @Override
    public ProbeStrategy supports() { return ProbeStrategy.PROMETHEUS_SCRAPE; }

    @Override
    public ProbeResult probe(MonitoringTarget target) {
        if (target.getPrometheusJobName() == null || target.getPrometheusJobName().isBlank()) {
            return new ProbeResult(false, 0);
        }
        String query = "up{job=\"" + target.getPrometheusJobName() + "\"}";
        String url = prometheusBaseUrl + "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        long start = System.currentTimeMillis();
        try {
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode results = objectMapper.readTree(raw).path("data").path("result");
            boolean up = results.isArray() && results.size() > 0
                    && "1".equals(results.get(0).path("value").get(1).asText());
            return new ProbeResult(up, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return new ProbeResult(false, System.currentTimeMillis() - start);
        }
    }
}