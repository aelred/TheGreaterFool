package agent.logging;

public class NoSuchChildException extends Exception {

	private static final long serialVersionUID = 7568852949831424803L;

	private Identity furthestChild;
	
	public NoSuchChildException(Identity identity) {
		furthestChild = identity;
	}
	
	public Identity getFurthestChild() {
		return furthestChild;
	}

}
