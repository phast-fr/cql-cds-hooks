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

package fr.phast.cql.cdshooks.resolvers

import fr.phast.cql.cdshooks.models.services.Services
import org.hl7.fhir.r4.client.rest.RestClient
import org.hl7.fhir.r4.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

class DiscoveryResolution(private val client: RestClient) {
    var maxUriLength = DEFAULT_MAX_URI_LENGTH
        set(maxUriLength) {
            require(maxUriLength > 0) { "maxUriLength must be >0" }
            field = maxUriLength
        }

    fun resolvePlanDefinition(entry: BundleEntry): PlanDefinition? {
        return if (entry.resource is PlanDefinition) {
            entry.resource as PlanDefinition
        } else null
    }

    fun isEca(planDefinition: PlanDefinition): Boolean {
        for (coding in planDefinition.type?.coding!!) {
            if (coding.code?.value == "eca-rule") {
                return true
            }
        }
        return false
    }

    fun resolvePrimaryLibrary(planDefinition: PlanDefinition): Library? {
        // Assuming 1 library
        // TODO: enhance to handle multiple libraries - need a way to identify primary library
        if (planDefinition.library?.isNotEmpty() == true) {
            return resolveLibrary(planDefinition.library!![0].value)
        }
        return null
    }

    fun resolveLibrary(referenceId: String): Library? {
        val response = client.read(Library::class.java)
            .resourceId(referenceId)
            .execute()
            .block()
        return response?.body
    }

    fun resolveValueCodingCodes(valueCodings: List<Coding>): List<String> {
        val result = mutableListOf<String>()
        var codes = StringBuilder()
        valueCodings.forEach { coding ->
            if (coding.code != null) {
                val system = coding.system?.value
                val code = coding.code?.value
                codes = code?.let { getCodesStringBuilder(result, codes, system, it) }!!
            }
        }
        result.add(codes.toString())
        return result
    }

    fun resolveValueSetCodes(valueSetUrl: CanonicalType): List<String> {
        val result = mutableListOf<String>()
        client.operation(ValueSet::class.java)
            .withResourceType("ValueSet")
            .withUrl(valueSetUrl)
            .operationName("\$expand")
            .execute()
            .block()
            ?.run {
                if (this.statusCode == HttpStatus.OK) {
                    var codes = StringBuilder()
                    val valueSet = this.body
                    if (valueSet?.expansion?.contains != null) {
                        valueSet.expansion?.contains?.forEach { contains ->
                            val system = contains.system?.value
                            val code = contains.code?.value
                            codes = code?.let { getCodesStringBuilder(result, codes, system, it) }!!
                        }
                    }
                    else if (valueSet?.compose?.include != null) {
                        valueSet.compose!!.include.forEach { concepts ->
                            val system = concepts.system?.value
                            if (concepts.concept != null) {
                                for (concept in concepts.concept!!) {
                                    val code = concept.code.value
                                    codes = getCodesStringBuilder(result, codes, system, code)
                                }
                            }
                        }
                    }
                    result.add(codes.toString())
                }
            }
        return result
    }

    private fun getCodesStringBuilder(
        ret: MutableList<String>,
        codes: StringBuilder,
        system: String?,
        code: String
    ): StringBuilder {
        var lCodes = codes
        val codeToken = "$system|$code"
        val postAppendLength = lCodes.length + codeToken.length
        if (lCodes.isNotEmpty() && postAppendLength < maxUriLength) {
            lCodes.append(",")
        }
        else if (postAppendLength > maxUriLength) {
            ret.add(codes.toString())
            lCodes = StringBuilder()
        }
        lCodes.append(codeToken)
        return lCodes
    }

