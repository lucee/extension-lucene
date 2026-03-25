component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-2025: cfsearch with special characters in criteria", body=function() {

			beforeEach( function() {
				variables.path = server._getTempDir( "LDEV2025" );

				if ( DirectoryExists( variables.path ) ) {
					directoryDelete( variables.path, true );
				}
				directoryCreate( variables.path );

				collection
					action="create"
					collection="LDEV2025"
					path="#variables.path#"
					language="English";

				var qry = QueryNew( 'id,title,body' );
				var row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "1", row );
				QuerySetCell( qry, "title", "Question", row );
				QuerySetCell( qry, "body", "What comes after 5 AAA CCC AAAAA testing lucee lucene", row );

				index
					collection="LDEV2025"
					action="update"
					type="custom"
					title="title"
					body="body"
					key="id"
					query="qry"
					urlpath="/";
			});

			afterEach( function() {
				try { collection action="delete" collection="LDEV2025"; } catch( any e ) {}
				if ( DirectoryExists( variables.path ) ) {
					directoryDelete( variables.path, true );
				}
			});

			it( title="criteria with ? between words should not throw", body=function() {
				search name="local.res" collection="LDEV2025" criteria="What comes after 5 ? AAA CCC AAAAA" language="English";
				expect( isQuery( res ) ).toBeTrue( "should return a query even with ? in criteria" );
			});

			it( title="criteria with leading ? should not throw", body=function() {
				search name="local.res" collection="LDEV2025" criteria="?testing" language="English";
				expect( isQuery( res ) ).toBeTrue( "should return a query even with leading ?" );
			});

			it( title="criteria with * wildcard should not throw", body=function() {
				search name="local.res" collection="LDEV2025" criteria="test*" language="English";
				expect( isQuery( res ) ).toBeTrue( "should return a query with trailing *" );
			});

			it( title="criteria with standalone * should not throw", body=function() {
				search name="local.res" collection="LDEV2025" criteria="*" language="English";
				expect( isQuery( res ) ).toBeTrue( "should return a query for standalone *" );
			});

			it( title="criteria with special Lucene chars should not throw", body=function() {
				// Lucene special chars: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
				var specials = [ "test + lucee", "test - lucee", "test && lucee",
					"test || lucee", "test ! lucee", "(test)", "{test}",
					"[test]", "test^2", "~test", "test:lucee" ];

				for ( var criteria in specials ) {
					search name="local.res" collection="LDEV2025" criteria="#criteria#" language="English";
					expect( isQuery( res ) ).toBeTrue( "should handle criteria: #criteria#" );
				}
			});
		});
	}
}
