package moa.tasks;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import cpjitsdpexperiment.ExpAIO_others;
import moa.classifiers.MultiClassClassifier;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningPerformanceEvaluator;
import moa.learners.Learner;
import moa.options.ClassOption;
import moa.streams.ExampleStream;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.Vector;

public class CpjitsdpAIO4 extends ClassificationMainTask {

    private static HashMap<String, Object> hashResults = new HashMap<>();
    private static final String RES_INSTANCE_CLASS = "instanceClass";
    private static final String RES_INSTANCE_PREDICTION = "instancePrediction";
    private static final String RES_RECALL_0 = "recall_0";
    private static final String RES_RECALL_1 = "recall_1";
    private static final String RES_TS = "timestamp";
    private static final String RES_TELAPSD = "timeelapsed";
    private static final String LOC = "LOC";
    private static final String LA_LD = "LA_LD";
    private static final String PROBPOS = "probPos";

    @Override
    public String getPurposeString() {
        return "Evaluates a classifier on a stream by testing then training with each example in sequence and respecting the "
                + "waiting period for finding the true labels.";
    }


    // Code representing each commit label scenario.
    // the commit is not buggy
    private static final int NOT_BUG = 0;
    // the commit is buggy but its true label was not found within W days for test dataset
    private static final int BUG_NOT_DISCOVERED_W_DAYS_TEST = 1;
    // the commit is buggy and its true label was found within W days for test dataset
    private static final int BUG_DISCOVERED_W_DAYS_TEST = 2;
    // the true label of a defective commit was assigned.
    private static final int BUG_FOUND = 3;
    // the commit is buggy but its true label was not found within W days for nontest dataset
    private static final int BUG_NOT_DISCOVERED_W_DAYS_NOT_TEST = 4;

