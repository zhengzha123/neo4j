/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.index.label;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.index.internal.gbptree.RecoveryCleanupWorkCollector;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.IOLimiter;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.tracing.cursor.PageCursorTracer;
import org.neo4j.kernel.impl.index.schema.ConsistencyCheckable;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.monitoring.Monitors;
import org.neo4j.storageengine.api.NodeLabelUpdateListener;

/**
 * Stores token-->entities mappings. It receives updates in the form of condensed token->entity transaction data
 * and can iterate through all entities for any given token.
 */
public interface TokenScanStore extends Lifecycle, ConsistencyCheckable
{
    /**
     * Create a new {@link LabelScanStore}.
     */
    static LabelScanStore labelScanStore( PageCache pageCache, DatabaseLayout directoryStructure, FileSystemAbstraction fs,
            FullStoreChangeStream fullStoreChangeStream, boolean readOnly, Monitors monitors, RecoveryCleanupWorkCollector recoveryCleanupWorkCollector )
    {
        return new NativeLabelScanStore( pageCache, directoryStructure, fs, fullStoreChangeStream, readOnly, monitors, recoveryCleanupWorkCollector );
    }

    /**
     * @return a {@link LabelScanReader} capable of retrieving nodes for labels.
     */
    LabelScanReader newReader();

    /**
     * Acquire a writer for updating the store.
     *
     * @return {@link LabelScanWriter} that can modify the {@link LabelScanStore}.
     */
    LabelScanWriter newWriter();

    /**
     * Acquire a writer that is specialized in bulk-append writing, e.g. building from initial data.
     *
     * @return {@link LabelScanWriter} that can modify the {@link LabelScanStore}.
     */
    LabelScanWriter newBulkAppendWriter();

    /**
     * Forces all changes to disk. Called at certain points from within Neo4j for example when
     * rotating the logical log. There cannot be any essential state not forced to disk
     * after completion of this call .
     *
     * @throws IOException if there was a problem forcing the state to persistent storage.
     */
    void force( IOLimiter limiter, PageCursorTracer cursorTracer ) throws IOException;

    /**
     * Acquire a reader for all {@link NodeLabelRange node label} ranges.
     *
     * @return the {@link AllEntriesLabelScanReader reader}.
     */
    AllEntriesLabelScanReader allNodeLabelRanges();

    /**
     * Acquire a reader for all {@link NodeLabelRange node label} ranges.
     *
     * @return the {@link AllEntriesLabelScanReader reader}.
     */
    AllEntriesLabelScanReader allNodeLabelRanges( long fromNodeId, long toNodeId );

    ResourceIterator<File> snapshotStoreFiles();

    /**
     * Acquire a listener that can take care of entity token updates.
     */
    NodeLabelUpdateListener updateListener();

    /**
     * @return {@code true} if there's no data at all in this label scan store, otherwise {@code false}.
     * @throws IOException on I/O error.
     */
    boolean isEmpty() throws IOException;

    /**
     * Initializes the store. Recovery updates can be processed after this has been called.
     */
    @Override
    void init() throws IOException;

    /**
     * Starts the store. Updates can be processed after this has been called.
     */
    @Override
    void start() throws IOException;

    @Override
    void stop();

    /**
     * Shuts down the store and all resources acquired by it.
     */
    @Override
    void shutdown() throws IOException;

    /**
     * Drops any persistent storage backing this store.
     *
     * @throws IOException on I/O error.
     */
    void drop() throws IOException;

    /**
     * @return whether or not this index is read-only.
     */
    boolean isReadOnly();

    interface Monitor
    {
        Monitor EMPTY = new Monitor.Adaptor();

        class Adaptor implements Monitor
        {
            @Override
            public void init()
            {   // empty
            }

            @Override
            public void noIndex()
            {   // empty
            }

            @Override
            public void notValidIndex()
            {   // empty
            }

            @Override
            public void rebuilding()
            {   // empty
            }

            @Override
            public void rebuilt( long roughNodeCount )
            {   // empty
            }

            @Override
            public void recoveryCleanupRegistered()
            {   // empty
            }

            @Override
            public void recoveryCleanupStarted()
            {   // empty
            }

            @Override
            public void recoveryCleanupFinished( long numberOfPagesVisited, long numberOfTreeNodes, long numberOfCleanedCrashPointers, long durationMillis )
            {   // empty
            }

            @Override
            public void recoveryCleanupClosed()
            {   // empty
            }

            @Override
            public void recoveryCleanupFailed( Throwable throwable )
            {   // empty
            }
        }

        void init();

        void noIndex();

        void notValidIndex();

        void rebuilding();

        void rebuilt( long roughNodeCount );

        void recoveryCleanupRegistered();

        void recoveryCleanupStarted();

        void recoveryCleanupFinished( long numberOfPagesVisited, long numberOfTreeNodes, long numberOfCleanedCrashPointers, long durationMillis );

        void recoveryCleanupClosed();

        void recoveryCleanupFailed( Throwable throwable );
    }
}
