package org.lucee.extension.search.lucene.query;

import lucee.loader.engine.CFMLEngineFactory;

public final class Concator implements Op {
	
	private Op left;
	private Op right;

	public Concator(Op left,Op right) {
		this.left=left;
		this.right=right;
	}

	@Override
	public String toString() {
		if(left instanceof Literal && right instanceof Literal) {
			String str=((Literal)left).literal+" "+((Literal)right).literal;
			return "\""+CFMLEngineFactory.getInstance().getStringUtil().replace(str, "\"", "\"\"", false,false)+"\"";
		}
		return left+" "+right;
	}
	
}
