/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.creation;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jberet.cdi.PartitionScoped;
import org.jberet.runtime.context.StepContextImpl;

public class PartitionScopedContextImpl implements Context {
    static final PartitionScopedContextImpl INSTANCE = new PartitionScopedContextImpl();

    private PartitionScopedContextImpl() {
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return PartitionScoped.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        final ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> partitionScopedBeans = getPartitionScopedBeans();
        JobScopedContextImpl.ScopedInstance<?> existing = partitionScopedBeans.get(contextual);
        if (existing == null) {
            final T instance = contextual.create(creationalContext);
            existing = partitionScopedBeans.putIfAbsent(contextual, new JobScopedContextImpl.ScopedInstance<T>(instance, creationalContext));
            if (existing == null) {
                return instance;
            } else {
                return (T) existing.instance;
            }
        } else {
            return (T) existing.instance;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual) {
        final JobScopedContextImpl.ScopedInstance<?> existing = getPartitionScopedBeans().get(contextual);
        return existing == null ? null : (T) existing.instance;
    }

    @Override
    public boolean isActive() {
        final StepContextImpl stepContext = ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext;
        return stepContext != null && stepContext.getPartitionScopedBeans() != null;
    }

    private ConcurrentMap<Contextual<?>, JobScopedContextImpl.ScopedInstance<?>> getPartitionScopedBeans() {
        final StepContextImpl stepContext = ArtifactCreationContext.getCurrentArtifactCreationContext().stepContext;
        return stepContext.getPartitionScopedBeans();
    }
}
