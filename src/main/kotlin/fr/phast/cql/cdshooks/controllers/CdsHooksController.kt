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

package fr.phast.cql.cdshooks.controllers

import fr.phast.cql.cdshooks.configurations.CIOcdsProperties
import fr.phast.cql.cdshooks.evaluations.R4EvaluationContext
import fr.phast.cql.cdshooks.evaluators.r4.HookEvaluator
import fr.phast.cql.cdshooks.models.cards.Card
import fr.phast.cql.cdshooks.models.cards.CdsResponse
import fr.phast.cql.cdshooks.models.cards.IndicatorCode
import fr.phast.cql.cdshooks.models.hooks.Hook
import fr.phast.cql.cdshooks.models.services.Services
import fr.phast.cql.cdshooks.resolvers.DiscoveryResolution
import fr.phast.cql.engine.fhir.helper.FHIRHelpers
import fr.phast.cql.engine.fhir.model.R4FhirModelResolver
import fr.phast.cql.services.LibraryService
import fr.phast.cql.services.helpers.LibraryHelper
import fr.phast.cql.services.providers.LibraryResourceProvider
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.client.rest.RestClient
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.cql.engine.exception.CqlException
import org.opencds.cqf.cql.engine.execution.Context
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.web.bind.annotation.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.ServletException

@RestController
class CdsHooksController(
    ciOcdsProperties: CIOcdsProperties,
    private val terminologyProvider: TerminologyProvider,
    private val libraryService: LibraryService
): HealthIndicator {

    private val client = RestClient(ciOcdsProperties.uri ?: "http://localhost").apply {
        tokenType = "Basic"
        credential = ciOcdsProperties.credential
    }

    private val libraryResolutionProvider = LibraryResourceProvider(
        ciOcdsProperties.uri ?: "http://localhost",
        Library::class.java,
        ciOcdsProperties.credential
    )

    override fun health(): Health {
        return Health.up().build()
    }

    @GetMapping("/cds-services")
    fun doGetCdsServices(): Services {
        return getCdsServices()
    }

    @PostMapping("/cds-services/{planDefinitionId}")
    fun doPostCdsHook(@PathVariable planDefinitionId: String, @RequestBody hook: Hook): CdsResponse {
        logger.info("cds-hooks hook: ${hook.hook}")
        logger.info("cds-hooks hook instance: ${hook.hookInstance}")
        logger.info("cds-hooks PlanDefinition id: $planDefinitionId")

        try {
            val planDefinition = getPlanDefinition(planDefinitionId)
            val planDefinitionHookMatchesRequestHook = AtomicBoolean(false)

            planDefinition!!.action!!.forEach action@{ action ->
                action.trigger!!.forEach trigger@{ trigger ->
                    if (hook.hook == trigger.name?.value) {
                        planDefinitionHookMatchesRequestHook.set(true)
                        return@trigger
                    }
                }
                if (planDefinitionHookMatchesRequestHook.get()) {
                    return@action
                }
            }
            if (!planDefinitionHookMatchesRequestHook.get()) {
                throw ServletException("ERROR: Request hook does not match the service called.")
            }

            val libraryLoader = libraryService.createLibraryLoader(libraryResolutionProvider)
            val library = LibraryHelper.resolvePrimaryLibrary(planDefinition, libraryLoader, libraryResolutionProvider)
            val context = Context(library)

            //context.setDebugMap(LoggingHelper.getDebugMap())

            context.registerTerminologyProvider(terminologyProvider)
            context.registerLibraryLoader(libraryLoader)
            context.setContextValue("Patient",
                hook.context.patientId.replace("Patient/", "")
            )
            context.registerExternalFunctionProvider(
                VersionedIdentifier()
                    .withId("FHIRHelpers")
                    .withVersion("4.0.1"),
                FHIRHelpers()
            )
            context.setExpressionCaching(true)

            val modelResolver = R4FhirModelResolver()
            val evaluationContext = R4EvaluationContext(
                hook,
                context,
                library,
                planDefinition,
                modelResolver
            )
            return CdsResponse(
                HookEvaluator(
                    modelResolver
                ).evaluate(
                    evaluationContext
                )
            )
        }
        catch (e: CqlException) {
            /*this.setAccessControlHeaders(response);
            response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
            response.getWriter().println("ERROR: Exception in CQL Execution.");
            this.printMessageAndCause(e, response);
            if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
                this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
            }

            this.printStackTrack(e, response);*/
            logger.error(e.toString())
        }
        catch (e: Exception) {
            logger.error(e.toString())
            throw ServletException("ERROR: Exception in cds-hooks processing.", e)
        }

        val cards = mutableListOf<Card>()
        cards.add(createTestCdsCard())
        return CdsResponse(cards)
    }

    private fun getCdsServices(): Services {
        val discoveryResolution = DiscoveryResolution(
            client
        )
        return discoveryResolution.resolve()
    }

    private fun getPlanDefinition(planDefinitionId: String): PlanDefinition? {
        val response = client.read(PlanDefinition::class.java)
            .resourceType("PlanDefinition")
            .resourceId(planDefinitionId)
            .execute()
            .block()
        return response?.body
    }

    private fun createTestCdsCard(): Card {
        val source = Card.Source("Phast").also {
            it.url = "https://phast.fr"
        }
        return Card(
            "This is a test card",
            IndicatorCode.INFO,
            source
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CdsHooksController::class.java)
    }
}
