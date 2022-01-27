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

import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.UrlType

class AttachmentBuilder: BaseBuilder<Attachment>(Attachment()) {

    // TODO - incomplete
    fun buildUrl(url: UrlType): AttachmentBuilder {
        complexProperty.url = url
        return this
    }

    fun buildTitle(title: StringType): AttachmentBuilder {
        complexProperty.title = title
        return this
    }

    fun buildExtension(extensions: List<Extension>): AttachmentBuilder {
        complexProperty.extension = extensions
        return this
    }
}
