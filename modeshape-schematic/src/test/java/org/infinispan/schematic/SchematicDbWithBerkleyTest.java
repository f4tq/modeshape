package org.infinispan.schematic;

import java.io.File;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.loaders.bdbje.BdbjeCacheStoreConfig;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings( "deprecation" )
public class SchematicDbWithBerkleyTest {

    private SchematicDb db;
    private EmbeddedCacheManager cm;

    @Before
    public void beforeTest() {
        File dbDir = new File("target/bdb");
        TestUtil.delete(dbDir);

        GlobalConfiguration global = new GlobalConfiguration();
        global = global.fluent().serialization().addAdvancedExternalizer(Schematic.externalizers()).build();

        BdbjeCacheStoreConfig berkleyConfig = new BdbjeCacheStoreConfig();
        berkleyConfig.setLocation(dbDir.getAbsolutePath());
        Configuration c = new Configuration();
        c = c.fluent()
             .invocationBatching()
             .transaction()
             .transactionManagerLookup(new DummyTransactionManagerLookup())
             .loaders()
             .addCacheLoader(berkleyConfig)
             .build();
        cm = TestCacheManagerFactory.createCacheManager(global, c, true);
        // Now create the SchematicDb ...
        db = Schematic.get(cm, "documents");
    }

    @After
    public void afterTest() {
        TestingUtil.killCacheManagers(cm);
        db = null;
    }

    @Test
    public void shouldGetNonExistantDocument() {
        String key = "can be anything";
        SchematicEntry entry = db.get(key);
        assert entry == null : "Should not have found a prior entry";
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, null);
        assert prior == null : "Should not have found a prior entry";
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";

        // Verify the content ...
        Document read = entry.getContentAsDocument();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert entry.getContentAsBinary() == null : "Should not have a Binary value for the entry's content";
        assert read.containsAll(doc);
        assert read.equals(doc);

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assert readMetadata != null;
        assert readMetadata.getString("id").equals(key);
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNonNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        Document metadata = Schematic.newDocument("mimeType", "text/plain");
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, metadata);
        assert prior == null : "Should not have found a prior entry";

        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";

        // Verify the content ...
        Document read = entry.getContentAsDocument();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert entry.getContentAsBinary() == null : "Should not have a Binary value for the entry's content";
        assert read.containsAll(doc);
        assert read.equals(doc);

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assert readMetadata != null;
        assert readMetadata.getString("mimeType").equals(metadata.getString("mimeType"));
        assert readMetadata.containsAll(metadata);
        // metadata contains more than what we specified ...
        assert !readMetadata.equals(metadata) : "Expected:\n" + metadata + "\nFound: \n" + readMetadata;
    }

    @Test
    public void shouldStoreDocumentAndFetchAndModifyAndRefetch() throws Exception {
        // Store the document ...
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        Document metadata = Schematic.newDocument("mimeType", "text/plain");
        String key = "can be anything";
        SchematicEntry prior = db.put(key, doc, metadata);
        assert prior == null : "Should not have found a prior entry";

        // Read back from the database ...
        SchematicEntry entry = db.get(key);
        assert entry != null : "Should have found the entry";

        // Verify the content ...
        Document read = entry.getContentAsDocument();
        assert read != null;
        assert "value1".equals(read.getString("k1"));
        assert 2 == read.getInteger("k2");
        assert entry.getContentAsBinary() == null : "Should not have a Binary value for the entry's content";
        assert read.containsAll(doc);
        assert read.equals(doc);

        // Modify using an editor ...
        try {
            // tm.begin();
            EditableDocument editable = entry.editDocumentContent();
            editable.setBoolean("k3", true);
            editable.setNumber("k4", 3.5d);
        } finally {
            // tm.commit();
        }

        // Now re-read ...
        SchematicEntry entry2 = db.get(key);
        Document read2 = entry2.getContentAsDocument();
        assert read2 != null;
        assert "value1".equals(read2.getString("k1"));
        assert 2 == read2.getInteger("k2");
        assert true == read2.getBoolean("k3");
        assert 3.4d < read2.getDouble("k4");
    }
}
