package com.mjog.dagrule;

import java.io.IOException;
import java.io.Reader;

public interface DSL {
	public DecisionDag buildDecisionDag(Reader reader, boolean validate) throws IOException, RulesException;
	public DecisionDag buildDecisionDag(String str, boolean validate) throws IOException, RulesException;
}
