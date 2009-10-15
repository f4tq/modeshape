/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.json;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import org.jboss.dna.web.jcr.rest.client.IRestClient;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;
import org.junit.Before;
import org.junit.Test;

/**
 * The <code>JsonRestClientTest</code> class is a test class for the {@link JsonRestClient JSON REST client} object.
 * <p>
 * One container: mvn -P cargo-1 clean install assembly:assembly
 * <p>
 * Two containers: mvn -P cargo-1,cargo-2 clean install assembly:assembly
 */
public final class JsonRestClientTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    // user and password configured in pom
    private static final String PSWD = "password"; //$NON-NLS-1$
    private static final String USER = "dnauser"; //$NON-NLS-1$

    private static final Server SERVER = new Server("http://localhost:8080", USER, PSWD, false); //$NON-NLS-1$
    private static final String REPOSITORY_NAME = "dna:repository"; //$NON-NLS-1$
    private static final Repository REPOSITORY1 = new Repository(REPOSITORY_NAME, SERVER);
    private static final String WORKSPACE_NAME = "default"; //$NON-NLS-1$
    private static final Workspace WORKSPACE1 = new Workspace(WORKSPACE_NAME, REPOSITORY1);

    private static final String WORKSPACE_PATH = "/myproject/myfolder/"; //$NON-NLS-1$
    private static final String FILE_PATH = WORKSPACE_PATH + "document.txt"; //$NON-NLS-1$
    private static final String BINARY_FILE_PATH = WORKSPACE_PATH + "picture.jpg"; //$NON-NLS-1$

    private static final String WORKSPACE_UNUSUALPATH = "/myproject/My.Test - Folder/"; //$NON-NLS-1$
    private static final String FILE_UNUSUALPATH = WORKSPACE_UNUSUALPATH + "Test File_.a-().txt"; //$NON-NLS-1$

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private IRestClient restClient;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Before
    public void beforeEach() {
        this.restClient = new JsonRestClient();
    }

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldGetRepositories() throws Exception {
        Collection<Repository> repositories = this.restClient.getRepositories(SERVER);
        assertThat(repositories.size(), equalTo(1));
        assertThat(repositories.iterator().next(), is(REPOSITORY1));
    }

    @Test
    public void shouldGetWorkspaces() throws Exception {
        Collection<Workspace> workspaces = this.restClient.getWorkspaces(REPOSITORY1);
        assertThat(workspaces.size(), is(1));
        assertThat(workspaces.iterator().next(), is(WORKSPACE1));
    }

    @Test
    public void shouldNotUnpublishNonexistentFile() throws Exception {
        File file = new File("bogusfile"); //$NON-NLS-1$
        Status status = this.restClient.unpublish(WORKSPACE1, WORKSPACE_PATH, file);

        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
        }

        assertThat(status.getMessage(), status.isInfo(), is(true));
    }

    @Test
    public void shouldPublishBinaryResource() throws Exception {
        URL binaryFile = getClass().getResource(BINARY_FILE_PATH);
        assertThat(binaryFile, is(notNullValue()));

        // publish
        File file = new File(binaryFile.toURI());
        Status status = this.restClient.publish(WORKSPACE1, WORKSPACE_PATH, file);

        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
        }

        assertThat(status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(WORKSPACE1, WORKSPACE_PATH, file), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(WORKSPACE1, WORKSPACE_PATH, file).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(WORKSPACE1, WORKSPACE_PATH, file);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldPublishTextResource() throws Exception {
        URL textFile = getClass().getResource(FILE_PATH);
        assertThat(textFile, is(notNullValue()));

        // publish
        File file = new File(textFile.toURI());
        Status status = this.restClient.publish(WORKSPACE1, WORKSPACE_PATH, file);

        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
        }

        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(WORKSPACE1, WORKSPACE_PATH, file), is(true));

        // compare file contents to the contents that have been published
        String expected = new FileNode(WORKSPACE1, WORKSPACE_PATH, file).readFile();
        String actual = ((JsonRestClient)this.restClient).getFileContents(WORKSPACE1, WORKSPACE_PATH, file);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldPublishResourcesHavingNonLettersNonNumbersInName() throws Exception {
        URL textFile = getClass().getResource(FILE_UNUSUALPATH);
        assertThat(textFile, is(notNullValue()));

        // publish
        File file = new File(textFile.toURI());
        Status status = this.restClient.publish(WORKSPACE1, WORKSPACE_UNUSUALPATH, file);

        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
        }

        assertThat(status.getMessage(), status.isOk(), is(true));

        // confirm it exists in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(WORKSPACE1, WORKSPACE_UNUSUALPATH, file), is(true));
    }

    @Test
    public void shouldUnpublish() throws Exception {
        // first publish
        shouldPublishTextResource();

        URL textFile = getClass().getResource(FILE_PATH);
        File file = new File(textFile.toURI());
        Status status = this.restClient.unpublish(WORKSPACE1, WORKSPACE_PATH, file);

        if (status.isError()) {
            System.err.println(status.getMessage());
            status.getException().printStackTrace(System.err);
        }

        assertThat(status.isOk(), is(true));

        // confirm it does not exist in repository
        assertThat(((JsonRestClient)this.restClient).pathExists(WORKSPACE1, WORKSPACE_PATH, file), is(false));
    }

}