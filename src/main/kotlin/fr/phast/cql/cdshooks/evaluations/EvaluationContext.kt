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

package fr.phast.cql.cdshooks.evaluations

import fr.phast.cql.cdshooks.models.hooks.Hook
import fr.phast.cql.cdshooks.models.hooks.OrderSelectHook
import fr.phast.cql.cdshooks.models.hooks.OrderSignHook
import org.cqframework.cql.elm.execution.Library
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.opencds.cqf.cql.engine.execution.Context
import org.opencds.cqf.cql.engine.model.ModelResolver

abstract class EvaluationContext<R: Resource>(
    private val hook: Hook,
    private val context: Context,
    private val library: Library,
    private val planDefinition: R,
    private val modelResolver: ModelResolver
) {

    private var contextResources: MutableList<Resource>? = null
    private var prefetchResources: MutableList<Resource>? = null

    open fun getHook(): Hook {
        return hook
    }

    open fun getPlanDefinition(): R {
        return planDefinition
    }

    open fun getLibrary(): Library {
        return library
    }

    open fun getContext(): Context {
        return context
    }

    open fun getContextResources(): List<Resource> {
        if (contextResources == null) {
            contextResources = mutableListOf()
            when (hook) {
                is OrderSelectHook -> {
                    hook.context.draftOrders.entry?.forEach { entry ->
                        entry.resource?.let { contextResources!!.add(it) }
                    }
                }
                is OrderSignHook -> {
                    hook.context.draftOrders.entry?.forEach { entry ->
                        entry.resource?.let { contextResources!!.add(it) }
                    }
                }
            }
            /*if (hook.getRequest().isApplyCql()) {
                contextResources = applyCqlToResources(contextResources!!)
            }*/
        }
        return contextResources!!.toList()
    }

    open fun getPrefetchResources(): List<Resource> {
        if (prefetchResources == null) {
            prefetchResources = mutableListOf()
            hook.prefetch?.forEach { entry ->
                addResources(entry.value, prefetchResources!!)
            }
            /*if (hook.getRequest().isApplyCql()) {
                prefetchResources = applyCqlToResources(prefetchResources!!)
            }*/
        }
        return prefetchResources!!.toList()
    }

    private fun addResources(resource: Resource, resources: MutableList<Resource>) {
        if (resource is Bundle) {
            resources.addAll(resolveBundle(resource))
        }
        else {
            resources.add(resource)
        }
    }

    private fun resolveBundle(bundle: Bundle): List<Resource> {
        val resources = mutableListOf<Resource>()
        bundle.entry?.forEach { entry ->
            entry.resource?.let { resources.add(it) }
        }
        return resources
    }

    abstract fun applyCqlToResources(resources: List<Any>): List<Any>
}
