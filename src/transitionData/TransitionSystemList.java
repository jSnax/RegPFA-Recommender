package transitionData;

import java.io.Serializable;
import java.util.ArrayList;

import recommendationData.Recommendation;
import recommendationData.RecommendationList;

public class TransitionSystemList implements Serializable{
	private static final long serialVersionUID = 1L;
	public ArrayList<TransitionSystem> allSystems;

	public ArrayList<TransitionSystem> getAllSystems() {
		return allSystems;
	}

	public void setAllSystems(ArrayList<TransitionSystem> allSystems) {
		this.allSystems = allSystems;
	}
	
	public TransitionSystemList(){
		ArrayList<TransitionSystem> allSystems = new ArrayList<TransitionSystem>();
		this.setAllSystems(allSystems);
	}
	
	
	public ArrayList<Recommendation> recommendNextTransition(ArrayList<String> currentTransitions, int howMany, int weightFactor, TransitionFrequencyList allFrequencies){
		RecommendationList allRecommendations = new RecommendationList();
		// pre-creating list where all possible recommendations are stored
		if (currentTransitions.isEmpty()){
			// If no prior transitions are recorded, we want to output transitions from the start state
			for (TransitionSystem currentSystem : this.getAllSystems()){
				// iterate over all transition system
				for (String currentTransitionName : currentSystem.getStartState().getTransitionsTo().keySet()){
					// iterate over all transition labels originating from the starting state
					for (Transition currentTransition : currentSystem.getStartState().getTransitionsTo().get(currentTransitionName)){
						// iterate over all transitions of the currently viewed transition labels originating from the start state
						allRecommendations.getRecommendations().add(new Recommendation(currentTransition.probability +1, currentTransitionName));
						// add label as recommendation with probability inflated by adding 1
					}
				}
			}
		}
		else {
			// if at least one prior transition exists
			int startingPoint = 0;
			int endPoint = currentTransitions.size();
			ArrayList<String> uniqueFound = new ArrayList<String>();
			// pre-creating variables
			while (startingPoint < endPoint && uniqueFound.size()<howMany){
				// iterate over the starting point in the given list of transitions and stop when either the end of the list was reached
				// OR enough possible following transitions were found
				ArrayList<Recommendation> currentRecommendations = new ArrayList<Recommendation>();
				for (TransitionSystem currentSystem : this.getAllSystems()){
					currentRecommendations.addAll(currentSystem.getRecommendations(currentTransitions));
				}
				// Iterate over all systems and get recommendations adhering to the current list of previous transitions
				for (Recommendation recommendation : currentRecommendations){
					recommendation.setProbability(recommendation.getProbability()+currentTransitions.size());
					// Since longer sequences of transitions are weighed more heavily, inflate the probability by adding the length of the sequence
					if (recommendation.getProbability().intValue() == recommendation.getProbability()){
						recommendation.setProbability(recommendation.getProbability()-0.0001);
					}
					// Adjusting the recommendation value by subtracting 0,0001 in case the uninflated proability was 1.0
					// Needed to ensure accuracy of recommendations since else a recommendation with currentTransitions length of 1
					// And probability 1 can lead to higher ranked recommendations than a recommendation with currentTransition length of 2, but probability lower than 1
					if (!uniqueFound.contains(recommendation.getNextTransition())){
						uniqueFound.add(recommendation.getNextTransition());
					}
					// Check if the transition to be performed next was already found in an earlier iteration in order to guarantee that howMany unique transitions will be returned
				}
				allRecommendations.getRecommendations().addAll(currentRecommendations);
				// before adding them to the lists of results
				ArrayList<String> tempList = new ArrayList<String>();
				for (int i = 1; i < currentTransitions.size(); i++){
					tempList.add(currentTransitions.get(i));
				}
				currentTransitions = tempList;
				// removing the first element of transition sequence to only search for shorter sequences now
				startingPoint++;
			}
		}
		if (!allRecommendations.getRecommendations().isEmpty()){
			// if at least one recommendation was found
			allRecommendations.mergeRecommendations(weightFactor);
			// Merge multiple recommendations for the same transition to just one
		}
		RecommendationList finalResult = new RecommendationList();
		int foundNormally = allRecommendations.getRecommendations().size();
		// store amount of recommendations found via checking for previous transitions on foundNormally
		if (foundNormally > howMany){
			// if more transitions were found than need to be returned
			for (int i = 0; i < howMany; i++){
				finalResult.getRecommendations().add(allRecommendations.getRecommendations().get(i));
			}
			// Only retain those that have the highest probability
		}
		else {
			if (foundNormally < howMany){
				// if fewer transitions were found than need to be returned
				System.out.println("Only "+foundNormally+" definite recommendations were found.");
				int counter = 0;
				if (foundNormally != 0){
					// if at least one recommendation was found
					while (counter < allFrequencies.getAllFrequencies().size() && (allRecommendations.getRecommendations().size() < howMany)){
						if (!allRecommendations.getTransitionLabels().contains(allFrequencies.getAllFrequencies().get(counter).getLabel())){
							allRecommendations.getRecommendations().add(new Recommendation(Double.valueOf(allFrequencies.getAllFrequencies().get(counter).getFrequency()), allFrequencies.getAllFrequencies().get(counter).getLabel()));
						}
						counter++;
					}
					// only add as many transitions as needed from the sorted list of all transitions and their respective frequencies, and only
					// those that are not already found. Probability is equal to their frequency for now, but will be adjusted
					Double totalNewFrequencies = 0.0;
					for (int i = foundNormally; i < allRecommendations.getRecommendations().size(); i++){
						totalNewFrequencies += allRecommendations.getRecommendations().get(i).getProbability();
					}
					// calculating total amount of frequencies over all frequency based recommendations
					for (int i = foundNormally; i < allRecommendations.getRecommendations().size(); i++){
						allRecommendations.getRecommendations().get(i).setProbability(allRecommendations.getRecommendations().get(i).getProbability() / totalNewFrequencies);
					}
					// dividing all frequencies by the total amount
					finalResult = allRecommendations;
				}
				else {
					// if no recommendation was found at all (i.e. the last modeled element was not ever recorded previously)
					while (counter < allFrequencies.getAllFrequencies().size() && (allRecommendations.getRecommendations().size() < howMany)){
						if (!allRecommendations.getTransitionLabels().contains(allFrequencies.getAllFrequencies().get(counter).getLabel())){
							allRecommendations.getRecommendations().add(new Recommendation(Double.valueOf(allFrequencies.getAllFrequencies().get(counter).getFrequency()), allFrequencies.getAllFrequencies().get(counter).getLabel()));
						}
						counter++;
					}
					finalResult = allRecommendations;
					// return the first howMany transitions from the sorted list of all transitions and their respective frequencies
					// with probability according to their frequency
				}
			}
			else{
				// if exactly as many recommendations were found as needed
				finalResult = allRecommendations;
			}
		}
		finalResult.weighRecommendations();
		return finalResult.getRecommendations();
	}
}
