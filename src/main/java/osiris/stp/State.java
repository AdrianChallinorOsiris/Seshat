package osiris.stp;

import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import java.util.Scanner;

@Data
@Log4j2
public class State {

	private String name;
	private Vector<Transition> trans;
	private Iterator<Transition> it;
	private Grammar grammar;
	public static final String newline = System.getProperty("line.separator");

	public State(String n) {
		name = n.toUpperCase();
		trans = new Vector<Transition>();
	}
	
	public Match match(Scanner sc) throws noNextTransitionException { 
		Match match = null;
		if (trans.isEmpty()) 
			throw new noNextTransitionException("State = " + name);
		for (Transition t : trans) { 
			Token token = t.match(sc);
			if (token != null) { 
				match = new Match(t, token);
				break;
			}
		}
		return match;
	}

	public String toString() {
		return "STATE=" + name + " size=" + trans.size();
	}
	public String getName() {
		return name;
	}

	public State add(Transition t) { 
		trans.add(t);
		return this;
	}
	
	public int size() { 
		return trans.size();
	}
		
	
	public void print(Writer w, int level) throws IOException {
		trans.sort(Comparator.comparing((Transition t) -> t.getName()));
		for(Transition t: trans) { 
			t.print(w, level);
		}
	}

	
	public void help() { 
		System.out.println("Available commands / options are:");
		trans.sort(Comparator.comparing((Transition t) -> t.getName()));
		for(Transition t: trans) { 
			t.help();
		}
		System.out.println();
	}
	
	public boolean check() { 
		boolean ok = true;
		if (trans.isEmpty()) { 
			log.error("No transitions in state {}", name);
			ok = false;			
		}
		
		for (Transition t: trans) {
			State s = t.getSuccessState();
			if (s != null) {
				if (!grammar.contains(s)) {
					log.error("Missing state {} in state {} - Transition {}", s.getName(), name, t.toString());
					ok = false;			
				}
			}
		}
		return ok;
	}

}
