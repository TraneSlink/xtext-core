/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.parsetree.reconstr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.formatting.IFormatter;
import org.eclipse.xtext.parsetree.CompositeNode;
import org.eclipse.xtext.parsetree.NodeAdapter;
import org.eclipse.xtext.parsetree.NodeUtil;
import org.eclipse.xtext.parsetree.reconstr.IParseTreeConstructor.TreeConstructionReport;
import org.eclipse.xtext.parsetree.reconstr.impl.TokenOutputStream;
import org.eclipse.xtext.parsetree.reconstr.impl.TokenStringBuffer;
import org.eclipse.xtext.validation.IConcreteSyntaxValidator;

import com.google.inject.Inject;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class SerializerUtil {

	public static class SerializationOptions {
		
		private boolean format = true;
		private boolean validateConcreteSyntax = false;
		
		public boolean isFormat() {
			return format;
		}

		public void setFormat(boolean format) {
			this.format = format;
		}

		public boolean isValidateConcreteSyntax() {
			return validateConcreteSyntax;
		}

		public void setValidateConcreteSyntax(boolean validateConcreteSyntax) {
			this.validateConcreteSyntax = validateConcreteSyntax;
		}
	}

	private IParseTreeConstructor parseTreeReconstructor;
	private IFormatter formatter;
	private IHiddenTokenMerger merger;
	private IConcreteSyntaxValidator validator;

	@Inject
	public SerializerUtil(IParseTreeConstructor ptc, IFormatter fmt, IHiddenTokenMerger mgr,
			IConcreteSyntaxValidator val) {
		this.parseTreeReconstructor = ptc;
		this.formatter = fmt;
		this.merger = mgr;
		this.validator = val;
	}

	protected CompositeNode getNode(EObject obj) {
		NodeAdapter adapter = NodeUtil.getNodeAdapter(obj);
		if (adapter == null)
			return null;
		CompositeNode n = adapter.getParserNode();
		return n.getParent() == null ? n : n.getParent();
	}

	public TreeConstructionReport serialize(EObject obj, ITokenStream out, CompositeNode node,
			SerializationOptions options) throws IOException {
		if (options.isValidateConcreteSyntax()) {
			List<Diagnostic> diags = new ArrayList<Diagnostic>();
			validator.validateRecursive(obj, new IConcreteSyntaxValidator.DiagnosticListAcceptor(diags),
					new HashMap<Object, Object>());
			if (diags.size() > 0)
				throw new IConcreteSyntaxValidator.InvalidConcreteSyntaxException(
						"These errors need to be fixed before the model an be serialized.", diags);
		}
		ITokenStream t = formatter.createFormatterStream(null, out, !options.isFormat());
		if (node != null)
			t = merger.createHiddenTokenMerger(t, node);
		return parseTreeReconstructor.serialize(obj, t);
	}

	@Deprecated
	// use serialize(EObject, OutputStream, CompositeNode, SerializationOptions) instead
	public TreeConstructionReport serialize(EObject obj, OutputStream out, CompositeNode node, boolean format)
			throws IOException {
		SerializationOptions opt = new SerializationOptions();
		opt.setFormat(format);
		return serialize(obj, out, node, opt);
	}

	public TreeConstructionReport serialize(EObject obj, OutputStream out, CompositeNode node, SerializationOptions opt)
			throws IOException {
		return serialize(obj, new TokenOutputStream(out), node, opt);
	}

	public String serialize(EObject obj) {
		return serialize(obj, new SerializationOptions());
	}

	@Deprecated
	// SerializerUtil.serialize(EObject, SerializationOptions) instead
	public String serialize(EObject obj, boolean format) {
		SerializationOptions opt = new SerializationOptions();
		opt.setFormat(format);
		return serialize(obj, opt);
	}

	public String serialize(EObject obj, SerializationOptions opt) {
		TokenStringBuffer out = new TokenStringBuffer();
		try {
			serialize(obj, out, getNode(obj), opt);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toString();
	}

}
