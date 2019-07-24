package osiris.stp;


import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Vector;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class Grammar {
	/**
	 * Logger for this class
	 */

	private HashMap<String, State> states;
	private String name;
	private State InitialState;
	public static final String newline = System.getProperty("line.separator");
	
	public Grammar() {
		states = new HashMap<String, State>();
	}

	public Grammar(String name, String InitialState) {
		states = new HashMap<String, State>();
		this.name = name;
		this.InitialState = state(InitialState);
	}


	public Grammar add(State s) {
		s.setGrammar(this);
		states.put(s.getName(), s);
		return this;
	}
	
	public int stateCount() {
		return states.size();
	}
	
	public State state(String n) {
		State s = null;
		s = states.get(n.toUpperCase());
		if (s == null) {
			s = new State(n);
			add(s);
		}
		return s;
	}
	
	public State getInitialState() {
		return InitialState;
	}
	
	public void print(Writer w) throws IOException { 
		w.write("Grammar name = " + name + " InitialState = " + InitialState.getName() + newline);
		w.write(newline);
		InitialState.print(w, 0);		
		w.flush();
	}

	public void prune() {
		log.debug("Pruning grammar..."); //$NON-NLS-1$
	
		Vector<String> v = new Vector<String> (states.keySet());
		for (String sn : v) {
			State s = states.get(sn);
			if (s.size() == 0) {
				states.remove(sn);
				log.debug("Pruning state -" + sn); //$NON-NLS-1$
			}
		}
	}
	
	public boolean check () { 
		log.debug("Checking grammar..."); //$NON-NLS-1$
		boolean ok = true;
		Vector<String> v = new Vector<String> (states.keySet());
		for (String sn : v) {
			State s = states.get(sn);
			ok &= s.check();
		}
		log.debug("Grammar {} checked - result {} ", name, ok);
		return ok;
	}
	
	public boolean contains(State s) {
		return states.containsKey(s.getName());
	}
	
}
