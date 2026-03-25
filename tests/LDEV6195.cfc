component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-6195: cfindex action=delete should remove individual documents", body=function() {

			it( title="delete by key removes specific document from index", body=function() {
				var path = server._getTempDir( "index-delete-key" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="deleteByKey"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Keep This", row );
					QuerySetCell( qry, "body", "Document that should survive deletion xylophone", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "2", row );
					QuerySetCell( qry, "title", "Delete This", row );
					QuerySetCell( qry, "body", "Document that should be removed zeppelin", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "3", row );
					QuerySetCell( qry, "title", "Also Keep", row );
					QuerySetCell( qry, "body", "Another document that should survive accordion", row );

					index
						collection="deleteByKey"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					// verify all 3 indexed
					search name="local.res" collection="deleteByKey" criteria="document" language="English";
					expect( res.recordcount ).toBe( 3, "should find all 3 documents before delete" );

					// delete document with key "2"
					var delQry = QueryNew( 'id,title,body' );
					row = QueryAddRow( delQry );
					QuerySetCell( delQry, "id", "2", row );
					QuerySetCell( delQry, "title", "", row );
					QuerySetCell( delQry, "body", "", row );

					index
						collection="deleteByKey"
						action="delete"
						type="custom"
						key="id"
						query="delQry";

					// verify document 2 is gone
					search name="local.res" collection="deleteByKey" criteria="zeppelin" language="English";
					expect( res.recordcount ).toBe( 0, "deleted document should not be found" );

					// verify other documents still exist
					search name="local.res" collection="deleteByKey" criteria="xylophone" language="English";
					expect( res.recordcount ).toBe( 1, "non-deleted document 1 should still be found" );

					search name="local.res" collection="deleteByKey" criteria="accordion" language="English";
					expect( res.recordcount ).toBe( 1, "non-deleted document 3 should still be found" );

					collection action="delete" collection="deleteByKey";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="delete then re-search does not return stale results", body=function() {
				var path = server._getTempDir( "index-delete-stale" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="deleteStale"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "100", row );
					QuerySetCell( qry, "title", "Persistent", row );
					QuerySetCell( qry, "body", "This persistent searchable document stays forever", row );

					row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "200", row );
					QuerySetCell( qry, "title", "Ephemeral", row );
					QuerySetCell( qry, "body", "This ephemeral searchable document gets deleted", row );

					index
						collection="deleteStale"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search name="local.res" collection="deleteStale" criteria="searchable" language="English";
					expect( res.recordcount ).toBe( 2, "both documents should be found initially" );

					// delete ephemeral document
					var delQry = QueryNew( 'id,title,body' );
					row = QueryAddRow( delQry );
					QuerySetCell( delQry, "id", "200", row );
					QuerySetCell( delQry, "title", "", row );
					QuerySetCell( delQry, "body", "", row );

					index
						collection="deleteStale"
						action="delete"
						type="custom"
						key="id"
						query="delQry";

					// search with a term that matched both documents
					search name="local.res" collection="deleteStale" criteria="searchable" language="English";
					expect( res.recordcount ).toBe( 1, "only persistent document should remain" );

					// search specifically for deleted content
					search name="local.res" collection="deleteStale" criteria="ephemeral" language="English";
					expect( res.recordcount ).toBe( 0, "ephemeral document should be completely gone" );

					collection action="delete" collection="deleteStale";
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
