//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 12:04:53 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

import java.util.AbstractList;

public class Definition extends ProtoExpression
{
  private Symbol _symbol;
  private Expression _body;

  private DefinedEntry _codeEntry;
  private boolean _isField = false;
  private boolean _isProjection = false;
  private boolean _isSetOnEvaluation = false;

  public Definition (Symbol symbol, Expression body)
    {
      _symbol = symbol;
      _body = body;
    }

  public Definition (Tables tables, String name, Expression body)
    {
      _symbol = tables.symbol(name);
      _body = body;
    }

  public Definition (Tables tables, String symbol, String arg, Expression body)
    {
      this(tables,symbol,new Abstraction(arg,body));
    }

  public Definition (Tables tables, String symbol, AbstractList args, Expression body)
    {
      this(tables,symbol,new Abstraction(args,body));
    }

  public Definition (Tables tables, String symbol, Expression body, boolean isField)
    {
      this(tables,symbol,body);
      _isField = isField;
    }

  public Definition (Tables tables, String symbol, AbstractList args, Expression body, boolean isField)
    {
      this(tables,symbol,args,body);
      _isField = isField;
    }

  public final Expression copy ()
    {
      Definition copy = new Definition(_symbol,_body.copy());
      copy._codeEntry = _codeEntry;
      copy._isField = _isField;
      copy._isProjection = _isProjection;
      copy._isSetOnEvaluation = _isSetOnEvaluation;
      return copy;
    }

  public final Expression typedCopy ()
    {
      Definition copy = (Definition)new Definition(_symbol,_body.typedCopy()).addTypes(this);
      copy._codeEntry = _codeEntry;
      copy._isField = _isField;
      copy._isProjection = _isProjection;
      copy._isSetOnEvaluation = _isSetOnEvaluation;
      return copy;
    }

  public final int numberOfSubexpressions ()
    {
      return 1;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n == 0)
        return _body;

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      if (n == 0)
        _body = expression;
      else
        throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final Symbol symbol ()
    {
      return _symbol;
    }

  public final boolean isProjection ()
    {
      return _isProjection;
    }

  public final Definition setIsProjection ()
    {
      _isProjection = true;
      return this;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _body.setCheckedType();
      setCheckedType(type().standardize());
    }

  public final DefinedEntry codeEntry ()
    {
      return _codeEntry;
    }

  public final Definition setOnEvaluation ()
    {
      _isSetOnEvaluation = true;
      return this;
    }

  public final boolean isSetOnEvaluation ()
    {
      return _isSetOnEvaluation;
    }

  /**
   * This method registers a definitively type-checked definition of a global symbol.
   * It must be called <i>only</i> after type-checking of the definition has been
   * completed; namely, after the <tt>setCheckedType</tt> method has been invoked,
   * which ensures that the checked typed has been <i>standardized</i>.
   */
  public final void registerCodeEntry () throws DefinitionException
    {
      _codeEntry = _symbol.registerCodeEntry(_checkedType);
      if (_isSetOnEvaluation)
        _codeEntry.setOnEvaluation();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _body.typeCheck(typeChecker);
      typeChecker.unify(_type,_body.typeRef(),this);

      if (_isField)
        typeChecker.disallowVoid(((FunctionType)type()).curryedRange(),
                                 this,
                                 "field "+_symbol+" of class "+
                                 ((ClassType)((FunctionType)type()).domain(0)).name());
    }

  public final Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                        int intDepth, int realDepth, int objectDepth)
    {
      throw new UnsupportedOperationException
        ("method shiftOffsets may not be called on a Definition!");
    }

  /**
   * Compiling a definition amounts to assigning a code array to the defined symbol
   * for its type and compiling its body into this code array. This will also make
   * the compiler be aware that it is compiling a definition (as opposed to an
   * expression to evaluate), by setting its "<tt>codeEntry</tt>" field to the
   * defined symbol's <a href="DefinedEntry.html"><tt>DefinedEntry</tt></a>. This
   * allows keeping track of unsafe call dependencies that need to be released
   * later (<i>i.e.</i>, in the case of mutual recursion). Thus, a compiled
   * definition's code is deemed <i>unsafe</i> as long as it contains such unsafe
   * code. Clearly, unsafe code must never be allowed to run.  A new definition
   * will release any unsafe code that need it by following dependency pointers
   * established from the <a href="DefinedEntry.html"><tt>DefinedEntry</tt></a> of
   * any unsafe definition where calls to it appear.
   */
  public final void compile (Compiler compiler)
    {
      if (_isField)
        _codeEntry.setFieldInfo();

      if (_isProjection)
        _codeEntry.setIsProjection();

      compiler.setCodeEntry(_codeEntry);
      _body.compile(compiler);
    }

  public final String toString ()
    {
      return _symbol + " = " + _body;
    }
}
