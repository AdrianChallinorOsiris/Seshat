package osiris.stp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Match {
	private Transition trans;
	private Token token;
}
