/*-
 * <<
 * DBus
 * ==
 * Copyright (C) 2016 - 2017 Bridata
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

package com.creditease.dbus.ws.service.schema;

import com.creditease.dbus.enums.DbusDatasourceType;
import com.creditease.dbus.ws.domain.*;

import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.Map;

public abstract class SchemaFetcher {
    private DbusDataSource ds;
    private Connection conn;

    public SchemaFetcher(DbusDataSource ds) {
        this.ds = ds;
    }

    public abstract String buildQuery(Object... args);
    public abstract String fillParameters(PreparedStatement statement, Map<String, Object> params) throws Exception;

    public List<DataSchema> fetchSchema(Map<String, Object> params) throws Exception {
        try {
            PreparedStatement statement = conn.prepareStatement(buildQuery(params));
            fillParameters(statement, params);
            ResultSet resultSet = statement.executeQuery();
                return buildResultMySQLAndOracle(resultSet);
        } finally {
            if (!conn.isClosed()) {
                conn.close();
            }
        }
    }

    public static SchemaFetcher getFetcher(DbusDataSource ds) throws Exception {
        SchemaFetcher fetcher;
        DbusDatasourceType dsType = DbusDatasourceType.parse(ds.getDsType());
        switch (dsType) {
            case MYSQL:
                Class.forName("com.mysql.jdbc.Driver");
                fetcher = new MySqlSchemaFetcher(ds);
                break;
            case ORACLE:
                Class.forName("oracle.jdbc.driver.OracleDriver");
                fetcher = new OracleSchemaFetcher(ds);
                break;
            default:
                throw new IllegalArgumentException();
        }
        Connection conn = DriverManager.getConnection(ds.getMasterURL(), ds.getDbusUser(), ds.getDbusPassword());
        fetcher.setConnection(conn);
        return fetcher;
    }

    protected void setConnection(Connection conn) {
        this.conn = conn;
    }

    protected List<DataSchema> buildResultMySQLAndOracle(ResultSet rs) throws SQLException {
        List<DataSchema> list = new ArrayList<>();
        DataSchema schema;
        ResultSetMetaData rsm = rs.getMetaData();
        int col = rsm.getColumnCount();
        String colName = "";
        for(int i = 0;i<col;i++)
        {
            colName = rsm.getColumnName(i + 1);
        }
       // System.out.println(colName);
        while (rs.next()) {
            schema = new DataSchema();
            if("USERNAME".equals(colName))
            {
                schema.setSchemaName(rs.getString("USERNAME"));
            }
            else //if("TABLE_SCHEMA".equals(colName))
            {
                schema.setSchemaName(rs.getString("TABLE_SCHEMA"));
            }
            schema.setStatus("active");
            list.add(schema);
        }
        return list;
    }

}