    fun createRequestUrl(dataRequirement: DataRequirement): List<String> {
        val ret = mutableListOf<String>()
        if (!isPatientCompartment(dataRequirement.type.value)) {
            return ret
        }
        val patientRelatedResource =
            "${dataRequirement.type.value}?${getPatientSearchParam(dataRequirement.type.value)}=Patient/$PATIENT_ID_CONTEXT"
        return if (dataRequirement.codeFilter != null) {
            dataRequirement.codeFilter?.forEach { codeFilterComponent ->
                if (codeFilterComponent.path != null) {
                    val path = mapCodePathToSearchParam(dataRequirement.type.value, codeFilterComponent.path!!.value)
                    if (codeFilterComponent.valueSet != null) {
                        resolveValueSetCodes(codeFilterComponent.valueSet!!).forEach { codes ->
                            ret.add("$patientRelatedResource&$path=$codes")
                        }
                    }
                    else if (codeFilterComponent.code != null) {
                        val codeFilterValueCodings = codeFilterComponent.code
                        if (codeFilterValueCodings != null) {
                            var isFirstCodingInFilter = true
                            resolveValueCodingCodes(codeFilterValueCodings).forEach { code ->
                                if (isFirstCodingInFilter) {
                                    ret.add("$patientRelatedResource&$path=$code")
                                } else {
                                    ret.add(",$code")
                                }
                                isFirstCodingInFilter = false
                            }
                        }
                    }
                }
            }
            ret
        }
        else {
            ret.add(patientRelatedResource)
            ret
        }
    }

    private fun getPrefetchUrlList(planDefinition: PlanDefinition): PrefetchUrlList {
        val prefetchList = PrefetchUrlList()
        val library = resolvePrimaryLibrary(planDefinition)

        if (!isEca(planDefinition)) {
            return prefetchList
        }

        library?.let {
            // TODO: resolve data requirements
            if (library.dataRequirement == null) {
                return prefetchList
            }

            library.dataRequirement?.forEach { requirement ->
                prefetchList.addAll(createRequestUrl(requirement))
            }
        }
        return prefetchList
    }

    fun resolve(): Services {
        val response = client.search()
             .withResourceType("PlanDefinition")
             .execute()
             .block()

        val serviceList = response?.body?.entry?.map { entry ->
            resolvePlanDefinition(entry)?.let { planDefinition ->
                resolve(planDefinition)
            }
        }
        if (serviceList != null) {
            return Services(serviceList)
        }
        return Services(listOf())
    }

    private fun resolve(planDefinition: PlanDefinition): Services.Service? {
        var hook: String? = null

        if (planDefinition.action?.isNotEmpty() == true) {
            // TODO - this needs some work - too naive
            if (planDefinition.action?.get(0)?.trigger != null) {
                if (planDefinition.action?.get(0)?.trigger?.get(0)?.name?.value != null) {
                    hook = planDefinition.action?.get(0)?.trigger?.get(0)?.name!!.value
                }
            }
        }

        val name = planDefinition.name?.value
        val title = planDefinition.title?.value
        val description = planDefinition.description?.value
        val id = planDefinition.id?.value

        logger.info("Mapping PlanDefinition to Hook: $hook")

        val prefetch = mutableMapOf("item1" to "Patient?_id={{context.patientId}}")
        var itemNo = 1
        getPrefetchUrlList(planDefinition).forEach {
            prefetch["item" + (++itemNo).toString()] = it
        }

        if (hook != null && name != null && id != null) {
            return Services.Service(id, hook, name, title, description, prefetch)
        }
        return null
    }

    private fun mapCodePathToSearchParam(dataType: String, path: String): String {
        when (dataType) {
            "MedicationAdministration" -> if (path == "medication") return "code"
            "MedicationDispense" -> if (path == "medication") return "code"
            "MedicationRequest" -> if (path == "medication") return "code"
            "MedicationStatement" -> if (path == "medication") return "code"
            else -> if (path == "vaccineCode") return "vaccine-code"
        }
        return path.replace('.', '-').lowercase()
    }

