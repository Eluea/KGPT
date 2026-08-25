package tn.eluea.kgpt.apk;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkStructureTest {

    @Test
    public void testReleaseApkContainsModernXposedMetaInf() throws Exception {
        // Must match outputFileName in app/build.gradle ("${appName}-${buildType}-v${versionName}.apk")
        File apkFile = new File("build/outputs/apk/release/KGPT-release-v4.1.5.apk");
        // The release APK only exists after an assembled+signed release build;
        // skip silently on machines where it hasn't been produced instead of
        // hard-failing every unit test run.
        Assume.assumeTrue("Release APK not built yet — skipping", apkFile.exists());

        try (ZipFile zip = new ZipFile(apkFile)) {
            // Check java_init.list
            ZipEntry javaInit = zip.getEntry("META-INF/xposed/java_init.list");
            Assert.assertNotNull("META-INF/xposed/java_init.list must be present in APK", javaInit);

            try (InputStream is = zip.getInputStream(javaInit);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                Assert.assertEquals("tn.eluea.kgpt.MainHook", line != null ? line.trim() : null);
            }

            // Check module.prop
            ZipEntry moduleProp = zip.getEntry("META-INF/xposed/module.prop");
            Assert.assertNotNull("META-INF/xposed/module.prop must be present in APK", moduleProp);

            try (InputStream is = zip.getInputStream(moduleProp);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String content = sb.toString();
                Assert.assertTrue("module.prop must specify targetApiVersion=102", content.contains("targetApiVersion=102"));
                Assert.assertTrue("module.prop must specify minApiVersion=100", content.contains("minApiVersion=100"));
            }

            // Check dex files
            ZipEntry classesDex = zip.getEntry("classes.dex");
            Assert.assertNotNull("classes.dex must be present in APK", classesDex);
        }
    }
}
