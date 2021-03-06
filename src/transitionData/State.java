package transitionData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import recommendationData.Recommendation;

public class State implements Serializable {
	private static final long serialVersionUID = 1L;
	private String label;
	public Double probability;
	public boolean startState;
	public Map<String, ArrayList<Transition>> transitionsTo = new HashMap<String, ArrayList<Transition>>();
	public Map<String, ArrayList<Transition>> transitionsFrom = new HashMap<String, ArrayList<Transition>>();
	
	public State(){
		this.setTransitionsFrom(new HashMap<String, ArrayList<Transition>>());
		this.setTransitionsTo(new HashMap<String, ArrayList<Transition>>());
		this.setProbability(0.0);
	}
	
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	


	public Map<String, ArrayList<Transition>> getTransitionsTo() {
		return transitionsTo;
	}


	public void setTransitionsTo(Map<String, ArrayList<Transition>> transitionsTo) {
		this.transitionsTo = transitionsTo;
	}


	public Map<String, ArrayList<Transition>> getTransitionsFrom() {
		return transitionsFrom;
	}


	public void setTransitionsFrom(Map<String, ArrayList<Transition>> transitionsFrom) {
		this.transitionsFrom = transitionsFrom;
	}
	
	public Double getProbability() {
		return probability;
	}


	public void setProbability(Double probability) {
		this.probability = probability;
	}

	public boolean isStartState() {
		return startState;
	}

	public void setStartState(boolean startState) {
		this.startState = startState;
	}

	public void addTransitionTo (Transition transition){
		ArrayList<Transition> newList = this.getTransitionsTo().get(transition.getLabel());
		newList.add(transition);
		this.transitionsTo.put(transition.getLabel(), newList);
	}
	
	public void addTransitionFrom (Transition transition){
		ArrayList<Transition> newList = this.getTransitionsFrom().get(transition.getLabel());
		newList.add(transition);
		this.transitionsFrom.put(transition.getLabel(), newList);
	}

	public ArrayList<Recommendation> getRecommendations(ArrayList<String> sequence, Double probability) {
		ArrayList<Recommendation> result = new ArrayList<Recommendation>();
		if (sequence.size() == 0){
			return (this.getFinalRecommendations(probability));
		}
		if (this.getTransitionsTo().containsKey(sequence.get(0))){
			// If the next transition in the sequence is possible from this state
			for (Transition transition : this.getTransitionsTo().get(sequence.get(0))){
				// Iterate over all transitions with that name
				Double new_probability = transition.getProbability() * probability;
				// Probability to leave this state via current transition equals probability to reach this state with current sub-sequence of transitions
				// multiplied by probability of current transition to occur starting from this state 
				ArrayList<Recommendation> recommendations = transition.getTarget().getRecommendations(new ArrayList<String> (sequence.subList(1, sequence.size())), new_probability);
				// recursive call of this function. The first element of sequence was removed and the probability was altered
				result.addAll(recommendations);				
				// all possible next transitions and their probabilities are stored in result
			}
		}
		return (result);	
	}
	
	public ArrayList<Recommendation> getFinalRecommendations(Double probability) {
		// This method generates all recommendations for a certain sequence of transitions and a given start state
		ArrayList<Recommendation> result = new ArrayList<Recommendation>();
			// If the end of the input sequence was reached, all possible transitions from the current state may be recommended
			// This marks the end of the recursion
			ArrayList<String> allTransitions = new ArrayList<String>(this.getTransitionsTo().keySet());
			// Store the names of all possible transitions as an ArrayList of Strings
			if (allTransitions.isEmpty()){
				// if the current state is a final state with no outgoing transitions
				Recommendation endProcess = new Recommendation(probability, "<End of Process reached>");
				result.add(endProcess);
				return (result);
				// recommend no further transition
			}
			for (String transitionName : allTransitions){
				// Iterate over all possible transitions
				Double totalProbability = 0.0;
				for (Transition transition : this.getTransitionsTo().get(transitionName)){
					totalProbability += transition.getProbability();
				}
				Double recommendationProbability = totalProbability * probability;
				// calculate the probability of said transition to occur by summing up the probabilities to leave the current state
				// via this transition and then multiply by the probability to reach this state with the initial sequence
				if (recommendationProbability > 1.0){
					recommendationProbability = 1.0;
				}
				// If the probabilities to leave this state sum up to a number greater than 1 and the probability to land on
				// this state is 1, reset the total probability to 1 since there can't be a probability greater than 1.
				Recommendation recommendation = new Recommendation(recommendationProbability, transitionName);
				// Save next transition name as well as the aforementioned probability as a recommendation
				result.add(recommendation);
				// add the recommendation to result
			}
			return (result);
		}
	
	public void setStateProbabilities(ArrayList<State> alreadyVisited, Double probability){
		// this function adds the parameter probability to the current state's probability, then follows every transition from this state
		// and adds probability * transition_probability to the probability of that state by calling itself
		// the recursion ends once either an end state was reached, i.e. no transitions are possible
		// or if a state is reached that was already visited in the ongoing transition sequence, i.e. a circle was found
		this.setProbability(this.getProbability() + probability);
		// set probability of current state
		ArrayList<State> newlyVisited = new ArrayList<State>(); 
		for (int i = 0; i < alreadyVisited.size(); i++){
			newlyVisited.add(alreadyVisited.get(i));
		}
		newlyVisited.add(this);
		// create new object for list of all visited states, add current state to it
		for (String s : this.getTransitionsTo().keySet()){
			for (Transition t : this.getTransitionsTo().get(s)){
				// for all transitions originating from the current state
				if (!newlyVisited.contains(t.getTarget())){
					// if the target of the transition was not already visited by the current transition sequence
					t.getTarget().setStateProbabilities(newlyVisited, probability * t.getProbability());
					// recursive call with new list of visited states and new probability
				}
			}
		}
	}
	
}
