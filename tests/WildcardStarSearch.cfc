component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="Bug: NPE when using wildcard * criteria with cfsearch", body=function() {

			it( title="wildcard * criteria returns all documents", body=function() {
				var path = server._getTempDir( "wildcard-star-basic" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="wildcardStarBasic"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Alpha", row );
					QuerySetCell( qry, "body", "First document about apples", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "2", row );
					QuerySetCell( qry, "title", "Beta", row );
					QuerySetCell( qry, "body", "Second document about bananas", row );

					index
						collection="wildcardStarBasic"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="wildcardStarBasic" criteria="*" language="English";
					expect( res.recordcount ).toBe( 2, "wildcard * should return all documents" );

					collection action="delete" collection="wildcardStarBasic";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="wildcard * criteria with startRow does not throw NPE", body=function() {
				var path = server._getTempDir( "wildcard-star-startrow" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="wildcardStarPage"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Alpha", row );
					QuerySetCell( qry, "body", "First document about cats", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "2", row );
					QuerySetCell( qry, "title", "Beta", row );
					QuerySetCell( qry, "body", "Second document about dogs", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "3", row );
					QuerySetCell( qry, "title", "Gamma", row );
					QuerySetCell( qry, "body", "Third document about birds", row );

					index
						collection="wildcardStarPage"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					// this combination triggers NPE: criteria="*" + startRow
					search
						name="local.res"
						collection="wildcardStarPage"
						criteria="*"
						startRow="1"
						maxRows="2"
						language="English";

					expect( isQuery( res ) ).toBeTrue( "should return a query, not throw NPE" );
					expect( res.recordcount ).toBeGTE( 1, "should return at least one result" );

					collection action="delete" collection="wildcardStarPage";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="wildcard * criteria with maxRows does not throw NPE", body=function() {
				var path = server._getTempDir( "wildcard-star-maxrows" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="wildcardStarMax"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "One", row );
					QuerySetCell( qry, "body", "Document one content", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "2", row );
					QuerySetCell( qry, "title", "Two", row );
					QuerySetCell( qry, "body", "Document two content", row );

					index
						collection="wildcardStarMax"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="wildcardStarMax"
						criteria="*"
						maxRows="1"
						language="English";

					expect( isQuery( res ) ).toBeTrue( "should return a query, not throw NPE" );
					expect( res.recordcount ).toBe( 1, "maxRows should limit results" );

					collection action="delete" collection="wildcardStarMax";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});
		});
	}
}
