component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.path = server._getTempDir( "search-delete-lifecycle" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );
	}

	function afterAll() {
		try { collection action="delete" collection="deleteLifeA"; } catch( any e ) {}
		try { collection action="delete" collection="deleteLifeB"; } catch( any e ) {}
		try { collection action="delete" collection="deleteLifeC"; } catch( any e ) {}
		try { collection action="delete" collection="deleteLifeD"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	private function indexDocs( collection, path ) {
		var qry = QueryNew( 'id,title,body' );
		var row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "1", row );
		QuerySetCell( qry, "title", "Alpha", row );
		QuerySetCell( qry, "body", "The quick brown fox jumps over the lazy dog", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "2", row );
		QuerySetCell( qry, "title", "Beta", row );
		QuerySetCell( qry, "body", "A lazy cat sleeps on the warm windowsill all day", row );

		index
			collection=arguments.collection
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qry"
			urlpath="/";
	}

	function run( testResults, testBox ) {
		describe( title="collection delete lifecycle", body=function() {
			it( title="delete removes the collection directory from disk", body=function() {
				collection
					action="create"
					collection="deleteLifeA"
					path="#variables.path#/colA"
					language="English";
				indexDocs( "deleteLifeA", variables.path & "/colA" );

				// The identity directory Lucene opens is CF<name><changes>, whose case folding differs
				// per platform, so assert on the children of the create path rather than its name.
				expect( ArrayLen( DirectoryList( path=variables.path & "/colA", recurse=false, listInfo="name" ) ) ).toBe( 1, "the index directory exists under the collection path before delete" );

				collection action="delete" collection="deleteLifeA";

				expect( ArrayLen( DirectoryList( path=variables.path & "/colA", recurse=false, listInfo="name" ) ) ).toBe( 0, "delete leaves no children under the collection path" );
			});

			it( title="delete succeeds right after a search completes", body=function() {
				collection
					action="create"
					collection="deleteLifeB"
					path="#variables.path#/colB"
					language="English";
				indexDocs( "deleteLifeB", variables.path & "/colB" );

				search name="local.res" collection="deleteLifeB" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 2, "should find both documents" );
				expect( res.recordssearched ).toBe( 2, "should report both documents as searched" );

				collection action="delete" collection="deleteLifeB";

				expect( ArrayLen( DirectoryList( path=variables.path & "/colB", recurse=false, listInfo="name" ) ) ).toBe( 0, "delete after a search still removes the collection directory" );
			});

			it( title="purge empties the collection without deleting the directory", body=function() {
				collection
					action="create"
					collection="deleteLifeC"
					path="#variables.path#/colC"
					language="English";
				indexDocs( "deleteLifeC", variables.path & "/colC" );

				search name="local.res" collection="deleteLifeC" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 2, "should find both documents before purge" );

				index collection="deleteLifeC" action="purge";

				expect( ArrayLen( DirectoryList( path=variables.path & "/colC", recurse=false, listInfo="name" ) ) ).toBe( 1, "purge keeps the collection directory" );
				search name="local.res" collection="deleteLifeC" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 0, "purge removes the documents" );
			});

			it( title="a collection survives an external delete and recovers through the tags", body=function() {
				collection
					action="create"
					collection="deleteLifeD"
					path="#variables.path#/colD"
					language="English";
				indexDocs( "deleteLifeD", variables.path & "/colD" );

				search name="local.res" collection="deleteLifeD" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 2, "should find both documents" );

				// Delete the index directory behind the extension's back, leaving the collection configured.
				directoryDelete( variables.path & "/colD", true );

				// Deleting the directory behind the extension's back leaves the collection configured and the
				// searcher pointing at file handles that stay valid after the names are gone. The search then
				// returns no matches rather than throwing, and reports no document count. Measured against the
				// jar before the shared SearcherManager too, so this behaviour is not introduced by reusing the
				// reader per path. See extensions/search/LDEV-6956-per-query-reader.md.
				var thrown = false;
				try {
					search name="local.res" collection="deleteLifeD" criteria="lazy" language="English";
				} catch( any e ) {
					thrown = true;
				}
				expect( thrown ).toBe( false, "searching a deleted index directory does not throw" );
				expect( res.recordcount ).toBe( 0, "searching a deleted index directory returns no matches" );

				// Deleting through the tags drops the stale searcher, so the collection can be recreated.
				collection action="delete" collection="deleteLifeD";
				collection
					action="create"
					collection="deleteLifeD"
					path="#variables.path#/colD"
					language="English";
				indexDocs( "deleteLifeD", variables.path & "/colD" );

				search name="local.res" collection="deleteLifeD" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 2, "the recreated collection searches again" );
				expect( res.recordssearched ).toBe( 2, "the recreated collection reports its document count" );
			});
		});
	}
}
