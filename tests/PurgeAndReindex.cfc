component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="Forum: purge + re-index should not return stale records", body=function() {

			it( title="purge then re-index with fewer records does not return old data", body=function() {
				var path = server._getTempDir( "purge-reindex" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="purgeReindex"
						path="#path#"
						language="English";

					// index 3 documents
					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Alpha", row );
					QuerySetCell( qry, "body", "Alpha document about searchable content", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "2", row );
					QuerySetCell( qry, "title", "Beta", row );
					QuerySetCell( qry, "body", "Beta document about searchable content", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "3", row );
					QuerySetCell( qry, "title", "Gamma", row );
					QuerySetCell( qry, "body", "Gamma document about searchable content", row );

					index
						collection="purgeReindex"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="purgeReindex" criteria="searchable" language="English";
					expect( res.recordcount ).toBe( 3, "should find all 3 documents before purge" );

					// purge all documents
					index collection="purgeReindex" action="purge";

					search name="local.res" collection="purgeReindex" criteria="searchable" language="English";
					expect( res.recordcount ).toBe( 0, "should find 0 documents after purge" );

					// re-index with only 1 document
					var qry2 = QueryNew( 'id,title,body' );
					row = QueryAddRow( qry2 );
					QuerySetCell( qry2, "id", "10", row );
					QuerySetCell( qry2, "title", "Delta", row );
					QuerySetCell( qry2, "body", "Delta is the only searchable document now", row );

					index
						collection="purgeReindex"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry2"
						urlpath="/";

					search name="local.res" collection="purgeReindex" criteria="searchable" language="English";
					expect( res.recordcount ).toBe( 1, "should find only 1 document after re-index" );

					// verify old documents are truly gone
					search name="local.res" collection="purgeReindex" criteria="Alpha" language="English";
					expect( res.recordcount ).toBe( 0, "old document Alpha should not be found" );

					search name="local.res" collection="purgeReindex" criteria="Beta" language="English";
					expect( res.recordcount ).toBe( 0, "old document Beta should not be found" );

					search name="local.res" collection="purgeReindex" criteria="Gamma" language="English";
					expect( res.recordcount ).toBe( 0, "old document Gamma should not be found" );

					collection action="delete" collection="purgeReindex";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="refresh replaces all documents without needing explicit purge", body=function() {
				var path = server._getTempDir( "refresh-reindex" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="refreshReindex"
						path="#path#"
						language="English";

					// index 2 documents
					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Original One", row );
					QuerySetCell( qry, "body", "Original first document zeppelin", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "2", row );
					QuerySetCell( qry, "title", "Original Two", row );
					QuerySetCell( qry, "body", "Original second document zeppelin", row );

					index
						collection="refreshReindex"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="refreshReindex" criteria="zeppelin" language="English";
					expect( res.recordcount ).toBe( 2 );

					// refresh with only 1 different document
					var qry2 = QueryNew( 'id,title,body' );
					row = QueryAddRow( qry2 );
					QuerySetCell( qry2, "id", "3", row );
					QuerySetCell( qry2, "title", "Replacement", row );
					QuerySetCell( qry2, "body", "This completely replaces the index xylophone", row );

					index
						collection="refreshReindex"
						action="refresh"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry2"
						urlpath="/";

					search name="local.res" collection="refreshReindex" criteria="zeppelin" language="English";
					expect( res.recordcount ).toBe( 0, "original documents should be gone after refresh" );

					search name="local.res" collection="refreshReindex" criteria="xylophone" language="English";
					expect( res.recordcount ).toBe( 1, "replacement document should be found" );

					collection action="delete" collection="refreshReindex";
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
