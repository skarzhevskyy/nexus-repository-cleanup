package com.pyx4j.nxrm.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import com.pyx4j.nxrm.cleanup.model.CleanupRule;
import com.pyx4j.nxrm.cleanup.model.CleanupRuleSet;
import org.junit.jupiter.api.Test;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Integration test that demonstrates the complete ComponentFilter functionality
 * matching the original issue requirements.
 */
class ComponentFilterIntegrationTest {

    @Test
    void componentFilter_withRealWorldScenario_shouldWorkCorrectly() {
        // Create a rule set that mimics real-world cleanup policies
        CleanupRule deleteOldSnapshots = CleanupRuleBuilder.builder()
                .name("delete-old-snapshots")
                .action("delete")
                .repositories(List.of("maven-snapshots"))
                .names(List.of("*-SNAPSHOT"))
                .updated("30d") // Components updated more than 30 days ago
                .downloaded("90d") // Not downloaded in the last 90 days
                .build();

        CleanupRule keepProductionArtifacts = CleanupRuleBuilder.builder()
                .name("keep-production-artifacts") 
                .action("keep")
                .repositories(List.of("maven-*"))
                .names(List.of("prod-*"))
                .build();

        CleanupRule deleteNeverDownloaded = CleanupRuleBuilder.builder()
                .name("delete-never-downloaded")
                .action("delete")
                .repositories(List.of("*"))
                .downloaded("never")
                .build();

        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(
                deleteOldSnapshots, 
                keepProductionArtifacts, 
                deleteNeverDownloaded
        ));

        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Test repository filtering
        assertThat(filter.matchesRepositoryFilter("maven-snapshots")).isTrue();
        assertThat(filter.matchesRepositoryFilter("npm-public")).isTrue(); // '*' matches all
        assertThat(filter.matchesRepositoryFilter("docker-private")).isTrue();

        // Component that should be deleted: old snapshot, not downloaded recently
        ComponentXO oldSnapshot = createComponent(
                "maven-snapshots", "com.example", "test-app-SNAPSHOT", "1.0-SNAPSHOT",
                OffsetDateTime.now().minusDays(60), // Created 60 days ago
                OffsetDateTime.now().minusDays(120) // Downloaded 120 days ago
        );
        assertThat(filter.getComponentFilter().test(oldSnapshot)).isTrue();

        // Component that should be kept: production artifact (keep rule overrides delete)
        ComponentXO prodArtifact = createComponent(
                "maven-releases", "com.example", "prod-service", "1.0.0",
                OffsetDateTime.now().minusDays(60), // Created 60 days ago
                null // Never downloaded
        );
        assertThat(filter.getComponentFilter().test(prodArtifact)).isFalse();

        // Component that should be deleted: never downloaded
        ComponentXO neverDownloaded = createComponent(
                "maven-central-proxy", "org.apache", "commons-lang", "3.0",
                OffsetDateTime.now().minusDays(10), // Created recently
                null // Never downloaded
        );
        assertThat(filter.getComponentFilter().test(neverDownloaded)).isTrue();

        // Component that should not be deleted: recent activity
        ComponentXO recentlyUsed = createComponent(
                "maven-snapshots", "com.example", "test-SNAPSHOT", "2.0-SNAPSHOT",
                OffsetDateTime.now().minusDays(10), // Created recently
                OffsetDateTime.now().minusDays(5) // Downloaded recently
        );
        assertThat(filter.getComponentFilter().test(recentlyUsed)).isFalse();
    }

    @Test
    void componentFilter_withMultipleAssets_shouldRequireAllAssetsToMatch() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .name("delete-old-components")
                .action("delete")
                .updated("30d")
                .build();

        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Component with mixed asset ages - should not be deleted
        ComponentXO mixedAgeComponent = new ComponentXO();
        mixedAgeComponent.setRepository("test-repo");
        mixedAgeComponent.setName("mixed-component");
        
        AssetXO oldAsset = new AssetXO();
        oldAsset.setBlobCreated(OffsetDateTime.now().minusDays(60));
        
        AssetXO newAsset = new AssetXO();
        newAsset.setBlobCreated(OffsetDateTime.now().minusDays(10));
        
        mixedAgeComponent.setAssets(List.of(oldAsset, newAsset));

        // Should not match because not ALL assets are old
        assertThat(filter.getComponentFilter().test(mixedAgeComponent)).isFalse();

        // Component with all old assets - should be deleted
        ComponentXO allOldComponent = new ComponentXO();
        allOldComponent.setRepository("test-repo");
        allOldComponent.setName("old-component");
        
        AssetXO oldAsset1 = new AssetXO();
        oldAsset1.setBlobCreated(OffsetDateTime.now().minusDays(60));
        
        AssetXO oldAsset2 = new AssetXO();
        oldAsset2.setBlobCreated(OffsetDateTime.now().minusDays(45));
        
        allOldComponent.setAssets(List.of(oldAsset1, oldAsset2));

        // Should match because ALL assets are old
        assertThat(filter.getComponentFilter().test(allOldComponent)).isTrue();
    }

    private ComponentXO createComponent(String repository, String group, String name, String version,
                                        OffsetDateTime blobCreated, OffsetDateTime lastDownloaded) {
        ComponentXO component = new ComponentXO();
        component.setRepository(repository);
        component.setGroup(group);
        component.setName(name);
        component.setVersion(version);
        
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(blobCreated);
        asset.setLastDownloaded(lastDownloaded);
        
        component.setAssets(List.of(asset));
        return component;
    }
}