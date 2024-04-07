package lu.uni.apkanalysistemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lu.uni.apkanalysistemplate.utils.CommandLineOptions;
import lu.uni.apkanalysistemplate.utils.Constants;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.jimple.infoflow.InfoflowConfiguration; 

/**
 * This class is a template for APK analysis
 * For educational purpose
 *
 * @author  Jordan Samhi
 * @version 1.0
 * @since   2021
 */

public class Main { 
	// Inside the Virtual Machine //

    // Tests //
    static String APK_PATH = "/home/micsispa/Desktop/Project/koopaApp.apk"; 
    static String ANDROID_PATH = "/home/micsispa/Desktop/Project/Android-platforms/jars/stubs/";
    static String PKG_NAME = "lu.snt.trux.koopaapp";  
    static String SOURCES_AND_SINK_FILE_PATH = "/home/micsispa/git/FlowDroid/soot-infoflow-android/SourcesAndSinks.txt"; 
    
	public static void main(String[] args) throws Throwable { 
		

	    
		System.out.println(String.format("%s v1.0 started on %s\n", Constants.TOOL_NAME, new Date()));
		CommandLineOptions.v().parseArgs(args);

		
        
	        // Initialize
	        initializeSoot();

	        final InfoflowAndroidConfiguration ifac = new InfoflowAndroidConfiguration();
	        ifac.getAnalysisFileConfig().setTargetAPKFile(APK_PATH);
	        ifac.getAnalysisFileConfig().setAndroidPlatformDir(ANDROID_PATH);
	        ifac.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);

	        // CHA 
	        ifac.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA); 

	        // Setup
	        SetupApplication sa = new SetupApplication(ifac);
			sa.getConfig().setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance); 

	        // Call Graph
	        System.out.println("\n\n⚡ CALL GRAPH ⚡");
	        sa.constructCallgraph();
	        CallGraph callGraph = Scene.v().getCallGraph();

	        System.out.println("\n\n⚡ ENTRY POINTS ⚡");
	        sa.printEntrypoints();

	        // Nodes 
	        System.out.println("\n #️⃣  Number of nodes in the call graph: " + callGraph.size());

	        String locationMethod = ""; 
	        String locationMethodClass = ""; 
	        
	        //Find the location method 
	        Map<String, String> classAndMethod = findLocationMethod(); 
	        if(classAndMethod.isEmpty()) {
	        	System.out.println("Method not found!");
	        } 
	        else {
		        Map.Entry<String, String> entry = classAndMethod.entrySet().iterator().next(); 
	        	locationMethodClass = entry.getKey(); 
	        	locationMethod = entry.getValue(); 
	        }

	        
	        SootClass mainActivityClass = Scene.v().forceResolve("lu.snt.trux.koopaapp.MainActivity", SootClass.BODIES);
	        SootMethod onCreate = mainActivityClass.getMethodByName("onCreate"); 
	        
	        
			// Get the SootClass object for the target class
	        SootClass targetClass = Scene.v().forceResolve(locationMethodClass, SootClass.BODIES);

	        // Get the SootMethod object for the target method
	        SootMethod targetMethod = targetClass.getMethodByName(locationMethod); 
	        

	        // Find the shortest path between the onCreate and the targetMethod using breadth-first search
	        List<SootMethod> shortestPath = findShortestPath(callGraph, onCreate, targetMethod);

	        // Print the shortest path
	        System.out.println("The shortest path from " + onCreate + " to " + targetMethod + " is:");
	        for (SootMethod method : shortestPath) { 
	            System.out.println(method); 
	        } 
	        
	        System.out.println("The length of the shortest path is: " + shortestPath.size());

		
		/*
		 * The whole-jimple transformation pack.
		 * This is the primary pack into which you should insert any inter-procedural/whole-program analysis.
		 * When it executes, a call-graph has already been computed and can be accessed right away.
		 */
		PackManager.v().getPack("wjtp").add(
				new Transform("wjtp.myTransform", new SceneTransformer() {
					protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {						

				        
					}
				})); 
		

		/*
		 * The jimple transformation pack.
		 * This is usually where you want to place your intra-procedural analyses.
		 */
		PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {
					protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) { 
 
					}
				}));
		PackManager.v().runPacks();
	}

	

