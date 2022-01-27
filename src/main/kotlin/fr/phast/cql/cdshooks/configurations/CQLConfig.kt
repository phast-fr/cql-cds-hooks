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

package fr.phast.cql.cdshooks.configurations

import com.github.benmanes.caffeine.cache.Caffeine
import fr.phast.cql.engine.fhir.terminology.R4FhirTerminologyProvider
import fr.phast.cql.services.LibraryService
import fr.phast.cql.services.decorators.CacheAwareTerminologyDecorator
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.model.Model
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


@Configuration
open class CQLConfig {

    @Bean
    open fun caffeineConfig(): Caffeine<Any, Any> {
        return Caffeine
            .newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
    }

    @Bean
    open fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
        return CaffeineCacheManager().also {
            it.setCaffeine(caffeine)
        }
    }

    @Bean(name = ["globalModelCache"])
    open fun globalModelCache(): MutableMap<org.hl7.elm.r1.VersionedIdentifier, Model> {
        return ConcurrentHashMap()
    }

    @Bean(name = ["globalLibraryCache"])
    open fun globalLibraryCache(): MutableMap<VersionedIdentifier, Library> {
        return ConcurrentHashMap()
    }

    @Bean(name = ["globalTerminologyCache"])
    open fun terminologyCache(): Map<String, Iterable<Code>> {
        val cache = Caffeine
            .newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofMinutes(60))
            .build<String, Iterable<Code>>()
        return cache.asMap()
    }

    @Lazy
    @Bean
    open fun terminologyProvider(tioProperties: TIOProperties,
                                 globalTerminologyCache: MutableMap<String, Iterable<Code>>): TerminologyProvider {
        return CacheAwareTerminologyDecorator(
            R4FhirTerminologyProvider(tioProperties.uri, tioProperties.credential), globalTerminologyCache
        )
    }

    @Lazy
    @Bean
    open fun libraryService(
        globalModelCache: MutableMap<org.hl7.elm.r1.VersionedIdentifier, Model>,
        globalLibraryCache: MutableMap<VersionedIdentifier, Library>,
        cqlTranslatorOptions: CqlTranslatorOptions
    ): LibraryService {
        return LibraryService(globalModelCache, globalLibraryCache, cqlTranslatorOptions)
    }

    @Lazy
    @Bean
    open fun cqlTranslatorOptions(): CqlTranslatorOptions {
        return CqlTranslatorOptions(
            CqlTranslator.Options.EnableAnnotations,
            CqlTranslator.Options.EnableLocators,
            CqlTranslator.Options.DisableListDemotion,
            CqlTranslator.Options.DisableListPromotion,
            CqlTranslator.Options.DisableMethodInvocation
        )
    }
}
