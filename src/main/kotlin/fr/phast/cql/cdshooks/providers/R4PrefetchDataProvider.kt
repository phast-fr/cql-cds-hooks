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

import fr.phast.cql.cdshooks.helpers.PrefetchDataProviderHelper
import org.hl7.fhir.r4.model.Resource
import org.opencds.cqf.cql.engine.model.ModelResolver
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Interval
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo

class R4PrefetchDataProvider(resources: List<Resource>, private val resolver: ModelResolver): TerminologyAwareRetrieveProvider() {

    private val prefetchResources = PrefetchDataProviderHelper.populateMap(resources)

    override fun retrieve(
        context: String?,
        contextPath: String?,
        contextValue: Any?,
        dataType: String?,
        templateId: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
        datePath: String?,
        dateLowPath: String?,
        dateHighPath: String?,
        dateRange: Interval?
    ): Iterable<Any>? {
        require(!(codePath == null && (codes != null || valueSet != null))) { "A code path must be provided when filtering on codes or a valueset." }
        requireNotNull(dataType) { "A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval" }

        // This dataType can't be related to patient, therefore may
        // not be in the pre-fetch bundle, or might required a lookup by Id
        if (context == "Patient" && contextPath == null) {
            return null
        }

        val resourcesOfType = prefetchResources[dataType] ?: return emptyList()

        // no resources or no filtering -> return list
        if (resourcesOfType.isEmpty() || dateRange == null && codePath == null) {
            return resourcesOfType
        }

        val returnList = mutableListOf<Any>()
        var includeResource = true
        resourcesOfType.forEach { resource ->
            if (codePath != null && codePath != "") {
                var codesLoc = codes
                if (valueSet != null && terminologyProvider != null) {
                    var valueSetLoc = valueSet
                    if (valueSet.startsWith("urn:oid:")) {
                        valueSetLoc = valueSet.replace("urn:oid:", "")
                    }
                    val valueSetInfo = ValueSetInfo().withId(valueSetLoc)
                    codesLoc = terminologyProvider.expand(valueSetInfo)
                }
                if (codesLoc != null) {
                    val value = resolver.resolvePath(resource, codePath)
                    if (value != null) {
                        val codeObject = PrefetchDataProviderHelper.getR4Code(value)
                        includeResource = PrefetchDataProviderHelper.checkCodeMembership(codesLoc, codeObject)
                    }
                    else {
                        includeResource = false
                    }
                }
            }

            if (includeResource) {
                returnList.add(resource)
            }
        }
        return returnList
    }
}
