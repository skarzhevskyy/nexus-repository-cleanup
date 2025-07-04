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
 * Unit tests for ComponentFilter functionality.
 */
class ComponentFilterTest {

    @Test
    void constructor_withEmptyRuleSet_shouldCreateFilter() {
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of());

        ComponentFilter filter = new ComponentFilter(ruleSet);

        assertThat(filter.getComponentFilter()).isNotNull();
    }

    @Test
    void matchesRepositoryFilter_withNoPatterns_shouldReturnTrue() {
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of());
        ComponentFilter filter = new ComponentFilter(ruleSet);

        boolean result = filter.matchesRepositoryFilter("any-repo");

        assertThat(result).isTrue();
    }

    @Test
    void matchesRepositoryFilter_withNullRepositoryName_shouldReturnFalse() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .repositories(List.of("test-repo"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        boolean result = filter.matchesRepositoryFilter(null);

        assertThat(result).isFalse();
    }

    @Test
    void matchesRepositoryFilter_withEmptyRepositoryName_shouldReturnFalse() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .repositories(List.of("test-repo"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        boolean result = filter.matchesRepositoryFilter("");

        assertThat(result).isFalse();
    }

    @Test
    void matchesRepositoryFilter_withMatchingPattern_shouldReturnTrue() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .repositories(List.of("test-*", "prod-*"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        assertThat(filter.matchesRepositoryFilter("test-repo")).isTrue();
        assertThat(filter.matchesRepositoryFilter("prod-maven")).isTrue();
        assertThat(filter.matchesRepositoryFilter("dev-repo")).isFalse();
    }

    @Test
    void matchesRepositoryFilter_withDisabledRule_shouldIgnorePattern() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .enabled(false)
                .repositories(List.of("test-*"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        boolean result = filter.matchesRepositoryFilter("test-repo");

        assertThat(result).isTrue(); // No enabled patterns, so matches all
    }

    @Test
    void getComponentFilter_withNullComponent_shouldReturnFalse() {
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of());
        ComponentFilter filter = new ComponentFilter(ruleSet);

        boolean result = filter.getComponentFilter().test(null);

        assertThat(result).isFalse();
    }

    @Test
    void getComponentFilter_withComponentWithoutAssets_shouldReturnFalse() {
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of());
        ComponentFilter filter = new ComponentFilter(ruleSet);
        ComponentXO component = new ComponentXO();
        component.setAssets(null);

        boolean result = filter.getComponentFilter().test(component);

        assertThat(result).isFalse();
    }

    @Test
    void getComponentFilter_withComponentWithEmptyAssets_shouldReturnFalse() {
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of());
        ComponentFilter filter = new ComponentFilter(ruleSet);
        ComponentXO component = new ComponentXO();
        component.setAssets(List.of());

        boolean result = filter.getComponentFilter().test(component);

        assertThat(result).isFalse();
    }

    @Test
    void getComponentFilter_withDeleteRuleMatching_shouldReturnTrue() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .names(List.of("test-*"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        ComponentXO component = createComponent("test-component");

        boolean result = filter.getComponentFilter().test(component);

        assertThat(result).isTrue();
    }

    @Test
    void getComponentFilter_withKeepRuleMatching_shouldReturnFalse() {
        CleanupRule keepRule = CleanupRuleBuilder.builder()
                .action("keep")
                .names(List.of("test-*"))
                .build();
        CleanupRule deleteRule = CleanupRuleBuilder.builder()
                .action("delete")
                .names(List.of("*"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(keepRule, deleteRule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        ComponentXO component = createComponent("test-component");

        boolean result = filter.getComponentFilter().test(component);

        assertThat(result).isFalse();
    }

    @Test
    void getComponentFilter_withDisabledRule_shouldIgnoreRule() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .enabled(false)
                .action("delete")
                .names(List.of("*"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        ComponentXO component = createComponent("test-component");

        boolean result = filter.getComponentFilter().test(component);

        assertThat(result).isFalse(); // No enabled rules match
    }

    @Test
    void getComponentFilter_withFormatFilter_shouldMatchCorrectFormat() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .formats(List.of("maven2"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        ComponentXO mavenComponent = createComponent("test-component");
        mavenComponent.setFormat("maven2");

        ComponentXO npmComponent = createComponent("test-component");
        npmComponent.setFormat("npm");

        assertThat(filter.getComponentFilter().test(mavenComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(npmComponent)).isFalse();
    }

    @Test
    void getComponentFilter_withVersionFilter_shouldMatchCorrectVersion() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .versions(List.of("1.*"))
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        ComponentXO matchingComponent = createComponent("test-component");
        matchingComponent.setVersion("1.0.0");

        ComponentXO nonMatchingComponent = createComponent("test-component");
        nonMatchingComponent.setVersion("2.0.0");

        assertThat(filter.getComponentFilter().test(matchingComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(nonMatchingComponent)).isFalse();
    }

    @Test
    void getComponentFilter_withUpdatedFilter_shouldMatchOldComponents() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .updated("30d") // 30 days ago
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Component with asset created 60 days ago (should match)
        ComponentXO oldComponent = createComponentWithAssetCreated(OffsetDateTime.now().minusDays(60));

        // Component with asset created 10 days ago (should not match)
        ComponentXO newComponent = createComponentWithAssetCreated(OffsetDateTime.now().minusDays(10));

        assertThat(filter.getComponentFilter().test(oldComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(newComponent)).isFalse();
    }

    @Test
    void getComponentFilter_withNeverDownloadedFilter_shouldMatchNeverDownloadedComponents() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .downloaded("never")
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Component with never downloaded asset
        ComponentXO neverDownloadedComponent = createComponent("test-component");
        AssetXO neverDownloadedAsset = neverDownloadedComponent.getAssets().get(0);
        neverDownloadedAsset.setLastDownloaded(null);

        // Component with downloaded asset
        ComponentXO downloadedComponent = createComponent("test-component");
        AssetXO downloadedAsset = downloadedComponent.getAssets().get(0);
        downloadedAsset.setLastDownloaded(OffsetDateTime.now().minusDays(5));

        assertThat(filter.getComponentFilter().test(neverDownloadedComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(downloadedComponent)).isFalse();
    }

    @Test
    void getComponentFilter_withDownloadedBeforeFilter_shouldMatchOldDownloads() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .downloaded("30d") // 30 days ago
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Component with asset downloaded 60 days ago (should match)
        ComponentXO oldDownloadComponent = createComponent("test-component");
        AssetXO oldAsset = oldDownloadComponent.getAssets().get(0);
        oldAsset.setLastDownloaded(OffsetDateTime.now().minusDays(60));

        // Component with asset downloaded 10 days ago (should not match)
        ComponentXO recentDownloadComponent = createComponent("test-component");
        AssetXO recentAsset = recentDownloadComponent.getAssets().get(0);
        recentAsset.setLastDownloaded(OffsetDateTime.now().minusDays(10));

        // Component with never downloaded asset (should match)
        ComponentXO neverDownloadedComponent = createComponent("test-component");
        AssetXO neverDownloadedAsset = neverDownloadedComponent.getAssets().get(0);
        neverDownloadedAsset.setLastDownloaded(null);

        assertThat(filter.getComponentFilter().test(oldDownloadComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(recentDownloadComponent)).isFalse();
        assertThat(filter.getComponentFilter().test(neverDownloadedComponent)).isTrue();
    }

    @Test
    void getComponentFilter_withMultipleAssets_shouldRequireAllAssetsToMatch() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .updated("30d")
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Component where all assets are old (should match)
        ComponentXO allOldComponent = new ComponentXO();
        allOldComponent.setName("test-component");
        AssetXO oldAsset1 = new AssetXO();
        oldAsset1.setBlobCreated(OffsetDateTime.now().minusDays(60));
        AssetXO oldAsset2 = new AssetXO();
        oldAsset2.setBlobCreated(OffsetDateTime.now().minusDays(45));
        allOldComponent.setAssets(List.of(oldAsset1, oldAsset2));

        // Component where one asset is new (should not match)
        ComponentXO mixedComponent = new ComponentXO();
        mixedComponent.setName("test-component");
        AssetXO oldAsset3 = new AssetXO();
        oldAsset3.setBlobCreated(OffsetDateTime.now().minusDays(60));
        AssetXO newAsset = new AssetXO();
        newAsset.setBlobCreated(OffsetDateTime.now().minusDays(10));
        mixedComponent.setAssets(List.of(oldAsset3, newAsset));

        assertThat(filter.getComponentFilter().test(allOldComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(mixedComponent)).isFalse();
    }

    @Test
    void getComponentFilter_withMultipleFilters_shouldMatchAllFilters() {
        CleanupRule rule = CleanupRuleBuilder.builder()
                .action("delete")
                .names(List.of("test-*"))
                .formats(List.of("maven2"))
                .updated("30d")
                .build();
        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        ComponentFilter filter = new ComponentFilter(ruleSet);

        // Component matching all filters
        ComponentXO matchingComponent = createComponentWithAssetCreated(OffsetDateTime.now().minusDays(60));
        matchingComponent.setName("test-component");
        matchingComponent.setFormat("maven2");

        // Component not matching name filter
        ComponentXO wrongNameComponent = createComponentWithAssetCreated(OffsetDateTime.now().minusDays(60));
        wrongNameComponent.setName("prod-component");
        wrongNameComponent.setFormat("maven2");

        // Component not matching format filter
        ComponentXO wrongFormatComponent = createComponentWithAssetCreated(OffsetDateTime.now().minusDays(60));
        wrongFormatComponent.setName("test-component");
        wrongFormatComponent.setFormat("npm");

        // Component not matching updated filter
        ComponentXO tooRecentComponent = createComponentWithAssetCreated(OffsetDateTime.now().minusDays(10));
        tooRecentComponent.setName("test-component");
        tooRecentComponent.setFormat("maven2");

        assertThat(filter.getComponentFilter().test(matchingComponent)).isTrue();
        assertThat(filter.getComponentFilter().test(wrongNameComponent)).isFalse();
        assertThat(filter.getComponentFilter().test(wrongFormatComponent)).isFalse();
        assertThat(filter.getComponentFilter().test(tooRecentComponent)).isFalse();
    }

    private ComponentXO createComponent(String name) {
        ComponentXO component = new ComponentXO();
        component.setName(name);
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(OffsetDateTime.now().minusDays(1));
        component.setAssets(List.of(asset));
        return component;
    }

    private ComponentXO createComponentWithAssetCreated(OffsetDateTime blobCreated) {
        ComponentXO component = new ComponentXO();
        component.setName("test-component");
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(blobCreated);
        component.setAssets(List.of(asset));
        return component;
    }
}
