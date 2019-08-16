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
 * This class is the class of dummy assignment expressions.
 */
public class DummyAssignment extends Assignment
{
  /**
   * Constructs a dummy assignment with the specified name and expression.
   */
  public DummyAssignment (Tables tables, String name, Expression expression)
    {
      _lhs = new Dummy(tables,name).setExtent(this);
      _rhs = expression; 
    }

  public final Expression copy ()
    {
      return new DummyAssignment(((Dummy)_lhs).tables(),((Dummy)_lhs).name(), _rhs.copy());
    }

  public final Expression typedCopy ()
    {
      return new DummyAssignment(((Dummy)_lhs).tables(),((Dummy)_lhs).name(), _rhs.typedCopy())
                 .addTypes(this);
    }

  /**
   * Sanitizes the names of all expressions in this assignment.
   */
  public final Expression sanitizeNames (ParameterStack parameters)
    {
      _lhs = _lhs.sanitizeNames(parameters);
      _rhs = _rhs.sanitizeNames(parameters);

      if (_lhs instanceof Local)
        return new LocalAssignment(_lhs,_rhs).setExtent(this);
      if (_lhs instanceof Global)
        return new GlobalAssignment(_lhs,_rhs).setExtent(this);

      throw locate(new AssignmentErrorException("unassignable location: "+_lhs));
    }

  /**
   * A DummyAssignment never gets to invoke this.
   */
  public final void compile (Compiler compiler)
    {
      throw new UnsupportedOperationException("method compile may not be called on a DummyAssignment!");
    }
}
