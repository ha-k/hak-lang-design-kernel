//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Thu Mar 24 12:07:15 2016 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

/**
 * This class denotes an expression as a yet undecided choice between two
 * expressions. It is used for an expression that may be either of two options
 * depending of their types when their respective types can be determined only
 * from the syntactic nature of these expressions. Therefore, <i>this construct
 * is only safe to use on two expressions that may not be simultaneously
 * well-typed based on their internal structures</i> (as opposed to the
 * preceding typing context). The semantics of this construct is simply that of
 * the first expression if it can be type-checked without backtracking to an
 * earlier state; otherwise it is that of the second expression.
 *
 * <p>
 *
 * If more than two options are to be considered, this expression may be nested
 * within similar expressions. The typechecker maintains a stack of cut-point
 * states to ensure that the correct typing context is recovered for each such
 * undecided expression choice.
 */
public class UndecidedExpression extends ProtoExpression
{
  private Expression _fstOption;
  private Expression _sndOption;
  private Expression _actualChoice;

  public UndecidedExpression (Expression first, Expression second)
    {
      _fstOption = first.setExtent(this);
      _sndOption = second.setExtent(this);
    }

  public final Expression copy ()
    {
      return new UndecidedExpression(_fstOption.copy(),_sndOption.copy());
    }

  public final Expression typedCopy ()
    {
      return new UndecidedExpression(_fstOption.typedCopy(),
                                     _sndOption.typedCopy()).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return 2;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0: return _fstOption;
        case 1: return _sndOption;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _fstOption = expression;
          break;
        case 1:
          _sndOption = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  /**
   * Returns an indicator of the subexpression selected:
   * <ul>
   * <li> 0 if this is the first option expression,
   * <li> 1 if this is the second option expression,
   * <li> -1 otherwise (no choice yet)
   * </ul>
   **/
  public final int getActualChoiceIndicator ()
    {
      if (_actualChoice == null)
        return -1;

      if (_actualChoice == _fstOption)
        return 0;

      return 1;
    }

  /**
   * Returns the selected option.
   **/
  public final Expression getActualChoice () 
    {
      return _actualChoice;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _actualChoice.setCheckedType();
      _checkedType = _actualChoice.checkedType();
    }      

  public final void sanitizeSorts (Enclosure enclosure)
    {
      _actualChoice.sanitizeSorts(enclosure);
    }

  public final Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                        int intDepth, int realDepth, int objectDepth)
    {
      return _actualChoice.shiftOffsets(intShift,realShift,objectShift,
                                        intDepth,realDepth,objectDepth);
    }

  /**
   * Type-checking an <tt>UndecidedExpression</tt> works as follows:
   * <ol>
   * <li> a <a href="../types/TypingState.html"><tt>TypingState</tt></a> is pushed on
   *      the typechecker's cut-point stack in order to inhibit backtracking before
   *      this point;
   * <li> the first option expression is typechecked;
   * <li> if type-checking succeeds, the typechecker's cut-point stack is popped;
   * <li> if it fails:
   *      <ol>
   *      <li> all the typechecker's goals trailed after the latest cut-point
   *           are thrown away;
   *      <li> all the other typechecker's trails are unwound up to the points
   *           indicated by the latest cut-point;
   *      <li> the cut-point stack is popped;
   *      <li> the second option is typechecked as the actual expression.
   *      </ol>
   * </ol>
   * Note that if later bactracking comes back before this expression, only the previously
   * selected  option will be considered. This is fine for this construct's purpose since
   * the decision to choose between the two options must depend only on the syntactic nature
   * of the two expressions, and not the preceding typing context. Hence, a second pass of
   * type-checking is bound to give the same choice.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      typeChecker.pushCutPoint();
      try
        {
          (_actualChoice = _fstOption).typeCheck(_type,typeChecker);
          typeChecker.popCutPoint();
        }
      catch (TypingErrorException error)
        {
          typeChecker.undoCutPoint(); // this will also pop the cut-point
          (_actualChoice = _sndOption).typeCheck(_type,typeChecker);
        }
    }

  public final void compile (Compiler compiler)
    {
      _actualChoice.compile(compiler);
    }

  final public String toString ()
    {
      if (_actualChoice == null)
        return "undecided("+_fstOption+" ? "+_sndOption+")";

      return _actualChoice.toString();
    }
}
