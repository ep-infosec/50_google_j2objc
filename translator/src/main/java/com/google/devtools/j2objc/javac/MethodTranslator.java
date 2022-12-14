/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.javac;

import com.google.devtools.j2objc.ast.AbstractTypeDeclaration;
import com.google.devtools.j2objc.ast.ArrayAccess;
import com.google.devtools.j2objc.ast.ArrayCreation;
import com.google.devtools.j2objc.ast.ArrayInitializer;
import com.google.devtools.j2objc.ast.ArrayType;
import com.google.devtools.j2objc.ast.Assignment;
import com.google.devtools.j2objc.ast.Block;
import com.google.devtools.j2objc.ast.BreakStatement;
import com.google.devtools.j2objc.ast.ClassInstanceCreation;
import com.google.devtools.j2objc.ast.ConditionalExpression;
import com.google.devtools.j2objc.ast.ConstructorInvocation;
import com.google.devtools.j2objc.ast.ContinueStatement;
import com.google.devtools.j2objc.ast.DoStatement;
import com.google.devtools.j2objc.ast.EnhancedForStatement;
import com.google.devtools.j2objc.ast.Expression;
import com.google.devtools.j2objc.ast.ExpressionStatement;
import com.google.devtools.j2objc.ast.FieldAccess;
import com.google.devtools.j2objc.ast.ForStatement;
import com.google.devtools.j2objc.ast.IfStatement;
import com.google.devtools.j2objc.ast.InfixExpression;
import com.google.devtools.j2objc.ast.MethodInvocation;
import com.google.devtools.j2objc.ast.NullLiteral;
import com.google.devtools.j2objc.ast.ParenthesizedExpression;
import com.google.devtools.j2objc.ast.PostfixExpression;
import com.google.devtools.j2objc.ast.PrefixExpression;
import com.google.devtools.j2objc.ast.ReturnStatement;
import com.google.devtools.j2objc.ast.SimpleName;
import com.google.devtools.j2objc.ast.SimpleType;
import com.google.devtools.j2objc.ast.SingleVariableDeclaration;
import com.google.devtools.j2objc.ast.SourcePosition;
import com.google.devtools.j2objc.ast.Statement;
import com.google.devtools.j2objc.ast.SuperConstructorInvocation;
import com.google.devtools.j2objc.ast.ThisExpression;
import com.google.devtools.j2objc.ast.TreeNode;
import com.google.devtools.j2objc.ast.TreeUtil;
import com.google.devtools.j2objc.ast.Type;
import com.google.devtools.j2objc.ast.VariableDeclarationExpression;
import com.google.devtools.j2objc.ast.VariableDeclarationFragment;
import com.google.devtools.j2objc.ast.VariableDeclarationStatement;
import com.google.devtools.j2objc.ast.WhileStatement;
import com.google.devtools.j2objc.types.ExecutablePair;
import com.google.devtools.j2objc.types.GeneratedVariableElement;
import com.google.devtools.j2objc.util.ElementUtil;
import com.google.devtools.j2objc.util.ErrorUtil;
import com.google.devtools.j2objc.util.TranslationEnvironment;
import com.google.devtools.j2objc.util.TranslationUtil;
import com.google.devtools.j2objc.util.TypeUtil;
import com.strobel.assembler.metadata.Flags;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.languages.java.ast.AnonymousObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayInitializerExpression;
import com.strobel.decompiler.languages.java.ast.ArraySpecifier;
import com.strobel.decompiler.languages.java.ast.AssignmentExpression;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstNodeCollection;
import com.strobel.decompiler.languages.java.ast.AstType;
import com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.CaseLabel;
import com.strobel.decompiler.languages.java.ast.ClassOfExpression;
import com.strobel.decompiler.languages.java.ast.ComposedType;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.DoWhileStatement;
import com.strobel.decompiler.languages.java.ast.EnumValueDeclaration;
import com.strobel.decompiler.languages.java.ast.ForEachStatement;
import com.strobel.decompiler.languages.java.ast.GotoStatement;
import com.strobel.decompiler.languages.java.ast.IAstVisitor;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.IfElseStatement;
import com.strobel.decompiler.languages.java.ast.ImportDeclaration;
import com.strobel.decompiler.languages.java.ast.IndexerExpression;
import com.strobel.decompiler.languages.java.ast.InstanceInitializer;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.JavaTokenNode;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.MethodGroupExpression;
import com.strobel.decompiler.languages.java.ast.NewLineNode;
import com.strobel.decompiler.languages.java.ast.NullReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.PrimitiveExpression;
import com.strobel.decompiler.languages.java.ast.SuperReferenceExpression;
import com.strobel.decompiler.languages.java.ast.SwitchExpression;
import com.strobel.decompiler.languages.java.ast.SwitchSection;
import com.strobel.decompiler.languages.java.ast.TextNode;
import com.strobel.decompiler.languages.java.ast.ThisReferenceExpression;
import com.strobel.decompiler.languages.java.ast.TryCatchStatement;
import com.strobel.decompiler.languages.java.ast.TypeParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;
import com.strobel.decompiler.languages.java.ast.WildcardType;
import com.strobel.decompiler.patterns.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

