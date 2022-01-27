/*
 * MIT License
 *
 * Copyright (c) 2021 PHAST
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fr.phast.cql.cdshooks.providers

import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Interval

class PriorityRetrieveProvider(vararg providers: RetrieveProvider): RetrieveProvider {

    private val providers: MutableList<RetrieveProvider> = mutableListOf()

    init {
        providers.forEach { provider ->
            this.providers.add(provider)
        }
    }

    override fun retrieve(
        context: String?, contextPath: String?, contextValue: Any?, dataType: String?,
        templateId: String?, codePath: String?, codes: Iterable<Code?>?, valueSet: String?, datePath: String?,
        dateLowPath: String?, dateHighPath: String?, dateRange: Interval?
    ): Iterable<Any?>? {
        var result: Iterable<Any?>? = null
        providers.forEach { provider ->
            result = provider.retrieve(
                context,
                contextPath,
                contextValue,
                dataType,
                templateId,
                codePath,
                codes,
                valueSet,
                datePath,
                dateLowPath,
                dateHighPath,
                dateRange
            )
            if (result != null && result is Collection<*>) {
                if ((result as Collection<*>).isNotEmpty()) {
                    return result
                }
            }
        }
        return result
    }
}
