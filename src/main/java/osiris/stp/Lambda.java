package osiris.stp;
import java.util.Scanner;

public class Lambda  extends Transition {
	
	public Lambda() { 
		super();
		name = "LAMBDA"; 
	}

	@Override
	public Token match(Scanner sc)  {
		return new Token();
	}

}
