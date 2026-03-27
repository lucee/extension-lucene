component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-6196: cfsearch context highlighting", body=function() {

			it( title="default HTML markers wrap matched terms", body=function() {
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

					var ctx = toString( res.context[1] );
					expect( len( ctx ) ).toBeGT( 0, "context should not be empty" );
					expect( ctx ).toInclude( "<b", "context should contain highlight begin marker" );
					expect( ctx ).toInclude( "</b>", "context should contain highlight end marker" );
					expect( ctx ).toInclude( "xylophone</b>", "matched term should be wrapped" );

					collection action="delete" collection="ctxHighlight";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="custom non-HTML markers are applied correctly", body=function() {
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

					var ctx = toString( res.context[1] );
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

			it( title="context returned with default markers when none specified", body=function() {
				var path = server._getTempDir( "ctx-default-marker" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxDefault"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Default Markers", row );
					QuerySetCell( qry, "body", "The trombone echoed through the empty gymnasium creating a haunting sound", row );

					index
						collection="ctxDefault"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxDefault"
						criteria="trombone"
						language="English"
						contextpassages="1"
						contextBytes="500";

					expect( res.recordcount ).toBe( 1 );

					var ctx = toString( res.context[1] );
					expect( len( ctx ) ).toBeGT( 0, "context should not be empty" );
					expect( ctx ).toInclude( "trombone", "context should contain matched term" );
					// default markers are <B>/<\/B> from HTMLFormatterWithScore
					expect( ctx ).toInclude( "<B", "default markers should be applied" );
					expect( ctx ).toInclude( "trombone</B>", "matched term should be wrapped in default markers" );

					collection action="delete" collection="ctxDefault";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="contextBytes constrains total context length", body=function() {
				var path = server._getTempDir( "ctx-bytes" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxBytes"
						path="#path#"
						language="English";

					// long body so context must be truncated
					var longBody = "The xylophone is an amazing instrument. " &
						repeatString( "This is filler text to pad the document body out to a long length. ", 20 ) &
						"The xylophone sounds wonderful at the end.";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Bytes Test", row );
					QuerySetCell( qry, "body", longBody, row );

					index
						collection="ctxBytes"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxBytes"
						criteria="xylophone"
						language="English"
						contextpassages="5"
						contextBytes="100"
						contextpassageLength="50"
						contextHighlightBegin="[H]"
						contextHighlightEnd="[/H]";

					expect( res.recordcount ).toBe( 1 );

					var ctx = toString( res.context[1] );
					expect( len( ctx ) ).toBeGT( 0, "context should not be empty" );
					expect( len( ctx ) ).toBeLTE( 150, "context should be constrained by contextBytes (allowing overhead for markers and separators)" );
					expect( ctx ).toInclude( "[H]", "highlight markers should be present" );

					collection action="delete" collection="ctxBytes";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="contextPassages=0 returns empty context", body=function() {
				var path = server._getTempDir( "ctx-zero-passages" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxZeroPass"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Zero Passages", row );
					QuerySetCell( qry, "body", "The xylophone played a beautiful melody", row );

					index
						collection="ctxZeroPass"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxZeroPass"
						criteria="xylophone"
						language="English"
						contextpassages="0"
						contextBytes="500";

					expect( res.recordcount ).toBe( 1 );

					var ctx = toString( res.context[1] );
					expect( len( ctx ) ).toBe( 0, "context should be empty when contextPassages=0" );

					collection action="delete" collection="ctxZeroPass";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="highlighting works with type=explicit", body=function() {
				var path = server._getTempDir( "ctx-explicit" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxExplicit"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Explicit Test", row );
					QuerySetCell( qry, "body", "The saxophone solo was breathtaking during the jazz performance", row );

					index
						collection="ctxExplicit"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxExplicit"
						criteria="saxophone"
						type="explicit"
						language="English"
						contextpassages="1"
						contextBytes="500"
						contextpassageLength="200"
						contextHighlightBegin="{{HL}}"
						contextHighlightEnd="{{/HL}}";

					expect( res.recordcount ).toBe( 1 );

					var ctx = toString( res.context[1] );
					expect( len( ctx ) ).toBeGT( 0, "context should not be empty" );
					expect( ctx ).toInclude( "{{HL}}", "explicit mode should apply custom begin marker" );
					expect( ctx ).toInclude( "{{/HL}}", "explicit mode should apply custom end marker" );
					expect( ctx ).toInclude( "{{HL}}saxophone{{/HL}}", "matched term should be wrapped" );

					collection action="delete" collection="ctxExplicit";
				}
				finally {
					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
				}
			});

			it( title="multiple matched terms are all highlighted", body=function() {
				var path = server._getTempDir( "ctx-multi-match" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				try {
					collection
						action="create"
						collection="ctxMulti"
						path="#path#"
						language="English";

					var qry = QueryNew( 'id,title,body' );
					var row = QueryAddRow( qry );
					QuerySetCell( qry, "id", "1", row );
					QuerySetCell( qry, "title", "Multi Match", row );
					QuerySetCell( qry, "body", "The piano and the guitar played together while the piano echoed beautifully", row );

					index
						collection="ctxMulti"
						action="update"
						type="custom"
						title="title"
						body="body"
						key="id"
						query="qry"
						urlpath="/";

					search
						name="local.res"
						collection="ctxMulti"
						criteria="piano"
						language="English"
						contextpassages="1"
						contextBytes="500"
						contextpassageLength="200"
						contextHighlightBegin="[H]"
						contextHighlightEnd="[/H]";

					expect( res.recordcount ).toBe( 1 );

					var ctx = toString( res.context[1] );
					// "piano" appears twice in the body — both should be highlighted
					var markerCount = 0;
					var pos = 1;
					while ( true ) {
						pos = find( "[H]piano[/H]", ctx, pos );
						if ( pos == 0 ) break;
						markerCount++;
						pos++;
					}
					expect( markerCount ).toBeGTE( 2, "both occurrences of 'piano' should be highlighted" );

					collection action="delete" collection="ctxMulti";
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
