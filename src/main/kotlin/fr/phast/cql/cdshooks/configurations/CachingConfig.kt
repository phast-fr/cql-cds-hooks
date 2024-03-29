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
import org.cqframework.cql.cql2elm.model.Model
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.runtime.Code
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


@Configuration
@EnableCaching
class CachingConfig {
    @Bean
    fun caffeineConfig(): Caffeine<Any, Any> {
        return Caffeine
            .newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
    }

    @Bean
    fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
        return CaffeineCacheManager().apply {
            setCaffeine(caffeine)
        }
    }

    @Bean(name = ["globalModelCache"])
    fun globalModelCache(): Map<org.hl7.elm.r1.VersionedIdentifier, Model> {
        return ConcurrentHashMap()
    }

    @Bean(name = ["globalLibraryCache"])
    fun globalLibraryCache(): Map<VersionedIdentifier, Library> {
        return ConcurrentHashMap()
    }

    @Bean(name = ["globalTerminologyCache"])
    fun terminologyCache(): Map<String, Iterable<Code>> {
        val cache = Caffeine
            .newBuilder()
            .maximumSize(100)
            .expireAfterAccess(Duration.ofMinutes(60))
            .build<String, Iterable<Code>>()
        return cache.asMap()
    }

}
