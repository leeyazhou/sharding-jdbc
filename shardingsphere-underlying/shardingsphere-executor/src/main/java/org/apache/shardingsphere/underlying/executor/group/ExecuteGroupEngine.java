/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.underlying.executor.group;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.underlying.executor.context.ExecutionUnit;
import org.apache.shardingsphere.underlying.executor.context.SQLUnit;
import org.apache.shardingsphere.underlying.executor.kernel.InputGroup;
import org.apache.shardingsphere.underlying.executor.sql.StorageResourceExecuteUnit;
import org.apache.shardingsphere.underlying.executor.sql.StorageResourceOption;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.connection.ConnectionMode;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.connection.ExecutionConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Execute group engine.
 * 
 * @param <T> type of storage resource execute unit
 * @param <O> type of storage resource option
 */
@RequiredArgsConstructor
public abstract class ExecuteGroupEngine<T extends StorageResourceExecuteUnit, O extends StorageResourceOption> {
    
    private final int maxConnectionsSizePerQuery;
    
    /**
     * Generate storage resource execute unit groups.
     *
     * @param executionUnits execution units
     * @param executionConnection execution connection
     * @param storageResourceOption storage resource option
     * @return storage resource execute unit groups
     * @throws SQLException SQL exception
     */
    public Collection<InputGroup<T>> generate(final Collection<ExecutionUnit> executionUnits, final ExecutionConnection executionConnection, final O storageResourceOption) throws SQLException {
        Collection<InputGroup<T>> result = new LinkedList<>();
        for (Entry<String, List<SQLUnit>> entry : generateSQLUnitGroups(executionUnits).entrySet()) {
            result.addAll(generateSQLExecuteGroups(entry.getKey(), entry.getValue(), executionConnection, storageResourceOption));
        }
        return result;
    }
    
    private Map<String, List<SQLUnit>> generateSQLUnitGroups(final Collection<ExecutionUnit> executionUnits) {
        Map<String, List<SQLUnit>> result = new LinkedHashMap<>(executionUnits.size(), 1);
        for (ExecutionUnit each : executionUnits) {
            if (!result.containsKey(each.getDataSourceName())) {
                result.put(each.getDataSourceName(), new LinkedList<>());
            }
            result.get(each.getDataSourceName()).add(each.getSqlUnit());
        }
        return result;
    }
    
    private List<InputGroup<T>> generateSQLExecuteGroups(final String dataSourceName, final List<SQLUnit> sqlUnits,
                                                         final ExecutionConnection executionConnection, final O storageResourceOption) throws SQLException {
        List<InputGroup<T>> result = new LinkedList<>();
        int desiredPartitionSize = Math.max(0 == sqlUnits.size() % maxConnectionsSizePerQuery ? sqlUnits.size() / maxConnectionsSizePerQuery : sqlUnits.size() / maxConnectionsSizePerQuery + 1, 1);
        List<List<SQLUnit>> sqlUnitPartitions = Lists.partition(sqlUnits, desiredPartitionSize);
        ConnectionMode connectionMode = maxConnectionsSizePerQuery < sqlUnits.size() ? ConnectionMode.CONNECTION_STRICTLY : ConnectionMode.MEMORY_STRICTLY;
        List<Connection> connections = executionConnection.getConnections(dataSourceName, sqlUnitPartitions.size(), connectionMode);
        int count = 0;
        for (List<SQLUnit> each : sqlUnitPartitions) {
            result.add(generateSQLExecuteGroup(dataSourceName, each, executionConnection, connections.get(count++), connectionMode, storageResourceOption));
        }
        return result;
    }
    
    private InputGroup<T> generateSQLExecuteGroup(final String dataSourceName, final List<SQLUnit> sqlUnitGroup, final ExecutionConnection executionConnection,
                                                  final Connection connection, final ConnectionMode connectionMode, final O storageResourceOption) throws SQLException {
        List<T> result = new LinkedList<>();
        for (SQLUnit each : sqlUnitGroup) {
            result.add(createStorageResourceExecuteUnit(new ExecutionUnit(dataSourceName, each), executionConnection, connection, connectionMode, storageResourceOption));
        }
        return new InputGroup<>(result);
    }
    
    protected abstract T createStorageResourceExecuteUnit(ExecutionUnit executionUnit, ExecutionConnection executionConnection, 
                                                          Connection connection, ConnectionMode connectionMode, O storageResourceOption) throws SQLException;
}