/**
 * Procyon AST visitor that converts method/constructor bodies.
 *
 * @author Manvith Narahari
 * @author Tom Ball
 */
@SuppressWarnings("UnnecessarilyFullyQualified")
class MethodTranslator implements IAstVisitor<Void, TreeNode> {
  private final JavacEnvironment parserEnv;
  private final TypeUtil typeUtil;
  private final ExecutableElement executableElement;
  private final AbstractTypeDeclaration typeDecl;
  private final Map<String, VariableElement> localVariableTable;
  private final boolean sourceDebugging;

  public MethodTranslator(JavacEnvironment parserEnv, TranslationEnvironment translationEnv,
                          ExecutableElement executableElement, AbstractTypeDeclaration typeDecl,
                          Map<String, VariableElement> localVariableTable) {
    this.parserEnv = parserEnv;
    this.typeUtil = translationEnv.typeUtil();
    this.executableElement = executableElement;
    this.typeDecl = typeDecl;
    this.localVariableTable = localVariableTable;
    this.sourceDebugging = translationEnv.options().emitLineDirectives();
  }

  protected void visitChildren(final AstNode node, Consumer<TreeNode> builder) {
    for (AstNode child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      builder.accept(copySourcePosition(child, child.acceptVisitor(this, null)));
    }
  }

  protected TreeNode visitChild(final AstNode node) {
    AstNode child = node.getFirstChild();
    assert child == node.getLastChild();
    return copySourcePosition(child, child.acceptVisitor(this, null));
  }

  private TreeNode copySourcePosition(AstNode child, TreeNode node) {
    if (sourceDebugging) {
      int offset = -1;
      if (child instanceof com.strobel.decompiler.languages.java.ast.Expression) {
        offset = ((com.strobel.decompiler.languages.java.ast.Expression) child).getOffset();
      } else if (child instanceof com.strobel.decompiler.languages.java.ast.Statement) {
        offset = ((com.strobel.decompiler.languages.java.ast.Statement) child).getOffset();
      }
      // Approximate length based on decompilation text.
      int length = child.getText().length();
      int line = child.getStartLocation().line();
      node.setPosition(new SourcePosition(offset, length, line));
    }
    return node;
  }

  private TypeMirror resolve(AstType type) {
    return resolve(type.toTypeReference());
  }

  private TypeMirror resolve(TypeReference typeRef) {
    if (typeRef.isArray()) {
      // Multi-dimension arrays will recurse.
      TypeMirror componentType = resolve(typeRef.getElementType());
      return parserEnv.typeUtilities().getArrayType(componentType);
    }
    if (typeRef.isPrimitive() || typeRef.isVoid()) {
      return typeUtil.resolvePrimitiveType(typeRef.getSignature());
    }
    String typeName = typeRef.getFullName();
    Element element = parserEnv.elementUtilities().getTypeElement(typeName);
    // TODO(tball): element is raw, any support for type parameters needed?
    return element.asType();
  }

  @Override
  public TreeNode visitComment(com.strobel.decompiler.languages.java.ast.Comment node, Void data) {
    // Procyon only adds comment nodes to report decompilation errors.
    ErrorUtil.error(node.getContent());
    return null;
  }

