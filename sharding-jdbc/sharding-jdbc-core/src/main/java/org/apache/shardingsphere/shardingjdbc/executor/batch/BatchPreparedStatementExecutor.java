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

package org.apache.shardingsphere.shardingjdbc.executor.batch;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.context.impl.ShardingRuntimeContext;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.underlying.executor.context.ExecutionUnit;
import org.apache.shardingsphere.underlying.executor.kernel.InputGroup;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.StatementExecuteUnit;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.connection.ConnectionMode;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.executor.ExecutorExceptionHandler;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.executor.SQLExecutor;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.executor.SQLExecutorCallback;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prepared statement executor to process add batch.
 */
public final class BatchPreparedStatementExecutor {
    
    private final ShardingRuntimeContext runtimeContext;
    
    private final SQLExecutor sqlExecutor;
    
    private final Collection<InputGroup<StatementExecuteUnit>> inputGroups;
    
    @Getter
    private final Collection<BatchExecutionUnit> batchExecutionUnits;
    
    private int batchCount;
    
    public BatchPreparedStatementExecutor(final ShardingRuntimeContext runtimeContext, final SQLExecutor sqlExecutor) {
        this.runtimeContext = runtimeContext;
        this.sqlExecutor = sqlExecutor;
        inputGroups = new LinkedList<>();
        batchExecutionUnits = new LinkedList<>();
    }
    
    /**
     * Initialize executor.
     *
     * @param inputGroups input groups
     */
    public void init(final Collection<InputGroup<StatementExecuteUnit>> inputGroups) {
        this.inputGroups.addAll(inputGroups);
    }
    
    /**
     * Add batch for execution units.
     *
     * @param executionUnits execution units
     */
    public void addBatchForExecutionUnits(final Collection<ExecutionUnit> executionUnits) {
        Collection<BatchExecutionUnit> batchExecutionUnits = createBatchExecutionUnits(executionUnits);
        handleOldBatchExecutionUnits(batchExecutionUnits);
        handleNewBatchExecutionUnits(batchExecutionUnits);
        batchCount++;
    }
    
    private Collection<BatchExecutionUnit> createBatchExecutionUnits(final Collection<ExecutionUnit> executionUnits) {
        return executionUnits.stream().map(BatchExecutionUnit::new).collect(Collectors.toList());
    }
    
    private void handleOldBatchExecutionUnits(final Collection<BatchExecutionUnit> newExecutionUnits) {
        newExecutionUnits.forEach(this::reviseBatchExecutionUnits);
    }
    
    private void reviseBatchExecutionUnits(final BatchExecutionUnit batchExecutionUnit) {
        for (BatchExecutionUnit each : batchExecutionUnits) {
            if (each.equals(batchExecutionUnit)) {
                reviseBatchExecutionUnit(each, batchExecutionUnit);
            }
        }
    }
    
    private void reviseBatchExecutionUnit(final BatchExecutionUnit oldBatchExecutionUnit, final BatchExecutionUnit newBatchExecutionUnit) {
        oldBatchExecutionUnit.getExecutionUnit().getSqlUnit().getParameters().addAll(newBatchExecutionUnit.getExecutionUnit().getSqlUnit().getParameters());
        oldBatchExecutionUnit.mapAddBatchCount(batchCount);
    }
    
    private void handleNewBatchExecutionUnits(final Collection<BatchExecutionUnit> newExecutionUnits) {
        newExecutionUnits.removeAll(batchExecutionUnits);
        for (BatchExecutionUnit each : newExecutionUnits) {
            each.mapAddBatchCount(batchCount);
        }
        batchExecutionUnits.addAll(newExecutionUnits);
    }
    
