// Backend/src/main/java/com/nexus/NeuroForge/services/probing/HttpPingProber.java
package com.nexus.NeuroForge.services.probing;

import com.nexus.NeuroForge.models.MonitoringTarget;
import com.nexus.NeuroForge.models.interfaces.ProbeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// Universal fallback — works with any HTTP-reachable service, regardless of
// host (Render, Vercel, AWS, bare VPS, whatever). Only requirement on the
// target side is a health endpoint that returns 2xx when healthy.
@Component
public class HttpPingProber implements Prober {

    @Autowired private RestTemplate restTemplate;

    @Override
    public ProbeStrategy supports() { return ProbeStrategy.HTTP_PING; }

    @Override
    public ProbeResult probe(MonitoringTarget target) {
        String url = target.getBaseUrl() + (target.getHealthCheckPath() != null ? target.getHealthCheckPath() : "/health");
        long start = System.currentTimeMillis();
        try {
            var response = restTemplate.getForEntity(url, String.class);
            boolean up = response.getStatusCode().is2xxSuccessful();
            return new ProbeResult(up, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return new ProbeResult(false, System.currentTimeMillis() - start);
        }
    }
}