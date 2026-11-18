component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.path = server._getTempDir( "search-records-searched" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );

		collection
			action="create"
			collection="recordsSearchedA"
			path="#variables.path#/colA"
			language="English";

		collection
			action="create"
			collection="recordsSearchedB"
			path="#variables.path#/colB"
			language="English";

		var qryA = QueryNew( 'id,title,body' );
		var row = QueryAddRow( qryA );
		QuerySetCell( qryA, "id", "1", row );
		QuerySetCell( qryA, "title", "Alpha", row );
		QuerySetCell( qryA, "body", "The quick brown fox jumps over the lazy dog", row );

		row = QueryAddRow( qryA );
		QuerySetCell( qryA, "id", "2", row );
		QuerySetCell( qryA, "title", "Alpha two", row );
		QuerySetCell( qryA, "body", "A lazy cat sleeps on the warm windowsill all day", row );

		row = QueryAddRow( qryA );
		QuerySetCell( qryA, "id", "3", row );
		QuerySetCell( qryA, "title", "Alpha three", row );
		QuerySetCell( qryA, "body", "Search engines rank results by relevance", row );

		index
			collection="recordsSearchedA"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qryA"
			urlpath="/";

		var qryB = QueryNew( 'id,title,body' );
		row = QueryAddRow( qryB );
		QuerySetCell( qryB, "id", "1", row );
		QuerySetCell( qryB, "title", "Beta", row );
		QuerySetCell( qryB, "body", "Another lazy horse eats slowly", row );

		row = QueryAddRow( qryB );
		QuerySetCell( qryB, "id", "2", row );
		QuerySetCell( qryB, "title", "Beta two", row );
		QuerySetCell( qryB, "body", "Vector search blends keyword scores", row );

		index
			collection="recordsSearchedB"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qryB"
			urlpath="/";
	}

	function afterAll() {
		try { collection action="delete" collection="recordsSearchedA"; } catch( any e ) {}
		try { collection action="delete" collection="recordsSearchedB"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	function run( testResults, testBox ) {
		describe( title="cfsearch recordssearched", body=function() {
			it( title="recordssearched reports the indexed document count of one collection", body=function() {
				search name="local.res" collection="recordsSearchedA" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 2, "should find two documents" );
				expect( res.recordssearched ).toBe( 3, "one collection reports its own document count" );
			});

			it( title="recordssearched sums across collections", body=function() {
				search name="local.res" collection="recordsSearchedA,recordsSearchedB" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 3, "should find results across both collections" );
				expect( res.recordssearched ).toBe( 5, "two collections report the sum of their document counts" );
			});

			it( title="status searched matches recordssearched", body=function() {
				search name="local.res" collection="recordsSearchedA,recordsSearchedB" criteria="lazy" status="local.sct" language="English";
				expect( sct.searched ).toBe( 5, "status.searched reports the same count" );
				expect( sct.found ).toBe( 3, "status.found reports the result count" );
			});
		});
	}
}
