component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.path = server._getTempDir( "search-spell-lifecycle" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );
	}

	function afterAll() {
		try { collection action="delete" collection="spellLifeA"; } catch( any e ) {}
		try { collection action="delete" collection="spellLifeC"; } catch( any e ) {}
		try { collection action="delete" collection="spellLifeB"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	private function createCollection( collection, sub ) {
		collection
			action="create"
			collection=arguments.collection
			path="#variables.path#/#arguments.sub#"
			language="English";
	}

	private function indexDocs( collection, extra ) {
		var qry = QueryNew( 'id,title,body' );
		var row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "1", row );
		QuerySetCell( qry, "title", "Search Engine Architecture", row );
		QuerySetCell( qry, "body", "A search engine consists of a crawler, an indexer and a query processor. The indexer builds an inverted index from crawled documents. #arguments.extra#", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "2", row );
		QuerySetCell( qry, "title", "Query Processing", row );
		QuerySetCell( qry, "body", "Query processing parses user input into structured queries. Boolean operators combine terms with AND, OR and NOT logic. #arguments.extra#", row );

		index
			collection=arguments.collection
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qry"
			urlpath="/";
	}

	// Lucene opens <createPath>/CF<name><changes>, and the index directory and the spell directory sit
	// side by side beneath it. The identity name's case folding differs per platform, so walk into the
	// single child rather than guessing its name.
	private function spellDirs( sub ) {
		var found = [];
		var roots = DirectoryList( path="#variables.path#/#arguments.sub#", recurse=false, listInfo="name" );
		if ( ArrayLen( roots ) == 0 ) {
			return found;
		}

		var children = DirectoryList( path="#variables.path#/#arguments.sub#/#roots[ 1 ]#", recurse=false, listInfo="name" );
		for ( var name in children ) {
			if ( name.endsWith( "_spell" ) ) {
				found.append( name );
			}
		}
		return found;
	}
	private function suggestedWords( stats ) {
		var words = [];
		for ( var term in arguments.stats.keywords ) {
			for ( var suggestion in arguments.stats.keywords[ term ] ) {
				words.append( suggestion );
			}
		}
		return words;
	}

	function run( testResults, testBox ) {
		describe( title="spell index lifecycle", body=function() {

			it( title="a reindex replaces the spell directory instead of adding one", body=function() {
				createCollection( "spellLifeA", "colA" );
				indexDocs( "spellLifeA", "" );

				// A search with suggestions is what keeps the spell directory open: the read side caches
				// one shared SpellChecker, and with it its FSDirectory, for the life of the JVM. Without
				// the search the write path's own checker is closed in its finally and the wipe succeeds.
				search name="local.res" collection="spellLifeA" criteria="serch" language="English" suggestions="always" status="local.stats";
				expect( stats ).toHaveKey( "keywords" );
				expect( structCount( stats.keywords ) ).toBeGT( 0, "the search served a suggestion, so the shared checker is open" );

				indexDocs( "spellLifeA", "" );

				// Measured on 7.1/Windows: the wipe succeeds, so the collection keeps exactly one spell
				// directory and the rebuild lands at the same path on a new inode. That is what leaves the
				// cached checker holding the old one - the freshness test below.
				var dirs = spellDirs( "colA" );
				expect( ArrayLen( dirs ) ).toBe( 1, "one spell directory per collection, found [#ArrayToList( dirs )#]" );
			});

			it( title="a suggestion reflects a word added by a later index operation", body=function() {
				createCollection( "spellLifeB", "colB" );
				indexDocs( "spellLifeB", "" );

				search name="local.res" collection="spellLifeB" criteria="serch" language="English" suggestions="always" status="local.stats";
				expect( structCount( stats.keywords ) ).toBeGT( 0, "the first index serves a suggestion" );

				// "quuxolite" exists only in the second index.
				indexDocs( "spellLifeB", "A quuxolite is a crystallised mineral found in quartz veins." );

				// The rebuild removes the old spell directory and writes a new one at the same path, so the
				// cached checker is still keyed to that path and still reading the unlinked inode. It answers,
				// with the previous dictionary.
				search name="local.res" collection="spellLifeB" criteria="serch" language="English" suggestions="always" status="local.stats";
				expect( ArrayFindNoCase( suggestedWords( stats ), "search" ) ).toBeGT( 0, "the cached checker is alive and still serves the first dictionary" );

				search name="local.res" collection="spellLifeB" criteria="quuxolit" language="English" suggestions="always" status="local.stats";
				var suggested = [];
				if ( structKeyExists( stats, "keywords" ) ) {
					suggested = suggestedWords( stats );
				}
				expect( ArrayFindNoCase( suggested, "quuxolite" ) ).toBeGT( 0, "suggestions come from the current index, found [#ArrayToList( suggested )#]" );
			});

			// Control for the test above: the same two index operations with no search in between, so
			// nothing has cached a checker over the first dictionary. Proves the word is suggestible at
			// all, which is what makes the failure above staleness rather than a bad fixture.
			it( title="a dictionary rebuilt before the first suggestion serves the new word", body=function() {
				createCollection( "spellLifeC", "colC" );
				indexDocs( "spellLifeC", "" );
				indexDocs( "spellLifeC", "A quuxolite is a crystallised mineral found in quartz veins." );

				search name="local.res" collection="spellLifeC" criteria="quuxolit" language="English" suggestions="always" status="local.stats";
				var suggested = [];
				if ( structKeyExists( stats, "keywords" ) ) {
					suggested = suggestedWords( stats );
				}
				expect( ArrayFindNoCase( suggested, "quuxolite" ) ).toBeGT( 0, "the word is suggestible when the dictionary predates the cache, found [#ArrayToList( suggested )#]" );
			});
		});
	}
}
