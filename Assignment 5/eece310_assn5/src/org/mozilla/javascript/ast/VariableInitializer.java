/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Steve Yegge
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * A variable declaration or initializer, part of a {@link VariableDeclaration}
 * expression.  The variable "target" can be a simple name or a destructuring
 * form.  The initializer, if present, can be any expression.<p>
 *
 * Node type is one of {@link Token#VAR}, {@link Token#CONST}, or
 * {@link Token#LET}.<p>
 */
public class VariableInitializer extends AstNode implements Cloneable {

    private AstNode target;
    private AstNode initializer;

    {
        type = Token.VAR;
    }

    /**
     * Sets the node type.
     * @throws IllegalArgumentException if {@code nodeType} is not one of
     * {@link Token#VAR}, {@link Token#CONST}, or {@link Token#LET}
     */
    public void setNodeType(int nodeType) {
        if (nodeType != Token.VAR
            && nodeType != Token.CONST
            && nodeType != Token.LET)
            throw new IllegalArgumentException("invalid node type");
        setType(nodeType);
    }

    public VariableInitializer() {
    }

    public VariableInitializer(int pos) {
        super(pos);
    }

    public VariableInitializer(int pos, int len) {
        super(pos, len);
    }


    /**
     * Returns true if this is a destructuring assignment.  If so, the
     * initializer must be non-{@code null}.
     * @return {@code true} if the {@code target} field is a destructuring form
     * (an {@link ArrayLiteral} or {@link ObjectLiteral} node)
     */
    public boolean isDestructuring() {
        return !(target instanceof Name);
    }

    /**
     * Returns the variable name or destructuring form
     */
    public AstNode getTarget() {
        return target;
    }

    /**
     * Sets the variable name or destructuring form, and sets
     * its parent to this node.
     * @throws IllegalArgumentException if target is {@code null}
     */
    public void setTarget(AstNode target) {
        // Don't throw exception if target is an "invalid" node type.
        // See mozilla/js/tests/js1_7/block/regress-350279.js
        if (target == null)
            throw new IllegalArgumentException("invalid target arg");
        this.target = target;
        target.setParent(this);
    }

    /**
     * Returns the initial value, or {@code null} if not provided
     */
    public AstNode getInitializer() {
        return initializer;
    }

    /**
     * Sets the initial value expression, and sets its parent to this node.
     * @param initializer the initial value.  May be {@code null}.
     */
    public void setInitializer(AstNode initializer) {
        this.initializer = initializer;
        if (initializer != null)
            initializer.setParent(this);
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append(target.toSource(0));
        if (initializer != null) {
            sb.append (" = ");
            sb.append(initializer.toSource(0));
        }
        return sb.toString();
    }

    /**
     * Visits this node, then the target expression, then the initializer
     * expression if present.
     */
    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            target.visit(v);
            if (initializer != null) {
                initializer.visit(v);
            }
        }
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
    	VariableInitializer newNode = new VariableInitializer(this.position, this.length);
    	newNode.setTarget(this.getTarget());
    	newNode.setInitializer(this.getInitializer());
    	newNode.type = this.type;
    	newNode.setParent(this.parent);
    	return newNode;
    }
}