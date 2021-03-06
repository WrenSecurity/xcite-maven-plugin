/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * If applicable, add the following below this MPL 2.0 HEADER, replacing
 * the fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *     Portions Copyright [yyyy] [name of copyright owner]
 *
 *     Copyright 2014 ForgeRock AS
 *
 */

package org.forgerock.maven.plugins.xcite;

import static org.assertj.core.api.Assertions.*;

import org.forgerock.maven.plugins.xcite.utils.FileUtils;
import org.forgerock.maven.plugins.xcite.utils.StringUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("javadoc")
public class ResolverTest {

    @Test
    public void resolveCitations() throws IOException {
        File outputDir = new File(System.getProperty("java.io.tmpdir"));
        Resolver resolver = new Resolver(outputDir, false, 0, true);

        File sourceDir = new File(getClass().getResource("/").getFile());
        String[] files = { "cite.txt" };
        resolver.resolve(sourceDir, files);

        File resolved = new File(getClass().getResource("/resolved.txt").getFile());
        File expected = File.createTempFile(resolved.getName(), null);
        org.apache.commons.io.FileUtils.copyFile(resolved, expected);

        assertThat(resolved).hasContentEqualTo(expected);

        expected.deleteOnExit();
        new File(System.getProperty("java.io.tmpdir"), "cite.txt").deleteOnExit();
    }

    @Test
    public void resolveCitationRelativePath() throws IOException {
        File outputDir = new File(System.getProperty("java.io.tmpdir"));
        Resolver resolver = new Resolver(outputDir, false, 0, true);

        File resourcesDir = new File(outputDir, "resources");
        resourcesDir.mkdirs();

        File relative = new File(getClass().getResource("/relative.txt").getFile());
        org.apache.commons.io.FileUtils.copyFileToDirectory(relative, resourcesDir);
        File resolved = new File(getClass().getResource("/resolved.txt").getFile());
        org.apache.commons.io.FileUtils.copyFileToDirectory(resolved, resourcesDir);

        String s = File.separator;
        String[] files = { "resources" + s + "relative.txt" };
        resolver.resolve(outputDir, files);

        assertThat(new File(outputDir, "resources" + s + "relative.txt"))
                .hasContentEqualTo(new File(outputDir, "resources" + s + "resolved.txt"));

        org.apache.commons.io.FileUtils.deleteDirectory(resourcesDir);
    }

    @Test
    public void resolveCitationInFile() throws IOException {
        File outputDir = new File(System.getProperty("java.io.tmpdir"));
        Resolver resolver = new Resolver(outputDir, false, 0, true);

        File file = new File(getClass().getResource("/cite.txt").getFile());
        File baseDir = file.getParentFile();
        resolver.resolve(baseDir, file);

        File resolved = new File(getClass().getResource("/resolved.txt").getFile());
        File expected = File.createTempFile(resolved.getName(), null);
        org.apache.commons.io.FileUtils.copyFile(resolved, expected);

        assertThat(resolved).hasContentEqualTo(expected);

        expected.deleteOnExit();
        new File(System.getProperty("java.io.tmpdir"), "cite.txt").deleteOnExit();
    }

    @Test
    public void resolveQuote() throws IOException {
        Resolver resolver = new Resolver(new File("."), false, 0, true);

        File file = new File(getClass().getResource("/cite.txt").getFile());
        String result = resolver.resolve(file, "[file.txt%// To be included]");

        File resolved = new File(getClass().getResource("/resolved.txt").getFile());
        String expected = StringUtils.asString(FileUtils.getStrings(resolved));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void splitLines() {
        Resolver resolver = new Resolver(new File("."), false, 0, false);

        String[] tests = {
            "No citation to match.",        // 0
            "pre [/test:start:end] post",   // 1
            "pre [/test:start] post",       // 2
            "pre [/test] post",             // 3
            "pre [/test]",                  // 4
            "pre [/test:start:end]",        // 5
            "pre [/test:start]",            // 6
            "[/test:start:end] post",       // 7
            "[/test:start] post",           // 8
            "[/test] post",                 // 9
            "[/test:start:end]",            // 10
            "[/test:start]",                // 11
            "[/test] ",                     // 12
            "[/test] [/test]",              // 13
            "[/test][/test]"                // 14
        };

        assertThat(resolver.split(tests[0])).hasSize(1);
        assertThat(resolver.split(tests[1])[1]).isEqualTo("[/test:start:end]");
        assertThat(resolver.split(tests[2])[1]).isEqualTo("[/test:start]");
        assertThat(resolver.split(tests[3])[1]).isEqualTo("[/test]");
        assertThat(resolver.split(tests[4])).hasSize(2);
        assertThat(resolver.split(tests[5])[0]).isEqualTo("pre ");
        assertThat(resolver.split(tests[6])[1]).isEqualTo("[/test:start]");
        assertThat(resolver.split(tests[7])[1]).isEqualTo(" post");
        assertThat(resolver.split(tests[8])[0]).isEqualTo("[/test:start]");
        assertThat(resolver.split(tests[9])[1]).isEqualTo(" post");
        assertThat(resolver.split(tests[10])[0]).isEqualTo("[/test:start:end]");
        assertThat(resolver.split(tests[11])).hasSize(1);
        assertThat(resolver.split(tests[12])).hasSize(2);
        assertThat(resolver.split(tests[13])).hasSize(3);
        assertThat(resolver.split(tests[14])[1]).isEqualTo("[/test]");
    }

    @Test
    public void getQuote() throws IOException {
        Resolver resolver = new Resolver(new File("."), false, 0, true);

        File file = new File(getClass().getResource("/cite.txt").getFile());
        Citation citation = Citation.valueOf("[file.txt:// To be included]");
        String result = resolver.getQuote(file, citation);

        File resolved = new File(getClass().getResource("/resolved.txt").getFile());
        String expected = StringUtils.asString(FileUtils.getStrings(resolved));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getQuoteNonexistentFile() throws IOException {
        Resolver resolver = new Resolver(new File("."), false, 0, true);

        File file = new File(".");
        Citation citation = Citation.valueOf("[does-not-exist.txt]");
        String result = resolver.getQuote(file, citation);

        assertThat(result).isEqualTo("[does-not-exist.txt]");
    }
}
