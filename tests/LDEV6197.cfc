component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-6197: cfsearch filename not searchable in path-type index", body=function() {

			it( title="filename is searchable in path-type index", body=function() {
				var filePath = server._getTempDir( "path-filename-files" );
				var collPath = server._getTempDir( "path-filename-coll" );

				if ( DirectoryExists( filePath ) ) {
					directoryDelete( filePath, true );
				}
				directoryCreate( filePath );
				if ( DirectoryExists( collPath ) ) {
					directoryDelete( collPath, true );
				}
				directoryCreate( collPath );

				try {
					fileWrite( filePath & "/Marrakesh.html", "<html><body>Travel guide to Morocco</body></html>", "UTF-8" );
					fileWrite( filePath & "/Tokyo.html", "<html><body>Travel guide to Japan</body></html>", "UTF-8" );
					fileWrite( filePath & "/Berlin.html", "<html><body>Travel guide to Germany</body></html>", "UTF-8" );

					collection
						action="create"
						collection="pathFilename"
						path="#collPath#"
						language="English";

					index
						collection="pathFilename"
						action="update"
						key="#filePath#"
						type="path"
						urlpath="/travel/"
						extensions=".html"
						recurse="true";

					// search by filename — this is the reported bug
					search name="local.res" collection="pathFilename" criteria="Marrakesh" language="English";
					expect( res.recordcount ).toBeGTE( 1, "should find document by filename 'Marrakesh'" );

					// search by content to verify indexing worked at all
					search name="local.res" collection="pathFilename" criteria="Morocco" language="English";
					expect( res.recordcount ).toBe( 1, "should find document by content 'Morocco'" );

					// search by another filename
					search name="local.res" collection="pathFilename" criteria="Tokyo" language="English";
					expect( res.recordcount ).toBeGTE( 1, "should find document by filename 'Tokyo'" );

					collection action="delete" collection="pathFilename";
				}
				finally {
					if ( DirectoryExists( filePath ) ) {
						directoryDelete( filePath, true );
					}
					if ( DirectoryExists( collPath ) ) {
						directoryDelete( collPath, true );
					}
				}
			});

			it( title="URL path is returned in search results for path-type index", body=function() {
				var filePath = server._getTempDir( "path-url-files" );
				var collPath = server._getTempDir( "path-url-coll" );

				if ( DirectoryExists( filePath ) ) {
					directoryDelete( filePath, true );
				}
				directoryCreate( filePath );
				if ( DirectoryExists( collPath ) ) {
					directoryDelete( collPath, true );
				}
				directoryCreate( collPath );

				try {
					fileWrite( filePath & "/sample.html", "<html><body>Unique xylophone content for URL test</body></html>", "UTF-8" );

					collection
						action="create"
						collection="pathUrl"
						path="#collPath#"
						language="English";

					index
						collection="pathUrl"
						action="update"
						key="#filePath#"
						type="path"
						urlpath="/mysite/"
						extensions=".html"
						recurse="true";

					search name="local.res" collection="pathUrl" criteria="xylophone" language="English";
					expect( res.recordcount ).toBe( 1 );
					expect( res.key ).toInclude( "sample.html", "key should contain the filename" );

					collection action="delete" collection="pathUrl";
				}
				finally {
					if ( DirectoryExists( filePath ) ) {
						directoryDelete( filePath, true );
					}
					if ( DirectoryExists( collPath ) ) {
						directoryDelete( collPath, true );
					}
				}
			});
		});
	}
}
