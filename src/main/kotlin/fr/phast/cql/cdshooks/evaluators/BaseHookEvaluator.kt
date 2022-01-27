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

package fr.phast.cql.cdshooks.evaluators

import fr.phast.cql.cdshooks.evaluations.EvaluationContext
import fr.phast.cql.cdshooks.models.cards.Card
import fr.phast.cql.cdshooks.models.hooks.*
import fr.phast.cql.cdshooks.providers.PriorityRetrieveProvider
import fr.phast.cql.cdshooks.providers.R4PrefetchDataProvider
import org.cqframework.cql.elm.execution.ListTypeSpecifier
import org.hl7.fhir.r4.model.Resource
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.execution.Context
import org.opencds.cqf.cql.engine.model.ModelResolver

abstract class BaseHookEvaluator<R: Resource>(protected val modelResolver: ModelResolver) {

    fun evaluate(context: EvaluationContext<R>): List<Card> {
        // resolve context resources parameter
        // TODO - this will need some work for libraries with multiple parameters
        if (context.getLibrary().parameters != null && context.getHook() !is PatientViewHook) {
            for (params in context.getLibrary().parameters.def) {
                if (params.parameterTypeSpecifier is ListTypeSpecifier) {
                    context.getContext().setParameter(null, params.name, context.getContextResources())
                }
            }
        }

        val prefetchRetriever = R4PrefetchDataProvider(context.getPrefetchResources(), modelResolver).also {
            it.terminologyProvider = context.getContext().resolveTerminologyProvider()
        }

        val contextRetriever = R4PrefetchDataProvider(context.getContextResources(), modelResolver).also {
            it.terminologyProvider = context.getContext().resolveTerminologyProvider()
        }

        context.getContext().registerDataProvider(
            "http://hl7.org/fhir",
            CompositeDataProvider(modelResolver, PriorityRetrieveProvider(contextRetriever, prefetchRetriever))
        )

        return evaluateCdsHooksPlanDefinition(
            context.getContext(), context.getPlanDefinition(), context.getHook().context.patientId
        )
    }

    abstract fun evaluateCdsHooksPlanDefinition(context: Context, planDefinition: R, patientId: String): List<Card>
}
