component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function run( testResults, testBox ) {
		describe( title="LDEV-1023: non-English language analyzers should not throw ClassNotFoundException", body=function() {

			var languages = [
				"Arabic", "Bulgarian", "Bengali", "Brazilian", "Catalan",
				"Czech", "Danish", "Dutch", "English", "Finnish",
				"French", "German", "Greek", "Hungarian", "Italian",
				"Japanese", "Korean", "Norwegian", "Portuguese", "Russian",
				"Spanish", "Swedish", "Thai", "Turkish"
			];

			for ( var lang in languages ) {
				it( title="create, index and search with language: #lang#", data={ lang: lang }, body=function( data ) {
					var safeName = "LDEV1023" & reReplace( data.lang, "[^a-zA-Z]", "", "all" );
					var path = server._getTempDir( "LDEV1023-#lCase( data.lang )#" );

					if ( DirectoryExists( path ) ) {
						directoryDelete( path, true );
					}
					directoryCreate( path );

					try {
						collection
							action="create"
							collection="#safeName#"
							path="#path#"
							language="#data.lang#";

						var qry = QueryNew( 'id,title,body' );
						var row = QueryAddRow( qry );
						QuerySetCell( qry, "id", "1", row );
						QuerySetCell( qry, "title", "Language Test", row );
						QuerySetCell( qry, "body", "Testing the #data.lang# language analyzer works correctly", row );

						index
							collection="#safeName#"
							action="update"
							type="custom"
							title="title"
							body="body"
							key="id"
							query="qry"
							urlpath="/";

						search name="local.res" collection="#safeName#" criteria="language" language="#data.lang#";
						expect( isQuery( res ) ).toBeTrue( "#data.lang# analyzer should return a query" );

						collection
							action="delete"
							collection="#safeName#";
					}
					finally {
						if ( DirectoryExists( path ) ) {
							directoryDelete( path, true );
						}
					}
				});
			}
		});
	}
}
