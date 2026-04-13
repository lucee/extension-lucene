component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" skip=true {

	// LDEV-1166: Lucene index updates not reflected across servers sharing a SAN path.
	// This cannot be meaningfully tested in a single-process environment —
	// it requires multiple Lucee instances pointing at the same index directory.
	// Skipped; manual validation required in a clustered environment.

	function run( testResults, testBox ) {
		describe( title="LDEV-1166: shared index across servers (manual test only)", body=function() {

			it( title="placeholder — requires multi-server setup", body=function() {
				// This ticket describes index updates on server A not being visible
				// on server B when both share the same collection path via SAN/NFS.
				// The underlying issue is likely Lucene IndexReader caching.
				skip( "requires multi-server environment to validate" );
			});
		});
	}
}
