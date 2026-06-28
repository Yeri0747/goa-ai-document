package es.upm.api.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SystemResourceTest {

    private SystemResource systemResource;

    @BeforeEach
    void setUp() {
        systemResource = new SystemResource();
        ReflectionTestUtils.setField(systemResource, "artifact", "goa-ai-document");
        ReflectionTestUtils.setField(systemResource, "version", "1.7.0-SNAPSHOT");
        ReflectionTestUtils.setField(systemResource, "build", "2026-06-28");
    }

    @Test
    void generateBadgeBuildsSvg() {
        String badge = systemResource.generateBadge("AWS", "v1.0.0");

        assertThat(badge).startsWith("<svg");
        assertThat(badge).contains("AWS");
        assertThat(badge).contains("v1.0.0");
    }

    @Test
    void applicationInfoIncludesMetadata() {
        String info = systemResource.applicationInfo();

        assertThat(info).contains("goa-ai-document");
        assertThat(info).contains("1.7.0-SNAPSHOT");
        assertThat(info).contains("2026-06-28");
    }

    @Test
    void generateBadgeEndpointReturnsSvgBytes() {
        byte[] badgeBytes = systemResource.generateBadge();

        assertThat(badgeBytes).isNotEmpty();
        assertThat(new String(badgeBytes)).startsWith("<svg");
    }
}
