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

package fr.phast.cql.cdshooks.helpers

import com.fasterxml.jackson.annotation.JsonSubTypes
import fr.phast.cql.services.helpers.CanonicalHelper
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.cql.engine.runtime.Code

object PrefetchDataProviderHelper {

    fun populateMap(resources: List<Resource>): Map<String, List<Resource>> {
        val prefetchResources = mutableMapOf<String, MutableList<Resource>>()

        val subtypes = mutableListOf<JsonSubTypes.Type>()
        Resource::class.java.annotations.forEach { annotation ->
            if (annotation is JsonSubTypes) {
                subtypes.addAll(annotation.value)
            }
        }

        resources.forEach { resource ->
            when (resource) {
                is MedicationRequest -> {
                    if (resource.medicationReference?.reference?.value != null) {
                        val medicationId = CanonicalHelper.getId(
                            CanonicalType(resource.medicationReference!!.reference?.value!!)
                        )
                        resources.forEach { resourceMed ->
                            if (resourceMed.id?.value == medicationId) {
                                resource.medicationReferenceTarget = resourceMed
                            }
                        }
                    }
                }
            }

            val subtype = subtypes.find { type ->
                type.value.qualifiedName == resource::class.java.canonicalName
            }
            if (subtype != null) {
                if (prefetchResources.containsKey(subtype.name)) {
                    prefetchResources[subtype.name]?.add(resource)
                }
                else {
                    prefetchResources[subtype.name] = mutableListOf(resource)
                }

            }
        }
        return prefetchResources
    }

    fun getR4Code(codeObject: Any): Any {
        return when (codeObject) {
            is CodeType -> codeObject.value
            is Coding -> Code().withSystem(codeObject.system?.value)
                .withCode(codeObject.code?.value)
            is CodeableConcept -> {
                val codes = mutableListOf<Code>()
                codeObject.coding?.forEach { coding ->
                    codes.add(getR4Code(coding) as Code)
                }
                codes
            }
            is Medication -> {
                if (codeObject.code != null) {
                    getR4Code(codeObject.code!!)
                }
                else {
                    codeObject
                }
            }
            else -> codeObject
        }
    }

    fun checkCodeMembership(codes: Iterable<Code>, codeObject: Any?): Boolean {
        if (codeObject != null) {
            val qualifyingCodes = getElmCodesFromObject(codeObject)
            if (qualifyingCodes.isNotEmpty()) {
                for (qualifyingCode in qualifyingCodes) {
                    for (code in codes) {
                        if (qualifyingCode.system == null ||
                            qualifyingCode.system == code.system && qualifyingCode.code != null
                            && qualifyingCode.code == code.code) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun getElmCodesFromObject(`object`: Any): List<Code> {
        val codes = mutableListOf<Code>()
        if (`object` is Iterable<*>) {
            for (innerObject in `object`) {
                val elmCodes = getElmCodesFromObject(
                    innerObject!!
                )
                codes.addAll(elmCodes)
            }
        }
        else {
            val elmCodes = getElmCodesFromObjectInner(`object`)
            codes.addAll(elmCodes)
        }
        return codes
    }

    private fun getElmCodesFromObjectInner(`object`: Any?): List<Code> {
        val codes = mutableListOf<Code>()
        if (`object` == null) {
            return codes
        }

        when (`object`) {
            is CodeableConcept -> {
                val codesFromObject = this.getCodesInConcept(`object`)
                if (codesFromObject != null) {
                    codes.addAll(codesFromObject)
                }
            }
            is Coding -> codes.addAll(generateCodes(listOf(`object`)))
            is Code -> codes.add(`object`)
            else -> throw IllegalArgumentException(
                String.format("Unable to extract codes from object %s", `object`.toString())
            )
        }
        return codes
    }

    private fun getCodesInConcept(`object`: CodeableConcept): List<Code>? {
        return `object`.coding?.let { generateCodes(it) }
    }

    private fun generateCodes(codingObjects: List<Coding>): List<Code> {
        val codes = mutableListOf<Code>()
        codingObjects.forEach { coding ->
            codes.add(Code()
                .withSystem(coding.system?.value)
                .withCode(coding.code?.value)
                .withDisplay(coding.display?.value)
                .withVersion(coding.version?.value))
        }
        return codes
    }
}
