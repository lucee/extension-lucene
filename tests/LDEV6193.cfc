component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" {

	function beforeAll() {
		variables.path = server._getTempDir( "LDEV6193" );

		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
		directoryCreate( variables.path );

		collection
			action="create"
			collection="LDEV6193"
			path="#variables.path#"
			language="English";

		var qry = QueryNew( 'id,title,body' );

		var row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "1", row );
		QuerySetCell( qry, "title", "Lucee Server", row );
		QuerySetCell( qry, "body", "Lucee is an open source CFML application server written in Java", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "2", row );
		QuerySetCell( qry, "title", "Apache Tomcat", row );
		QuerySetCell( qry, "body", "Tomcat is a Java servlet container for running web applications", row );

		row = QueryAddRow( qry );
		QuerySetCell( qry, "id", "3", row );
		QuerySetCell( qry, "title", "Python Flask", row );
		QuerySetCell( qry, "body", "Flask is a lightweight Python web framework for building APIs", row );

		index
			collection="LDEV6193"
			action="update"
			type="custom"
			title="title"
			body="body"
			key="id"
			query="qry"
			urlpath="/";
	}

	function afterAll() {
		try { collection action="delete" collection="LDEV6193"; } catch( any e ) {}
		if ( DirectoryExists( variables.path ) ) {
			directoryDelete( variables.path, true );
		}
	}

	function run( testResults, testBox ) {
		describe( title="LDEV-6193: type=explicit for native Lucene query syntax", body=function() {

			it( title="basic term search with type=explicit", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Lucee" type="explicit" language="English";
				expect( res.recordcount ).toBe( 1 );
			});

			it( title="+MUST modifier", body=function() {
				search name="local.res" collection="LDEV6193" criteria="+Java +open" type="explicit" language="English";
				expect( res.recordcount ).toBe( 1, "should only match the doc with both Java AND open" );
			});

			it( title="-MUST_NOT modifier", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Java -servlet" type="explicit" language="English";
				expect( res.recordcount ).toBe( 1, "should match Java doc but exclude servlet doc" );
			});

			it( title="wildcard with ?", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Luce?" type="explicit" language="English";
				expect( res.recordcount ).toBe( 1 );
			});

			it( title="wildcard with *", body=function() {
				search name="local.res" collection="LDEV6193" criteria="serv*" type="explicit" language="English";
				expect( res.recordcount ).toBeGT( 0, "should match server and/or servlet" );
			});

			it( title="phrase query", body=function() {
				search name="local.res" collection="LDEV6193" criteria='"open source"' type="explicit" language="English";
				expect( res.recordcount ).toBe( 1 );
			});

			it( title="boolean grouping", body=function() {
				search name="local.res" collection="LDEV6193" criteria="(Java OR Python) AND web" type="explicit" language="English";
				expect( res.recordcount ).toBe( 2, "should match Tomcat and Flask docs" );
			});

			it( title="fuzzy search", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Luce~1" type="explicit" language="English";
				expect( res.recordcount ).toBeGT( 0, "fuzzy match should find Lucee" );
			});

			it( title="boost", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Java^4 Python" type="explicit" language="English";
				expect( res.recordcount ).toBeGT( 0, "boosted query should return results" );
			});

			it( title="criteria=* returns all docs", body=function() {
				search name="local.res" collection="LDEV6193" criteria="*" type="explicit" language="English";
				expect( res.recordcount ).toBe( 3, "should return all documents" );
			});

			it( title="type=simple still works (regression)", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Lucee" type="simple" language="English";
				expect( res.recordcount ).toBe( 1 );
			});

			it( title="no type defaults to simple (regression)", body=function() {
				search name="local.res" collection="LDEV6193" criteria="Lucee" language="English";
				expect( res.recordcount ).toBe( 1 );
			});
		});
	}
}
