component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" skip=true {
	function beforeAll(){
		variables.uri = createURI("LDEV1684");
		if(directoryExists(variables.uri)){
			directoryDelete(variables.uri);
		}
		directoryCreate(variables.uri);
	}
	function run( testResults , testBox ) {
		describe( title="LDEV-1684: new Collection() ACF10 compat - never implemented", body=function() {
			it( title='checking CFcollection in script with action create', body=function( currentSpec ) {
				var obj = new collection().CREATE(collection="TEST1", path="#variables.uri#", language="english");
				var list = new collection().list(name="test1");
				expect(list.getresult().name.RecordCount).toBe(1);
			});

			it( title='checking CFcollection in script with action list', body=function( currentSpec ) {
				var list = new collection().list(name="test1");
				expect(list.getresult().name.RecordCount).toBe(1);
			});

			it( title='checking CFcollection in script with action delete', body=function( currentSpec ) {
				var obj = new collection().Delete(collection="TEST1");
				var list = new collection().list(name="test1");
				expect(list.getresult().name.RecordCount).toBe(0);
			});
		});
	}
	private string function createURI(string calledName){
		var baseURI="/testAdditional/";
		return baseURI&""&calledName;
	}
}