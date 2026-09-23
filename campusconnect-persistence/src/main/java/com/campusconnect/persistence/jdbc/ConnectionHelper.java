package com.campusconnect.persistence.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Hand-rolled connection helper.
 *
 * LEGACY SMELL #5 / #6 support class. It ignores the Spring-managed DataSource
 * entirely and opens its own DriverManager connection from a properties file on
 * the classpath, which means:
 *   - the JDBC work is outside any Spring transaction,
 *   - connections are not pooled,
 *   - the credentials live in a second place.
 *
 * XXX CC-1156: under load the enrollment screen exhausts MySQL's max_connections
 * because a few callers still forget to close(). The ops fix was to raise
 * max_connections.
 */
public final class ConnectionHelper {

    private static final Logger LOG = Logger.getLogger(ConnectionHelper.class);

    private static final String PROPS_FILE = "jdbc.properties";

    private static String url;
    private static String user;
    private static String password;
    private static boolean initialized = false;

    private ConnectionHelper() {
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        Properties props = new Properties();
        try {
            props.load(ConnectionHelper.class.getClassLoader().getResourceAsStream(PROPS_FILE));
            Class.forName(props.getProperty("jdbc.driverClassName"));
            url = props.getProperty("jdbc.url");
            user = props.getProperty("jdbc.username");
            password = props.getProperty("jdbc.password");
        } catch (Exception e) {
            // LEGACY SMELL #4: swallowed. The first getConnection() then fails with
            // a null URL and the stack trace points here, not at the real problem.
            e.printStackTrace();
        }
        initialized = true;
    }

    public static Connection getConnection() throws SQLException {
        init();
        LOG.debug("Opening raw JDBC connection to " + url);
        return DriverManager.getConnection(url, user, password);
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
