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

package fr.phast.cql.cdshooks.builders.r4

import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.model.Annotation

class CarePlanActivityBuilder(): BaseBuilder<CarePlanActivity>(CarePlanActivity()) {

    fun buildOutcomeConcept(concepts: List<CodeableConcept>): CarePlanActivityBuilder {
        complexProperty.outcomeCodeableConcept = concepts
        return this
    }

    fun buildOutcomeConcept(concept: CodeableConcept): CarePlanActivityBuilder {
        if (complexProperty.outcomeCodeableConcept == null) {
            complexProperty.outcomeCodeableConcept = mutableListOf()
        }
        (complexProperty.outcomeCodeableConcept as MutableList<CodeableConcept>).add(concept)
        return this
    }

    fun buildOutcomeReference(references: List<Reference>): CarePlanActivityBuilder {
        complexProperty.outcomeReference = references
        return this
    }

    fun buildOutcomeReference(reference: Reference): CarePlanActivityBuilder {
        if (complexProperty.outcomeReference == null) {
            complexProperty.outcomeReference = mutableListOf()
        }
        (complexProperty.outcomeReference as MutableList<Reference>).add(reference)
        return this
    }

    fun buildProgress(annotations: List<Annotation>): CarePlanActivityBuilder {
        complexProperty.progress = annotations
        return this
    }

    fun buildProgress(annotation: Annotation): CarePlanActivityBuilder {
        if (complexProperty.progress == null) {
            complexProperty.progress = mutableListOf()
        }
        (complexProperty.progress as MutableList<Annotation>).add(annotation)
        return this
    }

    fun buildReference(reference: Reference): CarePlanActivityBuilder {
        complexProperty.reference = reference
        return this
    }

    fun buildReferenceTarget(resource: Resource): CarePlanActivityBuilder {
        complexProperty.referenceTarget = resource
        return this
    }

    fun buildDetail(detail: CarePlanDetail): CarePlanActivityBuilder {
        complexProperty.detail = detail
        return this
    }
}
