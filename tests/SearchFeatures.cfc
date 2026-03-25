component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.path = server._getTempDir( "search-features" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );

		collection
			action="create"
			collection="searchFeatA"
			path="#variables.path#/colA"
			language="English";

		collection
			action="create"
			collection="searchFeatB"
			path="#variables.path#/colB"
			language="English";

		var qryA = QueryNew( 'id,title,body' );
		var row = QueryAddRow( qryA );
		QuerySetCell( qryA, "id", "1", row );
		QuerySetCell( qryA, "title", "Alpha", row );
		QuerySetCell( qryA, "body", "The quick brown fox jumps over the lazy dog", row );

		var qryB = QueryNew( 'id,title,body' );
		row = QueryAddRow( qryB );
		QuerySetCell( qryB, "id", "2", row );
		QuerySetCell( qryB, "title", "Beta", row );
		QuerySetCell( qryB, "body", "A lazy cat sleeps on the warm windowsill all day", row );

		index
			collection="searchFeatA"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qryA"
			urlpath="/";

		index
			collection="searchFeatB"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qryB"
			urlpath="/";
	}

	function afterAll() {
		try { collection action="delete" collection="searchFeatA"; } catch( any e ) {}
		try { collection action="delete" collection="searchFeatB"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	function run( testResults, testBox ) {
		describe( title="cfsearch features", body=function() {

			it( title="search across multiple collections", body=function() {
				search name="local.res" collection="searchFeatA,searchFeatB" criteria="lazy" language="English";
				expect( res.recordcount ).toBe( 2, "should find results across both collections" );
			});

			it( title="search results contain expected columns", body=function() {
				search name="local.res" collection="searchFeatA" criteria="fox" language="English";
				expect( res.recordcount ).toBe( 1 );

				var cols = res.columnList;
				expect( cols ).toInclude( "title", "results should include title column" );
				expect( cols ).toInclude( "key", "results should include key column" );
				expect( cols ).toInclude( "score", "results should include score column" );
			});

			it( title="search with no matches returns empty query", body=function() {
				search name="local.res" collection="searchFeatA" criteria="xyznonexistent" language="English";
				expect( res.recordcount ).toBe( 0, "should return empty result set for no matches" );
				expect( isQuery( res ) ).toBeTrue( "should still return a query object" );
			});

			it( title="startRow past end of results returns empty", body=function() {
				search name="local.res" collection="searchFeatA" criteria="fox" language="English"
					startRow=999 maxRows=10;
				expect( res.recordcount ).toBe( 0, "startRow past results should return empty, not crash" );
			});

			it( title="context highlight markers", body=function() {
				search
					name="local.res"
					collection="searchFeatA"
					criteria="fox"
					language="English"
					contextpassages="1"
					contextBytes="500"
					contextpassageLength="200"
					contextHighlightBegin="<mark>"
					contextHighlightEnd="</mark>";
				expect( res.recordcount ).toBe( 1 );

				var cols = res.columnList;
				expect( cols ).toInclude( "context", "results should include context column" );
			});
		});
	}
}
