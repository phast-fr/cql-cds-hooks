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

package fr.phast.cql.cdshooks.converters

import fr.phast.cql.cdshooks.models.cards.Card
import fr.phast.cql.cdshooks.models.cards.IndicatorCode
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.RequestGroup
import org.hl7.fhir.r4.model.RequestGroupAction

object R4CarePlanToCdsCard {

    fun convert(carePlan: CarePlan): List<Card> {
        val cards = mutableListOf<Card>()
        for (activity in carePlan.activity!!) {
            if (activity.referenceTarget is RequestGroup) {
                cards.addAll(convert(activity.referenceTarget as RequestGroup))
            }
        }
        return cards
    }

    private fun convert(requestGroup: RequestGroup): List<Card> {
        val cards = mutableListOf<Card>()

        // links
        val links = mutableListOf<Card.Link>()
        if (!requestGroup.extension.isNullOrEmpty()) {
            requestGroup.extension!!.forEach { extension ->
                if (extension.valueAttachment != null) {
                    if (extension.valueAttachment!!.title?.value != null &&
                        extension.valueAttachment!!.url?.value != null &&
                        extension.valueAttachment!!.extension?.get(0)?.valueString?.value != null)
                    links.add(
                        Card.Link(
                            extension.valueAttachment!!.title?.value!!,
                            extension.valueAttachment!!.url?.value!!,
                            extension.valueAttachment!!.extension?.get(0)?.valueString?.value!!
                        )
                    )
                }
                else {
                    throw RuntimeException("Invalid link extension type: $extension")
                }
            }
        }
        if (!requestGroup.action.isNullOrEmpty()) {
            requestGroup.action!!.forEach { action ->
                if (action.title?.value != null &&
                        action.extension?.get(0)?.valueString?.value != null) {
                    val indicator = when (action.extension?.get(0)?.valueString?.value!!) {
                        "info" -> IndicatorCode.INFO
                        "warn" -> IndicatorCode.WARN
                        "critical" -> IndicatorCode.CRITICAL
                        "hard-stop" -> IndicatorCode.HARD_STOP
                        else -> throw RuntimeException(
                            "Invalid indicator code: ${action.extension?.get(0)?.valueString?.value}"
                        )
                    }
                    if (!action.documentation.isNullOrEmpty()) {
                        // Assuming first related artifact has everything
                        val documentation = action.documentation!![0]
                        if (documentation.display != null) {
                            val source = Card.Source(documentation.display!!.value)
                            source.url = documentation.url?.value
                            source.icon = documentation.document?.url?.value
                            val card = Card(
                                action.title?.value!!,
                                indicator,
                                source
                            )
                            card.detail = action.description?.value

                            if (action.selectionBehavior != null) {
                                card.selectionBehavior = action.selectionBehavior!!.text
                            }

                            // suggestions
                            // TODO - uuid
                            if (action.prefix?.value != null) {
                                val suggestion = Card.Suggestion(
                                    action.prefix!!.value,
                                    listOf(
                                        convert(action)
                                    )
                                )
                                if (card.suggestions == null) {
                                    card.suggestions = mutableListOf()
                                }
                                (card.suggestions as MutableList<Card.Suggestion>).add(suggestion)
                            }

                            if (links.isNotEmpty()) {
                                if (card.links == null) {
                                    card.links = mutableListOf()
                                }
                                (card.links as MutableList<Card.Link>).addAll(links)
                            }
                            cards.add(card)
                        }
                        else {
                            throw RuntimeException(
                                "Invalid documentation display: $documentation"
                            )
                        }
                    }
                    else {
                        throw RuntimeException("Invalid documentation")
                    }
                }
                else {
                    throw RuntimeException("Invalid action: $action")
                }
            }
        }
        return cards.toList()
    }

    private fun convert(action: RequestGroupAction): Card.Action {
        if (action.type?.coding?.get(0)?.code?.value != null &&
            action.description?.value != null &&
            action.resourceTarget != null)
        return Card.Action(
            action.type!!.coding?.get(0)?.code?.value!!,
            action.description!!.value,
            action.resourceTarget!!
        )
        throw RuntimeException("Invalid action")
    }
}
