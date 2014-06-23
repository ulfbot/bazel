// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.syntax;

import com.google.devtools.build.lib.events.ErrorEventListener;
import com.google.devtools.build.lib.events.Location;
import com.google.devtools.build.lib.packages.CachingPackageLocator;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract syntax node for an entire BUILD file.
 */
// TODO(bazel-team): Should be final;
public class BuildFileAST extends ASTNode {

  private final List<Statement> stmts;

  private final List<Comment> comments;

  private final List<PathFragment> imports;

  /**
   * Whether any errors were encountered during scanning or parsing.
   */
  private final boolean containsErrors;

  private BuildFileAST(Lexer lexer, Parser.ParseResult result) {
    this.stmts = result.statements;
    this.comments = result.comments;
    this.containsErrors = result.containsErrors;
    this.imports = fetchImports(result.statements);
    if (result.statements.size() > 0) {
      setLocation(lexer.createLocation(
          stmts.get(0).getLocation().getStartOffset(),
          stmts.get(stmts.size() - 1).getLocation().getEndOffset()));
    } else {
      setLocation(Location.fromFile(lexer.getFilename()));
    }
  }

  private List<PathFragment> fetchImports(List<Statement> stmts) {
    List<PathFragment> imports = new ArrayList<>();
    for (Statement stmt : stmts) {
      if (stmt instanceof ImportStatement) {
        ImportStatement imp = (ImportStatement) stmt;
        imports.add(imp.getImportPath());
      }
    }
    return imports;
  }

  /**
   * Returns true if any errors were encountered during scanning or parsing. If
   * set, clients should not rely on the correctness of the AST for builds or
   * BUILD-file editing.
   */
  public boolean containsErrors() {
    return containsErrors;
  }

  /**
   * Returns an (immutable, ordered) list of statements in this BUILD file.
   */
  public List<Statement> getStatements() {
    return stmts;
  }

  /**
   * Returns an (ordered) list of comments in this BUILD file.
   */
  public List<Comment> getComments() {
    return comments;
  }

  /**
   * Returns an (ordered) list of imports in this BUILD file.
   */
  public List<PathFragment> getImports() {
    return imports;
  }

  /**
   * Executes this build file in a given Environment.
   *
   * <p>If, for any reason, execution of a statement cannot be completed, an
   * {@link EvalException} is thrown by {@link Statement#exec(Environment)}.
   * This exception is caught here and reported through reporter and execution
   * continues on the next statement.  In effect, there is a "try/except" block
   * around every top level statement.  Such exceptions are not ignored, though:
   * they are visible via the return value.  Rules declared in a package
   * containing any error (including loading-phase semantical errors that
   * cannot be checked here) must also be considered "in error".
   *
   * <p>Note that this method will not affect the value of {@link
   * #containsErrors()}; that refers only to lexer/parser errors.
   *
   * @return true if no error occurred during execution.
   */
  public boolean exec(Environment env, ErrorEventListener listener) throws InterruptedException {
    boolean ok = true;
    for (Statement stmt : stmts) {
      try {
        stmt.exec(env);
      } catch (EvalException e) {
        // Do not report errors caused by a previous parsing error, as it has already been
        // reported.
        if (!e.isDueToIncompleteAST()) {
          listener.error(e.getLocation(), e.getMessage());
        }
        ok = false;
      }
    }
    return ok;
  }

  @Override
  public String toString() {
    return "BuildFileAST" + getStatements();
  }

  @Override
  public void accept(SyntaxTreeVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Parse the specified build file, returning its AST. All errors during
   * scanning or parsing will be reported to the reporter.
   *
   * @throws IOException if the file cannot not be read.
   */
  public static BuildFileAST parseBuildFile(Path buildFile, ErrorEventListener listener,
                                            CachingPackageLocator locator, boolean parsePython)
      throws IOException {
    ParserInputSource inputSource = ParserInputSource.create(buildFile);
    return parseBuildFile(inputSource, listener, locator, parsePython);
  }

  /**
   * Parse the specified build file, returning its AST. All errors during
   * scanning or parsing will be reported to the reporter.
   */
  public static BuildFileAST parseBuildFile(ParserInputSource input, ErrorEventListener listener,
                                            CachingPackageLocator locator, boolean parsePython) {
    Lexer lexer = new Lexer(input, listener, parsePython);
    Parser.ParseResult result = Parser.parseFile(lexer, listener, locator, parsePython);
    return new BuildFileAST(lexer, result);
  }

  /**
   * Parse the specified build file, returning its AST. All errors during
   * scanning or parsing will be reported to the reporter.
   */
  public static BuildFileAST parseBuildFile(Lexer lexer, ErrorEventListener listener) {
    Parser.ParseResult result = Parser.parseFile(lexer, listener, null, false);
    return new BuildFileAST(lexer, result);
  }

  /**
   * Parse the specified Skylark file, returning its AST. All errors during
   * scanning or parsing will be reported to the reporter.
   *
   * @throws IOException if the file cannot not be read.
   */
  public static BuildFileAST parseSkylarkFile(Path file, ErrorEventListener listener,
      CachingPackageLocator locator, ValidationEnvironment validationEnvironment)
          throws IOException {
    ParserInputSource input = ParserInputSource.create(file);
    Lexer lexer = new Lexer(input, listener, false);
    Parser.ParseResult result =
        Parser.parseFileForSkylark(lexer, listener, locator, validationEnvironment);
    return new BuildFileAST(lexer, result);
  }

  /**
   * Parse the specified build file, without building the AST.
   *
   * @return true if the input file is syntactically valid
   */
  public static boolean checkSyntax(ParserInputSource input,
                                    ErrorEventListener listener, boolean parsePython) {
    return !parseBuildFile(input, listener, null, parsePython).containsErrors();
  }
}