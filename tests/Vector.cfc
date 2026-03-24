component extends="org.lucee.cfml.test.LuceeTestCase" labels="search"{
	
	function run( testResults, testBox ) {
		describe( title="vector and hybrid search", skip=isNotSupported(), body=function() {

			it( title="vector search with word2vec embedding", body=function() {
				var vectorsFile = generateTestVectors();
				var path = server._getTempDir( "vector-test-word2vec" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				collection
					action="create"
					collection="vectorW2V"
					path="#path#"
					language="English"
					mode="vector"
					embedding="#vectorsFile#";

				var qry = QueryNew( 'id,title,body' );
				var row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "1", row );
				QuerySetCell( qry, "title", "Cats", row );
				QuerySetCell( qry, "body", "Cats are independent pets that love to sleep and play", row );

				row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "2", row );
				QuerySetCell( qry, "title", "Dogs", row );
				QuerySetCell( qry, "body", "Dogs are loyal companions that enjoy walks and fetching", row );

				row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "3", row );
				QuerySetCell( qry, "title", "Cars", row );
				QuerySetCell( qry, "body", "Cars are vehicles powered by engines that drive on roads", row );

				index
					collection="vectorW2V"
					action="update"
					type="custom"
					title="title"
					body="body"
					key="id"
					query="qry"
					urlpath="/";

				search name="local.res" collection="vectorW2V" criteria="pet animals" language="English";
				expect( res.recordcount ).toBeGT( 0, "vector search should return results" );

				collection
					action="delete"
					collection="vectorW2V";
			});

			it( title="vector search with TF-IDF embedding", skip=isNotSupported(),  body=function() {
				var path = server._getTempDir( "vector-test-tfidf" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				collection
					action="create"
					collection="vectorTFIDF"
					path="#path#"
					language="English"
					mode="vector"
					embedding="TF-IDF";

				var qry = QueryNew( 'id,title,body' );
				var row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "1", row );
				QuerySetCell( qry, "title", "Java Programming", row );
				QuerySetCell( qry, "body", "Java is an object oriented programming language used for building enterprise applications", row );

				row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "2", row );
				QuerySetCell( qry, "title", "Python Programming", row );
				QuerySetCell( qry, "body", "Python is a scripting language popular for data science and machine learning", row );

				index
					collection="vectorTFIDF"
					action="update"
					type="custom"
					title="title"
					body="body"
					key="id"
					query="qry"
					urlpath="/";

				search name="local.res" collection="vectorTFIDF" criteria="programming language" language="English";
				expect( res.recordcount ).toBeGT( 0, "TF-IDF search should return results" );

				collection
					action="delete"
					collection="vectorTFIDF";
			});

			it( title="hybrid search with ratio weighting", skip=isNotSupported(),  body=function() {
				var vectorsFile = generateTestVectors();
				var path = server._getTempDir( "vector-test-hybrid" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				collection
					action="create"
					collection="vectorHybrid"
					path="#path#"
					language="English"
					mode="hybrid"
					embedding="#vectorsFile#"
					ratio="0.7";

				var qry = QueryNew( 'id,title,body' );
				var row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "1", row );
				QuerySetCell( qry, "title", "Lucee Server", row );
				QuerySetCell( qry, "body", "Lucee is an open source CFML server deployed as a Java servlet", row );

				row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "2", row );
				QuerySetCell( qry, "title", "Apache Tomcat", row );
				QuerySetCell( qry, "body", "Tomcat is a Java servlet container used to run web applications", row );

				index
					collection="vectorHybrid"
					action="update"
					type="custom"
					title="title"
					body="body"
					key="id"
					query="qry"
					urlpath="/";

				search name="local.res" collection="vectorHybrid" criteria="CFML web server" language="English";
				expect( res.recordcount ).toBeGT( 0, "hybrid search should return results" );

				collection
					action="delete"
					collection="vectorHybrid";
			});

			it( title="context passages extraction", skip=isNotSupported(), body=function() {
				var vectorsFile = generateTestVectors();
				var path = server._getTempDir( "vector-test-context" );

				if ( DirectoryExists( path ) ) {
					directoryDelete( path, true );
				}
				directoryCreate( path );

				collection
					action="create"
					collection="vectorContext"
					path="#path#"
					language="English"
					mode="vector"
					embedding="#vectorsFile#";

				var qry = QueryNew( 'id,title,body' );
				var row = QueryAddRow( qry );
				QuerySetCell( qry, "id", "1", row );
				QuerySetCell( qry, "title", "Long Document", row );
				QuerySetCell( qry, "body", "This is a long document about software development. Software engineers write code to solve problems. Testing is an important part of the development process. Continuous integration helps teams deliver quality software faster.", row );

				index
					collection="vectorContext"
					action="update"
					type="custom"
					title="title"
					body="body"
					key="id"
					query="qry"
					urlpath="/";

				search
					name="local.res"
					collection="vectorContext"
					criteria="testing software"
					language="English"
					contextpassages="2"
					contextBytes="500"
					contextpassageLength="200";
				expect( res.recordcount ).toBeGT( 0, "context passage search should return results" );

				collection
					action="delete"
					collection="vectorContext";
			});
		});
	}

	private boolean function isNotSupported() {
		return listFirst( server.lucee.version, "." ) < 7;
	}

	/**
	 * Generate a synthetic GloVe-format vectors file with clustered word embeddings.
	 * Words in the same cluster have similar vectors so cosine similarity works for testing.
	 */
	private string function generateTestVectors() {
		var vectorsDir = server._getTempDir( "vector-test-embeddings" );
		var vectorsFile = vectorsDir & "/test-vectors.txt";

		if ( DirectoryExists( vectorsDir ) ) {
			directoryDelete( vectorsDir, true );
		}
		directoryCreate( vectorsDir );

		var dims = 50;
		var rng = createObject( "java", "java.util.Random" ).init( 42 );

		// define word clusters with base vectors — related words share a cluster
		var clusters = {
			"animals": [ "cat", "cats", "dog", "dogs", "pet", "pets", "animals", "independent", "loyal",
				"companions", "love", "sleep", "play", "enjoy", "walks", "fetching" ],
			"vehicles": [ "car", "cars", "vehicles", "powered", "engines", "engine", "drive", "roads", "road" ],
			"tech": [ "java", "server", "servlet", "web", "applications", "application", "container",
				"deployed", "run", "open", "source", "cfml", "lucee", "tomcat", "apache" ],
			"software": [ "software", "development", "engineers", "write", "code", "solve", "problems",
				"testing", "important", "process", "continuous", "integration", "helps", "teams",
				"deliver", "quality", "faster", "programming", "language", "object", "oriented",
				"building", "enterprise", "scripting", "popular", "data", "science", "machine", "learning" ],
			"common": [ "is", "a", "an", "the", "are", "that", "to", "and", "for", "of", "this",
				"long", "document", "about", "part" ]
		};

		// generate a stable base vector per cluster
		var baseVectors = {};
		for ( var clusterName in clusters ) {
			var base = [];
			for ( var d = 1; d <= dims; d++ ) {
				base.append( rng.nextGaussian() );
			}
			baseVectors[ clusterName ] = base;
		}

		var sb = createObject( "java", "java.lang.StringBuilder" );

		for ( var clusterName in clusters ) {
			var words = clusters[ clusterName ];
			var base = baseVectors[ clusterName ];

			for ( var word in words ) {
				sb.append( word );
				for ( var d = 1; d <= dims; d++ ) {
					// base vector + small noise so words in same cluster are similar but not identical
					sb.append( " " );
					sb.append( base[ d ] + rng.nextGaussian() * 0.1 );
				}
				sb.append( chr( 10 ) );
			}
		}

		fileWrite( vectorsFile, sb.toString(), "UTF-8" );
		return vectorsFile;
	}


}
