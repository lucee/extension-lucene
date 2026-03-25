component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-2032: collections should persist and be listed after creation", body=function() {

			it( title="collection appears in list immediately after create", body=function() {
				var path = server._getTempDir( "LDEV2032-persist" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV2032per"
						path="#path#"
						language="English";

					collection action="list" name="local.cols";
					var found = false;
					loop query="cols" {
						if ( cols.name == "LDEV2032PER" ) {
							found = true;
							break;
						}
					}
					expect( found ).toBeTrue( "collection should be in list after create" );

					collection
						action="delete"
						collection="LDEV2032per";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="multiple collections all appear in list", body=function() {
				var path = server._getTempDir( "LDEV2032-multi" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV2032mA"
						path="#path#/colA"
						language="English";

					collection
						action="create"
						collection="LDEV2032mB"
						path="#path#/colB"
						language="English";

					collection
						action="create"
						collection="LDEV2032mC"
						path="#path#/colC"
						language="English";

					collection action="list" name="local.cols";
					var names = valueList( cols.name );
					expect( names ).toInclude( "LDEV2032MA", "collection A should be listed" );
					expect( names ).toInclude( "LDEV2032MB", "collection B should be listed" );
					expect( names ).toInclude( "LDEV2032MC", "collection C should be listed" );

					collection action="delete" collection="LDEV2032mA";
					collection action="delete" collection="LDEV2032mB";
					collection action="delete" collection="LDEV2032mC";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="indexed data persists and is searchable after re-listing", body=function() {
				var path = server._getTempDir( "LDEV2032-data" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV2032dat"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Persist Doc", row );
					QuerySetCell( qry, "body", "This document should survive across list operations", row );

					index
						collection="LDEV2032dat"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					// list collections (simulates what the admin does)
					collection action="list" name="local.cols";

					// search should still work
					search name="local.res" collection="LDEV2032dat" criteria="document" language="English";
					expect( res.recordcount ).toBe( 1, "data should persist and be searchable" );

					collection
						action="delete"
						collection="LDEV2032dat";
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
