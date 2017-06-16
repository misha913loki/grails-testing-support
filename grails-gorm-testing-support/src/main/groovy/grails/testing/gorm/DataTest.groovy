/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package grails.testing.gorm

import grails.core.GrailsDomainClass
import grails.gorm.validation.PersistentEntityValidator
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.lifecycle.Initializable
import org.grails.testing.GrailsUnitTest
import org.grails.testing.gorm.MockCascadingDomainClassValidator
import org.grails.testing.gorm.spock.DataTestSetupSpecInterceptor
import org.grails.validation.ConstraintEvalUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Validator

@CompileStatic
trait DataTest extends GrailsUnitTest {

    boolean domainsHaveBeenMocked = false
    Session currentSession

    Class<?>[] getDomainClassesToMock() {}

    boolean getFailOnError() {
        false
    }

    /**
     * Mocks a domain class providing the equivalent GORM behavior but against an in-memory
     * concurrent hash map instead of a database
     *
     * @param domainClassToMock The domain class to mock
     * @param domains Optional. The list of domains to save
     */
    void mockDomain(Class<?> domainClassToMock, List domains = []) {
        mockDomains(domainClassToMock)
        PersistentEntity entity = dataStore.mappingContext.getPersistentEntity(domainClassToMock.name)
        if (domains) {
            saveDomainList(entity, domains)
        }
    }

    /**
     * Mocks domain classes providing the equivalent GORM behavior but against an in-memory
     * concurrent hash map instead of a database
     *
     * @param domainClassesToMock The list of domain classes to mock
     */
    void mockDomains(Class<?>... domainClassesToMock) {
        initialMockDomainSetup()
        Collection<PersistentEntity> entities = dataStore.mappingContext.addPersistentEntities(domainClassesToMock)
        for (PersistentEntity entity in entities) {
            registerGrailsDomainClass(entity.javaClass)

            Validator validator = registerDomainClassValidator(entity)
            dataStore.mappingContext.addEntityValidator(entity, validator)
        }
        new GormEnhancer(dataStore, transactionManager, getFailOnError())

        initializeMappingContext()
    }

    AbstractDatastore getDataStore() {
        applicationContext.getBean(AbstractDatastore)
    }

    PlatformTransactionManager getTransactionManager() {
        applicationContext.getBean('transactionManager', PlatformTransactionManager)
    }

    private void registerGrailsDomainClass(Class<?> domainClassToMock) {
        grailsApplication.addArtefact(DomainClassArtefactHandler.TYPE, domainClassToMock)
    }

    @CompileDynamic
    private Validator registerDomainClassValidator(PersistentEntity domain) {
        String validationBeanName = "${domain.javaClass.name}Validator"
        defineBeans {
            "${domain.javaClass.name}"(domain.javaClass) { bean ->
                bean.singleton = false
                bean.autowire = "byName"
            }

            if (DataTestSetupSpecInterceptor.IS_OLD_SETUP) {
                GrailsDomainClass grailsDomain = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, domain.javaClass.name)

                "$validationBeanName"(MockCascadingDomainClassValidator) { bean ->
                    getDelegate().messageSource = ref("messageSource")
                    bean.lazyInit = true
                    getDelegate().domainClass = grailsDomain
                    getDelegate().grailsApplication = grailsApplication
                }
            } else {
                "$validationBeanName"(PersistentEntityValidator, domain, ref("messageSource"), ref(DataTestSetupSpecInterceptor.BEAN_NAME))
            }

        }

        applicationContext.getBean(validationBeanName, Validator)
    }

    private void initialMockDomainSetup() {
        ConstraintEvalUtils.clearDefaultConstraints()
        ((DomainClassArtefactHandler) grailsApplication.getArtefactHandler(DomainClassArtefactHandler.TYPE)).setGrailsApplication(grailsApplication)
    }

    private void initializeMappingContext() {
        def context = dataStore.mappingContext
        if (context instanceof Initializable) {
            context.initialize()
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void saveDomainList(PersistentEntity entity, List domains) {
        for (obj in domains) {
            if (obj instanceof Map) {
                entity.javaClass.newInstance(obj).save()
            } else if (entity.isInstance(obj)) {
                obj.save()
            }
        }
    }
}
