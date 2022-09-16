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

import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.DomainResource
import org.opencds.cqf.cql.engine.execution.Context
import org.opencds.cqf.cql.engine.execution.LibraryLoader

// TODO Dev
class ExpressionEvaluation {

    fun evaluateInContext(resource: DomainResource, cql: String, patientId: String): Any? {
        return evaluateInContext(resource, cql, patientId, false)
    }

    fun evaluateInContext(resource: DomainResource, cql: String, patientId: String, aliasedExpression: Boolean): Any? {
        val context = setupContext(resource, cql, patientId, aliasedExpression)
        return context.resolveExpressionRef("Expression").evaluate(context)
    }

    private fun setupContext(resource: DomainResource, cql: String, patientId: String, aliasedExpression: Boolean): Context {
        return setupContext(resource, patientId, null)
    }

    private fun setupContext(resource: DomainResource, patientId: String, libraryLoader: LibraryLoader?): Context {
        // Provide the instance as the value of the '%context' parameter, as well as the
        // value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through
        // the %context attribute
        val context = Context(libraryLoader?.load(VersionedIdentifier().withId("LocalLibrary")))
        //context.debugMap = getDebugMap()
        context.setParameter(null, "resource.fhirType()", resource)
        context.setParameter(null, "%context", resource)
        context.setExpressionCaching(true)
        context.registerLibraryLoader(libraryLoader)
        context.setContextValue("Patient", patientId)
        /*val terminologyProvider = jpaTerminologyProviderFactory.create(theRequest)

        context.registerTerminologyProvider(terminologyProvider)
        val dataProvider = jpaDataProviderFactory.create(theRequest, terminologyProvider)
        context.registerDataProvider("http://hl7.org/fhir", dataProvider)*/
        return context
    }
}