    //		// the commit is buggy and its true label was found within W days for nontest dataset
    //		private static final int BUG_DISCOVERED_W_DAYS_NOT_TEST = 5;

    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l', "Learner to train.", MultiClassClassifier.class,
            "moa.classifiers.bayes.NaiveBayes");

    public ClassOption streamOption = new ClassOption("stream", 's', "Stream to learn from.", ExampleStream.class,
            "generators.RandomTreeGenerator");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.", LearningPerformanceEvaluator.class,
            "WindowClassificationPerformanceEvaluator");

    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).", 100000000, -1, Integer.MAX_VALUE);

    public IntOption timeLimitOption = new IntOption("timeLimit", 't',
            "Maximum number of seconds to test/train for (-1 = no limit).", -1, -1, Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency", 'f',
            "How many instances between samples of the learning performance.", 100000, 0, Integer.MAX_VALUE);

    public IntOption memCheckFrequencyOption = new IntOption("memCheckFrequency", 'q',
            "How many instances between memory bound checks.", 100000, 0, Integer.MAX_VALUE);

    public FileOption dumpFileOption = new FileOption("dumpFile", 'd', "File to append intermediate csv results to.",
            null, "csv", true);

    public FileOption outputPredictionFileOption = new FileOption("outputPredictionFile", 'o',
            "File to append output predictions to.", null, "pred", true);


    public IntOption widthOption = new IntOption("width", 'w', "Size of Window", 1000);

    public FloatOption alphaOption = new FloatOption("alpha", 'a', "Fading factor or exponential smoothing factor",
            .01);


    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        long startTime = System.nanoTime();
        Learner learner = (Learner) getPreparedClassOption(this.learnerOption);
        ExampleStream stream = (ExampleStream) getPreparedClassOption(this.streamOption);
        LearningPerformanceEvaluator evaluator = (LearningPerformanceEvaluator) getPreparedClassOption(
                this.evaluatorOption);
        LearningCurve learningCurve = new LearningCurve("learning evaluation instances");

        //hash containing the results to be dumped to a file at the end
        hashResults.put(RES_INSTANCE_CLASS, new Vector<Integer>());
        hashResults.put(RES_INSTANCE_PREDICTION, new Vector<Integer>());
        hashResults.put(RES_RECALL_0, new Vector<Double>());
        hashResults.put(RES_RECALL_1, new Vector<Double>());
        hashResults.put(RES_TS, new Vector<String>());
        hashResults.put(RES_TELAPSD, new Vector<String>());
        hashResults.put(LOC, new Vector<Integer>());
        hashResults.put(PROBPOS, new Vector<Double>());
        hashResults.put(LA_LD, new Vector<Integer>());


        if (evaluator instanceof FadingFactorClassificationPerformanceEvaluator) {
            if (alphaOption.getValue() != .01) {
                System.out.println(
                        "DEPRECATED! Use EvaluatePrequential -e (FadingFactorClassificationPerformanceEvaluator -a "
                                + alphaOption.getValue() + ")");
                return learningCurve;
            }
        }

        learner.setModelContext(stream.getHeader());
        int maxInstances = this.instanceLimitOption.getValue();
        long instancesProcessed = 0;
        long traincount = 0;
        int maxSeconds = this.timeLimitOption.getValue();
        int secondsElapsed = 0;
        monitor.setCurrentActivity("Evaluating learner...", -1.0);

        File outputPredictionFile = this.outputPredictionFileOption.getFile();
        PrintStream outputPredictionResultStream = null;
        if (outputPredictionFile != null) {
            try {
                if (outputPredictionFile.exists()) {
                    outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile, true),
                            true);
                } else {
                    outputPredictionResultStream = new PrintStream(new FileOutputStream(outputPredictionFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Unable to open prediction result file: " + outputPredictionFile, ex);
            }
        }

        String[] args = ExpAIO_others.getArgs();
//        System.out.println(Integer.parseInt(args[0]));
//        System.out.println(Integer.parseInt(args[1]));
        System.out.println(Integer.parseInt(args[0]));
        System.out.println(Integer.parseInt(args[5]));

        while (stream.hasMoreInstances() && ((maxInstances < 0) || (instancesProcessed < maxInstances))
                && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {

            boolean write_results = true;

            Example trainInst = stream.nextInstance();
            Example testInst = (Example) trainInst.copy();

            Attribute a = new Attribute("commit_type");
            int commit_type = (int) ((Instance) trainInst.getData()).value(a);

            Attribute b = new Attribute("project_no");
            int project_no = (int) ((Instance) trainInst.getData()).value(b);

//            Attribute c = new Attribute("la");
//            int la = (int) ((Instance) trainInst.getData()).value(c);
////            System.out.println("la:" + la);
//
//            Attribute d = new Attribute("lt");
//            int lt = (int) ((Instance) trainInst.getData()).value(d);
////            System.out.println("lt:" + lt);
//
//            Attribute e = new Attribute("ld");
//            int ld = (int) ((Instance) trainInst.getData()).value(e);
//            System.out.println("ld:" + ld);

//            int LOCtmp = lt - ld + la;
//            if (LOCtmp < 0) {
//                LOCtmp = 0;
//            }
//
//            int LA_LDtmp = la + ld;


//            // AEEEM
//            Attribute c = new Attribute("ck_oo_numberOfLinesOfCode");
//            int LOCtmp = (int) ((Instance) trainInst.getData()).value(c);

            // Z-JIRA ReLink
            Attribute c = new Attribute("CountLineCode");
            int LOCtmp = (int) ((Instance) trainInst.getData()).value(c);

//            // PROMISE
//            Attribute c = new Attribute("loc");
//            int LOCtmp = (int) ((Instance) trainInst.getData()).value(c);


            double probPostmp = .0;

//            System.out.println(trainInst.getData());

            //29  68  64  23
            ((Instance) trainInst.getData()).deleteAttributeAt(68);
            ((Instance) testInst.getData()).deleteAttributeAt(68);
            //28  67  63  22
            ((Instance) trainInst.getData()).deleteAttributeAt(67);
            ((Instance) testInst.getData()).deleteAttributeAt(67);


            int predictedClass = 0;

            double pNo = Integer.parseInt(args[0]);
            int sourcedata = Integer.parseInt(args[5]);


            if (commit_type == NOT_BUG) {
//                ((Instance) trainInst.getData()).setClassValue(0);
//                learner.trainOnInstance(trainInst);
                if (project_no == sourcedata) {
                    learner.trainOnInstance(trainInst);
                    traincount++;
                }
                if (project_no == pNo) {
                    double[] prediction = learner.getVotesForInstance(testInst);
                    if (prediction.length > 1) {
//                        System.out.println(prediction[0] + "," + prediction[1]);
//                        System.out.println(prediction[1]);
                        probPostmp = prediction[1] / 20;
//                        System.out.println(probPostmp);
                        if (Double.isNaN(prediction[1])) {
//                        System.out.println("prediction[0]" + prediction[0]);
                            probPostmp = 0;
                        }
                        if (Double.isInfinite(prediction[1])) {
//                        System.out.println("prediction[0]" + prediction[0]);
                            probPostmp = 1;
                        }
//                        if (prediction[0] > prediction[1]) {
//                            predictedClass = 0;
//                        } else {
//                            predictedClass = 1;
//                        }
                        if (probPostmp < 0.5) {
                            predictedClass = 0;
                        } else {
                            predictedClass = 1;
                        }
                    } else {
                        predictedClass = 0;
                    }
                    if (outputPredictionFile != null) {
                        int trueClass = (int) ((Instance) trainInst.getData()).classValue();

                        outputPredictionResultStream.println(Utils.maxIndex(prediction) + ","
                                + (((Instance) testInst.getData()).classIsMissing() == true ? " ? " : trueClass));
                    }
                    evaluator.addResult(testInst, prediction);
                    learner.trainOnInstance(trainInst);
                    traincount++;
                } else {
                    write_results = false;
                }

//                if (project_no != pNo) {
//                    learner.trainOnInstance(trainInst);
//                    traincount++;
//                }

//                learner.trainOnInstance(trainInst);
//                traincount++;

//                if (project_no == sourcedata) {
//                    learner.trainOnInstance(trainInst);
//                    traincount++;
//                }

//                if (project_no == sourcedata ) {
//                    learner.trainOnInstance(trainInst);
//                    traincount++;
//                }
            }

            // If bug found, then train.
            if (commit_type == BUG_FOUND) {
//                ((Instance) trainInst.getData()).setClassValue(1);
//                learner.trainOnInstance(trainInst);
                if (project_no == sourcedata) {
                    learner.trainOnInstance(trainInst);
                    traincount++;
                }
                if (project_no == pNo) {
                    double[] prediction = learner.getVotesForInstance(testInst);
                    if (prediction.length > 1) {
//                        System.out.println(prediction[0] + "," + prediction[1]);
//                        System.out.println(prediction[1]);
                        probPostmp = prediction[1] / 20;
//                        System.out.println(probPostmp);
                        if (Double.isNaN(prediction[1])) {
//                        System.out.println("prediction[0]" + prediction[0]);
                            probPostmp = 0;
                        }
                        if (Double.isInfinite(prediction[1])) {
//                        System.out.println("prediction[0]" + prediction[0]);
                            probPostmp = 1;
                        }
//                        if (prediction[0] > prediction[1]) {
//                            predictedClass = 0;
//                        } else {
//                            predictedClass = 1;
//                        }
                        if (probPostmp < 0.5) {
                            predictedClass = 0;
                        } else {
                            predictedClass = 1;
                        }
                    } else {
                        predictedClass = 0;
                    }
                    if (outputPredictionFile != null) {
                        int trueClass = (int) ((Instance) trainInst.getData()).classValue();

                        outputPredictionResultStream.println(Utils.maxIndex(prediction) + ","
                                + (((Instance) testInst.getData()).classIsMissing() == true ? " ? " : trueClass));
                    }
                    evaluator.addResult(testInst, prediction);
                    learner.trainOnInstance(trainInst);
                    traincount++;
                } else {
                    write_results = false;
                }


//                if (project_no != pNo) {
//                    learner.trainOnInstance(trainInst);
//                    traincount++;
//                }

//                learner.trainOnInstance(trainInst);
//                traincount++;

//                if (project_no == sourcedata) {
//                    learner.trainOnInstance(trainInst);
//                    traincount++;
//                }

//                if (project_no == sourcedata) {
//                    learner.trainOnInstance(trainInst);
//                    traincount++;
//                }
            }

            if (LOCtmp == 0) {
                write_results = false;
            }

            if (project_no == pNo && write_results) {
                instancesProcessed++;
                if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0
                        || stream.hasMoreInstances() == false) {

                    String ts = (((Instance) testInst.getData()).value(66) + "");
                    int idxE = ts.indexOf("E");

                    ts = new BigDecimal(((Instance) testInst.getData()).value(66) + "").intValue() + "";

                    if (idxE > 0) {

                        ((Vector<String>) hashResults.get(RES_TS))
                                .add(ts);//时间戳

                        ((Vector<Integer>) hashResults.get(RES_INSTANCE_CLASS))
                                .add(new Double(((Instance) testInst.getData()).classValue()).intValue());
                        ((Vector<Integer>) hashResults.get(LOC)).add(LOCtmp);
                        ((Vector<Double>) hashResults.get(PROBPOS)).add(probPostmp);
//                        ((Vector<Integer>) hashResults.get(LA_LD)).add(LA_LDtmp);
                        ((Vector<Integer>) hashResults.get(RES_INSTANCE_PREDICTION)).add(predictedClass);
                        ((Vector<Double>) hashResults.get(RES_RECALL_0))
                                .add(evaluator.getPerformanceMeasurements()[7].getValue());
                        ((Vector<Double>) hashResults.get(RES_RECALL_1))
                                .add(evaluator.getPerformanceMeasurements()[8].getValue());

                    }

                }
            }
        }


        // print output file
        String filePath = dumpFileOption.getValue();
        try (PrintWriter writer = new PrintWriter(new File(filePath))) {

            DecimalFormat df = new DecimalFormat("#0.00");

            StringBuilder sb = new StringBuilder();
            sb.append("actual label,");
            sb.append("LOC,");
//            sb.append("probPos(0)");
//            sb.append("probPos(1)");
            sb.append("predicted label,");
            sb.append("probPos,");
//            sb.append("LA_LD,");
            sb.append("\n");
//            sb.append("recall(0),");
//            sb.append("recall(1),");
//            sb.append("avg-gmean,");
//            sb.append("timestamp,");
//            sb.append("total timeelapsed in nanoseconds,");
//            sb.append("total timeelapsed in seconds \n");

            double each_gmean = 0;
            long endTime = System.nanoTime();
            // get difference of two nanoTime values
            long timeElapsed = endTime - startTime;

//            System.out.println("\nExecution time in nanoseconds  : " + timeElapsed);
//            System.out.println("Execution time in seconds : " +
//                    timeElapsed / (1000000 * 1000));
            long telapsed_nano = timeElapsed;
            long telapsed_second = timeElapsed / (1000000 * 1000);

            for (int i = 0; i < ((Vector<Integer>) hashResults.get(RES_INSTANCE_CLASS)).size(); i++) {
                each_gmean = Math.sqrt(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i) * ((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i));

                sb.append(((Vector<Integer>) hashResults.get(RES_INSTANCE_CLASS)).get(i) + ",");
                sb.append(((Vector<Integer>) hashResults.get(LOC)).get(i) + ",");
                sb.append(((Vector<Integer>) hashResults.get(RES_INSTANCE_PREDICTION)).get(i) + ",");
                sb.append(((Vector<Double>) hashResults.get(PROBPOS)).get(i) + ",");
//                sb.append(((Vector<Integer>) hashResults.get(LA_LD)).get(i) + ",");
                sb.append("\n");
//                sb.append(((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i) + ",");
//                sb.append(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i) + ",");
//                sb.append(each_gmean + ",");
//                sb.append(((Vector<String>) hashResults.get(RES_TS)).get(i) + ",");
//                sb.append(String.valueOf(telapsed_nano) + ",");
//                sb.append(String.valueOf(telapsed_second) + "\n");
            }

            writer.write(sb.toString());

            //compute summary of the results
            OptionalDouble averageRec0 = ((Vector<Double>) hashResults.get(RES_RECALL_0)).stream()
                    .mapToDouble(a -> a)
                    .average();

            OptionalDouble averageRec1 = ((Vector<Double>) hashResults.get(RES_RECALL_1)).stream()
                    .mapToDouble(a -> a)
                    .average();

            double avg_gmean = 0.;
            double avg_diffRecalls = 0.;

            for (int i = 0; i < ((Vector<Double>) hashResults.get(RES_RECALL_1)).size(); i++) {
                avg_gmean += Math.sqrt(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i) * ((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i));
                avg_diffRecalls += Math.abs(((Vector<Double>) hashResults.get(RES_RECALL_1)).get(i) - ((Vector<Double>) hashResults.get(RES_RECALL_0)).get(i));
            }

            avg_diffRecalls /= ((Vector<Double>) hashResults.get(RES_RECALL_1)).size();
            avg_gmean /= ((Vector<Double>) hashResults.get(RES_RECALL_1)).size();

            //DecimalFormat df = new DecimalFormat("#0.00");

            System.out.println("\nResults*******\n");
            System.out.println("Tested Instances: " + ((Vector<Double>) hashResults.get(RES_RECALL_1)).size());
            System.out.println("Average Recall(0): " + df.format(averageRec0.getAsDouble()));
            System.out.println("Average Recall(1): " + df.format(averageRec1.getAsDouble()));
            System.out.println("Average Diff_recalls: " + df.format(avg_diffRecalls));
            System.out.println("Average Gmean between recalls: " + df.format(avg_gmean));
            System.out.println("traincount:" + traincount);
            System.out.println("instancesProcessed:" + instancesProcessed);

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        return learningCurve;
    }
}