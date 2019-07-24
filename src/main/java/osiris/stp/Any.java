package osiris.stp;

import java.util.Scanner;

public class Any extends Transition {
	
	public Any() { 
		super();
		name = "ANY"; 
	}

	@Override
	public Token match(Scanner sc)  {
		return new Token(sc.next());
	}


}
