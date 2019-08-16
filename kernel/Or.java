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

public class Or extends AndOr
{
  public Or (Expression left, Expression rite)
    {
      super(left,rite);
    }

  public final Expression copy ()
    {
      return new Or(_left.copy(),_left.copy());
    }

  public final Expression typedCopy ()
    {
      return new Or(_left.copy(),_left.copy()).addTypes(this);
    }

  public final void compile (Compiler compiler)
    {
      _left.compile(compiler);
      if (_left.checkedType().isBoxedType())
        compiler.generateUnwrapper(Type.INT_SORT);

      JumpOnTrue jot = new JumpOnTrue();
      compiler.generate(jot);

      _rite.compile(compiler);
      if (_rite.checkedType().isBoxedType())
        compiler.generateUnwrapper(Type.INT_SORT);

      JumpOnFalse jof = new JumpOnFalse();
      compiler.generate(jof);

      jot.setAddress(compiler.targetAddress());

      if (checkedType().isBoxedType())
        compiler.generate(Instruction.PUSH_BOXED_TRUE);
      else
        compiler.generate(Instruction.PUSH_TRUE);

      Jump jmp = new Jump();
      compiler.generate(jmp);

      jof.setAddress(compiler.targetAddress());

      if (checkedType().isBoxedType())
        compiler.generate(Instruction.PUSH_BOXED_FALSE);
      else
        compiler.generate(Instruction.PUSH_FALSE);

      jmp.setAddress(compiler.targetAddress());
    }

  final public String toString ()
    {
      return _left + " or " + _rite;
    }

}
