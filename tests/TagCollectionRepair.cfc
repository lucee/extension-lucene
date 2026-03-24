component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="cfcollection action=repair", body=function() {

			it( title="collection is searchable after repair", body=function() {
				var path = server._getTempDir( "tag-collection-repair" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="repairTest"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Test Doc", row );
					QuerySetCell( qry, "body", "This is a test document for repair", row );

					index
						collection="repairTest"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					collection
						action="repair"
						collection="repairTest";

					search name="local.res" collection="repairTest" criteria="test" language="English";
					expect( res.recordcount ).toBe( 1, "collection should still be searchable after repair" );

					collection
						action="delete"
						collection="repairTest";
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
