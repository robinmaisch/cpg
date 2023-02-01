/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.ogm.annotation.Relationship;

/**
 * An expression, which refers to something which is declared, e.g. a variable. For example, the
 * expression <code>a = b</code>, which itself is a {@link BinaryOperator}, contains two {@link
 * DeclaredReferenceExpression}s, one for the variable <code>a</code> and one for variable <code>b
 * </code>, which have been previously been declared.
 */
public class DeclaredReferenceExpression extends Expression
    implements TypeListener, AssignmentTarget {

  /** The {@link Declaration}s this expression might refer to. */
  @Relationship(value = "REFERS_TO")
  @Nullable
  private Declaration refersTo;

  /**
   * Is this reference used for writing data instead of just reading it? Determines dataflow
   * direction
   */
  private AccessValues access = AccessValues.READ;

  private boolean staticAccess = false;

  public boolean isStaticAccess() {
    return staticAccess;
  }

  public void setStaticAccess(boolean staticAccess) {
    this.staticAccess = staticAccess;
  }

  public @Nullable Declaration getRefersTo() {
    return this.refersTo;
  }

  /**
   * Returns the contents of {@link #refersTo} as the specified class, if the class is assignable.
   * Otherwise, it will return null.
   *
   * @param clazz the expected class
   * @param <T> the type
   * @return the declaration cast to the expected class, or null if the class is not assignable
   */
  public @Nullable <T extends VariableDeclaration> T getRefersToAs(Class<T> clazz) {
    if (this.refersTo == null) {
      return null;
    }

    return clazz.isAssignableFrom(this.refersTo.getClass()) ? clazz.cast(this.refersTo) : null;
  }

  public AccessValues getAccess() {
    return access;
  }

  public void setRefersTo(@Nullable Declaration refersTo) {
    if (refersTo == null) {
      return;
    }
    var current = this.refersTo;

    // unregister type listeners for current declaration
    if (current != null) {
      if (current instanceof ValueDeclaration) {
        ((ValueDeclaration) current).unregisterTypeListener(this);
      }
      if (current instanceof TypeListener) {
        this.unregisterTypeListener((TypeListener) current);
      }
    }

    // set it
    this.refersTo = refersTo;
    if (refersTo instanceof ValueDeclaration) {
      ((ValueDeclaration) refersTo).addUsage(this);
    }

    // update type listeners
    if (this.refersTo instanceof ValueDeclaration) {
      ((ValueDeclaration) this.refersTo).registerTypeListener(this);
    }
    if (this.refersTo instanceof TypeListener) {
      this.registerTypeListener((TypeListener) this.refersTo);
    }
  }

  @Override
  public void typeChanged(HasType src, List<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.type;
    setType(src.getPropagationType(), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, List<HasType> root) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    List<Type> subTypes = new ArrayList<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public @NotNull String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .append(super.toString())
        .append("refersTo", refersTo)
        .toString();
  }

  public void setAccess(AccessValues access) {
    // set the access
    this.access = access;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeclaredReferenceExpression that)) {
      return false;
    }
    return super.equals(that) && Objects.equals(refersTo, that.refersTo);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
