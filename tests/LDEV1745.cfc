component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" skip=true {
	function beforeAll(){
		variables.uri = createURI("LDEV1745");
	}

	function run( testResults , testBox ) {
		describe( title="LDEV-1745: Lucene lexical search error with multi-word phrases - never fixed in extension", body=function() {
			it( title='checking cfsearch with "+ Test Test"', body=function( currentSpec ) {
				local.result = _InternalRequest(
					template:"#variables.uri#/test.cfm",
					forms:{Scene:1}
				);
			});

			it( title='checking cfsearch with "+ Test Test Test"', body=function( currentSpec ) {
				local.result = _InternalRequest(
					template:"#variables.uri#/test.cfm",
					forms:{Scene:2}
				);
				expect(result.filecontent.trim()).toBe(0);
			});

			it( title='checking cfsearch with "+ Test Test Test Test', body=function( currentSpec ) {
				local.result = _InternalRequest(
					template:"#variables.uri#/test.cfm",
					forms:{Scene:3}
				);
				expect(result.filecontent.trim()).toBe(0);
			});

			it( title='checking cfsearch with "+ Test Test Test Test', body=function( currentSpec ) {
				local.result = _InternalRequest(
					template:"#variables.uri#/test.cfm",
					forms:{Scene:3}
				);
				expect(result.filecontent.trim()).toBe(0);
			});

			it( title='checking cfsearch with "Test Test Test Test', body=function( currentSpec ) {
				local.result = _InternalRequest(
					template:"#variables.uri#/test.cfm",
					forms:{Scene:4}
				);
				expect(result.filecontent.trim()).toBe(0);
			});
		});
	}

	private string function createURI(string calledName){
		var baseURI="/testAdditional/";
		return baseURI&""&calledName;
	}
}