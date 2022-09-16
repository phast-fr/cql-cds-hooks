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

class RequestGroupActionBuilder: BaseBuilder<RequestGroupAction>(RequestGroupAction()) {

    // TODO - incomplete
    fun buildPrefix(prefix: StringType): RequestGroupActionBuilder {
        complexProperty.prefix = prefix
        return this
    }

    fun buildTitle(title: StringType): RequestGroupActionBuilder {
        complexProperty.title = title
        return this
    }

    fun buildDescription(description: StringType): RequestGroupActionBuilder {
        complexProperty.description = description
        return this
    }

    fun buildDocumentation(documentations: List<RelatedArtifact>): RequestGroupActionBuilder {
        complexProperty.documentation = documentations
        return this
    }

    fun buildType(type: CodeableConcept): RequestGroupActionBuilder {
        complexProperty.type = type
        return this
    }

    fun buildSelectionBehavior(selectionBehavior: ActionSelectionBehavior): RequestGroupActionBuilder {
        complexProperty.selectionBehavior = selectionBehavior
        return this
    }

    fun buildResource(resource: Reference): RequestGroupActionBuilder {
        complexProperty.resource = resource
        return this
    }

    fun buildResourceTarget(resource: Resource): RequestGroupActionBuilder {
        complexProperty.resourceTarget = resource
        return this
    }

    fun buildExtension(extension: StringType): RequestGroupActionBuilder {
        complexProperty.extension = listOf(
            Extension("http://example.org").also {
                it.valueString = extension
            }
        )
        return this
    }

    fun buildExtension(extensions: List<Extension>): RequestGroupActionBuilder {
        complexProperty.extension = extensions
        return this
    }
}
