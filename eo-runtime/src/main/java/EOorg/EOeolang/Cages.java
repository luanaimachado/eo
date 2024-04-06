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

/*
 * @checkstyle PackageNameCheck (4 lines)
 */
package EOorg.EOeolang;

import java.util.concurrent.ConcurrentHashMap;
import org.eolang.Phi;
import org.eolang.Versionized;

/**
 * Cages for objects.
 * @since 0.36.0
 */
@Versionized
final class Cages {
    /**
     * Cages instance.
     */
    static final ThreadLocal<Cages> INSTANCE = ThreadLocal.withInitial(Cages::new);

    /**
     * Encaged objects.
     */
    private final ConcurrentHashMap<Integer, Phi> objects = new ConcurrentHashMap<>(0);


    /**
     * Ctor.
     */
    private Cages() {
        // singleton :(
    }

    /**
     * Encage object for the first time.
     * When object is encaged - locator will be generated.
     * @param object Object ot encage
     * @return Locator to the object in cage
     */
    int encage(final Phi object) {
        final int locator = object.hashCode();
        if (this.objects.containsKey(locator)) {

        }
        this.objects.put(locator, object);
        return locator;
    }

    /**
     * Encage object by locator.
     * @param locator Locator to the object in cage
     * @param object Object to encage
     */
    void encage(final int locator, final Phi object) {
        if (!this.objects.containsKey(locator)) {

        }
        final Phi current = this.objects.get(locator);
        if (!current.forma().equals(object.forma())) {

        }
        this.objects.put(locator, object);
    }

    /**
     * Get object from cage by locator.
     * @param locator The locator of the object
     * @return Object
     */
    Phi get(final int locator) {
        if (!this.objects.containsKey(locator)) {

        }
        return this.objects.get(locator);
    }

    /**
     * Remove object from the cage by locator.
     * @param locator Locator of the object
     */
    void remove(final int locator) {
        if (!this.objects.containsKey(locator)) {

        }
        this.objects.remove(locator);
    }
}
