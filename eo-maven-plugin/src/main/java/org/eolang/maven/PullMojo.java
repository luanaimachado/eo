/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2024 Objectionary.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eolang.maven;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cactoos.text.TextOf;
import org.eolang.maven.footprint.CachePath;
import org.eolang.maven.footprint.Footprint;
import org.eolang.maven.footprint.FpFork;
import org.eolang.maven.footprint.FpGenerated;
import org.eolang.maven.footprint.FpIfReleased;
import org.eolang.maven.footprint.FpIfTargetExists;
import org.eolang.maven.footprint.FpIgnore;
import org.eolang.maven.footprint.FpUpdateBoth;
import org.eolang.maven.footprint.FpUpdateFromCache;
import org.eolang.maven.hash.ChCached;
import org.eolang.maven.hash.ChNarrow;
import org.eolang.maven.hash.ChRemote;
import org.eolang.maven.hash.CommitHash;
import org.eolang.maven.name.ObjectName;
import org.eolang.maven.name.OnCached;
import org.eolang.maven.name.OnSwap;
import org.eolang.maven.name.OnVersioned;
import org.eolang.maven.objectionary.Objectionaries;
import org.eolang.maven.objectionary.ObjsDefault;
import org.eolang.maven.tojos.ForeignTojo;

/**
 * Pull EO files from Objectionary.
 * @since 0.1
 */
@Mojo(
    name = "pull",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    threadSafe = true
)
public final class PullMojo extends SafeMojo {
    /**
     * The directory where to process to.
     */
    public static final String DIR = "4-pull";

    /**
     * Cache directory.
     */
    public static final String CACHE = "pulled";

    /**
     * The Git tag to pull objects from, in objectionary.
     * @since 0.21.0
     */
    @SuppressWarnings("PMD.ImmutableField")
    @Parameter(property = "eo.tag", required = true, defaultValue = "master")
    private String tag = "master";

    /**
     * The Git hash to pull objects from, in objectionary.
     * If not set, will be computed from {@code tag} field.
     * @since 0.29.6
     */
    @SuppressWarnings("PMD.ImmutableField")
    private CommitHash hash;

    /**
     * Objectionaries.
     * @checkstyle MemberNameCheck (5 lines)
     */
    private final Objectionaries objectionaries = new ObjsDefault();

    /**
     * Pull again even if the .eo file is already present?
     * @since 0.10.0
     * @checkstyle MemberNameCheck (7 lines)
     */
    @Parameter(property = "eo.overWrite", required = true, defaultValue = "false")
    private boolean overWrite;

    @Override
    @SuppressWarnings("PMD.PrematureDeclaration")
    public void exec() throws IOException {
        if (this.offline) {
            Logger.info(
                this,
                "No programs were pulled because eo.offline flag is TRUE"
            );
        } else {
            final long start = System.currentTimeMillis();
            if (this.hash == null) {
                this.hash = new ChCached(
                    new ChNarrow(
                        new ChRemote(this.tag)
                    )
                );
            }
            final Collection<ForeignTojo> tojos = this.scopedTojos().withoutSources();
            final Collection<ObjectName> names = new ArrayList<>(0);
            final Path base = this.targetDir.toPath().resolve(PullMojo.DIR);
            final String hsh = this.hash.value();
            for (final ForeignTojo tojo : tojos) {
                final ObjectName object = new OnCached(
                    new OnSwap(
                        this.withVersions,
                        new OnVersioned(tojo.identifier(), hsh)
                    )
                );
                try {
                    tojo.withSource(this.pulled(object, base, hsh))
                        .withHash(new ChNarrow(this.hash));
                } catch (final IOException exception) {
                    throw new IOException(
                        String.format(
                            "Failed to pull object '%s' discovered at %s",
                            tojo.identifier(),
                            tojo.discoveredAt()
                        ),
                        exception
                    );
                }
                names.add(object);
            }
            Logger.info(
                this,
                "%d program(s) were pulled in %[ms]s: %s",
                tojos.size(),
                System.currentTimeMillis() - start,
                names
            );
        }
    }

    /**
     * Pull one object.
     * @param object Name of the object with/without version, e.g. "org.eolang.io.stdout|5f82cc1"
     * @param base Base cache path
     * @param hsh Git hash
     * @return The path of .eo file
     * @throws IOException If fails
     */
    private Path pulled(final ObjectName object, final Path base, final String hsh)
        throws IOException {
        final String semver = this.plugin.getVersion();
        final Path target = new Place(object).make(base, AssembleMojo.EO);
        final Supplier<Path> che = new CachePath(
            this.cache.toPath().resolve(PullMojo.CACHE),
            semver,
            hsh,
            base.relativize(target)
        );
        final Footprint generated = new FpGenerated(
            src -> {
                Logger.debug(
                    this,
                    "Pulling %s object from remote objectionary with hash %s",
                    object, hsh
                );
                return new TextOf(
                    this.objectionaries.object(object)
                ).asString();
            }
        );
        return new FpIfTargetExists(
            new FpFork(
                (src, tgt) -> {
                    final boolean rewrite = this.overWrite;
                    if (rewrite) {
                        Logger.debug(
                            this,
                            "Pulling sources again since eo.overWrite=TRUE"
                        );
                    }
                    return rewrite;
                },
                new FpIfReleased(
                    semver,
                    hsh,
                    new FpUpdateBoth(generated, che),
                    generated
                ),
                new FpIgnore()
            ),
            new FpIfReleased(
                semver,
                hsh,
                new FpIfTargetExists(
                    tgt -> che.get(),
                    new FpUpdateFromCache(che),
                    new FpUpdateBoth(generated, che)
                ),
                generated
            )
        ).apply(Paths.get(""), target);
    }
}