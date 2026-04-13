component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-1550: corrupt search.xml should not break the site", body=function() {

			it( title="collection operations work after creating a collection on a clean path", body=function() {
				var path = server._getTempDir( "LDEV1550-clean" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV1550clean"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Corrupt Test", row );
					QuerySetCell( qry, "body", "Testing that search survives after corrupt index files", row );

					index
						collection="LDEV1550clean"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="LDEV1550clean" criteria="corrupt" language="English";
					expect( res.recordcount ).toBe( 1 );

					collection
						action="delete"
						collection="LDEV1550clean";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="collection list works even when index directory is empty", body=function() {
				var path = server._getTempDir( "LDEV1550-empty" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV1550empty"
						path="#path#"
						language="English";

					// wipe the index files to simulate corruption
					var indexPath = path & "/LDEV1550EMPTY";
					if ( DirectoryExists( indexPath ) ) {
						directoryDelete( indexPath, true );
						directoryCreate( indexPath );
					}

					// list should still work without throwing
					collection action="list" name="local.cols";
					expect( isQuery( cols ) ).toBeTrue( "collection list should return a query" );

					collection
						action="delete"
						collection="LDEV1550empty";
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
