package com.pyx4j.nxrm.cleanup;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import picocli.CommandLine.IVersionProvider;

class ManifestVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        Enumeration<URL> resources = this.getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                Manifest manifest = new Manifest(url.openStream());
                if (isApplicableManifest(manifest)) {
                    Attributes attr = manifest.getMainAttributes();
                    return new String[]{
                            get(attr, "Implementation-Title") + " version " + get(attr, "Implementation-Version")
                    };
                }
            } catch (IOException ex) {
                return new String[]{"Unable to read from " + url + ": " + ex};
            }
        }
        return new String[0];
    }

    private boolean isApplicableManifest(Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        return "nexus-repository-cleanup".equals(get(attributes, "Implementation-Title"));
    }

    private static Object get(Attributes attributes, String key) {
        return attributes.get(new Attributes.Name(key));
    }
}
