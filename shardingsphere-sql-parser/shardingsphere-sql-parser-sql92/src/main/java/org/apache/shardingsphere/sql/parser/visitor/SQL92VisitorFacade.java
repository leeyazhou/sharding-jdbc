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

package org.apache.shardingsphere.sql.parser.visitor;

import org.apache.shardingsphere.sql.parser.api.visitor.DALVisitor;
import org.apache.shardingsphere.sql.parser.api.visitor.DCLVisitor;
import org.apache.shardingsphere.sql.parser.api.visitor.DDLVisitor;
import org.apache.shardingsphere.sql.parser.api.visitor.DMLVisitor;
import org.apache.shardingsphere.sql.parser.api.visitor.ParseTreeVisitorFacade;
import org.apache.shardingsphere.sql.parser.api.visitor.RLVisitor;
import org.apache.shardingsphere.sql.parser.api.visitor.TCLVisitor;
import org.apache.shardingsphere.sql.parser.visitor.impl.SQL92DALVisitor;
import org.apache.shardingsphere.sql.parser.visitor.impl.SQL92DCLVisitor;
import org.apache.shardingsphere.sql.parser.visitor.impl.SQL92DDLVisitor;
import org.apache.shardingsphere.sql.parser.visitor.impl.SQL92DMLVisitor;
import org.apache.shardingsphere.sql.parser.visitor.impl.SQL92TCLVisitor;

/**
 * Visitor facade for SQL92.
 */
public final class SQL92VisitorFacade implements ParseTreeVisitorFacade {
    
    @Override
    public Class<? extends DMLVisitor> getDMLVisitorClass() {
        return SQL92DMLVisitor.class;
    }
    
    @Override
    public Class<? extends DDLVisitor> getDDLVisitorClass() {
        return SQL92DDLVisitor.class;
    }
    
    @Override
    public Class<? extends TCLVisitor> getTCLVisitorClass() {
        return SQL92TCLVisitor.class;
    }
    
    @Override
    public Class<? extends DCLVisitor> getDCLVisitorClass() {
        return SQL92DCLVisitor.class;
    }
    
    @Override
    public Class<? extends DALVisitor> getDALVisitorClass() {
        return SQL92DALVisitor.class;
    }
    
    @Override
    public Class<? extends RLVisitor> getRLVisitorClass() {
        return null;
    }
}
