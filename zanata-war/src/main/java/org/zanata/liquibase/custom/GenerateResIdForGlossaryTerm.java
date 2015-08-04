/*
 * Copyright 2015, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.zanata.liquibase.custom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.zanata.common.LocaleId;
import org.zanata.common.util.GlossaryUtil;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class GenerateResIdForGlossaryTerm implements CustomTaskChange {

    @Override
    public String getConfirmationMessage() {
        return "GenerateResIdForGlossaryTerm generated resId column in HGlossaryTerm table";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        final JdbcConnection conn = (JdbcConnection) database.getConnection();
        try (Statement stmt =
                conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_UPDATABLE)) {


            Map<Long, String> termLocaleMap = new HashMap<Long, String>();
            String termLocaleSql =
                "select term.id, locale.localeId from HGlossaryTerm term, HLocale locale where term.localeId = locale.id and term.resId = ''";
            ResultSet rs1 = stmt.executeQuery(termLocaleSql);
            while (rs1.next()) {
                long termId = rs1.getLong(1);
                String localeId = rs1.getString(2);
                termLocaleMap.put(termId, localeId);
            }

            String termSql =
                    "select term.id, term.content, term.resId from HGlossaryTerm term where term.resId = ''";
            ResultSet rs2 = stmt.executeQuery(termSql);

            while (rs2.next()) {
                long id = rs2.getLong(1);
                String content = rs2.getString(2);
                LocaleId localeId = new LocaleId(termLocaleMap.get(id));
                String resId = GlossaryUtil.getResId(localeId, content);

                rs2.updateString(3, resId);
                rs2.updateRow();
            }
        } catch (SQLException e) {
            throw new CustomChangeException(e);
        } catch (DatabaseException e) {
            throw new CustomChangeException(e);
        }
    }
}