    fun isPatientCompartment(dataType: String?): Boolean {
        return when (dataType) {
            "Account", "AdverseEvent", "AllergyIntolerance", "Appointment", "AppointmentResponse", "AuditEvent",
            "Basic", "BodyStructure", "CarePlan", "CareTeam", "ChargeItem", "Claim", "ClaimResponse",
            "ClinicalImpression", "Communication", "CommunicationRequest", "Composition", "Condition", "Consent",
            "Coverage", "CoverageEligibilityRequest", "CoverageEligibilityResponse", "DetectedIssue", "DeviceRequest",
            "DeviceUseStatement", "DiagnosticReport", "DocumentManifest", "DocumentReference", "Encounter",
            "EnrollmentRequest", "EpisodeOfCare", "ExplanationOfBenefit", "FamilyMemberHistory", "Flag", "Goal",
            "Group", "ImagingStudy", "Immunization", "ImmunizationEvaluation", "ImmunizationRecommendation", "Invoice",
            "List", "MeasureReport", "Media", "MedicationAdministration", "MedicationDispense", "MedicationRequest",
            "MedicationStatement", "MolecularSequence", "NutritionOrder", "Observation", "Patient", "Person",
            "Procedure", "Provenance", "QuestionnaireResponse", "RelatedPerson", "RequestGroup", "ResearchSubject",
            "RiskAssessment", "Schedule", "ServiceRequest", "Specimen", "SupplyDelivery", "SupplyRequest",
            "VisionPrescription" -> true
            else -> false
        }
    }

    fun getPatientSearchParam(dataType: String?): String? {
        when (dataType) {
            "Account" -> return "subject"
            "AdverseEvent" -> return "subject"
            "AllergyIntolerance" -> return "patient"
            "Appointment" -> return "actor"
            "AppointmentResponse" -> return "actor"
            "AuditEvent" -> return "patient"
            "Basic" -> return "patient"
            "BodyStructure" -> return "patient"
            "CarePlan" -> return "patient"
            "CareTeam" -> return "patient"
            "ChargeItem" -> return "subject"
            "Claim" -> return "patient"
            "ClaimResponse" -> return "patient"
            "ClinicalImpression" -> return "subject"
            "Communication" -> return "subject"
            "CommunicationRequest" -> return "subject"
            "Composition" -> return "subject"
            "Condition" -> return "patient"
            "Consent" -> return "patient"
            "Coverage" -> return "policy-holder"
            "DetectedIssue" -> return "patient"
            "DeviceRequest" -> return "subject"
            "DeviceUseStatement" -> return "subject"
            "DiagnosticReport" -> return "subject"
            "DocumentManifest" -> return "subject"
            "DocumentReference" -> return "subject"
            "Encounter" -> return "patient"
            "EnrollmentRequest" -> return "subject"
            "EpisodeOfCare" -> return "patient"
            "ExplanationOfBenefit" -> return "patient"
            "FamilyMemberHistory" -> return "patient"
            "Flag" -> return "patient"
            "Goal" -> return "patient"
            "Group" -> return "member"
            "ImagingStudy" -> return "patient"
            "Immunization" -> return "patient"
            "ImmunizationRecommendation" -> return "patient"
            "Invoice" -> return "subject"
            "List" -> return "subject"
            "MeasureReport" -> return "patient"
            "Media" -> return "subject"
            "MedicationAdministration" -> return "patient"
            "MedicationDispense" -> return "patient"
            "MedicationRequest" -> return "subject"
            "MedicationStatement" -> return "subject"
            "MolecularSequence" -> return "patient"
            "NutritionOrder" -> return "patient"
            "Observation" -> return "subject"
            "Patient" -> return "_id"
            "Person" -> return "patient"
            "Procedure" -> return "patient"
            "Provenance" -> return "patient"
            "QuestionnaireResponse" -> return "subject"
            "RelatedPerson" -> return "patient"
            "RequestGroup" -> return "subject"
            "ResearchSubject" -> return "individual"
            "RiskAssessment" -> return "subject"
            "Schedule" -> return "actor"
            "ServiceRequest" -> return "patient"
            "Specimen" -> return "subject"
            "SupplyDelivery" -> return "patient"
            "SupplyRequest" -> return "subject"
            "VisionPrescription" -> return "patient"
        }
        return null
    }

    companion object {
        private const val PATIENT_ID_CONTEXT = "{{context.patientId}}"
        private const val DEFAULT_MAX_URI_LENGTH = 8000
        private val logger: Logger = LoggerFactory.getLogger(DiscoveryResolution::class.java)
    }
}
