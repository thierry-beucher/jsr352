/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jberet.support._private.SupportLogger;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.io.ICsvWriter;

import static org.jberet.support.io.CsvProperties.BEAN_TYPE_KEY;
import static org.jberet.support.io.CsvProperties.HEADER_KEY;
import static org.jberet.support.io.CsvProperties.OVERWRITE;
import static org.jberet.support.io.CsvProperties.RESOURCE_STEP_CONTEXT;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that writes data to CSV file or resource.
 * This class is not designed to be thread-safe and its instance should not be shared between threads.
 */
@Named
public class CsvItemWriter extends CsvItemReaderWriterBase implements ItemWriter {
    @Inject
    protected StepContext stepContext;

    @Inject
    @BatchProperty
    protected String[] header;

    @Inject
    @BatchProperty
    protected String writeComments;

    @Inject
    @BatchProperty
    protected String writeMode;

    protected ICsvWriter delegateWriter;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        SupportLogger.LOGGER.tracef("Open CsvItemWriter with checkpoint %s, which is ignored for CsvItemWriter.%n", checkpoint);
        if (beanType == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, BEAN_TYPE_KEY);
        } else if (java.util.List.class.isAssignableFrom(beanType)) {
            delegateWriter = new CsvListWriter(getOutputWriter(writeMode, stepContext), getCsvPreference());
        } else if (java.util.Map.class.isAssignableFrom(beanType)) {
            delegateWriter = new CsvMapWriter(getOutputWriter(writeMode, stepContext), getCsvPreference());
        } else {
            delegateWriter = new CsvBeanWriter(getOutputWriter(writeMode, stepContext), getCsvPreference());
        }
        if (header == null) {
            throw SupportLogger.LOGGER.invalidReaderWriterProperty(null, HEADER_KEY);
        }
        if (this.nameMapping == null) {
            this.nameMapping = header;
        }
        SupportLogger.LOGGER.openingResource(resource, this.getClass());

        this.cellProcessorInstances = getCellProcessors();
        if (writeComments != null) {
            delegateWriter.writeComment(writeComments);
        }
        if (!skipWritingHeader) {
            delegateWriter.writeHeader(header);
        }
    }

    @Override
    public void close() throws Exception {
        if (delegateWriter != null) {
            if (resource.equalsIgnoreCase(RESOURCE_STEP_CONTEXT)) {
                final Object transientUserData = stepContext.getTransientUserData();
                if (OVERWRITE.equalsIgnoreCase(writeMode) || transientUserData == null) {
                    stepContext.setTransientUserData(stringWriter.toString());
                } else {
                    stepContext.setTransientUserData(transientUserData + getCsvPreference().getEndOfLineSymbols() +
                            stringWriter.toString());
                }
                stringWriter = null;
            }
            SupportLogger.LOGGER.closingResource(resource, this.getClass());
            delegateWriter.close();
            delegateWriter = null;
        }
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        if (SupportLogger.LOGGER.isTraceEnabled()) {
            SupportLogger.LOGGER.tracef("About to write items, number of items %s, element type %s%n",
                    items.size(), items.get(0).getClass());
        }
        if (delegateWriter instanceof ICsvBeanWriter) {
            final ICsvBeanWriter writer = (ICsvBeanWriter) delegateWriter;
            if (cellProcessorInstances.length == 0) {
                for (final Object e : items) {
                    writer.write(e, nameMapping);
                }
            } else {
                for (final Object e : items) {
                    writer.write(e, nameMapping, cellProcessorInstances);
                }
            }
        } else if (delegateWriter instanceof ICsvMapWriter) {
            final ICsvMapWriter writer = (ICsvMapWriter) delegateWriter;
            if (cellProcessorInstances.length == 0) {
                for (final Object e : items) {
                    writer.write((Map<String, ?>) e, nameMapping);
                }
            } else {
                for (final Object e : items) {
                    writer.write((Map<String, ?>) e, nameMapping, cellProcessorInstances);
                }
            }
        } else if (delegateWriter instanceof ICsvListWriter) {
            final ICsvListWriter writer = (ICsvListWriter) delegateWriter;
            if (cellProcessorInstances.length == 0) {
                for (final Object e : items) {
                    writer.write((List<?>) e);
                }
            } else {
                for (final Object e : items) {
                    writer.write((List<?>) e, cellProcessorInstances);
                }
            }
        }
        delegateWriter.flush();
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return delegateWriter.getRowNumber();
    }
}