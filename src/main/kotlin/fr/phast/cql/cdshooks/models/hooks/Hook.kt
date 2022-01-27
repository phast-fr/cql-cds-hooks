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

package fr.phast.cql.cdshooks.models.hooks

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "hook"
)
@JsonSubTypes(
    JsonSubTypes.Type(
        value = PatientViewHook::class,
        name = "patient-view"
    ),
    JsonSubTypes.Type(
        value = AppointmentBookHook::class,
        name = "appointment-book"
    ),
    JsonSubTypes.Type(
        value = EncounterDischargeHook::class,
        name = "encounter-discharge"
    ),
    JsonSubTypes.Type(
        value = EncounterStartHook::class,
        name = "encounter-start"
    ),
    JsonSubTypes.Type(
        value = OrderSelectHook::class,
        name = "order-select"
    ),
    JsonSubTypes.Type(
        value = OrderSignHook::class,
        name = "order-sign"
    )
)
abstract class Hook(val hook: String,
                    val hookInstance: String,
                    open val context: HookContext) {
    var fhirServer: String? = null
    var fhirAuthorization: Authorization? = null
    var prefetch: LinkedHashMap<String, Resource>? = null
}

data class Authorization(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val scope: String,
    val subject: String
)

abstract class HookContext(val userId: String,
                           val patientId: String) {
    var encounterId: String? = null
}

/**
 * Hook: patient-view
 * specificationVersion	1.0
 * hookVersion	1.0
 * Hook maturity	4 - Documented
 */
class PatientViewHook(hookInstance: String,
                      override val context: PatientViewContext
): Hook("patient-view", hookInstance, context) {
    class PatientViewContext(
        userId: String,
        patientId: String): HookContext(userId, patientId)
}

/**
 * Hook: appointment-book
 * specificationVersion	1.0
 * hookVersion	1.0
 * hookMaturity	1 - Submitted
 * standardsStatus	draft
 * publicationStatus	snapshot
 */
class AppointmentBookHook(hookInstance: String,
                          override val context: AppointmentBookContext
): Hook("appointment-book", hookInstance, context) {
    class AppointmentBookContext(
        userId: String,
        patientId: String,
        val appointment: Bundle
    ): HookContext(userId, patientId)
}

/**
 * Hook: encounter-discharge
 * specificationVersion	1.0
 * hookVersion	1.0
 * hookMaturity	1 - Submitted
 * standardsStatus	draft
 * publicationStatus	snapshot
 */
class EncounterDischargeHook(hookInstance: String,
                             override val context: EncounterDischargeContext
): Hook("encounter-discharge", hookInstance, context) {
    class EncounterDischargeContext(
        userId: String,
        patientId: String): HookContext(userId, patientId)
}

/**
 * Hook: encounter-start
 * specificationVersion	1.0
 * hookVersion	1.0
 * hookMaturity	1 - Submitted
 * standardsStatus	draft
 * publicationStatus	snapshot
 */
class EncounterStartHook(hookInstance: String,
                         override val context: EncounterStartContext
): Hook("encounter-start", hookInstance, context) {
    class EncounterStartContext(
        userId: String,
        patientId: String): HookContext(userId, patientId)
}

/**
 * Hook: order-select
 * specificationVersion	1.0
 * hookVersion	1.0
 * hookMaturity	1 - Submitted
 * standardsStatus	draft
 * publicationStatus	snapshot
 */
class OrderSelectHook(hookInstance: String,
                      override val context: OrderSelectContext
): Hook("order-select", hookInstance, context) {
    class OrderSelectContext(
        userId: String,
        patientId: String,
        val selections: List<String>,
        val draftOrders: Bundle) : HookContext(userId, patientId) {
    }
}

/**
 * Hook: order-sign
 * specificationVersion	1.0
 * hookVersion	1.0
 * hookMaturity	1 - Submitted
 * standardsStatus	draft
 * publicationStatus	snapshot
 */
class OrderSignHook(hookInstance: String,
                    override val context: OrderSignContext
): Hook("order-sign", hookInstance, context) {
    class OrderSignContext(
        userId: String,
        patientId: String,
        val draftOrders: Bundle): HookContext(userId, patientId)
}
