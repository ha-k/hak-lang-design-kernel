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

import hlt.language.util.ObjectToIntMap;

import java.util.AbstractList;
import java.util.Iterator;

/**
 * This class represents &lambda;-calculus abstractions. More
 * precisely, it represents multiple-parameter abstractions. This enables
 * application to multiple arguments without having to "curry" it into a
 * composition of single-parameter abstractions.  This class extends <a
 * href="Scope.html"><tt>Scope</tt></a> with the added mechanism needed to
 * encapsulate lexically closed variables.
 */
public class Abstraction extends Scope
{
  protected boolean _isExitable = true;
  private ObjectToIntMap _frame = new ObjectToIntMap();

  private int _intFrameSize = 0;
  private int _realFrameSize = 0;
  private int _objectFrameSize = 0;

  /**
   * Creates a new <code>Abstraction</code> instance.
   *
   * @param parameters a <code>Parameter[]</code> value
   * @param body an <code>Expression</code> value
   */
  public Abstraction (Parameter[] parameters, Expression body)
    {
      super(parameters,body);
    }

  public Abstraction (Parameter parameter, Expression body)
    {
      super(parameter,body);
    }

  public Abstraction (String name, Expression body)
    {
      super(name,body);
    }

  public Abstraction (Expression body)
    {
      super(body);
    }

  public Abstraction (AbstractList parameters, Expression body)
    {
      super(parameters,body);
    }

  public final Expression copy ()
    {
      Parameter[] parameters = new Parameter[_parameters.length];
      for (int i=parameters.length; i-->0;)
        parameters[i] = (Parameter)_parameters[i].copy();

      return new Abstraction(parameters,_body.copy()).setIsExitable(_isExitable);
    }

  public final Expression typedCopy ()
    {
      Parameter[] parameters = new Parameter[_parameters.length];
      for (int i=parameters.length; i-->0;)
        parameters[i] = (Parameter)_parameters[i].typedCopy();

      return new Abstraction(parameters,_body.typedCopy()).setIsExitable(_isExitable).addTypes(this);
    }

  protected final void _flatten ()
    {
      if (_body instanceof Abstraction)
        {
          Parameter[] nestedParameters = ((Abstraction)_body).parameters();
          Parameter[] newParameters = new Parameter[_parameters.length+nestedParameters.length];

          for (int i=_parameters.length; i-->0;)
            newParameters[i] = _parameters[i];
          for (int i=_parameters.length; i<newParameters.length;i++)
            newParameters[i] = nestedParameters[i-_parameters.length];

          _parameters = newParameters;
          _isExitable = ((Abstraction)_body).isExitable();
          _body = ((Abstraction)_body).body();
        }
    }

  public final boolean isExitable ()
    {
      return _isExitable;
    }

  public final Scope setIsExitable (boolean flag)
    {
      _isExitable = flag;
      return this;
    }

  public final Scope setNonExitable ()
    {
      _isExitable = false;
      return this;
    }

  public final void addToFrame (Local local)
    {
      _frame.put(local,local.offset());
    }

  public final void sanitizeSorts (Enclosure enclosure)
    {
      if (_isSortSanitized) return;

      enclosure.push(this);
      _body.sanitizeSorts(enclosure);
      enclosure.pop();

      for (Iterator i=_frame.keys(); i.hasNext();)
        {
          Local local = (Local)i.next();
          switch (local.boxSort())
            {
              case Type.INT_SORT:
                _intFrameSize = _maxFrameSize(local,_intFrameSize);
                break;
              case Type.REAL_SORT:
                _realFrameSize = _maxFrameSize(local,_realFrameSize);
                break;
              case Type.OBJECT_SORT:
                _objectFrameSize = _maxFrameSize(local,_objectFrameSize);
            }
        }

      _frame = null;    // no more needed: might as well recycle it!
      _isSortSanitized = true;
    }

  private final int _maxFrameSize (Local local, int frameSize)
    {
      return Math.max(frameSize,local.offset()-_frame.get(local));
    }

  public final Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                        int intDepth, int realDepth, int objectDepth)
    {
      boolean bodyHasShifted = false;

      if (bodyHasShifted |= (_intFrameSize > 0)) _intFrameSize += intShift;
      if (bodyHasShifted |= (_realFrameSize > 0)) _realFrameSize += realShift;
      if (bodyHasShifted |= (_objectFrameSize > 0)) _objectFrameSize += objectShift;

      if (bodyHasShifted)
        _body = _body.shiftOffsets(intShift,realShift,objectShift,
                                   intDepth+_intArity,
                                   realDepth+_realArity,
                                   objectDepth+_objectArity);

      return this;
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      if (_isExitable)
        typeChecker.prove(new PushExitableGoal(this));

      _typeCheckLocked = false;      
      super.typeCheck(typeChecker);

      if (_isExitable)
        typeChecker.prove(new PopExitableGoal(this));
    }

  protected final PushScope _pushInstruction ()
    {
      return new PushClosure(voidArity(),_intArity,_realArity,_objectArity,
                             _intFrameSize,_realFrameSize,_objectFrameSize).setIsExitable(_isExitable);
    }

  public final String toString ()
    {
      String s = "function";

      s += "(";
      
      for (int i=0; i<arity(); i++)
        {
          s += _parameters[i];
          if (i < arity()-1) s += ",";
        }

      return s + ") " + _body;
    }

}
