component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" skip=true {
	function beforeAll(){
		variables.uri = createURI("LDEV1901");
	}
	function run( testResults , testBox ) {
		describe( title="LDEV-1901: Lucene search with ^ - never fixed in extension", body=function() {
			it( title='Checking criteria with ^ in cfsearch', body=function( currentSpec ) {
				include template="#variables.uri#/test.cfm";
				local.result = _InternalRequest(
					template:"#variables.uri#/test2.cfm");
				expect(result.filecontent.trim()).toBe("true");
			});
		});
	}
	private string function createURI(string calledName){
		var baseURI="/testAdditional/";
		return baseURI&""&calledName;
	}
} 