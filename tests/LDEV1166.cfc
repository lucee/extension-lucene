component extends="org.lucee.cfml.test.LuceeTestCase" labels="search" skip=true {

	// LDEV-1166: Lucene index updates not reflected across servers sharing a SAN path.
	//
	// Won't fix. A writable Lucene index directory shared between processes is not a supported deployment:
	//
	//  - Lucene allows one IndexWriter per directory, and the write lock comes from a native lock factory
	//    that is per-process rather than an OS-level lock, so two servers on a UNC/SMB/NFS share are never
	//    mutually excluded. Concurrent writers corrupt the index whether or not readers notice.
	//  - A reader is a per-JVM snapshot of segments_N. This extension refreshes its reader when *it* writes
	//    (indexComplete), and refreshes nothing on the read path, so a directory written by another process
	//    is deliberately not picked up. Re-checking segments_N on every query to serve that case cost a
	//    filesystem read per index per search on the hot path.
	//
	// cfsearch/cfindex expose one JVM's view of a directory. Coordinating between JVMs is the caller's job,
	// the same way cfquery gives you a statement and not a distributed transaction. Applications that need
	// search across servers want a search server (Solr, Elasticsearch), where the index is not a shared
	// writable directory at all.
	//
	// Kept as a skipped placeholder rather than deleted because the ticket stays open: if this is ever
	// revisited it needs two Lucee instances on one collection path, which no single-process run can show.

	function run( testResults, testBox ) {
		describe( title="LDEV-1166: shared index across servers (manual test only)", body=function() {

			it( title="placeholder — requires multi-server setup", body=function() {
				// This ticket describes index updates on server A not being visible
				// on server B when both share the same collection path via SAN/NFS.
				// Server A writes, server B keeps its own snapshot and never re-reads segments_N.
				skip( "not supported: a collection directory belongs to one JVM, see the note at the top of this file" );
			});
		});
	}
}
