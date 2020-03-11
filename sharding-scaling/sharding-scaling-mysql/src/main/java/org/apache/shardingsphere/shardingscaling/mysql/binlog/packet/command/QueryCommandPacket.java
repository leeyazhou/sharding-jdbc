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

package org.apache.shardingsphere.shardingscaling.mysql.binlog.packet.command;

import org.apache.shardingsphere.shardingscaling.mysql.binlog.codec.DataTypesCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

/**
 * MySQL Query command packet.
 *
 * @deprecated Replaced with {@link org.apache.shardingsphere.database.protocol.mysql.packet.command.query.text.query.MySQLComQueryPacket}
 */
@Setter
@Deprecated
public final class QueryCommandPacket extends AbstractCommandPacket {
    
    private String queryString;
    
    public QueryCommandPacket() {
        setCommand((byte) 0x03);
    }
    
    @Override
    public ByteBuf toByteBuf() {
        ByteBuf result = ByteBufAllocator.DEFAULT.heapBuffer();
        DataTypesCodec.writeByte(getCommand(), result);
        DataTypesCodec.writeBytes(queryString.getBytes(StandardCharsets.UTF_8), result);
        return result;
    }
}
