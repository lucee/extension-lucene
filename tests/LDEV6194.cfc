component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.path = server._getTempDir( "LDEV6194" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );

		collection
			action="create"
			collection="LDEV6194"
			path="#variables.path#"
			language="English";

		var qry = QueryNew( 'id,title,body' );

		var row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "1", row );
		QuerySetCell( qry, "title", "Test Document", row );
		QuerySetCell( qry, "body", "What comes after 5 in this sequence of numbers and letters AAA CCC", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "2", row );
		QuerySetCell( qry, "title", "Another Document", row );
		QuerySetCell( qry, "body", "Testing the Lucee search extension with various queries", row );

		index
			collection="LDEV6194"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qry"
			urlpath="/";
	}

	function afterAll() {
		try { collection action="delete" collection="LDEV6194"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	function run( testResults, testBox ) {
		describe( title="LDEV-6194: Verity query parser bug fixes", body=function() {

			// Fix 1: MatchAllDocsQuery for criteria="*"
			it( title="criteria=* returns all docs without NPE", body=function() {
				search name="local.res" collection="LDEV6194" criteria="*" language="English";
				expect( res.recordcount ).toBe( 2, "should return all documents" );
			});

			it( title="criteria=* with startRow", body=function() {
				search name="local.res" collection="LDEV6194" criteria="*" language="English" startRow="1" maxRows="1";
				expect( res.recordcount ).toBe( 1, "should return one document with maxRows=1" );
			});

			// Fix 2: Guard topN parameter
			it( title="search with zero results does not throw numHits error", body=function() {
				search name="local.res" collection="LDEV6194" criteria="xyznonexistent" language="English";
				expect( res.recordcount ).toBe( 0 );
				expect( isQuery( res ) ).toBeTrue();
			});

			it( title="phrase query with no matches does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria='"no such phrase exists anywhere"' language="English";
				expect( res.recordcount ).toBe( 0 );
			});

			// Fix 3: Escape special characters
			it( title="? between words does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="What comes after 5 ? AAA CCC" language="English";
				expect( isQuery( res ) ).toBeTrue( "should return a query even with ? in criteria" );
			});

			it( title="standalone * does not NPE", body=function() {
				search name="local.res" collection="LDEV6194" criteria="*" language="English";
				expect( isQuery( res ) ).toBeTrue();
			});

			it( title="unbalanced ) does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="test)" language="English";
				expect( isQuery( res ) ).toBeTrue( "should handle unbalanced )" );
			});

			it( title="unbalanced ] does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="test]" language="English";
				expect( isQuery( res ) ).toBeTrue( "should handle unbalanced ]" );
			});

			it( title="curly braces do not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="{test}" language="English";
				expect( isQuery( res ) ).toBeTrue( "should handle curly braces" );
			});

			it( title="colon in criteria does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="test:value" language="English";
				expect( isQuery( res ) ).toBeTrue( "should handle colon" );
			});

			it( title="tilde in criteria does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="test~" language="English";
				expect( isQuery( res ) ).toBeTrue( "should handle tilde" );
			});

			it( title="caret in criteria does not throw", body=function() {
				search name="local.res" collection="LDEV6194" criteria="test^2" language="English";
				expect( isQuery( res ) ).toBeTrue( "should handle caret/boost" );
			});

			// Regression: normal searches still work
			it( title="normal word search still works", body=function() {
				search name="local.res" collection="LDEV6194" criteria="Lucee" language="English";
				expect( res.recordcount ).toBe( 1 );
			});

			it( title="AND/OR/NOT still work", body=function() {
				search name="local.res" collection="LDEV6194" criteria="Lucee OR numbers" language="English";
				expect( res.recordcount ).toBe( 2 );
			});

			it( title="quoted phrase still works", body=function() {
				search name="local.res" collection="LDEV6194" criteria='"search extension"' language="English";
				expect( res.recordcount ).toBe( 1 );
			});
		});
	}
}
