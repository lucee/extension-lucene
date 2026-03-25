component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-3310: cfcollection delete should not leave locked files on Windows", body=function() {

			it( title="collection delete removes index directory cleanly", body=function() {
				var path = server._getTempDir( "LDEV3310-delete" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				collection
					action="create"
					collection="LDEV3310del"
					path="#path#"
					language="English";

				var qry = QueryNew( 'id,title,body' );
				var row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "1", row );
				QuerySetCell( qry, "title", "Lock Test", row );
				QuerySetCell( qry, "body", "Testing that index files are not locked after delete", row );

				index
					collection="LDEV3310del"
					action="update"
					type="custom"
					title="title"
					body="body"
					key="id"
					query="qry"
					urlpath="/";

				// verify the index was created
				search name="local.res" collection="LDEV3310del" criteria="locked" language="English";
				expect( res.recordcount ).toBe( 1 );

				collection
					action="delete"
					collection="LDEV3310del";

				// the index directory should be deletable (no locked files)
				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				expect( DirectoryExists( path ) ).toBeFalse( "directory should be fully deleted without locked files" );
			});

			it( title="collection delete followed by recreate works", body=function() {
				var path = server._getTempDir( "LDEV3310-recreate" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV3310rec"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "First", row );
					QuerySetCell( qry, "body", "First version of the collection", row );

					index
						collection="LDEV3310rec"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					collection
						action="delete"
						collection="LDEV3310rec";

					// recreate on same path
					collection
						action="create"
						collection="LDEV3310rec"
						path="#path#"
						language="English";

					var qry2 = QueryNew( 'id,title,body' );
					row = QueryAddRow( qry2 );
					QuerySetCell( qry2, "id", "2", row );
					QuerySetCell( qry2, "title", "Second", row );
					QuerySetCell( qry2, "body", "Recreated collection content", row );

					index
						collection="LDEV3310rec"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry2"
						urlpath="/";

					search name="local.res" collection="LDEV3310rec" criteria="Recreated" language="English";
					expect( res.recordcount ).toBe( 1, "recreated collection should be searchable" );

					collection
						action="delete"
						collection="LDEV3310rec";
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
