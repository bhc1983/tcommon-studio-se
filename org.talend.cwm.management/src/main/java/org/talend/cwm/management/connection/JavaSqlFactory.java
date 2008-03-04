// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.cwm.management.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.cwm.softwaredeployment.TdProviderConnection;
import org.talend.utils.sql.ConnectionUtils;
import org.talend.utils.sugars.ReturnCode;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.objectmodel.core.TaggedValue;

/**
 * @author scorreia
 * 
 * This utility class provides methods that convert CWM object into java.sql object. It is a kind of inverse of the
 * DatabaseContentRetriever class.
 */
public final class JavaSqlFactory {

    private JavaSqlFactory() {
    }

    /**
     * Method "createConnection" returns the connection with {@link ReturnCode#getObject()} if {@link ReturnCode#isOk()}
     * is true. This is the behaviour when everything goes ok.
     * <p>
     * When something goes wrong, {@link ReturnCode#isOk()} is false and {@link ReturnCode#getMessage()} gives the error
     * message.
     * 
     * @param providerConnection the provider connection
     * @return a ReturnCode (never null)
     */
    public static TypedReturnCode<Connection> createConnection(TdProviderConnection providerConnection) {
        TypedReturnCode<Connection> rc = new TypedReturnCode<Connection>(false);
        String url = providerConnection.getConnectionString();
        if (url == null) {
            rc.setMessage("Database connection string is null");
            rc.setOk(false);
        }
        String driverClassName = providerConnection.getDriverClassName();
        Collection<TaggedValue> taggedValues = providerConnection.getTaggedValue();
        Properties props = TaggedValueHelper.createProperties(taggedValues);
        try {
            Connection connection = ConnectionUtils.createConnection(url, driverClassName, props);
            rc.setObject(connection);
            rc.setOk(true);
        } catch (SQLException e) {
            rc.setReturnCode(e.getMessage(), false);
        } catch (InstantiationException e) {
            rc.setReturnCode(e.getMessage(), false);
        } catch (IllegalAccessException e) {
            rc.setReturnCode(e.getMessage(), false);
        } catch (ClassNotFoundException e) {
            rc.setReturnCode(e.getMessage(), false);
        }
        return rc;
    }
}
