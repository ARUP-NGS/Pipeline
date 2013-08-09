package operator.hook;


public abstract class OperatorEndHook extends OperatorHook implements IOperatorEndHook {

	@Override
	public abstract void doHook() throws Exception;

}
