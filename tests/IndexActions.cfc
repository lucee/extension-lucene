component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="cfindex actions", body=function() {

			it( title="refresh replaces entire index", body=function() {
				var path = server._getTempDir( "tag-index-refresh" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="refreshTest"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "First", row );
					QuerySetCell( qry, "body", "First document in the collection", row );

					index
						collection="refreshTest"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="refreshTest" criteria="First" language="English";
					expect( res.recordcount ).toBe( 1 );

					// refresh should replace the entire index
					var qry2 = QueryNew( 'id,title,body' );
					row = QueryAddRow( qry2 );
					QuerySetCell( qry2, "id", "2", row );
					QuerySetCell( qry2, "title", "Second", row );
					QuerySetCell( qry2, "body", "Second document replaces everything", row );

					index
						collection="refreshTest"
						action="refresh"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry2"
						urlpath="/";

					search name="local.res" collection="refreshTest" criteria="First" language="English";
					expect( res.recordcount ).toBe( 0, "refresh should have removed old documents" );

					search name="local.res" collection="refreshTest" criteria="Second" language="English";
					expect( res.recordcount ).toBe( 1, "refresh should have added new document" );

					collection
						action="delete"
						collection="refreshTest";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="purge removes all documents", body=function() {
				var path = server._getTempDir( "tag-index-purge" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="purgeTest"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Purge Me", row );
					QuerySetCell( qry, "body", "This document should be purged", row );

					index
						collection="purgeTest"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="purgeTest" criteria="purged" language="English";
					expect( res.recordcount ).toBe( 1 );

					index
						collection="purgeTest"
						action="purge";

					search name="local.res" collection="purgeTest" criteria="purged" language="English";
					expect( res.recordcount ).toBe( 0, "purge should have removed all documents" );

					collection
						action="delete"
						collection="purgeTest";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="path indexing with extension filter", body=function() {
				var path = server._getTempDir( "tag-index-path" );
				var collPath = server._getTempDir( "tag-index-path-coll" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );
				if ( DirectoryExists( collPath ) ) {
					directoryDelete( collPath, true );
				}
				directoryCreate( collPath );

				try {
					fileWrite( path & "/doc1.html", "<html><body>Lucee is a CFML application server</body></html>", "UTF-8" );
					fileWrite( path & "/doc2.html", "<html><body>ColdFusion was the original CFML engine</body></html>", "UTF-8" );
					fileWrite( path & "/ignore.txt", "This text file should be ignored", "UTF-8" );

					collection
						action="create"
						collection="pathTest"
						path="#collPath#"
						language="English";

					index
						collection="pathTest"
						action="update"
						key="#path#"
						type="path"
						urlpath="/docs/"
						extensions=".html"
						recurse="true";

					search name="local.res" collection="pathTest" criteria="Lucee" language="English";
					expect( res.recordcount ).toBe( 1, "should find Lucee in indexed HTML files" );

					search name="local.res" collection="pathTest" criteria="ignored" language="English";
					expect( res.recordcount ).toBe( 0, "should not have indexed .txt files" );

					collection
						action="delete"
						collection="pathTest";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
					if ( DirectoryExists( collPath ) ) {
						directoryDelete( collPath, true );
					}
				}
			});
		});
	}
}
