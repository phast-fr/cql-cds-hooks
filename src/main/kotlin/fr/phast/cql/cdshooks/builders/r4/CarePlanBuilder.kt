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

class CarePlanBuilder(carePlan: CarePlan): BaseBuilder<CarePlan>(carePlan) {

    constructor(subject: Reference): this(CarePlan(RequestStatus.DRAFT, CarePlanIntent.PROPOSAL, subject))

    fun buildIdentifier(identifier: List<Identifier>): CarePlanBuilder {
        complexProperty.identifier = identifier
        return this
    }

    fun buildIdentifier(identifier: Identifier): CarePlanBuilder {
        if (complexProperty.identifier == null) {
            complexProperty.identifier = mutableListOf()
        }
        (complexProperty.identifier as MutableList<Identifier>).add(identifier)
        return this
    }

    fun buildInstantiatesCanonical(references: List<CanonicalType>): CarePlanBuilder {
        complexProperty.instantiatesCanonical = references
        return this
    }

    fun buildInstantiatesCanonical(reference: CanonicalType): CarePlanBuilder {
        if (complexProperty.instantiatesCanonical == null) {
            complexProperty.instantiatesCanonical = mutableListOf()
        }
        (complexProperty.instantiatesCanonical as MutableList<CanonicalType>).add(reference)
        return this
    }

    fun buildBasedOn(references: List<Reference>): CarePlanBuilder {
        complexProperty.basedOn = references
        return this
    }

    fun buildBasedOn(reference: Reference): CarePlanBuilder {
        if (complexProperty.basedOn == null) {
            complexProperty.basedOn = mutableListOf()
        }
        (complexProperty.basedOn as MutableList<Reference>).add(reference)
        return this
    }

    fun buildReplaces(references: List<Reference>): CarePlanBuilder {
        complexProperty.replaces = references
        return this
    }

    fun buildReplaces(reference: Reference): CarePlanBuilder {
        if (complexProperty.replaces == null) {
            complexProperty.replaces = mutableListOf()
        }
        (complexProperty.replaces as MutableList<Reference>).add(reference)
        return this
    }

    fun buildPartOf(references: List<Reference>): CarePlanBuilder {
        complexProperty.partOf = references
        return this
    }

    fun buildPartOf(reference: Reference): CarePlanBuilder {
        if (complexProperty.partOf == null) {
            complexProperty.partOf = mutableListOf()
        }
        (complexProperty.partOf as MutableList<Reference>).add(reference)
        return this
    }

    fun buildCategory(categories: List<CodeableConcept>): CarePlanBuilder {
        complexProperty.category = categories
        return this
    }

    fun buildCategory(category: CodeableConcept): CarePlanBuilder {
        if (complexProperty.category == null) {
            complexProperty.category = mutableListOf()
        }
        (complexProperty.category as MutableList<CodeableConcept>).add(category)
        return this
    }

    fun buildTitle(title: StringType): CarePlanBuilder {
        complexProperty.title = title
        return this
    }

    fun buildDescription(description: StringType): CarePlanBuilder {
        complexProperty.description = description
        return this
    }

    fun buildEncounter(reference: Reference): CarePlanBuilder {
        complexProperty.encounter = reference
        return this
    }

    fun buildPeriod(period: Period): CarePlanBuilder {
        complexProperty.period = period
        return this
    }

    fun buildAuthor(reference: Reference): CarePlanBuilder {
        complexProperty.author = reference
        return this
    }

    fun buildCareTeam(careTeams: List<Reference>): CarePlanBuilder {
        complexProperty.careTeam = careTeams
        return this
    }

    fun buildCareTeam(careTeam: Reference): CarePlanBuilder {
        if (complexProperty.careTeam == null) {
            complexProperty.careTeam = mutableListOf()
        }
        (complexProperty.careTeam as MutableList<Reference>).add(careTeam)
        return this
    }

    fun buildAddresses(addresses: List<Reference>): CarePlanBuilder {
        complexProperty.addresses = addresses
        return this
    }

    fun buildAddresses(address: Reference): CarePlanBuilder {
        if (complexProperty.addresses == null) {
            complexProperty.addresses = mutableListOf()
        }
        (complexProperty.addresses as MutableList<Reference>).add(address)
        return this
    }

    fun buildSupportingInfo(supportingInfo: List<Reference>): CarePlanBuilder {
        complexProperty.supportingInfo = supportingInfo
        return this
    }

    fun buildSupportingInfo(supportingInfo: Reference): CarePlanBuilder {
        if (complexProperty.supportingInfo == null) {
            complexProperty.supportingInfo = mutableListOf()
        }
        (complexProperty.supportingInfo as MutableList<Reference>).add(supportingInfo)
        return this
    }

    fun buildGoal(goals: List<Reference>): CarePlanBuilder {
        complexProperty.goal = goals
        return this
    }

    fun buildGoal(goal: Reference): CarePlanBuilder {
        if (complexProperty.goal == null) {
            complexProperty.goal = mutableListOf()
        }
        (complexProperty.goal as MutableList<Reference>).add(goal)
        return this
    }

    fun buildActivity(activities: List<CarePlanActivity>): CarePlanBuilder {
        complexProperty.activity = activities
        return this
    }

    fun buildActivity(activity: CarePlanActivity): CarePlanBuilder {
        if (complexProperty.activity == null) {
            complexProperty.activity = mutableListOf()
        }
        (complexProperty.activity as MutableList<CarePlanActivity>).add(activity)
        return this
    }

    fun buildNotes(notes: List<Annotation>): CarePlanBuilder {
        complexProperty.note = notes
        return this
    }

    fun buildNotes(note: Annotation): CarePlanBuilder {
        if (complexProperty.note == null) {
            complexProperty.note = mutableListOf()
        }
        (complexProperty.note as MutableList<Annotation>).add(note)
        return this
    }

    fun buildLanguage(language: CodeType): CarePlanBuilder {
        complexProperty.language = language
        return this
    }

    fun buildContained(result: Resource): CarePlanBuilder {
        if (complexProperty.contained == null) {
            complexProperty.contained = mutableListOf()
        }
        (complexProperty.contained as MutableList<Resource>).add(result)
        return this
    }
}
