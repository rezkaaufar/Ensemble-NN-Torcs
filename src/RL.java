//import burlap.behavior.singleagent.Episode;
//import burlap.behavior.singleagent.learning.LearningAgent;
//import burlap.behavior.singleagent.learning.tdmethods.QLearning;
//import burlap.mdp.singleagent.SADomain;
//
///**
// * Created by daniel on 17-11-2016.
// */
//public class RL {
//
//    public void QLearningExample(String outputPath){
//
//        SADomain domain = new SADomain();
//
//
//        LearningAgent agent = new QLearning(domain, 0.99, hashingFactory, 0., 1.);
//
//        //run learning for 50 episodes
//        for(int i = 0; i < 50; i++){
//            Episode e = agent.runLearningEpisode(env);
//
//            e.write(outputPath + "ql_" + i);
//            System.out.println(i + ": " + e.maxTimeStep());
//
//            //reset environment for next learning episode
//            env.resetEnvironment();
//        }
//
//    }
//}
