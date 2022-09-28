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

package fr.phast.cql.cdshooks.evaluators.r4

import fr.phast.cql.cdshooks.builders.r4.*
import fr.phast.cql.cdshooks.converters.R4CarePlanToCdsCard
import fr.phast.cql.cdshooks.evaluators.BaseHookEvaluator
import fr.phast.cql.cdshooks.models.cards.Card
import fr.phast.cql.engine.fhir.model.R4FhirModelResolver
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.cql.engine.execution.Context

class HookEvaluator(modelResolver: R4FhirModelResolver): BaseHookEvaluator<PlanDefinition>(modelResolver) {

    override fun evaluateCdsHooksPlanDefinition(
        context: Context,
        planDefinition: PlanDefinition,
        patientId: String
    ): List<Card> {
        val requestGroupBuilder = RequestGroupBuilder()
        // links
        if (!planDefinition.relatedArtifact.isNullOrEmpty()) {
            val extensions = mutableListOf<Extension>()
            planDefinition.relatedArtifact!!.forEach { relatedArtifact ->
                val attachmentBuilder = AttachmentBuilder()
                if (relatedArtifact.display?.value != null) { // label
                    attachmentBuilder.buildTitle(relatedArtifact.display!!)
                }
                if (relatedArtifact.url?.value != null) { // url
                    attachmentBuilder.buildUrl(relatedArtifact.url!!)
                }
                if (!relatedArtifact.extension.isNullOrEmpty()) { // type
                    attachmentBuilder.buildExtension(relatedArtifact.extension!!)
                }
                val extensionBuilder = ExtensionBuilder("http://example.org")
                extensionBuilder.buildValue(attachmentBuilder.build())
                extensions.add(extensionBuilder.build())
            }
            requestGroupBuilder.buildExtension(extensions)
        }

        resolveActions(
            planDefinition.action!!,
            context,
            patientId,
            requestGroupBuilder,
            mutableListOf()
        )

        val carePlanActivityBuilder = CarePlanActivityBuilder()
        carePlanActivityBuilder.buildReferenceTarget(requestGroupBuilder.build())

        val carePlanBuilder = CarePlanBuilder(Reference().also { it.reference = StringType("Patient/$patientId") })
        carePlanBuilder.buildActivity(carePlanActivityBuilder.build())

        return R4CarePlanToCdsCard.convert(carePlanBuilder.build())
    }

    private fun resolveActions(
        actions: List<PlanDefinitionAction>,
        context: Context,
        patientId: String,
        requestGroupBuilder: RequestGroupBuilder,
        actionComponents: MutableList<RequestGroupAction>
    ) {
        actions.forEach { action ->
            if (action.condition != null) {
                action.condition!!.forEach { condition ->
                    if (condition.kind === ActionConditionKind.APPLICABILITY) {
                        val conditionsMet = if (condition.expression?.expression?.value != null) {
                            context.resolveExpressionRef(condition.expression!!.expression?.value)
                                .expression.evaluate(context) as Boolean?
                        }
                        else {
                            false
                        }
                        if (conditionsMet == true) {
                            val actionBuilder = RequestGroupActionBuilder()
                            if (action.title?.value != null) {
                                actionBuilder.buildTitle(action.title!!)
                            }
                            if (action.description?.value != null) {
                                actionBuilder.buildDescription(action.description!!)
                            }

                            // source
                            if (!action.documentation.isNullOrEmpty()) {
                                val artifact = action.documentation!![0]
                                val artifactBuilder = RelatedArtifactBuilder()
                                if (artifact.display?.value != null) {
                                    artifactBuilder.buildDisplay(artifact.display!!)
                                }
                                if (artifact.url?.value != null) {
                                    artifactBuilder.buildUrl(artifact.url!!)
                                }
                                if (artifact.document != null && artifact.document!!.url?.value != null) {
                                    val attachmentBuilder = AttachmentBuilder()
                                    attachmentBuilder.buildUrl(artifact.document!!.url!!)
                                    artifactBuilder.buildDocument(attachmentBuilder.build())
                                }
                                actionBuilder.buildDocumentation(listOf(artifactBuilder.build()))
                            }

                            // suggestions
                            // TODO - uuid
                            if (action.prefix?.value != null) {
                                actionBuilder.buildPrefix(action.prefix!!)
                            }
                            if (action.type != null) {
                                actionBuilder.buildType(action.type!!)
                            }
                            if (action.selectionBehavior != null) {
                                actionBuilder.buildSelectionBehavior(action.selectionBehavior!!)
                            }
                            var resource: Resource? = null
                            if (action.definitionCanonical?.value != null) {
                                if (action.definitionCanonical!!.value.contains("ActivityDefinition")) {
                                    val inParams = Parameters()
                                    val patientParameter = ParametersParameter(StringType("patient"))
                                    patientParameter.valueString = StringType(patientId)
                                    val parameters = mutableListOf<ParametersParameter>()
                                    parameters.add(patientParameter)
                                    inParams.parameter = parameters
                                    /*val outParams = applyClient.operation()
                                        .onInstance(IdDt(action.definitionCanonical!!.value))
                                        .named("\$apply").withParameters(inParams).useHttpGet().execute()
                                    val response = outParams.parameter
                                    resource = response!![0].resource?.id.setId(UUID.randomUUID().toString())*/
                                }
                            }

                            // Dynamic values populate the RequestGroup - there is a bit of hijacking going
                            // on here...
                            if (!action.dynamicValue.isNullOrEmpty()) {
                                action.dynamicValue!!.forEach { dynamicValue ->
                                    if (dynamicValue.path?.value != null && dynamicValue.expression?.expression?.value != null) {
                                        if (dynamicValue.path?.value!!.endsWith("title")) { // summary
                                            val title = context
                                                .resolveExpressionRef(dynamicValue.expression?.expression?.value!!)
                                                .evaluate(context) as String
                                            actionBuilder.buildTitle(StringType(title))
                                        }
                                        else if (dynamicValue.path?.value!!.endsWith("description")) { // detail
                                            val description = context
                                                .resolveExpressionRef(dynamicValue.expression?.expression?.value)
                                                .evaluate(context) as String
                                            actionBuilder.buildDescription(StringType(description))
                                        }
                                        else if (dynamicValue.path?.value!!.endsWith("extension")) { // indicator
                                            val extension = context
                                                .resolveExpressionRef(dynamicValue.expression?.expression?.value)
                                                .evaluate(context) as String
                                            actionBuilder.buildExtension(StringType(extension))
                                        }
                                        else {
                                            if (resource != null) {
                                                var value = context.resolveExpressionRef(
                                                    dynamicValue.expression!!.expression!!.value
                                                ).evaluate(context)

                                                // TODO need to verify type... yay
                                                if (value is Boolean) {
                                                    value = BooleanType(value)
                                                }

                                                val modelResolver = R4FhirModelResolver()
                                                modelResolver.setValue(resource, dynamicValue.path!!.value, value)

                                                actionBuilder.buildResourceTarget(resource)
                                                actionBuilder.buildResource(
                                                    ReferenceBuilder().buildReference(StringType(resource.id!!.value)).build()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            actionComponents.add(actionBuilder.build())

                            if (!action.action.isNullOrEmpty()) {
                                resolveActions(
                                    action.action!!, context, patientId, requestGroupBuilder, actionComponents
                                )
                            }
                        }
                    }
                }
            }
        }
        requestGroupBuilder.buildAction(actionComponents)
    }
}
