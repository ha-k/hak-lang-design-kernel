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
 * This class is the class of local assignment expressions.
 */
public class LocalAssignment extends Assignment
{
  public LocalAssignment (Expression lhs, Expression rhs)
    {
      _lhs = lhs;
      _rhs = rhs; 
    }

  public final Expression copy ()
    {
      return new LocalAssignment(_lhs.copy(),_rhs.copy());
    }

  public final Expression typedCopy ()
    {
      return new LocalAssignment(_lhs.typedCopy(),_rhs.typedCopy()).addTypes(this);
    }

  /**
   * Sanitizes the names of all expressions in this assignment.
   */
  public final Expression sanitizeNames (ParameterStack parameters)
    {
      _lhs = _lhs.sanitizeNames(parameters);
      _rhs = _rhs.sanitizeNames(parameters);

      if (!(_lhs instanceof Local))
        throw locate(new AssignmentErrorException("unassignable location: "+_lhs));

      return this;
    }

  /**
   * Compiles this local assignment in the context of the specified
   * <a href="Compiler.html"><tt>Compiler</tt></a>.
   */
  public final void compile (Compiler compiler)
    {
      _rhs.compile(compiler);
      
      switch (_lhs.boxSort())
        {
        case Type.INT_SORT:
          if (_rhs.checkedType().isBoxedType())
            compiler.generateUnwrapper(Type.INT_SORT);
          compiler.generate(new SetOffsetInt(((Local)_lhs).offset()));
          break;
        case Type.REAL_SORT:
          if (_rhs.checkedType().isBoxedType())
            compiler.generateUnwrapper(Type.REAL_SORT);
          compiler.generate(new SetOffsetReal(((Local)_lhs).offset()));
          break;
        case Type.OBJECT_SORT:
          if (!_rhs.checkedType().isBoxedType())
            compiler.generateWrapper(_rhs.sort());
          compiler.generate(new SetOffsetObject(((Local)_lhs).offset()));
        }

      if (VOID_ASSIGNMENTS)
        compiler.generateStackPop(_lhs.boxSort());
    }
}
