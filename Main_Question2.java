package lu.uni.apkanalysistemplate;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.options.Options;

public class Main {

    public static void main(String[] args) {

        

        setupSoot("/home/micsispa/Desktop/Android-platforms/jars/stubs/", "/home/micsispa/Desktop/Euc/koopaApp.apk",
                "/home/micsispa/Desktop/Euc/sootOutput/");

        // prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk);

        // output as APK, too//-f J
        Options.v().set_output_format(Options.output_format_dex);

        // resolve the PrintStream and System soot-classes
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

        System.out.println("Soot Options:");
        System.out.println("Android Jars: " + Options.v().android_jars());
        System.out.println("Src Prec: " + Options.v().src_prec());
        System.out.println("Output Format: " + Options.v().output_format());
        System.out.println("Output Directory: " + Options.v().output_dir());
        // Add more options as needed

        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

            @Override
            protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {

                final PatchingChain<Unit> units = b.getUnits();

                for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
                    final Unit u = iter.next();
                    u.apply(new AbstractStmtSwitch() {

                    	public void caseInvokeStmt(InvokeStmt stmt) {
                    	    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    	    if (isActivityOrFragmentCreationMethod(invokeExpr)) {
                    	        Local tmpString = addTmpString(b);

                    	        // Assign the method name to tmpString
                    	        units.insertBefore(Jimple.v().newAssignStmt(tmpString,
                    	                StringConstant.v(invokeExpr.getMethod().toString())), u);

                    	        SootMethod toCall = Scene.v().getSootClass("android.util.Log")
                    	                .getMethod("int i(java.lang.String,java.lang.String)");
                    	        SootMethodRef toCallRef = toCall.makeRef();

                    	        // Use "KoopaFARES" as the tag and tmpString as the message
                    	        units.insertBefore(Jimple.v().newInvokeStmt(
                    	                Jimple.v().newStaticInvokeExpr(toCallRef, StringConstant.v("Koopa_FARES_MASHAL"), tmpString)), u);

                    	        b.validate();
                    	    }
                    	}


                    });
                }
            }

        }));

//        // Add a new transformation pack for APK generation
//        PackManager.v().getPack("jop").add(new Transform("jop.myOutput", new SceneTransformer() {
//            protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
//                // Write the modified code to a new APK file
//                PackManager.v().writeOutput();
//            }
//        }));

        System.out.println("Running Soot analysis...");
        // soot.Main.main(args);
        PackManager.v().runPacks();
        PackManager.v().writeOutput();
        System.out.println("Soot analysis completed.");

    }

    private static Local addTmpRef(Body body) {
        Local tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }

    private static Local addTmpString(Body body) {
        Local tmpString = Jimple.v().newLocal("tmpString", RefType.v("java.lang.String"));
        body.getLocals().add(tmpString);
        return tmpString;
    }



    private static boolean isActivityOrFragmentCreationMethod(InvokeExpr invokeExpr) {
        String className = invokeExpr.getMethod().getDeclaringClass().getName();
        return className.contains("Activity") || className.contains("Fragment");
    }

    /**
     * Initialize Soot options It mimics command-line options
     */
    public static void setupSoot(String androidJar, String apkPath, String outputPath) {
        // Reset the Soot settings (it's necessary if you are analyzing several APKs)
        // G.reset();
        // Generic options
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        // Read (APK Dex-to-Jimple) Options
        Options.v().set_android_jars(androidJar); // The path to Android Platforms
        Options.v().set_src_prec(Options.src_prec_apk); // Determine the input is an APK
        Options.v().set_process_dir(Collections.singletonList(apkPath)); // Provide paths to the APK
        Options.v().set_process_multiple_dex(true); // Inform Dexpler that the APK may have more than one .dex files
        Options.v().set_include_all(true);
        // Write (APK Generation) Options
        Options.v().set_output_format(Options.output_format_dex);
        Options.v().set_output_dir(outputPath);
        Options.v().set_validate(true); // Validate Jimple bodies in each transform pack
        // Resolve required classes
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
    }
}
