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

package org.apache.shardingsphere.underlying.executor.sql.jdbc.group;

import org.apache.shardingsphere.underlying.executor.context.ExecutionUnit;
import org.apache.shardingsphere.underlying.executor.group.ExecuteGroupEngine;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.StatementExecuteUnit;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.connection.ConnectionMode;
import org.apache.shardingsphere.underlying.executor.sql.jdbc.connection.ExecutionConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Execute group engine for prepared statement.
 */
public final class PreparedStatementExecuteGroupEngine extends ExecuteGroupEngine<StatementExecuteUnit, StatementOption> {
    
    public PreparedStatementExecuteGroupEngine(final int maxConnectionsSizePerQuery) {
        super(maxConnectionsSizePerQuery);
    }
    
    @Override
    protected StatementExecuteUnit createStorageResourceExecuteUnit(final ExecutionUnit executionUnit, final ExecutionConnection executionConnection, final Connection connection, 
                                                                    final ConnectionMode connectionMode, final StatementOption statementOption) throws SQLException {
        PreparedStatement preparedStatement = createPreparedStatement(
                executionUnit.getSqlUnit().getSql(), executionUnit.getSqlUnit().getParameters(), executionConnection, connection, connectionMode, statementOption);
        return new StatementExecuteUnit(executionUnit, preparedStatement, connectionMode);
    }
    
    private PreparedStatement createPreparedStatement(final String sql, final List<Object> parameters, final ExecutionConnection executionConnection, final Connection connection,
                                                final ConnectionMode connectionMode, final StatementOption statementOption) throws SQLException {
        return executionConnection.createPreparedStatement(sql, parameters, connection, connectionMode, statementOption);
    }
}
