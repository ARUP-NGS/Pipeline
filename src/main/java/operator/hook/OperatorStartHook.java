package operator.hook;



public abstract class OperatorStartHook extends OperatorHook implements IOperatorStartHook{

	@Override
	public abstract void doHook() throws Exception;

}
