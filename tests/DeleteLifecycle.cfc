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

				expect( DirectoryExists( variables.path & "/colA/CFdeleteLifeA0" ) ).toBe( true, "the collection index directory exists before delete" );

				collection action="delete" collection="deleteLifeA";

				expect( DirectoryExists( variables.path & "/colA/CFdeleteLifeA0" ) ).toBe( false, "delete removes the collection directory on disk" );
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

				expect( DirectoryExists( variables.path & "/colC/CFdeleteLifeC0" ) ).toBe( true, "purge keeps the collection directory" );
				search name="local.res" collection="deleteLifeC" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 0, "purge removes the documents" );
			});
		});
	}
}
