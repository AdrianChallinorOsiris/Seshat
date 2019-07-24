package osiris.stp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Scanner;

public class DateTime extends Transition {
	private final Date dat = new Date();
	private final Time tim = new Time();
	
	public DateTime() {
		super();
		name = "<DATETIME dd-MMM-yyyy HH:MM ";
	}
	

	@Override
	public Token match(Scanner sc) {
		Token t = dat.match(sc);
		if (t != null) {
			Token ti = tim.match(sc);
			LocalTime lt; 
			LocalDate ld = t.getLdval();
			if (ti != null) 
				lt = ti.getLtval();
			else
				lt = LocalTime.MIDNIGHT;
			
			t.setLdtval(LocalDateTime.of(ld, lt));
			t.sval = t.sval + " " + lt.toString();
			t.fmt = t.fmt + "THH:MM";
		}
		return t;
	}
	


}