private static void initializeSoot() {
	G.reset();
	Options.v().set_allow_phantom_refs(true);
	Options.v().set_output_format(Options.output_format_none);
	Options.v().set_process_multiple_dex(true);
	Options.v().set_src_prec(Options.src_prec_apk);
	Options.v().set_whole_program(true);
   Options.v().set_android_jars(ANDROID_PATH);

	List<String> dirs = new ArrayList<String>();
	dirs.add(APK_PATH);
	Options.v().set_process_dir(dirs);

	Scene.v().loadNecessaryClasses();
}

	
	
	// A helper method that finds the shortest path between two methods in the call graph using breadth-first search
    public static List<SootMethod> findShortestPath(CallGraph callGraph, SootMethod source, SootMethod destination) {
        // Initialize a queue, a visited set, and a parent map
        List<SootMethod> queue = new ArrayList<SootMethod>();
        Set<SootMethod> visited = new HashSet<SootMethod>();
        Map<SootMethod, SootMethod> parent = new HashMap<SootMethod, SootMethod>();

        // Enqueue the source method and mark it as visited
        queue.add(source);
        visited.add(source);

        // Loop until the queue is empty or the destination is found
        while (!queue.isEmpty()) {
            // Dequeue the first method in the queue
            SootMethod current = queue.remove(0);

            // Check if the current method is the destination
            if (current.equals(destination)) {
                // Construct the path from the parent map
                List<SootMethod> path = new ArrayList<SootMethod>();
                SootMethod node = current;
                while (node != null) {
                    path.add(0, node);
                    node = parent.get(node);
                }
                // Return the path
                return path;
            }

            // Get the outgoing edges of the current method
            Iterator<Edge> edges = callGraph.edgesOutOf(current);

            // Loop through the edges
            while (edges.hasNext()) {
                // Get the target method of the edge
                SootMethod target = edges.next().tgt();

                // Check if the target method is not visited
                if (!visited.contains(target)) {
                    // Enqueue the target method and mark it as visited
                    queue.add(target);
                    visited.add(target);

                    // Set the parent of the target method to the current method
                    parent.put(target, current);
                }
            }
        }

        // Return null if no path is found
        return null;
    }
	
    
	

	    
	public static Map<String, String> findLocationMethod() { 
		Map<String, String> keyValue = new HashMap<String, String>(); 
		//Iterate over all classes 
		for (SootClass sootClass : Scene.v().getClasses()) { 
			//Check if class belongs to the following package 
			if(sootClass.getName().contains("lu.snt.trux.koopaapp")) { 
				//Iterate over all the methods of the found class 
				for(SootMethod sootMethod : sootClass.getMethods()) { 		
					//Check if the method contains the keyword location 
					if(sootMethod.getName().toLowerCase().contains("location")) { 
						//Get the body of the method 
						Body sootBody = sootMethod.retrieveActiveBody(); 
						//Iterate over the units 
						for (Unit unit : sootBody.getUnits()) { 
							Stmt stmt = (Stmt) unit; 	
							//Check if it has invoke expression 
							if(stmt.containsInvokeExpr()) {
								SootMethod target = stmt.getInvokeExpr().getMethod(); 
								//Finally check if it is iterating getLastLocation 
								if(target.getName().equals("getLastLocation")) { 
									System.out.println("Method found in the following unit: "); 
									System.out.println(unit); 
									System.out.println("Class: " + sootClass.getName()); 
									System.out.println("Method: " + sootMethod.getName()); 	 
									keyValue.put(sootClass.getName(), sootMethod.getName()); 
								}
							}
						} 									
					}
				} 
			}
		} 
		//Return the class name and method name 
		return keyValue; 
	}
	
	
 

}
