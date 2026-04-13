component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.colName = "spellcheckTest";
		variables.path = server._getTempDir( "spellcheck-test" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );

		collection
			action="create"
			collection="#variables.colName#"
			path="#variables.path#"
			language="English";

		// rich vocabulary so the spellchecker dictionary has enough words to
		// suggest multiple alternatives for misspellings
		var qry = QueryNew( 'id,title,body' );

		var row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "1", row );
		QuerySetCell( qry, "title", "Search Engine Architecture", row );
		QuerySetCell( qry, "body", "A search engine consists of a crawler, an indexer, and a query processor. The indexer builds an inverted index from crawled documents. Search engines use complex algorithms to rank results by relevance.", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "2", row );
		QuerySetCell( qry, "title", "Performance and Optimization", row );
		QuerySetCell( qry, "body", "Performance of search applications depends on index structure. Permanent storage solutions and caching improve throughput. Permission controls ensure secure access to indexed content. Personal preferences can customize ranking.", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "3", row );
		QuerySetCell( qry, "title", "Indexing Documents", row );
		QuerySetCell( qry, "body", "Document indexing processes text through analysis, tokenization, and normalization. The analyzer handles stemming and stop word removal. Indexed documents are stored in segments that are periodically merged.", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "4", row );
		QuerySetCell( qry, "title", "Query Processing", row );
		QuerySetCell( qry, "body", "Query processing involves parsing user input into structured queries. Boolean operators combine terms with AND, OR, and NOT logic. Phrase queries match exact sequences of words in the index.", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "5", row );
		QuerySetCell( qry, "title", "Relevance Scoring", row );
		QuerySetCell( qry, "body", "Relevance scoring uses term frequency and inverse document frequency. Important terms that appear rarely across documents score higher. Persistent caching of scoring data improves repeated query performance.", row );

		index
			collection="#variables.colName#"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qry"
			urlpath="/";
	}

	function afterAll() {
		try { collection action="delete" collection="#variables.colName#"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	function run( testResults, testBox ) {
		describe( title="LDEV-6198: cfsearch suggestions/spellcheck", body=function() {

			it( title="suggestedQuery returned with clean tags for misspelled term", body=function() {
				search
					name="local.res"
					collection="#variables.colName#"
					criteria="serch"
					language="English"
					suggestions="always"
					status="local.stats";

				systemOutput( "test1 stats: " & serializeJSON( stats ), true );

				expect( stats ).toHaveKey( "suggestedQuery" );
				expect( len( stats.suggestedQuery ) ).toBeGT( 0, "suggestedQuery should not be empty" );
				// tags should be fully stripped by Lucee core — no leftover <suggestion> or </suggestion>
				expect( stats.suggestedQuery ).notToInclude( "<suggestion>", "open tag should be stripped" );
				expect( stats.suggestedQuery ).notToInclude( "</suggestion>", "close tag should be stripped" );
				expect( stats.suggestedQuery ).notToInclude( "<\/suggestion>", "escaped close tag should not appear" );
				expect( stats.suggestedQuery ).toInclude( "search", "should correct 'serch' to 'search'" );
			});

			it( title="keywords struct populated for misspelled term", body=function() {
				search
					name="local.res"
					collection="#variables.colName#"
					criteria="serch"
					language="English"
					suggestions="always"
					status="local.stats";

				expect( stats ).toHaveKey( "keywords" );
				expect( isStruct( stats.keywords ) ).toBeTrue( "keywords should be a struct" );
				expect( structCount( stats.keywords ) ).toBeGT( 0, "should have keyword suggestions" );
			});

			it( title="multiple suggestions with real scores for ambiguous misspelling", body=function() {
				// "perman" is close to "permanent", "performance", "personal", "permission"
				// — should produce multiple suggestions with varying Lucene string distances
				search
					name="local.res"
					collection="#variables.colName#"
					criteria="perman"
					language="English"
					suggestions="always"
					status="local.stats";

				systemOutput( "test3 stats: " & serializeJSON( stats ), true );

				expect( stats ).toHaveKey( "keywordScore" );
				expect( isStruct( stats.keywordScore ) ).toBeTrue();
				expect( structCount( stats.keywordScore ) ).toBeGT( 0 );

				for ( var term in stats.keywordScore ) {
					var scoreArr = stats.keywordScore[ term ];
					expect( isArray( scoreArr ) ).toBeTrue();
					expect( arrayLen( scoreArr ) ).toBeGT( 0 );

					for ( var s in scoreArr ) {
						expect( s ).toBeGTE( 0, "score should be >= 0" );
						expect( s ).toBeLTE( 100, "score should be <= 100" );
					}

					// if we have 2+ suggestions, verify scores aren't a simple countdown
					if ( arrayLen( scoreArr ) >= 2 ) {
						var allGapsAreOne = true;
						for ( var i = 2; i <= arrayLen( scoreArr ); i++ ) {
							if ( scoreArr[ i - 1 ] - scoreArr[ i ] != 1 ) {
								allGapsAreOne = false;
								break;
							}
						}
						expect( allGapsAreOne ).toBeFalse( "scores should be real Lucene distances, not a fake 99,98,97 countdown" );
					}
				}
			});

			it( title="suggestions=never returns no suggestions", body=function() {
				search
					name="local.res"
					collection="#variables.colName#"
					criteria="serch"
					language="English"
					suggestions="never"
					status="local.stats";

				var hasKeywords = structKeyExists( stats, "keywords" ) && isStruct( stats.keywords ) && structCount( stats.keywords ) > 0;
				expect( hasKeywords ).toBeFalse( "should have no suggestions when suggestions=never" );
			});

			it( title="correct spelling produces no false corrections", body=function() {
				search
					name="local.res"
					collection="#variables.colName#"
					criteria="search"
					language="English"
					suggestions="always"
					status="local.stats";

				expect( res.recordcount ).toBeGT( 0, "correctly spelled term should find results" );
			});

			it( title="type=explicit with suggestions does not NPE", body=function() {
				search
					name="local.res"
					collection="#variables.colName#"
					criteria="serch"
					language="English"
					type="explicit"
					suggestions="always"
					status="local.stats";

				// should not throw — that's the main assertion
				expect( isStruct( stats ) ).toBeTrue();
			});
		});
	}
}