    /**
     * Execute batch.
     * 
     * @param sqlStatementContext SQL statement context
     * @return execute results
     * @throws SQLException SQL exception
     */
    public int[] executeBatch(final SQLStatementContext sqlStatementContext) throws SQLException {
        boolean isExceptionThrown = ExecutorExceptionHandler.isExceptionThrown();
        SQLExecutorCallback<int[]> callback = new SQLExecutorCallback<int[]>(runtimeContext.getDatabaseType(), isExceptionThrown) {
            
            @Override
            protected int[] executeSQL(final String sql, final Statement statement, final ConnectionMode connectionMode) throws SQLException {
                return statement.executeBatch();
            }
        };
        List<int[]> results = sqlExecutor.execute(inputGroups, callback);
        if (!runtimeContext.getRule().isAllBroadcastTables(sqlStatementContext.getTablesContext().getTableNames())) {
            return accumulate(results);
        } else {
            return results.get(0);
        }
    }
    
    private int[] accumulate(final List<int[]> results) {
        int[] result = new int[batchCount];
        int count = 0;
        for (InputGroup<StatementExecuteUnit> each : inputGroups) {
            for (StatementExecuteUnit eachUnit : each.getInputs()) {
                Map<Integer, Integer> jdbcAndActualAddBatchCallTimesMap = Collections.emptyMap();
                for (BatchExecutionUnit eachExecutionUnit : batchExecutionUnits) {
                    if (isSameDataSourceAndSQL(eachExecutionUnit, eachUnit)) {
                        jdbcAndActualAddBatchCallTimesMap = eachExecutionUnit.getJdbcAndActualAddBatchCallTimesMap();
                        break;
                    }
                }
                for (Entry<Integer, Integer> entry : jdbcAndActualAddBatchCallTimesMap.entrySet()) {
                    int value = null == results.get(count) ? 0 : results.get(count)[entry.getValue()];
                    result[entry.getKey()] += value;
                }
                count++;
            }
        }
        return result;
    }
    
    private boolean isSameDataSourceAndSQL(final BatchExecutionUnit batchExecutionUnit, final StatementExecuteUnit statementExecuteUnit) {
        return batchExecutionUnit.getExecutionUnit().getDataSourceName().equals(statementExecuteUnit.getExecutionUnit().getDataSourceName())
                && batchExecutionUnit.getExecutionUnit().getSqlUnit().getSql().equals(statementExecuteUnit.getExecutionUnit().getSqlUnit().getSql());
    }
    
    /**
     * Get statements.
     *
     * @return statements
     */
    public List<Statement> getStatements() {
        List<Statement> result = new LinkedList<>();
        for (InputGroup<StatementExecuteUnit> each : inputGroups) {
            result.addAll(each.getInputs().stream().map(StatementExecuteUnit::getStorageResource).collect(Collectors.toList()));
        }
        return result;
    }
    
    /**
     * Get parameter sets.
     *
     * @param statement statement
     * @return parameter sets
     */
    public List<List<Object>> getParameterSet(final Statement statement) {
        List<List<Object>> result = new LinkedList<>();
        for (InputGroup<StatementExecuteUnit> each : inputGroups) {
            Optional<StatementExecuteUnit> target = findStatementExecuteUnit(statement, each);
            if (target.isPresent()) {
                result = getParameterSets(target.get());
                break;
            }
        }
        return result;
    }
    
    private Optional<StatementExecuteUnit> findStatementExecuteUnit(final Statement statement, final InputGroup<StatementExecuteUnit> executeGroup) {
        for (StatementExecuteUnit each : executeGroup.getInputs()) {
            if (each.getStorageResource().equals(statement)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }
    
    private List<List<Object>> getParameterSets(final StatementExecuteUnit executeUnit) {
        Optional<BatchExecutionUnit> batchExecutionUnit = batchExecutionUnits.stream().filter(each -> isSameDataSourceAndSQL(each, executeUnit)).findFirst();
        Preconditions.checkState(batchExecutionUnit.isPresent());
        return batchExecutionUnit.get().getParameterSets();
    }
    
    /**
     * Clear.
     *
     * @throws SQLException SQL exception
     */
    public void clear() throws SQLException {
        closeStatements();
        getStatements().clear();
        inputGroups.clear();
        batchCount = 0;
        batchExecutionUnits.clear();
    }
    
    private void closeStatements() throws SQLException {
        for (Statement each : getStatements()) {
            each.close();
        }
    }
}


