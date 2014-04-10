/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.feature.document;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zanata.feature.Feature;
import org.zanata.feature.testharness.ZanataTestCase;
import org.zanata.feature.testharness.TestPlan.DetailedTest;
import org.zanata.page.projectversion.VersionDocumentsPage;
import org.zanata.page.projectversion.VersionLanguagesPage;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.util.CleanDocumentStorageRule;
import org.zanata.util.SampleProjectRule;
import org.zanata.util.TestFileGenerator;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.zanata.util.FunctionalTestHelper.assumeTrue;

/**
 * @author Damian Jansen
 * <a href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Category(DetailedTest.class)
@Feature(bugzilla = 980670, summary = "As a new maintainer, I wish to have my HTML files supported for translation by Zanata")
public class HTMLDocumentTypeTest extends ZanataTestCase {

    @Rule
    public SampleProjectRule sampleProjectRule = new SampleProjectRule();

    @Rule
    public CleanDocumentStorageRule documentStorageRule =
            new CleanDocumentStorageRule();

    private TestFileGenerator testFileGenerator = new TestFileGenerator();

    @Before
    public void before() {
        new BasicWorkFlow().goToHome().deleteCookiesAndRefresh();
        String documentStorageDirectory = CleanDocumentStorageRule
                .getDocumentStoragePath()
                .concat(File.separator).concat("documents")
                .concat(File.separator);
        File docStorage = new File(documentStorageDirectory);
        assumeTrue("The storage folder is empty",
                docStorage == null ||
                        !docStorage.exists() ||
                        docStorage.listFiles().length == 0);
    }

    @Test(timeout = ZanataTestCase.MAX_SHORT_TEST_DURATION)
    public void uploadHTMLFile() {
        File htmlfile = testFileGenerator
                .generateTestFileWithContent("testhtmlfile", ".html",
                "<html><title>Test content</title><br>" +
                "This is <b>Bold</b> text</html>");
        String testFileName = htmlfile.getName();
        String successfullyUploaded = "Document " + testFileName + " uploaded.";
        VersionLanguagesPage projectVersionPage = new LoginWorkFlow()
                .signIn("admin", "admin")
                .goToProjects()
                .goToProject("about fedora")
                .gotoVersion("master")
                .gotoSettingsTab()
                .gotoSettingsDocumentsTab()
                .pressUploadFileButton()
                .enterFilePath(htmlfile.getAbsolutePath())
                .submitUpload();

        assertThat(projectVersionPage.expectNotification(successfullyUploaded))
                .isTrue()
                .as("Document uploaded notification shows");

        VersionDocumentsPage versionDocumentsPage =
                projectVersionPage.gotoDocumentTab();

        assertThat(versionDocumentsPage.sourceDocumentsContains(htmlfile.getName()))
                .isTrue()
                .as("Document shows in table");

        EditorPage editorPage =
                projectVersionPage.goToProjects().goToProject("about fedora")
                        .gotoVersion("master").translate("pl", testFileName);

        assertThat(editorPage.getMessageSourceAtRowIndex(0))
                .isEqualTo("Test content")
                .as("The first translation source is correct");
        assertThat(editorPage.getMessageSourceAtRowIndex(1))
                .isEqualTo("This is <g2>Bold</g2> text")
                .as("The second translation source is correct");
    }
}
