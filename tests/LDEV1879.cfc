component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-1879: NPE on cfcollection/cfindex operations", body=function() {

			it( title="cfcollection create does not throw NPE", body=function() {
				var path = server._getTempDir( "LDEV1879-create" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV1879create"
						path="#path#"
						language="English";

					collection action="list" name="local.cols";
					var found = false;
					loop query="cols" {
						if ( cols.name == "LDEV1879CREATE" ) {
							found = true;
							break;
						}
					}
					expect( found ).toBeTrue( "collection should appear in list after create" );

					collection
						action="delete"
						collection="LDEV1879create";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="cfindex and cfsearch work after collection create", body=function() {
				var path = server._getTempDir( "LDEV1879-index" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV1879idx"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Test Doc", row );
					QuerySetCell( qry, "body", "CFCOLLECTION and CFINDEX should work without NPE", row );

					index
						collection="LDEV1879idx"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="LDEV1879idx" criteria="CFINDEX" language="English";
					expect( res.recordcount ).toBe( 1, "search should find indexed document" );

					collection
						action="delete"
						collection="LDEV1879idx";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="optimize does not throw NPE", body=function() {
				var path = server._getTempDir( "LDEV1879-optimize" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV1879opt"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Optimize", row );
					QuerySetCell( qry, "body", "Document to test optimize action", row );

					index
						collection="LDEV1879opt"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					collection
						action="optimize"
						collection="LDEV1879opt";

					search name="local.res" collection="LDEV1879opt" criteria="optimize" language="English";
					expect( res.recordcount ).toBe( 1, "search should still work after optimize" );

					collection
						action="delete"
						collection="LDEV1879opt";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="repair does not throw NPE", body=function() {
				var path = server._getTempDir( "LDEV1879-repair" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="LDEV1879rep"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Repair", row );
					QuerySetCell( qry, "body", "Document to test repair action", row );

					index
						collection="LDEV1879rep"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					collection
						action="repair"
						collection="LDEV1879rep";

					search name="local.res" collection="LDEV1879rep" criteria="repair" language="English";
					expect( res.recordcount ).toBe( 1, "search should still work after repair" );

					collection
						action="delete"
						collection="LDEV1879rep";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="multiple create/index/search cycles do not cause NPE", body=function() {
				var path = server._getTempDir( "LDEV1879-cycle" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					// cycle 1
					collection
						action="create"
						collection="LDEV1879cyc"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Cycle One", row );
					QuerySetCell( qry, "body", "First cycle document", row );

					index
						collection="LDEV1879cyc"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="LDEV1879cyc" criteria="cycle" language="English";
					expect( res.recordcount ).toBe( 1 );

					collection
						action="delete"
						collection="LDEV1879cyc";

					// cycle 2 — reuse same path
					collection
						action="create"
						collection="LDEV1879cyc"
						path="#path#"
						language="English";

					var qry2 = QueryNew( 'id,title,body' );
					row = QueryAddRow( qry2 );
					QuerySetCell( qry2, "id", "2", row );
					QuerySetCell( qry2, "title", "Cycle Two", row );
					QuerySetCell( qry2, "body", "Second cycle document", row );

					index
						collection="LDEV1879cyc"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry2"
						urlpath="/";

					search name="local.res" collection="LDEV1879cyc" criteria="Second" language="English";
					expect( res.recordcount ).toBe( 1, "second cycle should work without NPE" );

					collection
						action="delete"
						collection="LDEV1879cyc";
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