  @Override
  public TreeNode visitPatternPlaceholder(AstNode node, Pattern pattern, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitInvocationExpression(InvocationExpression node, Void data) {
    MethodReference methodDef = (MethodReference) node.getUserData(Keys.MEMBER_REFERENCE);
    com.strobel.decompiler.languages.java.ast.Expression target = node.getTarget();
    if (target instanceof SuperReferenceExpression) {
      return target.acceptVisitor(this, null);
    }
    if (target instanceof ThisReferenceExpression) {
      ThisExpression cons = (ThisExpression) target.acceptVisitor(this, null);
      TypeElement type = (TypeElement) ((DeclaredType) cons.getTypeMirror()).asElement();
      List<Expression> args = node.getArguments().stream()
          .map(e -> (Expression) e.acceptVisitor(this, null))
          .collect(Collectors.toList());
      ExecutableElement sym = findConstructor(type, methodDef);
      ConstructorInvocation newNode = new ConstructorInvocation()
          .setExecutablePair(new ExecutablePair(sym))
          .setArguments(args);
      return newNode;
    }
    if (target instanceof MemberReferenceExpression) {
      TypeMirror type = resolve(methodDef.getDeclaringType());
      List<Expression> args = node.getArguments().stream()
          .map(e -> (Expression) e.acceptVisitor(this, null))
          .collect(Collectors.toList());
      ExecutableElement sym = findMethod(methodDef.getName(), type, methodDef);
      Expression expr = (Expression) target.getFirstChild().acceptVisitor(this, null);
      MethodInvocation newNode = new MethodInvocation()
          .setExecutablePair(new ExecutablePair(sym))
          .setTypeMirror(sym.getReturnType())
          .setArguments(args)
          .setExpression(expr);
      return newNode;
    }
    throw new AssertionError("not implemented");
  }

  private ExecutableElement findMethod(String name, TypeMirror type, MethodReference methodDef) {
    TypeElement typeElement = (TypeElement) parserEnv.typeUtilities().asElement(type);
    String signature = methodDef.getSignature();
    String erasedSignature = methodDef.getErasedSignature();
    for (Element e : typeElement.getEnclosedElements()) {
      if (e.getKind() == ElementKind.METHOD && e.getSimpleName().contentEquals(name)) {
        String sig = typeUtil.getReferenceSignature((ExecutableElement) e);
        if (sig.equals(signature) || sig.equals(erasedSignature)) {
          return (ExecutableElement) e;
        }
      }
    }
    throw new AssertionError("failed method lookup: " + type + " " + name + signature);
  }

  private ExecutableElement findConstructor(TypeElement type, MethodReference methodDef) {
    String signature = methodDef.getSignature();
    String erasedSignature = methodDef.getErasedSignature();
    for (Element e : type.getEnclosedElements()) {
      if (e.getKind() == ElementKind.CONSTRUCTOR) {
        String sig = typeUtil.getReferenceSignature((ExecutableElement) e);
        if (sig.equals(signature) || sig.equals(erasedSignature)) {
          return (ExecutableElement) e;
        }
      }
    }
    throw new AssertionError("failed constructor lookup: " + type.getQualifiedName() + signature);
  }

  @Override
  public TreeNode visitTypeReference(TypeReferenceExpression node, Void data) {
    SimpleType typeNode = (SimpleType) node.getFirstChild().acceptVisitor(this, null);
    DeclaredType type = (DeclaredType) typeNode.getTypeMirror();
    return ClassFileConverter.convertName(type.asElement());
  }

  @Override
  public TreeNode visitJavaTokenNode(JavaTokenNode node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitMemberReferenceExpression(MemberReferenceExpression node, Void data) {
    Expression qualifier = (Expression) node.getFirstChild().acceptVisitor(this, null);
    TypeElement owner = (TypeElement) ((DeclaredType) qualifier.getTypeMirror()).asElement();
    VariableElement field = ElementUtil.findField(owner, node.getMemberName());
    if (field != null) {
      return new FieldAccess(field, qualifier);
    }
    throw new AssertionError("not implemented");
  }

  @Override
  public TreeNode visitIdentifier(
      com.strobel.decompiler.languages.java.ast.Identifier node, Void data) {
    return new SimpleName(localVariableTable.get(node.getName()));
  }

  @Override
  public TreeNode visitNullReferenceExpression(NullReferenceExpression node, Void data) {
    return new NullLiteral(typeUtil.getNull());
  }

  @Override
  public TreeNode visitThisReferenceExpression(ThisReferenceExpression node, Void data) {
    return new ThisExpression().setTypeMirror(typeDecl.getTypeElement().asType());
  }

  @Override
  public TreeNode visitSuperReferenceExpression(SuperReferenceExpression node, Void data) {
    TypeMirror objType = typeUtil.getJavaObject().asType();
    TypeMirror nodeType = typeDecl.getTypeElement().asType();
    assert !parserEnv.typeUtilities().isSameType(objType, nodeType);
    TypeElement superClass = TranslationUtil.getSuperType(typeDecl);
    SuperConstructorInvocation superCall = null;
    for (ExecutableElement exec : ElementUtil.getConstructors(superClass)) {
      if (exec.getParameters().size() == 0) {
        ExecutableType execType = typeUtil
            .asMemberOf((DeclaredType) superClass.asType(), exec);
        superCall = new SuperConstructorInvocation()
            .setExecutablePair(new ExecutablePair(exec, execType));
        break;
      }
    }
    return superCall;
  }

  @Override
  public TreeNode visitClassOfExpression(ClassOfExpression node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitBlockStatement(BlockStatement node, Void data) {
    Block block = new Block();
    visitChildren(node, (TreeNode tn) -> block.addStatement((Statement) tn));
    return block;
  }

  @Override
  public TreeNode visitExpressionStatement(
      com.strobel.decompiler.languages.java.ast.ExpressionStatement node, Void data) {
    TreeNode tn = node.getExpression().acceptVisitor(this, null);
    return tn instanceof Statement ? tn : new ExpressionStatement((Expression) tn);
  }

  @Override
  public TreeNode visitBreakStatement(
      com.strobel.decompiler.languages.java.ast.BreakStatement node, Void data) {
    String label = node.getLabel();
    if (StringUtilities.isNullOrEmpty(label)) {
      return new BreakStatement();
    } else {
      return new BreakStatement().setLabel(new SimpleName(label));
    }
  }

  @Override
  public TreeNode visitContinueStatement(
      com.strobel.decompiler.languages.java.ast.ContinueStatement node, Void data) {
    String label = node.getLabel();
    if (StringUtilities.isNullOrEmpty(label)) {
      return new ContinueStatement();
    } else {
      return new ContinueStatement().setLabel(new SimpleName(label));
    }
  }

  @Override
  public TreeNode visitDoWhileStatement(DoWhileStatement node, Void data) {
    return new DoStatement()
        .setExpression((Expression) node.getCondition().acceptVisitor(this, null))
        .setBody((Statement) node.getEmbeddedStatement().acceptVisitor(this, null));
  }

  @Override
  public TreeNode visitEmptyStatement(
      com.strobel.decompiler.languages.java.ast.EmptyStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitIfElseStatement(IfElseStatement node, Void data) {
    IfStatement ifStatement = new IfStatement()
        .setExpression((Expression) node.getCondition().acceptVisitor(this, null))
        .setThenStatement((Statement) node.getTrueStatement().acceptVisitor(this, null));
    com.strobel.decompiler.languages.java.ast.Statement falseBlock = node.getFalseStatement();
    if (falseBlock != null) {
      ifStatement.setElseStatement((Statement) falseBlock.acceptVisitor(this, null));
    }
    return ifStatement;
  }

  @Override
  public TreeNode visitLabelStatement(
      com.strobel.decompiler.languages.java.ast.LabelStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitLabeledStatement(
      com.strobel.decompiler.languages.java.ast.LabeledStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitReturnStatement(
      com.strobel.decompiler.languages.java.ast.ReturnStatement node, Void data) {
    return new ReturnStatement()
        .setExpression((Expression) node.getExpression().acceptVisitor(this, null));
  }

  @Override
  public TreeNode visitSwitchStatement(
      com.strobel.decompiler.languages.java.ast.SwitchStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitSwitchSection(SwitchSection node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitSwitchExpression(SwitchExpression node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitCaseLabel(CaseLabel node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitThrowStatement(
      com.strobel.decompiler.languages.java.ast.ThrowStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitCatchClause(
      com.strobel.decompiler.languages.java.ast.CatchClause node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitAnnotation(
      com.strobel.decompiler.languages.java.ast.Annotation node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitNewLine(NewLineNode node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitVariableDeclaration(
      com.strobel.decompiler.languages.java.ast.VariableDeclarationStatement node, Void data) {
    VariableDeclarationStatement varDecl =  new VariableDeclarationStatement();
    boolean isFinal = node.getModifiers().contains(Flags.Flag.FINAL);
    if (isFinal) {
      varDecl.setModifiers(java.lang.reflect.Modifier.FINAL);
    }
    AstType astType = node.getType();
    Type type = (Type) astType.acceptVisitor(this, null);
    for (VariableInitializer init : node.getVariables()) {
      Expression expr = (Expression) init.acceptVisitor(this, null);
      String varName = init.getName();
      GeneratedVariableElement elem
          = GeneratedVariableElement.newLocalVar(varName, type.getTypeMirror(), executableElement);
      if (isFinal) {
        elem.addModifiers(javax.lang.model.element.Modifier.FINAL);
      }
      localVariableTable.put(varName, elem);
      varDecl.addFragment(new VariableDeclarationFragment(elem, expr));
    }
    return varDecl;
  }

  @Override
  public TreeNode visitVariableInitializer(VariableInitializer node, Void data) {
    return node.getInitializer().acceptVisitor(this, null);
  }

  @Override
  public TreeNode visitText(TextNode node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitImportDeclaration(ImportDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitSimpleType(
      com.strobel.decompiler.languages.java.ast.SimpleType node, Void data) {
    return Type.newType(resolve(node));
  }

  @Override
  public TreeNode visitMethodDeclaration(
      com.strobel.decompiler.languages.java.ast.MethodDeclaration node, Void data) {
    return node.getBody().acceptVisitor(this, null);
  }

  @Override
  public TreeNode visitInitializerBlock(InstanceInitializer node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitConstructorDeclaration(ConstructorDeclaration node, Void data) {
    return node.getBody().acceptVisitor(this, null);
  }

  @Override
  public TreeNode visitTypeParameterDeclaration(TypeParameterDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitParameterDeclaration(ParameterDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitFieldDeclaration(
      com.strobel.decompiler.languages.java.ast.FieldDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitTypeDeclaration(
      com.strobel.decompiler.languages.java.ast.TypeDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitCompilationUnit(
      com.strobel.decompiler.languages.java.ast.CompilationUnit node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitPackageDeclaration(
      com.strobel.decompiler.languages.java.ast.PackageDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitArraySpecifier(ArraySpecifier node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitComposedType(ComposedType node, Void data) {
    return Type.newType(resolve(node));
  }

  @Override
  public TreeNode visitWhileStatement(
      com.strobel.decompiler.languages.java.ast.WhileStatement node, Void data) {
    return new WhileStatement()
        .setExpression((Expression) node.getCondition().acceptVisitor(this, null))
        .setBody((Statement) node.getEmbeddedStatement().acceptVisitor(this, null));
  }

  @Override
  public TreeNode visitPrimitiveExpression(PrimitiveExpression node, Void data) {
    return TreeUtil.newLiteral(node.getValue(), typeUtil);
  }

  @Override
  public TreeNode visitCastExpression(
      com.strobel.decompiler.languages.java.ast.CastExpression node, Void data) {
    // Procyon 0.6+ adds (unnecessary) cast expressions to its decompilation.
    // This skips the cast and just returns its expression.
    return node.getExpression().acceptVisitor(this, null);
  }

  @Override
  public TreeNode visitBinaryOperatorExpression(BinaryOperatorExpression node, Void data) {
    Expression leftExpr = (Expression) node.getLeft().acceptVisitor(this, null);
    Expression rightExpr = (Expression) node.getRight().acceptVisitor(this, null);
    InfixExpression binaryExpr = new InfixExpression()
        .addOperand(leftExpr)
        .addOperand(rightExpr);
    switch (node.getOperator()) {
      case BITWISE_AND:
        binaryExpr.setOperator(InfixExpression.Operator.AND).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case BITWISE_OR:
        binaryExpr.setOperator(InfixExpression.Operator.OR).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case EXCLUSIVE_OR:
        binaryExpr.setOperator(InfixExpression.Operator.XOR).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case LOGICAL_AND:
        binaryExpr.setOperator(InfixExpression.Operator.CONDITIONAL_AND)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case LOGICAL_OR:
        binaryExpr.setOperator(InfixExpression.Operator.CONDITIONAL_OR)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case GREATER_THAN:
        binaryExpr.setOperator(InfixExpression.Operator.GREATER)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case GREATER_THAN_OR_EQUAL:
        binaryExpr.setOperator(InfixExpression.Operator.GREATER_EQUALS)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case LESS_THAN:
        binaryExpr.setOperator(InfixExpression.Operator.LESS)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case LESS_THAN_OR_EQUAL:
        binaryExpr.setOperator(InfixExpression.Operator.LESS_EQUALS)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case EQUALITY:
        binaryExpr.setOperator(InfixExpression.Operator.EQUALS)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case INEQUALITY:
        binaryExpr.setOperator(InfixExpression.Operator.NOT_EQUALS)
            .setTypeMirror(typeUtil.getBoolean());
        break;
      case ADD:
        binaryExpr.setOperator(InfixExpression.Operator.PLUS).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case SUBTRACT:
        binaryExpr.setOperator(InfixExpression.Operator.MINUS).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case MULTIPLY:
        binaryExpr.setOperator(InfixExpression.Operator.TIMES).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case DIVIDE:
        binaryExpr.setOperator(InfixExpression.Operator.DIVIDE).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case MODULUS:
        binaryExpr.setOperator(InfixExpression.Operator.REMAINDER).setTypeMirror(
            typeUtil.binaryNumericPromotion(leftExpr.getTypeMirror(), rightExpr.getTypeMirror()));
        break;
      case SHIFT_LEFT:
        binaryExpr.setOperator(InfixExpression.Operator.LEFT_SHIFT)
            .setTypeMirror(typeUtil.unaryNumericPromotion(leftExpr.getTypeMirror()));
        break;
      case SHIFT_RIGHT:
        binaryExpr.setOperator(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)
            .setTypeMirror(typeUtil.unaryNumericPromotion(leftExpr.getTypeMirror()));
        break;
      case UNSIGNED_SHIFT_RIGHT:
        binaryExpr.setOperator(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED)
            .setTypeMirror(typeUtil.unaryNumericPromotion(leftExpr.getTypeMirror()));
        break;
      default:
        throw new AssertionError("Unsupported infix operator: " + node.getOperator());
    }
    return binaryExpr;
  }

  @Override
  public TreeNode visitInstanceOfExpression(
      com.strobel.decompiler.languages.java.ast.InstanceOfExpression node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitIndexerExpression(IndexerExpression node, Void data) {
    return new ArrayAccess()
        .setArray((Expression) node.getTarget().acceptVisitor(this, null))
        .setIndex((Expression) node.getArgument().acceptVisitor(this, null));
  }

  @Override
  public TreeNode visitIdentifierExpression(IdentifierExpression node, Void data) {
    return visitChild(node);
  }

  @Override
  public TreeNode visitUnaryOperatorExpression(UnaryOperatorExpression node, Void data) {
    Expression expr = (Expression) node.getExpression().acceptVisitor(this, null);
    switch (node.getOperator()) {
      case NOT:
        return new PrefixExpression()
            .setTypeMirror(expr.getTypeMirror())
            .setOperator(PrefixExpression.Operator.NOT)
            .setOperand(expr);
      case BITWISE_NOT:
        return new PrefixExpression()
            .setTypeMirror(typeUtil.unaryNumericPromotion(expr.getTypeMirror()))
            .setOperator(PrefixExpression.Operator.COMPLEMENT)
            .setOperand(expr);
      case PLUS:
        return new PrefixExpression()
            .setTypeMirror(typeUtil.unaryNumericPromotion(expr.getTypeMirror()))
            .setOperator(PrefixExpression.Operator.POSITIVE)
            .setOperand(expr);
      case MINUS:
        return new PrefixExpression()
            .setTypeMirror(typeUtil.unaryNumericPromotion(expr.getTypeMirror()))
            .setOperator(PrefixExpression.Operator.NEGATIVE)
            .setOperand(expr);
      case INCREMENT:
        return new PrefixExpression()
            .setTypeMirror(expr.getTypeMirror())
            .setOperator(PrefixExpression.Operator.INCREMENT)
            .setOperand(expr);
      case DECREMENT:
        return new PrefixExpression()
            .setTypeMirror(expr.getTypeMirror())
            .setOperator(PrefixExpression.Operator.DECREMENT)
            .setOperand(expr);
      case POST_INCREMENT:
        return new PostfixExpression()
            .setOperator(PostfixExpression.Operator.INCREMENT)
            .setOperand(expr);
      case POST_DECREMENT:
        return new PostfixExpression()
            .setOperator(PostfixExpression.Operator.DECREMENT)
            .setOperand(expr);
      default:
        throw new AssertionError("Unsupported unary operator: " + node.getOperator());
    }
  }

  @Override
  public TreeNode visitConditionalExpression(
      com.strobel.decompiler.languages.java.ast.ConditionalExpression node, Void data) {
    Expression conditionExpr = (Expression) node.getCondition().acceptVisitor(this, null);
    Expression trueExpr = (Expression) node.getTrueExpression().acceptVisitor(this, null);
    Expression falseExpr = (Expression) node.getFalseExpression().acceptVisitor(this, null);
    TypeMirror trueType = trueExpr.getTypeMirror();
    TypeMirror falseType = falseExpr.getTypeMirror();
    return new ConditionalExpression()
        .setTypeMirror(typeUtil.inferConditionalExpressionType(trueType, falseType))
        .setExpression(conditionExpr)
        .setThenExpression(trueExpr)
        .setElseExpression(falseExpr);
  }

  @Override
  public TreeNode visitArrayInitializerExpression(ArrayInitializerExpression node, Void data) {
    List<Expression> expressions = node.getElements().stream()
        .map(e -> (Expression) e.acceptVisitor(this, null))
        .collect(Collectors.toList());
    return new ArrayInitializer().setExpressions(expressions);
  }

  @Override
  public TreeNode visitObjectCreationExpression(ObjectCreationExpression node, Void data) {
    Type type = (Type) node.getType().acceptVisitor(this, null);
    TypeElement typeElement = (TypeElement) ((DeclaredType) type.getTypeMirror()).asElement();
    ClassInstanceCreation newNode = new ClassInstanceCreation();
    List<String> paramTypes = new ArrayList<>();
    for (com.strobel.decompiler.languages.java.ast.Expression arg : node.getArguments()) {
      Expression expr = (Expression) arg.acceptVisitor(this,  null);
      newNode.addArgument(expr);
      paramTypes.add(expr.getTypeMirror().toString());
    }
    ExecutableElement constructor =
        ElementUtil.findConstructor(typeElement, paramTypes.toArray(new String[0]));
    return newNode
        .setExecutablePair(new ExecutablePair(constructor))
        .setType(type);
  }

  @Override
  public TreeNode visitArrayCreationExpression(ArrayCreationExpression node, Void data) {
    Type baseType = (Type) node.getType().acceptVisitor(this, null);
    AstNodeCollection<com.strobel.decompiler.languages.java.ast.Expression> dimexprs
        = node.getDimensions();
    ArrayInitializerExpression init = node.getInitializer();
    ArrayType arrayType = new ArrayType(typeUtil.getArrayType(
        baseType.getTypeMirror(), dimexprs.size() + node.getAdditionalArraySpecifiers().size()));
    if (init.isNull()) {
      List<Expression> dimensions = dimexprs.stream()
          .map(e -> (Expression) e.acceptVisitor(this, null))
          .collect(Collectors.toList());
      return new ArrayCreation()
          .setType(arrayType)
          .setDimensions(dimensions);
    } else {
      ArrayInitializer arrayInit = (ArrayInitializer) init.acceptVisitor(this, null);
      return new ArrayCreation()
          .setType(arrayType)
          .setInitializer(arrayInit.setTypeMirror(arrayType.getTypeMirror()));
    }
  }

  @Override
  public TreeNode visitAssignmentExpression(AssignmentExpression node, Void data) {
    Expression leftExpr = (Expression) node.getLeft().acceptVisitor(this, null);
    Expression rightExpr = (Expression) node.getRight().acceptVisitor(this, null);
    Assignment assignment = new Assignment(leftExpr, rightExpr);
    switch (node.getOperator()) {
      case ASSIGN:
        assignment.setOperator(Assignment.Operator.ASSIGN);
        break;
      case ADD:
        assignment.setOperator(Assignment.Operator.PLUS_ASSIGN);
        break;
      case SUBTRACT:
        assignment.setOperator(Assignment.Operator.MINUS_ASSIGN);
        break;
      case MULTIPLY:
        assignment.setOperator(Assignment.Operator.TIMES_ASSIGN);
        break;
      case DIVIDE:
        assignment.setOperator(Assignment.Operator.DIVIDE_ASSIGN);
        break;
      case MODULUS:
        assignment.setOperator(Assignment.Operator.REMAINDER_ASSIGN);
        break;
      case SHIFT_LEFT:
        assignment.setOperator(Assignment.Operator.LEFT_SHIFT_ASSIGN);
        break;
      case SHIFT_RIGHT:
        assignment.setOperator(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN);
        break;
      case UNSIGNED_SHIFT_RIGHT:
        assignment.setOperator(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN);
        break;
      case BITWISE_AND:
        assignment.setOperator(Assignment.Operator.BIT_AND_ASSIGN);
        break;
      case BITWISE_OR:
        assignment.setOperator(Assignment.Operator.BIT_OR_ASSIGN);
        break;
      case EXCLUSIVE_OR:
        assignment.setOperator(Assignment.Operator.BIT_XOR_ASSIGN);
        break;
      default:
        throw new AssertionError("Unsupported assignment operator: " + node.getOperator());
    }
    return assignment;
  }

  @Override
  public TreeNode visitForStatement(
      com.strobel.decompiler.languages.java.ast.ForStatement node, Void data) {
    ForStatement forStatement = new ForStatement();
    for (com.strobel.decompiler.languages.java.ast.Statement init : node.getInitializers()) {
      TreeNode stmt = init.acceptVisitor(this, null);
      if (stmt instanceof VariableDeclarationStatement) {
        VariableDeclarationStatement varDeclStmt = (VariableDeclarationStatement) stmt;
        VariableDeclarationExpression varDeclExpr = new VariableDeclarationExpression()
            .setType(Type.newType(varDeclStmt.getTypeMirror()));
        for (VariableDeclarationFragment varFrag : varDeclStmt.getFragments()) {
          varDeclExpr.addFragment(varFrag.copy());
        }
        forStatement.addInitializer(varDeclExpr);
      } else {
        forStatement.addInitializer(((ExpressionStatement) stmt).getExpression().copy());
      }
    }
    forStatement.setExpression((Expression) node.getCondition().acceptVisitor(this, null));
    for (com.strobel.decompiler.languages.java.ast.Statement updater : node.getIterators()) {
      TreeNode stmt = updater.acceptVisitor(this, null);
      forStatement.addUpdater(((ExpressionStatement) stmt).getExpression().copy());
    }
    forStatement.setBody((Statement) node.getEmbeddedStatement().acceptVisitor(this, null));
    return forStatement;
  }

  @Override
  public TreeNode visitForEachStatement(ForEachStatement node, Void data) {
    EnhancedForStatement forStatement = new EnhancedForStatement();
    AstType astType = node.getVariableType();
    Type type = (Type) astType.acceptVisitor(this, null);
    String varName = node.getVariableName();
    GeneratedVariableElement elem
        = GeneratedVariableElement.newLocalVar(varName, type.getTypeMirror(), executableElement);
    if (node.getVariableModifiers().contains(Flags.Flag.FINAL)) {
      elem.addModifiers(javax.lang.model.element.Modifier.FINAL);
    }
    localVariableTable.put(varName, elem);
    return forStatement.setParameter(new SingleVariableDeclaration(elem))
        .setExpression((Expression) node.getInExpression().acceptVisitor(this, null))
        .setBody((Statement) node.getEmbeddedStatement().acceptVisitor(this, null));
  }

  @Override
  public TreeNode visitTryCatchStatement(TryCatchStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitGotoStatement(GotoStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitParenthesizedExpression(
      com.strobel.decompiler.languages.java.ast.ParenthesizedExpression node, Void data) {
    Expression innerExpr = (Expression) node.getExpression().acceptVisitor(this, null);
    // Remove unnecessary parentheses, added by Procyon 0.6+ decompiler.
    if (innerExpr.getKind() == TreeNode.Kind.SIMPLE_NAME) {
      return innerExpr;
    }
    return ParenthesizedExpression.parenthesize(innerExpr);
  }

  @Override
  public TreeNode visitSynchronizedStatement(
      com.strobel.decompiler.languages.java.ast.SynchronizedStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression node,
      Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitWildcardType(WildcardType node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitMethodGroupExpression(MethodGroupExpression node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitEnumValueDeclaration(EnumValueDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitAssertStatement(
      com.strobel.decompiler.languages.java.ast.AssertStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitLambdaExpression(
      com.strobel.decompiler.languages.java.ast.LambdaExpression node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitLocalTypeDeclarationStatement(
      com.strobel.decompiler.languages.java.ast.LocalTypeDeclarationStatement node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitBytecodeConstant(
      com.strobel.decompiler.languages.java.ast.BytecodeConstant node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitInlinedBytecode(
      com.strobel.decompiler.languages.java.ast.InlinedBytecodeExpression node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitUnionType(
      com.strobel.decompiler.languages.java.ast.UnionType node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitIntersectionType(
      com.strobel.decompiler.languages.java.ast.IntersectionType node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitModuleDeclaration(
      com.strobel.decompiler.languages.java.ast.ModuleDeclaration node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }

  @Override
  public TreeNode visitSwitchExpressionArm(
      com.strobel.decompiler.languages.java.ast.SwitchExpressionArm node, Void data) {
    throw new AssertionError("Method not yet implemented");
  }
}
