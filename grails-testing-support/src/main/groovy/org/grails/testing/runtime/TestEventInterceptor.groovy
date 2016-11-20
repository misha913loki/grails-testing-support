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
package org.grails.testing.runtime
/**
 * Interface for event interceptors in the TestRuntime
 * 
 * An event interceptor can be registered in 2 different ways:
 * - all plugins that implement this interface
 * - manually by calling TestRuntime.addInterceptor method
 * 
 */
interface TestEventInterceptor {
    /**
     * This method is called before an event gets delivered and processed
     * 
     * event delivery can be prevented by setting the "stopDelivery" flag on the event instance
     * 
     * @param event
     */
    void eventPublished(TestEvent event)
    /**
     * This method is called after a single event has been delivered to plugins
     * 
     * @param event
     */
    void eventDelivered(TestEvent event)
    /**
     * This method is called after each "top-level" event has been fully processed.
     * This means that the event has been delivered and all of the consequnce events have been also
     * processed.
     * 
     * Events with "immediateDelivery" flag are considered top-level events besides the first initial 
     * event delivered to the system
     * 
     * This list of consequence events doesn't contain other "top-level" events.
     * 
     * @param event
     * @param consequenceEvents processed consequence events that were the implication of the initial event and that were also processed
     */
    void eventsProcessed(TestEvent event, List<TestEvent> consequenceEvents)
    /**
     * This method is called before deferred events are delivered
     * 
     * this method might be called several times for a single top-level event since the processing 
     * of each deferred event might bring in more deferred events.
     * 
     * A deferred event can be removed/combined or events can be reordered before they get delivered.
     * 
     * @param event top-level event
     * @param deferredEvents list of deferred 
     */
    void mutateDeferredEvents(TestEvent event, List<TestEvent> deferredEvents)
}
