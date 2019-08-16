//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

/**
 * This class is the class of global assignment expressions.
 */
public class GlobalAssignment extends Assignment
{
  public GlobalAssignment (Expression lhs, Expression rhs)
    {
      _lhs = lhs;
      _rhs = rhs; 
    }

  public final Expression copy ()
    {
      return new GlobalAssignment(_lhs.copy(),_rhs.copy());
    }

  public final Expression typedCopy ()
    {
      return new GlobalAssignment(_lhs.typedCopy(),_rhs.typedCopy()).addTypes(this);
    }

  /**
   * Sanitizes the names of all expressions in this assignment.
   */
  public final Expression sanitizeNames (ParameterStack parameters)
    {
      _lhs = _lhs.sanitizeNames(parameters);
      _rhs = _rhs.sanitizeNames(parameters);

      if (!(_lhs instanceof Global))
        throw locate(new AssignmentErrorException("unassignable location: "+_lhs));

      return this;
    }

  /**
   * Compiles this global assignment in the context of the specified
   * <a href="Compiler.html"><tt>Compiler</tt></a>.
   */
  public final void compile (Compiler compiler)
    {
      Global lhs = (Global)_lhs;
      DefinedEntry entry = lhs.symbol().registerCodeEntry(lhs.checkedType().standardize());
      entry.setInlinable(false);

      _rhs.compile(compiler);

      if (_rhs.checkedType().isBoxedType() && !_lhs.checkedType().isBoxedType())
        compiler.generateUnwrapper(_rhs.boxSort());

      if (!_rhs.checkedType().isBoxedType() && _lhs.checkedType().isBoxedType())
        compiler.generateWrapper(_rhs.boxSort());

      compiler.generate(new SetGlobal(entry));

      if (VOID_ASSIGNMENTS)
        compiler.generateStackPop(entry.type().boxSort());
    }
}
