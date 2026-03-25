component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-6196: cfsearch context highlighting markers not applied", body=function() {

			it( title="contextHighlightBegin and contextHighlightEnd wrap matched terms", skip=needsCoreHighlightFix(), body=function() {
				var path = server._getTempDir( "ctx-highlight" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxHighlight"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Highlight Test", row );
					QuerySetCell( qry, "body", "The xylophone played a beautiful melody in the concert hall last night", row );

					index
						collection="ctxHighlight"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxHighlight"
						criteria="xylophone"
						language="English"
						contextpassages="1"
						contextBytes="500"
						contextpassageLength="200"
						contextHighlightBegin="<b>"
						contextHighlightEnd="</b>";

					expect( res.recordcount ).toBe( 1 );

					var ctx = res.context;
					expect( len( ctx ) ).toBeGT( 0, "context should not be empty" );
					expect( ctx ).toInclude( "<b>", "context should contain highlight begin marker" );
					expect( ctx ).toInclude( "</b>", "context should contain highlight end marker" );

					collection action="delete" collection="ctxHighlight";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="custom highlight markers are applied correctly", skip=needsCoreHighlightFix(), body=function() {
				var path = server._getTempDir( "ctx-custom-marker" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxCustom"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Custom Markers", row );
					QuerySetCell( qry, "body", "The accordion player performed on the street corner every Saturday morning", row );

					index
						collection="ctxCustom"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxCustom"
						criteria="accordion"
						language="English"
						contextpassages="1"
						contextBytes="500"
						contextpassageLength="200"
						contextHighlightBegin="[MATCH]"
						contextHighlightEnd="[/MATCH]";

					expect( res.recordcount ).toBe( 1 );

					var ctx = res.context;
					expect( ctx ).toInclude( "[MATCH]", "context should contain custom begin marker" );
					expect( ctx ).toInclude( "[/MATCH]", "context should contain custom end marker" );
					expect( ctx ).toInclude( "[MATCH]accordion[/MATCH]", "matched term should be wrapped in custom markers" );

					collection action="delete" collection="ctxCustom";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="context is returned without highlighting when markers not specified", body=function() {
				var path = server._getTempDir( "ctx-no-marker" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxNoMarker"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "No Markers", row );
					QuerySetCell( qry, "body", "The trombone echoed through the empty gymnasium creating a haunting sound", row );

					index
						collection="ctxNoMarker"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxNoMarker"
						criteria="trombone"
						language="English";

					expect( res.recordcount ).toBe( 1 );

					var cols = res.columnList;
					expect( cols ).toInclude( "context", "results should include context column" );

					collection action="delete" collection="ctxNoMarker";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});
		});
	}

	// core fix for AddionalAttrs landed in 7.0.3.30-SNAPSHOT
	private boolean function needsCoreHighlightFix() {
		return !server.checkVersionGTE( server.lucee.version, 7, 0, 3, 30 );
	}
}
