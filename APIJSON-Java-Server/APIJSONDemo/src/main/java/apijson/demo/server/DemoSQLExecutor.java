/*Copyright ©2016 TommyLemon(https://github.com/TommyLemon/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.demo.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.validation.constraints.NotNull;

import zuo.biao.apijson.Log;
import zuo.biao.apijson.server.AbstractSQLExecutor;
import zuo.biao.apijson.server.SQLConfig;


/**executor for query(read) or update(write) MySQL database
 * @author Lemon
 */
public class DemoSQLExecutor extends AbstractSQLExecutor {
	private static final String TAG = "DemoSQLExecutor";


	static {
		try { //加载驱动程序
			Class.forName("com.mysql.jdbc.Driver");
			Log.d(TAG, "成功加载 MySQL 驱动！");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try { //加载驱动程序
			Class.forName("org.postgresql.Driver");
			Log.d(TAG, "成功加载 PostgresSQL 驱动！");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}




	@Override
	public ResultSet executeQuery(@NotNull SQLConfig config) throws Exception {
		return getStatement(config).executeQuery();
	}

	@Override
	public int executeUpdate(@NotNull SQLConfig config) throws Exception {
		return getStatement(config).executeUpdate();
	}


	//TODO 根据不同数据库来分组存 connection 和 statement，例如 Map<database, connection>，
	//	解决一次请求中有2个以上不同数据库类型导致后面的查询都用第一个的数据库类型
	private Connection connection = null;
	private PreparedStatement statement = null;
	/**
	 * @param config 
	 * @return
	 * @throws Exception
	 */
	private PreparedStatement getStatement(@NotNull SQLConfig config) throws Exception {
		if (connection == null || connection.isClosed()) {
			Log.i(TAG, "select  connection " + (connection == null ? " = null" : ("isClosed = " + connection.isClosed()))) ;

			if (DemoSQLConfig.DATABASE_POSTGRESQL.equalsIgnoreCase(config.getDatabase())) { //PostgreSQL 不允许 cross-database
				connection = DriverManager.getConnection(config.getDBUri() + "/" + config.getSchema(), config.getDBAccount(), config.getDBPassword());
			}
			else {
				connection = DriverManager.getConnection(config.getDBUri() + "?useUnicode=true&characterEncoding=UTF-8&user="
						+ config.getDBAccount() + "&password=" + config.getDBPassword());
			}
		}

		statement = connection.prepareStatement(config.getSQL(config.isPrepared())); //创建Statement对象
		List<Object> valueList = config.isPrepared() ? config.getPreparedValueList() : null;

		if (valueList != null && valueList.isEmpty() == false) {

			for (int i = 0; i < valueList.size(); i++) {

				if (DemoSQLConfig.DATABASE_POSTGRESQL.equalsIgnoreCase(config.getDatabase())) {
					statement.setObject(i + 1, valueList.get(i)); //PostgreSQL JDBC 不支持隐式类型转换 tinyint = varchar 报错
				}
				else {
					statement.setString(i + 1, "" + valueList.get(i)); //MySQL setObject 不支持 JSON 类型
				}
			}
		}

		return statement;
	}


	/**关闭连接，释放资源
	 */
	@Override
	public void close() {
		super.close();
		try {
			if (statement != null && statement.isClosed() == false) {
				statement.close();
			}
			if (connection != null && connection.isClosed() == false) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		statement = null;
		connection = null;
	}



}
